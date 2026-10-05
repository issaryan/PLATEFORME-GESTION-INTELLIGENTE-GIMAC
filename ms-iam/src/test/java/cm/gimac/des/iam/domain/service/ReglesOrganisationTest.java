package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-76 : catalogue des unités (code, type, parent sans cycle, désactivation) et rattachement d'un compte. */
class ReglesOrganisationTest {

    static Map<String, Unite> catalogue() {
        Map<String, Unite> m = new LinkedHashMap<>();
        for (Unite u : List.of(
            new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true),
            new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true),
            new Unite("PROD_INFO", "Service Production informatique", TypeUnite.SERVICE, "DES", true),
            new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true),
            new Unite("ANCIEN", "Service dissous", TypeUnite.SERVICE, "DG", false)
        )) {
            m.put(u.code(), u);
        }
        return m;
    }

    static Compte compte(String id, String institution, boolean actif, String unite, String responsable) {
        return new Compte(id, id, "Prénom", "Nom " + id, id + "@gimac.test", true, actif, Instant.EPOCH, null, "Agent", "fr", institution, true, Set.of(), List.of(), true, unite, responsable);
    }

    static Function<String, Optional<Compte>> lecteur(Compte... comptes) {
        Map<String, Compte> m = new HashMap<>();
        for (Compte c : comptes) {
            m.put(c.id(), c);
        }
        return id -> Optional.ofNullable(m.get(id));
    }

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Unité : code, libellé, type obligatoires ; une direction n'a pas de parent ; normalisation")
    void champs() {
        Unite u = ReglesOrganisation.normaliser(new Unite("CELLULE_QUALITE", "  Cellule qualité ", TypeUnite.CELLULE, " ", true));
        assertThat(u.libelle()).isEqualTo("Cellule qualité");
        assertThat(u.parent()).isNull();
        assertThat(ReglesOrganisation.normaliser(new Unite("SSI", "SSI", TypeUnite.SERVICE, " DG ", true)).parent()).isEqualTo("DG");
        refus(() -> ReglesOrganisation.normaliser(new Unite("pole", "x", TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("A", "x", TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite(null, "x", TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("A" + "B".repeat(32), "x", TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("POLE_X", " ", TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("POLE_X", null, TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("POLE_X", "x".repeat(121), TypeUnite.POLE, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("POLE_X", "x", null, null, true)), "ORG_UNITE_INVALIDE", 400);
        refus(() -> ReglesOrganisation.normaliser(new Unite("DIR2", "x", TypeUnite.DIRECTION, "DG", true)), "ORG_UNITE_INVALIDE", 400);
    }

    @Test
    @DisplayName("Place dans le catalogue : parent connu et actif, aucun cycle, pas de désactivation avec des sous-unités actives")
    void place() {
        Map<String, Unite> c = catalogue();
        assertThatCode(() -> ReglesOrganisation.verifierPlace(new Unite("POLE_SYSTEME", "Pôle système", TypeUnite.POLE, "PROD_INFO", true), c)).doesNotThrowAnyException();
        refus(() -> ReglesOrganisation.verifierPlace(new Unite("X1", "x", TypeUnite.POLE, "INCONNUE", true), c), "ORG_UNITE_INCONNUE", 422);
        refus(() -> ReglesOrganisation.verifierPlace(new Unite("X1", "x", TypeUnite.POLE, "ANCIEN", true), c), "ORG_UNITE_INCONNUE", 422);
        // Une unité inactive peut rester sous un parent inactif.
        assertThatCode(() -> ReglesOrganisation.verifierPlace(new Unite("X1", "x", TypeUnite.POLE, "ANCIEN", false), c)).doesNotThrowAnyException();
        // DES placé sous POLE_RESEAU, sa propre sous-unité : cycle.
        refus(() -> ReglesOrganisation.verifierPlace(new Unite("DES", "x", TypeUnite.DEPARTEMENT, "POLE_RESEAU", true), c), "ORG_CYCLE", 409);
        refus(() -> ReglesOrganisation.verifierPlace(new Unite("PROD_INFO", "x", TypeUnite.SERVICE, "DES", false), c), "ORG_UNITE_SOUS_UNITES_ACTIVES", 409);
        assertThatCode(() -> ReglesOrganisation.verifierPlace(new Unite("POLE_RESEAU", "x", TypeUnite.POLE, "PROD_INFO", false), c)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Chemin : de la plus haute à la plus fine ; vide pour une unité inconnue ; borné face à un cycle en base")
    void chemin() {
        Map<String, Unite> c = catalogue();
        assertThat(ReglesOrganisation.chemin("POLE_RESEAU", c)).extracting(Unite::code).containsExactly("DG", "DES", "PROD_INFO", "POLE_RESEAU");
        assertThat(ReglesOrganisation.chemin("INCONNUE", c)).isEmpty();
        assertThat(ReglesOrganisation.chemin(null, c)).isEmpty();
        c.put("A1", new Unite("A1", "a", TypeUnite.POLE, "B1", true));
        c.put("B1", new Unite("B1", "b", TypeUnite.POLE, "A1", true));
        assertThat(ReglesOrganisation.chemin("A1", c)).extracting(Unite::code).containsExactly("B1", "A1");
    }

    @Test
    @DisplayName("Rattachement : compte d'institution refusé (ORG_COMPTE_EXTERNE), retrait admis")
    void compteExterne() {
        Compte corr = compte("corr", "INST-0042", true, null, null);
        Compte chef = compte("chef", null, true, null, null);
        refus(() -> ReglesOrganisation.verifierRattachement(corr, "DES", null, catalogue(), lecteur(chef)), "ORG_COMPTE_EXTERNE", 422);
        refus(() -> ReglesOrganisation.verifierRattachement(corr, null, "chef", catalogue(), lecteur(chef)), "ORG_COMPTE_EXTERNE", 422);
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(corr, null, null, catalogue(), lecteur(chef))).doesNotThrowAnyException();
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(compte("x", " ", true, null, null), "DES", null, catalogue(), lecteur())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rattachement : unité inconnue ou inactive refusée (ORG_UNITE_INCONNUE), unité inactive actuelle conservée")
    void uniteInconnue() {
        Compte agent = compte("agent", null, true, "ANCIEN", null);
        refus(() -> ReglesOrganisation.verifierRattachement(agent, "INCONNUE", null, catalogue(), lecteur()), "ORG_UNITE_INCONNUE", 422);
        refus(() -> ReglesOrganisation.verifierRattachement(compte("autre", null, true, "DES", null), "ANCIEN", null, catalogue(), lecteur()), "ORG_UNITE_INCONNUE", 422);
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(agent, "ANCIEN", null, catalogue(), lecteur())).doesNotThrowAnyException();
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(agent, "POLE_RESEAU", null, catalogue(), lecteur())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rattachement : responsable interne, actif, distinct du titulaire (ORG_RESPONSABLE_INVALIDE)")
    void responsableInvalide() {
        Compte agent = compte("agent", null, true, null, null);
        Compte corr = compte("corr", "INST-0042", true, null, null);
        Compte parti = compte("parti", null, false, null, null);
        Compte chef = compte("chef", null, true, null, null);
        var l = lecteur(agent, corr, parti, chef);
        refus(() -> ReglesOrganisation.verifierRattachement(agent, null, "agent", catalogue(), l), "ORG_RESPONSABLE_INVALIDE", 422);
        refus(() -> ReglesOrganisation.verifierRattachement(agent, null, "absent", catalogue(), l), "ORG_RESPONSABLE_INVALIDE", 422);
        refus(() -> ReglesOrganisation.verifierRattachement(agent, null, "corr", catalogue(), l), "ORG_RESPONSABLE_INVALIDE", 422);
        refus(() -> ReglesOrganisation.verifierRattachement(agent, null, "parti", catalogue(), l), "ORG_RESPONSABLE_INVALIDE", 422);
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(agent, "DES", "chef", catalogue(), l)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rattachement : le titulaire ne peut être dans la chaîne de ses responsables (ORG_CYCLE) ; cycle préexistant borné")
    void cycle() {
        // a <- b <- c : rattacher a à c ferait a -> c -> b -> a.
        Compte a = compte("a", null, true, null, null);
        Compte b = compte("b", null, true, null, "a");
        Compte c = compte("c", null, true, null, "b");
        refus(() -> ReglesOrganisation.verifierRattachement(a, null, "c", catalogue(), lecteur(a, b, c)), "ORG_CYCLE", 409);
        refus(() -> ReglesOrganisation.verifierRattachement(a, null, "b", catalogue(), lecteur(a, b, c)), "ORG_CYCLE", 409);
        // Chaîne qui s'arrête sur un compte absent, ou qui boucle sans passer par le titulaire.
        Compte x = compte("x", null, true, null, "absent");
        Compte y = compte("y", null, true, null, "z");
        Compte z = compte("z", null, true, null, "y");
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(a, null, "x", catalogue(), lecteur(a, x))).doesNotThrowAnyException();
        assertThatCode(() -> ReglesOrganisation.verifierRattachement(a, null, "y", catalogue(), lecteur(a, y, z))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Chaîne des responsables : ordre n+1, n+2… ; 10 au plus ; arrêtée à un cycle ou à un compte absent")
    void chaine() {
        Compte[] comptes = new Compte[15];
        for (int i = 0; i < 15; i++) {
            comptes[i] = compte("c" + i, null, true, null, i < 14 ? "c" + (i + 1) : null);
        }
        var l = lecteur(comptes);
        assertThat(ReglesOrganisation.chaine(comptes[0], l)).containsExactly("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10");
        assertThat(ReglesOrganisation.chaine(comptes[12], l)).containsExactly("c13", "c14");
        assertThat(ReglesOrganisation.chaine(compte("seul", null, true, null, null), l)).isEmpty();
        assertThat(ReglesOrganisation.chaine(compte("o", null, true, null, "absent"), l)).containsExactly("absent");
        Compte p = compte("p", null, true, null, "q");
        Compte q = compte("q", null, true, null, "p");
        assertThat(ReglesOrganisation.chaine(p, lecteur(p, q))).containsExactly("q");
    }
}
