package cm.gimac.des.gateway.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-133 : règles du contexte d'action d'un compte à double rôle. */
class ContexteActionTest {

    @Test
    @DisplayName("Lecture de l'en-tête : absent ou vide, INTEGRATION ; casse indifférente ; inconnu refusé")
    void lecture() {
        assertThat(ContexteAction.lire(null)).contains(ContexteAction.INTEGRATION);
        assertThat(ContexteAction.lire("  ")).contains(ContexteAction.INTEGRATION);
        assertThat(ContexteAction.lire(" administration ")).contains(ContexteAction.ADMINISTRATION);
        assertThat(ContexteAction.lire("ROOT")).isEmpty();
    }

    @Test
    @DisplayName("Double rôle : administration de l'institution et action d'un autre module")
    void doubleRole() {
        assertThat(ContexteAction.doubleRole(List.of("SBX_HAB_ADMINISTRATION", "SBX_RAC_ADMINISTRATION"))).isTrue();
        assertThat(ContexteAction.doubleRole(List.of("SBX_HAB_ADMINISTRATION", "SBX_RAC_CONSULTATION", "SBX_HAB_CONSULTATION"))).isFalse();
        assertThat(ContexteAction.doubleRole(List.of("SBX_RAC_ADMINISTRATION", "SBX_SIM_SIMULATION"))).isFalse();
        assertThat(ContexteAction.doubleRole(List.of("SBX_HAB_ADMINISTRATION", "NOT_INBOX_READ"))).isFalse();
    }

    @Test
    @DisplayName("Réduction : Administration, HAB et consultations ; Intégration, tout sauf l'administration de l'institution")
    void reduction() {
        List<String> p = List.of("SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "SBX_RAC_ADMINISTRATION", "SBX_CER_CONSULTATION");
        assertThat(ContexteAction.ADMINISTRATION.reduire(p)).containsExactly("SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "SBX_CER_CONSULTATION");
        assertThat(ContexteAction.INTEGRATION.reduire(p)).containsExactly("SBX_HAB_CONSULTATION", "SBX_RAC_ADMINISTRATION", "SBX_CER_CONSULTATION");
    }
}
