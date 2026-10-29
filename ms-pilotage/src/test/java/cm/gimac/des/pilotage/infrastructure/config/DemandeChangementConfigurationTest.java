package cm.gimac.des.pilotage.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class DemandeChangementConfigurationTest {
    @Test
    void modeleCompletPublieUneSeuleFois() {
        ProcessusRepository processus = mock(ProcessusRepository.class);
        HistoriqueProcessusRepository historique = mock(HistoriqueProcessusRepository.class);
        TransactionTemplate transactions = new TransactionTemplate(mock(PlatformTransactionManager.class));
        DemandeChangementConfiguration configuration = new DemandeChangementConfiguration(processus, historique, transactions, Clock.fixed(Instant.parse("2026-10-28T08:00:00Z"), ZoneOffset.UTC));
        configuration.run(null);
        ArgumentCaptor<Processus> capt = ArgumentCaptor.forClass(Processus.class);
        verify(processus).save(capt.capture());
        Processus p = capt.getValue();
        assertThat(p.getCode()).isEqualTo(DemandeChangementConfiguration.CODE);
        assertThat(p.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(p.getReferenceModele()).isEqualTo(DemandeChangementConfiguration.REFERENCE);
        assertThat(p.getInitiateurs().regles()).containsExactly(RegleInitiateur.TOUT_LE_GIMAC);
        assertThat(p.getInitiateurs().initiateurIntervenant()).isTrue();
        assertThat(p.getFormulaire().champs()).hasSize(12);
        assertThat(p.getFormulaire().champs()).extracting(c -> c.type()).contains(TypeChampFormulaire.LISTE, TypeChampFormulaire.TEXTE, TypeChampFormulaire.TEXTE_LONG);
        assertThat(p.getEtapes()).extracting(e -> e.code()).containsExactly("IDENTIFICATION", "RAISON", "IMPACT", "IMPLEMENTATION", "SSI");
        assertThat(p.getEtapes().subList(0, 3)).allMatch(e -> e.intervenant().mode() == ModeIntervenant.LANCEUR);
        assertThat(p.getEtapes().get(3).intervenant().mode()).isEqualTo(ModeIntervenant.AU_LANCEMENT);
        assertThat(p.getEtapes().get(3).livrables().get(0).obligatoire()).isTrue();
        assertThat(p.getEtapes().get(4).intervenant().profil()).isEqualTo("ROLE_AGENT_SSI");
        assertThat(p.getEtapes().get(4).signatures()).hasSize(1);
        assertThat(p.getEtapes().get(4).champs()).extracting(c -> c.type()).containsExactly(TypeChamp.DATE, TypeChamp.DATE, TypeChamp.TEXTE, TypeChamp.CHOIX, TypeChamp.TEXTE);
        assertThat(ControleProcessus.structure(p.getEtapes())).isEmpty();
        verify(historique).save(any());
        when(processus.existsByCode(anyString())).thenReturn(true);
        configuration.run(null);
        verify(processus).save(any());
    }
}
