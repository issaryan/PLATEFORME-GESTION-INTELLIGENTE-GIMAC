package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.sandbox.ReleveSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-EXI-15 : sbx-lecture.campaign-published.v1 déclenche une relecture, une seule fois par notification. */
class FinCampagneSandboxConsumerTest {

    ProcessedEventRepository traitees;
    IdempotentConsumer idempotent;
    ReleveSandbox releve;
    FinCampagneSandboxConsumer consommateur;

    @BeforeEach
    void init() {
        traitees = mock(ProcessedEventRepository.class);
        idempotent = mock(IdempotentConsumer.class);
        releve = mock(ReleveSandbox.class);
        when(releve.releverCampagneNotifiee(anyString())).thenReturn(new ReleveSandbox.Bilan(ReleveSandbox.Issue.REUSSIE, 1, 4));
        consommateur = new FinCampagneSandboxConsumer(traitees, idempotent, releve);
    }

    static String evenement(String id, String type, String data) {
        return "{\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"data\":" + data + "}";
    }

    @Test
    @DisplayName("Critère 1 : relecture de la campagne désignée, notification retenue comme traitée")
    void relectureDeclenchee() {
        consommateur.recevoir(evenement("n1", FinCampagneSandboxConsumer.TYPE, "{\"identifiantCampagne\":\"C1\",\"motif\":\"FIN_DE_CAMPAGNE\"}"));
        verify(releve).releverCampagneNotifiee("C1");
        verify(idempotent).handleOnce(eq("n1"), eq(FinCampagneSandboxConsumer.GROUPE), any());
    }

    @Test
    @DisplayName("Critère 2 : notification réémise à l'identique, aucun second traitement")
    void notificationReemise() {
        when(traitees.existsByEventIdAndConsumerGroup("n2", FinCampagneSandboxConsumer.GROUPE)).thenReturn(true);
        consommateur.recevoir(evenement("n2", FinCampagneSandboxConsumer.TYPE, "{\"identifiantCampagne\":\"C1\"}"));
        verify(releve, never()).releverCampagneNotifiee(anyString());
        verify(idempotent, never()).handleOnce(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Relecture en échec : notification retenue, la relève périodique reprendra la campagne")
    void relectureEnEchec() {
        when(releve.releverCampagneNotifiee("C9")).thenReturn(new ReleveSandbox.Bilan(ReleveSandbox.Issue.ECHEC, 0, 0));
        consommateur.recevoir(evenement("n3", FinCampagneSandboxConsumer.TYPE, "{\"identifiantCampagne\":\"C9\",\"motif\":\"ANNULATION\"}"));
        verify(idempotent).handleOnce(eq("n3"), eq(FinCampagneSandboxConsumer.GROUPE), any());
    }

    @Test
    @DisplayName("Autres types, sans identifiant ou sans campagne : ignorés")
    void ignores() {
        consommateur.recevoir(evenement("n4", "sbx-lecture.access-recorded.v1", "{\"identifiantCampagne\":\"C1\"}"));
        consommateur.recevoir(evenement("", FinCampagneSandboxConsumer.TYPE, "{\"identifiantCampagne\":\"C1\"}"));
        consommateur.recevoir("{\"type\":\"" + FinCampagneSandboxConsumer.TYPE + "\",\"data\":{}}");
        consommateur.recevoir(evenement("n5", FinCampagneSandboxConsumer.TYPE, "{}"));
        consommateur.recevoir(evenement("n6", FinCampagneSandboxConsumer.TYPE, "{\"identifiantCampagne\":\" \"}"));
        verify(releve, never()).releverCampagneNotifiee(anyString());
        verify(idempotent, never()).handleOnce(anyString(), anyString(), any());
    }
}
