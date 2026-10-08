package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.Pouvoirs;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-81 §1, §3 : portées, calcul des pouvoirs d'un compte, profils demandables sans escalade, unités dirigées. */
class ReglesPouvoirsTest {

    static final Set<String> ATTRIBUABLES = Set.of("ROLE_CHEF_DES", "ROLE_RESPONSABLE_SERVICE", "ROLE_COLLABORATEUR", "ROLE_SNA", "ROLE_SUPER_ADMIN", "ROLE_RSSI");
    static final Set<String> PRIVILEGIES = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI");

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Portées ordonnées : AUCUNE < DIRECTS < EQUIPE < UNITES ; max et couverture")
    void portees() {
        assertThat(Portee.DIRECTS.max(Portee.EQUIPE)).isEqualTo(Portee.EQUIPE);
        assertThat(Portee.UNITES.max(Portee.DIRECTS)).isEqualTo(Portee.UNITES);
        assertThat(Portee.AUCUNE.max(null)).isEqualTo(Portee.AUCUNE);
        assertThat(Portee.UNITES.couvre(Portee.EQUIPE)).isTrue();
        assertThat(Portee.DIRECTS.couvre(Portee.EQUIPE)).isFalse();
    }

    @Test
    @DisplayName("Plusieurs profils : portée la plus large par action ; profil absent de la matrice : AUCUNE ; ligne complète")
    void porteeMaximale() {
        Map<String, PouvoirsProfil> matrice = Map.of(
            "ROLE_RESPONSABLE_SERVICE",
            new PouvoirsProfil("ROLE_RESPONSABLE_SERVICE", Map.of(ActionPouvoir.TACHE_CONFIER, Portee.DIRECTS, ActionPouvoir.ACTIVITE_LIRE, Portee.EQUIPE), Set.of("ROLE_COLLABORATEUR")),
            "ROLE_CHEF_DES",
            new PouvoirsProfil("ROLE_CHEF_DES", Map.of(ActionPouvoir.TACHE_CONFIER, Portee.UNITES, ActionPouvoir.ACTIVITE_LIRE, Portee.DIRECTS), Set.of("ROLE_SNA"))
        );
        Pouvoirs p = ReglesPouvoirs.calculer(List.of("ROLE_RESPONSABLE_SERVICE", "ROLE_CHEF_DES", "ROLE_INCONNU"), matrice, ATTRIBUABLES, PRIVILEGIES);
        assertThat(p.portee(ActionPouvoir.TACHE_CONFIER)).isEqualTo(Portee.UNITES);
        assertThat(p.portee(ActionPouvoir.ACTIVITE_LIRE)).isEqualTo(Portee.EQUIPE);
        assertThat(p.portee(ActionPouvoir.STATUT_DEMANDER)).isEqualTo(Portee.AUCUNE);
        assertThat(p.actions()).hasSize(ActionPouvoir.values().length);
        assertThat(p.profilsDemandables()).containsExactly("ROLE_COLLABORATEUR", "ROLE_SNA");
        assertThat(p.porteeMax()).isEqualTo(Portee.UNITES);
        assertThat(ReglesPouvoirs.calculer(List.of("ROLE_COLLABORATEUR"), matrice, ATTRIBUABLES, PRIVILEGIES).porteeMax()).isEqualTo(Portee.AUCUNE);
    }

