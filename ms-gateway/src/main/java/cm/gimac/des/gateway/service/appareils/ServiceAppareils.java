package cm.gimac.des.gateway.service.appareils;

import cm.gimac.des.gateway.domain.appareils.MasqueAdresseIp;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.security.session.FermetureSession;
import cm.gimac.des.gateway.security.session.ParametresSession;
import cm.gimac.des.gateway.security.session.RegistreSessions;
import cm.gimac.des.gateway.security.session.SessionGimac;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Appareils connectés d'un compte (docs/lot1/mon-compte.md §3) : sessions ouvertes par la connexion de
 * la plateforme et encore valides, session courante marquée, adresse masquée au dernier octet hors de
 * la session courante ; fermeture d'une autre session ou de toutes les autres (jeton de
 * rafraîchissement révoqué chez Keycloak, session supprimée) ; historique paginé des connexions.
 */
public class ServiceAppareils {

    /** Session ouverte décrite pour l'interface ; « id » opaque, propre à la session. */
    public record SessionOuverte(
        String id,
        boolean courante,
        String navigateur,
        String systeme,
        String type,
        String adresseIp,
        String couche,
        Instant ouverteLe,
        Instant derniereActivite,
        Instant expireA
    ) {}

    public static final int TAILLE_MAX = 100;

    private final RegistreSessions registre;
    private final FermetureSession fermeture;
    private final ParametresSession parametres;
    private final HistoriqueConnexions historique;
    private final Clock horloge;

    public ServiceAppareils(RegistreSessions registre, FermetureSession fermeture, ParametresSession parametres, HistoriqueConnexions historique, Clock horloge) {
        this.registre = registre;
        this.fermeture = fermeture;
        this.parametres = parametres;
        this.historique = historique;
        this.horloge = horloge;
    }

    /** Sessions ouvertes du compte, la courante d'abord, puis les plus récemment actives. */
    public List<SessionOuverte> sessions(String sujet, WebSession courante) {
        Instant maintenant = horloge.instant();
        return du(sujet)
            .filter(s -> !SessionGimac.expiree(s, maintenant, parametres))
            .map(s -> vue(s, estCourante(s, courante)))
            .sorted(Comparator.comparing(SessionOuverte::courante).reversed().thenComparing(SessionOuverte::derniereActivite, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    /** Ferme une autre session du compte ; 409 CNX_SESSION_COURANTE pour la courante, 404 si inconnue. */
    public Mono<Void> fermer(String sujet, String id, WebSession courante) {
        return Mono.defer(() -> {
            WebSession cible = du(sujet).filter(s -> Objects.equals(id, SessionGimac.texte(s, SessionGimac.IDENTIFIANT_APPAREIL))).findFirst().orElse(null);
            if (cible == null) {
                return Mono.error(new EchecConnexion(CauseEchec.APPAREIL_INCONNU));
            }
            if (estCourante(cible, courante)) {
                return Mono.error(new EchecConnexion(CauseEchec.SESSION_COURANTE));
            }
            return fermeture.fermer(cible, false);
        });
    }

    /** Ferme toutes les autres sessions du compte ; nombre de sessions fermées. */
    public Mono<Integer> fermerAutres(String sujet, WebSession courante) {
        return Flux.defer(() -> Flux.fromStream(du(sujet).filter(s -> !estCourante(s, courante)).toList().stream()))
            .concatMap(s -> fermeture.fermer(s, false).thenReturn(1))
            .reduce(0, Integer::sum);
    }

    /** Historique des connexions du compte (sujet, empreintes de ses identifiants), page bornée. */
    public Mono<HistoriqueConnexions.Page> historique(String sujet, Collection<String> empreintes, int page, int taille) {
        int p = Math.max(page, 0);
        int t = Math.clamp(taille, 1, TAILLE_MAX);
        return historique.lire(sujet, empreintes, p, t).onErrorMap(e -> !(e instanceof EchecConnexion), e -> new EchecConnexion(CauseEchec.HISTORIQUE_INDISPONIBLE, e.getMessage()));
    }

    private java.util.stream.Stream<WebSession> du(String sujet) {
        return registre.sessions().stream().filter(s -> !s.isExpired() && sujet != null && sujet.equals(SessionGimac.texte(s, SessionGimac.SUJET)) && SessionGimac.texte(s, SessionGimac.IDENTIFIANT_APPAREIL) != null);
    }

    static boolean estCourante(WebSession s, WebSession courante) {
        return courante != null && (s == courante || s.getId().equals(courante.getId()));
    }

    SessionOuverte vue(WebSession s, boolean courante) {
        String ip = SessionGimac.texte(s, SessionGimac.ADRESSE_IP);
        Object derniere = s.getAttribute(SessionGimac.DERNIERE_ACTIVITE);
        return new SessionOuverte(
            SessionGimac.texte(s, SessionGimac.IDENTIFIANT_APPAREIL),
            courante,
            SessionGimac.texte(s, SessionGimac.NAVIGATEUR),
            SessionGimac.texte(s, SessionGimac.SYSTEME),
            SessionGimac.texte(s, SessionGimac.TYPE_APPAREIL),
            courante ? ip : MasqueAdresseIp.masquer(ip),
            SessionGimac.texte(s, SessionGimac.COUCHE),
            s.getAttribute(SessionGimac.DEBUT) instanceof Instant i ? i : null,
            derniere instanceof Instant i ? i : null,
            SessionGimac.expireA(s, parametres).orElse(null)
        );
    }
}
