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

    /**
     * Déposant : utilisateur (sujet du jeton ou, par l'interface interne, identifiant transmis par
     * le service), son institution pour un représentant d'un participant, et le client du service
     * qui dépose pour son compte (absent pour un dépôt direct par l'utilisateur).
     */
    record Deposant(String id, String institutionId, String service) {
        /** Dépôt direct par l'utilisateur, sans service intermédiaire. */
        public Deposant(String id, String institutionId) {
            this(id, institutionId, null);
        }

        /** Dépôt par un service interne pour le compte d'un utilisateur. */
        public static Deposant pourLeCompteDe(String utilisateurId, String service) {
            return new Deposant(utilisateurId, null, service);
        }
    }

    /** Résultat restitué au client. */
    record Resultat(UUID id, String sha256, String nomFichier, long taille, String typeMime) {}

    Resultat deposer(Commande commande, Deposant deposant);

    /** Empreinte SHA-256 (hexadécimal minuscule) d'un document déposé ; DOC_NOT_FOUND s'il est inconnu. */
    String empreinte(UUID id);
}
