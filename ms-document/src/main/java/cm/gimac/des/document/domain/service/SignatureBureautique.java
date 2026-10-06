package cm.gimac.des.document.domain.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Formats bureautiques admis à l'aperçu converti en PDF (D-83, docs/lot1/apercu-bureautique.md §1, §2) et
 * contrôle de la signature du fichier, lue dans ses premiers octets, contre son extension :
 * <ul>
 *   <li>Open XML (docx, pptx, xlsx) et OpenDocument (odt, odp, ods) : conteneur ZIP ({@code PK\3\4}) ; un
 *   fichier Open XML protégé par mot de passe est un conteneur OLE2 chiffré, admis lui aussi pour que le
 *   moteur le signale comme illisible (422) plutôt que comme un format non pris en charge ;</li>
 *   <li>doc, ppt, xls : conteneur OLE2 ({@code D0 CF 11 E0 A1 B1 1A E1}) ;</li>
 *   <li>rtf : texte commençant par {@code {\rtf}.</li>
 * </ul>
 */
public final class SignatureBureautique {

    /** Familles de signature. */
    public enum Famille {
        ZIP,
        OLE2,
        RTF,
    }

    private static final byte[] ZIP = { 'P', 'K', 3, 4 };
    private static final byte[] OLE2 = { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1 };
    private static final byte[] RTF = { '{', '\\', 'r', 't', 'f' };

    private static final Set<String> OPEN_XML = Set.of("docx", "pptx", "xlsx");

    /** Extensions admises et famille de signature attendue. */
    public static final Map<String, Famille> FORMATS = Map.ofEntries(
        Map.entry("docx", Famille.ZIP),
        Map.entry("pptx", Famille.ZIP),
        Map.entry("xlsx", Famille.ZIP),
        Map.entry("odt", Famille.ZIP),
        Map.entry("odp", Famille.ZIP),
        Map.entry("ods", Famille.ZIP),
        Map.entry("doc", Famille.OLE2),
        Map.entry("ppt", Famille.OLE2),
        Map.entry("xls", Famille.OLE2),
        Map.entry("rtf", Famille.RTF)
    );

    private SignatureBureautique() {}

    /**
     * Extension admise du nom de fichier, en minuscules, ou vide si le nom n'en porte pas ou si elle est
     * hors de la liste.
     */
    public static Optional<String> extension(String nomFichier) {
        if (nomFichier == null) {
            return Optional.empty();
        }
        int point = nomFichier.lastIndexOf('.');
        if (point < 0 || point == nomFichier.length() - 1) {
            return Optional.empty();
        }
        String ext = nomFichier.substring(point + 1).toLowerCase(Locale.ROOT);
        return FORMATS.containsKey(ext) ? Optional.of(ext) : Optional.empty();
    }

    /** Vrai si les premiers octets du contenu portent la signature attendue pour l'extension admise. */
    public static boolean concorde(String extension, byte[] contenu) {
        Famille attendue = FORMATS.get(extension);
        if (attendue == null || contenu == null) {
            return false;
        }
        return switch (attendue) {
            case ZIP -> commencePar(contenu, ZIP) || (OPEN_XML.contains(extension) && commencePar(contenu, OLE2));
            case OLE2 -> commencePar(contenu, OLE2);
            case RTF -> commencePar(contenu, RTF);
        };
    }

    static boolean commencePar(byte[] contenu, byte[] signature) {
        if (contenu.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (contenu[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
