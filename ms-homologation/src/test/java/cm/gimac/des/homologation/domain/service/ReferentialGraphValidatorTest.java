package cm.gimac.des.homologation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.Anomalie;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.TypeAnomalie;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReferentialGraphValidatorTest {

    private final ReferentialGraphValidator validator = new ReferentialGraphValidator();

    static NoeudReferentiel n(String code, Segment s, String... preds) {
        return new NoeudReferentiel(code, code, s, List.of(preds));
    }

    static Stream<Arguments> cyclesDeReference() {
        return Stream.of(
            Arguments.of("boucle simple", List.of(n("A", Segment.BRANCHE, "A")), List.of("A", "A")),
            Arguments.of("cycle de longueur deux", List.of(n("A", Segment.BRANCHE, "B"), n("B", Segment.BRANCHE, "A")), List.of("A", "B", "A")),
            Arguments.of(
                "cycle long",
                List.of(n("A", Segment.BRANCHE, "B"), n("B", Segment.BRANCHE, "C"), n("C", Segment.BRANCHE, "A"), n("D", Segment.BRANCHE, "A")),
                List.of("A", "B", "C", "A")
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cyclesDeReference")
    @DisplayName("HOM-02 : le cycle est détecté et restitué sous forme de suite ordonnée de codes")
    void cycleRestitue(String cas, List<NoeudReferentiel> noeuds, List<String> cycleAttendu) {
        var rapport = validator.controler(noeuds);
        assertThat(rapport.conforme()).isFalse();
        assertThat(rapport.anomalies()).filteredOn(a -> a.type() == TypeAnomalie.CYCLE).singleElement().extracting(Anomalie::codes).isEqualTo(cycleAttendu);
    }

    @Test
    @DisplayName("HOM-02 : un prédécesseur de branche pour une opération du tronc commun amont est incohérent")
    void segmentIncoherent() {
        var rapport = validator.controler(List.of(n("DL-AMONT-03", Segment.TRONC_COMMUN_AMONT, "DL-BR-02"), n("DL-BR-02", Segment.BRANCHE)));
        assertThat(rapport.anomalies()).singleElement().satisfies(a -> {
            assertThat(a.type()).isEqualTo(TypeAnomalie.SEGMENT_INCOHERENT);
            assertThat(a.codes()).containsExactly("DL-AMONT-03", "DL-BR-02");
        });
    }

    @Test
    @DisplayName("HOM-02 : code dupliqué et prédécesseur inconnu sont tous recensés")
    void anomaliesRecensees() {
        var rapport = validator.controler(List.of(n("A", Segment.BRANCHE), n("A", Segment.BRANCHE), n("B", Segment.BRANCHE, "Z")));
        assertThat(rapport.anomalies()).extracting(Anomalie::type).containsExactlyInAnyOrder(TypeAnomalie.CODE_DUPLIQUE, TypeAnomalie.PREDECESSEUR_INCONNU);
    }

    @Test
    @DisplayName("HOM-02 : la publication d'un graphe non conforme est refusée par HOM_REFERENTIAL_GRAPH_INVALID")
    void validateRefuse() {
        assertThatThrownBy(() -> validator.validate(List.of(n("A", Segment.BRANCHE, "A"))))
            .isInstanceOfSatisfying(HomologationException.class, ex -> {
                assertThat(ex.code()).isEqualTo("HOM_REFERENTIAL_GRAPH_INVALID");
                assertThat(ex.status()).isEqualTo(409);
            });
    }

    @RepeatedTest(20)
    @DisplayName("HOM-02 : tout graphe accepté admet un ordre topologique vérifié arc par arc")
    void ordreVerifie(RepetitionInfo info) {
        List<NoeudReferentiel> dag = GraphSamples.dagAleatoire(120, 0.05, info.getCurrentRepetition());
        var rapport = validator.controler(dag);
        assertThat(rapport.conforme()).isTrue();
        Map<String, Integer> position = IntStream.range(0, rapport.ordre().size()).boxed().collect(Collectors.toMap(rapport.ordre()::get, i -> i));
        dag.forEach(op ->
            op.predecesseurs().forEach(p -> assertThat(position.get(p)).as("%s avant %s", p, op.code()).isLessThan(position.get(op.code())))
        );
    }

    @Test
    @DisplayName("HOM-02 : un référentiel de cinq cents opérations est contrôlé en moins d'une seconde")
    void chargeCinqCentsOperations() {
        List<NoeudReferentiel> dag = GraphSamples.dagAleatoire(500, 0.02, 42);
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> validator.controler(dag));
    }

    @Test
    @DisplayName("HOM-02 : les couches donnent le plus long chemin depuis une source")
    void couches() {
        var rapport = validator.controler(List.of(n("A", Segment.BRANCHE), n("B", Segment.BRANCHE, "A"), n("C", Segment.BRANCHE, "A", "B")));
        assertThat(rapport.ordre()).containsExactly("A", "B", "C");
        assertThat(rapport.couches()).containsEntry("A", 0).containsEntry("B", 1).containsEntry("C", 2);
    }
}
