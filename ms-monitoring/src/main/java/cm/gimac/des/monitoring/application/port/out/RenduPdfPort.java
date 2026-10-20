package cm.gimac.des.monitoring.application.port.out;

/**
 * Mise en page PDF d'un document HTML autonome composé par MS-MONITORING (D-117, programme téléchargeable) : route interne
 * {@code POST /api/v1/interne/rendus/pdf} de MS-DOCUMENT (jeton de service, INTERNAL_DOCUMENT_RENDER), imprimé par le
 * moteur Gotenberg, comme l'organigramme de MS-IAM (D-102). Le HTML est construit à partir des seules données du
 * programme, jamais fourni par un client.
 *
 * <p>Moteur absent, injoignable ou délai dépassé : MonitoringException 503 MON_PDF_INDISPONIBLE.
 */
public interface RenduPdfPort {
    /**
     * @param html document HTML autonome (aucune ressource externe)
     * @param piedDePage pied de page de chaque page (classes {@code pageNumber} et {@code totalPages} remplies par le moteur)
     * @param paysage A4 en paysage, sinon en portrait
     */
    byte[] rendre(String html, String piedDePage, boolean paysage);
}
