package cm.gimac.des.gateway.service.appareils;

import cm.gimac.des.gateway.domain.appareils.AnalyseurUserAgent;
import cm.gimac.des.gateway.domain.appareils.Appareil;
import cm.gimac.des.gateway.domain.appareils.ResultatTentative;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import cm.gimac.des.gateway.domain.appareils.port.AlerteNouvelAppareil;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.security.session.SessionGimac;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Suivi des tentatives de connexion (docs/lot1/mon-compte.md §3) : chaque tentative entre à
 * l'historique persistant ; une session ouverte est décrite (appareil, adresse, couche) pour
 * « Appareils connectés » ; une connexion complète depuis un appareil jamais vu pour le compte (familles
 * du navigateur et du système) déclenche l'alerte de MS-IAM, sauf à la toute première connexion
 * réussie du compte. Le suivi ne fait jamais échouer ni attendre indûment la connexion : base ou MS-IAM
 * en panne, il est abandonné et journalisé.
 */
public class SuiviConnexions {

    private static final Logger LOG = LoggerFactory.getLogger(SuiviConnexions.class);
    /** Délai laissé à l'historique avant de répondre au navigateur. */
    static final Duration DELAI = Duration.ofSeconds(2);

    private final HistoriqueConnexions historique;
    private final AlerteNouvelAppareil alerte;
    private final Clock horloge;

    public SuiviConnexions(HistoriqueConnexions historique, AlerteNouvelAppareil alerte, Clock horloge) {
        this.historique = historique;
        this.alerte = alerte;
        this.horloge = horloge;
    }

    /** Suivi désactivé (aucun historique, aucune alerte). */
    public static SuiviConnexions aucun() {
        return new SuiviConnexions(null, null, Clock.systemUTC());
    }

    /**
     * Connexion réussie pour le compte {@code sujet} : session décrite (si elle est ouverte),
     * tentative REUSSIE inscrite et, pour une connexion complète, détection d'un nouvel appareil.
     */
    public Mono<Void> reussie(DemandeConnexion d, String sujet, boolean complete, ServerWebExchange exchange) {
        Appareil appareil = AnalyseurUserAgent.analyser(d.userAgent());
        Instant le = horloge.instant();
        String couche = couche(d);
        Mono<Void> description = exchange == null
            ? Mono.empty()
            : exchange
                .getSession()
                .doOnNext(s -> {
                    if (SessionGimac.geree(s)) {
                        SessionGimac.decrire(s, sujet, appareil.navigateur(), appareil.systeme(), appareil.type().name(), d.adresseIp(), couche);
                    }
                })
                .then();
        if (historique == null) {
            return description;
        }
        Mono<Boolean> nouveau = complete && alerte != null
            ? historique
                .appareilsConnus(sujet)
                .map(Appareil::empreinte)
                .collect(Collectors.toSet())
                .map(connus -> !connus.isEmpty() && !connus.contains(appareil.empreinte()))
            : Mono.just(false);
        TentativeConnexion t = new TentativeConnexion(le, sujet, empreinte(d.identifiant()), ResultatTentative.REUSSIE, appareil, d.adresseIp(), couche);
        Mono<Void> suivi = nouveau
            .flatMap(n -> historique.enregistrer(t).thenReturn(n))
            .doOnNext(n -> {
                if (Boolean.TRUE.equals(n)) {
                    alerter(sujet, appareil, d.adresseIp(), couche, le);
                }
            })
            .then();
        return description.then(proteger(suivi, "connexion réussie"));
    }

    /** Tentative refusée : inscrite si c'est une tentative de l'utilisateur (identifiants, blocage). */
    public Mono<Void> echouee(DemandeConnexion d, EchecConnexion e) {
        if (historique == null) {
            return Mono.empty();
        }
        return ResultatTentative.pour(e.causeEchec(), e.etapeCode())
            .map(r ->
                proteger(
                    historique.enregistrer(
                        new TentativeConnexion(horloge.instant(), e.sujet(), empreinte(d.identifiant()), r, AnalyseurUserAgent.analyser(d.userAgent()), d.adresseIp(), couche(d))
                    ),
                    "tentative refusée"
                )
            )
            .orElse(Mono.empty());
    }

    /** Alerte hors du fil de la réponse : la connexion n'attend pas MS-IAM. */
    private void alerter(String sujet, Appareil appareil, String ip, String couche, Instant le) {
        alerte
            .alerter(sujet, appareil, ip, couche, le)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                v -> {},
                err -> LOG.warn("Alerte « nouvel appareil » non transmise à MS-IAM ({})", err.getMessage()),
                () -> LOG.info("Alerte « nouvel appareil » transmise à MS-IAM pour le compte {}", sujet)
            );
    }

    private static Mono<Void> proteger(Mono<Void> suivi, String quoi) {
        return suivi
            .timeout(DELAI)
            .onErrorResume(err -> {
                LOG.warn("Historique des connexions : {} non inscrite ({})", quoi, err.getMessage());
                return Mono.empty();
            });
    }

    private static String couche(DemandeConnexion d) {
        return d.couche() == null ? null : d.couche().name().toLowerCase(Locale.ROOT);
    }

    /** Empreinte de l'identifiant saisi (SHA-256 tronqué, insensible à la casse) : jamais en clair à l'historique. */
    public static String empreinte(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            return null;
        }
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(identifiant.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Empreintes des identifiants d'un compte (identifiant et courriel), pour retrouver ses échecs. */
    public static Set<String> empreintes(String... identifiants) {
        return java.util.Arrays.stream(identifiants).map(SuiviConnexions::empreinte).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }
}
