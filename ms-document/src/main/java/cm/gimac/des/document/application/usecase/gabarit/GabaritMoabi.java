package cm.gimac.des.document.application.usecase.gabarit;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Classification;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Fait;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Gabarit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gabarit commun des PDF de Moabi (D-139), extrait de l'organigramme de MS-IAM (étalon, D-102) : le service appelant
 * fournit le corps du document ; le gabarit l'enveloppe d'un document HTML autonome (polices de la charte et logos
 * embarqués), d'un en-tête aux logos de Moabi et du GIMAC (surtitre, titre, sous-titre, date d'édition à l'heure de
 * Douala, filet bleu à accent vert), d'un cartouche des données clés et d'un filigrane facultatifs, et compose le pied
 * de chaque page : « Moabi · titre · référence · page n sur N » à gauche, date et classification à droite. Textes du
 * gabarit en fr, en ou es. Tout texte issu de la demande est échappé, sauf le corps et le style, contrôlés par
 * l'appelant (RenduPdfService) comme tout document du rendu interne.
 */
public final class GabaritMoabi {

    /** Fuseau de la date d'édition. */
    public static final ZoneId DOUALA = ZoneId.of("Africa/Douala");

    /** Document composé : HTML, pied de page et métadonnées du PDF (titre, sujet, auteur, mots-clés). */
    public record Compose(String html, String piedDePage, Map<String, String> metadonnees) {}

    static final String GIMAC = "Groupement Interbancaire Monétique de l'Afrique Centrale";

    /** Au-delà de ce nombre de caractères, le titre est imprimé en corps réduit. */
    static final int TITRE_LONG = 40;

    private static final Map<String, Map<String, String>> TEXTES = Map.of(
        "fr", Map.of(
            "edition", "Édité le %s (heure de Douala)",
            "date", "d MMMM yyyy 'à' HH:mm",
            "court", "dd/MM/yyyy HH:mm",
            "page", "page",
            "sur", "sur",
            "PUBLIC", "Document public",
            "INTERNE", "Document interne · usage interne",
            "CONFIDENTIEL", "Document confidentiel · usage interne"
        ),
        "en", Map.of(
            "edition", "Issued on %s (Douala time)",
            "date", "MMMM d, yyyy 'at' HH:mm",
            "court", "yyyy-MM-dd HH:mm",
            "page", "page",
            "sur", "of",
            "PUBLIC", "Public document",
            "INTERNE", "Internal document · internal use",
            "CONFIDENTIEL", "Confidential document · internal use"
        ),
        "es", Map.of(
            "edition", "Emitido el %s (hora de Duala)",
            "date", "d 'de' MMMM 'de' yyyy 'a las' HH:mm",
            "court", "dd/MM/yyyy HH:mm",
            "page", "página",
            "sur", "de",
            "PUBLIC", "Documento público",
            "INTERNE", "Documento interno · uso interno",
            "CONFIDENTIEL", "Documento confidencial · uso interno"
        )
    );

    private GabaritMoabi() {}

    /** Langue retenue : fr, en ou es, fr par défaut. */
    public static String langue(String l) {
        String x = l == null ? "" : l.strip().toLowerCase(Locale.ROOT);
        return TEXTES.containsKey(x) ? x : "fr";
    }

