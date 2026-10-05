package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Lecture des enveloppes CloudEvents et idempotence du consommateur (table processed_event). */
class EvenementsNotificationsConsumerTest {

    ProcessedEventRepository traites;
    RecevoirEvenementsUseCase evenements;
    EvenementsNotificationsConsumer consommateur;

    @BeforeEach
    void init() {
        traites = mock(ProcessedEventRepository.class);
        evenements = mock(RecevoirEvenementsUseCase.class);
        consommateur = new EvenementsNotificationsConsumer(new IdempotentConsumer(traites), evenements, new ObjectMapper());
    }

    @Test
    @DisplayName("collaboration.message-sent.v1 : charge lue (destinataires, auteur, instant, événement de groupe)")
    void messageEnvoye() throws Exception {
        consommateur.recevoir(
            "{\"specversion\":\"1.0\",\"id\":\"e-1\",\"type\":\"collaboration.message-sent.v1\",\"data\":{\"conversationId\":\"c-1\",\"messageId\":\"m-1\"," +
            "\"auteurId\":\"paul\",\"destinataires\":[\"aminata\",\"serge\"],\"envoyeLe\":\"2026-10-05T08:00:00Z\",\"evenement\":null}}"
        );
        ArgumentCaptor<MessageEnvoye> m = ArgumentCaptor.forClass(MessageEnvoye.class);
        verify(evenements).messageEnvoye(m.capture());
        assertThat(m.getValue()).isEqualTo(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "serge"), Instant.parse("2026-10-05T08:00:00Z"), null));
        verify(traites).save(any());
    }

    @Test
    @DisplayName("Idempotence : un événement déjà traité par le groupe est acquitté sans effet")
    void idempotence() throws Exception {
        String e = "{\"id\":\"e-2\",\"type\":\"pilotage.task-assigned.v1\",\"data\":{\"tacheId\":\"t-1\",\"responsableId\":\"aminata\",\"acteurId\":\"paul\",\"le\":1759651200.5}}";
        AtomicBoolean deja = new AtomicBoolean(false);
        when(traites.existsByEventIdAndConsumerGroup("e-2", EvenementsNotificationsConsumer.GROUPE)).thenAnswer(i -> deja.get());
        when(traites.save(any())).thenAnswer(i -> {
            deja.set(true);
            return i.getArgument(0);
        });
        consommateur.recevoir(e);
        consommateur.recevoir(e);
        ArgumentCaptor<TacheAffectee> t = ArgumentCaptor.forClass(TacheAffectee.class);
        verify(evenements, times(1)).tacheAffectee(t.capture());
        assertThat(t.getValue().responsableId()).isEqualTo("aminata");
        assertThat(t.getValue().le()).isEqualTo(Instant.ofEpochSecond(1759651200L, 500_000_000L));
        verify(traites, times(1)).save(any());
    }

    @Test
    @DisplayName("pilotage.report-submitted.v1 lu ; autres types (task-created, report-visaed) et enveloppes sans identifiant ignorés")
    void rapportEtTypesIgnores() throws Exception {
        consommateur.recevoir(
            "{\"id\":\"e-3\",\"type\":\"pilotage.report-submitted.v1\",\"data\":{\"rapportId\":\"r-1\",\"type\":\"QUOTIDIEN\",\"periode\":\"2026-10-05\"," +
            "\"etat\":\"SOUMIS\",\"auteurId\":\"serge\",\"destinataireId\":\"paul\",\"acteurId\":\"serge\",\"le\":\"2026-10-05T09:00:00Z\"}}"
        );
        verify(evenements).rapportSoumis(new RapportSoumis("r-1", "QUOTIDIEN", "2026-10-05", "serge", "paul", "serge", Instant.parse("2026-10-05T09:00:00Z")));
        consommateur.recevoir("{\"id\":\"e-4\",\"type\":\"pilotage.task-created.v1\",\"data\":{}}");
        consommateur.recevoir("{\"id\":\"e-5\",\"type\":\"pilotage.report-visaed.v1\",\"data\":{}}");
        consommateur.recevoir("{\"type\":\"collaboration.message-sent.v1\",\"data\":{}}");
        verify(evenements, never()).tacheAffectee(any());
        verify(evenements, never()).messageEnvoye(any());
        verify(traites, never()).existsByEventIdAndConsumerGroup(eq("e-4"), anyString());
    }

    @Test
    @DisplayName("Donnée transmise en chaîne JSON : lue comme un objet")
    void donneeEnChaine() throws Exception {
        RecevoirEvenementsUseCase autre = mock(RecevoirEvenementsUseCase.class);
        new EvenementsNotificationsConsumer(new IdempotentConsumer(traites), autre, new ObjectMapper()).recevoir(
            "{\"id\":\"e-6\",\"type\":\"collaboration.message-sent.v1\",\"data\":\"{\\\"conversationId\\\":\\\"c-9\\\",\\\"auteurId\\\":\\\"paul\\\",\\\"destinataires\\\":[\\\"serge\\\"]}\"}"
        );
        ArgumentCaptor<MessageEnvoye> m = ArgumentCaptor.forClass(MessageEnvoye.class);
        verify(autre).messageEnvoye(m.capture());
        assertThat(m.getValue().conversationId()).isEqualTo("c-9");
        assertThat(m.getValue().destinataires()).containsExactly("serge");
        verifyNoInteractions(evenements);
    }
}
