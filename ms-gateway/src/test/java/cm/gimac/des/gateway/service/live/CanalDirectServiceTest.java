package cm.gimac.des.gateway.service.live;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.live.Abonne;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.domain.live.EvenementLive;
import cm.gimac.des.gateway.domain.live.TamponRejeu;
import cm.gimac.des.gateway.domain.live.VueEvenement;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

/** Canal poussé : reprise, direct, filtrage par abonné, maintien, durée bornée, débit par connexion. */
class CanalDirectServiceTest {

    static final Duration DELAI = Duration.ofSeconds(5);

    final Sinks.Many<EvenementLive> source = Sinks.many().multicast().directBestEffort();

    CanalDirectService canal(Duration battement, Duration dureeMax, int tamponConnexion) {
        return new CanalDirectService(
            source::asFlux,
            new TamponRejeu(500, Duration.ofMinutes(5), Clock.systemUTC()),
            new CanalDirectService.Parametres(battement, dureeMax, tamponConnexion)
        );
    }

    CanalDirectService canal() {
        return canal(Duration.ofHours(1), Duration.ofHours(1), 16);
    }

    static EvenementLive homologation(String id, String institution) {
        return new EvenementLive(id, "homologation.liaison-opened.v1", "l1", EvenementLive.HOMOLOGATION, institution, null);
    }

    static EvenementLive message(String id, String... destinataires) {
        return new EvenementLive(id, "collaboration.message-sent.v1", "c1", EvenementLive.COLLABORATION, null, Set.of(destinataires));
    }

    static boolean commentaire(ServerSentEvent<VueEvenement> e, String texte) {
        return e.data() == null && texte.equals(e.comment());
    }

    static boolean evenement(ServerSentEvent<VueEvenement> e, String id) {
        return e.data() != null && id.equals(e.id()) && id.equals(e.data().id());
    }

