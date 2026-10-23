package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.Demande;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Lecture des demandes de notification de la sandbox et idempotence du consommateur (US-EXI-09). */
class DemandesSandboxConsumerTest {

    ProcessedEventRepository traites;
    RecevoirNotificationsSandboxUseCase sandbox;
    DemandesSandboxConsumer consommateur;

    static final String INVITATION =
        """
        {"specversion":"1.0","id":"d-1","type":"sbx-espace.notification-requested.v1","source":"/gimac/sandbox/sbx-espace","partitionkey":"99001",
        "data":{"modele":"SBX_ESP_INVITATION_ADMINISTRATEUR","destinataires":["c-1","ROLE:ADMINISTRATEUR_GIMAC",""],"codeParticipant":"99001","codeEspace":"ESP-99001","le":"2026-10-23T08:00:00Z"}}""";

    @BeforeEach
    void init() {
        traites = mock(ProcessedEventRepository.class);
        sandbox = mock(RecevoirNotificationsSandboxUseCase.class);
        consommateur = new DemandesSandboxConsumer(new IdempotentConsumer(traites), sandbox, new ObjectMapper());
    }

    @Test
    @DisplayName("Demande lue (service, modèle, destinataires, codes, instant) puis marquée traitée")
    void invitation() throws Exception {
        consommateur.recevoir(INVITATION);
        ArgumentCaptor<Demande> d = ArgumentCaptor.forClass(Demande.class);
        verify(sandbox).demander(d.capture());
        assertThat(d.getValue()).isEqualTo(
            new Demande(
                "d-1",
                "sbx-espace",
                "SBX_ESP_INVITATION_ADMINISTRATEUR",
                java.util.List.of("c-1", "ROLE:ADMINISTRATEUR_GIMAC"),
                "99001",
                "ESP-99001",
                Instant.parse("2026-10-23T08:00:00Z")
            )
        );
        verify(traites).save(any());
    }

    @Test
    @DisplayName("Redélivrance : aucune seconde remise")
    void doublon() throws Exception {
        when(traites.existsByEventIdAndConsumerGroup("d-1", DemandesSandboxConsumer.GROUPE)).thenReturn(true);
        consommateur.recevoir(INVITATION);
        verifyNoInteractions(sandbox);
    }

    @Test
    @DisplayName("Données en texte, instant illisible ou absent, destinataires absents")
    void donneesTexte() throws Exception {
        consommateur.recevoir(
            """
            {"id":"d-2","type":"sbx-raccordement.notification-requested.v1","data":"{\\"modele\\":\\"SBX_RAC_ECHEANCE_CERTIFICAT\\",\\"le\\":\\"hier\\",\\"destinataires\\":[1,\\"a\\",null]}"}"""
        );
        consommateur.recevoir("{\"id\":\"d-3\",\"type\":\"sbx-trace.notification-requested.v1\",\"data\":{\"modele\":\"SBX_TRA_ALERTE\",\"destinataires\":\"x\"}}");
        ArgumentCaptor<Demande> d = ArgumentCaptor.forClass(Demande.class);
        verify(sandbox, org.mockito.Mockito.times(2)).demander(d.capture());
        assertThat(d.getAllValues().get(0).service()).isEqualTo("sbx-raccordement");
        assertThat(d.getAllValues().get(0).le()).isNull();
        assertThat(d.getAllValues().get(0).destinataires()).containsExactly("a");
        assertThat(d.getAllValues().get(1).le()).isNull();
        assertThat(d.getAllValues().get(1).codeParticipant()).isNull();
    }

    @Test
    @DisplayName("Forme de SBX-DONNEES : gabarit, destinataire unique (rôle ou compte), code participant de partitionkey")
    void formeDonnees() throws Exception {
        consommateur.recevoir(
            """
            {"id":"d-4","type":"sbx-donnees.notification-requested.v1","partitionkey":"99001","data":{"gabarit":"SBX_DON_CLE_APPROUVEE","destinataire":"PRODUCTION_MONETIQUE","cle":"k"}}"""
        );
        consommateur.recevoir(
            """
            {"id":"d-5","type":"sbx-donnees.notification-requested.v1","data":{"modele":"SBX_DON_VISA_PLAGE","codeParticipant":"99002","destinataire":"c-9","destinataires":[]}}"""
        );
        ArgumentCaptor<Demande> d = ArgumentCaptor.forClass(Demande.class);
        verify(sandbox, org.mockito.Mockito.times(2)).demander(d.capture());
        Demande a = d.getAllValues().get(0);
        assertThat(a.modele()).isEqualTo("SBX_DON_CLE_APPROUVEE");
        assertThat(a.destinataires()).containsExactly("ROLE:PRODUCTION_MONETIQUE");
        assertThat(a.codeParticipant()).isEqualTo("99001");
        Demande b = d.getAllValues().get(1);
        assertThat(b.destinataires()).containsExactly("c-9");
        assertThat(b.codeParticipant()).isEqualTo("99002");
    }

    @Test
    @DisplayName("Autre type, type ou identifiant absent : ignoré sans rien marquer")
    void ignores() throws Exception {
        consommateur.recevoir("{\"id\":\"x\",\"type\":\"sbx-espace.space-opened.v1\",\"data\":{}}");
        consommateur.recevoir("{\"id\":\"x\",\"type\":\"autre.notification-requested.v1\"}");
        consommateur.recevoir("{\"type\":\"sbx-espace.notification-requested.v1\"}");
        consommateur.recevoir("{\"id\":\"x\"}");
        verifyNoInteractions(sandbox);
        verify(traites, org.mockito.Mockito.never()).existsByEventIdAndConsumerGroup(anyString(), eq(DemandesSandboxConsumer.GROUPE));
    }
}
