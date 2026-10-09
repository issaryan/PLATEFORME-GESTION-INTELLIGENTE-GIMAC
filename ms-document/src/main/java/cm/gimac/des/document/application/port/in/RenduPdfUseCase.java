package cm.gimac.des.document.application.port.in;

/**
 * Port entrant du rendu PDF interne (D-102, docs/lot1/organigramme.md §4) : un service de la plateforme (MS-IAM pour
 * l'organigramme) transmet un document HTML autonome qu'il a composé à partir de ses données ; MS-DOCUMENT le contrôle
 * (taille, aucune ressource externe, aucun script) et le fait imprimer par le moteur interne. Rien n'est conservé.
 */
public interface RenduPdfUseCase {
    /** Format du papier. */
    enum Format {
        A4,
        A3,
    }

    /** Document à imprimer : HTML, pied de page facultatif, papier et orientation. */
    record Demande(String html, String piedDePage, Format format, boolean paysage) {}

    /**
     * PDF du document. Erreurs (DocumentException) : 400 DOC_RENDU_INVALIDE (vide, ressource externe ou contenu actif),
     * 413 DOC_RENDU_TROP_VOLUMINEUX, 422 DOC_RENDU_REFUSE, 503 DOC_RENDU_INDISPONIBLE.
     */
    byte[] rendre(Demande demande, String client);
}
