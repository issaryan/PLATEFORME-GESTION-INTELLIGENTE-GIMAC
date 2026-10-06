package cm.gimac.des.document.infrastructure.apercu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.usecase.ApercuProprietes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Conversion réelle par un moteur Gotenberg en service, à la demande seulement : GIMAC_APERCU_ESSAI_URL
 * (par exemple http://127.0.0.1:3000 pour « docker run -p 127.0.0.1:3000:3000 gotenberg/gotenberg:8.37.0 »).
 */
@EnabledIfEnvironmentVariable(named = "GIMAC_APERCU_ESSAI_URL", matches = "https?://.+")
class ConvertisseurApercuGotenbergReelTest {

    static ConvertisseurApercuGotenberg adaptateur() {
        return new ConvertisseurApercuGotenberg(new ApercuProprietes(System.getenv("GIMAC_APERCU_ESSAI_URL"), null, null, null, null, null, null));
    }

    /** docx minimal : types de contenu, relation principale, corps du document. */
    static byte[] docx(String texte) throws IOException {
        Map<String, String> parties = Map.of(
            "[Content_Types].xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
            "_rels/.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>",
            "word/document.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:body><w:p><w:r><w:t>" +
            texte +
            "</w:t></w:r></w:p></w:body></w:document>"
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (var p : parties.entrySet()) {
                zip.putNextEntry(new ZipEntry(p.getKey()));
                zip.write(p.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    @DisplayName("docx et rtf convertis en PDF")
    void conversions() throws IOException {
        assertThat(new String(adaptateur().convertirEnPdf("docx", docx("Aperçu D-83")), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        byte[] rtf = "{\\rtf1\\ansi Aper\\'e7u D-83}".getBytes(StandardCharsets.US_ASCII);
        assertThat(new String(adaptateur().convertirEnPdf("rtf", rtf), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Fichier corrompu : conversion refusée")
    void corrompu() {
        byte[] faux = "PK\u0003\u0004pas un docx".getBytes(StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> adaptateur().convertirEnPdf("docx", faux)).isInstanceOf(ConversionRefusee.class);
    }
}
