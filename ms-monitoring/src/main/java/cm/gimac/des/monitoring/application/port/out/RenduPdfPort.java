package cm.gimac.des.monitoring.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Mise en page PDF d'un document composé par MS-MONITORING (D-117, programme téléchargeable) : route interne
 * {@code POST /api/v1/interne/rendus/pdf} de MS-DOCUMENT (jeton de service, INTERNAL_DOCUMENT_RENDER), imprimé par le
 * moteur Gotenberg dans le gabarit commun des PDF de la plateforme (D-139) : en-tête aux logos de Moabi et du GIMAC,
 * cartouche, pied paginé avec référence, date et classification, comme l'organigramme de MS-IAM (D-102). Le corps est
 * construit à partir des seules données du programme, jamais fourni par un client.
 *
 * <p>Moteur absent, injoignable ou délai dépassé : MonitoringException 503 MON_PDF_INDISPONIBLE.
 */
public interface RenduPdfPort {
    /** Donnée clé du cartouche. */
    record Fait(String libelle, String valeur) {}

    /**
     * Document au gabarit commun.
     *
     * @param corps fragment HTML du corps (classes du gabarit : h2, h3, table, .colonnes, .bloc, .encart, .badge…)
     * @param titre titre de l'en-tête, du pied et des métadonnées
     * @param sousTitre ligne sous le titre, facultative
     * @param reference référence portée par le pied
     * @param classification PUBLIC, INTERNE ou CONFIDENTIEL
     * @param langue fr, en ou es
     * @param edition date d'édition
     * @param filigrane texte en filigrane (projet non publié), facultatif
     * @param faits cartouche des données clés
     * @param paysage A4 en paysage, sinon en portrait
     */
    record Document(
        String corps,
        String titre,
        String sousTitre,
        String reference,
        String classification,
        String langue,
        Instant edition,
        String filigrane,
        List<Fait> faits,
        boolean paysage
    ) {}

    byte[] rendre(Document document);
}
