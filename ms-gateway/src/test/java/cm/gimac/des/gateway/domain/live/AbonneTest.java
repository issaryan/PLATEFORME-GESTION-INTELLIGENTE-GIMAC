package cm.gimac.des.gateway.domain.live;

import static cm.gimac.des.gateway.domain.live.EvenementLive.CERTIFICATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.COLLABORATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.HOMOLOGATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.NOTIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles de remise du canal poussé : sujets de la couche, institution du portail, destinataires. */
class AbonneTest {

    static EvenementLive homologation(String institution) {
        return new EvenementLive("e1", "homologation.liaison-opened.v1", "l1", HOMOLOGATION, institution, null);
    }

    static EvenementLive message(String... destinataires) {
        return new EvenementLive("m1", "collaboration.message-sent.v1", "c1", COLLABORATION, null, Set.of(destinataires));
    }

    @Test
    @DisplayName("Couche lue dans le chemin, insensible à la casse ; inconnue : vide")
    void couche() {
        assertThat(Couche.depuis("des")).contains(Couche.DES);
        assertThat(Couche.depuis("PORTAIL")).contains(Couche.PORTAIL);
        assertThat(Couche.depuis("console")).contains(Couche.CONSOLE);
        assertThat(Couche.depuis("autre")).isEmpty();
        assertThat(Couche.depuis(null)).isEmpty();
        assertThat(Couche.DES.sujets()).containsExactlyInAnyOrder(HOMOLOGATION, CERTIFICATION, COLLABORATION, EvenementLive.PRESENCE, NOTIFICATION);
        assertThat(Couche.PORTAIL.sujets()).containsExactlyInAnyOrder(HOMOLOGATION, CERTIFICATION, NOTIFICATION);
        assertThat(Couche.CONSOLE.sujets()).containsExactlyInAnyOrder(HOMOLOGATION, CERTIFICATION, COLLABORATION, EvenementLive.PRESENCE, NOTIFICATION);
    }

    @Test
    @DisplayName("Sujets : sans paramètre, ceux de la couche ; inconnus et interdits ignorés")
    void sujets() {
        assertThat(Abonne.de(Couche.DES, "u1", "", null).sujets()).isEqualTo(Couche.DES.sujets());
        assertThat(Abonne.de(Couche.DES, "u1", "", " ").sujets()).isEqualTo(Couche.DES.sujets());
        assertThat(Abonne.de(Couche.DES, "u1", "", "homologation, collaboration,inconnu").sujets()).containsExactlyInAnyOrder(
            HOMOLOGATION,
            COLLABORATION
        );
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", "collaboration,certification").sujets()).containsExactly(CERTIFICATION);
    }

    @Test
    @DisplayName("Sujet non retenu : non remis")
    void sujetNonRetenu() {
        assertThat(Abonne.de(Couche.DES, "u1", "", "certification").recoit(homologation(null))).isFalse();
        assertThat(Abonne.de(Couche.DES, "u1", "", "homologation").recoit(homologation(null))).isTrue();
        assertThat(Abonne.de(Couche.CONSOLE, "u1", "", null).recoit(homologation("I1"))).isTrue();
    }

