package cm.gimac.des.collaboration.application.port.in;

import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.ContenuDocument;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.Lecture;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lectures de la messagerie (D-64), toujours limitées aux conversations dont l'appelant est membre. */
public interface ConsulterMessagerieUseCase {
    record Page<T>(List<T> contenu, long total, int page, int taille) {
        public Page {
            contenu = List.copyOf(contenu);
        }
    }

    /**
     * « evenement » et « cibles » : événement de groupe (D-68), extrait alors vide. « lecture » (D-88) : état de
     * lecture quand l'appelant en est l'auteur, null sinon et pour un événement de groupe.
     */
    record DernierMessage(
        UUID id,
        String auteurNom,
        String extrait,
        java.time.Instant envoyeLe,
        int piecesJointes,
        cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe evenement,
        List<String> cibles,
        Lecture lecture
    ) {
        public DernierMessage(
            UUID id,
            String auteurNom,
            String extrait,
            java.time.Instant envoyeLe,
            int piecesJointes,
            cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe evenement,
            List<String> cibles
        ) {
            this(id, auteurNom, extrait, envoyeLe, piecesJointes, evenement, cibles, null);
        }
    }

    record ConversationLue(Conversation conversation, List<ConversationMembre> membres, DernierMessage dernierMessage, long nonLus) {}

    /**
     * Message cité par une réponse (D-82) : extrait de 120 caractères, nombre de pièces jointes.
     * « supprime » : message cité purgé ou illisible ; seul l'identifiant reste, extrait vide.
     */
    record CitationLue(UUID id, String auteurId, String auteurNom, String extrait, int piecesJointes, java.time.Instant envoyeLe, boolean supprime) {
        public static CitationLue supprimee(UUID id) {
            return new CitationLue(id, null, null, "", 0, null, true);
        }
    }

    /** Membre mentionné (D-82), nom à l'envoi. */
    record MentionLue(String id, String nom) {}

    /**
     * « reponseA » : nul hors réponse ; « mentions » : membres mentionnés, dans l'ordre (D-82). « lecture »
     * (D-88) : état de lecture quand l'appelant est l'auteur d'un vrai message, null sinon.
     */
    record MessageLu(
        Message message,
        String contenu,
        List<Etiquette> etiquettes,
        List<PieceJointe> piecesJointes,
        CitationLue reponseA,
        List<MentionLue> mentions,
        Lecture lecture
    ) {
        public MessageLu {
            mentions = mentions == null ? List.of() : List.copyOf(mentions);
        }

        public MessageLu(Message message, String contenu, List<Etiquette> etiquettes, List<PieceJointe> piecesJointes, CitationLue reponseA, List<MentionLue> mentions) {
            this(message, contenu, etiquettes, piecesJointes, reponseA, mentions, null);
        }

        public MessageLu(Message message, String contenu, List<Etiquette> etiquettes, List<PieceJointe> piecesJointes) {
            this(message, contenu, etiquettes, piecesJointes, null, List.of());
        }

        /**
         * Le lecteur est concerné (D-82) : mentionné, ou auteur du message cité ; jamais pour son propre message.
         */
        public boolean concerne(String lecteurId) {
            if (lecteurId == null || lecteurId.equals(message.getAuteurId())) {
                return false;
            }
            return mentions.stream().anyMatch(m -> m.id().equals(lecteurId)) || (reponseA != null && lecteurId.equals(reponseA.auteurId()));
        }
    }

    record PageMessages(List<MessageLu> contenu, boolean plusAnciens) {}

    /**
     * Lecture d'un message par un destinataire (D-88, « Lu par ») : nom figé à l'entrée ; « luLe » : heure de la
     * première avancée de sa position qui a couvert le message, null s'il ne l'a pas lu ou si cette heure
     * n'est pas connue (lecture antérieure à D-88).
     */
    record LectureMembre(String id, String nom, boolean lu, java.time.Instant luLe) {}

    record PieceATelecharger(PieceJointe piece, ContenuDocument contenu) {}

    Page<Collaborateur> annuaire(String appelantId, String recherche, int page, int taille);

    Page<ConversationLue> conversations(String appelantId, String recherche, UUID liaisonId, int page, int taille);

    ConversationLue conversation(String appelantId, UUID conversationId);

    PageMessages messages(String appelantId, UUID conversationId, UUID avant, int taille);

    MessageLu message(String appelantId, UUID messageId);

    /**
     * Détail « Lu par » d'un message (D-88) : destinataires présents à l'envoi et encore membres, lecteurs
     * d'abord (par heure de lecture), puis les autres. Réservé à l'auteur : sinon, comme pour un message
     * inconnu, d'une autre conversation ou un événement de groupe, 404 COL_MESSAGE_NOT_FOUND.
     */
    List<LectureMembre> lectures(String appelantId, UUID conversationId, UUID messageId);

    long nonLus(String appelantId);

    PieceATelecharger piece(String appelantId, UUID pieceId);

    /**
     * Versions des photos de profil des personnes affichées (membres, auteurs ; mon-compte.md §1).
     * Annuaire injoignable : vide (les initiales s'affichent), jamais d'échec de la lecture.
     */
    Map<String, String> versionsAvatar(Collection<String> ids);
}
