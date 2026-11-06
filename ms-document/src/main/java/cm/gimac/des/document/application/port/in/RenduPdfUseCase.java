package cm.gimac.des.document.application.port.in;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Port entrant du rendu PDF interne (D-102, docs/lot1/organigramme.md §4) : un service de la plateforme (MS-IAM pour
 * l'organigramme) transmet un document HTML autonome qu'il a composé à partir de ses données ; MS-DOCUMENT le contrôle
 * (taille, aucune ressource externe, aucun script) et le fait imprimer par le moteur interne. Rien n'est conservé.
 *
 * <p>Gabarit commun (D-139) : avec un {@link Gabarit}, le service ne transmet que le corps du document (fragment HTML) ;
 * MS-DOCUMENT l'enveloppe dans la mise en page de l'organigramme (polices de la charte, logos de Moabi et du GIMAC,
 * filet bleu et vert, styles des tableaux, pied « page n sur N » avec référence, date et classification, fr, en, es).
 */
public interface RenduPdfUseCase {
    /** Format du papier. */
    enum Format {
        A4,
        A3,
    }

    /** Classification portée par le pied de page. */
    enum Classification {
        PUBLIC,
        INTERNE,
        CONFIDENTIEL,
    }

    /** Donnée clé du cartouche, sous l'en-tête (référence, état, demandeur…). */
    record Fait(String libelle, String valeur) {}

    /**
     * En-tête et pied du gabarit commun.
     *
     * @param titre titre du document (en-tête, pied et métadonnées), obligatoire
     * @param surtitre ligne au-dessus du titre, nulle pour le nom du GIMAC
     * @param sousTitre ligne sous le titre, facultative
     * @param reference référence du document (pied et métadonnées), facultative
     * @param classification nulle pour INTERNE
     * @param langue fr, en ou es (fr si nulle)
     * @param edition date d'édition, affichée à l'heure de Douala
     * @param filigrane texte en filigrane de chaque page (projet, démonstration), facultatif
     * @param faits cartouche des données clés, facultatif
     * @param style règles CSS propres au document, ajoutées après celles du gabarit, facultatives
     */
    record Gabarit(
        String titre,
        String surtitre,
        String sousTitre,
        String reference,
        Classification classification,
        String langue,
        OffsetDateTime edition,
        String filigrane,
        List<Fait> faits,
        String style
    ) {}

    /** Document à imprimer : HTML (corps seul avec un gabarit), pied de page facultatif, papier et orientation. */
    record Demande(String html, String piedDePage, Format format, boolean paysage, Gabarit gabarit) {
        public Demande(String html, String piedDePage, Format format, boolean paysage) {
            this(html, piedDePage, format, paysage, null);
        }
    }

    /**
     * PDF du document. Erreurs (DocumentException) : 400 DOC_RENDU_INVALIDE (vide, ressource externe ou contenu actif,
     * gabarit incomplet), 413 DOC_RENDU_TROP_VOLUMINEUX, 422 DOC_RENDU_REFUSE, 503 DOC_RENDU_INDISPONIBLE.
     */
    byte[] rendre(Demande demande, String client);
}
