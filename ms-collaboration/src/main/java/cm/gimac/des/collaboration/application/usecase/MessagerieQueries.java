package cm.gimac.des.collaboration.application.usecase;

import cm.gimac.des.collaboration.application.port.in.ConsulterConversationInterneUseCase;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.CitationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.LectureMembre;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.MentionLue;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Lecture;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.MessageEtiquette;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.LectureConversationRepository;
import cm.gimac.des.collaboration.repository.LectureConversationRepository.PremiereLecture;
import cm.gimac.des.collaboration.repository.MessageCite;
import cm.gimac.des.collaboration.repository.MessageRepository;
import cm.gimac.des.collaboration.repository.PieceJointeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectures de la messagerie (D-64) : annuaire, conversations de l'appelant, fil des messages
 * (déchiffrés à la lecture), non-lus et pièces jointes. Hors des conversations dont l'appelant est
 * membre, tout répond 404 : l'existence d'une conversation n'est pas révélée. Accusés de lecture (D-88) :
 * état de lecture des messages de l'appelant calculé en mémoire à partir des positions de lecture des
 * membres, déjà chargées avec la conversation (aucune requête par message).
 */
@Service
@Transactional(readOnly = true)
public class MessagerieQueries implements ConsulterMessagerieUseCase, ConsulterConversationInterneUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(MessagerieQueries.class);
    static final int TAILLE_MAX = 100;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final LectureConversationRepository lectures;
    private final PieceJointeRepository pieces;
    private final CollaboratorDirectoryPort annuaire;
    private final DocumentStoragePort documents;
    private final ChiffrementPort chiffrement;
    private final ReglesMessagerie regles;

    public MessagerieQueries(
        ConversationRepository conversations,
        MessageRepository messages,
        LectureConversationRepository lectures,
        PieceJointeRepository pieces,
        CollaboratorDirectoryPort annuaire,
        DocumentStoragePort documents,
        ChiffrementPort chiffrement,
        ReglesMessagerie regles
    ) {
        this.conversations = conversations;
        this.messages = messages;
        this.lectures = lectures;
        this.pieces = pieces;
        this.annuaire = annuaire;
        this.documents = documents;
        this.chiffrement = chiffrement;
        this.regles = regles;
    }

    @Override
    public ConversationInterne conversationInterne(UUID conversationId) {
        Conversation c = conversations
            .findById(conversationId)
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        return new ConversationInterne(
            c.getId(),
            c.getType().name(),
            c.estGroupe() ? c.getTitre() : null,
            c.getMembres().stream().map(cm.gimac.des.collaboration.domain.messagerie.ConversationMembre::getMembreId).toList()
        );
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
    public Map<String, String> versionsAvatar(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            return annuaire.versionsAvatar(ids);
        } catch (CollaborationException e) {
            LOG.warn("Versions des photos de profil indisponibles ({}) : initiales affichées", e.getMessage());
            return Map.of();
        }
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
        List<ConversationLue> contenu = resultat
            .getContent()
            .stream()
            .map(c -> lue(appelantId, c, nonLus.getOrDefault(c.getId(), 0L)))
            .toList();
        return new Page<>(contenu, resultat.getTotalElements(), p, t);
    }

    @Override
    public ConversationLue conversation(String appelantId, UUID conversationId) {
        Conversation c = membreDe(appelantId, conversationId);
        return lue(appelantId, c, nonLusPar(appelantId, List.of(conversationId)).getOrDefault(conversationId, 0L));
    }

    @Override
    public PageMessages messages(String appelantId, UUID conversationId, UUID avant, int taille) {
        Conversation conversation = membreDe(appelantId, conversationId);
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
        Map<UUID, CitationLue> cites = citations(conversationId, page);
        List<ConversationMembre> membres = conversation.getMembres();
        return new PageMessages(page.stream().map(m -> lu(m, cites, lecture(appelantId, m, membres))).toList(), lot.size() > t);
    }

    @Override
    public MessageLu message(String appelantId, UUID messageId) {
        Message m = messages.findById(messageId).orElseThrow(() -> CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", messageId));
        if (conversations.pourMembre(m.getConversation().getId(), appelantId).isEmpty()) {
            throw CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", messageId);
        }
        return lu(m, citations(m.getConversation().getId(), List.of(m)), lecture(appelantId, m, m.getConversation().getMembres()));
    }

    @Override
    public List<LectureMembre> lectures(String appelantId, UUID conversationId, UUID messageId) {
        // 404 indistinct : non-membre, message inconnu, d'une autre conversation, d'un autre auteur ou événement.
        Conversation c = conversations
            .pourMembre(conversationId, appelantId)
            .orElseThrow(() -> CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", messageId));
        Message m = messages
            .findById(messageId)
            .filter(x -> x.getConversation().getId().equals(conversationId))
            .filter(x -> x.getAuteurId().equals(appelantId) && x.getEvenement() == null)
            .orElseThrow(() -> CollaborationException.introuvable("COL_MESSAGE_NOT_FOUND", messageId));
        Instant envoyeLe = m.getEnvoyeLe();
        Map<String, Instant> heures = new HashMap<>();
        for (PremiereLecture p : lectures.premieresLectures(conversationId, envoyeLe)) {
            heures.put(p.membreId(), p.luLe());
        }
        Comparator<LectureMembre> ordre = Comparator.comparing((LectureMembre l) -> !l.lu()).thenComparing(
            LectureMembre::luLe,
            Comparator.nullsLast(Comparator.naturalOrder())
        );
        return c
            .getMembres()
            .stream()
            .filter(x -> Lecture.destinataire(x, appelantId, envoyeLe))
            .map(x -> {
                boolean lu = Lecture.aLu(x, envoyeLe);
                return new LectureMembre(x.getMembreId(), x.getNom(), lu, lu ? heures.get(x.getMembreId()) : null);
            })
            .sorted(ordre)
            .toList();
    }

    /** État de lecture (D-88) d'un message de l'appelant ; null pour le message d'un autre ou un événement. */
    private static Lecture lecture(String appelantId, Message m, List<ConversationMembre> membres) {
        return m.getAuteurId().equals(appelantId) ? Lecture.de(m, membres) : null;
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

    private ConversationLue lue(String appelantId, Conversation c, long nonLus) {
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
                        m.getPiecesJointes().size(),
                        m.getEvenement(),
                        m.getEvenementCibles(),
                        lecture(appelantId, m, c.getMembres())
                    )
                )
                .orElse(null);
        return new ConversationLue(c, c.getMembres(), dernier, nonLus);
    }

    private MessageLu lu(Message m, Map<UUID, CitationLue> cites, Lecture lecture) {
        UUID reponseA = m.getReponseAId();
        return new MessageLu(
            m,
            chiffrement.dechiffrer(m.getContenuChiffre(), m.getId()),
            m.getEtiquettes().stream().map(MessageEtiquette::vue).toList(),
            m.getPiecesJointes(),
            reponseA == null ? null : cites.getOrDefault(reponseA, CitationLue.supprimee(reponseA)),
            m.getMentions().stream().map(x -> new MentionLue(x.getMembreId(), x.getNom())).toList(),
            lecture
        );
    }

    /**
     * Messages cités par une page du fil (D-82), en une seule requête quelle que soit la taille de la page.
     * Absent (purgé, RG-53), d'une autre conversation ou indéchiffrable : absent du résultat, rendu
     * « supprime » par l'appelant.
     */
    private Map<UUID, CitationLue> citations(UUID conversationId, List<Message> page) {
        Set<UUID> ids = page.stream().map(Message::getReponseAId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, CitationLue> r = new HashMap<>();
        for (MessageCite c : messages.cites(ids)) {
            if (!conversationId.equals(c.conversationId()) || c.evenement() != null) {
                continue;
            }
            try {
                String clair = chiffrement.dechiffrer(c.contenuChiffre(), c.id());
                r.put(
                    c.id(),
                    new CitationLue(c.id(), c.auteurId(), c.auteurNom(), regles.extraitCitation(clair), (int) c.piecesJointes(), c.envoyeLe(), false)
                );
            } catch (RuntimeException e) {
                LOG.warn("D-82 : message cité {} illisible ({}), citation rendue comme supprimée", c.id(), e.getClass().getSimpleName());
            }
        }
        return r;
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
