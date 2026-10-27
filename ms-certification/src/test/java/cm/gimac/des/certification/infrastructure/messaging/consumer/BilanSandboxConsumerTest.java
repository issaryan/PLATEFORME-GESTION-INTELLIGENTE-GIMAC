package cm.gimac.des.certification.infrastructure.messaging.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.certification.application.usecase.sandbox.SocleEvaluationSandbox;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consommation idempotente et rétrocompatible du bilan Sandbox (US-EXI-25). */
class BilanSandboxConsumerTest {

    static final UUID LIAISON = UUID.randomUUID();
    static final String A = "a".repeat(64);

    static String evenement(String id, String donnees) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"homologation.campaign-result-imported.v1\",\"data\":" + donnees + "}";
    }

    @Test
    @DisplayName("Bilan courant et événement historique sans empreinte transmis au cas d'utilisation")
    void bilanCourantEtHistorique() {
        IdempotentConsumer idempotent = org.mockito.Mockito.mock(IdempotentConsumer.class);
        SocleEvaluationSandbox socles = org.mockito.Mockito.mock(SocleEvaluationSandbox.class);
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenAnswer(i -> {
            i.<Runnable>getArgument(2).run();
            return true;
        });
        BilanSandboxConsumer consumer = new BilanSandboxConsumer(idempotent, socles);

        consumer.recevoir(evenement("evt-1", "{\"liaisonId\":\"" + LIAISON + "\",\"versionBilan\":2,\"empreinteBilan\":\"" + A + "\"}"));
        consumer.recevoir(evenement("evt-2", "{\"liaisonId\":\"" + LIAISON + "\"}"));

        verify(socles).reprendre(LIAISON, 2, A);
        verify(socles).reprendre(LIAISON, null, null);
    }

    @Test
    @DisplayName("Doublon sans effet ; autre type du sujet ignoré")
    void doublonEtAutreType() {
        IdempotentConsumer idempotent = org.mockito.Mockito.mock(IdempotentConsumer.class);
        SocleEvaluationSandbox socles = org.mockito.Mockito.mock(SocleEvaluationSandbox.class);
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenReturn(false);
        BilanSandboxConsumer consumer = new BilanSandboxConsumer(idempotent, socles);

        consumer.recevoir(evenement("evt-1", "{\"liaisonId\":\"" + LIAISON + "\",\"empreinteBilan\":\"" + A + "\"}"));
        consumer.recevoir("{\"id\":\"evt-2\",\"type\":\"homologation.liaison-opened.v1\",\"data\":{}}");

        verify(socles, never()).reprendre(any(), any(), any());
        verify(idempotent).handleOnce(eq("evt-1"), eq(BilanSandboxConsumer.GROUPE), any());
    }
}