    @Test
    @DisplayName("Escalade interdite : un profil privilégié ou inconnu porté par la matrice n'est jamais demandable")
    void escaladeInterdite() {
        Map<String, PouvoirsProfil> matrice = Map.of("ROLE_CHEF_DES", new PouvoirsProfil("ROLE_CHEF_DES", Map.of(), Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI", "ROLE_SUPPRIME", "ROLE_SNA")));
        assertThat(ReglesPouvoirs.calculer(List.of("ROLE_CHEF_DES"), matrice, ATTRIBUABLES, PRIVILEGIES).profilsDemandables()).containsExactly("ROLE_SNA");
        refus(() -> ReglesPouvoirs.verifierLigne(new PouvoirsProfil("ROLE_CHEF_DES", Map.of(), Set.of("ROLE_RSSI")), ATTRIBUABLES, PRIVILEGIES), "ORG_PROFIL_NON_DEMANDABLE", 422);
        refus(() -> ReglesPouvoirs.verifierLigne(new PouvoirsProfil("ROLE_CHEF_DES", Map.of(), Set.of("ROLE_INCONNU")), ATTRIBUABLES, PRIVILEGIES), "IAM_PROFILE_UNKNOWN", 400);
        ReglesPouvoirs.verifierLigne(new PouvoirsProfil("ROLE_CHEF_DES", Map.of(ActionPouvoir.TACHE_CONFIER, Portee.UNITES), Set.of("ROLE_SNA")), ATTRIBUABLES, PRIVILEGIES);
        refus(() -> ReglesPouvoirs.exigerDemandables(Set.of("ROLE_SNA", "ROLE_RSSI"), Set.of("ROLE_SNA")), "ORG_PROFIL_NON_DEMANDABLE", 422);
        refus(() -> ReglesPouvoirs.exigerCouverture(false, ActionPouvoir.PROFIL_DEMANDER), "ORG_HORS_PERIMETRE", 403);
    }

    @Test
    @DisplayName("D-92 : TOUTES, la plus large ; admise seulement pour PROCESSUS_DEFINIR et PROCESSUS_LANCER")
    void porteeToutes() {
        assertThat(Portee.values()).containsExactly(Portee.AUCUNE, Portee.DIRECTS, Portee.EQUIPE, Portee.UNITES, Portee.TOUTES);
        assertThat(Portee.UNITES.max(Portee.TOUTES)).isEqualTo(Portee.TOUTES);
        assertThat(Portee.TOUTES.max(Portee.UNITES)).isEqualTo(Portee.TOUTES);
        assertThat(Portee.TOUTES.couvre(Portee.UNITES)).isTrue();
        assertThat(Portee.UNITES.couvre(Portee.TOUTES)).isFalse();
        assertThat(Portee.TOUTES.min(Portee.UNITES)).isEqualTo(Portee.UNITES);
        assertThat(Portee.DIRECTS.min(Portee.UNITES)).isEqualTo(Portee.DIRECTS);
        assertThat(Portee.DIRECTS.min(null)).isEqualTo(Portee.DIRECTS);
        for (ActionPouvoir a : ActionPouvoir.values()) {
            boolean processus = a == ActionPouvoir.PROCESSUS_DEFINIR || a == ActionPouvoir.PROCESSUS_LANCER;
            assertThat(a.admet(Portee.TOUTES)).as(a.name()).isEqualTo(processus);
            assertThat(a.admet(Portee.UNITES)).isTrue();
            assertThat(a.admet(null)).isFalse();
            assertThat(a.porteesAdmises()).hasSize(processus ? 5 : 4).startsWith(Portee.AUCUNE);
        }

        ReglesPouvoirs.verifierLigne(
            new PouvoirsProfil("ROLE_SNA", Map.of(ActionPouvoir.PROCESSUS_DEFINIR, Portee.TOUTES, ActionPouvoir.PROCESSUS_LANCER, Portee.TOUTES, ActionPouvoir.TACHE_CONFIER, Portee.UNITES), Set.of()),
            ATTRIBUABLES,
            PRIVILEGIES
        );
        for (ActionPouvoir a : List.of(ActionPouvoir.TACHE_CONFIER, ActionPouvoir.TACHE_SUIVRE, ActionPouvoir.ACTIVITE_LIRE, ActionPouvoir.RAPPORT_VISER, ActionPouvoir.RATTACHEMENT_MODIFIER, ActionPouvoir.COMPTE_DEMANDER, ActionPouvoir.PROFIL_DEMANDER, ActionPouvoir.STATUT_DEMANDER)) {
            refus(() -> ReglesPouvoirs.verifierLigne(new PouvoirsProfil("ROLE_SNA", Map.of(a, Portee.TOUTES), Set.of()), ATTRIBUABLES, PRIVILEGIES), "ORG_PORTEE_NON_ADMISE", 422);
        }
        assertThatThrownBy(() ->
            ReglesPouvoirs.verifierLigne(new PouvoirsProfil("ROLE_SNA", Map.of(ActionPouvoir.PROCESSUS_DEFINIR, Portee.TOUTES, ActionPouvoir.TACHE_SUIVRE, Portee.TOUTES), Set.of("ROLE_RSSI")), ATTRIBUABLES, PRIVILEGIES)
        ).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("ORG_PORTEE_NON_ADMISE");
            assertThat(e.details()).containsEntry("actions", List.of("TACHE_SUIVRE"));
        });
    }

    @Test
    @DisplayName("D-92 : cumul de profils, la portée la plus large l'emporte (TOUTES sur UNITES) ; portée non admise lue en base ramenée à UNITES")
    void cumulToutes() {
        Map<String, PouvoirsProfil> matrice = Map.of(
            "ROLE_CHEF_DES",
            new PouvoirsProfil("ROLE_CHEF_DES", Map.of(ActionPouvoir.PROCESSUS_DEFINIR, Portee.UNITES, ActionPouvoir.PROCESSUS_LANCER, Portee.UNITES, ActionPouvoir.TACHE_CONFIER, Portee.UNITES), Set.of()),
            "ROLE_SNA",
            new PouvoirsProfil("ROLE_SNA", Map.of(ActionPouvoir.PROCESSUS_DEFINIR, Portee.TOUTES, ActionPouvoir.TACHE_SUIVRE, Portee.TOUTES), Set.of())
        );
        Pouvoirs p = ReglesPouvoirs.calculer(List.of("ROLE_CHEF_DES", "ROLE_SNA"), matrice, ATTRIBUABLES, PRIVILEGIES);
        assertThat(p.portee(ActionPouvoir.PROCESSUS_DEFINIR)).isEqualTo(Portee.TOUTES);
        assertThat(p.portee(ActionPouvoir.PROCESSUS_LANCER)).isEqualTo(Portee.UNITES);
        assertThat(p.portee(ActionPouvoir.TACHE_CONFIER)).isEqualTo(Portee.UNITES);
        assertThat(p.portee(ActionPouvoir.TACHE_SUIVRE)).isEqualTo(Portee.UNITES);
        assertThat(p.porteeMax()).isEqualTo(Portee.TOUTES);
        assertThat(p.porteeSubordonnes()).isEqualTo(Portee.UNITES);
        assertThat(ReglesPouvoirs.calculer(List.of("ROLE_SNA", "ROLE_CHEF_DES"), matrice, ATTRIBUABLES, PRIVILEGIES)).isEqualTo(p);
        assertThat(ReglesPouvoirs.calculer(List.of("ROLE_COLLABORATEUR"), matrice, ATTRIBUABLES, PRIVILEGIES).porteeSubordonnes()).isEqualTo(Portee.AUCUNE);
    }

