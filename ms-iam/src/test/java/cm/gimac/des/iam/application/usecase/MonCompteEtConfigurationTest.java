package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.CleSecuriteRepository;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** « Mon compte » (premier accès, libre-service) et configuration de l'authentification (D-62). */
@ExtendWith(MockitoExtension.class)
class MonCompteEtConfigurationTest {

    static final Acteur SUPER = new Acteur("demo-super-admin", "Super", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
    static final Acteur RSSI = new Acteur("demo-rssi", "RSSI", Set.of());

    @Mock
    JournalActes journal;

    @Mock
    CleSecuriteRepository cles;

    AnnuaireEnMemoire annuaire;
    MonCompteService moi;
    ConfigurationAuthentificationService configuration;
    String nouveau;
    Acteur titulaire;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(
            Map.of("ROLE_SUPER_ADMIN", Set.of("IAM_ACCOUNT_READ"), "ROLE_RSSI", Set.of("IAM_ACCOUNT_READ"), "ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ")),
            Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI")
        );
        moi = new MonCompteService(annuaire, journal, cles);
        configuration = new ConfigurationAuthentificationService(annuaire, journal, AdministrationComptesServiceTest.proprietes());
        nouveau = annuaire.creer(
            new cm.gimac.des.iam.domain.compte.NouveauCompte("agent", "A", "B", "agent@gimac.test", null, null, null, Set.of("ROLE_CHARGE_DDRM")),
            java.util.List.of(ActionRequise.UPDATE_PROFILE)
        );
        titulaire = new Acteur(nouveau, "Agent", Set.of());
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Premier accès : étapes restantes, puis fiche complète après la première configuration")
    void premierAcces() {
        when(cles.countBySujet(nouveau)).thenReturn(0L);
        assertThat(moi.moi(titulaire).etapesRestantes()).containsExactly("UPDATE_PROFILE", "FICHE", "CONFIGURE_TOTP");
        refus(() -> moi.premiereConfiguration(titulaire, "+237600000000", "fr", null, false), "IAM_CHARTER_REQUIRED");
        refus(() -> moi.premiereConfiguration(titulaire, " ", "fr", null, true), "IAM_PHONE_REQUIRED");
        refus(() -> moi.premiereConfiguration(titulaire, "+237600000000", "de", null, true), "IAM_LANGUAGE_INVALID");
        var m = moi.premiereConfiguration(titulaire, "+237600000000", "en", "Chargé DDRM", true);
        assertThat(m.compte().profilComplet()).isTrue();
        assertThat(m.compte().langue()).isEqualTo("en");
        assertThat(m.etapesRestantes()).doesNotContain("FICHE");
        verify(journal).compte(eq(titulaire), eq("PREMIERE_CONFIGURATION"), eq(nouveau), eq("agent"), isNull(), anyList());
    }

