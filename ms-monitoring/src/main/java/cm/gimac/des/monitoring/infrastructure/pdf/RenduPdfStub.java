package cm.gimac.des.monitoring.infrastructure.pdf;

import cm.gimac.des.monitoring.application.port.out.RenduPdfPort;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mise en page simulée (développement et tests, D-117) : PDF d'une page qui reprend le texte du document, marqué
 * « Exemple de démonstration » ; le rendu fidèle est celui du moteur de MS-DOCUMENT.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.rendu-pdf", havingValue = "stub", matchIfMissing = true)
public class RenduPdfStub implements RenduPdfPort {

    @Override
    public byte[] rendre(String html, String piedDePage, boolean paysage) {
        String texte = html.replaceAll("(?s)<style.*?</style>", "").replaceAll("<(br|/tr|/h1|/h2|/p|/li)[^>]*>", "\n").replaceAll("<[^>]+>", " ");
        List<String> lignes = new ArrayList<>();
        lignes.add("Exemple de démonstration (mise en page simulée)");
        for (String l : texte.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'").replace("&quot;", "\"").split("\n")) {
            String x = l.replaceAll("\\s+", " ").strip();
            if (!x.isEmpty() && lignes.size() < 45) {
                lignes.add(x.length() > 110 ? x.substring(0, 110) : x);
            }
        }
        StringBuilder flux = new StringBuilder();
        for (int i = 0; i < lignes.size(); i++) {
            String l = lignes.get(i).replaceAll("[()\\\\]", "").replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
            flux.append("BT /F1 ").append(i == 0 ? 13 : 9).append(" Tf 36 ").append((paysage ? 560 : 806) - i * 12).append(" Td (").append(l).append(") Tj ET\n");
        }
        String largeur = paysage ? "842 595" : "595 842";
        String[] objets = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + largeur + "] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length " + flux.toString().getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + flux + "endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < objets.length; i++) {
            positions.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objets[i]).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objets.length + 1).append("\n0000000000 65535 f \n");
        positions.forEach(p -> pdf.append(String.format("%010d 00000 n \n", p)));
        pdf.append("trailer\n<< /Size ").append(objets.length + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