    @Test
    @DisplayName("Ligne de la matrice : résumé pour le journal ; ligne vide : AUCUNE")
    void resume() {
        assertThat(PouvoirsProfil.aucun("ROLE_X").resume()).isEqualTo("AUCUNE demandables=");
        assertThat(new PouvoirsProfil("ROLE_X", Map.of(ActionPouvoir.TACHE_CONFIER, Portee.UNITES), Set.of("ROLE_B", "ROLE_A")).resume()).isEqualTo("TACHE_CONFIER=UNITES demandables=ROLE_A|ROLE_B");
    }

    @Test
    @DisplayName("Unités dirigées : celles dont le compte est responsable et toutes leurs sous-unités, sans doublon")
    void unitesDirigees() {
        List<Unite> catalogue = List.of(
            new Unite("DG", "DG", TypeUnite.DIRECTION, null, true, "dg"),
            new Unite("DES", "DES", TypeUnite.DEPARTEMENT, "DG", true, "chef"),
            new Unite("PROD_INFO", "PI", TypeUnite.SERVICE, "DES", true),
            new Unite("POLE_RESEAU", "PR", TypeUnite.POLE, "PROD_INFO", true, "chef"),
            new Unite("POLE_SYSTEME", "PS", TypeUnite.POLE, "PROD_INFO", false),
            new Unite("SSI", "SSI", TypeUnite.SERVICE, "DG", true, "rssi")
        );
        assertThat(ReglesOrganisation.unitesDirigees("chef", catalogue)).containsExactly("DES", "POLE_RESEAU", "POLE_SYSTEME", "PROD_INFO");
        assertThat(ReglesOrganisation.unitesDirigees("rssi", catalogue)).containsExactly("SSI");
        assertThat(ReglesOrganisation.unitesDirigees("dg", catalogue)).hasSize(6);
        assertThat(ReglesOrganisation.unitesDirigees("personne", catalogue)).isEmpty();
        assertThat(ReglesOrganisation.unitesDirigees(null, catalogue)).isEmpty();
    }

    @Test
    @DisplayName("Rattachement d'un subordonné : jamais soi-même ; cible, unité (actuelle ou dirigée) et n+1 (actuel, chef ou périmètre) couverts")
    void rattachementSubordonne() {
        Set<String> couverts = Set.of("agent", "adjoint");
        List<String> dirigees = List.of("DES", "POLE_RESEAU");
        // Unité dirigée et n+1 du périmètre ; unité et n+1 inchangés ; le chef comme n+1.
        ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", "PROD_INFO", "chef", "POLE_RESEAU", "adjoint", couverts, dirigees);
        ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", "PROD_INFO", "dg", "PROD_INFO", "dg", couverts, dirigees);
        ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", null, null, null, "chef", couverts, dirigees);
        refus(() -> ReglesPouvoirs.verifierRattachementSubordonne("chef", "chef", "DES", "dg", "DES", "dg", Set.of("chef"), dirigees), "IAM_SELF_ADMINISTRATION", 403);
        refus(() -> ReglesPouvoirs.verifierRattachementSubordonne("chef", "etranger", "DES", "dg", "DES", "chef", couverts, dirigees), "ORG_HORS_PERIMETRE", 403);
        refus(() -> ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", "PROD_INFO", "chef", "SSI", "chef", couverts, dirigees), "ORG_HORS_PERIMETRE", 403);
        refus(() -> ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", "PROD_INFO", "chef", null, "chef", couverts, dirigees), "ORG_HORS_PERIMETRE", 403);
        refus(() -> ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", "PROD_INFO", "chef", "PROD_INFO", "etranger", couverts, dirigees), "ORG_HORS_PERIMETRE", 403);
        refus(() -> ReglesPouvoirs.verifierRattachementSubordonne("chef", "agent", "PROD_INFO", "chef", "PROD_INFO", null, couverts, dirigees), "ORG_HORS_PERIMETRE", 403);
    }
}
