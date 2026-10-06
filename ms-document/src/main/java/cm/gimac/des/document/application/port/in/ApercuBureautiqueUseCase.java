package cm.gimac.des.document.application.port.in;

/**
 * Port entrant de l'aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md) : le fichier
 * transmis par la fenêtre d'aperçu est converti en PDF par le moteur de conversion, puis rendu à
 * l'aperçu PDF existant. Aucune copie durable de la source n'est gardée ; le PDF est gardé en cache par
 * empreinte SHA-256 de la source.
 */
public interface ApercuBureautiqueUseCase {
    /**
     * Fichier à prévisualiser : nom (pour son extension seulement, jamais journalisé) et contenu. L'adaptateur
     * web lit au plus {@link #tailleMax()} + 1 octets du corps, pour que le dépassement soit constaté ici.
     */
    record Demande(String nomFichier, byte[] contenu) {}

    /**
     * PDF de l'aperçu du fichier, converti pour le compte {@code compte} (sujet du jeton). Erreurs
     * (DocumentException) : 413 DOC_APERCU_TROP_VOLUMINEUX, 415 DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE,
     * 422 DOC_APERCU_ILLISIBLE, 429 DOC_APERCU_LIMITE, 503 DOC_APERCU_INDISPONIBLE.
     */
    byte[] apercu(Demande demande, String compte);

    /** Taille maximale d'un fichier source, en octets. */
    long tailleMax();
}
