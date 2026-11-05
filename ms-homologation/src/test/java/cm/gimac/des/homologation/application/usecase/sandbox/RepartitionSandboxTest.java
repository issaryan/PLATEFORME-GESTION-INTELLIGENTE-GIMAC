package cm.gimac.des.homologation.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.infrastructure.persistence.RepartitionSandboxRepository;
import cm.gimac.des.homologation.infrastructure.persistence.RepartitionSandboxRepository.ParCas;
import cm.gimac.des.homologation.infrastructure.persistence.RepartitionSandboxRepository.ParJour;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Répartition (D-121) : cas sans identifiant rendu nul, jours remis dans l'ordre du temps, fuseau du GIMAC annoncé. */
class RepartitionSandboxTest {

    final LiaisonRepository liaisons = mock(LiaisonRepository.class);
    final RepartitionSandboxRepository depot = mock(RepartitionSandboxRepository.class);
    final UUID liaison = UUID.randomUUID();

    static ParCas cas(String id, long def) {
        return new ParCas() {
            public String getIdentifiantCas() { return id; }
            public long getExecutions() { return 3; }
            public long getFavorables() { return 1; }
            public long getDefavorables() { return def; }
            public long getNonExecutes() { return 0; }
            public long getEnAttente() { return 2 - def; }
        };
    }

    static ParJour jour(String j) {
        return new ParJour() {
            public String getJour() { return j; }
            public long getExecutions() { return 2; }
            public long getFavorables() { return 1; }
            public long getDefavorables() { return 1; }
        };
    }

    @Test
    void rendLesCasEtLesJoursDansLeFuseauDuGimac() {
        when(liaisons.existsById(liaison)).thenReturn(true);
        when(depot.parCas(liaison, "C")).thenReturn(List.of(cas("CAS-1", 2), cas("", 1)));
        when(depot.parJour(liaison, "C", "Africa/Douala")).thenReturn(List.of(jour("2026-10-14"), jour("2026-10-13")));
        var r = new RepartitionSandbox(liaisons, depot, "Africa/Douala").repartition(liaison, "C");
        assertThat(r.fuseau()).isEqualTo("Africa/Douala");
        assertThat(r.parCas()).extracting(RepartitionSandbox.Cas::identifiantCas).containsExactly("CAS-1", null);
        assertThat(r.parCas().get(1).enAttente()).isEqualTo(1);
        assertThat(r.parJour()).extracting(RepartitionSandbox.Jour::jour).containsExactly("2026-10-13", "2026-10-14");
    }

    @Test
    void refuseUneLiaisonInconnueEtUnFuseauInvalide() {
        when(liaisons.existsById(liaison)).thenReturn(false);
        assertThatThrownBy(() -> new RepartitionSandbox(liaisons, depot, "Africa/Douala").repartition(liaison, "C")).isInstanceOf(HomologationException.class);
        assertThatThrownBy(() -> new RepartitionSandbox(liaisons, depot, "Lune/Mer")).isInstanceOf(java.time.DateTimeException.class);
    }
}
