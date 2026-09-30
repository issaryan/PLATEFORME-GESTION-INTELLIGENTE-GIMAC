package cm.gimac.des.collaboration.application.port.in;

import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.ContenuDocument;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import java.util.List;
import java.util.UUID;

/** Lectures de la messagerie (D-64), toujours limitées aux conversations dont l'appelant est membre. */
public interface ConsulterMessagerieUseCase {
    record Page<T>(List<T> contenu, long total, int page, int taille) {
        public Page {
            contenu = List.copyOf(contenu);
        }
    }

    record DernierMessage(UUID id, String auteurNom, String extrait, java.time.Instant envoyeLe, int piecesJointes) {}

    record ConversationLue(Conversation conversation, List<ConversationMembre> membres, DernierMessage dernierMessage, long nonLus) {}

    record MessageLu(Message message, String contenu, List<Etiquette> etiquettes, List<PieceJointe> piecesJointes) {}

    record PageMessages(List<MessageLu> contenu, boolean plusAnciens) {}

    record PieceATelecharger(PieceJointe piece, ContenuDocument contenu) {}

    Page<Collaborateur> annuaire(String appelantId, String recherche, int page, int taille);

    Page<ConversationLue> conversations(String appelantId, String recherche, UUID liaisonId, int page, int taille);

    ConversationLue conversation(String appelantId, UUID conversationId);

    PageMessages messages(String appelantId, UUID conversationId, UUID avant, int taille);

    MessageLu message(String appelantId, UUID messageId);

    long nonLus(String appelantId);

    PieceATelecharger piece(String appelantId, UUID pieceId);
}
