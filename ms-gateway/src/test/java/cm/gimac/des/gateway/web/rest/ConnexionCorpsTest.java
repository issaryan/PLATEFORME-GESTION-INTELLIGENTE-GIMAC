package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Corps de la connexion : jamais de mot de passe, de code ni de jeton dans les journaux. */
class ConnexionCorpsTest {

    @Test
    @DisplayName("toString : identifiant masqué, secrets absents")
    void journaux() {
        var c = new ConnexionResource.CorpsConnexion("agent.sna", "Secret-1", "123456", "des");
        assertThat(c.toString()).contains("ag***a").doesNotContain("Secret-1").doesNotContain("123456");
        assertThat(c.motDePasse()).isEqualTo("Secret-1");
        var o = new ConnexionResource.CorpsMotDePasseOublie("agent.sna@gimac.org", "des");
        assertThat(o.toString()).contains("ag***a@gimac.org").doesNotContain("agent.sna");
        assertThat(o.couche()).isEqualTo("des");
        var n = new ConnexionResource.CorpsNouveauMotDePasse("jeton-lien", "Nouveau-1");
        assertThat(n.toString()).doesNotContain("jeton-lien").doesNotContain("Nouveau-1");
        assertThat(n.jeton()).isEqualTo("jeton-lien");
    }
}
