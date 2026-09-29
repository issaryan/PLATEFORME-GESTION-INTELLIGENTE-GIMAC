package cm.gimac.des.participant.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consommation de homologation.liaison-opened.v1 (étape 3, D-57). */
@ExtendWith(MockitoExtension.class)
class LiaisonOuverteConsumerTest {

    static final UUID INSTITUTION = UUID.randomUUID();
    static final UUID LIAISON = UUID.randomUUID();

    @Mock
    IdempotentConsumer idempotent;

    @Mock
    ManageInstitutionUseCase institutions;

    static String evenement(String type) {
        return """
            {"specversion":"1.0","id":"evt-1","type":"%s","data":{"liaisonId":"%s","institutionId":"%s","typeProjet":"PAIEMENT_MOBILE"}}"""
            .formatted(type, LIAISON, INSTITUTION);
    }

    @Test
    @DisplayName("Liaison ouverte : transition automatique demandée une seule fois par événement")
    void liaisonOuverte() {
        when(idempotent.handleOnce(eq("evt-1"), eq(LiaisonOuverteConsumer.GROUPE), any())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(2).run();
            return true;
        });
        new LiaisonOuverteConsumer(idempotent, institutions).recevoir(evenement(LiaisonOuverteConsumer.TYPE));
        verify(institutions).debuterHomologation(INSTITUTION, LIAISON, "PAIEMENT_MOBILE");
    }

    @Test
    @DisplayName("Doublon acquitté sans effet ; autre type d'événement du sujet ignoré")
    void doublonEtAutreType() {
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenReturn(false);
        LiaisonOuverteConsumer c = new LiaisonOuverteConsumer(idempotent, institutions);
        c.recevoir(evenement(LiaisonOuverteConsumer.TYPE));
        c.recevoir(evenement("homologation.liaison-frozen.v1"));
        verify(institutions, never()).debuterHomologation(any(), any(), any());
        verify(idempotent).handleOnce(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Message illisible : exception, remis au gestionnaire d'erreurs (rebut)")
    void illisible() {
        LiaisonOuverteConsumer c = new LiaisonOuverteConsumer(idempotent, institutions);
        assertThatThrownBy(() -> c.recevoir("pas du json")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.recevoir(evenement(LiaisonOuverteConsumer.TYPE).replace(INSTITUTION.toString(), "x"))).isInstanceOf(IllegalArgumentException.class);
    }
}
