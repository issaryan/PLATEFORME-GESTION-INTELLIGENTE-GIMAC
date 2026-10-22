package cm.gimac.des.document.infrastructure.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.IntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Relais de l'outbox de la sandbox (PostgreSQL par Testcontainers) : échec réessayé, rebut, interruption. */
@IntegrationTest
class RelaisEvenementsSandboxIT {

    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager tm;

    @SuppressWarnings("unchecked")
    final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    @BeforeEach
    void vider() {
        new TransactionTemplate(tm).executeWithoutResult(s -> jdbc.update("delete from sbx_evenement_sortant where statut <> 'ENVOYE'"));
    }

    UUID inscrire(int tentatives) {
        UUID id = UUID.randomUUID();
        new TransactionTemplate(tm).executeWithoutResult(s ->
            jdbc.update(
                "insert into sbx_evenement_sortant (id, type, cle, donnees, statut, tentatives, cree_le) values (?, 'document.document-sealed.v1', 'k', '{\"a\":1}', 'EN_ATTENTE', ?, ?)",
                id,
                tentatives,
                OffsetDateTime.parse("2026-10-22T09:00:00Z")
            )
        );
        return id;
    }

    RelaisEvenementsSandbox relais() {
        return new RelaisEvenementsSandbox(jdbcClient, kafka, new TransactionTemplate(tm), HORLOGE);
    }

    String statut(UUID id) {
        return jdbc.queryForObject("select statut || ':' || tentatives from sbx_evenement_sortant where id = ?", String.class, id);
    }

    @Test
    @DisplayName("Courtier en échec : événement réessayé, puis au rebut après dix essais (alerte d'exploitation)")
    void echecPuisRebut() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("courtier absent")));
        UUID a = inscrire(0);
        assertThat(relais().relayer()).isZero();
        assertThat(statut(a)).isEqualTo("EN_ATTENTE:1");
        UUID b = inscrire(RelaisEvenementsSandbox.TENTATIVES_MAX - 1);
        new TransactionTemplate(tm).executeWithoutResult(s -> jdbc.update("delete from sbx_evenement_sortant where id = ?", a));
        relais().relayer();
        assertThat(statut(b)).isEqualTo("REBUT:" + RelaisEvenementsSandbox.TENTATIVES_MAX);
    }

    @Test
    @DisplayName("Interruption pendant l'envoi : essai compté, fil marqué interrompu")
    void interruption() {
        CompletableFuture<SendResult<String, String>> interrompu = new CompletableFuture<>() {
            @Override
            public SendResult<String, String> get(long delai, TimeUnit unite) throws InterruptedException, ExecutionException {
                throw new InterruptedException("arrêt");
            }
        };
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(interrompu);
        UUID a = inscrire(0);
        assertThat(relais().relayer()).isZero();
        assertThat(Thread.interrupted()).isTrue();
        assertThat(statut(a)).isEqualTo("EN_ATTENTE:1");
    }

    @Test
    @DisplayName("Envoi réussi : statut ENVOYE, sujet de la plateforme dérivé du type")
    void envoi() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        UUID a = inscrire(0);
        assertThat(relais().relayer()).isEqualTo(1);
        assertThat(statut(a)).isEqualTo("ENVOYE:0");
        assertThat(RelaisEvenementsSandbox.sujet("document.document-sealed.v1")).isEqualTo("document.document");
    }
}
