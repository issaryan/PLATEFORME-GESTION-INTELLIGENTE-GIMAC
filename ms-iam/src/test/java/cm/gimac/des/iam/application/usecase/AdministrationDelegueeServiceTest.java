package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.application.usecase.sandbox.Memoires;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.JetonLien;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Administration déléguée des comptes d'une institution (D-115, US-EXI-03, RG-15) sur l'annuaire de substitution :
 * création bornée à l'institution, rôles délégables seulement, réponse indistinguable hors de l'institution, jamais sur
 * soi-même (tentative journalisée), dernier administrateur protégé, étapes de saga rejouées sans double effet.
 */
@ExtendWith(MockitoExtension.class)
class AdministrationDelegueeServiceTest {

    static final String INST = "99001";
    static final String AUTRE = "99002";
    static final Map<String, Set<String>> CATALOGUE = new LinkedHashMap<>(
        Map.of(
            "ROLE_SUPER_ADMIN",
            Set.of("IAM_ACCOUNT_READ", "SBX_HAB_ADMINISTRATION"),
            "ROLE_CORRESPONDANT",
            Set.of("HOM_PORTAL_READ", "NOT_INBOX_READ"),
            "ROLE_SBX_ADMIN_INSTITUTION",
            Set.of("SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "NOT_INBOX_READ"),
            "ROLE_SBX_TESTEUR",
            Set.of("SBX_MOB_EXECUTION", "SBX_MOB_CONSULTATION", "NOT_INBOX_READ"),
            "ROLE_SBX_LECTEUR",
            Set.of("SBX_CER_CONSULTATION", "NOT_INBOX_READ")
        )
    );

    @Mock
    JournalActes journal;

    @Mock
    JournalAdministrationRepository entrees;

    AnnuaireEnMemoire annuaire;
    AdministrationDelegueeService service;
    final Map<String, String> etapes = new LinkedHashMap<>();
    final Memoires.Protections protections = new Memoires.Protections();
    String admin;
    String admin2;
    String testeur;
    String etranger;
    Acteur acteur;

    static IamProprietes proprietes() {
        return new IamProprietes(
            new IamProprietes.Keycloak("http://kc", "gimac", "iam-service", "s"),
            new IamProprietes.PremiereConnexion(List.of(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR)),
            new IamProprietes.Profils(CATALOGUE.keySet(), Set.of("ROLE_CORRESPONDANT", "ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR", "ROLE_SBX_LECTEUR")),
            new IamProprietes.Webauthn("localhost", "GIMAC", List.of("http://localhost"), Duration.ofMinutes(2)),
            new IamProprietes.Liens("https://des.gimac.test", "https://portail.gimac.test", "https://console.gimac.test/", null, null, null, null),
            new IamProprietes.Connexion("gimac-connexion", "s", null),
            null
        );
    }

    String compte(String identifiant, String institution, String... profils) {
        return annuaire.creer(
            new NouveauCompte(identifiant, "Prénom", identifiant, identifiant + "@institution.test", null, "Fonction", institution, Set.of(profils)),
            List.of()
        );
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(CATALOGUE);
        for (String p : List.of("ROLE_CORRESPONDANT", "ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR", "ROLE_SBX_LECTEUR")) {
            annuaire.definirExterne(p, true);
        }
        Clock clock = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZoneOffset.UTC);
        List<JetonLien> jetons = new ArrayList<>();
        LiensCourriel liens = new LiensCourriel(
            JetonsEnMemoire.creer(jetons),
            new CourrielsPlateforme(new cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire()),
            proprietes(),
            clock
        );
        CatalogueProfils catalogue = new CatalogueProfils(proprietes(), new DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire(), annuaire);
        AdministrationComptesService comptes = new AdministrationComptesService(annuaire, journal, entrees, proprietes(), liens, clock, catalogue);
        EtapesIdempotentes memoire = new EtapesIdempotentes() {
            @Override
            public Optional<String> resultat(String cle, String operation) {
                return Optional.ofNullable(etapes.get(cle + "/" + operation));
            }

            @Override
            public void enregistrer(String cle, String operation, String resultat, String acteur) {
                etapes.put(cle + "/" + operation, resultat);
            }
        };
        service = new AdministrationDelegueeService(comptes, catalogue, annuaire, journal, memoire, protections);
        admin = compte("admin.99001", INST, "ROLE_SBX_ADMIN_INSTITUTION");
        testeur = compte("testeur.99001", INST, "ROLE_SBX_TESTEUR", "ROLE_CORRESPONDANT");
        etranger = compte("testeur.99002", AUTRE, "ROLE_SBX_TESTEUR");
        acteur = new Acteur(admin, "Administrateur 99001", Set.of("SBX_HAB_ADMINISTRATION"));
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    AdministrationDelegueeService.NouveauCollaborateur collaborateur(String identifiant, String... roles) {
        return new AdministrationDelegueeService.NouveauCollaborateur(identifiant, "Awa", "Ndiaye", identifiant + "@institution.test", null, "Testeuse", Set.of(roles));
    }

    @Test
    @DisplayName("Rôles délégables : profils externes portant une permission de la sandbox ; ni correspondant ni profil interne")
    void rolesDelegables() {
        assertThat(service.roles()).containsExactly("ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_LECTEUR", "ROLE_SBX_TESTEUR");
    }

    @Test
    @DisplayName("Nominal : création d'un compte de testeur rattaché à l'institution, journalisée ; rejouée : même compte, rien de plus")
    void creation() {
        var c = service.creer(acteur, INST, collaborateur("awa.ndiaye", "ROLE_SBX_TESTEUR"), "saga-1/etape-2");
        assertThat(c.rejouee()).isFalse();
        assertThat(c.compte().institutionId()).isEqualTo(INST);
        assertThat(c.compte().profils()).containsExactly("ROLE_SBX_TESTEUR");
        verify(journal).compte(eq(acteur), eq("CREATION"), eq(c.compte().id()), eq("awa.ndiaye"), any(), eq(List.of("ROLE_SBX_TESTEUR")));
        int avant = annuaire.lister().size();
        var r = service.creer(acteur, INST, collaborateur("awa.ndiaye", "ROLE_SBX_TESTEUR"), "saga-1/etape-2");
        assertThat(r.rejouee()).isTrue();
        assertThat(r.compte().id()).isEqualTo(c.compte().id());
        assertThat(annuaire.lister()).hasSize(avant);
        // Sans clé : une seconde création du même identifiant est un conflit ordinaire.
        refus(() -> service.creer(acteur, INST, collaborateur("awa.ndiaye", "ROLE_SBX_TESTEUR"), null), "IAM_ACCOUNT_CONFLICT", 409);
    }

    @Test
    @DisplayName("Création refusée : sans rôle, rôle hors portée (correspondant, profil interne), sans institution")
    void creationRefusee() {
        refus(() -> service.creer(acteur, INST, collaborateur("x.y"), null), "IAM_SBX_ROLE_REQUIS", 400);
        refus(() -> service.creer(acteur, INST, collaborateur("x.y", "ROLE_CORRESPONDANT"), null), "IAM_SBX_ROLE_HORS_PORTEE", 403);
        refus(() -> service.creer(acteur, INST, collaborateur("x.y", "ROLE_SUPER_ADMIN"), null), "IAM_SBX_ROLE_HORS_PORTEE", 403);
        refus(() -> service.creer(acteur, " ", collaborateur("x.y", "ROLE_SBX_TESTEUR"), null), "IAM_SBX_SANS_INSTITUTION", 403);
        refus(() -> service.lister(null, 0, 20), "IAM_SBX_SANS_INSTITUTION", 403);
        assertThat(annuaire.parIdentifiant("x.y")).isEmpty();
    }

    @Test
    @DisplayName("Rôles : la sandbox remplacée, le correspondant gardé ; rejeu sans effet ; autre institution indistinguable d'un inexistant")
    void roles() {
        var c = service.definirRoles(acteur, INST, testeur, Set.of("ROLE_SBX_LECTEUR"), "Changement de mission", "saga-2/etape-1");
        assertThat(c.profils()).containsExactlyInAnyOrder("ROLE_SBX_LECTEUR", "ROLE_CORRESPONDANT");
        annuaire.definirProfils(testeur, Set.of("ROLE_SBX_TESTEUR", "ROLE_CORRESPONDANT"));
        assertThat(service.definirRoles(acteur, INST, testeur, Set.of("ROLE_SBX_LECTEUR"), "Changement de mission", "saga-2/etape-1").profils()).contains("ROLE_SBX_TESTEUR");
        refus(() -> service.definirRoles(acteur, INST, etranger, Set.of("ROLE_SBX_LECTEUR"), "x", null), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> service.definirRoles(acteur, INST, "inexistant", Set.of("ROLE_SBX_LECTEUR"), "x", null), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> service.lire(INST, etranger), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> service.definirRoles(acteur, INST, testeur, Set.of("ROLE_SUPER_ADMIN"), "x", null), "IAM_SBX_ROLE_HORS_PORTEE", 403);
        assertThat(annuaire.lire(etranger).orElseThrow().profils()).containsExactly("ROLE_SBX_TESTEUR");
    }

    @Test
    @DisplayName("Erreur : rôle attribué à soi-même, refusé sans effet et journalisé")
    void soiMeme() {
        refus(() -> service.definirRoles(acteur, INST, admin, Set.of("ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR"), "x", null), "IAM_SELF_ADMINISTRATION", 403);
        verify(journal).compte(eq(acteur), eq("TENTATIVE_AUTO_ATTRIBUTION"), eq(admin), any(), any(), eq(List.of("IAM_SELF_ADMINISTRATION")));
        assertThat(annuaire.lire(admin).orElseThrow().profils()).containsExactly("ROLE_SBX_ADMIN_INSTITUTION");
        refus(() -> service.definirActif(acteur, INST, admin, false, "x", null), "IAM_SELF_ADMINISTRATION", 403);
        verify(journal).compte(eq(acteur), eq("TENTATIVE_AUTO_SUSPENSION"), eq(admin), any(), any(), any());
    }

    @Test
    @DisplayName("RG-15 : le dernier administrateur actif n'est ni suspendu ni privé de son rôle ; avec un second, si")
    void dernierAdministrateur() {
        admin2 = compte("admin2.99001", INST, "ROLE_SBX_ADMIN_INSTITUTION");
        Acteur autreAdmin = new Acteur(admin2, "Second", Set.of("SBX_HAB_ADMINISTRATION"));
        // admin2 suspend admin : il reste un administrateur actif (admin2).
        assertThat(service.definirActif(autreAdmin, INST, admin, false, "Départ", null).actif()).isFalse();
        // admin, suspendu, ne compte plus : admin2 est le dernier ; acteur tiers de l'institution (chef désigné).
        Acteur tiers = new Acteur("tiers", "Tiers", Set.of("SBX_HAB_ADMINISTRATION"));
        refus(() -> service.definirActif(tiers, INST, admin2, false, "x", null), "IAM_SBX_DERNIER_ADMINISTRATEUR", 409);
        refus(() -> service.definirRoles(tiers, INST, admin2, Set.of("ROLE_SBX_LECTEUR"), "x", null), "IAM_SBX_DERNIER_ADMINISTRATEUR", 409);
        // Rétablissement : admin redevient actif, admin2 peut alors perdre son rôle.
        assertThat(service.definirActif(autreAdmin, INST, admin, true, "Retour", "saga-3/etape-1").actif()).isTrue();
        assertThat(service.definirActif(autreAdmin, INST, admin, true, "Retour", "saga-3/etape-1").actif()).isTrue();
        assertThat(service.definirRoles(tiers, INST, admin2, Set.of("ROLE_SBX_LECTEUR"), "Changement", null).profils()).containsExactly("ROLE_SBX_LECTEUR");
        verify(journal, never()).compte(any(), eq("TENTATIVE_AUTO_SUSPENSION"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Liste : comptes de l'institution, suspendus compris, triés, paginés ; jamais ceux d'une autre institution")
    void liste() {
        annuaire.definirActif(testeur, false);
        var p = service.lister(INST, 0, 1);
        assertThat(p.total()).isEqualTo(2);
        assertThat(p.contenu()).hasSize(1);
        assertThat(p.taille()).isEqualTo(1);
        var tous = service.lister(INST, 0, 500);
        assertThat(tous.taille()).isEqualTo(100);
        assertThat(tous.contenu()).extracting(c -> c.id()).containsExactlyInAnyOrder(admin, testeur).doesNotContain(etranger);
        assertThat(service.lister(INST, -3, 0).page()).isZero();
    }

    @Test
    @DisplayName("Clé blanche : pas de rejeu ni de mémoire ; rétablissement d'un compte non administrateur ; administrateur suspendu sans perte")
    void clesEtCasLimites() {
        var c = service.creer(acteur, INST, collaborateur("sans.cle", "ROLE_SBX_LECTEUR"), "  ");
        assertThat(c.rejouee()).isFalse();
        assertThat(etapes).isEmpty();
        assertThat(service.definirActif(acteur, INST, testeur, false, "Absence", " ").actif()).isFalse();
        assertThat(service.definirActif(acteur, INST, testeur, true, "Retour", null).actif()).isTrue();
        assertThat(service.definirRoles(acteur, INST, testeur, Set.of("ROLE_SBX_LECTEUR"), "x", " ").profils()).contains("ROLE_SBX_LECTEUR");
        // Administrateur déjà suspendu : le suspendre à nouveau ne retire aucun administrateur actif.
        admin2 = compte("admin2.99001", INST, "ROLE_SBX_ADMIN_INSTITUTION");
        annuaire.definirActif(admin2, false);
        assertThat(service.definirActif(acteur, INST, admin2, false, "x", null).actif()).isFalse();
        // Administrateur suspendu privé de son rôle : pas de dernier administrateur à protéger.
        assertThat(service.definirRoles(acteur, INST, admin2, Set.of("ROLE_SBX_LECTEUR"), "x", null).profils()).containsExactly("ROLE_SBX_LECTEUR");
        assertThat(etapes).isEmpty();
    }

    @Test
    @DisplayName("US-EXI-06 scénario 3 : la suspension du compte protégé, demandée par l'institution, est refusée avec motif et journalisée")
    void suspensionCompteProtegeRefusee() {
        String admin2 = compte("admin2.99001", INST, "ROLE_SBX_ADMIN_INSTITUTION");
        protections.definir(admin2, INST, true, "service-account-sbx-espace", Instant.parse("2026-10-19T08:00:00Z"));
        assertThatThrownBy(() -> service.definirActif(acteur, INST, admin2, false, "départ", null)).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_SBX_COMPTE_PROTEGE");
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.details()).containsEntry("motif", cm.gimac.des.iam.domain.sandbox.ReglesPerimetre.MOTIF_PROTECTION);
        });
        assertThat(annuaire.lire(admin2).orElseThrow().actif()).isTrue();
        verify(journal).compte(eq(acteur), eq("TENTATIVE_SUSPENSION_COMPTE_PROTEGE"), eq(admin2), any(), eq("départ"), any());
        // Rétablir un compte protégé reste possible.
        assertThat(service.definirActif(acteur, INST, admin2, true, "retour", null).actif()).isTrue();
    }

    @Test
    @DisplayName("US-EXI-23 scénario 3 : déblocage demandé par l'institution, refusé et journalisé ; compte étranger : 404 sans trace")
    void deblocageRefuse() {
        refus(() -> service.refuserDeblocage(acteur, INST, testeur, "Le testeur est bloqué"), "IAM_SBX_DEBLOCAGE_RESERVE", 403);
        verify(journal).compte(eq(acteur), eq("TENTATIVE_DEBLOCAGE"), eq(testeur), eq("testeur.99001"), eq("Le testeur est bloqué"), eq(List.of(INST)));
        refus(() -> service.refuserDeblocage(acteur, INST, etranger, null), "IAM_ACCOUNT_NOT_FOUND", 404);
        verify(journal, never()).compte(any(), eq("TENTATIVE_DEBLOCAGE"), eq(etranger), any(), any(), any());
    }
}