    @Test
    @DisplayName("Ouverture signalée, puis événements en direct remis selon les règles de l'abonné")
    void direct() {
        CanalDirectService c = canal();
        Abonne u1 = Abonne.de(Couche.DES, "u1", "", null);
        StepVerifier.create(c.ouvrir(u1, null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(() -> {
                assertThat(c.connexions()).isEqualTo(1);
                source.tryEmitNext(message("m1", "u2"));
                source.tryEmitNext(message("m2", "u1", "u2"));
                source.tryEmitNext(homologation("h1", "I1"));
            })
            .expectNextMatches(e -> evenement(e, "m2") && e.data().topic().equals(EvenementLive.COLLABORATION))
            .expectNextMatches(e -> evenement(e, "h1"))
            .thenCancel()
            .verify(DELAI);
        assertThat(c.connexions()).isZero();
    }

    @Test
    @DisplayName("Reprise après « depuis » : événements du tampon postérieurs, puis direct, sans doublon")
    void reprise() {
        CanalDirectService c = canal();
        source.tryEmitNext(homologation("h1", null));
        source.tryEmitNext(homologation("h2", null));
        source.tryEmitNext(message("m3", "u9"));
        source.tryEmitNext(homologation("h4", null));
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.DES, "u1", "", null), "h1"))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .expectNextMatches(e -> evenement(e, "h2"))
            .expectNextMatches(e -> evenement(e, "h4"))
            .then(() -> source.tryEmitNext(homologation("h5", null)))
            .expectNextMatches(e -> evenement(e, "h5"))
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Portail : ni collaboration ni événement d'une autre institution")
    void portail() {
        CanalDirectService c = canal();
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.PORTAIL, "u1", "I1", "homologation,collaboration"), null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(() -> {
                source.tryEmitNext(message("m1", "u1"));
                source.tryEmitNext(homologation("h1", "I2"));
                source.tryEmitNext(homologation("h2", "I1"));
            })
            .expectNextMatches(e -> evenement(e, "h2"))
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Commentaire de maintien périodique ; flux clos après la durée maximale")
    void battementEtDuree() {
        StepVerifier.withVirtualTime(() -> canal(Duration.ofSeconds(25), Duration.ofSeconds(60), 16).ouvrir(Abonne.de(Couche.DES, "u1", "", null), null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .expectNoEvent(Duration.ofSeconds(24))
            .thenAwait(Duration.ofSeconds(1))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.BATTEMENT))
            .thenAwait(Duration.ofSeconds(25))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.BATTEMENT))
            .thenAwait(Duration.ofSeconds(10))
            .expectComplete()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Connexion lente : les événements les plus anciens sont abandonnés, les autres connexions ne sont pas freinées")
    void debitParConnexion() {
        CanalDirectService c = canal(Duration.ofHours(1), Duration.ofHours(1), 2);
        Abonne u1 = Abonne.de(Couche.DES, "u1", "", null);
        StepVerifier.create(c.ouvrir(u1, null), 1)
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(() -> {
                for (int i = 1; i <= 5; i++) {
                    source.tryEmitNext(homologation("h" + i, null));
                }
            })
            .thenRequest(2)
            .expectNextMatches(e -> evenement(e, "h4"))
            .expectNextMatches(e -> evenement(e, "h5"))
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Fermeture : connexions closes, source abandonnée ; erreur de la source journalisée")
    void fermeture() {
        CanalDirectService c = canal();
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.DES, "u1", "", null), null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(c::close)
            .expectComplete()
            .verify(DELAI);
        assertThat(source.currentSubscriberCount()).isZero();
        CanalDirectService enErreur = new CanalDirectService(
            () -> Flux.error(new IllegalStateException("courtier perdu")),
            new TamponRejeu(1, Duration.ofMinutes(1), Clock.systemUTC()),
            new CanalDirectService.Parametres(Duration.ofHours(1), Duration.ofHours(1), 1)
        );
        assertThat(enErreur.connexions()).isZero();
    }

    // Présence et saisie (D-72).

    static final Duration GRACE = Duration.ofMillis(200);

    CanalDirectService canalPresence() {
        return new CanalDirectService(
            source::asFlux,
            new TamponRejeu(500, Duration.ofMinutes(5), Clock.systemUTC()),
            new CanalDirectService.Parametres(Duration.ofHours(1), Duration.ofHours(1), 16, GRACE)
        );
    }

    static boolean presence(ServerSentEvent<VueEvenement> e, String type, String compte) {
        return e.data() != null && e.id() == null && type.equals(e.data().type()) && compte.equals(e.data().acteur()) && compte.equals(e.data().subject());
    }

    @Test
    @DisplayName("Présence : en ligne à la première connexion interne, hors ligne après le délai de grâce ; ni le portail ni soi-même")
    void presence() {
        CanalDirectService c = canalPresence();
        var u2 = new java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable>();
        var portail = new java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable>();
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.DES, "u1", "", null), null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(() -> {
                portail.set(c.ouvrir(Abonne.de(Couche.PORTAIL, "p1", "I1", null), null).subscribe());
                u2.set(c.ouvrir(Abonne.de(Couche.CONSOLE, "u2", "", null), null).subscribe());
            })
            .expectNextMatches(e -> presence(e, EvenementLive.EN_LIGNE, "u2"))
            .then(() -> {
                assertThat(c.presence().enLigne()).containsExactly("u1", "u2");
                u2.get().dispose();
                portail.get().dispose();
            })
            .expectNextMatches(e -> presence(e, EvenementLive.HORS_LIGNE, "u2"))
            .then(() -> {
                assertThat(c.presence().enLigne()).containsExactly("u1");
                assertThat(c.presence().vusLe()).containsOnlyKeys("u2");
            })
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Reconnexion dans le délai de grâce : aucune annonce hors ligne")
    void reconnexionSansClignotement() {
        CanalDirectService c = canalPresence();
        var u2 = new java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable>();
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.DES, "u1", "", null), null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(() -> u2.set(c.ouvrir(Abonne.de(Couche.DES, "u2", "", null), null).subscribe()))
            .expectNextMatches(e -> presence(e, EvenementLive.EN_LIGNE, "u2"))
            .then(() -> {
                u2.get().dispose();
                u2.set(c.ouvrir(Abonne.de(Couche.DES, "u2", "", null), null).subscribe());
            })
            .expectNextMatches(e -> presence(e, EvenementLive.EN_LIGNE, "u2"))
            .expectNoEvent(GRACE.multipliedBy(3))
            .thenCancel()
            .verify(DELAI);
        assertThat(c.presence().enLigne()).contains("u2");
    }

    @Test
    @DisplayName("Saisie : remise aux destinataires sans identifiant, jamais rejouée à la reprise")
    void saisieEphemere() {
        CanalDirectService c = canal();
        var saisie = new EvenementLive("s1", EvenementLive.SAISIE, "c1", EvenementLive.COLLABORATION, null, Set.of("u1"), "u2");
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.DES, "u1", "", null), null))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .then(() -> {
                source.tryEmitNext(homologation("h1", null));
                source.tryEmitNext(saisie);
                source.tryEmitNext(homologation("h2", null));
            })
            .expectNextMatches(e -> evenement(e, "h1"))
            .expectNextMatches(e -> e.id() == null && "s1".equals(e.data().id()) && "u2".equals(e.data().acteur()))
            .expectNextMatches(e -> evenement(e, "h2"))
            .thenCancel()
            .verify(DELAI);
        StepVerifier.create(c.ouvrir(Abonne.de(Couche.DES, "u1", "", null), "h1"))
            .expectNextMatches(e -> commentaire(e, CanalDirectService.OUVERTURE))
            .expectNextMatches(e -> evenement(e, "h2"))
            .expectNoEvent(Duration.ofMillis(100))
            .thenCancel()
            .verify(DELAI);
    }
}
