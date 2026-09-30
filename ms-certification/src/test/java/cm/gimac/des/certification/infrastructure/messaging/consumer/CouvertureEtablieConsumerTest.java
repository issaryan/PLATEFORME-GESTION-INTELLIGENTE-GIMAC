package cm.gimac.des.certification.infrastructure.messaging.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.certification.application.usecase.ProcesVerbalService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consommation de homologation.coverage-established.v1 (rang 17, parcours-backend §3). */
@ExtendWith(MockitoExtension.class)
class CouvertureEtablieConsumerTest {

    static final UUID LIAISON = UUID.randomUUID();

    @Mock
    IdempotentConsumer idempotent;

    @Mock
    ProcesVerbalService procesVerbaux;

    static String evenement(String type, String taux) {
        return """
            {"specversion":"1.0","id":"evt-1","type":"%s","data":{"liaisonId":"%s","taux":%s,"servicesCouverts":["RETRAIT","PAIEMENT"]}}"""
            .formatted(type, LIAISON, taux);
    }

    @Test
    @DisplayName("Couverture établie : procès-verbal rendu établissable une seule fois par événement")
    void couvertureEtablie() {
        when(idempotent.handleOnce(eq("evt-1"), eq(CouvertureEtablieConsumer.GROUPE), any())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(2).run();
            return true;
        });
        CouvertureEtablieConsumer c = new CouvertureEtablieConsumer(idempotent, procesVerbaux);
        c.recevoir(evenement(CouvertureEtablieConsumer.TYPE, "100"));
        verify(procesVerbaux).enregistrerCouvertureEtablie(LIAISON, 100, List.of("RETRAIT", "PAIEMENT"));
        c.recevoir(evenement(CouvertureEtablieConsumer.TYPE, "null"));
        verify(procesVerbaux).enregistrerCouvertureEtablie(LIAISON, null, List.of("RETRAIT", "PAIEMENT"));
    }

    @Test
    @DisplayName("Doublon acquitté sans effet ; autre type d'événement du sujet ignoré")
    void doublonEtAutreType() {
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenReturn(false);
        CouvertureEtablieConsumer c = new CouvertureEtablieConsumer(idempotent, procesVerbaux);
        c.recevoir(evenement(CouvertureEtablieConsumer.TYPE, "100"));
        c.recevoir(evenement("homologation.liaison-opened.v1", "100"));
        verify(procesVerbaux, never()).enregistrerCouvertureEtablie(any(), any(), any());
        verify(idempotent).handleOnce(anyString(), anyString(), any());
    }
}