    @Test
    @DisplayName("Portail : événements de l'institution de l'utilisateur seulement")
    void portail() {
        Abonne correspondant = Abonne.de(Couche.PORTAIL, "u1", "I1", null);
        assertThat(correspondant.recoit(homologation("I1"))).isTrue();
        assertThat(correspondant.recoit(homologation("I2"))).isFalse();
        assertThat(correspondant.recoit(homologation(null))).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "", null).recoit(homologation(""))).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", null, null).recoit(homologation(null))).isFalse();
    }

    @Test
    @DisplayName("Collaboration : remise aux seuls destinataires, au DES et à la console, jamais au portail")
    void collaboration() {
        assertThat(Abonne.de(Couche.DES, "u1", "", null).recoit(message("u1", "u2"))).isTrue();
        assertThat(Abonne.de(Couche.DES, "u3", "", null).recoit(message("u1", "u2"))).isFalse();
        assertThat(Abonne.de(Couche.DES, "", "", null).recoit(message(""))).isFalse();
        assertThat(Abonne.de(Couche.DES, null, "", null).recoit(message("u1"))).isFalse();
        assertThat(Abonne.de(Couche.DES, "u1", "", null).recoit(message())).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", "collaboration").recoit(message("u1"))).isFalse();
        assertThat(Abonne.de(Couche.CONSOLE, "u1", "", "collaboration").recoit(message("u1", "u2"))).isTrue();
        assertThat(Abonne.de(Couche.CONSOLE, "u3", "", null).recoit(message("u1", "u2"))).isFalse();
    }

    @Test
    @DisplayName("Notification de la plateforme (D-80) : sujet « notification » accepté, remise au seul destinataire, portail compris (D-93 §11.4)")
    void notification() {
        EvenementLive n = new EvenementLive("n1", "notification.inapp-created.v1", "n-1", NOTIFICATION, null, Set.of("u1"), "u2");
        assertThat(Abonne.de(Couche.DES, "u1", "", "collaboration,notification").sujets()).containsExactlyInAnyOrder(COLLABORATION, NOTIFICATION);
        assertThat(Abonne.de(Couche.DES, "u1", "", "notification").recoit(n)).isTrue();
        assertThat(Abonne.de(Couche.CONSOLE, "u1", "", null).recoit(n)).isTrue();
        assertThat(Abonne.de(Couche.DES, "u2", "", null).recoit(n)).isFalse();
        assertThat(Abonne.de(Couche.DES, "", "", null).recoit(n)).isFalse();
        assertThat(Abonne.de(Couche.DES, "u1", "", "collaboration").recoit(n)).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", "notification").sujets()).containsExactly(NOTIFICATION);
        // Portail : notification personnelle, sans institution dans l'enveloppe ; destinataire seul, institution exigée.
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", null).recoit(n)).isTrue();
        assertThat(Abonne.de(Couche.PORTAIL, "u3", "I1", null).recoit(n)).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "", null).recoit(n)).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", "homologation").recoit(n)).isFalse();
        assertThat(n.ephemere()).isFalse();
        assertThat(n.vue()).isEqualTo(new VueEvenement("n1", "notification.inapp-created.v1", "n-1", NOTIFICATION, "u2"));
    }

    @Test
    @DisplayName("Vue remise au navigateur : identifiant, type, sujet et sujet fonctionnel seulement")
    void vue() {
        EvenementLive e = new EvenementLive("m1", "collaboration.message-sent.v1", "c1", COLLABORATION, "I1", Set.of("u1"));
        assertThat(e.vue()).isEqualTo(new VueEvenement("m1", "collaboration.message-sent.v1", "c1", COLLABORATION));
        assertThat(homologation(null).destinataires()).isEmpty();
    }

    @Test
    @DisplayName("Présence (D-72) : tout abonné du DES ou de la console, sauf pour lui-même ; jamais le portail")
    void presence() {
        EvenementLive enLigne = new EvenementLive("p1", EvenementLive.EN_LIGNE, "u2", EvenementLive.PRESENCE, null, null, "u2");
        assertThat(Abonne.de(Couche.DES, "u1", "", null).recoit(enLigne)).isTrue();
        assertThat(Abonne.de(Couche.CONSOLE, "u1", "", null).recoit(enLigne)).isTrue();
        assertThat(Abonne.de(Couche.DES, "u2", "", null).recoit(enLigne)).isFalse();
        assertThat(Abonne.de(Couche.DES, "", "", null).recoit(enLigne)).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", "presence").recoit(enLigne)).isFalse();
        assertThat(Abonne.de(Couche.DES, "u1", "", "collaboration").recoit(enLigne)).isFalse();
        assertThat(enLigne.ephemere()).isTrue();
        assertThat(new EvenementLive("s1", EvenementLive.SAISIE, "c1", COLLABORATION, null, Set.of("u1"), "u2").ephemere()).isTrue();
        assertThat(message("u1").ephemere()).isFalse();
        assertThat(Couche.PORTAIL.interne()).isFalse();
        assertThat(enLigne.vue()).isEqualTo(new VueEvenement("p1", EvenementLive.EN_LIGNE, "u2", EvenementLive.PRESENCE, "u2"));
    }

    @Test
    @DisplayName("Accusé de lecture (D-88) : remis aux seuls destinataires internes, rejouable, luJusquA dans la vue")
    void accuseDeLecture() {
        EvenementLive l = new EvenementLive("r1", EvenementLive.LECTURE, "c1", COLLABORATION, null, Set.of("u1"), "u2", "2026-10-06T08:00:00Z");
        assertThat(Abonne.de(Couche.DES, "u1", "", "collaboration").recoit(l)).isTrue();
        assertThat(Abonne.de(Couche.CONSOLE, "u1", "", null).recoit(l)).isTrue();
        assertThat(Abonne.de(Couche.DES, "u2", "", null).recoit(l)).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", null).recoit(l)).isFalse();
        assertThat(l.ephemere()).isFalse();
        assertThat(l.vue()).isEqualTo(new VueEvenement("r1", EvenementLive.LECTURE, "c1", COLLABORATION, "u2", "2026-10-06T08:00:00Z"));
    }
}
