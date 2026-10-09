package cm.gimac.des.document.infrastructure.apercu;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase;
import cm.gimac.des.document.application.usecase.ApercuProprietes;
import cm.gimac.des.document.application.usecase.RenduPdfService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Impression réelle par un moteur Gotenberg en service (D-102), à la demande seulement : GIMAC_APERCU_ESSAI_URL (moteur
 * lancé avec les options du Compose). Avec GIMAC_ORGANIGRAMME_ESSAI_DOSSIER (documents écrits par
 * DocumentOrganigrammeTest de MS-IAM), l'organigramme de démonstration est imprimé dans ce dossier.
 */
@EnabledIfEnvironmentVariable(named = "GIMAC_APERCU_ESSAI_URL", matches = "https?://.+")
class ConvertisseurHtmlGotenbergReelTest {

    static RenduPdfService service() {
        return new RenduPdfService(new ConvertisseurHtmlGotenberg(new ApercuProprietes(System.getenv("GIMAC_APERCU_ESSAI_URL"), null, null, null, null, null, null)));
    }

    @Test
    @DisplayName("Document autonome imprimé en PDF")
    void impression() {
        byte[] pdf = service().rendre(new RenduPdfUseCase.Demande("<html><body><h1>Organigramme é</h1></body></html>", "<p style=\"font-size:8px\">page <span class=\"pageNumber\"></span></p>", RenduPdfUseCase.Format.A4, true), "essai");
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GIMAC_ORGANIGRAMME_ESSAI_DOSSIER", matches = ".+")
    @DisplayName("Organigramme de démonstration de MS-IAM imprimé (A3 paysage avec annexe, A4 portrait, branche A4 paysage)")
    void organigramme() throws IOException {
        Path d = Path.of(System.getenv("GIMAC_ORGANIGRAMME_ESSAI_DOSSIER"));
        String pied = Files.readString(d.resolve("pied.html"));
        Object[][] l = { { "organigramme-a3", RenduPdfUseCase.Format.A3, true }, { "organigramme-a4", RenduPdfUseCase.Format.A4, false }, { "organigramme-branche", RenduPdfUseCase.Format.A4, true } };
        for (Object[] x : l) {
            byte[] pdf = service().rendre(new RenduPdfUseCase.Demande(Files.readString(d.resolve(x[0] + ".html")), pied, (RenduPdfUseCase.Format) x[1], (Boolean) x[2]), "essai");
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            Files.write(d.resolve(x[0] + ".pdf"), pdf);
        }
        if (Files.exists(d.resolve("organigramme-demonstration.html"))) {
            byte[] pdf = service().rendre(
                new RenduPdfUseCase.Demande(Files.readString(d.resolve("organigramme-demonstration.html")), Files.readString(d.resolve("pied-demonstration.html")), RenduPdfUseCase.Format.A3, true),
                "essai"
            );
            Files.write(d.resolve("organigramme-demonstration.pdf"), pdf);
        }
    }
}
