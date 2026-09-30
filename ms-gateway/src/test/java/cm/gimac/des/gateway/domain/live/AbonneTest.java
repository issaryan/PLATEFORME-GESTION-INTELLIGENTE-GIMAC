package cm.gimac.des.gateway.domain.live;

import static cm.gimac.des.gateway.domain.live.EvenementLive.CERTIFICATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.COLLABORATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.HOMOLOGATION;
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
        assertThat(Couche.DES.sujets()).containsExactlyInAnyOrder(HOMOLOGATION, CERTIFICATION, COLLABORATION);
        assertThat(Couche.PORTAIL.sujets()).doesNotContain(COLLABORATION);
        assertThat(Couche.CONSOLE.sujets()).doesNotContain(COLLABORATION);
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
    @DisplayName("Collaboration : remise aux seuls destinataires, jamais au portail")
    void collaboration() {
        assertThat(Abonne.de(Couche.DES, "u1", "", null).recoit(message("u1", "u2"))).isTrue();
        assertThat(Abonne.de(Couche.DES, "u3", "", null).recoit(message("u1", "u2"))).isFalse();
        assertThat(Abonne.de(Couche.DES, "", "", null).recoit(message(""))).isFalse();
        assertThat(Abonne.de(Couche.DES, null, "", null).recoit(message("u1"))).isFalse();
        assertThat(Abonne.de(Couche.DES, "u1", "", null).recoit(message())).isFalse();
        assertThat(Abonne.de(Couche.PORTAIL, "u1", "I1", "collaboration").recoit(message("u1"))).isFalse();
    }

    @Test
    @DisplayName("Vue remise au navigateur : identifiant, type, sujet et sujet fonctionnel seulement")
    void vue() {
        EvenementLive e = new EvenementLive("m1", "collaboration.message-sent.v1", "c1", COLLABORATION, "I1", Set.of("u1"));
        assertThat(e.vue()).isEqualTo(new VueEvenement("m1", "collaboration.message-sent.v1", "c1", COLLABORATION));
        assertThat(homologation(null).destinataires()).isEmpty();
    }
}
