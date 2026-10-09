package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Demande;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Format;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
import cm.gimac.des.document.application.port.out.ConvertisseurHtml;
import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Rendu PDF interne (D-102) : papier, contrôles du document (taille, éléments actifs, ressources externes), erreurs du moteur. */
class RenduPdfServiceTest {

    static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
    final AtomicReference<ConvertisseurHtml.Papier> papier = new AtomicReference<>();
    volatile RuntimeException panne;

    final RenduPdfService service = new RenduPdfService((html, pied, p) -> {
        if (panne != null) {
            throw panne;
        }
        papier.set(p);
        return PDF;
    });

    static final String DOCUMENT =
        "<!DOCTYPE html><html><head><style>@font-face{src:url(data:font/woff2;base64,AAAA)}</style></head><body>" +
        "<img src=\"data:image/png;base64,AAAA\"><svg xmlns=\"http://www.w3.org/2000/svg\"><path d=\"M0 0\"/></svg><a href=\"#haut\">haut</a></body></html>";

    @Test
    @DisplayName("Papier A3 paysage, A4 portrait, marges de 12 mm et 16 mm en bas ; document autonome accepté")
    void papier() {
        assertThat(service.rendre(new Demande(DOCUMENT, "<p>pied</p>", Format.A3, true), "iam-service")).isEqualTo(PDF);
        assertThat(papier.get()).isEqualTo(new ConvertisseurHtml.Papier(420, 297, 12, 16, 12));
        service.rendre(new Demande(DOCUMENT, null, Format.A4, false), "iam-service");
        assertThat(papier.get()).isEqualTo(new ConvertisseurHtml.Papier(210, 297, 12, 16, 12));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "<script>alert(1)</script>",
            "<img src=\"http://169.254.169.254/latest\">",
            "<img src='https://exemple.org/a.png'>",
            "<div style=\"background:url(http://interne:8081/x)\">",
            "<style>@import 'http://x/y.css';</style>",
            "<iframe src=\"data:text/html,x\"></iframe>",
            "<link rel=stylesheet href=//x/y.css>",
            "<img src=file:///etc/passwd>",
            "<meta http-equiv=\"refresh\" content=\"0;url=http://x\">",
            "<body onload=\"x()\">",
            "<a href=\"javascript:x()\">x</a>",
            "<object data=\"x\"></object>",
        }
    )
    @DisplayName("Élément actif ou ressource externe : 400 DOC_RENDU_INVALIDE, le moteur n'est pas appelé")
    void refuse(String fragment) {
        assertThatThrownBy(() -> service.rendre(new Demande("<html><body>" + fragment + "</body></html>", null, Format.A4, true), "iam-service"))
            .isInstanceOfSatisfying(DocumentException.class, e -> {
                assertThat(e.code()).isEqualTo("DOC_RENDU_INVALIDE");
                assertThat(e.status()).isEqualTo(400);
            });
        assertThat(papier.get()).isNull();
    }

    @Test
    @DisplayName("Pied de page contrôlé aussi ; vide, sans format : 400 ; au-delà de 4 Mio : 413")
    void controles() {
        assertThatThrownBy(() -> service.rendre(new Demande(DOCUMENT, "<img src=\"http://x\">", Format.A4, true), "c")).hasFieldOrPropertyWithValue("code", "DOC_RENDU_INVALIDE");
        assertThatThrownBy(() -> service.rendre(new Demande(" ", null, Format.A4, true), "c")).hasFieldOrPropertyWithValue("code", "DOC_RENDU_INVALIDE");
        assertThatThrownBy(() -> service.rendre(new Demande(DOCUMENT, null, null, true), "c")).hasFieldOrPropertyWithValue("code", "DOC_RENDU_INVALIDE");
        assertThatThrownBy(() -> service.rendre(null, "c")).hasFieldOrPropertyWithValue("code", "DOC_RENDU_INVALIDE");
        String gros = "<html>" + "a".repeat(RenduPdfService.TAILLE_MAX) + "</html>";
        assertThatThrownBy(() -> service.rendre(new Demande(gros, null, Format.A4, true), "c")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.code()).isEqualTo("DOC_RENDU_TROP_VOLUMINEUX");
            assertThat(e.status()).isEqualTo(413);
        });
    }

    @Test
    @DisplayName("Moteur : refus 422 DOC_RENDU_REFUSE, indisponible 503 DOC_RENDU_INDISPONIBLE")
    void moteur() {
        panne = new ConversionRefusee("refus");
        assertThatThrownBy(() -> service.rendre(new Demande(DOCUMENT, null, Format.A3, true), "c")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.code()).isEqualTo("DOC_RENDU_REFUSE");
            assertThat(e.status()).isEqualTo(422);
        });
        panne = new MoteurIndisponible("panne", null);
        assertThatThrownBy(() -> service.rendre(new Demande(DOCUMENT, null, Format.A3, true), "c")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.code()).isEqualTo("DOC_RENDU_INDISPONIBLE");
            assertThat(e.status()).isEqualTo(503);
        });
    }
}
