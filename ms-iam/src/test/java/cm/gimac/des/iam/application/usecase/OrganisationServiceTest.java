package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.port.out.CatalogueUnites;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
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

/** D-76 : catalogue des unités, rattachement d'un compte, lecture du rattachement et de l'équipe (annuaire de substitution). */
@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {

    static final Acteur RSSI = new Acteur("demo-rssi", "RSSI", Set.of("IAM_ACCOUNT_EDIT"));

    /** Catalogue en mémoire, trié par code. */
    static final class CatalogueEnMemoire implements CatalogueUnites {

        final Map<String, Unite> unites = new TreeMap<>();

        @Override
        public List<Unite> toutes() {
            return List.copyOf(unites.values());
        }

        @Override
        public Unite enregistrer(Unite unite) {
            unites.put(unite.code(), unite);
            return unite;
        }
    }

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    CatalogueEnMemoire catalogue;
    OrganisationService service;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(Map.of(), Set.of());
        catalogue = new CatalogueEnMemoire();
        catalogue.enregistrer(new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true));
        catalogue.enregistrer(new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true));
        catalogue.enregistrer(new Unite("PROD_INFO", "Service Production informatique", TypeUnite.SERVICE, "DES", true));
        catalogue.enregistrer(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true));
        catalogue.enregistrer(new Unite("ANCIEN", "Service dissous", TypeUnite.SERVICE, "DG", false));
        service = new OrganisationService(catalogue, annuaire, journal);
    }

    String creer(String identifiant, String prenom, String institution) {
        return annuaire.creer(new NouveauCompte(identifiant, prenom, "Mballa", identifiant + "@gimac.test", null, "Fonction de " + prenom, institution, Set.of()), List.of());
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Catalogue : liste complète ou actives seules ; création, conflit de code, modification, désactivation, journal")
    void catalogueDesUnites() {
        assertThat(service.unites(false)).extracting(Unite::code).containsExactly("ANCIEN", "DES", "DG", "POLE_RESEAU", "PROD_INFO");
        assertThat(service.unites(true)).extracting(Unite::code).doesNotContain("ANCIEN");

        Unite pole = service.creerUnite(RSSI, new Unite("POLE_SYSTEME", " Pôle système ", TypeUnite.POLE, "PROD_INFO", true));
        assertThat(pole).isEqualTo(new Unite("POLE_SYSTEME", "Pôle système", TypeUnite.POLE, "PROD_INFO", true));
        verify(journal).configuration(eq(RSSI), eq("UNITE_ORGANISATIONNELLE"), isNull(), eq(List.of("CREATION", "code=POLE_SYSTEME", "type=POLE", "parent=PROD_INFO", "active=true", "responsable=null")));
        refus(() -> service.creerUnite(RSSI, new Unite("POLE_SYSTEME", "x", TypeUnite.POLE, "PROD_INFO", true)), "ORG_UNITE_EXISTANTE", 409);
        refus(() -> service.creerUnite(RSSI, new Unite("POLE_X", "x", TypeUnite.POLE, "ANCIEN", true)), "ORG_UNITE_INCONNUE", 422);

        Unite renommee = service.modifierUnite(RSSI, "POLE_SYSTEME", new Unite("IGNORE", "Pôle systèmes", TypeUnite.POLE, "PROD_INFO", true));
        assertThat(renommee.code()).isEqualTo("POLE_SYSTEME");
        assertThat(renommee.libelle()).isEqualTo("Pôle systèmes");
        refus(() -> service.modifierUnite(RSSI, "INCONNUE", new Unite(null, "x", TypeUnite.POLE, null, true)), "ORG_UNITE_INTROUVABLE", 404);
        refus(() -> service.modifierUnite(RSSI, "DES", new Unite(null, "x", TypeUnite.DEPARTEMENT, "POLE_RESEAU", true)), "ORG_CYCLE", 409);
        refus(() -> service.modifierUnite(RSSI, "PROD_INFO", new Unite(null, "x", TypeUnite.SERVICE, "DES", false)), "ORG_UNITE_SOUS_UNITES_ACTIVES", 409);
        service.modifierUnite(RSSI, "POLE_SYSTEME", new Unite(null, "Pôle systèmes", TypeUnite.POLE, "PROD_INFO", false));
        verify(journal).configuration(eq(RSSI), eq("UNITE_ORGANISATIONNELLE"), isNull(), eq(List.of("DESACTIVATION", "code=POLE_SYSTEME", "type=POLE", "parent=PROD_INFO", "active=false", "responsable=null")));
        assertThat(service.unites(true)).extracting(Unite::code).doesNotContain("POLE_SYSTEME");
    }

    @Test
    @DisplayName("Rattachement : unité et responsable posés, journal avant/après ; refus (externe, unité, responsable, cycle) sans écriture")
    void rattachement() {
        String chef = creer("chef.des", "Chef", null);
        String agent = creer("agent.reseau", "Agent", null);
        String corr = creer("correspondant", "Corr", "INST-0042");

        Compte c = service.rattacher(RSSI, agent, "POLE_RESEAU", chef);
        assertThat(c.unite()).isEqualTo("POLE_RESEAU");
        assertThat(c.responsableId()).isEqualTo(chef);
        verify(journal).compte(RSSI, "COMPTE_RATTACHEMENT_MODIFIE", agent, "agent.reseau", null, List.of("avant: unite=null responsable=null", "après: unite=POLE_RESEAU responsable=" + chef));

        refus(() -> service.rattacher(RSSI, corr, "DES", null), "ORG_COMPTE_EXTERNE", 422);
        refus(() -> service.rattacher(RSSI, agent, "INCONNUE", chef), "ORG_UNITE_INCONNUE", 422);
        refus(() -> service.rattacher(RSSI, agent, "ANCIEN", chef), "ORG_UNITE_INCONNUE", 422);
        refus(() -> service.rattacher(RSSI, agent, "POLE_RESEAU", corr), "ORG_RESPONSABLE_INVALIDE", 422);
        refus(() -> service.rattacher(RSSI, agent, "POLE_RESEAU", agent), "ORG_RESPONSABLE_INVALIDE", 422);
        refus(() -> service.rattacher(RSSI, chef, "DES", agent), "ORG_CYCLE", 409);
        refus(() -> service.rattacher(RSSI, "inconnu", "DES", null), "IAM_ACCOUNT_NOT_FOUND", 404);
        verify(journal, never()).compte(any(), eq("COMPTE_RATTACHEMENT_MODIFIE"), eq(chef), any(), any(), anyList());

        // Valeurs vides : rattachement retiré.
        Compte retire = service.rattacher(RSSI, agent, " ", "");
        assertThat(retire.unite()).isNull();
        assertThat(retire.responsableId()).isNull();
    }

    @Test
    @DisplayName("Lecture du rattachement : unité, chemin, responsable, chaîne, équipe directe")
    void lecture() {
        String dg = creer("dg", "Directeur", null);
        String chef = creer("chef.des", "Chef", null);
        String agent = creer("agent.reseau", "Agent", null);
        String collegue = creer("collegue", "Collègue", null);
        service.rattacher(RSSI, chef, "DES", dg);
        service.rattacher(RSSI, agent, "POLE_RESEAU", chef);
        service.rattacher(RSSI, collegue, "POLE_RESEAU", chef);

        OrganisationService.Rattachement r = service.rattachement(agent);
        assertThat(r.id()).isEqualTo(agent);
        assertThat(r.nomAffiche()).isEqualTo("Agent Mballa");
        assertThat(r.unite().code()).isEqualTo("POLE_RESEAU");
        assertThat(r.chemin()).extracting(Unite::code).containsExactly("DG", "DES", "PROD_INFO", "POLE_RESEAU");
        assertThat(r.responsable()).isEqualTo(new OrganisationService.Responsable(chef, "Chef Mballa", "Fonction de Chef"));
        assertThat(r.chaine()).containsExactly(chef, dg);
        assertThat(r.equipeDirecte()).isZero();

        OrganisationService.Rattachement duChef = service.rattachement(chef);
        assertThat(duChef.equipeDirecte()).isEqualTo(2);

        OrganisationService.Rattachement duDg = service.rattachement(dg);
        assertThat(duDg.unite()).isNull();
        assertThat(duDg.chemin()).isEmpty();
        assertThat(duDg.responsable()).isNull();
        assertThat(duDg.chaine()).isEmpty();

        // Responsable supprimé depuis : identifiant gardé dans la chaîne, responsable non résolu.
        annuaire.supprimer(dg);
        assertThat(service.rattachement(chef).responsable()).isNull();
        assertThat(service.rattachement(chef).chaine()).containsExactly(dg);
        refus(() -> service.rattachement("inconnu"), "IAM_ACCOUNT_NOT_FOUND", 404);
    }

    @Test
    @DisplayName("Équipe : récursive avec niveau, triée par niveau puis nom ; inactifs signalés ; cycle préexistant borné")
    void equipe() {
        String dg = creer("dg", "Directeur", null);
        String chef = creer("chef.des", "Chef", null);
        String agent = creer("agent.reseau", "Agent", null);
        String bob = creer("bob", "Bob", null);
        service.rattacher(RSSI, chef, "DES", dg);
        service.rattacher(RSSI, agent, "POLE_RESEAU", chef);
        service.rattacher(RSSI, bob, "POLE_RESEAU", chef);
        annuaire.definirActif(bob, false);

        List<OrganisationService.MembreEquipe> l = service.equipe(dg);
        assertThat(l).extracting(OrganisationService.MembreEquipe::id).containsExactly(chef, agent, bob);
        assertThat(l.get(0)).isEqualTo(new OrganisationService.MembreEquipe(chef, "Chef Mballa", "Fonction de Chef", "DES", dg, 1, true));
        assertThat(l.get(1).niveau()).isEqualTo(2);
        assertThat(l.get(2).actif()).isFalse();
        assertThat(service.equipe(agent)).isEmpty();

        // Cycle introduit hors de MS-IAM (attributs posés directement) : chaque compte une seule fois.
        annuaire.definirRattachement(dg, null, agent);
        assertThat(service.equipe(dg)).extracting(OrganisationService.MembreEquipe::id).containsExactly(chef, agent, bob);
        refus(() -> service.equipe("inconnu"), "IAM_ACCOUNT_NOT_FOUND", 404);
    }

    @Test
    @DisplayName("Équipe : 500 comptes au plus")
    void equipeBornee() {
        String chef = creer("chef", "Chef", null);
        for (int i = 0; i < 510; i++) {
            String id = creer("agent." + i, "Agent " + i, null);
            annuaire.definirRattachement(id, null, chef);
        }
        assertThat(service.equipe(chef)).hasSize(500);
    }

    @Test
    @DisplayName("D-86 : membres d'une unité (sous-unités comprises ou non), comptes internes actifs, filtre par profil, profils de chacun, unité inconnue 404")
    void membresUnite() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_POLE_RESEAU", "ROLE_COLLABORATEUR", "ROLE_RESPONSABLE_SERVICE")), Set.of());
        service = new OrganisationService(catalogue, annuaire, journal);
        String chef = annuaire.creer(new NouveauCompte("chef.prod", "Paul", "Ekane", "chef@gimac.test", null, "Chef de service", null, Set.of("ROLE_RESPONSABLE_SERVICE")), List.of());
        String bob = annuaire.creer(new NouveauCompte("bob", "Bob", "Ndi", "bob@gimac.test", null, "Technicien", null, Set.of("ROLE_POLE_RESEAU", "ROLE_COLLABORATEUR")), List.of());
        String awa = annuaire.creer(new NouveauCompte("awa", "Awa", "Atangana", "awa@gimac.test", null, "Technicienne", null, Set.of("ROLE_POLE_RESEAU")), List.of());
        String inactif = annuaire.creer(new NouveauCompte("parti", "Jean", "Parti", "parti@gimac.test", null, "Ancien", null, Set.of("ROLE_POLE_RESEAU")), List.of());
        String externe = annuaire.creer(new NouveauCompte("corr", "Corr", "Ext", "corr@banque.test", null, "Correspondant", "INST-0042", Set.of("ROLE_POLE_RESEAU")), List.of());
        annuaire.definirRattachement(chef, "PROD_INFO", null);
        annuaire.definirRattachement(bob, "POLE_RESEAU", chef);
        annuaire.definirRattachement(awa, "POLE_RESEAU", chef);
        annuaire.definirRattachement(inactif, "POLE_RESEAU", chef);
        annuaire.definirRattachement(externe, "POLE_RESEAU", null);
        annuaire.definirActif(inactif, false);

        var tous = service.membresUnite("PROD_INFO", null, true);
        assertThat(tous).extracting(OrganisationService.MembreUnite::id).containsExactly(awa, bob, chef);
        assertThat(tous.get(1)).satisfies(m -> {
            assertThat(m.nomAffiche()).isEqualTo("Bob Ndi");
            assertThat(m.fonction()).isEqualTo("Technicien");
            assertThat(m.unite()).isEqualTo("POLE_RESEAU");
            assertThat(m.responsableId()).isEqualTo(chef);
            assertThat(m.actif()).isTrue();
            assertThat(m.profils()).containsExactly("ROLE_COLLABORATEUR", "ROLE_POLE_RESEAU");
        });
        assertThat(service.membresUnite("PROD_INFO", null, false)).extracting(OrganisationService.MembreUnite::id).containsExactly(chef);
        assertThat(service.membresUnite("PROD_INFO", "ROLE_POLE_RESEAU", true)).extracting(OrganisationService.MembreUnite::id).containsExactly(awa, bob);
        assertThat(service.membresUnite("DG", " ROLE_RESPONSABLE_SERVICE ", true)).extracting(OrganisationService.MembreUnite::id).containsExactly(chef);
        assertThat(service.membresUnite("POLE_RESEAU", "ROLE_INCONNU", true)).isEmpty();
        assertThat(service.membresUnite("ANCIEN", null, true)).isEmpty();
        refus(() -> service.membresUnite("INCONNUE", null, true), "ORG_UNITE_INTROUVABLE", 404);
    }

    @Test
    @DisplayName("D-87 : une unité par son code, active ou non, avec son responsable ; unité inconnue 404")
    void uniteParCode() {
        String chef = creer("chef.reseau", "Brice", null);
        catalogue.enregistrer(catalogue.unites.get("POLE_RESEAU").avecResponsable(chef));
        assertThat(service.unite("POLE_RESEAU")).satisfies(u -> {
            assertThat(u.code()).isEqualTo("POLE_RESEAU");
            assertThat(u.libelle()).isEqualTo("Pôle réseau");
            assertThat(u.type()).isEqualTo(TypeUnite.POLE);
            assertThat(u.parent()).isEqualTo("PROD_INFO");
            assertThat(u.active()).isTrue();
            assertThat(u.responsableId()).isEqualTo(chef);
        });
        assertThat(service.unite("ANCIEN").active()).isFalse();
        assertThat(service.unite("ANCIEN").responsableId()).isNull();
        refus(() -> service.unite("INCONNUE"), "ORG_UNITE_INTROUVABLE", 404);
    }
}
