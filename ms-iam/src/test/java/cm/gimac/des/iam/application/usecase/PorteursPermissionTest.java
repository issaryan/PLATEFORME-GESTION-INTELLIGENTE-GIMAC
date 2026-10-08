package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Porteurs d'une permission (D-94 §3) : composition lue à chaque appel, comptes internes actifs seulement. */
class PorteursPermissionTest {

    AnnuaireEnMemoire annuaire;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(
            Map.of(
                "ROLE_SUPER_ADMIN", Set.of("IAM_ACCOUNT_READ"),
                "ROLE_RSSI", Set.of("IAM_ACCOUNT_READ"),
                "ROLE_A", Set.of("HOM_PROJECT_CONDUCT", "COL_MESSAGE_READ"),
                "ROLE_B", Set.of("COL_MESSAGE_READ"),
                "ROLE_PORTAIL", Set.of("HOM_PROJECT_CONDUCT", "HOM_PORTAL_READ")
            )
        );
    }

    private String creer(String identifiant, String prenom, String institution, String... profils) {
        return annuaire.creer(new NouveauCompte(identifiant, prenom, "Ngono", identifiant + "@gimac.test", null, null, institution, Set.of(profils)), List.of());
    }

    @Test
    @DisplayName("Comptes internes actifs dont un profil compose la permission, triés par nom, avec leurs profils")
    void porteurs() {
        String zoe = creer("zoe", "Zoé", null, "ROLE_A");
        String aminata = creer("aminata", "Aminata", null, "ROLE_A", "ROLE_B");
        creer("bertin", "Bertin", null, "ROLE_B");
        creer("correspondant", "Carine", "INST-0042", "ROLE_PORTAIL");
        String parti = creer("parti", "Paul", null, "ROLE_A");
        annuaire.definirActif(parti, false);

        List<Compte> l = PorteursPermission.lire(annuaire, "HOM_PROJECT_CONDUCT");
        assertThat(l).extracting(Compte::id).containsExactly(aminata, zoe);
        assertThat(l.get(0).profils()).containsExactlyInAnyOrder("ROLE_A", "ROLE_B");
        assertThat(PorteursPermission.lire(annuaire, "PERMISSION_INCONNUE")).isEmpty();
        assertThat(PorteursPermission.lire(annuaire, " ")).isEmpty();
        assertThat(PorteursPermission.lire(annuaire, null)).isEmpty();
    }

    @Test
    @DisplayName("Recomposition et profil créé dans la console : pris en compte dès l'appel suivant")
    void recompositionEtProfilCree() {
        String bertin = creer("bertin", "Bertin", null, "ROLE_B");
        assertThat(PorteursPermission.lire(annuaire, "HOM_PROJECT_CONDUCT")).isEmpty();
        annuaire.definirPermissionsProfil("ROLE_B", Set.of("COL_MESSAGE_READ", "HOM_PROJECT_CONDUCT"));
        assertThat(PorteursPermission.lire(annuaire, "HOM_PROJECT_CONDUCT")).extracting(Compte::id).containsExactly(bertin);

        annuaire.creerProfil("ROLE_CELLULE_ESSAI", "Cellule", "Profil créé", Set.of("HOM_ESSAI_PILOT"));
        String carine = creer("carine", "Carine", null, "ROLE_CELLULE_ESSAI");
        assertThat(PorteursPermission.lire(annuaire, "HOM_ESSAI_PILOT")).extracting(Compte::id).containsExactly(carine);
    }

    @Test
    @DisplayName("Règle pure : profils dont la composition porte la permission")
    void profilsPortant() {
        Map<String, Set<String>> compositions = Map.of("ROLE_A", Set.of("X", "Y"), "ROLE_B", Set.of("Y"), "ROLE_C", Set.of());
        assertThat(ReglesProfils.profilsPortant("Y", compositions)).containsExactly("ROLE_A", "ROLE_B");
        assertThat(ReglesProfils.profilsPortant("X", compositions)).containsExactly("ROLE_A");
        assertThat(ReglesProfils.profilsPortant("", compositions)).isEmpty();
    }
}
