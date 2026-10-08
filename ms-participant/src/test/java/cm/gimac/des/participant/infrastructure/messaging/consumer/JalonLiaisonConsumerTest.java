package cm.gimac.des.participant.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consommation de homologation.liaison-milestone-reached.v1 (D-93 §4). */
@ExtendWith(MockitoExtension.class)
class JalonLiaisonConsumerTest {

    static final UUID INSTITUTION = UUID.randomUUID();
    static final UUID LIAISON = UUID.randomUUID();

    @Mock
    IdempotentConsumer idempotent;

    @Mock
    ManageInstitutionUseCase institutions;

    static String evenement(String type, String jalon) {
        return """
            {"specversion":"1.0","id":"evt-9","type":"%s","data":{"liaisonId":"%s","reference":"LIA-2026-001","institutionId":"%s",\
            "typeProjet":"CARTE_INTERBANCAIRE","jalon":"%s","statut":"EN_PRODUCTION"}}"""
            .formatted(type, LIAISON, INSTITUTION, jalon);
    }

    @Test
    @DisplayName("Jalon EN_PRODUCTION : mise en production demandée une seule fois par événement")
    void enProduction() {
        when(idempotent.handleOnce(eq("evt-9"), eq(JalonLiaisonConsumer.GROUPE), any())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(2).run();
            return true;
        });
        new JalonLiaisonConsumer(idempotent, institutions).recevoir(evenement(JalonLiaisonConsumer.TYPE, "EN_PRODUCTION"));
        verify(institutions).mettreEnProduction(INSTITUTION, LIAISON, "LIA-2026-001", "CARTE_INTERBANCAIRE");
    }

    @Test
    @DisplayName("Doublon acquitté sans effet")
    void doublon() {
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenReturn(false);
        new JalonLiaisonConsumer(idempotent, institutions).recevoir(evenement(JalonLiaisonConsumer.TYPE, "EN_PRODUCTION"));
        verify(institutions, never()).mettreEnProduction(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Autres jalons et autres types du sujet : ignorés sans trace d'idempotence")
    void autresJalons() {
        JalonLiaisonConsumer c = new JalonLiaisonConsumer(idempotent, institutions);
        for (String jalon : new String[] { "APTE_CERTIFICATION", "CERTIFIEE", "APPROUVEE_SECURITE" }) {
            c.recevoir(evenement(JalonLiaisonConsumer.TYPE, jalon));
        }
        c.recevoir(evenement("homologation.liaison-opened.v1", "EN_PRODUCTION"));
        verifyNoInteractions(idempotent, institutions);
    }

    @Test
    @DisplayName("Message illisible : exception, remis au gestionnaire d'erreurs (rebut)")
    void illisible() {
        JalonLiaisonConsumer c = new JalonLiaisonConsumer(idempotent, institutions);
        assertThatThrownBy(() -> c.recevoir("pas du json")).isInstanceOf(IllegalArgumentException.class);
        String mauvais = evenement(JalonLiaisonConsumer.TYPE, "EN_PRODUCTION").replace(INSTITUTION.toString(), "x");
        assertThatThrownBy(() -> c.recevoir(mauvais)).isInstanceOf(IllegalArgumentException.class);
    }
}
