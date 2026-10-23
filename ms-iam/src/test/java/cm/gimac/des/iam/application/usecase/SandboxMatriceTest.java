package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Matrice de la Sandbox GIMAC dans MS-IAM (D-115, US-EXI-02) : permissions SBX filtrées par préfixe, attribuées sans
 * redéploiement, validation des verdicts refusée à tout profil externe par les trois chemins (création, recomposition,
 * passage en externe), refus journalisé nulle part et sans effet.
 */
@ExtendWith(MockitoExtension.class)
class SandboxMatriceTest {

    static final Instant T = Instant.parse("2026-10-19T08:00:00Z");
    static final Acteur ADMIN = new Acteur(
        "admin",
        "Admin",
        Set.of("IAM_ACCOUNT_READ", "IAM_PROFILE_EDIT", "IAM_ROLE_PRIVILEGED_GRANT")
    );

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire crees;
    CatalogueProfils catalogue;
    ProfilsService profils;
    ConfigurationAuthentificationService configuration;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SUPER_ADMIN", "ROLE_CHARGE_DDRM", "ROLE_CORRESPONDANT")));
        crees = new DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire();
        catalogue = new CatalogueProfils(AdministrationComptesServiceTest.proprietes(), crees, annuaire);
        Clock horloge = Clock.fixed(T, ZoneOffset.UTC);
        profils = new ProfilsService(annuaire, crees, catalogue, new PouvoirsServiceTest.MatriceEnMemoire(), journal, horloge);
        configuration = new ConfigurationAuthentificationService(annuaire, journal, null, null, horloge, catalogue, crees);
    }

    private static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("GET /permissions?prefixe=SBX : les soixante et une permissions de la sandbox (treize modules, SUP en consultation seule), et elles seules")
    void prefixe() {
        List<String> sbx = configuration.permissions("sbx");
        assertThat(sbx).hasSize(61).allMatch(p -> p.startsWith("SBX_")).contains("SBX_MOB_EXECUTION", "SBX_CER_VALIDATION", "SBX_LEC_CONSULTATION", "SBX_SUP_CONSULTATION")
            .doesNotContain("SBX_SUP_ADMINISTRATION");
        assertThat(configuration.permissions(" ")).isEqualTo(configuration.permissions()).contains("PAR_INSTITUTION_READ");
        assertThat(configuration.permissions(null)).containsAll(sbx);
    }

    @Test
    @DisplayName("Profil interne : la validation des verdicts est attribuée, sans redéploiement, et journalisée")
    void interneAutorise() {
        var p = configuration.definirPermissions(ADMIN, "ROLE_CHARGE_DDRM", Set.of("SBX_CER_VALIDATION", "SBX_CER_CONSULTATION"), "Certificateur de la sandbox");
        assertThat(p.permissions()).contains("SBX_CER_VALIDATION", "SBX_CER_CONSULTATION");
        assertThat(annuaire.compositions().get("ROLE_CHARGE_DDRM")).contains("SBX_CER_VALIDATION");
        verify(journal).configuration(eq(ADMIN), eq("PROFIL_ROLE_CHARGE_DDRM"), eq("Certificateur de la sandbox"), any());
        var cree = profils.creer(ADMIN, "ROLE_SBX_AUDIT", "Audit de la sandbox", "Audit", Set.of("SBX_CER_VALIDATION"), "Création");
        assertThat(cree.permissions()).contains("SBX_CER_VALIDATION");
    }

    @Test
    @DisplayName("Profil externe recomposé avec une validation de verdict : 422 IAM_SBX_VALIDATION_RESERVEE, rien n'est écrit")
    void recompositionExterneRefusee() {
        Set<String> avant = Set.copyOf(annuaire.compositions().get("ROLE_CORRESPONDANT"));
        assertThatThrownBy(() -> configuration.definirPermissions(ADMIN, "ROLE_CORRESPONDANT", Set.of("SBX_MOB_EXECUTION", "SBX_CER_VALIDATION"), "x"))
            .isInstanceOfSatisfying(IamException.class, e -> {
                assertThat(e.code()).isEqualTo("IAM_SBX_VALIDATION_RESERVEE");
                assertThat(e.status()).isEqualTo(422);
                assertThat(e.details()).isEqualTo(Map.of("permissions", List.of("SBX_CER_VALIDATION")));
            });
        assertThat(annuaire.compositions().get("ROLE_CORRESPONDANT")).isEqualTo(avant);
        verify(journal, never()).configuration(any(), any(), any(), any());
        // Les autres fonctions restent attribuables à un profil externe.
        assertThat(configuration.definirPermissions(ADMIN, "ROLE_CORRESPONDANT", Set.of("SBX_MOB_EXECUTION"), "Testeur").permissions()).contains("SBX_MOB_EXECUTION");
    }

    @Test
    @DisplayName("Profil créé externe (repli *_PORTAL_*) avec une validation de verdict : 422, aucun profil créé")
    void creationExterneRefusee() {
        refus(() -> profils.creer(ADMIN, "ROLE_SBX_FAUX", "Faux", "x", Set.of("HOM_PORTAL_READ", "SBX_SIM_VALIDATION"), "x"), "IAM_SBX_VALIDATION_RESERVEE", 422);
        assertThat(catalogue.attribuables()).doesNotContain("ROLE_SBX_FAUX");
        assertThat(crees.lire("ROLE_SBX_FAUX")).isEmpty();
    }

    @Test
    @DisplayName("Passage en externe d'un profil qui valide les verdicts : 422 ; sans validation, accepté")
    void passageExterne() {
        profils.creer(ADMIN, "ROLE_SBX_VALIDEUR", "Valideur", "Valide", Set.of("SBX_CAR_VALIDATION"), "x");
        refus(() -> profils.modifier(ADMIN, "ROLE_SBX_VALIDEUR", "Valideur", "Valide", true, "Ouvert au portail"), "IAM_SBX_VALIDATION_RESERVEE", 422);
        assertThat(catalogue.externes()).doesNotContain("ROLE_SBX_VALIDEUR");
        profils.creer(ADMIN, "ROLE_SBX_LECTURE", "Lecture", "Lit", Set.of("SBX_CAR_CONSULTATION"), "x");
        assertThat(profils.modifier(ADMIN, "ROLE_SBX_LECTURE", "Lecture", "Lit", true, "Ouvert au portail").externe()).isTrue();
        // Inchangé ou retour en interne : jamais refusé.
        assertThat(profils.modifier(ADMIN, "ROLE_SBX_VALIDEUR", "Valideur", "Valide", false, "x").externe()).isFalse();
    }
}
