package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.DepartsIntervenants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Départ d'un compte (D-93 §1) : iam.account-administered.v1, DESACTIVATION ou SUPPRESSION, idempotent. */
class ComptesIamConsumerTest {

    static String evenement(String type, String action, String compte) {
        return "{\"id\":\"e1\",\"type\":\"" + type + "\",\"data\":{\"action\":" + (action == null ? "null" : "\"" + action + "\"") + ",\"compteId\":" + (compte == null ? "null" : "\"" + compte + "\"") + "}}";
    }

    @Test
    @DisplayName("Désactivation et suppression traitées une fois ; autre action, autre type ou compte absent : ignorés")
    void departs() {
        IdempotentConsumer idempotent = mock(IdempotentConsumer.class);
        DepartsIntervenants departs = mock(DepartsIntervenants.class);
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenAnswer(i -> {
            ((Runnable) i.getArgument(2)).run();
            return true;
        });
        ComptesIamConsumer c = new ComptesIamConsumer(idempotent, departs);
        c.recevoir(evenement("iam.account-administered.v1", "DESACTIVATION", "u1"));
        verify(departs).traiterDepart("u1");
        c.recevoir(evenement("iam.account-administered.v1", "PROFILS", "u2"));
        c.recevoir(evenement("iam.account-created.v1", "SUPPRESSION", "u3"));
        c.recevoir(evenement("iam.account-administered.v1", "SUPPRESSION", null));
        c.recevoir(evenement("iam.account-administered.v1", null, "u4"));
        verify(departs, never()).traiterDepart(eq("u2"));
        verify(departs, never()).traiterDepart(eq("u3"));
        verify(departs, never()).traiterDepart(eq("u4"));
        org.mockito.Mockito.doReturn(false).when(idempotent).handleOnce(anyString(), anyString(), any());
        c.recevoir(evenement("iam.account-administered.v1", "SUPPRESSION", "u5"));
        verify(departs, never()).traiterDepart(eq("u5"));
    }
}
