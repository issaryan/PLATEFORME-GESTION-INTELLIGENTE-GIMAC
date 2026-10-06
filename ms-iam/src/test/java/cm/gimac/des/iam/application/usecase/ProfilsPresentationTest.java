package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.port.out.PresentationsProfils;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D-86 §2 (docs/lot1/profils-organisation.md) : libellé et description des profils livrés et créés,
 * règles (motif, bornes, profil privilégié, code inconnu), journal (avant, après), trace de la dernière
 * modification, libellés dans la matrice et les pouvoirs, réapplication au démarrage après un réimport.
 */
@ExtendWith(MockitoExtension.class)
class ProfilsPresentationTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");
    static final Acteur SUPER = new Acteur("super", "Super Admin", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
    static final Acteur RSSI = new Acteur("rssi", "Carine RSSI", Set.of());

    /** Traces en mémoire (table profil_presentation). */
    static final class PresentationsEnMemoire implements PresentationsProfils {

        final Map<String, PresentationProfil> traces = new TreeMap<>();

        @Override
        public Map<String, PresentationProfil> toutes() {
            return new TreeMap<>(traces);
        }

        @Override
        public Optional<PresentationProfil> lire(String code) {
            return Optional.ofNullable(traces.get(code));
        }

        @Override
        public void fixer(String code, String libelle, String description, Instant le, String parId, String parNom) {
            traces.put(code, new PresentationProfil(code, libelle, description, le, parId, parNom));
        }

        @Override
        public void toucher(String code, Instant le, String parId, String parNom) {
            PresentationProfil p = traces.get(code);
            traces.put(code, new PresentationProfil(code, p == null ? null : p.libelle(), p == null ? null : p.description(), le, parId, parNom));
        }

        @Override
        public void supprimer(String code) {
            traces.remove(code);
        }
    }

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire crees;
    PresentationsEnMemoire presentations;
    PouvoirsServiceTest.MatriceEnMemoire matrice;
    CatalogueProfils catalogue;
    ProfilsService service;
    ConfigurationAuthentificationService configuration;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(AdministrationComptesServiceTest.CATALOGUE), AdministrationComptesServiceTest.PRIVILEGIES);
        crees = new DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire();
        presentations = new PresentationsEnMemoire();
        matrice = new PouvoirsServiceTest.MatriceEnMemoire();
        catalogue = new CatalogueProfils(AdministrationComptesServiceTest.proprietes(), crees);
        Clock horloge = Clock.fixed(T, ZoneOffset.UTC);
        service = new ProfilsService(annuaire, crees, catalogue, matrice, journal, horloge, presentations);
        configuration = new ConfigurationAuthentificationService(annuaire, journal, null, null, horloge, catalogue, crees, presentations);
    }

    @Test
    @DisplayName("Profil livré : libellé et description modifiés, code inchangé, journal avant/après, trace (date, auteur) dans la liste")
    void profilLivre() {
        assertThat(configuration.profils()).filteredOn(p -> p.code().equals("ROLE_CHEF_DDRM")).singleElement().satisfies(p -> {
            assertThat(p.libelle()).isEqualTo("Chef de la DDRM");
            assertThat(p.modifieLe()).isNull();
            assertThat(p.modifiePar()).isNull();
        });
        Profil p = service.modifier(RSSI, "ROLE_CHEF_DDRM", "  Direction des relations membres ", " Prononce les décisions. ", " Réorganisation ");
        assertThat(p.code()).isEqualTo("ROLE_CHEF_DDRM");
        assertThat(p.libelle()).isEqualTo("Direction des relations membres");
        assertThat(p.description()).isEqualTo("Prononce les décisions.");
        assertThat(p.cree()).isFalse();
        assertThat(p.modifieLe()).isEqualTo(T);
        assertThat(p.modifiePar()).isEqualTo(new Profil.Auteur("rssi", "Carine RSSI"));
        verify(journal).profil(eq(RSSI), eq("ROLE_CHEF_DDRM"), eq("Réorganisation"), eq("libelle=Chef de la DDRM ; description="), eq("libelle=Direction des relations membres ; description=Prononce les décisions."));
        assertThat(annuaire.presentationsProfils().get("ROLE_CHEF_DDRM").libelle()).isEqualTo("Direction des relations membres");
        assertThat(configuration.profils()).filteredOn(x -> x.code().equals("ROLE_CHEF_DDRM")).singleElement().satisfies(x -> {
            assertThat(x.libelle()).isEqualTo("Direction des relations membres");
            assertThat(x.modifieLe()).isEqualTo(T);
            assertThat(x.modifiePar().nomAffiche()).isEqualTo("Carine RSSI");
        });
        // Description vidée : permise ; recomposition : la trace change d'auteur, le libellé reste.
        assertThat(service.modifier(RSSI, "ROLE_CHEF_DDRM", "Chef DDRM", null, "Simplification").description()).isEmpty();
        configuration.definirPermissions(SUPER, "ROLE_CHEF_DDRM", Set.of("PAR_INSTITUTION_READ"), "Revue");
        assertThat(presentations.traces.get("ROLE_CHEF_DDRM")).satisfies(t -> {
            assertThat(t.libelle()).isEqualTo("Chef DDRM");
            assertThat(t.modifieParId()).isEqualTo("super");
        });
    }

    @Test
    @DisplayName("Refus : motif absent 400, libellé vide ou trop long 400, description trop longue 400, profil inconnu 404, privilégié sans octroi 403")
    void refus() {
        DemandesEtProfilsServiceTest.refus(() -> service.modifier(RSSI, "ROLE_CHEF_DDRM", "Chef", "", " "), "IAM_REASON_REQUIRED", 400);
        DemandesEtProfilsServiceTest.refus(() -> service.modifier(RSSI, "ROLE_CHEF_DDRM", "  ", "", "m"), "IAM_PROFIL_INVALIDE", 400);
        DemandesEtProfilsServiceTest.refus(() -> service.modifier(RSSI, "ROLE_CHEF_DDRM", "x".repeat(81), "", "m"), "IAM_PROFIL_INVALIDE", 400);
        DemandesEtProfilsServiceTest.refus(() -> service.modifier(RSSI, "ROLE_CHEF_DDRM", "Chef", "d".repeat(501), "m"), "IAM_PROFIL_INVALIDE", 400);
        DemandesEtProfilsServiceTest.refus(() -> service.modifier(RSSI, "ROLE_INCONNU", "Chef", "", "m"), "IAM_PROFILE_NOT_FOUND", 404);
        DemandesEtProfilsServiceTest.refus(() -> service.modifier(RSSI, "ROLE_SUPER_ADMIN", "Super", "", "m"), "IAM_PRIVILEGED_PROFILE", 403);
        assertThat(service.modifier(SUPER, "ROLE_SUPER_ADMIN", "x".repeat(80), "d".repeat(500), "m").libelle()).hasSize(80);
        verify(journal, never()).profil(eq(RSSI), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Profil créé : libellé à la création (absent : le code), modification, description tenue aussi dans profil_cree, trace supprimée avec le profil")
    void profilCree() {
        Profil p = service.creer(SUPER, "ROLE_QUALITE", " Cellule qualité ", "Qualité", Set.of("HOM_DASHBOARD_READ"), null);
        assertThat(p.libelle()).isEqualTo("Cellule qualité");
        assertThat(p.modifieLe()).isEqualTo(T);
        assertThat(service.creer(SUPER, "ROLE_SANS_LIBELLE", null, "Sans libellé", Set.of(), null).libelle()).isEqualTo("ROLE_SANS_LIBELLE");
        DemandesEtProfilsServiceTest.refus(() -> service.creer(SUPER, "ROLE_LONG", "x".repeat(81), "d", Set.of(), null), "IAM_PROFIL_INVALIDE", 400);
        DemandesEtProfilsServiceTest.refus(() -> service.creer(SUPER, "ROLE_LONG", "Long", "d".repeat(501), Set.of(), null), "IAM_PROFIL_INVALIDE", 400);
        assertThat(service.creer(SUPER, "ROLE_LONGUE", "Longue", "d".repeat(500), Set.of(), null).description()).hasSize(500);

        Profil m = service.modifier(RSSI, "ROLE_QUALITE", "Qualité et méthodes", "Méthodes", "Extension");
        assertThat(m.cree()).isTrue();
        assertThat(m.libelle()).isEqualTo("Qualité et méthodes");
        assertThat(crees.lire("ROLE_QUALITE").orElseThrow().description()).isEqualTo("Méthodes");
        service.supprimer(SUPER, "ROLE_QUALITE", null);
        assertThat(presentations.traces).doesNotContainKey("ROLE_QUALITE");
    }

    @Test
    @DisplayName("Matrice et pouvoirs : libellé de chaque profil (repli : le code), libellés des profils demandables ; actions des processus")
    void libellesDansLesPouvoirs() {
        OrganisationServiceTest.CatalogueEnMemoire unites = new OrganisationServiceTest.CatalogueEnMemoire();
        OrganisationService organisation = new OrganisationService(unites, annuaire, journal);
        PouvoirsService pouvoirs = new PouvoirsService(matrice, catalogue, annuaire, organisation, journal);
        service.modifier(SUPER, "ROLE_CHARGE_DDRM", "Instructeur DDRM", "", "Renommage");
        Map<ActionPouvoir, Portee> a = new java.util.EnumMap<>(ActionPouvoir.class);
        a.put(ActionPouvoir.PROCESSUS_DEFINIR, Portee.UNITES);
        a.put(ActionPouvoir.PROCESSUS_LANCER, Portee.UNITES);
        matrice.enregistrer(new PouvoirsProfil("ROLE_CHEF_DDRM", a, Set.of("ROLE_CHARGE_DDRM")));

        var m = pouvoirs.matrice();
        assertThat(m.actions()).endsWith(ActionPouvoir.PROCESSUS_DEFINIR, ActionPouvoir.PROCESSUS_LANCER);
        assertThat(m.profils()).filteredOn(l -> l.profil().equals("ROLE_CHARGE_DDRM")).singleElement().extracting(PouvoirsService.LigneMatrice::libelle).isEqualTo("Instructeur DDRM");
        assertThat(m.profils()).filteredOn(l -> l.profil().equals("ROLE_CHEF_DDRM")).singleElement().satisfies(l -> {
            assertThat(l.libelle()).isEqualTo("Chef de la DDRM");
            assertThat(l.actions()).containsEntry(ActionPouvoir.PROCESSUS_DEFINIR, Portee.UNITES).containsEntry(ActionPouvoir.PROCESSUS_LANCER, Portee.UNITES);
        });

        String chef = annuaire.creer(new cm.gimac.des.iam.domain.compte.NouveauCompte("chef.ddrm", "Chef", "Ddrm", "chef@gimac.test", null, "Chef", null, Set.of("ROLE_CHEF_DDRM")), java.util.List.of());
        var vue = pouvoirs.vue(chef);
        assertThat(vue.actions()).containsEntry(ActionPouvoir.PROCESSUS_LANCER, Portee.UNITES);
        assertThat(vue.libellesProfils()).containsExactly(Map.entry("ROLE_CHARGE_DDRM", "Instructeur DDRM"));
    }

    @Test
    @DisplayName("Démarrage : un libellé fixé par l'administration et écrasé par un réimport est réappliqué ; idempotent ; profil disparu ignoré")
    void restauration() {
        RestaurationPresentationsProfils restauration = new RestaurationPresentationsProfils(annuaire, presentations);
        assertThat(restauration.restaurer()).isZero();
        service.modifier(SUPER, "ROLE_CHARGE_DDRM", "Instructeur DDRM", "Instruit les dossiers.", "Renommage");
        configuration.definirPermissions(SUPER, "ROLE_CHEF_DDRM", Set.of("PAR_INSTITUTION_READ"), "Revue");
        presentations.fixer("ROLE_DISPARU", "Disparu", "", T, "super", "Super Admin");
        assertThat(restauration.restaurer()).isZero();
        // Réimport du domaine : la valeur du dépôt revient.
        annuaire.definirPresentationProfil("ROLE_CHARGE_DDRM", "Chargé de la DDRM", "Mission du dépôt");
        assertThat(restauration.restaurer()).isEqualTo(1);
        assertThat(annuaire.presentationsProfils().get("ROLE_CHARGE_DDRM")).satisfies(p -> {
            assertThat(p.libelle()).isEqualTo("Instructeur DDRM");
            assertThat(p.description()).isEqualTo("Instruit les dossiers.");
        });
        // La seule recomposition ne fixe ni libellé ni description.
        assertThat(annuaire.presentationsProfils().get("ROLE_CHEF_DDRM").libelle()).isEqualTo("Chef de la DDRM");
        assertThat(restauration.restaurer()).isZero();
        restauration.auDemarrage();
    }
}
