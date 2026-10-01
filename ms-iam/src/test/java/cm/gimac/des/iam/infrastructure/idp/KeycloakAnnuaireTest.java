package cm.gimac.des.iam.infrastructure.idp;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Conversions de l'adaptateur Keycloak (D-65), sans serveur : refus de la politique, actions, JSON. */
class KeycloakAnnuaireTest {

    @Test
    @DisplayName("Refus de Keycloak ramenés aux codes et violations de la plateforme")
    void refusMotDePasse() {
        var longueur = KeycloakAnnuaire.refusMotDePasse("{\"error\":\"invalidPasswordMinLengthMessage\",\"error_description\":\"x\"}");
        assertThat(longueur.code()).isEqualTo("IAM_PASSWORD_POLICY");
        assertThat(longueur.details()).containsEntry("violations", List.of("LONGUEUR"));
        assertThat(KeycloakAnnuaire.refusMotDePasse("{\"error\": \"invalidPasswordHistoryMessage\"}").code()).isEqualTo("IAM_PASSWORD_REUSED");
        assertThat(KeycloakAnnuaire.refusMotDePasse("{\"error\":\"invalidPasswordBlacklistedMessage\"}").details()).containsEntry("violations", List.of("invalidPasswordBlacklistedMessage"));
        assertThat(KeycloakAnnuaire.refusMotDePasse("{\"error\":\"autre\"}").code()).isEqualTo("IAM_IDP_REJECTED");
        assertThat(KeycloakAnnuaire.refusMotDePasse(null).code()).isEqualTo("IAM_IDP_REJECTED");
    }

    @Test
    @DisplayName("Actions lues dans l'attribut gimac_actions_requises (anciens noms admis, inconnues ignorées) ; JSON échappé")
    void conversions() {
        assertThat(KeycloakAnnuaire.actions(Map.of("gimac_actions_requises", List.of("CHARTE", "UPDATE_PASSWORD", "INCONNUE")))).containsExactly(ActionRequise.MOT_DE_PASSE, ActionRequise.CHARTE);
        assertThat(KeycloakAnnuaire.actions(Map.of())).isEmpty();
        assertThat(KeycloakAnnuaire.json("a\"b\\c\n")).isEqualTo("a\\\"b\\\\c\\u000a");
    }
}
