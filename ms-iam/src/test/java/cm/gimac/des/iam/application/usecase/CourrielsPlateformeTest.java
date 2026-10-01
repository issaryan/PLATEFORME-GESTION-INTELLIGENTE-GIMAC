package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Courriel;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gabarits des courriels de la plateforme (D-65) : langue du compte (fr, en, es), français à défaut, HTML échappé. */
class CourrielsPlateformeTest {

    static Compte compte(String langue, String prenom) {
        return new Compte("id", "agent", prenom, "<Mballa>", "awa@gimac.test", true, true, null, null, null, langue, null, true, Set.of(), List.of(), true);
    }

    @Test
    @DisplayName("Langue du compte, français pour une langue absente ou inconnue ; durées en heures ou en minutes")
    void langues() {
        Courriel en = CourrielsPlateforme.composer(compte("en", "Awa"), CourrielsPlateforme.Modele.INVITATION, "https://des.gimac.test/x", Duration.ofHours(24));
        assertThat(en.objet()).isEqualTo("Your account on the GIMAC DES platform");
        assertThat(en.texte()).contains("24 hours").contains("Hello Awa <Mballa>,");
        assertThat(en.html()).contains("lang=\"en\"").contains("&lt;Mballa&gt;").doesNotContain("<Mballa>");
        Courriel es = CourrielsPlateforme.composer(compte("es", "Awa"), CourrielsPlateforme.Modele.REINITIALISATION, "https://des.gimac.test/x", Duration.ofMinutes(90));
        assertThat(es.texte()).contains("90 minutos");
        assertThat(CourrielsPlateforme.composer(compte(null, null), CourrielsPlateforme.Modele.ACTIONS, "https://des.gimac.test/connexion", null).objet()).isEqualTo("Actions demandées sur votre compte");
        assertThat(CourrielsPlateforme.composer(compte("de", "Awa"), CourrielsPlateforme.Modele.ACTIONS, "l", null).html()).contains("lang=\"fr\"");
        assertThat(CourrielsPlateforme.langue("es")).isEqualTo("es");
    }
}
