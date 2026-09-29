package cm.gimac.des.document.web.rest.errors;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.domain.DocumentException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Erreurs en ProblemDetail : code, message, détails, identifiant de corrélation. */
class DocumentExceptionHandlerTest {

    final DocumentExceptionHandler handler = new DocumentExceptionHandler();

    @Test
    @DisplayName("Erreur métier : statut, code, détails et corrélation reçue de la passerelle")
    void erreurMetier() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader("X-Correlation-Id", "corr-1");
        ResponseEntity<ProblemDetail> r = handler.traiter(DocumentException.tropVolumineux(30, 20), requete);

        assertThat(r.getStatusCode().value()).isEqualTo(413);
        ProblemDetail pd = r.getBody();
        assertThat(pd.getTitle()).isEqualTo("DOC_FILE_TOO_LARGE");
        assertThat(pd.getType()).hasToString("https://des.gimac.local/problemes/DOC_FILE_TOO_LARGE");
        assertThat(pd.getProperties())
            .containsEntry("code", "DOC_FILE_TOO_LARGE")
            .containsEntry("message", "Le fichier dépasse la taille maximale admise")
            .containsEntry("details", Map.of("taille", 30L, "tailleMax", 20L))
            .containsEntry("correlationId", "corr-1");
    }

    @Test
    @DisplayName("Limite du serveur (multipart) : 413 DOC_FILE_TOO_LARGE, avec ou sans taille connue")
    void limiteMultipart() {
        ResponseEntity<ProblemDetail> r = handler.traiter(new MaxUploadSizeExceededException(20_971_520L), new MockHttpServletRequest());
        assertThat(r.getStatusCode().value()).isEqualTo(413);
        assertThat(r.getBody().getProperties()).containsEntry("code", "DOC_FILE_TOO_LARGE").containsEntry("details", Map.of("tailleMax", 20_971_520L));
        assertThat(r.getBody().getProperties().get("correlationId")).isNotNull();

        ResponseEntity<ProblemDetail> inconnue = handler.traiter(new MaxUploadSizeExceededException(-1), new MockHttpServletRequest());
        assertThat(inconnue.getBody().getProperties()).containsEntry("details", Map.of());
    }
}
