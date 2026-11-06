package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** PDF minimal de développement ; le profil production utilise MS-DOCUMENT. */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.rendu-pdf", havingValue = "stub", matchIfMissing = true)
public class RenduPdfStubAdapter implements RenduPdfPort {
    @Override
    public byte[] rendre(Document document) {
        String texte = "BT /F1 11 Tf 36 800 Td (Moabi - demande de processus) Tj ET";
        String pdf = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/Parent 2 0 R/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>endobj\n"
            + "4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n5 0 obj<</Length " + texte.length() + ">>stream\n" + texte + "\nendstream endobj\n"
            + "trailer<</Root 1 0 R>>\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }
}
