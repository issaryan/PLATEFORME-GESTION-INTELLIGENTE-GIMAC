package cm.gimac.des.document.infrastructure.texte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Extraction du texte pour le contrôle de masquage (BNF-16). */
class ExtracteurTextePdfBoxTest {

    final ExtracteurTextePdfBox extracteur = new ExtracteurTextePdfBox();

    @ParameterizedTest
    @ValueSource(strings = { "text/plain", "text/csv", "application/json", "application/xml", "application/csv" })
    @DisplayName("Textes : lus en UTF-8")
    void textes(String type) {
        assertThat(extracteur.extraire("Carte 4111111111111111 é".getBytes(StandardCharsets.UTF_8), type)).contains("Carte 4111111111111111 é");
    }

    @Test
    @DisplayName("PDF : texte extrait par PDFBox")
    void pdf() {
        assertThat(extracteur.extraire(PdfDeTest.avecTexte("Titulaire carte 4111111111111111"), "application/pdf")).hasValueSatisfying(t ->
            assertThat(t).contains("4111111111111111")
        );
    }

    @Test
    @DisplayName("PDF illisible ou chiffré : 422 DOC_PDF_UNREADABLE")
    void pdfIllisible() {
        byte[] tronque = "%PDF-1.7\nceci n'est pas un PDF".getBytes(StandardCharsets.US_ASCII);
        assertThatThrownBy(() -> extracteur.extraire(tronque, "application/pdf")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(422);
            assertThat(e.code()).isEqualTo("DOC_PDF_UNREADABLE");
        });
        assertThatThrownBy(() -> extracteur.extraire(PdfDeTest.chiffre("4111111111111111"), "application/pdf")).hasFieldOrPropertyWithValue(
            "code",
            "DOC_PDF_UNREADABLE"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/png", "image/jpeg", "application/octet-stream", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" })
    @DisplayName("Images et documents bureautiques : non lus (hors périmètre de ce contrôle)")
    void nonLus(String type) {
        assertThat(extracteur.extraire(new byte[] { 1, 2 }, type)).isEmpty();
    }
}
