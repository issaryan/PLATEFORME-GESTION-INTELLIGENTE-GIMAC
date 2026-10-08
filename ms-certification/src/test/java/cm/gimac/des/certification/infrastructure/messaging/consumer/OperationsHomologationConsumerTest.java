package cm.gimac.des.certification.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.certification.application.usecase.OperationsLiaison;
import cm.gimac.des.certification.application.usecase.OperationsLiaison.OperationRecue;
import cm.gimac.des.certification.domain.operation.Intervenant;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Consommation des événements d'opération de MS-HOMOLOGATION (D-93 §4, charges de §8.2). */
class OperationsHomologationConsumerTest {

    static final UUID LIAISON = UUID.randomUUID();
    static final UUID OPERATION = UUID.randomUUID();

    final IdempotentConsumer idempotent = mock(IdempotentConsumer.class);
    final OperationsLiaison operations = mock(OperationsLiaison.class);
    final OperationsHomologationConsumer consommateur = new OperationsHomologationConsumer(idempotent, operations);

    static String activee(String type) {
        return """
            {"specversion":"1.0","id":"evt-1","type":"%s","time":"2026-10-07T08:00:00Z","data":{"liaisonId":"%s","reference":"LIA-2026-001",\
            "participantId":"%s","operationId":"%s","code":"18","libelle":"Procès-verbal","etape":"DECISION","vue":"DECISION",\
            "geste":"EVENEMENT_EXTERNE","echeance":"2026-10-09","motif":"OUVERTURE",\
            "responsable":{"qualite":"R","fonction":"CONDUCTEUR","libelleFonction":"Conducteur","compteId":"u-1","aAffecter":false},\
            "validateur":{"qualite":"V","fonction":"CHEF_DES","aAffecter":true,"profils":["ROLE_X"]},\
            "destinataires":[{"qualite":"C","fonction":"RSSI","compteId":"u-2","aAffecter":false}],"externe":true,\
            "institutionId":"INST-1","institutionsConcernees":["INST-1"],\
            "declencheur":"certification.pv-established.v1",\
            "signatures":[{"code":"VISA_DDRM","libelle":"Visa","signataire":"DDRM","ordre":2}],"le":"2026-10-07T08:00:01Z"}}"""
            .formatted(type, LIAISON, UUID.randomUUID(), OPERATION);
    }

