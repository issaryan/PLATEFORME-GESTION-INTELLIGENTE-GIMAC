package cm.gimac.des.gateway.service.live;

import cm.gimac.des.gateway.domain.live.Abonne;
import cm.gimac.des.gateway.domain.live.EvenementLive;
import cm.gimac.des.gateway.domain.live.RegistrePresence;
import cm.gimac.des.gateway.domain.live.TamponRejeu;
import cm.gimac.des.gateway.domain.live.VueEvenement;
import cm.gimac.des.gateway.domain.live.port.SourceEvenements;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Canal poussé des couches de présentation (F0 §A3, contrat de messagerie §5) : diffuse à chaque
 * connexion les événements de la source qu'elle peut recevoir, après ceux du tampon de reprise
 * postérieurs à son dernier identifiant ; commentaire de maintien périodique ; tampon borné par
 * connexion (les plus anciens sont abandonnés si le navigateur ne suit pas) ; durée de connexion
 * bornée, pour que les droits soient réévalués à la reconnexion. Présence (D-72) : une connexion d'une
 * couche interne met son compte en ligne ; il passe hors ligne si aucune ne la remplace pendant le
 * délai de grâce (la reconnexion périodique ne fait pas clignoter la présence). Signaux éphémères
 * (présence, saisie) : diffusés sans tampon de reprise ni identifiant.
 */
public class CanalDirectService implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CanalDirectService.class);

    /**
     * @param battement intervalle du commentaire de maintien (25 s)
     * @param dureeMaxConnexion durée au-delà de laquelle le flux est clos (le navigateur se reconnecte)
     * @param tamponConnexion événements en attente par connexion avant abandon des plus anciens
     * @param graceHorsLigne délai sans connexion avant d'annoncer un compte hors ligne (D-72)
     */
    public record Parametres(Duration battement, Duration dureeMaxConnexion, int tamponConnexion, Duration graceHorsLigne) {
        public static final Duration GRACE = Duration.ofSeconds(10);

        public Parametres(Duration battement, Duration dureeMaxConnexion, int tamponConnexion) {
            this(battement, dureeMaxConnexion, tamponConnexion, GRACE);
        }
    }

    static final String BATTEMENT = "battement";
    static final String OUVERTURE = "ouvert";

    private final Object verrou = new Object();
    private final TamponRejeu tampon;
    private final Parametres parametres;
    private final Sinks.Many<EvenementLive> diffusion = Sinks.many().multicast().directBestEffort();
    private final Sinks.Empty<Void> fermeture = Sinks.empty();
    private final Disposable abonnementSource;
    private final RegistrePresence presence;

    public CanalDirectService(SourceEvenements source, TamponRejeu tampon, Parametres parametres) {
        this(source, tampon, parametres, new RegistrePresence(Clock.systemUTC()));
    }

    public CanalDirectService(SourceEvenements source, TamponRejeu tampon, Parametres parametres, RegistrePresence presence) {
        this.tampon = tampon;
        this.parametres = parametres;
        this.presence = presence;
        this.abonnementSource = source
            .evenements()
            .subscribe(this::publier, erreur -> LOG.error("Source du canal poussé interrompue : {}", erreur.getMessage()));
    }

    /** Reçoit un événement : tampon de reprise (sauf signal éphémère), puis diffusion, atomiquement vis-à-vis d'une ouverture. */
    void publier(EvenementLive e) {
        synchronized (verrou) {
            if (!e.ephemere()) {
                tampon.ajouter(e);
            }
            diffusion.tryEmitNext(e);
        }
    }

    /** Présence des comptes internes (D-72). */
    public RegistrePresence.Etat presence() {
        return presence.etat();
    }

    private void annoncer(String type, String compte) {
        publier(new EvenementLive(UUID.randomUUID().toString(), type, compte, EvenementLive.PRESENCE, null, null, compte));
    }

    private void connecter(String compte) {
        if (presence.connecter(compte)) {
            annoncer(EvenementLive.EN_LIGNE, compte);
        }
    }

    private void deconnecter(String compte) {
        if (presence.deconnecter(compte)) {
            Mono.delay(parametres.graceHorsLigne())
                .takeUntilOther(fermeture.asMono())
                .subscribe(t -> {
                    if (!presence.enLigne(compte)) {
                        annoncer(EvenementLive.HORS_LIGNE, compte);
                    }
                });
        }
    }

    /**
     * Flux d'une connexion : reprise après {@code dernier}, puis événements en direct, filtrés par
     * les règles de l'abonné ; données limitées à {@link VueEvenement} (sans acteur sur le portail), identifiant
     * en ligne « id: ».
     */
    public Flux<ServerSentEvent<VueEvenement>> ouvrir(Abonne abonne, String dernier) {
        Flux<ServerSentEvent<VueEvenement>> evenements = Flux.<EvenementLive>create(sink -> brancher(sink, dernier), FluxSink.OverflowStrategy.BUFFER)
            .filter(abonne::recoit)
            .map(e -> {
                VueEvenement vue = abonne.vue(e);
                return e.ephemere() ? ServerSentEvent.builder(vue).build() : ServerSentEvent.builder(vue).id(e.id()).build();
            });
        Flux<ServerSentEvent<VueEvenement>> battements = Flux.interval(parametres.battement()).map(i ->
            ServerSentEvent.<VueEvenement>builder().comment(BATTEMENT).build()
        );
        ServerSentEvent<VueEvenement> ouverture = ServerSentEvent.<VueEvenement>builder().comment(OUVERTURE).build();
        Flux<ServerSentEvent<VueEvenement>> flux = Flux.merge(evenements, battements)
            .startWith(ouverture)
            .onBackpressureBuffer(
                parametres.tamponConnexion(),
                abandonne -> LOG.debug("Connexion lente : événement {} abandonné", abandonne.id()),
                BufferOverflowStrategy.DROP_OLDEST
            )
            .take(parametres.dureeMaxConnexion())
            .takeUntilOther(fermeture.asMono());
        String compte = abonne.compte();
        if (!abonne.couche().interne() || compte == null || compte.isBlank()) {
            return flux;
        }
        return flux.doOnSubscribe(s -> connecter(compte)).doFinally(signal -> deconnecter(compte));
    }

    /** Reprise et abonnement au direct sous le même verrou que {@link #publier} : ni trou ni doublon. */
    private void brancher(FluxSink<EvenementLive> sink, String dernier) {
        Disposable direct;
        synchronized (verrou) {
            tampon.depuis(dernier).forEach(sink::next);
            direct = diffusion.asFlux().subscribe(sink::next, sink::error, sink::complete);
        }
        sink.onDispose(direct);
    }

    /** Connexions en cours sur cette instance. */
    public int connexions() {
        return diffusion.currentSubscriberCount();
    }

    @Override
    public void close() {
        abonnementSource.dispose();
        diffusion.tryEmitComplete();
        fermeture.tryEmitEmpty();
    }
}
