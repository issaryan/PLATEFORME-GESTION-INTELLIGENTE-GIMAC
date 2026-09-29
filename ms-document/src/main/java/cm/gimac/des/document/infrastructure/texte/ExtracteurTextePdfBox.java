package cm.gimac.des.document.infrastructure.texte;

import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.domain.DocumentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extraction du texte pour le contrôle de masquage (BNF-16) : textes (text/*, JSON, XML, CSV) lus
 * en UTF-8, PDF par PDFBox (déjà présent pour la fusion reprise d'afriland). Un PDF illisible ou
 * chiffré est refusé, faute de pouvoir le contrôler. Les images et documents bureautiques ne sont
 * pas lus (reconnaissance de caractères hors périmètre) : leur contrôle relève de la chaîne 6.17.
 */
@Component
public class ExtracteurTextePdfBox implements ExtracteurTexte {

    private static final Set<String> TEXTES = Set.of("application/json", "application/xml", "application/csv");

    @Override
    public Optional<String> extraire(byte[] contenu, String typeMime) {
        if (typeMime.startsWith("text/") || TEXTES.contains(typeMime)) {
            return Optional.of(new String(contenu, StandardCharsets.UTF_8));
        }
        if ("application/pdf".equals(typeMime)) {
            return Optional.of(textePdf(contenu));
        }
        return Optional.empty();
    }

    static String textePdf(byte[] contenu) {
        try (PDDocument pdf = PDDocument.load(contenu)) {
            return new PDFTextStripper().getText(pdf);
        } catch (IOException e) {
            throw DocumentException.refuse(
                "DOC_PDF_UNREADABLE",
                "PDF illisible ou chiffré : le contrôle des numéros de carte est impossible",
                Map.of()
            );
        }
    }
}