    void executer() {
        when(idempotent.handleOnce(eq("evt-1"), eq(OperationsHomologationConsumer.GROUPE), any())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(2).run();
            return true;
        });
    }

    @Test
    @DisplayName("operation-activated.v1 : opération, intervenants R, V, C, institutions et signatures configurées lus")
    void activee() {
        executer();
        consommateur.recevoir(activee(OperationsHomologationConsumer.ACTIVEE));
        ArgumentCaptor<OperationRecue> o = ArgumentCaptor.forClass(OperationRecue.class);
        verify(operations).activee(o.capture());
        OperationRecue r = o.getValue();
        assertThat(r.liaisonId()).isEqualTo(LIAISON);
        assertThat(r.operationId()).isEqualTo(OPERATION);
        assertThat(r.le()).isEqualTo(Instant.parse("2026-10-07T08:00:01Z"));
        assertThat(r.description().code()).isEqualTo("18");
        assertThat(r.description().declencheur()).isEqualTo("certification.pv-established.v1");
        assertThat(r.intervenants()).containsExactly(
            new Intervenant("R", "CONDUCTEUR", "u-1", null, false),
            new Intervenant("V", "CHEF_DES", null, null, true),
            new Intervenant("C", "RSSI", "u-2", null, false)
        );
        assertThat(r.institutionsConcernees()).containsExactly("INST-1");
        assertThat(r.signatures()).singleElement().satisfies(s -> {
            assertThat(s.code()).isEqualTo("VISA_DDRM");
            assertThat(s.fonction()).isEqualTo("DDRM");
            assertThat(s.ordre()).isEqualTo(2);
            assertThat(s.portail()).isFalse();
        });
    }

    @Test
    @DisplayName("validation demandée, signature D-91, clôture et jalon : état projeté")
    void autresTypes() {
        when(idempotent.handleOnce(anyString(), eq(OperationsHomologationConsumer.GROUPE), any())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(2).run();
            return true;
        });
        consommateur.recevoir(activee(OperationsHomologationConsumer.VALIDATION));
        verify(operations).enValidation(any());
        consommateur.recevoir(
            """
            {"id":"evt-2","type":"homologation.operation-signed.v1","data":{"liaisonId":"%s","operationId":"%s","rang":"19","libelle":"PV",\
            "signature":"VISA","operationClose":true,"le":"2026-10-07T09:00:00Z"}}""".formatted(LIAISON, OPERATION)
        );
        verify(operations).signee(LIAISON, OPERATION, "19", "PV", true, Instant.parse("2026-10-07T09:00:00Z"));
        consommateur.recevoir(
            """
            {"id":"evt-3","type":"homologation.liaison-milestone-reached.v1","time":"2026-10-07T10:00:00Z","data":{"liaisonId":"%s",\
            "operationId":"%s","code":"19","libelle":"PV","jalon":"CERTIFIEE"}}""".formatted(LIAISON, OPERATION)
        );
        verify(operations).jalonAtteint(LIAISON, OPERATION, "19", "PV", "CERTIFIEE", Instant.parse("2026-10-07T10:00:00Z"));
        consommateur.recevoir(
            """
            {"id":"evt-5","type":"homologation.operation-closed.v1","data":{"liaisonId":"%s","reference":"LIA-1","participantId":"%s",\
            "operationId":"%s","code":"20","libelle":"Dossier","etat":"SANS_OBJET","declencheur":"certification.change-record-constituted.v1",\
            "lien":"/des/parcours/x","le":"2026-10-07T11:00:00Z"}}""".formatted(LIAISON, UUID.randomUUID(), OPERATION)
        );
        ArgumentCaptor<cm.gimac.des.certification.domain.operation.OperationLiaison.Description> d = ArgumentCaptor.forClass(
            cm.gimac.des.certification.domain.operation.OperationLiaison.Description.class
        );
        verify(operations).cloturee(eq(LIAISON), eq(OPERATION), eq("SANS_OBJET"), d.capture(), eq(Instant.parse("2026-10-07T11:00:00Z")));
        assertThat(d.getValue().code()).isEqualTo("20");
        assertThat(d.getValue().declencheur()).isEqualTo("certification.change-record-constituted.v1");
        assertThat(d.getValue().jalon()).isNull();
    }

    @Test
    @DisplayName("Doublon acquitté sans effet ; autres types et événement sans opération ignorés")
    void doublonEtIgnores() {
        when(idempotent.handleOnce(anyString(), anyString(), any())).thenReturn(false);
        consommateur.recevoir(activee(OperationsHomologationConsumer.ACTIVEE));
        verify(operations, never()).activee(any());
        OperationsHomologationConsumer autre = new OperationsHomologationConsumer(mock(IdempotentConsumer.class), operations);
        autre.recevoir(activee("homologation.liaison-opened.v1"));
        autre.recevoir("""
            {"id":"evt-4","type":"homologation.liaison-milestone-reached.v1","data":{"liaisonId":"%s","jalon":"EN_PRODUCTION"}}""".formatted(LIAISON));
        verifyNoInteractions(operations);
    }

    @Test
    @DisplayName("Message illisible : exception, remis au gestionnaire d'erreurs (rebut)")
    void illisible() {
        assertThatThrownBy(() -> consommateur.recevoir("pas du json")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consommateur.recevoir(activee(OperationsHomologationConsumer.ACTIVEE).replace(LIAISON.toString(), "x"))).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
