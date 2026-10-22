package cm.gimac.des.document.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.PiecesSandbox;
import cm.gimac.des.document.infrastructure.sandbox.PiecesSandboxJdbc;
import cm.gimac.des.document.infrastructure.sandbox.RelaisEvenementsSandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Assemblage des pièces de la sandbox (D-120, US-EXI-08). */
class SandboxDocumentConfigurationTest {

    @Test
    @DisplayName("Adaptateur JDBC, cas d'utilisation, relais et déclencheur du relais")
    @SuppressWarnings("unchecked")
    void assemblage() {
        SandboxDocumentConfiguration c = new SandboxDocumentConfiguration();
        PiecesSandbox pieces = c.piecesSandbox(mock(JdbcClient.class));
        assertThat(pieces).isInstanceOf(PiecesSandboxJdbc.class);
        assertThat(c.depotPieceSandboxService(pieces, mock(DeposerDocumentUseCase.class), mock(ExtracteurTexte.class))).isNotNull();
        assertThat(c.typesPiecesSandboxService(pieces)).isNotNull();
        assertThat(c.rechercheParEmpreinteService(mock(cm.gimac.des.document.repository.DepotDocumentRepository.class))).isNotNull();
        assertThat(c.relaisEvenementsSandbox(mock(JdbcClient.class), mock(KafkaTemplate.class), mock(PlatformTransactionManager.class))).isNotNull();
        RelaisEvenementsSandbox relais = mock(RelaisEvenementsSandbox.class);
        c.declencheurRelaisSandbox(relais).declencher();
        verify(relais).relayer();
    }
}
