package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.sandbox.GelLiaisonsInstitution;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HOM-06 : lecture de participant.status-changed.v1, version de statut facultative, consommation idempotente. */
class StatutsInstitutionConsumerTest {

    static final UUID I = UUID.fromString("0192f0aa-0000-7000-8000-000000099001");

    IdempotentConsumer idempotent;
    GelLiaisonsInstitution gel;
    StatutsInstitutionConsumer consommateur;

    @BeforeEach
    void init() {
        idempotent = mock(IdempotentConsumer.class);
        when(idempotent.handleOnce(anyString(), eq(StatutsInstitutionConsumer.GROUPE), any())).thenAnswer(a -> {
            a.<Runnable>getArgument(2).run();
            return true;
        });
        gel = mock(GelLiaisonsInstitution.class);
        consommateur = new StatutsInstitutionConsumer(idempotent, gel);
    }

    static String evenement(String id, String type, String data) {
        return "{\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"data\":" + data + "}";
    }

    @Test
    @DisplayName("Suspension avec version de statut (D-120), date d'effet et motif")
    void avecVersion() {
        consommateur.recevoir(evenement("e1", StatutsInstitutionConsumer.TYPE,
            "{\"institutionId\":\"" + I + "\",\"statut\":\"SUSPENDU\",\"motif\":\"Défaut\",\"dateEffet\":\"2026-10-22T08:00:00Z\",\"versionStatutRegistre\":7}"));
        verify(gel).appliquer(I, "SUSPENDU", 7L, Instant.parse("2026-10-22T08:00:00Z"), "Défaut");
    }

    @Test
    @DisplayName("Événement antérieur à D-120 : sans version (absente ou nulle), ordre par la date d'effet ; motif absent")
    void sansVersion() {
        consommateur.recevoir(evenement("e2", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\"" + I + "\",\"statut\":\"ACTIF\",\"dateEffet\":\"2026-10-22T09:00:00Z\"}"));
        verify(gel).appliquer(I, "ACTIF", null, Instant.parse("2026-10-22T09:00:00Z"), null);
        consommateur.recevoir(evenement("e3", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\"" + I + "\",\"statut\":\"RETIRE\",\"versionStatutRegistre\":0,\"motif\":null}"));
        verify(gel).appliquer(I, "RETIRE", null, null, null);
    }

    @Test
    @DisplayName("Notification reçue plusieurs fois : aucun second traitement")
    void doublon() {
        when(idempotent.handleOnce(eq("e4"), eq(StatutsInstitutionConsumer.GROUPE), any())).thenReturn(false);
        consommateur.recevoir(evenement("e4", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\"" + I + "\",\"statut\":\"SUSPENDU\"}"));
        verify(gel, never()).appliquer(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Autres types, sans identifiant, sans institution ou sans statut : ignorés")
    void ignores() {
        consommateur.recevoir(evenement("e5", "participant.institution-updated.v1", "{\"institutionId\":\"" + I + "\",\"statut\":\"SUSPENDU\"}"));
        consommateur.recevoir(evenement("", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\"" + I + "\",\"statut\":\"SUSPENDU\"}"));
        consommateur.recevoir("{\"type\":\"" + StatutsInstitutionConsumer.TYPE + "\",\"data\":{}}");
        consommateur.recevoir(evenement("e6", StatutsInstitutionConsumer.TYPE, "{\"statut\":\"SUSPENDU\"}"));
        consommateur.recevoir(evenement("e7", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\"" + I + "\"}"));
        consommateur.recevoir(evenement("e8", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\" \",\"statut\":\"SUSPENDU\"}"));
        consommateur.recevoir(evenement("e9", StatutsInstitutionConsumer.TYPE, "{\"institutionId\":\"" + I + "\",\"statut\":\" \"}"));
        verify(idempotent, never()).handleOnce(anyString(), anyString(), any());
    }
}
