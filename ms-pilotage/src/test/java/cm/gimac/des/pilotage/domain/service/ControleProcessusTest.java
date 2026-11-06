package cm.gimac.des.pilotage.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.domain.processus.Anomalie;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.DocumentSigne;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.Livrable;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.infrastructure.demo.DemonstrationProcessus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contrôles structurels de publication (D-87 §1), graphe (Kahn) et empreinte des signatures. */
class ControleProcessusTest {

    private static final Intervenant PROFIL = new Intervenant(ModeIntervenant.PROFIL, "ROLE_POLE_RESEAU", null, null, null);

    private static Etape etape(String code, Intervenant i, List<String> preds) {
        return new Etape(code, code, null, i, null, preds, 0, List.of(), List.of(), List.of(), null);
    }

    private static List<String> codes(List<Anomalie> a) {
        return a.stream().map(Anomalie::code).toList();
    }

    @Test
    @DisplayName("Le processus de démonstration ne présente aucune anomalie structurelle")
    void demonstration() {
        assertThat(ControleProcessus.structure(DemonstrationProcessus.etapes())).isEmpty();
        assertThat(GrapheProcessus.kahn(DemonstrationProcessus.etapes()).ordre()).containsExactly("DEMANDE", "ANALYSE", "MISE_EN_OEUVRE", "RECETTE");
        assertThat(GrapheProcessus.anterieures(DemonstrationProcessus.etapes(), "RECETTE")).containsExactlyInAnyOrder("DEMANDE", "ANALYSE", "MISE_EN_OEUVRE");
        assertThat(GrapheProcessus.successeurs(DemonstrationProcessus.etapes(), "ANALYSE")).containsExactly("MISE_EN_OEUVRE");
    }

    @Test
    @DisplayName("Graphe : aucune étape, cycle (Kahn), boucle sur soi, prédécesseur inconnu, aucune étape initiale, code en double")
    void graphe() {
        assertThat(codes(ControleProcessus.structure(List.of()))).containsExactly("AUCUNE_ETAPE");
        List<Etape> cycle = List.of(etape("A", PROFIL, List.of()), etape("B", PROFIL, List.of("A", "C")), etape("C", PROFIL, List.of("B")));
        GrapheProcessus.Ordre o = GrapheProcessus.kahn(cycle);
        assertThat(o.ordre()).containsExactly("A");
        assertThat(o.enCycle()).containsExactly("B", "C");
        assertThat(codes(ControleProcessus.structure(cycle))).containsExactly("CYCLE");
        assertThat(codes(ControleProcessus.structure(List.of(etape("A", PROFIL, List.of("A")))))).contains("CYCLE", "AUCUNE_ETAPE_INITIALE");
        assertThat(codes(ControleProcessus.structure(List.of(etape("A", PROFIL, List.of()), etape("B", PROFIL, List.of("X")))))).containsExactly("PREDECESSEUR_INCONNU");
        assertThat(codes(ControleProcessus.structure(List.of(etape("A", PROFIL, List.of()), etape("A", PROFIL, List.of()))))).contains("CODE_DUPLIQUE");
    }

