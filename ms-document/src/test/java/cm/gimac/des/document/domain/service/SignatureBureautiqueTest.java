package cm.gimac.des.document.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Formats de l'aperçu bureautique et signatures (D-83, apercu-bureautique.md §2). */
class SignatureBureautiqueTest {

    static final byte[] ZIP = { 'P', 'K', 3, 4, 20, 0, 6, 0 };
    static final byte[] OLE2 = { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0 };
    static final byte[] RTF = "{\\rtf1\\ansi Bonjour}".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @ValueSource(strings = { "rapport.docx", "Présentation finale.PPTX", "budget.xlsx", "note.odt", "diapos.odp", "calc.ods", "ancien.doc", "ancien.ppt", "ancien.xls", "lettre.rtf", "a.b.c.Docx" })
    @DisplayName("Extensions admises, quelle que soit la casse")
    void extensionsAdmises(String nom) {
        assertThat(SignatureBureautique.extension(nom)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = { "rapport.pdf", "image.png", "script.js", "macro.docm", "modele.dotx", "sans-extension", "fin-par-point.", ".docx.exe" })
    @DisplayName("Extensions hors liste ou absentes : refusées")
    void extensionsRefusees(String nom) {
        assertThat(SignatureBureautique.extension(nom)).isEmpty();
    }

    @Test
    @DisplayName("Nom absent : aucune extension")
    void nomAbsent() {
        assertThat(SignatureBureautique.extension(null)).isEmpty();
    }

    @Test
    @DisplayName("Open XML et OpenDocument : conteneur ZIP")
    void zip() {
        for (String ext : new String[] { "docx", "pptx", "xlsx", "odt", "odp", "ods" }) {
            assertThat(SignatureBureautique.concorde(ext, ZIP)).as(ext).isTrue();
            assertThat(SignatureBureautique.concorde(ext, PDF)).as(ext).isFalse();
            assertThat(SignatureBureautique.concorde(ext, RTF)).as(ext).isFalse();
        }
    }

    @Test
    @DisplayName("Open XML protégé par mot de passe (OLE2 chiffré) admis, pour un refus 422 du moteur ; pas OpenDocument")
    void openXmlChiffre() {
        assertThat(SignatureBureautique.concorde("docx", OLE2)).isTrue();
        assertThat(SignatureBureautique.concorde("pptx", OLE2)).isTrue();
        assertThat(SignatureBureautique.concorde("xlsx", OLE2)).isTrue();
        assertThat(SignatureBureautique.concorde("odt", OLE2)).isFalse();
    }

    @Test
    @DisplayName("doc, ppt, xls : OLE2 seulement")
    void ole2() {
        for (String ext : new String[] { "doc", "ppt", "xls" }) {
            assertThat(SignatureBureautique.concorde(ext, OLE2)).as(ext).isTrue();
            assertThat(SignatureBureautique.concorde(ext, ZIP)).as(ext).isFalse();
        }
    }

    @Test
    @DisplayName("rtf : {\\rtf en tête")
    void rtf() {
        assertThat(SignatureBureautique.concorde("rtf", RTF)).isTrue();
        assertThat(SignatureBureautique.concorde("rtf", "{\\rt".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(SignatureBureautique.concorde("rtf", PDF)).isFalse();
    }

    @Test
    @DisplayName("Contenu vide, trop court ou extension inconnue : discordance")
    void casLimites() {
        assertThat(SignatureBureautique.concorde("docx", new byte[0])).isFalse();
        assertThat(SignatureBureautique.concorde("docx", new byte[] { 'P', 'K' })).isFalse();
        assertThat(SignatureBureautique.concorde("docx", null)).isFalse();
        assertThat(SignatureBureautique.concorde("pdf", PDF)).isFalse();
    }
}