    @Test
    @DisplayName("Libre-service : coordonnées, mot de passe, second facteur, sessions")
    void libreService() {
        assertThat(moi.modifier(titulaire, "Alice", null, "+237 699 99 99 99", null, "es").compte().prenom()).isEqualTo("Alice");
        refus(() -> moi.modifier(titulaire, null, null, null, null, "xx"), "IAM_LANGUAGE_INVALID");
        assertThat(moi.demander(titulaire, ActionRequise.UPDATE_PASSWORD).compte().actionsRequises()).contains(ActionRequise.UPDATE_PASSWORD);
        assertThat(moi.demander(titulaire, ActionRequise.CONFIGURE_TOTP).compte().secondFacteur()).isFalse();
        refus(() -> moi.demander(titulaire, ActionRequise.VERIFY_EMAIL), "IAM_ACTION_NOT_ALLOWED");
        String s = annuaire.ouvrirSession(nouveau, "10.1.1.1");
        annuaire.ouvrirSession(nouveau, "10.1.1.2");
        assertThat(moi.sessions(titulaire)).hasSize(2);
        moi.fermerSession(titulaire, s);
        refus(() -> moi.fermerSession(titulaire, s), "IAM_SESSION_NOT_FOUND");
        moi.fermerToutesSessions(titulaire);
        assertThat(moi.sessions(titulaire)).isEmpty();
        refus(() -> moi.moi(new Acteur("inconnu", "x", Set.of())), "IAM_ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("Politique de sécurité : appliquée avec motif et différences journalisées ; refusée sous les planchers")
    void politique() {
        PolitiqueSecurite p = new PolitiqueSecurite(14, 1, 1, 1, 1, 6, 60, true, 6, 30, "HmacSHA256", 20, 8, true, 5, 30);
        assertThat(configuration.appliquerPolitique(RSSI, p, "revue annuelle")).isEqualTo(p);
        verify(journal).configuration(eq(RSSI), eq("POLITIQUE_SECURITE"), eq("revue annuelle"), anyList());
        assertThat(ConfigurationAuthentificationService.differences(p, p)).isEmpty();
        assertThat(configuration.politique().longueurMinimale()).isEqualTo(14);
        refus(() -> configuration.appliquerPolitique(RSSI, p, ""), "IAM_REASON_REQUIRED");
        PolitiqueSecurite faible = new PolitiqueSecurite(6, 0, 0, 0, 0, 0, 0, false, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
        refus(() -> configuration.appliquerPolitique(RSSI, faible, "x"), "IAM_POLICY_INVALID");
    }

    @Test
    @DisplayName("Messagerie : lue sans secret, appliquée, testée ; saisies invalides refusées")
    void messagerie() {
        ConfigurationMessagerie m = new ConfigurationMessagerie("smtp.gimac.cm", 587, "des@gimac.cm", "DES", true, false, true, "des", "secret", false);
        ConfigurationMessagerie lue = configuration.appliquerMessagerie(SUPER, m, "mise en service");
        assertThat(lue.motDePasse()).isNull();
        assertThat(lue.motDePasseDefini()).isTrue();
        configuration.testerMessagerie("rssi@gimac.cm");
        refus(() -> configuration.testerMessagerie("pas-une-adresse"), "IAM_SMTP_INVALID");
        refus(() -> configuration.appliquerMessagerie(SUPER, new ConfigurationMessagerie("", 0, "x", null, false, false, false, null, null, false), "x"), "IAM_SMTP_INVALID");
        refus(() -> configuration.appliquerMessagerie(SUPER, new ConfigurationMessagerie("h", 25, "a@b.cm", null, false, false, true, " ", null, false), "x"), "IAM_SMTP_INVALID");
        annuaire.appliquerMessagerie(new ConfigurationMessagerie(" ", 25, "a@b.cm", null, false, false, false, null, null, false));
        refus(() -> configuration.testerMessagerie("rssi@gimac.cm"), "IAM_SMTP_FAILED");
    }

    @Test
    @DisplayName("Profils : composition des permissions, privilégiés réservés au super administrateur, permissions inconnues refusées")
    void profils() {
        assertThat(configuration.profils()).extracting("code").contains("ROLE_CHARGE_DDRM", "ROLE_RSSI");
        assertThat(configuration.permissions()).contains("IAM_ACCOUNT_READ", "PAR_INSTITUTION_READ");
        assertThat(configuration.definirPermissions(RSSI, "ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ", "IAM_ACCOUNT_READ"), "revue").permissions()).hasSize(2);
        refus(() -> configuration.definirPermissions(RSSI, "ROLE_RSSI", Set.of(), "x"), "IAM_PRIVILEGED_PROFILE");
        refus(() -> configuration.definirPermissions(SUPER, "ROLE_INCONNU", Set.of(), "x"), "IAM_PROFILE_NOT_FOUND");
        refus(() -> configuration.definirPermissions(SUPER, "ROLE_RSSI", Set.of("PERM_INEXISTANTE"), "x"), "IAM_PERMISSION_UNKNOWN");
        assertThat(configuration.definirPermissions(SUPER, "ROLE_RSSI", Set.of("IAM_ACCOUNT_READ"), "x").privilegie()).isTrue();
    }
}
