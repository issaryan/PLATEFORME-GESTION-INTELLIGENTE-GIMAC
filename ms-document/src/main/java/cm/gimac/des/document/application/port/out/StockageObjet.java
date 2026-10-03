package cm.gimac.des.document.application.port.out;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : dépôt objet (Ceph RGW en production, mémoire en développement), avec deux
 * compartiments (D-74, docs/lot1/depots-fragmentes.md §2) :
 * <ul>
 *   <li>le dépôt WORM des documents vérifiés (verrouillage d'objets) : écriture d'un objet entier,
 *   ou en flux par tranches ({@link EcritureWorm}), lecture entière ou en flux ;</li>
 *   <li>le compartiment des dépôts en cours, sans verrouillage : téléversement multipart des parties
 *   envoyées par le client, assemblage, lecture en flux de l'objet assemblé, puis suppression.</li>
 * </ul>
 * Toute panne du dépôt objet est signalée par DOC_STORAGE_UNAVAILABLE (503).
 */
public interface StockageObjet {
    /** Référence de l'objet déposé : sa clé et la version attribuée par le dépôt. */
    record ObjetDepose(String cle, String version) {}

    /** Contenu d'un objet lu en flux (à fermer par le lecteur) et sa taille en octets. */
    record Flux(InputStream contenu, long taille) {}

    /** Partie d'un téléversement multipart : numéro (à partir de 1) et ETag rendu par le dépôt. */
    record Partie(int numero, String etag) {}

    // ------------------------------------------------------------------ dépôt WORM

    ObjetDepose deposer(String cle, byte[] contenu, String typeMime, String sha256);

    Optional<byte[]> lire(String cle);

    /** Objet du dépôt WORM lu en flux, sans le charger en mémoire ; vide si la clé est inconnue. */
    Optional<Flux> lireFlux(String cle);

    /**
     * Ouvre l'écriture en flux d'un objet du dépôt WORM (téléversement multipart) : chaque tranche
     * est envoyée comme une partie, avec son Content-MD5 (exigé par le verrouillage d'objets).
     */
    EcritureWorm ouvrirEcriture(String cle, String typeMime);

    /** Abandonne une écriture WORM laissée en cours (reprise après un arrêt) ; sans effet si elle n'existe plus. */
    void abandonnerEcriture(String cle, String envoi);

    /**
     * Écriture en flux dans le dépôt WORM. Les tranches font toutes au moins 5 Mio, sauf la
     * dernière (minimum S3). {@link #terminer()} crée l'objet ; {@link #abandonner()} n'en crée
     * aucun (rien n'est alors retenu par le verrouillage).
     */
    interface EcritureWorm {
        /** Identifiant du téléversement, gardé pour pouvoir l'abandonner après un arrêt. */
        String envoi();

        /** Envoie les {@code longueur} premiers octets de {@code tranche} comme partie suivante. */
        void ecrire(byte[] tranche, int longueur);

        ObjetDepose terminer();

        void abandonner();
    }

    // ------------------------------------------------------------------ compartiment des dépôts en cours

    /** Ouvre un téléversement multipart sous {@code cle} ; rend son identifiant. */
    String ouvrirDepot(String cle, String typeMime);

    /** Envoie (ou remplace) la partie {@code numero} ; rend son ETag. */
    String deposerPartie(String cle, String envoi, int numero, byte[] octets);

    /** Assemble les parties, dans l'ordre croissant des numéros, en un seul objet. */
    void assemblerDepot(String cle, String envoi, List<Partie> parties);

    /** Abandonne un téléversement multipart ; sans effet s'il n'existe plus. */
    void abandonnerDepot(String cle, String envoi);

    /** Objet assemblé lu en flux ; vide s'il n'existe pas (ou plus). */
    Optional<Flux> lireDepot(String cle);

    /** Supprime l'objet assemblé ; sans effet s'il n'existe plus. */
    void supprimerDepot(String cle);
}
