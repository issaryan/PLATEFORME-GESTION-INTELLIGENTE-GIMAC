package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
import cm.gimac.des.document.application.port.out.ConvertisseurHtml;
import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
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
 */
@Service
public class RenduPdfService implements RenduPdfUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RenduPdfService.class);

    /** Taille maximale du document et du pied de page réunis (polices et images embarquées comprises). */
    public static final int TAILLE_MAX = 4 * 1024 * 1024;
    static final double MARGE = 12;
    static final double MARGE_BAS = 16;

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
        long taille = (long) d.html().getBytes(StandardCharsets.UTF_8).length + (d.piedDePage() == null ? 0 : d.piedDePage().getBytes(StandardCharsets.UTF_8).length);
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
        try {
            byte[] pdf = convertisseur.imprimer(d.html(), d.piedDePage(), papier);
            LOG.info("Rendu PDF pour {} : {} {}, {} octets", client, d.format(), d.paysage() ? "paysage" : "portrait", pdf.length);
            return pdf;
        } catch (ConversionRefusee e) {
            throw DocumentException.refuse("DOC_RENDU_REFUSE", "Le moteur de mise en page a refusé le document", Map.of());
        } catch (MoteurIndisponible e) {
            throw DocumentException.indisponible("DOC_RENDU_INDISPONIBLE", "Le moteur de mise en page PDF est momentanément indisponible");
        }
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
