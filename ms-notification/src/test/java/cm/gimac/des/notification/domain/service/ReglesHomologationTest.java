package cm.gimac.des.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.domain.service.ReglesHomologation.Cible;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsJalon;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsOperation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Intervenant;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Nature;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Role;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles des notifications du parcours d'homologation (D-93 §11). */
class ReglesHomologationTest {

    static final String LIAISON = "11111111-1111-4111-8111-111111111111";
    static final String OPERATION = "22222222-2222-4222-8222-222222222222";
    static final String BANQUE = "33333333-3333-4333-8333-333333333333";
    static final String PERSO = "44444444-4444-4444-8444-444444444444";

    final ReglesHomologation regles = new ReglesHomologation();

    static Intervenant compte(String qualite, String id) {
        return new Intervenant(qualite, "DDRM", id, false, null);
    }

    static Intervenant institution(String qualite, String id) {
        return new Intervenant(qualite, "BANQUE_PARTENAIRE", null, false, id);
    }

    static Intervenant aAffecter(String qualite, String fonction) {
        return new Intervenant(qualite, fonction, null, true, null);
    }

    static FaitsOperation operation(Nature nature, String motif, Intervenant r, Intervenant v, List<Intervenant> dest, List<String> institutions) {
        return new FaitsOperation(
            nature,
            LIAISON,
            "LIA-2026-001",
            "Banque A",
            OPERATION,
            "3",
            "Cadrage du projet",
            "DEPOSER_PREUVE",
            "2026-10-09",
            motif,
            r,
            v,
            dest,
            institutions,
            "/des/parcours/" + LIAISON + "?onglet=operations&operation=" + OPERATION
        );
    }

    @Test
    @DisplayName("Opération ouverte : R action, C avis, I information, E escalade ; V non notifié ; à affecter jamais nommé ; rôle le plus fort")
    void ouverte() {
        FaitsOperation f = operation(
            Nature.OPERATION_ACTIVEE,
            "OUVERTURE",
            compte("R", "r"),
            compte("V", "v"),
            List.of(compte("C", "c"), compte("I", "i"), compte("E", "e"), compte("I", "r"), aAffecter("C", "SNA"), compte("X", "x")),
            null
        );
        Map<Cible, Role> c = regles.ciblesOperation(f);
        assertThat(c).containsExactly(
            Map.entry(Cible.compte("r"), Role.ACTION),
            Map.entry(Cible.compte("c"), Role.AVIS),
            Map.entry(Cible.compte("i"), Role.INFORMATION),
            Map.entry(Cible.compte("e"), Role.ESCALADE)
        );
        assertThat(regles.fonctionsAAffecter(f)).containsExactly("SNA");
        assertThat(regles.titreOperation(f, Role.ACTION)).isEqualTo("Action attendue : Cadrage du projet");
        assertThat(regles.titreOperation(f, Role.AVIS)).isEqualTo("Avis demandé : Cadrage du projet");
        assertThat(regles.titreOperation(f, Role.INFORMATION)).isEqualTo("Opération ouverte : Cadrage du projet");
        assertThat(regles.titreOperation(f, Role.ESCALADE)).isEqualTo("Escalade : Cadrage du projet");
        assertThat(regles.sousTitreOperation(f)).isEqualTo("LIA-2026-001 (Banque A), échéance 2026-10-09");
    }

    @Test
    @DisplayName("Opération renvoyée : titre propre au R ; R à affecter : aucune cible nommée")
    void renvoiEtAAffecter() {
        FaitsOperation f = operation(Nature.OPERATION_ACTIVEE, "RENVOI", compte("R", "r"), null, List.of(), null);
        assertThat(regles.titreOperation(f, Role.ACTION)).isEqualTo("Opération renvoyée : Cadrage du projet");
        FaitsOperation g = operation(Nature.OPERATION_ACTIVEE, "OUVERTURE", aAffecter("R", "DDRM"), null, List.of(), null);
        assertThat(regles.ciblesOperation(g)).isEmpty();
        assertThat(regles.fonctionsAAffecter(g)).containsExactly("DDRM");
    }

    @Test
    @DisplayName("Opération externe : institution R (action, lien de dépôt du portail), institutions E (escalade, lien de la liaison)")
    void externe() {
        FaitsOperation f = operation(Nature.OPERATION_ACTIVEE, "OUVERTURE", institution("R", BANQUE), compte("V", "v"), List.of(), List.of(BANQUE, PERSO));
        assertThat(regles.ciblesOperation(f)).containsExactly(Map.entry(Cible.institution(BANQUE), Role.ACTION), Map.entry(Cible.institution(PERSO), Role.ESCALADE));
        assertThat(regles.lienPortailOperation(f, Role.ACTION)).isEqualTo("/portail/liaisons/" + LIAISON + "/livrables/" + OPERATION);
        assertThat(regles.lienPortailOperation(f, Role.ESCALADE)).isEqualTo("/portail/liaisons/" + LIAISON);
    }

