package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.port.out.MatricePouvoirs;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D-81 §2 et §3 : responsable d'une unité, unités dirigées, périmètres par portée (DIRECTS, EQUIPE, UNITES),
 * pouvoirs d'un compte d'après la matrice, réglage de la matrice (annuaire et catalogue de substitution).
 */
@ExtendWith(MockitoExtension.class)
class PouvoirsServiceTest {

    static final Acteur ADMIN = new Acteur("demo-super-admin", "Super", Set.of("IAM_ACCOUNT_EDIT", "IAM_PROFILE_EDIT"));

    /** Matrice en mémoire. */
    static final class MatriceEnMemoire implements MatricePouvoirs {

        final Map<String, PouvoirsProfil> lignes = new TreeMap<>();

        @Override
        public Map<String, PouvoirsProfil> toutes() {
            return Map.copyOf(lignes);
        }

        @Override
        public PouvoirsProfil lire(String profil) {
            return lignes.getOrDefault(profil, PouvoirsProfil.aucun(profil));
        }

        @Override
        public void enregistrer(PouvoirsProfil ligne) {
            lignes.put(ligne.profil(), ligne);
        }

        @Override
        public void supprimer(String profil) {
            lignes.remove(profil);
        }
    }

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    OrganisationServiceTest.CatalogueEnMemoire unites;
    MatriceEnMemoire matrice;
    OrganisationService organisation;
    PouvoirsService service;
    String dg;
    String chef;
    String agent;
    String technicien;
    String horsChaine;
    String stagiaire;

