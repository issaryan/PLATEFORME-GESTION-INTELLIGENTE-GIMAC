package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Demande;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Fait;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Format;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Gabarit;
import cm.gimac.des.document.application.port.out.ConvertisseurHtml;
import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rendu PDF avec le gabarit commun (D-139) : enveloppe, pied et métadonnées transmis au moteur, contrôles du gabarit. */
class RenduPdfGabaritTest {

    static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
    static final OffsetDateTime EDITION = OffsetDateTime.parse("2026-11-06T09:30:00+01:00");

    final AtomicReference<String> html = new AtomicReference<>();
    final AtomicReference<String> pied = new AtomicReference<>();
    final AtomicReference<Map<String, String>> meta = new AtomicReference<>();

    final RenduPdfService service = new RenduPdfService(
        new ConvertisseurHtml() {
            @Override
            public byte[] imprimer(String h, String p, Papier papier) {
                throw new AssertionError("Le gabarit transmet ses métadonnées");
            }

            @Override
            public byte[] imprimer(String h, String p, Papier papier, Map<String, String> m) {
                html.set(h);
                pied.set(p);
                meta.set(m);
                return PDF;
            }
        }
    );

    static Gabarit gabarit(String titre, OffsetDateTime edition, List<Fait> faits, String style) {
        return new Gabarit(titre, null, null, "CHG-1", null, "fr", edition, null, faits, style);
    }

    static Demande demande(Gabarit g, String pied, boolean paysage) {
        return new Demande("<h2>Données</h2>", pied, Format.A4, paysage, g);
    }

    @Test
    @DisplayName("Corps enveloppé par le gabarit, pied composé, métadonnées ; A4 portrait étroit, A4 paysage non")
    void enveloppe() {
        assertThat(service.rendre(demande(gabarit("Demande de changement", EDITION, List.of(new Fait("État", "En cours")), ".x{}"), null, false), "pilotage-service")).isEqualTo(PDF);
        assertThat(html.get()).contains("<body class=\"etroit\">", "<h1>Demande de changement</h1>", "<main><h2>Données</h2></main>", ".x{}");
        assertThat(pied.get()).contains("Demande de changement · CHG-1 · page");
        assertThat(meta.get()).containsEntry("Title", "Demande de changement").containsEntry("Keywords", "CHG-1");
        service.rendre(demande(gabarit("Programme", EDITION, null, null), " ", true), "monitoring-service");
        assertThat(html.get()).contains("<body>");
    }

    @Test
    @DisplayName("Gabarit incomplet ou abusif : 400 DOC_RENDU_INVALIDE, moteur non appelé")
    void refus() {
        String long_ = "x".repeat(RenduPdfService.TEXTE_MAX + 1);
        List<Demande> refusees = List.of(
            demande(gabarit("Titre", EDITION, null, null), "<p>pied</p>", false),
            demande(gabarit(" ", EDITION, null, null), null, false),
            demande(gabarit(null, EDITION, null, null), null, false),
            demande(gabarit("Titre", null, null, null), null, false),
            demande(gabarit(long_, EDITION, null, null), null, false),
            demande(gabarit("Titre", EDITION, List.of(new Fait("État", long_)), null), null, false),
            demande(gabarit("Titre", EDITION, Collections.nCopies(RenduPdfService.FAITS_MAX + 1, new Fait("a", "b")), null), null, false),
            demande(gabarit("Titre", EDITION, null, "</style><script>x()</script>"), null, false),
            demande(gabarit("Titre", EDITION, null, "</style><p>"), null, false),
            demande(gabarit("Titre", EDITION, null, "body{background:url(http://interne/x)}"), null, false),
            new Demande("<img src=\"http://x/y.png\">", null, Format.A4, false, gabarit("Titre", EDITION, null, null))
        );
        for (Demande d : refusees) {
            assertThatThrownBy(() -> service.rendre(d, "pilotage-service")).isInstanceOfSatisfying(DocumentException.class, e -> assertThat(e.code()).isEqualTo("DOC_RENDU_INVALIDE"));
        }
        assertThat(html.get()).isNull();
    }

    @Test
    @DisplayName("Données clés nulles admises ; style compté dans la taille maximale")
    void bornes() {
        List<Fait> faits = new java.util.ArrayList<>();
        faits.add(null);
        service.rendre(demande(gabarit("Titre", EDITION, faits, null), null, false), "pilotage-service");
        assertThat(html.get()).doesNotContain("class=\"faits\"");
        String style = "a{}".repeat(RenduPdfService.TAILLE_MAX / 3 + 1);
        assertThatThrownBy(() -> service.rendre(demande(gabarit("Titre", EDITION, null, style), null, false), "x")).isInstanceOfSatisfying(DocumentException.class, e ->
            assertThat(e.code()).isEqualTo("DOC_RENDU_TROP_VOLUMINEUX")
        );
    }
}