    @Test
    @DisplayName("Validation demandée : V action, institution R informée, institutions concernées informées ; R interne non notifié")
    void validation() {
        FaitsOperation f = operation(Nature.VALIDATION_DEMANDEE, null, institution("R", BANQUE), compte("V", "v"), List.of(compte("C", "c")), List.of(BANQUE));
        assertThat(regles.ciblesOperation(f)).containsExactly(
            Map.entry(Cible.compte("v"), Role.ACTION),
            Map.entry(Cible.institution(BANQUE), Role.INFORMATION),
            Map.entry(Cible.compte("c"), Role.AVIS)
        );
        assertThat(regles.titreOperation(f, Role.ACTION)).isEqualTo("Validation attendue : Cadrage du projet");
        assertThat(regles.titreOperation(f, Role.INFORMATION)).isEqualTo("Opération en validation : Cadrage du projet");
        assertThat(regles.lienPortailOperation(f, Role.INFORMATION)).isEqualTo("/portail/liaisons/" + LIAISON);
        FaitsOperation interne = operation(Nature.VALIDATION_DEMANDEE, null, compte("R", "r"), compte("V", "v"), List.of(), null);
        assertThat(regles.ciblesOperation(interne)).containsOnlyKeys(Cible.compte("v"));
    }

    @Test
    @DisplayName("Liens internes : celui de l'événement s'il est sûr, sinon reconstruit ; identifiants douteux écartés")
    void liens() {
        FaitsOperation f = operation(Nature.OPERATION_ACTIVEE, null, compte("R", "r"), null, List.of(), null);
        assertThat(regles.lienInterneOperation(f)).isEqualTo("/des/parcours/" + LIAISON + "?onglet=operations&operation=" + OPERATION);
        FaitsOperation douteux = new FaitsOperation(Nature.OPERATION_ACTIVEE, LIAISON, null, null, OPERATION, "3", null, "AUCUN", null, null, null, null, null, null, "https://ailleurs/../x");
        assertThat(regles.lienInterneOperation(douteux)).isEqualTo("/des/parcours/" + LIAISON + "?onglet=operations&operation=" + OPERATION);
        assertThat(regles.titreOperation(douteux, Role.ACTION)).isEqualTo("Action attendue : opération 3");
        assertThat(regles.sousTitreOperation(douteux)).isNull();
        FaitsOperation injecte = new FaitsOperation(Nature.OPERATION_ACTIVEE, "../x", null, null, "a/b", null, null, "DEPOSER_LIVRABLE", null, null, null, null, null, null, null);
        assertThat(regles.lienInterneOperation(injecte)).isEqualTo("/des/parcours");
        assertThat(regles.lienPortailOperation(injecte, Role.ACTION)).isEqualTo("/portail/liaisons");
    }

    @Test
    @DisplayName("Jalon : conducteur, C et I informés, institution si le jalon la touche ; titres et liens")
    void jalon() {
        FaitsJalon f = new FaitsJalon(
            LIAISON,
            "LIA-2026-001",
            BANQUE,
            "Banque A",
            "EN_PRODUCTION",
            "En production",
            "conducteur",
            List.of(compte("C", "c"), compte("I", "conducteur"), aAffecter("I", "SNA")),
            "/des/parcours/" + LIAISON
        );
        assertThat(regles.ciblesJalon(f, ReglesHomologation.JALONS_PORTAIL_DEFAUT)).containsExactly(
            Map.entry(Cible.compte("conducteur"), Role.INFORMATION),
            Map.entry(Cible.compte("c"), Role.INFORMATION),
            Map.entry(Cible.institution(BANQUE), Role.INFORMATION)
        );
        assertThat(regles.ciblesJalon(f, Set.of("CERTIFIEE"))).doesNotContainKey(Cible.institution(BANQUE));
        assertThat(regles.titreJalon(f)).isEqualTo("Jalon atteint : En production");
        assertThat(regles.sousTitreJalon(f)).isEqualTo("LIA-2026-001 (Banque A)");
        assertThat(regles.lienInterneJalon(f)).isEqualTo("/des/parcours/" + LIAISON);
        assertThat(regles.lienPortailJalon(f)).isEqualTo("/portail/liaisons/" + LIAISON);
        FaitsJalon nu = new FaitsJalon(LIAISON, null, null, null, "CERTIFIEE", null, null, null, null);
        assertThat(regles.titreJalon(nu)).isEqualTo("Jalon atteint : CERTIFIEE");
        assertThat(regles.sousTitreJalon(nu)).isNull();
        assertThat(regles.lienInterneJalon(nu)).isEqualTo("/des/parcours/" + LIAISON);
    }
}
