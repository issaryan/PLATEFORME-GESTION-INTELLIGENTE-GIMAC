package cm.gimac.des.document.application.port.in;

import java.util.UUID;

/**
 * Port entrant du dépôt direct d'un fichier (tranche DDRM, D-60) : conservation WORM du contenu,
 * empreinte SHA-256 calculée par le serveur, métadonnées ; lecture de l'empreinte pour les
 * autres services, qui ne gardent que la référence et l'empreinte, jamais le contenu.
 */
public interface DeposerDocumentUseCase {
    /** Fichier reçu : nom d'origine, type MIME déclaré par le client, contenu. */
    record Fichier(String nomFichier, String typeMime, byte[] contenu) {}

    /** Commande de dépôt : fichier (absent si la partie manque), finalité (ADHESION...) et référence libre. */
    record Commande(Fichier fichier, String finalite, String reference) {}

    /** Déposant lu dans le jeton : sujet et, pour un représentant d'un participant, son institution. */
    record Deposant(String id, String institutionId) {}

    /** Résultat restitué au client. */
    record Resultat(UUID id, String sha256, String nomFichier, long taille, String typeMime) {}

    Resultat deposer(Commande commande, Deposant deposant);

    /** Empreinte SHA-256 (hexadécimal minuscule) d'un document déposé ; DOC_NOT_FOUND s'il est inconnu. */
    String empreinte(UUID id);
}
