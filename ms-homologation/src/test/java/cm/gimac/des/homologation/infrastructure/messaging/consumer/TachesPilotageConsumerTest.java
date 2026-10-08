package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.PrisesFonctions;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prise d'une tâche à prendre (D-94, §8.8) : pilotage.task-taken.v1 de ce service, liaison lue dans le lien, idempotent. */
class TachesPilotageConsumerTest {

    static final UUID LIAISON = UUID.fromString("be2b201b-4db6-48de-a918-b36f96175658");

    static String evenement(String type, String service, String lien, String responsable) {
        return (
            "{\"id\":\"e1\",\"type\":\"" + type + "\",\"data\":{\"origine\":{\"type\":\"OPERATION\",\"reference\":\"LIA-2026-001/AFFECTER/DDRM\",\"service\":" +
            (service == null ? "null" : "\"" + service + "\"") + ",\"lien\":" + (lien == null ? "null" : "\"" + lien + "\"") + "},\"responsableId\":" +
            (responsable == null ? "null" : "\"" + responsable + "\"") + ",\"acteurId\":\"a1\",\"mode\":\"PRISE\",\"permissionsRequises\":[\"HOM_X\"]}}"
        );
    }

    @Test
    @DisplayName("Prise d'une tâche de ce service traitée une fois ; autre type, autre service, lien ou responsable absent : ignorés")
    void prises() {
        IdempotentConsumer idempotent = mock(IdempotentConsumer.class);
        PrisesFonctions prises = mock(PrisesFonctions.class);
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenAnswer(i -> {
            ((Runnable) i.getArgument(2)).run();
            return true;
        });
        TachesPilotageConsumer c = new TachesPilotageConsumer(idempotent, prises);
        String lien = "/des/parcours/" + LIAISON + "?onglet=etapes&fonction=DDRM";
        c.recevoir(evenement("pilotage.task-taken.v1", "homologation", lien, "u1"));
        verify(prises).affecter(LIAISON, "LIA-2026-001/AFFECTER/DDRM", "u1", "a1", "PRISE");
        c.recevoir(evenement("pilotage.task-assigned.v1", "homologation", lien, "u2"));
        c.recevoir(evenement("pilotage.task-taken.v1", "certification", lien, "u3"));
        c.recevoir(evenement("pilotage.task-taken.v1", "homologation", "/des/autre/" + LIAISON, "u4"));
        c.recevoir(evenement("pilotage.task-taken.v1", "homologation", null, "u5"));
        c.recevoir(evenement("pilotage.task-taken.v1", "homologation", lien, null));
        verifyNoMoreInteractions(prises);
        org.mockito.Mockito.doReturn(false).when(idempotent).handleOnce(anyString(), anyString(), any());
        c.recevoir(evenement("pilotage.task-taken.v1", "homologation", lien, "u6"));
        verify(prises, never()).affecter(any(), any(), org.mockito.ArgumentMatchers.eq("u6"), any(), any());
    }

    @Test
    @DisplayName("Liaison du lien d'origine : /des/parcours/<uuid> suivi ou non de paramètres ; sinon aucune")
    void liaison() {
        assertThat(TachesPilotageConsumer.liaison("/des/parcours/" + LIAISON)).isEqualTo(LIAISON);
        assertThat(TachesPilotageConsumer.liaison("/des/parcours/" + LIAISON + "?onglet=etapes")).isEqualTo(LIAISON);
        assertThat(TachesPilotageConsumer.liaison("/des/parcours/pas-un-uuid")).isNull();
        assertThat(TachesPilotageConsumer.liaison(null)).isNull();
    }
}
