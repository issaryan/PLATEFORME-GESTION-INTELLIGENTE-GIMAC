package cm.gimac.des.audit.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.audit.application.port.in.InscrireEvenementSandboxUseCase;
import cm.gimac.des.audit.domain.sandbox.EnveloppeSandbox;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/** US-EXI-10 : lecture des enveloppes des sujets sbx-*, inscription, rebut avec motif, notifications ignorées. */
class EvenementsSandboxConsumerTest {

    InscrireEvenementSandboxUseCase registre;
    EvenementsSandboxConsumer consommateur;

    @BeforeEach
    void init() {
        registre = mock(InscrireEvenementSandboxUseCase.class);
        when(registre.inscrire(any(), anyString())).thenReturn(Optional.of(4L));
        consommateur = new EvenementsSandboxConsumer(registre, JsonMapper.builder().build());
    }

    static final String VERDICT =
        "{\"specversion\":\"1.0\",\"id\":\"v-1\",\"type\":\"sbx-campagne.verdict-validated.v1\",\"source\":\"/sbx-campagne\",\"time\":\"2026-10-22T08:00:00Z\"," +
        "\"institutionid\":\"99001\",\"data\":{\"acteurId\":\"u-certificateur\",\"verdict\":\"favorable\",\"objet\":{\"execution\":\"EXE-1\"}}}";

    @Test
    @DisplayName("Critère 1 : verdict validé inscrit avec son auteur, son institution et l'enveloppe telle quelle")
    void inscription() {
        consommateur.recevoir(VERDICT, "sbx-campagne.verdict");
        ArgumentCaptor<EnveloppeSandbox> c = ArgumentCaptor.forClass(EnveloppeSandbox.class);
        verify(registre).inscrire(c.capture(), eq(VERDICT));
        assertThat(c.getValue().acteur()).isEqualTo("u-certificateur");
        assertThat(c.getValue().institution()).isEqualTo("99001");
        // Relivraison : le registre rend vide, rien d'autre.
        when(registre.inscrire(any(), anyString())).thenReturn(Optional.empty());
        consommateur.traiter("sbx-campagne.verdict", VERDICT);
        verify(registre, never()).rebuter(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Critère 3 : enveloppe invalide ou JSON illisible au rebut avec son motif ; demande de notification ignorée")
    void rebut() {
        String sansTime = VERDICT.replace("\"time\":\"2026-10-22T08:00:00Z\",", "");
        consommateur.traiter("sbx-lecture.access", sansTime);
        verify(registre).rebuter("v-1", "sbx-lecture.access", "TIME_INVALIDE", sansTime);
        consommateur.traiter("sbx-lecture.access", "{pas du json");
        verify(registre).rebuter(isNull(), eq("sbx-lecture.access"), eq("JSON_ILLISIBLE"), eq("{pas du json"));
        consommateur.traiter("sbx-lecture.access", "[1,2]");
        consommateur.traiter("sbx-lecture.access", null);
        verify(registre).rebuter(isNull(), eq("sbx-lecture.access"), eq("JSON_ILLISIBLE"), isNull());
        consommateur.traiter("sbx-espace.notification", "{\"specversion\":\"1.0\",\"id\":\"n-1\",\"type\":\"sbx-espace.notification-requested.v1\",\"data\":{}}");
        verify(registre, never()).inscrire(any(), anyString());
        // Données sous forme de texte (non objet) : DATA_ABSENTES ; institution des données à défaut d'extension.
        consommateur.traiter("sbx-trace.trace", VERDICT.replace("\"data\":{", "\"data\":\"x\",\"autre\":{"));
        verify(registre).rebuter(eq("v-1"), eq("sbx-trace.trace"), eq("DATA_ABSENTES"), anyString());
        String parDonnees = VERDICT.replace("\"institutionid\":\"99001\",", "").replace("\"acteurId\"", "\"sub\"").replace("{\"acteurId", "{\"codeParticipant\":\"99003\",\"sub");
        consommateur.traiter("sbx-campagne.verdict", parDonnees.replace("\"data\":{", "\"data\":{\"codeParticipant\":\"99003\",\"id\":null,"));
        ArgumentCaptor<EnveloppeSandbox> c = ArgumentCaptor.forClass(EnveloppeSandbox.class);
        verify(registre).inscrire(c.capture(), anyString());
        assertThat(c.getValue().institution()).isEqualTo("99003");
        assertThat(c.getValue().acteur()).isEqualTo("u-certificateur");
    }

    @Test
    @DisplayName("Champ composé, nul ou blanc lu comme absent : identifiant objet ou blanc au rebut ID_ABSENT")
    void champsNonTextuels() {
        String objet = VERDICT.replace("\"id\":\"v-1\"", "\"id\":{\"v\":1}");
        consommateur.traiter("sbx-campagne.verdict", objet);
        verify(registre).rebuter(isNull(), eq("sbx-campagne.verdict"), eq("ID_ABSENT"), eq(objet));
        String blanc = VERDICT.replace("\"id\":\"v-1\"", "\"id\":\"  \"");
        consommateur.traiter("sbx-campagne.verdict", blanc);
        verify(registre).rebuter(isNull(), eq("sbx-campagne.verdict"), eq("ID_ABSENT"), eq(blanc));
        String nul = VERDICT.replace("\"id\":\"v-1\"", "\"id\":null");
        consommateur.traiter("sbx-campagne.verdict", nul);
        verify(registre).rebuter(isNull(), eq("sbx-campagne.verdict"), eq("ID_ABSENT"), eq(nul));
        verify(registre, never()).inscrire(any(), anyString());
    }
}
