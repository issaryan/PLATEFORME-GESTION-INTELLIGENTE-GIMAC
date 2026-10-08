package cm.gimac.des.audit.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.audit.application.port.in.ScellerSignatureUseCase;
import cm.gimac.des.audit.application.port.in.ScellerSignatureUseCase.SignatureRecue;
import cm.gimac.des.audit.domain.registre.NatureSignature;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/** Lecture des enveloppes de signature (D-93 §11). */
class SignaturesConsumerTest {

    ScellerSignatureUseCase registre;
    SignaturesConsumer consommateur;

    @BeforeEach
    void init() {
        registre = mock(ScellerSignatureUseCase.class);
        when(registre.sceller(any())).thenReturn(Optional.of(0L));
        consommateur = new SignaturesConsumer(registre, JsonMapper.builder().build());
    }

    @Test
    @DisplayName("pilotage.process-step-signed.v1 : signataire, instant time, corrélation, enveloppe transmise telle quelle")
    void processus() {
        String m =
            "{\"specversion\":\"1.0\",\"id\":\"e-1\",\"type\":\"pilotage.process-step-signed.v1\",\"time\":\"2026-10-07T08:00:00Z\",\"correlationid\":\"c-1\"," +
            "\"data\":{\"instanceId\":\"i-1\",\"signataireId\":\"s-1\",\"empreinte\":\"ab\",\"le\":\"2026-10-07T07:59:59Z\"}}";
        consommateur.recevoir(m);
        ArgumentCaptor<SignatureRecue> s = ArgumentCaptor.forClass(SignatureRecue.class);
        verify(registre).sceller(s.capture());
        assertThat(s.getValue()).isEqualTo(new SignatureRecue("e-1", NatureSignature.PROCESSUS, Instant.parse("2026-10-07T08:00:00Z"), "c-1", "s-1", m));
    }

    @Test
    @DisplayName("homologation.operation-signed.v1 : charge en chaîne, instant le à défaut de time")
    void operation() {
        String m = "{\"id\":\"e-2\",\"type\":\"homologation.operation-signed.v1\",\"data\":\"{\\\"signataireId\\\":\\\"s-2\\\",\\\"le\\\":\\\"2026-10-07T09:00:00Z\\\"}\"}";
        consommateur.recevoir(m);
        ArgumentCaptor<SignatureRecue> s = ArgumentCaptor.forClass(SignatureRecue.class);
        verify(registre).sceller(s.capture());
        assertThat(s.getValue().nature()).isEqualTo(NatureSignature.OPERATION);
        assertThat(s.getValue().signataireId()).isEqualTo("s-2");
        assertThat(s.getValue().survenueLe()).isEqualTo(Instant.parse("2026-10-07T09:00:00Z"));
        assertThat(s.getValue().correlationId()).isNull();
    }

    @Test
    @DisplayName("Autres types et enveloppes sans identifiant : ignorés")
    void ignores() {
        consommateur.recevoir("{\"id\":\"e-3\",\"type\":\"pilotage.process-step-activated.v1\",\"data\":{}}");
        consommateur.recevoir("{\"id\":\"e-4\",\"type\":\"homologation.operation-activated.v1\",\"data\":{}}");
        consommateur.recevoir("{\"type\":\"homologation.operation-signed.v1\",\"data\":{}}");
        verifyNoInteractions(registre);
    }
}
