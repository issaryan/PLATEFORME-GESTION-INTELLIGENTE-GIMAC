package cm.gimac.des.iam.application.port.out;

/**
 * Port sortant : mise en page PDF d'un document HTML autonome composé par MS-IAM (D-102, organigramme en PDF,
 * docs/lot1/organigramme.md §4). Adaptateur réel : route interne {@code POST /api/v1/interne/rendus/pdf} de
 * MS-DOCUMENT (jeton de service), qui le fait imprimer par le moteur Gotenberg (Chromium) du conteneur interne
 * {@code conversion-documents}. Le HTML n'est jamais fourni par un client : il est construit à partir des données.
 *
 * <p>Moteur absent, injoignable, saturé ou délai dépassé : IamException 503 ORG_PDF_INDISPONIBLE.
 */
public interface RenduPdf {
    /** Format du papier. */
    enum Format {
        A4(210, 297),
        A3(297, 420);

        /** Largeur et hauteur en millimètres, en portrait. */
        public final int largeurMm;
        public final int hauteurMm;

        Format(int largeurMm, int hauteurMm) {
            this.largeurMm = largeurMm;
            this.hauteurMm = hauteurMm;
        }
    }

    /** Papier et orientation ; les marges sont celles du document (12 mm, 16 mm en bas pour le pied de page). */
    record Mise(Format format, boolean paysage) {
        public double largeurMm() {
            return paysage ? format.hauteurMm : format.largeurMm;
        }

        public double hauteurMm() {
            return paysage ? format.largeurMm : format.hauteurMm;
        }
    }

    /**
     * PDF du document.
     *
     * @param html document HTML autonome (aucune ressource externe : polices et images en data URI)
     * @param piedDePage pied de page de chaque page (classes {@code pageNumber} et {@code totalPages} remplies par le moteur)
     */
    byte[] rendre(String html, String piedDePage, Mise mise);
}