    String creer(String identifiant, Set<String> profils) {
        return annuaire.creer(new NouveauCompte(identifiant, identifiant, "Mballa", identifiant + "@gimac.test", null, "Fonction", null, profils), List.of());
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(
            AnnuaireEnMemoire.catalogueParDefaut(AdministrationComptesServiceTest.CATALOGUE),
            AdministrationComptesServiceTest.PRIVILEGIES
        );
        unites = new OrganisationServiceTest.CatalogueEnMemoire();
        unites.enregistrer(new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true));
        unites.enregistrer(new Unite("DES", "DES", TypeUnite.DEPARTEMENT, "DG", true));
        unites.enregistrer(new Unite("PROD_INFO", "Production informatique", TypeUnite.SERVICE, "DES", true));
        unites.enregistrer(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true));
        matrice = new MatriceEnMemoire();
        Map<ActionPouvoir, Portee> chefs = new EnumMap<>(ActionPouvoir.class);
        chefs.put(ActionPouvoir.TACHE_CONFIER, Portee.UNITES);
        chefs.put(ActionPouvoir.ACTIVITE_LIRE, Portee.EQUIPE);
        chefs.put(ActionPouvoir.RATTACHEMENT_MODIFIER, Portee.DIRECTS);
        matrice.enregistrer(new PouvoirsProfil("ROLE_CHEF_DDRM", chefs, Set.of("ROLE_CHARGE_DDRM", "ROLE_RSSI")));
        organisation = new OrganisationService(unites, annuaire, journal);
        service = new PouvoirsService(matrice, CatalogueProfils.configuration(AdministrationComptesServiceTest.proprietes()), annuaire, organisation, journal);

        // dg → chef → agent → technicien ; horsChaine et stagiaire rattachés au pôle réseau, sous un autre responsable.
        dg = creer("dg", Set.of("ROLE_CHEF_DDRM"));
        chef = creer("chef", Set.of("ROLE_CHEF_DDRM"));
        agent = creer("agent", Set.of("ROLE_CHARGE_DDRM"));
        technicien = creer("technicien", Set.of("ROLE_CHARGE_DDRM"));
        horsChaine = creer("hors.chaine", Set.of("ROLE_CHARGE_DDRM"));
        stagiaire = creer("stagiaire", Set.of("ROLE_CHARGE_DDRM"));
        annuaire.definirRattachement(chef, "DES", dg);
        annuaire.definirRattachement(agent, "PROD_INFO", chef);
        annuaire.definirRattachement(technicien, "POLE_RESEAU", agent);
        annuaire.definirRattachement(horsChaine, "POLE_RESEAU", dg);
        annuaire.definirRattachement(stagiaire, "POLE_RESEAU", null);
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Responsable d'une unité : compte interne actif ; conservé si la requête ne le mentionne pas ; unités dirigées au rattachement")
    void responsableUnite() {
        organisation.modifierUnite(ADMIN, "PROD_INFO", new Unite("PROD_INFO", "Production informatique", TypeUnite.SERVICE, "DES", true, chef));
        assertThat(unites.unites.get("PROD_INFO").responsableId()).isEqualTo(chef);
        organisation.modifierUnite(ADMIN, "PROD_INFO", new Unite("PROD_INFO", "Production info", TypeUnite.SERVICE, "DES", true), true);
        assertThat(unites.unites.get("PROD_INFO").responsableId()).isEqualTo(chef);
        assertThat(organisation.rattachement(chef).unitesDirigees()).containsExactly("POLE_RESEAU", "PROD_INFO");
        assertThat(organisation.rattachement(agent).unitesDirigees()).isEmpty();

        refus(() -> organisation.modifierUnite(ADMIN, "DES", new Unite("DES", "DES", TypeUnite.DEPARTEMENT, "DG", true, "inconnu")), "ORG_RESPONSABLE_INVALIDE", 422);
        String externe = annuaire.creer(new NouveauCompte("corr", "C", "D", "corr@gimac.test", null, null, "INST-1", Set.of()), List.of());
        refus(() -> organisation.creerUnite(ADMIN, new Unite("CELLULE_X", "X", TypeUnite.CELLULE, "DES", true, externe)), "ORG_RESPONSABLE_INVALIDE", 422);
        annuaire.definirActif(agent, false);
        refus(() -> organisation.modifierUnite(ADMIN, "DES", new Unite("DES", "DES", TypeUnite.DEPARTEMENT, "DG", true, agent)), "ORG_RESPONSABLE_INVALIDE", 422);
        organisation.modifierUnite(ADMIN, "PROD_INFO", new Unite("PROD_INFO", "Production informatique", TypeUnite.SERVICE, "DES", true), false);
        assertThat(unites.unites.get("PROD_INFO").responsableId()).isNull();
    }

    @Test
    @DisplayName("Périmètres : DIRECTS (n+1), EQUIPE (chaîne), UNITES (équipe et membres des unités dirigées, niveau 0), AUCUNE ; jamais soi-même")
    void perimetres() {
        organisation.modifierUnite(ADMIN, "PROD_INFO", new Unite("PROD_INFO", "Production informatique", TypeUnite.SERVICE, "DES", true, chef));
        annuaire.definirRattachement(chef, "PROD_INFO", dg);

        assertThat(organisation.perimetre(chef, Portee.AUCUNE).comptes()).isEmpty();
        assertThat(organisation.perimetre(chef, Portee.DIRECTS).comptes()).extracting(OrganisationService.MembreEquipe::id).containsExactly(agent);
        var equipe = organisation.perimetre(chef, Portee.EQUIPE);
        assertThat(equipe.comptes()).extracting(OrganisationService.MembreEquipe::id).containsExactly(agent, technicien);
        assertThat(equipe.unites()).isEmpty();
        var parUnites = organisation.perimetre(chef, Portee.UNITES);
        assertThat(parUnites.comptes()).extracting(OrganisationService.MembreEquipe::id).containsExactly(agent, technicien, horsChaine, stagiaire);
        assertThat(parUnites.comptes()).extracting(OrganisationService.MembreEquipe::niveau).containsExactly(1, 2, 0, 0);
        assertThat(parUnites.comptes()).extracting(OrganisationService.MembreEquipe::id).doesNotContain(chef);
        assertThat(parUnites.unites()).containsExactly("POLE_RESEAU", "PROD_INFO");
        refus(() -> organisation.perimetre("inconnu", Portee.EQUIPE), "IAM_ACCOUNT_NOT_FOUND", 404);
    }

    @Test
    @DisplayName("Pouvoirs : matrice du profil ; compte sans profil responsable : AUCUNE ; inactif : aucun pouvoir ; vue avec subordonnés et unités dirigées")
    void pouvoirs() {
        organisation.modifierUnite(ADMIN, "POLE_RESEAU", new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true, chef));
        var p = service.pouvoirs(chef);
        assertThat(p.portee(ActionPouvoir.TACHE_CONFIER)).isEqualTo(Portee.UNITES);
        assertThat(p.portee(ActionPouvoir.COMPTE_DEMANDER)).isEqualTo(Portee.AUCUNE);
        assertThat(p.profilsDemandables()).containsExactly("ROLE_CHARGE_DDRM");
        assertThat(service.pouvoirs(agent).porteeMax()).isEqualTo(Portee.AUCUNE);

        var vue = service.vue(chef);
        assertThat(vue.unitesDirigees()).containsExactly("POLE_RESEAU");
        assertThat(vue.subordonnes()).extracting(OrganisationService.MembreEquipe::id).containsExactly(agent, technicien, horsChaine, stagiaire);
        assertThat(service.vue(agent).subordonnes()).isEmpty();
        assertThat(service.perimetre(chef, ActionPouvoir.RATTACHEMENT_MODIFIER).comptes()).extracting(OrganisationService.MembreEquipe::id).containsExactly(agent);

        annuaire.definirActif(chef, false);
        assertThat(service.pouvoirs(chef).porteeMax()).isEqualTo(Portee.AUCUNE);
    }

    @Test
    @DisplayName("Matrice : une ligne par profil attribuable (AUCUNE par défaut) ; réglage journalisé ; profil privilégié non demandable ; profil inconnu 404")
    void reglage() {
        var m = service.matrice();
        assertThat(m.actions()).hasSize(10).endsWith(ActionPouvoir.PROCESSUS_DEFINIR, ActionPouvoir.PROCESSUS_LANCER);
        assertThat(m.portees()).containsExactly(Portee.AUCUNE, Portee.DIRECTS, Portee.EQUIPE, Portee.UNITES);
        assertThat(m.profils()).extracting(PouvoirsService.LigneMatrice::profil).containsExactlyElementsOf(new java.util.TreeSet<>(AdministrationComptesServiceTest.CATALOGUE));
        var rssi = m.profils().stream().filter(l -> l.profil().equals("ROLE_RSSI")).findFirst().orElseThrow();
        assertThat(rssi.privilegie()).isTrue();
        assertThat(rssi.cree()).isFalse();
        assertThat(rssi.actions().values()).containsOnly(Portee.AUCUNE);

        var ligne = service.definir(ADMIN, "ROLE_CHARGE_DDRM", Map.of(ActionPouvoir.TACHE_SUIVRE, Portee.DIRECTS), Set.of("ROLE_CHARGE_DDRM"), " Renfort ");
        assertThat(ligne.actions().get(ActionPouvoir.TACHE_SUIVRE)).isEqualTo(Portee.DIRECTS);
        assertThat(ligne.actions().get(ActionPouvoir.TACHE_CONFIER)).isEqualTo(Portee.AUCUNE);
        verify(journal).matrice(eq(ADMIN), eq("ROLE_CHARGE_DDRM"), eq("Renfort"), eq("AUCUNE demandables="), eq("TACHE_SUIVRE=DIRECTS demandables=ROLE_CHARGE_DDRM"));
        assertThat(service.pouvoirs(agent).portee(ActionPouvoir.TACHE_SUIVRE)).isEqualTo(Portee.DIRECTS);

        refus(() -> service.definir(ADMIN, "ROLE_CHEF_DDRM", Map.of(), Set.of("ROLE_SUPER_ADMIN"), null), "ORG_PROFIL_NON_DEMANDABLE", 422);
        refus(() -> service.definir(ADMIN, "ROLE_INCONNU", Map.of(), Set.of(), null), "IAM_PROFILE_NOT_FOUND", 404);
        verify(journal, never()).matrice(any(), eq("ROLE_INCONNU"), any(), any(), any());
    }

    @Test
    @DisplayName("Rattachement d'un subordonné (RATTACHEMENT_MODIFIER, DIRECTS) : succès journalisé avec l'origine ; hors périmètre, unité non dirigée, n+1 hors périmètre, soi-même, sans pouvoir : 403")
    void rattachementSubordonne() {
        organisation.modifierUnite(ADMIN, "POLE_RESEAU", new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true, chef));
        Acteur leChef = new Acteur(chef, "Chef", Set.of("PIL_ACTIVITY_OWN"));
        var c = service.rattacherSubordonne(leChef, agent, " POLE_RESEAU ", chef, " Réorganisation ");
        assertThat(c.unite()).isEqualTo("POLE_RESEAU");
        assertThat(c.responsableId()).isEqualTo(chef);
        verify(journal).compte(
            eq(leChef),
            eq("COMPTE_RATTACHEMENT_MODIFIE"),
            eq(agent),
            eq("agent"),
            eq("Réorganisation"),
            eq(List.of("avant: unite=PROD_INFO responsable=" + chef, "après: unite=POLE_RESEAU responsable=" + chef, "pouvoir=RATTACHEMENT_MODIFIER portee=DIRECTS"))
        );
        // Unité inchangée : acceptée même hors des unités dirigées.
        service.rattacherSubordonne(leChef, agent, "POLE_RESEAU", chef, null);
        refus(() -> service.rattacherSubordonne(leChef, technicien, "POLE_RESEAU", chef, null), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.rattacherSubordonne(leChef, agent, "DG", chef, null), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.rattacherSubordonne(leChef, agent, "POLE_RESEAU", dg, null), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.rattacherSubordonne(leChef, chef, "DES", dg, null), "IAM_SELF_ADMINISTRATION", 403);
        refus(() -> service.rattacherSubordonne(new Acteur(agent, "Agent", Set.of("PIL_ACTIVITY_OWN")), technicien, "POLE_RESEAU", agent, null), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.rattacherSubordonne(new Acteur("inconnu", "X", Set.of()), agent, "POLE_RESEAU", chef, null), "IAM_ACCOUNT_NOT_FOUND", 404);
        assertThat(service.perimetre(chef, ActionPouvoir.RATTACHEMENT_MODIFIER).comptes()).extracting(OrganisationService.MembreEquipe::id).containsExactly(agent);
        // Règles habituelles : un subordonné ne devient pas son propre n+1.
        refus(() -> service.rattacherSubordonne(leChef, agent, "POLE_RESEAU", agent, null), "ORG_RESPONSABLE_INVALIDE", 422);
    }
}
