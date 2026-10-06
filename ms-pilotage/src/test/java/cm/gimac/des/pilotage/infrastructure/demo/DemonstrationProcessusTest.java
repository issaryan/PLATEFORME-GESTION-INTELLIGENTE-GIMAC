package cm.gimac.des.pilotage.infrastructure.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Jeu de démonstration (profil dev) : « Ouverture d'un flux réseau » publié une seule fois. */
class DemonstrationProcessusTest {

    @Test
    @DisplayName("Publie OUVERTURE_FLUX_RESEAU du pôle réseau s'il est absent, sinon ne fait rien")
    void charge() {
        ProcessusRepository processus = mock(ProcessusRepository.class);
        HistoriqueProcessusRepository historique = mock(HistoriqueProcessusRepository.class);
        TransactionTemplate t = new TransactionTemplate(mock(PlatformTransactionManager.class));
        Clock horloge = Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC);
        DemonstrationProcessus d = new DemonstrationProcessus(processus, historique, t, horloge);
        when(processus.existsByCode(DemonstrationProcessus.CODE)).thenReturn(false);
        d.run(null);
        ArgumentCaptor<Processus> p = ArgumentCaptor.forClass(Processus.class);
        verify(processus).save(p.capture());
        assertThat(p.getValue().getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(p.getValue().getUnite()).isEqualTo("POLE_RESEAU");
        assertThat(p.getValue().getEtapes()).hasSize(4);
        when(processus.existsByCode(DemonstrationProcessus.CODE)).thenReturn(true);
        d.run(null);
        verify(processus).save(any());
        verify(historique, never()).delete(any());
    }
}
