package cm.gimac.des.document.application.usecase.gabarit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Classification;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Fait;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Gabarit;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gabarit commun des PDF (D-139) : en-tête de l'organigramme, cartouche, filigrane, pied paginé, textes fr, en, es. */
class GabaritMoabiTest {

    static final OffsetDateTime EDITION = OffsetDateTime.parse("2026-11-06T09:30:00Z");

    static Gabarit gabarit(String langue, Classification c, List<Fait> faits, String filigrane) {
        return new Gabarit("Programme <d'astreinte>", null, "Semaine 46 & révision 2", "PRG-2026-W46-R2", c, langue, EDITION, filigrane, faits, ".x{color:red}");
    }

    @Test
    @DisplayName("Document autonome : polices et logos embarqués, en-tête aux deux logos, titres échappés, cartouche, corps et style")
    void document() {
        GabaritMoabi.Compose c = GabaritMoabi.composer(
            "<h2>Section</h2><table class=\"fiche\"></table>",
            gabarit("fr", null, Arrays.asList(new Fait("Référence", "PRG-1"), new Fait("État", null), new Fait(" ", "ignoré"), null), "Projet"),
            false
        );
        assertThat(c.html())
            .startsWith("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"utf-8\"><title>Programme &lt;d&#39;astreinte&gt;</title>")
            .contains("@font-face{font-family:'Ubuntu'", "@font-face{font-family:'Inter'", "data:font/woff2;base64,")
            .contains("class=\"logo-moabi\" alt=\"Moabi\" src=\"data:image/svg+xml;base64,", "class=\"logo-gimac\" alt=\"GIMAC\" src=\"data:image/png;base64,")
            .contains("<div class=\"surtitre\">Groupement Interbancaire Monétique de l&#39;Afrique Centrale</div>")
            .contains("<h1>Programme &lt;d&#39;astreinte&gt;</h1>", "<div class=\"sous-titre\">Semaine 46 &amp; révision 2</div>")
            .contains("Édité le 6 novembre 2026 à 10:30 (heure de Douala)")
            .contains("<div class=\"filigrane\">Projet</div>")
            .contains("<div class=\"fait-libelle\">Référence</div><div class=\"fait-valeur\">PRG-1</div>", "<div class=\"fait-libelle\">État</div><div class=\"fait-valeur\">—</div>")
            .doesNotContain("ignoré", "class=\"etroit\"")
            .contains(".x{color:red}</style>")
            .endsWith("<main><h2>Section</h2><table class=\"fiche\"></table></main></body></html>");
        assertThat(c.piedDePage())
            .contains("<span class=\"m\">Moabi</span> · Programme &lt;d&#39;astreinte&gt; · PRG-2026-W46-R2 · page <span class=\"pageNumber\"></span> sur <span class=\"totalPages\"></span>")
            .contains("06/11/2026 10:30 · <span class=\"c\">Document interne · usage interne</span>");
        assertThat(c.metadonnees())
            .containsEntry("Title", "Programme <d'astreinte>")
            .containsEntry("Author", "Moabi · GIMAC")
            .containsEntry("Creator", "Moabi")
            .containsEntry("Subject", "Semaine 46 & révision 2")
            .containsEntry("Keywords", "PRG-2026-W46-R2");
    }

    @Test
    @DisplayName("Anglais et espagnol : date, pagination et classification traduites ; papier étroit : en-tête resserré")
    void langues() {
        GabaritMoabi.Compose en = GabaritMoabi.composer("<p>x</p>", gabarit("EN ", Classification.CONFIDENTIEL, null, null), true);
        assertThat(en.html()).contains("<html lang=\"en\">", "<body class=\"etroit\">", "Issued on November 6, 2026 at 10:30 (Douala time)").doesNotContain("filigrane\">", "class=\"faits\"");
        assertThat(en.piedDePage()).contains("page <span class=\"pageNumber\"></span> of <span class=\"totalPages\"></span>", "2026-11-06 10:30", "Confidential document · internal use");
        GabaritMoabi.Compose es = GabaritMoabi.composer("<p>x</p>", gabarit("es", Classification.PUBLIC, List.of(), " "), false);
        assertThat(es.html()).contains("<html lang=\"es\">", "Emitido el 6 de noviembre de 2026 a las 10:30 (hora de Duala)").doesNotContain("filigrane\">");
        assertThat(es.piedDePage()).contains("página <span class=\"pageNumber\"></span> de <span class=\"totalPages\"></span>", "Documento público");
    }

    @Test
    @DisplayName("Sans sous-titre, référence ni style ; surtitre propre ; langue inconnue : français")
    void minimal() {
        Gabarit g = new Gabarit("Instance", "Processus du DES", " ", null, null, "de", EDITION, null, null, " ");
        GabaritMoabi.Compose c = GabaritMoabi.composer("", g, false);
        assertThat(c.html()).contains("<div class=\"surtitre\">Processus du DES</div>", "<html lang=\"fr\">", "<h1>Instance</h1>").doesNotContain("sous-titre\">");
        Gabarit long_ = new Gabarit("Liste des agents en permanence du 9 au 15 novembre 2026", null, null, null, null, "fr", EDITION, null, null, null);
        assertThat(GabaritMoabi.composer("", long_, false).html()).contains("<h1 class=\"long\">Liste des agents");
        assertThat(c.piedDePage()).contains("Moabi</span> · Instance · page").doesNotContain("Instance ·  ·");
        assertThat(c.metadonnees()).containsOnlyKeys("Title", "Author", "Creator");
        assertThat(GabaritMoabi.langue(null)).isEqualTo("fr");
        assertThat(GabaritMoabi.esc(null)).isEmpty();
        assertThat(GabaritMoabi.esc("a\u0001b\tc\nd\"")).isEqualTo("a b\tc\nd&quot;");
    }

    @Test
    @DisplayName("Ressource absente : erreur explicite")
    void ressourceAbsente() {
        assertThatThrownBy(() -> RessourcesGabarit.base64("gabarit/absente.svg")).isInstanceOf(IllegalStateException.class).hasMessageContaining("absente.svg");
    }
}
