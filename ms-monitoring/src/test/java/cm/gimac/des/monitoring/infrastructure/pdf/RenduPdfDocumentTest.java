package cm.gimac.des.monitoring.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.application.port.out.RenduPdfPort.Document;
import cm.gimac.des.monitoring.application.port.out.RenduPdfPort.Fait;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Requête interne au gabarit commun (D-139) et mise en page simulée du programme. */
class RenduPdfDocumentTest {

    static final Document DOCUMENT = new Document(
        "<h2>Créneaux</h2><h3>Matin</h3><table><tr><td>Lundi</td></tr></table>", "Liste des agents", "Programme 2026-W46, révision 2", "programme-2026-W46-r2",
        "INTERNE", "fr", Instant.parse("2026-11-06T09:30:00Z"), "Projet", List.of(new Fait("Semaine", "2026-W46")), true
    );

    @Test
    @DisplayName("Corps seul, A4 paysage, pied laissé au gabarit, date d'édition ISO 8601")
    void requete() {
        RenduPdfDocument.Requete r = RenduPdfDocument.requete(DOCUMENT);
        assertThat(r.html()).isEqualTo(DOCUMENT.corps());
        assertThat(r.piedDePage()).isNull();
        assertThat(r.format()).isEqualTo("A4");
        assertThat(r.orientation()).isEqualTo("PAYSAGE");
        assertThat(r.gabarit()).isEqualTo(
            new RenduPdfDocument.Gabarit("Liste des agents", "Programme 2026-W46, révision 2", "programme-2026-W46-r2", "INTERNE", "fr", "2026-11-06T09:30Z", "Projet", List.of(new Fait("Semaine", "2026-W46")))
        );
        Document portrait = new Document("x", "t", null, null, null, null, DOCUMENT.edition(), null, List.of(), false);
        assertThat(RenduPdfDocument.requete(portrait).orientation()).isEqualTo("PORTRAIT");
    }

    @Test
    @DisplayName("Mise en page simulée : PDF lisible reprenant titre, sous-titre et corps")
    void simulation() {
        byte[] pdf = new RenduPdfStub().rendre(DOCUMENT);
        String texte = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(texte).startsWith("%PDF-1.4").contains("Liste des agents", "Programme 2026-W46, r", "Matin", "Lundi", "842 595");
        Document portrait = new Document("<p>x</p>", "t", null, null, null, null, DOCUMENT.edition(), null, List.of(), false);
        assertThat(new String(new RenduPdfStub().rendre(portrait), StandardCharsets.ISO_8859_1)).contains("595 842");
    }
}
