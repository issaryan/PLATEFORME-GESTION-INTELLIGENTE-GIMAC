package cm.gimac.des.document.infrastructure.texte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/** Fabrique de PDF d'une page pour les tests du contrôle de masquage. */
public final class PdfDeTest {

    private PdfDeTest() {}

    /** PDF lisible contenant la ligne de texte donnée. */
    public static byte[] avecTexte(String texte) {
        return pdf(texte, null);
    }

    /** PDF chiffré par un mot de passe d'ouverture : illisible sans lui. */
    public static byte[] chiffre(String texte) {
        return pdf(texte, "secret");
    }

    private static byte[] pdf(String texte, String motDePasse) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream flux = new PDPageContentStream(doc, page)) {
                flux.beginText();
                flux.setFont(PDType1Font.HELVETICA, 12);
                flux.newLineAtOffset(50, 700);
                flux.showText(texte);
                flux.endText();
            }
            if (motDePasse != null) {
                StandardProtectionPolicy politique = new StandardProtectionPolicy("proprietaire", motDePasse, new AccessPermission());
                politique.setEncryptionKeyLength(128);
                doc.protect(politique);
            }
            doc.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
