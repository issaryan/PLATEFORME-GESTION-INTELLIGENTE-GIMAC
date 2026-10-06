package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.JetonLien;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Administration des comptes (D-62, D-65) sur l'annuaire de substitution : règles, journal, invitations par courriel de la plateforme. */
@ExtendWith(MockitoExtension.class)
class AdministrationComptesServiceTest {

    static final Set<String> CATALOGUE = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI", "ROLE_CHEF_DDRM", "ROLE_CHARGE_DDRM", "ROLE_CORRESPONDANT");
    static final Set<String> PRIVILEGIES = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI");
    static final Acteur SUPER = new Acteur("demo-super-admin", "Super", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
    static final Acteur RSSI = new Acteur("demo-rssi", "RSSI", Set.of());

    @Mock
    JournalActes journal;

    @Mock
    JournalAdministrationRepository entrees;

    AnnuaireEnMemoire annuaire;
    AdministrationComptesService service;
    CourrielsEnMemoire courriels;
    List<JetonLien> jetons;

    static IamProprietes proprietes() {
        return new IamProprietes(
            new IamProprietes.Keycloak("http://kc", "gimac", "iam-service", "s"),
            new IamProprietes.PremiereConnexion(List.of(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR, ActionRequise.FICHE, ActionRequise.CHARTE)),
            new IamProprietes.Profils(CATALOGUE, PRIVILEGIES),
            new IamProprietes.Webauthn("localhost", "GIMAC", List.of("http://localhost"), Duration.ofMinutes(2)),
            new IamProprietes.Liens("https://des.gimac.test", "https://portail.gimac.test", "https://console.gimac.test/", null, null, null),
            new IamProprietes.Connexion("gimac-connexion", "s", null),
            null
        );
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(CATALOGUE), PRIVILEGIES);
        courriels = new CourrielsEnMemoire();
        jetons = new ArrayList<>();
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZoneOffset.UTC);
        LiensCourriel liens = new LiensCourriel(JetonsEnMemoire.creer(jetons), new CourrielsPlateforme(courriels), proprietes(), clock);
        service = new AdministrationComptesService(annuaire, journal, entrees, proprietes(), liens, clock);
    }

    NouveauCompte nouveau(String id, String... profils) {
        return new NouveauCompte(id, "Prénom", "Nom", id + "@gimac.test", "+237600000000", "Chargé", null, Set.of(profils));
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Création par le RSSI : actions de premier accès, invitation, journal ; mot de passe provisoire rendu une fois")
    void creation() {
        var c = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false);
        assertThat(c.compte().actionsRequises()).containsExactly(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR, ActionRequise.FICHE, ActionRequise.CHARTE);
        assertThat(c.compte().premierAccesEnAttente()).isTrue();
        assertThat(c.invitationEnvoyee()).isTrue();
        assertThat(c.motDePasseProvisoire()).isNull();
        // D-65 : courriel de la plateforme, lien vers le département, jeton de 24 h conservé haché.
        var courriel = courriels.dernierPour("agent.ddrm@gimac.test").orElseThrow();
        assertThat(courriel.objet()).isEqualTo("Votre compte sur la plateforme Moabi du GIMAC");
        assertThat(courriel.texte()).contains("https://des.gimac.test/connexion/nouveau-mot-de-passe?jeton=").contains("24 heures").contains("agent.ddrm");
        assertThat(courriel.html()).contains("Choisir mon mot de passe");
        String jeton = JetonsEnMemoire.jeton(courriel.texte()).orElseThrow();
        assertThat(jetons).singleElement().satisfies(j -> {
            assertThat(j.getEmpreinte()).hasSize(64).isNotEqualTo(jeton);
            assertThat(j.getFinalite()).isEqualTo("INVITATION");
            assertThat(j.getExpireLe()).isEqualTo(Instant.parse("2026-09-30T08:00:00Z"));
        });
        verify(journal).compte(eq(RSSI), eq("CREATION"), eq(c.compte().id()), eq("agent.ddrm"), isNull(), anyList());
        verify(journal).compte(eq(RSSI), eq("INVITATION"), eq(c.compte().id()), eq("agent.ddrm"), isNull(), eq(List.of("DES")));

        var p = service.creer(RSSI, nouveau("agent.deux", "ROLE_CHARGE_DDRM"), false, true);
        assertThat(p.motDePasseProvisoire()).startsWith("Provisoire-");
        assertThat(p.compte().actionsRequises()).contains(ActionRequise.MOT_DE_PASSE);
        assertThat(p.invitationEnvoyee()).isFalse();
        // Super administrateur invité sur la console ; correspondant sur le portail.
        service.creer(SUPER, nouveau("admin.deux", "ROLE_SUPER_ADMIN"), true, false);
        assertThat(courriels.dernierPour("admin.deux@gimac.test").orElseThrow().texte()).contains("https://console.gimac.test/connexion/");
        service.creer(RSSI, new NouveauCompte("corr.a", "C", "A", "corr.a@banque.test", null, null, "INST-9", Set.of("ROLE_CORRESPONDANT")), true, false);
        assertThat(courriels.dernierPour("corr.a@banque.test").orElseThrow().texte()).contains("https://portail.gimac.test/connexion/");
    }

    @Test
    @DisplayName("Création refusée : identifiant invalide, courriel absent, doublon, aucun mode d'accès, profil privilégié par le RSSI, institution incohérente")
    void creationRefusee() {
        refus(() -> service.creer(RSSI, nouveau("Majuscules", "ROLE_CHARGE_DDRM"), true, false), "IAM_USERNAME_INVALID");
        refus(() -> service.creer(RSSI, new NouveauCompte("sans.courriel", "a", "b", " ", null, null, null, Set.of("ROLE_CHARGE_DDRM")), true, false), "IAM_EMAIL_REQUIRED");
        refus(() -> service.creer(RSSI, nouveau("rssi", "ROLE_CHARGE_DDRM"), true, false), "IAM_ACCOUNT_CONFLICT");
        refus(() -> service.creer(RSSI, new NouveauCompte("autre", "a", "b", "rssi@gimac.test", null, null, null, Set.of("ROLE_CHARGE_DDRM")), true, false), "IAM_ACCOUNT_CONFLICT");
        refus(() -> service.creer(RSSI, nouveau("sans.acces", "ROLE_CHARGE_DDRM"), false, false), "IAM_ONBOARDING_REQUIRED");
        refus(() -> service.creer(RSSI, nouveau("futur.admin", "ROLE_SUPER_ADMIN"), true, false), "IAM_PRIVILEGED_PROFILE");
        // Agent du GIMAC saisi avec une institution (ex. « GIMAC ») : il serait traité en compte externe.
        refus(() -> service.creer(RSSI, new NouveauCompte("agent.systeme", "a", "b", "systeme@gimac.test", null, null, "GIMAC", Set.of("ROLE_CHARGE_DDRM")), true, false), "IAM_INSTITUTION_INTERNAL_ACCOUNT");
        refus(() -> service.creer(RSSI, new NouveauCompte("corr.sans", "a", "b", "corr.sans@banque.test", null, null, null, Set.of("ROLE_CORRESPONDANT")), true, false), "IAM_INSTITUTION_REQUIRED");
        assertThat(service.creer(SUPER, nouveau("futur.admin", "ROLE_SUPER_ADMIN"), true, false).compte().profils()).containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("Liste filtrée et paginée ; indicateurs du tableau de bord")
    void listeEtIndicateurs() {
        String id = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        assertThat(service.lister(new AdministrationComptesService.Filtre("agent", null, null), 0, 25).total()).isEqualTo(1);
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, "ROLE_RSSI", null), 0, 25).contenu()).extracting(Compte::identifiant).containsExactly("rssi");
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "PREMIER_ACCES"), 0, 25).total()).isEqualTo(1);
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "SANS_2FA"), 0, 25).total()).isEqualTo(1);
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "ACTIF"), 1, 2).contenu()).hasSize(1);
        service.definirActif(RSSI, id, false, "départ");
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "INACTIF"), 0, 25).total()).isEqualTo(1);
        refus(() -> service.lister(new AdministrationComptesService.Filtre(null, null, "BIDON"), 0, 25), "IAM_FILTER_INVALID");
        when(entrees.countByHorodatageAfter(any())).thenReturn(4L);
        var i = service.indicateurs();
        assertThat(i.total()).isEqualTo(3);
        assertThat(i.actifs()).isEqualTo(2);
        assertThat(i.inactifs()).isEqualTo(1);
        assertThat(i.privilegies()).isEqualTo(2);
        assertThat(i.actesDepuis24Heures()).isEqualTo(4);
        assertThat(i.parProfil()).containsEntry("ROLE_RSSI", 1L);
    }

    @Test
    @DisplayName("Séries quotidiennes : 30 jours denses au fuseau de Douala, jours vides à zéro, hors fenêtre ignorés")
    void seriesQuotidiennes() {
        // 2026-09-28T23:30Z est déjà le 29 septembre à Douala (UTC+1) ; le 1er août sort de la fenêtre.
        when(entrees.horodatagesDepuis(any())).thenReturn(List.of(
            Instant.parse("2026-09-28T23:30:00Z"), Instant.parse("2026-09-29T07:00:00Z"), Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-08-01T10:00:00Z")));
        var i = service.indicateurs();
        assertThat(i.actesParJour()).hasSize(AdministrationComptesService.JOURS_SERIES);
        assertThat(i.actesParJour().get(0).jour()).isEqualTo(java.time.LocalDate.parse("2026-08-31"));
        assertThat(i.actesParJour().get(29)).isEqualTo(new AdministrationComptesService.ParJour(java.time.LocalDate.parse("2026-09-29"), 2));
        assertThat(i.actesParJour().get(1).nombre()).isEqualTo(1);
        assertThat(i.actesParJour().stream().mapToLong(AdministrationComptesService.ParJour::nombre).sum()).isEqualTo(3);
        assertThat(i.creationsParJour()).hasSize(AdministrationComptesService.JOURS_SERIES);
    }

    @Test
    @DisplayName("Fiche, profils, activation, suppression, avec journal et motif")
    void cycleDeVie() {
        String id = service.creer(SUPER, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        assertThat(service.modifier(RSSI, id, new FicheCompte("Jean", null, "jean@gimac.test", null, "Chef", null, null)).email()).isEqualTo("jean@gimac.test");
        refus(() -> service.modifier(RSSI, id, new FicheCompte(null, null, "rssi@gimac.test", null, null, null, null)), "IAM_ACCOUNT_CONFLICT");
        refus(() -> service.modifier(RSSI, id, new FicheCompte(null, null, null, "xx", null, null, null)), "IAM_PHONE_INVALID");
        assertThat(service.definirProfils(RSSI, id, Set.of("ROLE_CHEF_DDRM"), "promotion").profils()).containsExactly("ROLE_CHEF_DDRM");
        refus(() -> service.definirProfils(RSSI, id, Set.of("ROLE_CHEF_DDRM"), " "), "IAM_REASON_REQUIRED");
        refus(() -> service.definirProfils(RSSI, "demo-rssi", Set.of(), "moi"), "IAM_SELF_ADMINISTRATION");
        assertThat(service.definirActif(RSSI, id, false, "départ").actif()).isFalse();
        assertThat(service.definirActif(RSSI, id, true, "retour").actif()).isTrue();
        service.supprimer(SUPER, id, "doublon");
        refus(() -> service.lire(id), "IAM_ACCOUNT_NOT_FOUND");
        verify(journal).compte(eq(SUPER), eq("SUPPRESSION"), eq(id), eq("agent.ddrm"), eq("doublon"), anyList());
    }

    @Test
    @DisplayName("Le dernier super administrateur actif ne peut être désactivé, supprimé ni perdre son profil")
    void dernierSuperAdmin() {
        refus(() -> service.definirActif(RSSI, "demo-super-admin", false, "x"), "IAM_LAST_SUPER_ADMIN");
        refus(() -> service.supprimer(RSSI, "demo-super-admin", "x"), "IAM_LAST_SUPER_ADMIN");
        Acteur autreSuper = new Acteur("autre", "Autre", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
        refus(() -> service.definirProfils(autreSuper, "demo-super-admin", Set.of("ROLE_RSSI"), "x"), "IAM_LAST_SUPER_ADMIN");
        service.creer(SUPER, nouveau("second.admin", "ROLE_SUPER_ADMIN"), true, false);
        assertThat(service.definirActif(RSSI, "demo-super-admin", false, "rotation").actif()).isFalse();
    }

    @Test
    @DisplayName("Actions de sécurité : actions requises (courriel), second facteur, mot de passe provisoire, sessions")
    void securite() {
        String id = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        annuaire.ouvrirSession(id, "10.0.0.1");
        assertThat(service.sessions(id)).hasSize(1);
        assertThat(service.exigerActions(RSSI, id, Set.of(ActionRequise.VERIFIER_COURRIEL), true, "courriel changé").actionsRequises()).contains(ActionRequise.VERIFIER_COURRIEL);
        // Nouveau lien : l'invitation précédente n'est plus utilisable.
        assertThat(courriels.envoyes()).hasSize(2);
        assertThat(jetons).hasSize(2);
        assertThat(jetons.get(0).getUtiliseLe()).isNotNull();
        assertThat(jetons.get(1).getUtiliseLe()).isNull();
        // Rappel sans jeton quand ni le mot de passe ni le courriel ne sont en cause.
        service.exigerActions(RSSI, id, Set.of(ActionRequise.FICHE), true, "fiche à revoir");
        assertThat(courriels.envoyes()).hasSize(3);
        assertThat(courriels.envoyes().get(2).texte()).contains("https://des.gimac.test/connexion").doesNotContain("jeton=");
        assertThat(jetons).hasSize(2);
        refus(() -> service.exigerActions(RSSI, id, Set.of(), false, "x"), "IAM_ACTIONS_REQUIRED");
        assertThat(service.exigerActions(RSSI, id, Set.of(ActionRequise.MOT_DE_PASSE), false, "rotation").actionsRequises()).contains(ActionRequise.MOT_DE_PASSE);
        annuaire.enregistrerTotp(id, "JBSWY3DPEHPK3PXP", cm.gimac.des.iam.domain.service.Totp.code("JBSWY3DPEHPK3PXP", Instant.now(), 6, 30, "HmacSHA1"), null);
        annuaire.retirerActions(id, Set.of(ActionRequise.SECOND_FACTEUR));
        var reinitialise = service.reinitialiserSecondFacteur(RSSI, id, "perte du téléphone");
        assertThat(reinitialise.secondFacteur()).isFalse();
        assertThat(reinitialise.actionsRequises()).contains(ActionRequise.SECOND_FACTEUR);
        assertThat(service.sessions(id)).isEmpty();
        annuaire.retirerActions(id, Set.of(ActionRequise.MOT_DE_PASSE));
        String provisoire = service.motDePasseProvisoire(RSSI, id, "oubli");
        assertThat(provisoire).isNotBlank();
        assertThat(annuaire.verifierMotDePasse("agent.ddrm", provisoire)).isTrue();
        assertThat(service.lire(id).actionsRequises()).contains(ActionRequise.MOT_DE_PASSE);
        annuaire.ouvrirSession(id, "10.0.0.2");
        service.fermerSessions(RSSI, id, "incident");
        assertThat(service.sessions(id)).isEmpty();
        verify(journal, never()).compte(eq(RSSI), eq("SUPPRESSION"), any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("D-65 : messagerie en panne → création maintenue, invitation non envoyée et tracée ; exigence par courriel refusée (503)")
    void messagerieEnPanne() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZoneOffset.UTC);
        cm.gimac.des.iam.application.port.out.ServiceCourriel enPanne = c -> {
            throw IamException.indisponible("IAM_SMTP_FAILED", "refus");
        };
        LiensCourriel liens = new LiensCourriel(JetonsEnMemoire.creer(jetons), new CourrielsPlateforme(enPanne), proprietes(), clock);
        service = new AdministrationComptesService(annuaire, journal, entrees, proprietes(), liens, clock);
        var c = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false);
        assertThat(c.invitationEnvoyee()).isFalse();
        assertThat(service.lire(c.compte().id())).isNotNull();
        verify(journal).compte(eq(RSSI), eq("INVITATION_ECHEC"), eq(c.compte().id()), eq("agent.ddrm"), isNull(), eq(List.of("IAM_SMTP_FAILED")));
        refus(() -> service.exigerActions(RSSI, c.compte().id(), Set.of(ActionRequise.MOT_DE_PASSE), true, "rotation"), "IAM_SMTP_FAILED");
    }

    @Test
    @DisplayName("D-64 : annuaire interne, comptes actifs sans institution, filtrés par nom, courriel ou fonction, paginés")
    void annuaireInterne() {
        String agent = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        service.creer(RSSI, new NouveauCompte("correspondant", "Jeanne", "Owona", "jeanne@banque.test", "+237600000003", "Correspondante", "INST-1", Set.of("ROLE_CORRESPONDANT")), true, false);
        String parti = service.creer(RSSI, nouveau("ancien.agent", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        service.definirActif(RSSI, parti, false, "départ");

        var tous = service.annuaireInterne(null, 0, 20);
        assertThat(tous.total()).isEqualTo(3);
        assertThat(tous.contenu()).extracting(Compte::id).containsExactlyInAnyOrder("demo-super-admin", "demo-rssi", agent);
        assertThat(service.annuaireInterne(" RSSI ", 0, 20).contenu()).extracting(Compte::id).containsExactly("demo-rssi");
        assertThat(service.annuaireInterne("agent.ddrm@gimac", 0, 20).contenu()).extracting(Compte::id).containsExactly(agent);
        assertThat(service.annuaireInterne("chargé", 0, 20).contenu()).extracting(Compte::id).containsExactly(agent);
        assertThat(service.annuaireInterne("owona", 0, 20).total()).isZero();
        var page = service.annuaireInterne("", 1, 2);
        assertThat(page.contenu()).hasSize(1);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.taille()).isEqualTo(2);
        var bornee = service.annuaireInterne(null, -3, 500);
        assertThat(bornee.page()).isZero();
        assertThat(bornee.taille()).isEqualTo(100);
        assertThat(service.annuaireInterne(null, 9, 0).contenu()).isEmpty();
    }
}
