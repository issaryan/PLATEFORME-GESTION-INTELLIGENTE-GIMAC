package cm.gimac.des.pilotage.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.pilotage.application.port.in.ApprobationsTransitionsUseCase;
import cm.gimac.des.pilotage.application.port.in.ApprobationsTransitionsUseCase.Proposition;
import cm.gimac.des.pilotage.domain.ProcessedEvent;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture des transitions de MS-PARTICIPANT (D-98) : aiguillage, idempotence, messages ignorés, refus et indisponibilité. */
class TransitionsParticipantConsumerTest {

    private final ApprobationsTransitionsUseCase approbations = mock(ApprobationsTransitionsUseCase.class);
    private final ProcessedEventRepository traites = mock(ProcessedEventRepository.class);
    private final TransitionsParticipantConsumer consommateur = new TransitionsParticipantConsumer(
        approbations,
        traites,
        new ObjectMapper(),
        Clock.fixed(Instant.parse("2026-10-08T08:00:00Z"), ZoneOffset.UTC)
    );

    private static String evenement(String id, String type, String data) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"data\":" + data + "}";
    }

    @Test
    @DisplayName("Proposition : tâche demandée avec les champs de la charge, événement marqué traité")
    void proposee() {
        String id = UUID.randomUUID().toString();
        consommateur.recevoir(
            evenement(
                id,
                TransitionsParticipantConsumer.PROPOSEE,
                "{\"institutionId\":\"i-1\",\"transitionId\":\"t-1\",\"de\":\"CANDIDAT\",\"vers\":\"ADHERENT\",\"motif\":\"Dossier complet\"," +
                "\"demandeurId\":\"charge\",\"approbationsRequises\":1,\"institutionNom\":\"Banque B\",\"lien\":\"/des/institutions/i-1?onglet=transitions\"}"
            )
        );
        verify(approbations).proposee(
            new Proposition("i-1", "t-1", "CANDIDAT", "ADHERENT", "Dossier complet", "charge", 1, "Banque B", "/des/institutions/i-1?onglet=transitions")
        );
        verify(traites).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Approbation définitive ou partielle (défaut : définitive), refus : tâche close ou remise à prendre")
    void approuveeEtRejetee() {
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), TransitionsParticipantConsumer.APPROUVEE, "{\"institutionId\":\"i-1\",\"transitionId\":\"t-1\",\"definitive\":false}"));
        verify(approbations).approuvee("i-1", "t-1", false);
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), TransitionsParticipantConsumer.APPROUVEE, "{\"institutionId\":\"i-1\",\"transitionId\":\"t-2\"}"));
        verify(approbations).approuvee("i-1", "t-2", true);
        consommateur.recevoir(evenement("pas-un-uuid", TransitionsParticipantConsumer.REJETEE, "{\"institutionId\":\"i-1\",\"transitionId\":\"t-3\",\"motif\":\"Non\"}"));
        verify(approbations).rejetee("i-1", "t-3");
    }

    @Test
    @DisplayName("Déjà traité, illisible, d'un autre type ou sans transition : ignoré")
    void ignores() {
        String id = UUID.randomUUID().toString();
        when(traites.existsByEventId(UUID.fromString(id))).thenReturn(true);
        consommateur.recevoir(evenement(id, TransitionsParticipantConsumer.REJETEE, "{\"institutionId\":\"i-1\",\"transitionId\":\"t-1\"}"));
        consommateur.recevoir("{pas du json");
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), "participant.status-changed.v1", "{\"institutionId\":\"i-1\",\"transitionId\":\"t-1\"}"));
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), TransitionsParticipantConsumer.PROPOSEE, "{\"institutionId\":\"i-1\",\"transitionId\":\" \"}"));
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), TransitionsParticipantConsumer.PROPOSEE, "{\"transitionId\":\"t-1\",\"institutionId\":null}"));
        verifyNoInteractions(approbations);
        verify(traites, never()).save(any());
    }

    @Test
    @DisplayName("Refus définitif (4xx) : journalisé et marqué traité ; indisponibilité (503) : remonte pour être rejouée")
    void refusEtIndisponibilite() {
        when(approbations.rejetee("i-1", "t-1")).thenThrow(PilotageException.champ("origine.reference", "Référence trop longue"));
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), TransitionsParticipantConsumer.REJETEE, "{\"institutionId\":\"i-1\",\"transitionId\":\"t-1\"}"));
        verify(traites).save(any(ProcessedEvent.class));

        when(approbations.proposee(any())).thenThrow(PilotageException.indisponible("PIL_ORGANISATION_INDISPONIBLE", "MS-IAM injoignable"));
        String message = evenement(UUID.randomUUID().toString(), TransitionsParticipantConsumer.PROPOSEE, "{\"institutionId\":\"i-1\",\"transitionId\":\"t-2\"}");
        assertThatThrownBy(() -> consommateur.recevoir(message)).isInstanceOf(PilotageException.class);
        verify(traites).save(any(ProcessedEvent.class));
    }
}