    @Test
    @DisplayName("Intervenants : absent, profil ou personne manquants, remplacement exigé pour PERSONNE, désignation au lancement ouverte signalée (D-142), validateur distinct")
    void intervenants() {
        Intervenant personneSansRemplacement = new Intervenant(ModeIntervenant.PERSONNE, null, "p1", null, null);
        Intervenant auLancement = new Intervenant(ModeIntervenant.AU_LANCEMENT, null, null, null, null);
        Intervenant profilVide = new Intervenant(ModeIntervenant.PROFIL, null, null, null, null);
        List<Anomalie> a = ControleProcessus.structure(
            List.of(etape("A", null, List.of()), etape("B", personneSansRemplacement, List.of("A")), etape("C", auLancement, List.of("B")), etape("D", profilVide, List.of("C")))
        );
        assertThat(codes(a)).containsExactlyInAnyOrder("INTERVENANT_ABSENT", "REMPLACEMENT_ABSENT", "DESIGNATION_OUVERTE", "INTERVENANT_INCOMPLET");
        assertThat(a.stream().filter(x -> x.code().equals("DESIGNATION_OUVERTE")).findFirst().orElseThrow().gravite()).isEqualTo(cm.gimac.des.pilotage.domain.processus.GraviteAnomalie.LEVABLE);
        assertThat(ModeIntervenant.AU_LANCEMENT.exigeRemplacement()).isFalse();
        Intervenant p = new Intervenant(ModeIntervenant.PERSONNE, null, "p1", null, "ROLE_X");
        Etape memePersonne = new Etape("A", "A", null, p, p, List.of(), 0, List.of(), List.of(), List.of(), null);
        assertThat(codes(ControleProcessus.structure(List.of(memePersonne)))).containsExactly("VALIDATEUR_IDENTIQUE");
        assertThat(ModeIntervenant.CHEF_UNITE.exigeRemplacement()).isFalse();
    }

    @Test
    @DisplayName("Livrables, signatures, champs : codes uniques, portée des signatures (étape ou antérieures), choix d'un champ CHOIX")
    void elements() {
        Livrable l1 = new Livrable("L1", "L1", List.of(), true, null);
        Livrable l2 = new Livrable("L2", "L2", List.of(), true, null);
        Etape a = new Etape("A", "A", null, PROFIL, null, List.of(), 0, List.of(l1), List.of(), List.of(new Champ("C", "C", TypeChamp.CHOIX, List.of(), true)), null);
        SignatureRequise ok = new SignatureRequise("S1", "S1", PROFIL, List.of("L1", "L2"), 1);
        SignatureRequise future = new SignatureRequise("S2", "S2", PROFIL, List.of("L3"), 2);
        SignatureRequise vide = new SignatureRequise("S1", "S1", PROFIL, List.of(), 3);
        Etape b = new Etape("B", "B", null, PROFIL, null, List.of("A"), 0, List.of(l2, l1), List.of(ok, future, vide), List.of(new Champ("X", "X", TypeChamp.TEXTE, List.of(), false), new Champ("X", "X", TypeChamp.TEXTE, List.of(), false)), null);
        Etape c = new Etape("C", "C", null, PROFIL, null, List.of("B"), 0, List.of(new Livrable("L3", "L3", List.of(), true, null)), List.of(), List.of(), null);
        assertThat(codes(ControleProcessus.structure(List.of(a, b, c)))).containsExactlyInAnyOrder(
            "CHAMP_CHOIX_ABSENT",
            "LIVRABLE_CODE_DUPLIQUE",
            "SIGNATURE_CODE_DUPLIQUE",
            "CHAMP_CODE_DUPLIQUE",
            "SIGNATURE_PORTEE_INVALIDE",
            "SIGNATURE_SANS_LIVRABLE"
        );
    }

    @Test
    @DisplayName("Empreinte signée : SHA-256 des lignes livrable:document:empreinte, stable et sensible à l'ordre")
    void empreinte() {
        DocumentSigne d1 = new DocumentSigne("PV", "doc-1", EmpreinteSignature.sha256("a".getBytes(StandardCharsets.UTF_8)));
        DocumentSigne d2 = new DocumentSigne("NOTE", "doc-2", EmpreinteSignature.sha256("b".getBytes(StandardCharsets.UTF_8)));
        String e = EmpreinteSignature.calculer(List.of(d1, d2));
        assertThat(EmpreinteSignature.valide(e)).isTrue();
        assertThat(e).isEqualTo(EmpreinteSignature.calculer(List.of(d1, d2))).isNotEqualTo(EmpreinteSignature.calculer(List.of(d2, d1)));
        assertThat(EmpreinteSignature.sha256(new byte[0])).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(EmpreinteSignature.valide("ABC")).isFalse();
    }
}
