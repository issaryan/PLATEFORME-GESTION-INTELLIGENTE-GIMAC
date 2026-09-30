package cm.gimac.des.document.domain.service;

import cm.gimac.des.document.domain.DocumentException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Règles d'admission d'un dépôt direct : fichier présent, non vide et sous la taille maximale ;
 * finalité en code majuscule ; référence libre bornée ; nom de fichier et type MIME assainis.
 * Service de domaine pur (ni Spring, ni infrastructure).
 */
public final class ValidationDepot {

    public static final String TYPE_PAR_DEFAUT = "application/octet-stream";
    public static final String TYPE_PDF = "application/pdf";
    static final int LONGUEUR_MAX_REFERENCE = 255;
    static final int LONGUEUR_MAX_NOM = 255;
    static final int LONGUEUR_MAX_DEPOSANT = 100;

    private static final Pattern FINALITE = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");
    private static final Pattern TYPE_MIME = Pattern.compile("^[a-z0-9][a-z0-9!#$&^_.+-]{0,62}/[a-z0-9][a-z0-9!#$&^_.+-]{0,62}$");
    private static final Pattern CONTROLE = Pattern.compile("\\p{Cntrl}");
    private static final byte[] SIGNATURE_PDF = { '%', 'P', 'D', 'F', '-' };

    private ValidationDepot() {}

    /** Fichier absent (DOC_FILE_MISSING), vide (DOC_FILE_EMPTY) ou trop volumineux (DOC_FILE_TOO_LARGE, 413). */
    public static void verifierContenu(byte[] contenu, long tailleMax) {
        if (contenu == null) {
            throw DocumentException.invalide("DOC_FILE_MISSING", "La partie « fichier » est obligatoire");
        }
        if (contenu.length == 0) {
            throw DocumentException.invalide("DOC_FILE_EMPTY", "Le fichier est vide");
        }
        if (contenu.length > tailleMax) {
            throw DocumentException.tropVolumineux(contenu.length, tailleMax);
        }
    }

    /** Finalité normalisée en majuscules (ADHESION, PREUVE_HOMOLOGATION...) ; DOC_PURPOSE_INVALID sinon. */
    public static String finalite(String finalite) {
        String f = finalite == null ? "" : finalite.trim().toUpperCase(Locale.ROOT);
        if (!FINALITE.matcher(f).matches()) {
            throw DocumentException.invalide("DOC_PURPOSE_INVALID", "Finalité absente ou mal formée (code en majuscules, 2 à 50 caractères)");
        }
        return f;
    }

    /** Référence libre (par exemple institutionId/code), obligatoire, sans caractère de contrôle, 255 caractères au plus. */
    public static String reference(String reference) {
        String r = reference == null ? "" : reference.trim();
        if (r.isEmpty() || r.length() > LONGUEUR_MAX_REFERENCE || CONTROLE.matcher(r).find()) {
            throw DocumentException.invalide("DOC_REFERENCE_INVALID", "Référence absente, trop longue ou mal formée");
        }
        return r;
    }

    /**
     * Identifiant de l'utilisateur pour le compte duquel un service dépose (interface interne) :
     * obligatoire, sans caractère de contrôle, 100 caractères au plus ; DOC_DEPOSITOR_INVALID sinon.
     */
    public static String deposantId(String deposantId) {
        String d = deposantId == null ? "" : deposantId.trim();
        if (d.isEmpty() || d.length() > LONGUEUR_MAX_DEPOSANT || CONTROLE.matcher(d).find()) {
            throw DocumentException.invalide("DOC_DEPOSITOR_INVALID", "Identifiant du déposant absent, trop long ou mal formé");
        }
        return d;
    }

    /**
     * Nom de fichier conservé : dernier segment du chemin fourni par le navigateur, sans caractère
     * de contrôle, borné à 255 caractères ; « document » s'il est vide.
     */
    public static String nomFichier(String nom) {
        String n = nom == null ? "" : nom;
        n = n.substring(Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\')) + 1);
        n = CONTROLE.matcher(n).replaceAll("").trim();
        if (n.isEmpty() || ".".equals(n) || "..".equals(n)) {
            return "document";
        }
        return n.length() > LONGUEUR_MAX_NOM ? n.substring(0, LONGUEUR_MAX_NOM) : n;
    }

    /**
     * Type MIME conservé : un contenu qui commence par la signature PDF est un PDF quel que soit le
     * type déclaré (le contrôle de masquage ne peut pas être contourné en le déguisant) ; sinon le
     * type déclaré, sans paramètres, en minuscules, ou application/octet-stream s'il est mal formé.
     */
    public static String typeMime(String declare, byte[] contenu) {
        if (commencePar(contenu, SIGNATURE_PDF)) {
            return TYPE_PDF;
        }
        if (declare == null) {
            return TYPE_PAR_DEFAUT;
        }
        int pv = declare.indexOf(';');
        String t = (pv >= 0 ? declare.substring(0, pv) : declare).trim().toLowerCase(Locale.ROOT);
        return TYPE_MIME.matcher(t).matches() ? t : TYPE_PAR_DEFAUT;
    }

    static boolean commencePar(byte[] contenu, byte[] prefixe) {
        if (contenu == null || contenu.length < prefixe.length) {
            return false;
        }
        for (int i = 0; i < prefixe.length; i++) {
            if (contenu[i] != prefixe[i]) {
                return false;
            }
        }
        return true;
    }
}
