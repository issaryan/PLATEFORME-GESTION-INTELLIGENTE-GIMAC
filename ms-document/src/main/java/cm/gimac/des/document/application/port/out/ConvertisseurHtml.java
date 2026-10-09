package cm.gimac.des.document.application.port.out;

/**
 * Port sortant : impression en PDF d'un document HTML autonome composé par un service de la plateforme (D-102,
 * organigramme de MS-IAM, docs/lot1/organigramme.md §4). Adaptateur réel : le moteur Gotenberg (Chromium sans
 * interface, JavaScript désactivé, aucune adresse joignable hors de ses fichiers temporaires) du conteneur interne
 * {@code conversion-documents}, dont MS-DOCUMENT reste le seul client.
 */
public interface ConvertisseurHtml {
    /**
     * Papier et marges en millimètres ; le pied de page s'imprime dans la marge basse.
     */
    record Papier(double largeurMm, double hauteurMm, double margeHautMm, double margeBasMm, double margeCoteMm) {}

    /**
     * PDF du document.
     *
     * @param piedDePage HTML du pied de page (classes {@code pageNumber} et {@code totalPages}), ou nul
     * @throws ConvertisseurApercu.ConversionRefusee le moteur refuse le document
     * @throws ConvertisseurApercu.MoteurIndisponible moteur injoignable, saturé ou délai dépassé
     */
    byte[] imprimer(String html, String piedDePage, Papier papier);
}
