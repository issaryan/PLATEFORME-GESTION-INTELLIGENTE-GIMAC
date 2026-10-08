package cm.gimac.des.pilotage.infrastructure.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Jeu de démonstration (profil dev) : « Ouverture d'un flux réseau » et les demandes D-98 publiés une seule fois. */
class DemonstrationProcessusTest {

    @Test
    @DisplayName("Publie OUVERTURE_FLUX_RESEAU et les deux demandes ouvertes à tout le GIMAC (D-98) s'ils sont absents, sinon ne fait rien")
    void charge() {
        ProcessusRepository processus = mock(ProcessusRepository.class);
        HistoriqueProcessusRepository historique = mock(HistoriqueProcessusRepository.class);
        TransactionTemplate t = new TransactionTemplate(mock(PlatformTransactionManager.class));
        Clock horloge = Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC);
        DemonstrationProcessus d = new DemonstrationProcessus(processus, historique, t, horloge);
        when(processus.existsByCode(anyString())).thenReturn(false);
        d.run(null);
        ArgumentCaptor<Processus> p = ArgumentCaptor.forClass(Processus.class);
        verify(processus, times(3)).save(p.capture());
        Map<String, Processus> parCode = p.getAllValues().stream().collect(Collectors.toMap(Processus::getCode, x -> x));
        Processus flux = parCode.get(DemonstrationProcessus.CODE);
        assertThat(flux.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(flux.getUnite()).isEqualTo("POLE_RESEAU");
        assertThat(flux.getEtapes()).hasSize(4);
        assertThat(flux.getInitiateurs().regles()).containsExactly(RegleInitiateur.MEMBRES_UNITE);
        Processus conge = parCode.get(DemonstrationProcessus.CODE_CONGE);
        assertThat(conge.getUnite()).isEqualTo("RH");
        assertThat(conge.getInitiateurs().regles()).containsExactly(RegleInitiateur.TOUT_LE_GIMAC);
        assertThat(conge.getFormulaire().champs()).extracting(c -> c.type()).containsExactly(TypeChampFormulaire.PERIODE, TypeChampFormulaire.TEXTE_LONG, TypeChampFormulaire.PIECE_JOINTE);
        assertThat(conge.getFormulaire().champs().get(2).obligatoire()).isFalse();
        Processus ip = parCode.get(DemonstrationProcessus.CODE_IP);
        assertThat(ip.getUnite()).isEqualTo("POLE_RESEAU");
        assertThat(ip.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(ip.getFormulaire().champs()).extracting(c -> c.code()).containsExactly("EQUIPEMENT", "ADRESSE_ACTUELLE", "ADRESSE_DEMANDEE", "JUSTIFICATION");
        assertThat(ip.getEtapes()).extracting(e -> e.code()).containsExactly("ANALYSE", "VALIDATION", "EXECUTION", "CLOTURE");
        assertThat(ControleProcessus.structure(ip.getEtapes())).isEmpty();
        assertThat(ControleProcessus.structure(conge.getEtapes())).isEmpty();
        when(processus.existsByCode(anyString())).thenReturn(true);
        d.run(null);
        verify(processus, times(3)).save(any());
        verify(historique, never()).delete(any());
    }
}
