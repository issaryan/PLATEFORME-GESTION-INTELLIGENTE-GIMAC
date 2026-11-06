package cm.gimac.des.pilotage.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Rendu PDF mutualisé de MS-DOCUMENT, dans le gabarit commun des PDF de la plateforme (D-139) : en-tête aux logos de
 * Moabi et du GIMAC, cartouche des données clés, pied paginé avec référence, date et classification.
 */
public interface RenduPdfPort {
    /** Donnée clé du cartouche. */
    record Fait(String libelle, String valeur) {}

    /**
     * Document au gabarit commun : corps HTML (classes du gabarit), titre, sous-titre, référence, classification
     * (PUBLIC, INTERNE, CONFIDENTIEL), langue (fr, en, es), date d'édition et cartouche ; A4 portrait.
     */
    record Document(String corps, String titre, String sousTitre, String reference, String classification, String langue, Instant edition, List<Fait> faits) {}

    byte[] rendre(Document document);
}
