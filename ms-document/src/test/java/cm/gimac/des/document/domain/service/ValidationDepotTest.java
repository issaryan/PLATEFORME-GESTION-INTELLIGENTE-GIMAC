package cm.gimac.des.document.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Règles d'admission du dépôt direct. */
class ValidationDepotTest {

    static final long MAX = 20L * 1024 * 1024;

    private static DocumentException erreur(Runnable r) {
        try {
            r.run();
        } catch (DocumentException e) {
            return e;
        }
        throw new AssertionError("DocumentException attendue");
    }

    @Test
    @DisplayName("Fichier absent : 400 DOC_FILE_MISSING")
    void fichierAbsent() {
        DocumentException e = erreur(() -> ValidationDepot.verifierContenu(null, MAX));
        assertThat(e.code()).isEqualTo("DOC_FILE_MISSING");
        assertThat(e.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Fichier vide : 400 DOC_FILE_EMPTY")
    void fichierVide() {
        DocumentException e = erreur(() -> ValidationDepot.verifierContenu(new byte[0], MAX));
        assertThat(e.code()).isEqualTo("DOC_FILE_EMPTY");
        assertThat(e.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Au-delà de 20 Mo : 413 DOC_FILE_TOO_LARGE ; 20 Mo exactement : admis")
    void taille() {
        DocumentException e = erreur(() -> ValidationDepot.verifierContenu(new byte[(int) MAX + 1], MAX));
        assertThat(e.code()).isEqualTo("DOC_FILE_TOO_LARGE");
        assertThat(e.status()).isEqualTo(413);
        assertThat(e.details()).containsEntry("taille", MAX + 1).containsEntry("tailleMax", MAX);
        assertThatCode(() -> ValidationDepot.verifierContenu(new byte[(int) MAX], MAX)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Finalité : normalisée en majuscules")
    void finaliteValide() {
        assertThat(ValidationDepot.finalite("ADHESION")).isEqualTo("ADHESION");
        assertThat(ValidationDepot.finalite(" preuve_homologation ")).isEqualTo("PREUVE_HOMOLOGATION");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "A", "1ADHESION", "ADHÉSION", "ADHESION;DROP", "ADHESION ETAPE" })
    @DisplayName("Finalité absente ou mal formée : 400 DOC_PURPOSE_INVALID")
    void finaliteInvalide(String finalite) {
        assertThat(erreur(() -> ValidationDepot.finalite(finalite)).code()).isEqualTo("DOC_PURPOSE_INVALID");
    }

    @Test
    @DisplayName("Référence : libre, rognée")
    void referenceValide() {
        assertThat(ValidationDepot.reference(" 3f2a/STATUTS ")).isEqualTo("3f2a/STATUTS");
        assertThat(ValidationDepot.reference("x".repeat(255))).hasSize(255);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "ligne\nsuivante", "tab\there" })
    @DisplayName("Référence absente ou avec caractère de contrôle : 400 DOC_REFERENCE_INVALID")
    void referenceInvalide(String reference) {
        assertThat(erreur(() -> ValidationDepot.reference(reference)).code()).isEqualTo("DOC_REFERENCE_INVALID");
    }

    @Test
    @DisplayName("Référence de plus de 255 caractères : refusée")
    void referenceTropLongue() {
        assertThat(erreur(() -> ValidationDepot.reference("x".repeat(256))).code()).isEqualTo("DOC_REFERENCE_INVALID");
    }

    @ParameterizedTest(name = "« {0} » → « {1} »")
    @CsvSource(
        delimiter = '|',
        nullValues = "NULL",
        value = {
            "statuts.pdf | statuts.pdf",
            "C:\\\\Users\\\\agent\\\\statuts.pdf | statuts.pdf",
            "../../etc/passwd | passwd",
            "NULL | document",
            "'   ' | document",
            ".. | document",
            "dossier/ | document",
        }
    )
    @DisplayName("Nom de fichier : dernier segment, sans chemin")
    void nomFichier(String nom, String attendu) {
        assertThat(ValidationDepot.nomFichier(nom)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("Nom de fichier : caractères de contrôle retirés, 255 caractères au plus")
    void nomFichierBorne() {
        assertThat(ValidationDepot.nomFichier("pi\u0000ece\r\n.pdf")).isEqualTo("piece.pdf");
        assertThat(ValidationDepot.nomFichier("a".repeat(300) + ".pdf")).hasSize(255);
    }

    @Test
    @DisplayName("Type MIME : déclaré, normalisé ; octet-stream s'il est absent ou mal formé")
    void typeMime() {
        byte[] texte = "bonjour".getBytes(StandardCharsets.UTF_8);
        assertThat(ValidationDepot.typeMime("text/plain; charset=UTF-8", texte)).isEqualTo("text/plain");
        assertThat(ValidationDepot.typeMime("Image/PNG", texte)).isEqualTo("image/png");
        assertThat(ValidationDepot.typeMime(null, texte)).isEqualTo(ValidationDepot.TYPE_PAR_DEFAUT);
        assertThat(ValidationDepot.typeMime("n'importe quoi", texte)).isEqualTo(ValidationDepot.TYPE_PAR_DEFAUT);
        assertThat(ValidationDepot.typeMime("", texte)).isEqualTo(ValidationDepot.TYPE_PAR_DEFAUT);
    }

    @Test
    @DisplayName("Type MIME : un contenu PDF est un PDF, quel que soit le type déclaré")
    void typeMimePdf() {
        byte[] pdf = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);
        assertThat(ValidationDepot.typeMime("application/octet-stream", pdf)).isEqualTo("application/pdf");
        assertThat(ValidationDepot.typeMime(null, pdf)).isEqualTo("application/pdf");
        assertThat(ValidationDepot.typeMime("text/plain", "%PD".getBytes(StandardCharsets.US_ASCII))).isEqualTo("text/plain");
        assertThat(ValidationDepot.commencePar(null, new byte[] { 1 })).isFalse();
    }

    @Test
    @DisplayName("Erreurs : fabriques de DocumentException")
    void fabriques() {
        assertThat(DocumentException.introuvable("x").status()).isEqualTo(404);
        assertThat(DocumentException.introuvable("x").details()).containsEntry("id", "x");
        assertThat(DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "m").status()).isEqualTo(503);
        assertThat(DocumentException.interdit("DOC_USER_TOKEN_REQUIRED", "m").status()).isEqualTo(403);
        assertThatThrownBy(() -> {
            throw DocumentException.refuse("DOC_PAN_DETECTED", "m", java.util.Map.of("occurrences", 1));
        })
            .isInstanceOf(DocumentException.class)
            .hasMessage("m");
    }
}