    /**
     * Compose le document.
     *
     * @param corps fragment HTML du corps
     * @param g en-tête et pied (titre et date d'édition présents)
     * @param etroit papier de moins de 250 mm de large hors marges : en-tête resserré, comme l'organigramme
     */
    public static Compose composer(String corps, Gabarit g, boolean etroit) {
        String l = langue(g.langue());
        Map<String, String> t = TEXTES.get(l);
        Locale loc = Locale.forLanguageTag(l);
        String edition = DateTimeFormatter.ofPattern(t.get("date"), loc).format(g.edition().atZoneSameInstant(DOUALA));
        StringBuilder h = new StringBuilder(corps.length() + 700_000);
        h.append("<!DOCTYPE html><html lang=\"").append(l).append("\"><head><meta charset=\"utf-8\"><title>").append(esc(g.titre())).append("</title><style>");
        h.append(RessourcesGabarit.polices()).append(StylesGabarit.FEUILLE);
        if (g.style() != null && !g.style().isBlank()) {
            h.append(g.style());
        }
        h.append("</style></head><body").append(etroit ? " class=\"etroit\"" : "").append(">");
        if (vide(g.filigrane()) != null) {
            h.append("<div class=\"filigrane\">").append(esc(g.filigrane())).append("</div>");
        }
        h.append("<header class=\"tete\"><img class=\"logo-moabi\" alt=\"Moabi\" src=\"").append(RessourcesGabarit.logoMoabi()).append("\">");
        h.append("<div class=\"titres\"><div class=\"surtitre\">").append(esc(vide(g.surtitre()) == null ? GIMAC : g.surtitre())).append("</div>");
        // Titre long (programme d'astreinte) : corps réduit, pour tenir sur une ligne comme celui de l'organigramme.
        h.append(g.titre().length() > TITRE_LONG ? "<h1 class=\"long\">" : "<h1>").append(esc(g.titre())).append("</h1>");
        if (vide(g.sousTitre()) != null) {
            h.append("<div class=\"sous-titre\">").append(esc(g.sousTitre())).append("</div>");
        }
        h.append("<div class=\"edition\">").append(esc(String.format(t.get("edition"), edition))).append("</div></div>");
        h.append("<img class=\"logo-gimac\" alt=\"GIMAC\" src=\"").append(RessourcesGabarit.logoGimac()).append("\"></header>");
        List<Fait> faits = g.faits() == null ? List.of() : g.faits().stream().filter(f -> f != null && vide(f.libelle()) != null).toList();
        if (!faits.isEmpty()) {
            h.append("<section class=\"faits\">");
            for (Fait f : faits) {
                h.append("<div class=\"fait\"><div class=\"fait-libelle\">").append(esc(f.libelle())).append("</div><div class=\"fait-valeur\">");
                h.append(vide(f.valeur()) == null ? "—" : esc(f.valeur())).append("</div></div>");
            }
            h.append("</section>");
        }
        h.append("<main>").append(corps).append("</main></body></html>");
        return new Compose(h.toString(), pied(g, t, loc), metadonnees(g));
    }

    private static String pied(Gabarit g, Map<String, String> t, Locale loc) {
        Classification c = g.classification() == null ? Classification.INTERNE : g.classification();
        String date = DateTimeFormatter.ofPattern(t.get("court"), loc).format(g.edition().atZoneSameInstant(DOUALA));
        StringBuilder p = new StringBuilder(1_000);
        p.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>").append(StylesGabarit.PIED).append("</style></head><body><div class=\"pied\">");
        p.append("<span class=\"g\"><span class=\"m\">Moabi</span> · ").append(esc(g.titre()));
        if (vide(g.reference()) != null) {
            p.append(" · ").append(esc(g.reference()));
        }
        p.append(" · ").append(t.get("page")).append(" <span class=\"pageNumber\"></span> ").append(t.get("sur")).append(" <span class=\"totalPages\"></span></span>");
        p.append("<span class=\"d\">").append(esc(date)).append(" · <span class=\"c\">").append(esc(t.get(c.name()))).append("</span></span>");
        p.append("</div></body></html>");
        return p.toString();
    }

    private static Map<String, String> metadonnees(Gabarit g) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Title", g.titre());
        m.put("Author", "Moabi · GIMAC");
        m.put("Creator", "Moabi");
        if (vide(g.sousTitre()) != null) {
            m.put("Subject", g.sousTitre());
        }
        if (vide(g.reference()) != null) {
            m.put("Keywords", g.reference());
        }
        return m;
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Échappement HTML de tout texte issu de la demande. */
    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(ch < 0x20 && ch != '\n' && ch != '\t' ? ' ' : ch);
            }
        }
        return b.toString();
    }
}
