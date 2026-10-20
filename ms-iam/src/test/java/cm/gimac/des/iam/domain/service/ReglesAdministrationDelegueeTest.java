package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles de l'administration déléguée de la sandbox (D-115, US-EXI-03, RG-15) : cas limites. */
class ReglesAdministrationDelegueeTest {

    static Compte compte(String institution) {
        return new Compte("c1", "awa", "Awa", "Ndiaye", "a@b.test", true, true, Instant.EPOCH, null, null, "fr", institution, true, Set.of(), List.of(), true, null, null);
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Institution de l'acteur : nulle ou blanche refusée, sinon nettoyée")
    void institution() {
        refus(() -> ReglesAdministrationDeleguee.institution(null), "IAM_SBX_SANS_INSTITUTION", 403);
        refus(() -> ReglesAdministrationDeleguee.institution("  "), "IAM_SBX_SANS_INSTITUTION", 403);
        assertThat(ReglesAdministrationDeleguee.institution(" 99001 ")).isEqualTo("99001");
    }

    @Test
    @DisplayName("Périmètre : compte absent, d'une autre institution ou interne : même 404")
    void perimetre() {
        refus(() -> ReglesAdministrationDeleguee.dansPerimetre(null, "99001", "x"), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> ReglesAdministrationDeleguee.dansPerimetre(compte("99002"), "99001", "c1"), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> ReglesAdministrationDeleguee.dansPerimetre(compte(null), "99001", "c1"), "IAM_ACCOUNT_NOT_FOUND", 404);
        assertThat(ReglesAdministrationDeleguee.dansPerimetre(compte("99001"), "99001", "c1").id()).isEqualTo("c1");
    }

    @Test
    @DisplayName("Rôles : délégables par composition ; requis à la création ; nul admis ailleurs ; hors portée détaillé")
    void roles() {
        Map<String, Set<String>> compositions = Map.of("ROLE_SBX_TESTEUR", Set.of("SBX_MOB_EXECUTION"), "ROLE_CORRESPONDANT", Set.of("HOM_PORTAL_READ"));
        Set<String> delegables = ReglesAdministrationDeleguee.rolesDelegables(Set.of("ROLE_SBX_TESTEUR", "ROLE_CORRESPONDANT", "ROLE_INCONNU"), compositions);
        assertThat(delegables).containsExactly("ROLE_SBX_TESTEUR");
        refus(() -> ReglesAdministrationDeleguee.verifierRoles(null, delegables, true), "IAM_SBX_ROLE_REQUIS", 400);
        refus(() -> ReglesAdministrationDeleguee.verifierRoles(Set.of(), delegables, true), "IAM_SBX_ROLE_REQUIS", 400);
        assertThatCode(() -> ReglesAdministrationDeleguee.verifierRoles(null, delegables, false)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministrationDeleguee.verifierRoles(Set.of(), delegables, false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglesAdministrationDeleguee.verifierRoles(Set.of("ROLE_CORRESPONDANT", "ROLE_SBX_TESTEUR"), delegables, false))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.details()).containsEntry("profils", List.of("ROLE_CORRESPONDANT")));
        assertThat(ReglesAdministrationDeleguee.profilsApres(Set.of("ROLE_CORRESPONDANT", "ROLE_SBX_TESTEUR"), Set.of(), delegables)).containsExactly("ROLE_CORRESPONDANT");
    }

    @Test
    @DisplayName("Dernier administrateur : refusé seul, admis à plusieurs ou sans perte")
    void dernier() {
        refus(() -> ReglesAdministrationDeleguee.verifierDernierAdministrateur(true, 1), "IAM_SBX_DERNIER_ADMINISTRATEUR", 409);
        refus(() -> ReglesAdministrationDeleguee.verifierDernierAdministrateur(true, 0), "IAM_SBX_DERNIER_ADMINISTRATEUR", 409);
        assertThatCode(() -> ReglesAdministrationDeleguee.verifierDernierAdministrateur(true, 2)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministrationDeleguee.verifierDernierAdministrateur(false, 1)).doesNotThrowAnyException();
        assertThat(ReglesAdministrationDeleguee.administrateur(Set.of("ROLE_X"), Map.of())).isFalse();
    }
}
