package cm.gimac.des.pilotage.domain.processus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReferenceProcessusTest {
    @Test
    void referenceDatéeEtCopieDeVersion() {
        LocalDate jour = LocalDate.parse("2026-10-28");
        assertThat(ReferenceProcessus.suivante("CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}", "DEMANDE_CHANGEMENT", jour, 42)).isEqualTo("CHG-2026-10-28-000042");
        assertThat(ReferenceProcessus.suivante("{CODE}-{AAAA}-{MM}-{JJ}-{NNNNNN}", "CONGE", jour, 1234567)).isEqualTo("CONGE-2026-10-28-1234567");
        assertThat(ReferenceProcessus.suivante(null, "CONGE", jour, 1)).isNull();
        assertThat(ReferenceProcessus.valider(" chg-{AAAA}-{MM}-{JJ}-{NNNNNN} ")).isEqualTo("CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}");
        assertThat(ReferenceProcessus.valider(" ")).isNull();
        Processus p = Processus.brouillon("DEMANDE_CHANGEMENT", "SSI", "test", Instant.parse("2026-10-28T08:00:00Z"));
        p.definirReference("CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}");
        p.decrire("Changement", "", "", 2, DemandeChangementConfigurationSansReference.etapes(), "test", Instant.parse("2026-10-28T08:00:00Z"));
        p.publier("test", "", false, Instant.parse("2026-10-28T08:00:00Z"));
        assertThat(p.getReferenceModele()).isEqualTo("CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}");
        assertThat(p.nouvelleVersion(2, "test", Instant.parse("2026-10-29T08:00:00Z")).getReferenceModele()).isEqualTo(p.getReferenceModele());
        assertThatThrownBy(() -> p.definirReference("AUTRE-{AAAA}-{MM}-{JJ}-{NNNNNN}")).isInstanceOf(PilotageException.class);
    }

    @Test
    void sequenceSansZerosEtDateFacultative() {
        LocalDate jour = LocalDate.parse("2026-11-04");
        assertThat(ReferenceProcessus.suivante("CHG-{AAAA}-{N}", "DEMANDE_CHANGEMENT", jour, 412)).isEqualTo("CHG-2026-412");
        assertThat(ReferenceProcessus.suivante("CHG-{AAAA}-{NNNNNN}", "DEMANDE_CHANGEMENT", jour, 7)).isEqualTo("CHG-2026-000007");
        assertThat(ReferenceProcessus.suivante("{CODE}-{AAAA}-{MM}-{N}", "CONGE", jour, 3)).isEqualTo("CONGE-2026-11-3");
        assertThat(ReferenceProcessus.valider("CHG-{AAAA}-{JJ}-{NNNNNN}")).isEqualTo("CHG-{AAAA}-{JJ}-{NNNNNN}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHG-2026", "CHG-{MM}-{JJ}-{NNNNNN}", "CHG-{N}", "CHG-{AAAA}-{MM}-{JJ}", "CHG-{AAAA}-{NN}", "chg/{AAAA}/{MM}/{JJ}/{NNNNNN}", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"})
    void gabaritsInvalides(String modele) {
        assertThatThrownBy(() -> ReferenceProcessus.valider(modele)).isInstanceOf(PilotageException.class);
    }

    @Test
    void referenceInstanceAffecteeUneSeuleFois() {
        Processus p = Processus.brouillon("DEMANDE_CHANGEMENT", "SSI", "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Test", "u", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        i.definirReference("CHG-2026-10-28-000001");
        assertThat(i.getReference()).isEqualTo("CHG-2026-10-28-000001");
        assertThatThrownBy(() -> i.definirReference("CHG-2026-10-28-000002")).isInstanceOf(PilotageException.class);
    }

    /** Évite toute dépendance à la configuration de production dans ce test du domaine. */
    private static final class DemandeChangementConfigurationSansReference {
        private static java.util.List<Etape> etapes() {
            return java.util.List.of(new Etape("DEMANDE", "Demande", "", new Intervenant(ModeIntervenant.LANCEUR, null, null, null, null), null, java.util.List.of(), 1, java.util.List.of(), java.util.List.of(), java.util.List.of(), new Position(0, 0)));
        }
    }
}
