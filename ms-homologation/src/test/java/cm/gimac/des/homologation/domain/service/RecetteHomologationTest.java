package cm.gimac.des.homologation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.ExigencesCertification;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.TypeAnomalie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Recette du lot HOM (docs/recette/homologation.md) : anomalies reproduites côté MS-HOMOLOGATION. */
class RecetteHomologationTest {

    private static NoeudReferentiel noeud(String code, List<String> preds, Fonction r, Fonction v, Geste g, Jalon j, String declencheur) {
        return new NoeudReferentiel(
            code,
            code,
            Segment.values()[0],
            preds,
            new NoeudReferentiel.Modele(r, v, g, j, declencheur, false, null, List.of(), 3, List.of(), List.of())
        );
    }

    @Test
    @DisplayName("HOM-02 : GESTE_CERTIFICATION_ABSENT, motif en libellés (jalon, geste) et publication refusée")
    void gesteCertificationAbsentLisible() {
        ReferentialGraphValidator v = new ReferentialGraphValidator(
            new TopologicalSorter(),
            new ExigencesCertification(Map.of(Jalon.CERTIFIEE, List.of(Declencheurs.PV_ETABLI, Declencheurs.PV_SIGNE)), Map.of())
        );
        // Agrégateur sans l'opération qui établit le procès-verbal (A4 retirée), A5 porte le jalon CERTIFIEE.
        List<NoeudReferentiel> noeuds = List.of(
            noeud("A3", List.of(), Fonction.SNA, Fonction.CONDUCTEUR, Geste.CONSTATER_COUVERTURE, Jalon.APTE_CERTIFICATION, null),
            noeud("A5", List.of("A3"), Fonction.CHEF_DES, Fonction.CONDUCTEUR, Geste.EVENEMENT_EXTERNE, Jalon.CERTIFIEE, Declencheurs.PV_SIGNE)
        );
        var anomalie = v
            .controler(noeuds, false)
            .anomalies()
            .stream()
            .filter(a -> a.type() == TypeAnomalie.GESTE_CERTIFICATION_ABSENT)
            .findFirst()
            .orElseThrow();
        assertThat(anomalie.codes()).containsExactly("CERTIFIEE", Declencheurs.PV_ETABLI);
        assertThat(anomalie.motif()).isEqualTo(
            "Le jalon « Certifiée » exige une opération qui porte chaque geste de certification : Procès-verbal de certification établi."
        );
        assertThatThrownBy(() -> v.validate(noeuds, false)).isInstanceOfSatisfying(HomologationException.class, e ->
            assertThat(e.code()).isEqualTo("HOM_REFERENTIAL_GRAPH_INVALID")
        );
    }

    @Test
    @DisplayName("HOM-17 : la fonction « Chefs des deux départements » est reliée aux profils des deux chefs")
    void chefsDesDeuxDepartements() {
        var d = CatalogueFonctions.REPRISES.stream().filter(x -> x.code().equals("CHEFS_DEPARTEMENT")).findFirst().orElseThrow();
        assertThat(d.profils()).containsExactly("ROLE_CHEF_DES", "ROLE_CHEF_DDRM");
        // Elle reste tenue par les chefs à ce titre seulement : jamais déduite comme fonction propre d'un profil.
        assertThat(Fonction.duProfil("ROLE_CHEF_DDRM")).contains(Fonction.CHEF_DDRM);
    }
}
