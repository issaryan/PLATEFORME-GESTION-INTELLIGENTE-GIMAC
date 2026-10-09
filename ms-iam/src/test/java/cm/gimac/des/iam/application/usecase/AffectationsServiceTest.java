package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import cm.gimac.des.iam.domain.service.ReglesOrganisation;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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
 * D-102 : affectation explicite de personnes à une unité (n+1 résolu, refus d'ensemble avec détail par compte, chef dans
 * son périmètre) et retrait d'une unité.
 */
@ExtendWith(MockitoExtension.class)
class AffectationsServiceTest {

    static final Acteur ADMIN = new Acteur("demo-super-admin", "Super", Set.of("IAM_ACCOUNT_EDIT"));

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    OrganisationServiceTest.CatalogueEnMemoire unites;
    OrganisationService organisation;
    PouvoirsService pouvoirs;
    AffectationsService service;
    String dg;
    String chefDes;
    String chefInfo;
    String agent;
    String nouveau;
    String autre;

    String creer(String identifiant, Set<String> profils, String institution) {
        return annuaire.creer(new NouveauCompte(identifiant, identifiant, "Mballa", identifiant + "@gimac.test", null, "Fonction", institution, profils), List.of());
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(AdministrationComptesServiceTest.CATALOGUE));
        unites = new OrganisationServiceTest.CatalogueEnMemoire();
        dg = creer("dg", Set.of("ROLE_DG"), null);
        chefDes = creer("chef.des", Set.of("ROLE_CHEF_DDRM"), null);
        chefInfo = creer("chef.info", Set.of("ROLE_CHARGE_DDRM"), null);
        agent = creer("agent", Set.of("ROLE_CHARGE_DDRM"), null);
        nouveau = creer("nouveau", Set.of("ROLE_CHARGE_DDRM"), null);
        autre = creer("autre", Set.of("ROLE_CHARGE_DDRM"), null);
        unites.enregistrer(new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true, dg));
        unites.enregistrer(new Unite("DES", "DES", TypeUnite.DEPARTEMENT, "DG", true, chefDes));
        unites.enregistrer(new Unite("PROD_INFO", "Production informatique", TypeUnite.SERVICE, "DES", true, chefInfo));
        unites.enregistrer(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true, null));
        unites.enregistrer(new Unite("ANCIEN", "Service dissous", TypeUnite.SERVICE, "DG", false));
        annuaire.definirRattachement(chefDes, "DES", dg);
        annuaire.definirRattachement(chefInfo, "PROD_INFO", chefDes);
        annuaire.definirRattachement(agent, "PROD_INFO", chefInfo);
        PouvoirsServiceTest.MatriceEnMemoire matrice = new PouvoirsServiceTest.MatriceEnMemoire();
        Map<ActionPouvoir, Portee> chefs = new EnumMap<>(ActionPouvoir.class);
        chefs.put(ActionPouvoir.RATTACHEMENT_MODIFIER, Portee.UNITES);
        matrice.enregistrer(new PouvoirsProfil("ROLE_CHEF_DDRM", chefs, Set.of()));
        organisation = new OrganisationService(unites, annuaire, journal);
        pouvoirs = new PouvoirsService(matrice, CatalogueProfils.configuration(AdministrationComptesServiceTest.proprietes(), annuaire), annuaire, organisation, journal);
        service = new AffectationsService(unites, annuaire, organisation, pouvoirs);
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("n+1 par défaut : chef de l'unité, sinon de l'unité parente la plus proche ; le chef reçoit le chef de l'unité parente")
    void responsableParDefaut() {
        Map<String, Unite> parCode = new java.util.LinkedHashMap<>();
        unites.toutes().forEach(u -> parCode.put(u.code(), u));
        assertThat(ReglesOrganisation.responsableParDefaut(agent, "PROD_INFO", parCode)).isEqualTo(chefInfo);
        assertThat(ReglesOrganisation.responsableParDefaut(agent, "POLE_RESEAU", parCode)).isEqualTo(chefInfo);
        assertThat(ReglesOrganisation.responsableParDefaut(chefInfo, "PROD_INFO", parCode)).isEqualTo(chefDes);
        assertThat(ReglesOrganisation.responsableParDefaut(dg, "DG", parCode)).isNull();
        assertThat(ReglesOrganisation.responsableParDefaut(agent, null, parCode)).isNull();
        assertThat(ReglesOrganisation.responsableParDefaut(agent, "INCONNUE", parCode)).isNull();
    }

    @Test
    @DisplayName("Administration : plusieurs personnes affectées en un geste, n+1 résolu, avant et après, journal motivé par personne")
    void affecterParAdministration() {
        var r = service.affecter(ADMIN, "POLE_RESEAU", List.of(nouveau, autre, nouveau), false, null, "  Renfort du pôle  ");
        assertThat(r).extracting(AffectationsService.Resultat::id).containsExactly(nouveau, autre);
        assertThat(r.get(0).avant()).isEqualTo(new AffectationsService.Etat(null, null));
        assertThat(r.get(0).apres()).isEqualTo(new AffectationsService.Etat("POLE_RESEAU", chefInfo));
        assertThat(annuaire.lire(autre).orElseThrow().unite()).isEqualTo("POLE_RESEAU");
        verify(journal).compte(
            eq(ADMIN),
            eq("COMPTE_RATTACHEMENT_MODIFIE"),
            eq(nouveau),
            eq("nouveau"),
            eq("Renfort du pôle"),
            eq(List.of("avant: unite=null responsable=null", "après: unite=POLE_RESEAU responsable=" + chefInfo, "affectation=POLE_RESEAU"))
        );
        verify(journal, times(2)).compte(eq(ADMIN), eq("COMPTE_RATTACHEMENT_MODIFIE"), anyString(), anyString(), eq("Renfort du pôle"), anyList());
    }

    @Test
    @DisplayName("n+1 demandé : appliqué à tous ; null : aucun n+1 ; la personne qui est ce n+1 reçoit le n+1 résolu")
    void responsableDemande() {
        var r = service.affecter(ADMIN, "PROD_INFO", List.of(nouveau, autre), true, autre, "Réorganisation");
        assertThat(r.get(0).apres().responsableId()).isEqualTo(autre);
        // La personne désignée comme n+1 des autres reçoit le n+1 résolu (chef du service), jamais elle-même.
        assertThat(r.get(1).apres()).isEqualTo(new AffectationsService.Etat("PROD_INFO", chefInfo));
        // Le chef du DES affecté sous le chef du service, qui l'a lui-même pour n+1 : cycle refusé.
        refus(() -> service.affecter(ADMIN, "PROD_INFO", List.of(chefDes), false, null, "Réorganisation"), "ORG_AFFECTATION_REFUSEE", 422);
        var sans = service.affecter(ADMIN, "DES", List.of(autre), true, null, "Sans n+1");
        assertThat(sans.get(0).apres()).isEqualTo(new AffectationsService.Etat("DES", null));
    }

    @Test
    @DisplayName("Refus d'ensemble (422 ORG_AFFECTATION_REFUSEE) : compte inconnu, externe, inactif, cycle ; aucune modification")
    void refusDEnsemble() {
        String externe = creer("correspondant", Set.of(), "INST-0042");
        String inactif = creer("parti", Set.of(), null);
        annuaire.definirActif(inactif, false);
        assertThatThrownBy(() -> service.affecter(ADMIN, "PROD_INFO", List.of(nouveau, "inconnu", externe, inactif), false, null, "Motif valable"))
            .isInstanceOfSatisfying(IamException.class, e -> {
                assertThat(e.code()).isEqualTo("ORG_AFFECTATION_REFUSEE");
                assertThat(e.status()).isEqualTo(422);
                @SuppressWarnings("unchecked")
                List<Map<String, String>> refus = (List<Map<String, String>>) e.details().get("refus");
                assertThat(refus).extracting(m -> m.get("id") + ":" + m.get("code")).containsExactly(
                    "inconnu:IAM_ACCOUNT_NOT_FOUND",
                    externe + ":ORG_COMPTE_EXTERNE",
                    inactif + ":ORG_COMPTE_INACTIF"
                );
            });
        assertThat(annuaire.lire(nouveau).orElseThrow().unite()).isNull();
        // Cycle : le chef du service ne peut avoir pour n+1 son propre agent.
        refus(() -> service.affecter(ADMIN, "PROD_INFO", List.of(nouveau, chefInfo), true, agent, "Motif valable"), "ORG_AFFECTATION_REFUSEE", 422);
        assertThat(annuaire.lire(nouveau).orElseThrow().unite()).isNull();
        verify(journal, never()).compte(any(), anyString(), anyString(), anyString(), any(), anyList());
    }

    @Test
    @DisplayName("Contrôles : motif 3..500, 1 à 50 personnes, identifiant valide, unité inconnue (404) ou inactive (422)")
    void controles() {
        refus(() -> service.affecter(ADMIN, "DES", List.of(nouveau), false, null, " a "), "IAM_REASON_REQUIRED", 400);
        refus(() -> service.affecter(ADMIN, "DES", List.of(nouveau), false, null, "x".repeat(501)), "IAM_REASON_REQUIRED", 400);
        refus(() -> service.affecter(ADMIN, "DES", List.of(nouveau), false, null, null), "IAM_REASON_REQUIRED", 400);
        refus(() -> service.affecter(ADMIN, "DES", List.of(), false, null, "Motif"), "ORG_AFFECTATION_INVALIDE", 400);
        refus(() -> service.affecter(ADMIN, "DES", null, false, null, "Motif"), "ORG_AFFECTATION_INVALIDE", 400);
        refus(() -> service.affecter(ADMIN, "DES", new ArrayList<>(Collections.nCopies(51, nouveau)), false, null, "Motif"), "ORG_AFFECTATION_INVALIDE", 400);
        List<String> blanc = new ArrayList<>();
        blanc.add(" ");
        refus(() -> service.affecter(ADMIN, "DES", blanc, false, null, "Motif"), "ORG_AFFECTATION_INVALIDE", 400);
        refus(() -> service.affecter(ADMIN, "INCONNUE", List.of(nouveau), false, null, "Motif"), "ORG_UNITE_INTROUVABLE", 404);
        refus(() -> service.affecter(ADMIN, "ANCIEN", List.of(nouveau), false, null, "Motif"), "ORG_UNITE_INCONNUE", 422);
    }

    @Test
    @DisplayName("Chef (RATTACHEMENT_MODIFIER, UNITES) : affecte dans les unités qu'il dirige ; unité non dirigée, personne hors périmètre, soi-même : 403")
    void affecterParUnChef() {
        Acteur leChef = new Acteur(chefDes, "Chef DES", Set.of("PIL_ACTIVITY_OWN"));
        // L'agent (membre d'une unité dirigée) passe du service au pôle ; n+1 résolu : le chef du service.
        var r = service.affecter(leChef, "POLE_RESEAU", List.of(agent), false, null, "Mutation interne");
        assertThat(r.get(0).apres()).isEqualTo(new AffectationsService.Etat("POLE_RESEAU", chefInfo));
        verify(journal).compte(
            eq(leChef),
            eq("COMPTE_RATTACHEMENT_MODIFIE"),
            eq(agent),
            eq("agent"),
            eq("Mutation interne"),
            eq(List.of("avant: unite=PROD_INFO responsable=" + chefInfo, "après: unite=POLE_RESEAU responsable=" + chefInfo, "affectation=POLE_RESEAU pouvoir=RATTACHEMENT_MODIFIER portee=UNITES"))
        );
        refus(() -> service.affecter(leChef, "DG", List.of(agent), false, null, "Motif"), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.affecter(leChef, "PROD_INFO", List.of(nouveau), false, null, "Motif"), "ORG_HORS_PERIMETRE", 403);
        refus(() -> service.affecter(leChef, "PROD_INFO", List.of(chefDes), false, null, "Motif"), "IAM_SELF_ADMINISTRATION", 403);
        // n+1 demandé hors de son périmètre.
        refus(() -> service.affecter(leChef, "PROD_INFO", List.of(agent), true, dg, "Motif"), "ORG_HORS_PERIMETRE", 403);
        // Sans pouvoir de rattachement.
        refus(() -> service.affecter(new Acteur(agent, "Agent", Set.of("PIL_ACTIVITY_OWN")), "POLE_RESEAU", List.of(nouveau), false, null, "Motif"), "ORG_HORS_PERIMETRE", 403);
    }

    @Test
    @DisplayName("Retrait : unité retirée, n+1 venu de l'unité retiré, autre n+1 gardé ; absent (409), chef de l'unité (409), inconnus (404)")
    void retirer() {
        var r = service.retirer(ADMIN, "PROD_INFO", agent, "Départ du service");
        assertThat(r.avant()).isEqualTo(new AffectationsService.Etat("PROD_INFO", chefInfo));
        assertThat(r.apres()).isEqualTo(new AffectationsService.Etat(null, null));
        verify(journal).compte(eq(ADMIN), eq("COMPTE_RATTACHEMENT_MODIFIE"), eq(agent), eq("agent"), eq("Départ du service"), eq(List.of("avant: unite=PROD_INFO responsable=" + chefInfo, "après: unite=null responsable=null", "retrait=PROD_INFO")));
        annuaire.definirRattachement(autre, "PROD_INFO", dg);
        assertThat(service.retirer(ADMIN, "PROD_INFO", autre, "Départ").apres()).isEqualTo(new AffectationsService.Etat(null, dg));
        refus(() -> service.retirer(ADMIN, "PROD_INFO", agent, "Motif"), "ORG_AFFECTATION_ABSENTE", 409);
        refus(() -> service.retirer(ADMIN, "PROD_INFO", chefInfo, "Motif"), "ORG_CHEF_UNITE", 409);
        refus(() -> service.retirer(ADMIN, "INCONNUE", agent, "Motif"), "ORG_UNITE_INTROUVABLE", 404);
        refus(() -> service.retirer(ADMIN, "PROD_INFO", "inconnu", "Motif"), "IAM_ACCOUNT_NOT_FOUND", 404);
        refus(() -> service.retirer(ADMIN, "PROD_INFO", chefInfo, ""), "IAM_REASON_REQUIRED", 400);
    }
}
