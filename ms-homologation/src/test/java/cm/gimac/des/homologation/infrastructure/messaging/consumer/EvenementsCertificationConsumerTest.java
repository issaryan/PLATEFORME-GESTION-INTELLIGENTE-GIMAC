package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.EvenementsCertification;
import cm.gimac.des.homologation.application.usecase.EvenementsCertification.Signature;
import cm.gimac.des.homologation.domain.parcours.TypeAttestation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consommateur des événements de MS-CERTIFICATION : répartition par type, idempotence, messages sans effet. */
@ExtendWith(MockitoExtension.class)
class EvenementsCertificationConsumerTest {

    static final UUID L = UUID.randomUUID();

    @Mock
    IdempotentConsumer idempotent;

    @Mock
    EvenementsCertification effets;

    EvenementsCertificationConsumer consommateur;

    @BeforeEach
    void init() {
        consommateur = new EvenementsCertificationConsumer(idempotent, effets);
        lenient().when(idempotent.handleOnce(anyString(), eq(EvenementsCertificationConsumer.GROUPE), any())).thenAnswer(i -> {
            ((Runnable) i.getArgument(2)).run();
            return true;
        });
    }

    static String evenement(String type, String data) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"" + type + "\",\"data\":" + data + "}";
    }

    @Test
    @DisplayName("Les six événements du contrat et les quatre facultatifs atteignent leur effet")
    void repartition() {
        String l = "\"liaisonId\":\"" + L + "\"";
        consommateur.recevoir(evenement("certification.pv-established.v1", "{" + l + ",\"etabliPar\":\"Conducteur\",\"empreinte\":\"e\"}"));
        consommateur.recevoir(evenement("certification.pv-signed.v1", "{" + l + ",\"signatures\":[{\"fonction\":\"CHEF_DES\",\"signataire\":\"Chef\"},{\"fonction\":\"PARTICIPANT\",\"signataire\":null}],\"empreinte\":\"e\"}"));
        consommateur.recevoir(evenement("certification.change-record-constituted.v1", "{" + l + ",\"documentRef\":\"d\",\"nom\":\"n\",\"empreinte\":\"e\"}"));
        consommateur.recevoir(evenement("certification.change-record-referenced.v1", "{" + l + ",\"reference\":\"CHG-2026-1\"}"));
        consommateur.recevoir(evenement("certification.attestations-archived.v1", "{" + l + ",\"attestations\":[{\"type\":\"PCI_DSS\",\"documentRef\":\"a1\"},{\"type\":\"COBAC\"}]}"));
        consommateur.recevoir(evenement("certification.security-approved.v1", "{" + l + ",\"approuveeLe\":\"2026-10-06\"}"));
        consommateur.recevoir(evenement("certification.attestation-submitted.v1", "{" + l + ",\"type\":\"GSMA\"}"));
        consommateur.recevoir(evenement("certification.attestation-deposited.v1", "{" + l + ",\"type\":\"COBAC\"}"));
        consommateur.recevoir(evenement("certification.attestation-verified.v1", "{" + l + ",\"type\":\"GSMA\",\"conforme\":true}"));
        consommateur.recevoir(evenement("certification.visas-required.v1", "{" + l + ",\"natures\":[\"CONFORMITE\"]}"));
        consommateur.recevoir(evenement("certification.visa-signed.v1", "{" + l + ",\"nature\":\"CONFORMITE\"}"));
        verify(effets).pvEtabli(L, "Conducteur", "e");
        verify(effets).pvSigne(L, List.of(new Signature("CHEF_DES", "Chef"), new Signature("PARTICIPANT", null)), "e");
        verify(effets).dossierChangementConstitue(L, "d", "n", "e");
        verify(effets).ficheChangementReferencee(L, "CHG-2026-1");
        verify(effets).attestationsArchivees(L, List.of("a1"));
        verify(effets).securiteApprouvee(L, "2026-10-06");
        verify(effets).attestationDeposee(L, TypeAttestation.GSMA);
        verify(effets).attestationDeposee(L, TypeAttestation.COBAC);
        verify(effets).attestationVerifiee(L, TypeAttestation.GSMA);
        verify(effets).visasAttendus(L, List.of("CONFORMITE"));
        verify(effets).visaSigne(L, "CONFORMITE");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{\"id\":\"1\",\"type\":\"homologation.liaison-opened.v1\",\"data\":{}}",
            "{\"id\":\"\",\"type\":\"certification.pv-established.v1\",\"data\":{}}",
            "{\"type\":\"certification.pv-established.v1\",\"data\":{}}",
            "{\"id\":\"1\",\"data\":{}}",
            "{\"id\":\"1\",\"type\":\"certification.pv-established.v1\",\"data\":{\"liaisonId\":\"\"}}",
            "{\"id\":\"1\",\"type\":\"certification.pv-established.v1\",\"data\":{}}",
        }
    )
    @DisplayName("Messages sans effet : autre service, sans identifiant, sans liaison")
    void ignores(String message) {
        consommateur.recevoir(message);
        verifyNoInteractions(idempotent, effets);
    }

    @Test
    @DisplayName("Type inconnu : sans effet ; doublon : acquitté sans effet")
    void inconnuEtDoublon() {
        consommateur.recevoir(evenement("certification.quitus-signed.v1", "{\"liaisonId\":\"" + L + "\"}"));
        verify(idempotent, never()).handleOnce(anyString(), anyString(), any());
        when(idempotent.handleOnce(anyString(), eq(EvenementsCertificationConsumer.GROUPE), any())).thenReturn(false);
        consommateur.recevoir(evenement("certification.pv-established.v1", "{\"liaisonId\":\"" + L + "\",\"etabliPar\":null}"));
        verifyNoInteractions(effets);
        assertThat(EvenementsCertificationConsumer.GROUPE).isEqualTo("homologation-certification");
    }
}
