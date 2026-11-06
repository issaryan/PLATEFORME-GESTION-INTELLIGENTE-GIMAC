package cm.gimac.des.pilotage.domain.processus;

/**
 * Code d'un processus d'unité, généré par le système à la création, jamais saisi : unité, nature et numéro d'ordre,
 * {UNITE}-PRC-{N} (par exemple SSI-PRC-12), sur le modèle des références CHG-{AAAA}-{N}. Le numéro vient d'une
 * séquence PostgreSQL : aucune collision, même en concurrence. L'unité est tronquée pour tenir dans les 40 caractères
 * de la colonne ; le numéro, lui, n'est jamais tronqué.
 */
public final class CodeProcessus {

    public static final String NATURE = "PRC";
    static final int LONGUEUR_MAX = 40;

    private CodeProcessus() {}

    public static String suivant(String unite, long numero) {
        String suffixe = "-" + NATURE + "-" + numero;
        String u = unite.length() + suffixe.length() > LONGUEUR_MAX ? unite.substring(0, LONGUEUR_MAX - suffixe.length()) : unite;
        return u + suffixe;
    }
}
