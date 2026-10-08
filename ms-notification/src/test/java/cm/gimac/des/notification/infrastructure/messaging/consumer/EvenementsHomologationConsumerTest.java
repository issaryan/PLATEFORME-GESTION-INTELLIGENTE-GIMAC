package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.notification.application.port.in.RecevoirHomologationUseCase;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsJalon;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsOperation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Intervenant;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Nature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Lecture des événements du parcours d'homologation et idempotence du consommateur (D-93 §11). */
class EvenementsHomologationConsumerTest {

    ProcessedEventRepository traites;
    RecevoirHomologationUseCase homologation;
    EvenementsHomologationConsumer consommateur;

    static final String ACTIVEE =
        """
        {"specversion":"1.0","id":"e-1","type":"homologation.operation-activated.v1","subject":"l-1","institutionid":"p-1",
        "data":{"liaisonId":"l-1","reference":"LIA-2026-001","participantId":"p-1","institutionNom":"Banque A","typeProjet":"CARTE_INTERBANCAIRE",
        "operationId":"o-1","code":"3","libelle":"Cadrage","etape":"CADRAGE","vue":"AUCUNE","geste":"DEPOSER_PREUVE","echeance":[2026,10,9],
        "motif":"RENVOI","motifRenvoi":"Pièce illisible",
        "responsable":{"qualite":"R","fonction":"BANQUE_PARTENAIRE","libelleFonction":"Banque partenaire","aAffecter":false,"institutionId":"b-1"},
        "validateur":{"qualite":"V","fonction":"DDRM","compteId":"v-1","aAffecter":false},
        "destinataires":[{"qualite":"C","fonction":"SNA","compteId":"c-1","aAffecter":false},
                         {"qualite":"I","fonction":"RSSI","aAffecter":true,"profils":["X"]}],
        "externe":true,"institutionId":"b-1","institutionsConcernees":["b-1"],"lien":"/des/parcours/l-1?onglet=operations&operation=o-1","le":"2026-10-07T08:00:00Z"}}""";

    @BeforeEach
    void init() {
        traites = mock(ProcessedEventRepository.class);
        homologation = mock(RecevoirHomologationUseCase.class);
        consommateur = new EvenementsHomologationConsumer(new IdempotentConsumer(traites), homologation, new ObjectMapper());
    }

    @Test
    @DisplayName("operation-activated.v1 : charge lue (R institutionnel, V, C, I à affecter, échéance en tableau, motif) puis marquée traitée")
    void activee() throws Exception {
        consommateur.recevoir(ACTIVEE);
        ArgumentCaptor<FaitsOperation> f = ArgumentCaptor.forClass(FaitsOperation.class);
        verify(homologation).operationHomologation(f.capture(), eq(Instant.parse("2026-10-07T08:00:00Z")));
        FaitsOperation o = f.getValue();
        assertThat(o.nature()).isEqualTo(Nature.OPERATION_ACTIVEE);
        assertThat(o.echeance()).isEqualTo("2026-10-09");
        assertThat(o.motif()).isEqualTo("RENVOI");
        assertThat(o.responsable()).isEqualTo(new Intervenant("R", "BANQUE_PARTENAIRE", null, false, "b-1"));
        assertThat(o.validateur()).isEqualTo(new Intervenant("V", "DDRM", "v-1", false, null));
        assertThat(o.destinataires()).containsExactly(new Intervenant("C", "SNA", "c-1", false, null), new Intervenant("I", "RSSI", null, true, null));
        assertThat(o.institutionsConcernees()).containsExactly("b-1");
        assertThat(o.lien()).isEqualTo("/des/parcours/l-1?onglet=operations&operation=o-1");
        verify(traites).save(any());
    }

    @Test
    @DisplayName("Livraison en double : un seul traitement (processed_event, groupe notification-homologation)")
    void idempotence() throws Exception {
        when(traites.existsByEventIdAndConsumerGroup("e-1", EvenementsHomologationConsumer.GROUPE)).thenReturn(false, true);
        consommateur.recevoir(ACTIVEE);
        consommateur.recevoir(ACTIVEE);
        verify(homologation, times(1)).operationHomologation(any(), any());
        verify(traites, times(1)).save(any());
    }

    @Test
    @DisplayName("validation-requested.v1 : nature VALIDATION_DEMANDEE, sans motif ; milestone-reached.v1 : jalon lu ; autres types ignorés")
    void validationJalonEtAutres() throws Exception {
        consommateur.recevoir(
            "{\"id\":\"e-2\",\"type\":\"homologation.operation-validation-requested.v1\",\"data\":{\"liaisonId\":\"l-1\",\"operationId\":\"o-1\",\"motif\":\"X\",\"echeance\":\"2026-10-09\"," +
            "\"validateur\":{\"qualite\":\"V\",\"compteId\":\"v-1\"}}}"
        );
        ArgumentCaptor<FaitsOperation> f = ArgumentCaptor.forClass(FaitsOperation.class);
        verify(homologation).operationHomologation(f.capture(), eq(null));
        assertThat(f.getValue().nature()).isEqualTo(Nature.VALIDATION_DEMANDEE);
        assertThat(f.getValue().motif()).isNull();
        assertThat(f.getValue().echeance()).isEqualTo("2026-10-09");

        consommateur.recevoir(
            "{\"id\":\"e-3\",\"type\":\"homologation.liaison-milestone-reached.v1\",\"data\":\"{\\\"liaisonId\\\":\\\"l-1\\\",\\\"reference\\\":\\\"LIA\\\",\\\"institutionId\\\":\\\"b-1\\\"," +
            "\\\"jalon\\\":\\\"EN_PRODUCTION\\\",\\\"libelleJalon\\\":\\\"En production\\\",\\\"conducteurId\\\":\\\"k-1\\\",\\\"destinataires\\\":[{\\\"qualite\\\":\\\"I\\\",\\\"compteId\\\":\\\"i-1\\\"}]," +
            "\\\"lien\\\":\\\"/des/parcours/l-1\\\",\\\"le\\\":1791360000}\"}"
        );
        ArgumentCaptor<FaitsJalon> j = ArgumentCaptor.forClass(FaitsJalon.class);
        verify(homologation).jalonHomologation(j.capture(), eq(Instant.ofEpochSecond(1791360000L)));
        assertThat(j.getValue()).isEqualTo(
            new FaitsJalon("l-1", "LIA", "b-1", null, "EN_PRODUCTION", "En production", "k-1", List.of(new Intervenant("I", null, "i-1", false, null)), "/des/parcours/l-1")
        );

        consommateur.recevoir("{\"id\":\"e-4\",\"type\":\"homologation.operation-signed.v1\",\"data\":{}}");
        consommateur.recevoir("{\"type\":\"homologation.operation-activated.v1\",\"data\":{}}");
        verify(traites, never()).existsByEventIdAndConsumerGroup(eq("e-4"), anyString());
        verify(traites, times(2)).save(any());
    }

    @Test
    @DisplayName("Enveloppe sans type : ignorée sans effet")
    void sansType() throws Exception {
        consommateur.recevoir("{\"id\":\"e-9\"}");
        verifyNoInteractions(homologation);
    }
}
