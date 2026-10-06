package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-62 : séparation des pouvoirs, catalogue des profils, dernier super administrateur. */
class ReglesAdministrationTest {

    static final Set<String> CATALOGUE = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI", "ROLE_CHEF_DDRM");
    static final Set<String> PRIVILEGIES = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI");
    static final Acteur RSSI = new Acteur("rssi", "RSSI", Set.of("IAM_ACCOUNT_ROLES"));
    static final Acteur SUPER = new Acteur("sa", "Super", Set.of("IAM_ACCOUNT_ROLES", "IAM_ROLE_PRIVILEGED_GRANT"));

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Un administrateur n'agit pas sur son propre compte")
    void pasSoiMeme() {
        refus(() -> ReglesAdministration.verifierPasSoiMeme(RSSI, "rssi"), "IAM_SELF_ADMINISTRATION");
        assertThatCode(() -> ReglesAdministration.verifierPasSoiMeme(RSSI, "autre")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Identifiant : minuscules, 3 à 50 caractères ; téléphone : chiffres, espaces, +")
    void identifiantEtTelephone() {
        assertThatCode(() -> ReglesAdministration.verifierIdentifiant("jean.dupont")).doesNotThrowAnyException();
        refus(() -> ReglesAdministration.verifierIdentifiant("Jean"), "IAM_USERNAME_INVALID");
        refus(() -> ReglesAdministration.verifierIdentifiant(null), "IAM_USERNAME_INVALID");
        assertThatCode(() -> ReglesAdministration.verifierTelephone("+237 690 00 00 00")).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierTelephone(null)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierTelephone(" ")).doesNotThrowAnyException();
        refus(() -> ReglesAdministration.verifierTelephone("abc"), "IAM_PHONE_INVALID");
    }

    @Test
    @DisplayName("Profils : catalogue exigé ; profil privilégié réservé au super administrateur")
    void profils() {
        refus(() -> ReglesAdministration.verifierProfils(SUPER, Set.of(), Set.of("ROLE_INCONNU"), CATALOGUE, PRIVILEGIES), "IAM_PROFILE_UNKNOWN");
        refus(() -> ReglesAdministration.verifierProfils(RSSI, Set.of(), Set.of("ROLE_RSSI"), CATALOGUE, PRIVILEGIES), "IAM_PRIVILEGED_PROFILE");
        refus(() -> ReglesAdministration.verifierProfils(RSSI, Set.of("ROLE_SUPER_ADMIN"), Set.of(), CATALOGUE, PRIVILEGIES), "IAM_PRIVILEGED_PROFILE");
        assertThatCode(() -> ReglesAdministration.verifierProfils(RSSI, Set.of("ROLE_RSSI"), Set.of("ROLE_RSSI", "ROLE_CHEF_DDRM"), CATALOGUE, PRIVILEGIES)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierProfils(SUPER, Set.of(), Set.of("ROLE_SUPER_ADMIN"), CATALOGUE, PRIVILEGIES)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Le dernier super administrateur actif ne peut être retiré ; motif obligatoire")
    void dernierSuperAdminEtMotif() {
        refus(() -> ReglesAdministration.verifierDernierSuperAdmin(true, 1), "IAM_LAST_SUPER_ADMIN");
        assertThatCode(() -> ReglesAdministration.verifierDernierSuperAdmin(true, 2)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierDernierSuperAdmin(false, 1)).doesNotThrowAnyException();
        refus(() -> ReglesAdministration.exigerMotif(" "), "IAM_REASON_REQUIRED");
        refus(() -> ReglesAdministration.exigerMotif(null), "IAM_REASON_REQUIRED");
    }

    @Test
    @DisplayName("Institution : réservée au correspondant de participant, et obligatoire pour lui")
    void rattachement() {
        assertThatCode(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_POLE_SYSTEME"), null)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_POLE_SYSTEME"), " ")).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_CORRESPONDANT"), "INST-0042")).doesNotThrowAnyException();
        assertThatCode(() -> ReglesAdministration.verifierRattachement(Set.of(), null)).doesNotThrowAnyException();
        // Agent du GIMAC saisi avec l'institution « GIMAC » : il serait traité en compte externe.
        refus(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_POLE_SYSTEME"), "GIMAC"), "IAM_INSTITUTION_INTERNAL_ACCOUNT");
        refus(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_CORRESPONDANT", "ROLE_DG"), "INST-0042"), "IAM_INSTITUTION_INTERNAL_ACCOUNT");
        refus(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_CORRESPONDANT"), null), "IAM_INSTITUTION_REQUIRED");
        refus(() -> ReglesAdministration.verifierRattachement(Set.of("ROLE_CORRESPONDANT"), ""), "IAM_INSTITUTION_REQUIRED");
    }
}
