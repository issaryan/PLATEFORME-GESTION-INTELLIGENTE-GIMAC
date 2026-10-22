package cm.gimac.des.document.config;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.PiecesSandbox;
import cm.gimac.des.document.application.usecase.DepotPieceSandboxService;
import cm.gimac.des.document.application.usecase.RechercheParEmpreinteService;
import cm.gimac.des.document.application.usecase.TypesPiecesSandboxService;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import cm.gimac.des.document.infrastructure.sandbox.PiecesSandboxJdbc;
import cm.gimac.des.document.infrastructure.sandbox.RelaisEvenementsSandbox;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pièces de la Sandbox GIMAC (D-120, US-EXI-08) : catalogue et pièces en base, dépôt et catalogue, relais de l'outbox
 * (gimac.document.sandbox.relais.actif, actif par défaut ; délai gimac.document.sandbox.relais.delai, 1 s).
 */
@Configuration
public class SandboxDocumentConfiguration {

    @Bean
    public PiecesSandbox piecesSandbox(JdbcClient jdbc) {
        return new PiecesSandboxJdbc(jdbc);
    }

    @Bean
    public DepotPieceSandboxService depotPieceSandboxService(PiecesSandbox pieces, DeposerDocumentUseCase depot, ExtracteurTexte extracteur) {
        return new DepotPieceSandboxService(pieces, depot, extracteur, Clock.systemUTC());
    }

    @Bean
    public TypesPiecesSandboxService typesPiecesSandboxService(PiecesSandbox pieces) {
        return new TypesPiecesSandboxService(pieces, Clock.systemUTC());
    }

    @Bean
    public RechercheParEmpreinteService rechercheParEmpreinteService(DepotDocumentRepository documents) {
        return new RechercheParEmpreinteService(documents);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gimac.document.sandbox.relais", name = "actif", havingValue = "true", matchIfMissing = true)
    public RelaisEvenementsSandbox relaisEvenementsSandbox(JdbcClient jdbc, KafkaTemplate<String, String> kafka, PlatformTransactionManager tm) {
        return new RelaisEvenementsSandbox(jdbc, kafka, new TransactionTemplate(tm), Clock.systemUTC());
    }

    /** Déclencheur du relais, séparé pour que le relais reste testable sans planificateur. */
    @Bean
    @ConditionalOnProperty(prefix = "gimac.document.sandbox.relais", name = "actif", havingValue = "true", matchIfMissing = true)
    public DeclencheurRelais declencheurRelaisSandbox(RelaisEvenementsSandbox relais) {
        return new DeclencheurRelais(relais);
    }

    public static class DeclencheurRelais {

        private final RelaisEvenementsSandbox relais;

        DeclencheurRelais(RelaisEvenementsSandbox relais) {
            this.relais = relais;
        }

        @Scheduled(fixedDelayString = "${gimac.document.sandbox.relais.delai:1000}")
        public void declencher() {
            relais.relayer();
        }
    }
}
