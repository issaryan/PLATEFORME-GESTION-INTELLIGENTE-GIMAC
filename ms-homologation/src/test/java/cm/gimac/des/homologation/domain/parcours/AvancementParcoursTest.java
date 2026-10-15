package cm.gimac.des.homologation.domain.parcours;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Avancement global pondéré par étape (D-110). */
class AvancementParcoursTest {

    private static Parcours faitJusquA(String rang) {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        for (OperationParcours o : p.operations()) {
            o.setEtat(EtatOperationParcours.FAITE);
            if (o.getRang().equals(rang)) {
                break;
            }
        }
        return p;
    }

    @Test
    void poidsSurCentEtSuiviHorsCompte() {
        assertThat(AvancementParcours.POIDS.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(100);
        assertThat(AvancementParcours.POIDS).doesNotContainKey(EtapeCode.SUIVI);
    }

    @Test
    void ouvertureSeule_priseDeContactEtAdhesionAcquises() {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        Couverture a = p.avancement();
        assertThat(a.calculable()).isTrue();
        assertThat(a.taux()).isEqualTo(5);
        assertThat(a.denominateur()).isEqualTo(100);
    }

    @Test
    void interconnexionFaite_trenteCinqPourCent() {
        assertThat(faitJusquA("15").avancement().taux()).isEqualTo(35);
    }

    @Test
    void essaisMesuresSurLaSandbox_relevementDeLEtapeDesTests() {
        Parcours p = faitJusquA("15").avecMesures(Map.of(AvancementParcours.ESSAIS_GLOBAL, 40));
        assertThat(p.avancement().taux()).isEqualTo(49);
    }

    @Test
    void sansMesureGlobale_moyenneDesServicesBornee() {
        Parcours p = faitJusquA("15").avecMesures(Map.of("A", 20, "B", 140));
        // (20 + 100) / 2 = 60 % des essais : 35 + 21.
        assertThat(p.avancement().taux()).isEqualTo(56);
        assertThat(AvancementParcours.essais(Map.of())).isNull();
        assertThat(AvancementParcours.essais(null)).isNull();
        assertThat(AvancementParcours.essais(Map.of(AvancementParcours.ESSAIS_GLOBAL, -5))).isZero();
    }

    @Test
    void essaisFaits_laMesureNeDiminuePasLEtape() {
        Parcours p = faitJusquA("16").avecMesures(Map.of(AvancementParcours.ESSAIS_GLOBAL, 10));
        assertThat(p.avancement().taux()).isEqualTo(70);
    }

    @Test
    void certificationSignee_quatreVingtPourCent() {
        assertThat(faitJusquA("19").avancement().taux()).isEqualTo(80);
    }

    @Test
    void operationsSansObjet_exclues() {
        Parcours p = faitJusquA("15");
        Set<String> sansObjet = Set.of("17", "18", "19");
        p.operations().stream().filter(o -> sansObjet.contains(o.getRang())).forEach(o -> o.setEtat(EtatOperationParcours.SANS_OBJET));
        // Certification sans opération applicable : son poids sort du total (35 / 90).
        assertThat(p.avancement().taux()).isEqualTo(39);
    }

    @Test
    void enProduction_centPourCent() {
        Couverture c = AvancementParcours.de(StatutParcours.EN_PRODUCTION, List.of(), Map.of());
        assertThat(c.taux()).isEqualTo(100);
        assertThat(c.complete()).isTrue();
    }

    @Test
    void sansOperation_nonCalculable() {
        Couverture c = AvancementParcours.de(StatutParcours.EN_TEST, List.of(), Map.of(AvancementParcours.ESSAIS_GLOBAL, 50));
        assertThat(c.calculable()).isFalse();
        assertThat(c.taux()).isNull();
    }

    @Test
    void essaisSansOperationDeTests_mesureSeule() {
        Parcours p = faitJusquA("15");
        List<OperationParcours> sansTests = p.operations().stream().filter(o -> o.getEtape() != EtapeCode.TESTS).toList();
        assertThat(AvancementParcours.de(StatutParcours.EN_TEST, sansTests, Map.of(AvancementParcours.ESSAIS_GLOBAL, 100)).taux()).isEqualTo(70);
        assertThat(AvancementParcours.de(StatutParcours.EN_TEST, sansTests, Map.of()).taux()).isEqualTo(54);
    }
}
