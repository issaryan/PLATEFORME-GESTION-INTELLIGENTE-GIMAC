package cm.gimac.des.collaboration.application.usecase;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.MessageEtiquette;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.MessageRepository;
import cm.gimac.des.collaboration.repository.PieceJointeRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectures de la messagerie (D-64) : annuaire, conversations de l'appelant, fil des messages
 * (déchiffrés à la lecture), non-lus et pièces jointes. Hors des conversations dont l'appelant est
 * membre, tout répond 404 : l'existence d'une conversation n'est pas révélée.
 */
@Service
@Transactional(readOnly = true)
public class MessagerieQueries implements ConsulterMessagerieUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(MessagerieQueries.class);
    static final int TAILLE_MAX = 100;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final PieceJointeRepository pieces;
    private final CollaboratorDirectoryPort annuaire;
    private final DocumentStoragePort documents;
    private final ChiffrementPort chiffrement;
    private final ReglesMessagerie regles;

    public MessagerieQueries(
        ConversationRepository conversations,
        MessageRepository messages,
        PieceJointeRepository pieces,
        CollaboratorDirectoryPort annuaire,
        DocumentStoragePort documents,
        ChiffrementPort chiffrement,
        ReglesMessagerie regles
    ) {
        this.conversations = conversations;
        this.messages = messages;
        this.pieces = pieces;
        this.annuaire = annuaire;
        this.documents = documents;
        this.chiffrement = chiffrement;
        this.regles = regles;
    }

    @Override
    public Page<Collaborateur> annuaire(String appelantId, String recherche, int page, int taille) {
        int p = Math.max(page, 0);
        int t = Math.clamp(taille, 1, TAILLE_MAX);
        var r = annuaire.rechercher(recherche == null ? "" : recherche.strip(), p, t);
        List<Collaborateur> joignables = r.contenu().stream().filter(c -> !c.id().equals(appelantId) && c.joignable()).toList();
        boolean appelantRetire = r.contenu().stream().anyMatch(c -> c.id().equals(appelantId));
        return new Page<>(joignables, Math.max(0, r.total() - (appelantRetire ? 1 : 0)), p, t);
    }

    @Override
    public Page<ConversationLue> conversations(String appelantId, String recherche, UUID liaisonId, int page, int taille) {
        int p = Math.max(page, 0);
        int t = Math.clamp(taille, 1, TAILLE_MAX);
        var resultat = conversations.findAll(
            ConversationRepository.visibles(appelantId, recherche, liaisonId),
            PageRequest.of(p, t, Sort.by(Sort.Order.desc("misAJourLe"), Sort.Order.desc("id")))
        );
        Map<UUID, Long> nonLus = nonLusPar(appelantId, resultat.getContent().stream().map(Conversation::getId).toList());
        List<ConversationLue> contenu = resultat.getContent().stream().map(c -> lue(c, nonLus.getOrDefault(c.getId(), 0L))).toList();
        return new Page<>(contenu, resultat.getTotalElements(), p, t);
    }

    @Override
    public ConversationLue conversation(String appelantId, UUID conversationId) {
        Conversation c = membreDe(appelantId, conversationId);
        return lue(c, nonLusPar(appelantId, List.of(conversationId)).getOrDefault(conversationId, 0L));
    }

    @Override
    public PageMessages messages(String appelantId, UUID conversationId, UUID avant, int taille) {
        membreDe(appelantId, conversationId);
        int t = Math.clamp(taille, 1, TAILLE_MAX);
        PageRequest limite = PageRequest.of(0, t + 1);
        List<Message> lot;
        if (avant == null) {
            lot = messages.plusRecents(conversationId, limite);
        } else {
            Message repere = messages
                .findById(avant)
                .filter(m -> m.getConversation().getId().equals(conversationId))
                .orElseThrow(() -> CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", avant));
            lot = messages.anterieurs(conversationId, repere.getEnvoyeLe(), repere.getId(), limite);
        }
        List<Message> page = new ArrayList<>(lot.subList(0, Math.min(t, lot.size())));
        Collections.reverse(page);
        return new PageMessages(page.stream().map(this::lu).toList(), lot.size() > t);
    }

    @Override
    public MessageLu message(String appelantId, UUID messageId) {
        Message m = messages.findById(messageId).orElseThrow(() -> CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", messageId));
        if (conversations.pourMembre(m.getConversation().getId(), appelantId).isEmpty()) {
            throw CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", messageId);
        }
        return lu(m);
    }

    @Override
    public long nonLus(String appelantId) {
        return messages.compterNonLus(appelantId);
    }

    @Override
    public PieceATelecharger piece(String appelantId, UUID pieceId) {
        PieceJointe p = pieces
            .avecConversation(pieceId)
            .filter(pj -> conversations.pourMembre(pj.getMessage().getConversation().getId(), appelantId).isPresent())
            .orElseThrow(() -> CollaborationException.introuvable("COL_ATTACHMENT_NOT_FOUND", pieceId));
        var contenu = documents.lire(p.getDocumentRef());
        LOG.info(
            "D-64 : lecture de la pièce jointe {} (document {}, conversation {}) par {}",
            p.getId(),
            p.getDocumentRef(),
            p.getMessage().getConversation().getId(),
            appelantId
        );
        return new PieceATelecharger(p, contenu);
    }

    private Conversation membreDe(String appelantId, UUID conversationId) {
        return conversations
            .pourMembre(conversationId, appelantId)
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
    }

    private ConversationLue lue(Conversation c, long nonLus) {
        DernierMessage dernier = c.getDernierMessageId() == null
            ? null
            : messages
                .findById(c.getDernierMessageId())
                .map(m ->
                    new DernierMessage(
                        m.getId(),
                        m.getAuteurNom(),
                        regles.extrait(chiffrement.dechiffrer(m.getContenuChiffre(), m.getId())),
                        m.getEnvoyeLe(),
                        m.getPiecesJointes().size()
                    )
                )
                .orElse(null);
        return new ConversationLue(c, c.getMembres(), dernier, nonLus);
    }

    private MessageLu lu(Message m) {
        return new MessageLu(
            m,
            chiffrement.dechiffrer(m.getContenuChiffre(), m.getId()),
            m.getEtiquettes().stream().map(MessageEtiquette::vue).toList(),
            m.getPiecesJointes()
        );
    }

    private Map<UUID, Long> nonLusPar(String appelantId, List<UUID> ids) {
        Map<UUID, Long> r = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] ligne : messages.compterNonLusPar(appelantId, ids)) {
                r.put((UUID) ligne[0], ((Number) ligne[1]).longValue());
            }
        }
        return r;
    }
}
