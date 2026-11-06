package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
import cm.gimac.des.document.application.port.out.ConvertisseurHtml;
import cm.gimac.des.document.application.usecase.gabarit.GabaritMoabi;
import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Rendu PDF interne (D-102) : contrôle du document puis impression par le moteur. Défense en profondeur contre la
 * falsification de requêtes côté serveur : le moteur n'admet que ses fichiers temporaires et n'a aucune sortie réseau ;
 * ici, tout élément actif ou de chargement ({@code script}, {@code iframe}, {@code object}, {@code embed}, {@code link},
 * {@code base}, {@code meta http-equiv}, gestionnaire {@code on…=}) et toute adresse autre que {@code data:} ou une
 * ancre ({@code src}, {@code href}, {@code url()}, {@code @import}) sont refusés (400 DOC_RENDU_INVALIDE).
 *
 * <p>Avec un gabarit (D-139), le corps contrôlé est enveloppé par {@link GabaritMoabi} : en-tête, pied et métadonnées
 * communs à tous les PDF de la plateforme.
 */
@Service
public class RenduPdfService implements RenduPdfUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RenduPdfService.class);

    /** Taille maximale du document et du pied de page réunis (polices et images embarquées comprises). */
    public static final int TAILLE_MAX = 4 * 1024 * 1024;
    static final double MARGE = 12;
    static final double MARGE_BAS = 16;
    /** Bornes du gabarit commun : longueur de chaque texte, nombre de données clés. */
    static final int TEXTE_MAX = 300;
    static final int FAITS_MAX = 8;

    private static final Pattern ACTIF = Pattern.compile(
        "<\\s*(script|iframe|frame|object|embed|link|base|portal|applet)\\b|<\\s*meta[^>]+http-equiv|\\son[a-z]+\\s*=|javascript:",
        Pattern.CASE_INSENSITIVE
    );
    /** Attribut ou fonction CSS portant une adresse : seules data: et les ancres (#…) sont admises. */
    private static final Pattern ADRESSE = Pattern.compile(
        "(?:\\b(?:src|href|xlink:href|srcset|poster|action|formaction|background)\\s*=\\s*[\"']?|url\\(\\s*[\"']?|@import\\s+[\"'])\\s*(?!data:|#)([^\"'\\s>)]*)",
        Pattern.CASE_INSENSITIVE
    );

    private final ConvertisseurHtml convertisseur;

    public RenduPdfService(ConvertisseurHtml convertisseur) {
        this.convertisseur = convertisseur;
    }

    @Override
    public byte[] rendre(Demande d, String client) {
        if (d == null || d.html() == null || d.html().isBlank() || d.format() == null) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Document HTML et format obligatoires");
        }
        Gabarit g = d.gabarit();
        long taille = octets(d.html()) + octets(d.piedDePage()) + (g == null ? 0 : octets(g.style()));
        if (taille > TAILLE_MAX) {
            throw DocumentException.tropVolumineux("DOC_RENDU_TROP_VOLUMINEUX", taille, TAILLE_MAX);
        }
        verifier(d.html());
        if (d.piedDePage() != null) {
            verifier(d.piedDePage());
        }
        double l = d.format() == Format.A3 ? 297 : 210;
        double h = d.format() == Format.A3 ? 420 : 297;
        ConvertisseurHtml.Papier papier = d.paysage() ? new ConvertisseurHtml.Papier(h, l, MARGE, MARGE_BAS, MARGE) : new ConvertisseurHtml.Papier(l, h, MARGE, MARGE_BAS, MARGE);
        String html = d.html();
        String pied = d.piedDePage();
        Map<String, String> metadonnees = Map.of();
        if (g != null) {
            controler(g, d.piedDePage());
            GabaritMoabi.Compose c = GabaritMoabi.composer(d.html(), g, papier.largeurMm() - 2 * MARGE < 250);
            html = c.html();
            pied = c.piedDePage();
            metadonnees = c.metadonnees();
        }
        try {
            byte[] pdf = convertisseur.imprimer(html, pied, papier, metadonnees);
            LOG.info("Rendu PDF pour {} : {} {}{}, {} octets", client, d.format(), d.paysage() ? "paysage" : "portrait", g == null ? "" : ", gabarit commun", pdf.length);
            return pdf;
        } catch (ConversionRefusee e) {
            throw DocumentException.refuse("DOC_RENDU_REFUSE", "Le moteur de mise en page a refusé le document", Map.of());
        } catch (MoteurIndisponible e) {
            throw DocumentException.indisponible("DOC_RENDU_INDISPONIBLE", "Le moteur de mise en page PDF est momentanément indisponible");
        }
    }

    /** Gabarit commun (D-139) : titre et date d'édition présents, textes bornés, pied composé par le gabarit seul. */
    static void controler(Gabarit g, String piedDePage) {
        if (piedDePage != null && !piedDePage.isBlank()) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Le pied de page est composé par le gabarit commun");
        }
        if (g.titre() == null || g.titre().isBlank() || g.edition() == null) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Titre et date d'édition du gabarit obligatoires");
        }
        List<String> textes = new java.util.ArrayList<>(java.util.Arrays.asList(g.titre(), g.surtitre(), g.sousTitre(), g.reference(), g.filigrane()));
        List<Fait> faits = g.faits() == null ? List.of() : g.faits();
        if (faits.size() > FAITS_MAX) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Au plus " + FAITS_MAX + " données clés");
        }
        faits.stream().filter(java.util.Objects::nonNull).forEach(f -> {
            textes.add(f.libelle());
            textes.add(f.valeur());
        });
        if (textes.stream().anyMatch(t -> t != null && t.length() > TEXTE_MAX)) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Texte du gabarit trop long (" + TEXTE_MAX + " caractères au plus)");
        }
        if (g.style() != null) {
            verifier(g.style());
            if (g.style().contains("</")) {
                throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Le style du gabarit ne contient que des règles CSS");
            }
        }
    }

    private static long octets(String s) {
        return s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Refuse tout contenu actif ou toute ressource externe. */
    static void verifier(String html) {
        if (ACTIF.matcher(html).find()) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Le document contient un élément actif ou de chargement");
        }
        var m = ADRESSE.matcher(html);
        while (m.find()) {
            String adresse = m.group(1).toLowerCase(Locale.ROOT);
            if (!adresse.isEmpty()) {
                throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Le document référence une ressource externe");
            }
        }
    }
}
