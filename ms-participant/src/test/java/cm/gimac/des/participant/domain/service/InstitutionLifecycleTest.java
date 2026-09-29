package cm.gimac.des.participant.domain.service;

import static cm.gimac.des.participant.domain.enumeration.InstitutionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** PAR-02 (test unitaire exigé par le carnet) : exhaustivité de la table des transitions. */
class InstitutionLifecycleTest {

    /** Relation δ attendue : équation 6.27, adaptée par D-57. */
    private static final Set<List<InstitutionStatus>> DELTA = Set.of(
        List.of(CANDIDAT, ADHERENT),
        List.of(CANDIDAT, CLOS_SANS_SUITE),
        List.of(ADHERENT, EN_HOMOLOGATION),
        List.of(ADHERENT, CLOS_SANS_SUITE),
        List.of(EN_HOMOLOGATION, ACTIF),
        List.of(ACTIF, A_REGULARISER),
        List.of(ACTIF, SUSPENDU),
        List.of(ACTIF, RETIRE),
        List.of(A_REGULARISER, ACTIF),
        List.of(A_REGULARISER, SUSPENDU),
        List.of(SUSPENDU, ACTIF),
        List.of(SUSPENDU, RETIRE)
    );

    @Test
    @DisplayName("PAR-02 : chacun des 64 couples d'états est permis si et seulement s'il appartient à δ")
    void tableExhaustive() {
        for (InstitutionStatus de : InstitutionStatus.values()) {
            for (InstitutionStatus vers : InstitutionStatus.values()) {
                boolean attendu = DELTA.contains(List.of(de, vers));
                assertThat(InstitutionLifecycle.transitionsPermises(de).contains(vers)).as("%s vers %s", de, vers).isEqualTo(attendu);
                if (attendu) {
                    assertThatCode(() -> InstitutionLifecycle.verifierTransition(de, vers)).doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> InstitutionLifecycle.verifierTransition(de, vers))
                        .isInstanceOfSatisfying(ParticipantException.class, ex -> {
                            assertThat(ex.code()).isEqualTo("PAR_TRANSITION_NOT_ALLOWED");
                            assertThat(ex.status()).isEqualTo(409);
                        });
                }
            }
        }
    }

    @Test
    @DisplayName("PAR-02, critère 3 : un passage direct de candidat à actif restitue les transitions permises")
    void candidatVersActifRestitueLesPermises() {
        assertThatThrownBy(() -> InstitutionLifecycle.verifierTransition(CANDIDAT, ACTIF))
            .isInstanceOfSatisfying(ParticipantException.class, ex ->
                assertThat(ex.details()).containsEntry("permises", List.of("ADHERENT", "CLOS_SANS_SUITE"))
            );
    }

    @ParameterizedTest
    @EnumSource(value = InstitutionStatus.class, names = { "RETIRE", "CLOS_SANS_SUITE" })
    @DisplayName("RETIRE et CLOS_SANS_SUITE sont terminaux")
    void terminaux(InstitutionStatus etat) {
        assertThat(InstitutionLifecycle.estTerminal(etat)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = InstitutionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = { "RETIRE", "CLOS_SANS_SUITE" })
    void nonTerminaux(InstitutionStatus etat) {
        assertThat(InstitutionLifecycle.estTerminal(etat)).isFalse();
    }

    @Test
    @DisplayName("D-57 : EN_HOMOLOGATION est atteint à l'ouverture de la liaison, jamais sur proposition")
    void enHomologationAutomatique() {
        assertThatThrownBy(() -> InstitutionLifecycle.verifierProposable(ADHERENT, EN_HOMOLOGATION))
            .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_TRANSITION_AUTOMATIC"));
        assertThatCode(() -> InstitutionLifecycle.verifierProposable(CANDIDAT, ADHERENT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> InstitutionLifecycle.verifierProposable(CANDIDAT, ACTIF)).isInstanceOf(ParticipantException.class);
    }

    @Test
    @DisplayName("RG-14 : la levée de suspension exige deux approbations, toute autre transition une seule")
    void approbationsRequises() {
        for (List<InstitutionStatus> t : DELTA) {
            int attendu = t.equals(List.of(SUSPENDU, ACTIF)) ? 2 : 1;
            assertThat(InstitutionLifecycle.approbationsRequises(t.get(0), t.get(1))).as("%s", t).isEqualTo(attendu);
        }
    }

    @Test
    void ensembleRenduNonModifiable() {
        Set<InstitutionStatus> permises = InstitutionLifecycle.transitionsPermises(CANDIDAT);
        assertThatThrownBy(() -> permises.add(ACTIF)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(EnumSet.copyOf(permises)).containsExactlyInAnyOrder(ADHERENT, CLOS_SANS_SUITE);
    }
}
