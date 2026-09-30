package cm.gimac.des.collaboration.application.usecase;

import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.DocumentDepose;
import cm.gimac.des.collaboration.domain.event.MessageEnvoyePayload;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.domain.service.MasquagePan;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.collaboration.repository.ConversationMembreRepository;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.MessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envoi des messages de la messagerie de travail (D-64) : destinataires pris dans l'annuaire,
 * conversation directe réutilisée, masquage des numéros de carte (RG-52) puis chiffrement au repos,
 * pièces déposées dans MS-DOCUMENT, événement collaboration.message-sent.v1 par l'outbox.
 */
@Service
@Transactional
public class MessagerieService implements EnvoyerMessageUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(MessagerieService.class);

    private final ConversationRepository conversations;
    private final ConversationMembreRepository membres;
    private final MessageRepository messages;
    private final CollaboratorDirectoryPort annuaire;
    private final DocumentStoragePort documents;
    private final ChiffrementPort chiffrement;
    private final OutboxPublisher outbox;
    private final ReglesMessagerie regles;
    private final MasquagePan masquage;
    private final Clock clock;

    public MessagerieService(
        ConversationRepository conversations,
        ConversationMembreRepository membres,
        MessageRepository messages,
        CollaboratorDirectoryPort annuaire,
        DocumentStoragePort documents,
        ChiffrementPort chiffrement,
        OutboxPublisher outbox,
        ReglesMessagerie regles,
        MasquagePan masquage,
        Clock clock
    ) {
        this.conversations = conversations;
        this.membres = membres;
        this.messages = messages;
        this.annuaire = annuaire;
        this.documents = documents;
        this.chiffrement = chiffrement;
        this.outbox = outbox;
        this.regles = regles;
        this.masquage = masquage;
        this.clock = clock;
    }

    @Override
    public Envoi creerConversation(Expediteur expediteur, NouvelleConversation demande, List<FichierJoint> fichiers) {
        List<FichierJoint> pieces = regles.pieces(fichiers);
        String contenu = regles.contenu(demande.contenu(), pieces.size());
        List<Etiquette> etiquettes = regles.etiquettes(demande.etiquettes());
        List<String> ids = regles.destinataires(demande.destinataires(), expediteur.id());
        String titre = regles.titre(demande.titre());
        String reference = referenceLiaison(demande, etiquettes);
        List<Collaborateur> destinataires = ids.stream().map(this::destinataire).toList();
        Collaborateur auteur = annuaire.trouver(expediteur.id()).orElseGet(() -> depuisJeton(expediteur));
        Instant maintenant = maintenant();

        Conversation conversation;
        boolean creee;
        if (regles.estDirecte(ids, titre, demande.liaisonId())) {
            String cle = regles.cleDirecte(expediteur.id(), ids.get(0));
            Optional<Conversation> existante = conversations.findByCleDirecte(cle);
            creee = existante.isEmpty();
            conversation = existante.orElseGet(() -> new Conversation(TypeConversation.DIRECTE, null, null, null, cle, expediteur.id(), maintenant));
        } else {
            creee = true;
            conversation = new Conversation(TypeConversation.GROUPE, titre, demande.liaisonId(), reference, null, expediteur.id(), maintenant);
        }
        if (creee) {
            conversation.ajouterMembre(auteur, maintenant);
            destinataires.forEach(d -> conversation.ajouterMembre(d, maintenant));
            conversations.save(conversation);
        }
        UUID messageId = ajouterMessage(conversation, auteur, contenu, etiquettes, pieces, maintenant);
        return new Envoi(conversation.getId(), messageId, creee);
    }

    @Override
    public Envoi envoyer(Expediteur expediteur, UUID conversationId, NouveauMessage demande, List<FichierJoint> fichiers) {
        Conversation conversation = conversations
            .pourMembre(conversationId, expediteur.id())
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        List<FichierJoint> pieces = regles.pieces(fichiers);
        String contenu = regles.contenu(demande.contenu(), pieces.size());
        List<Etiquette> etiquettes = regles.etiquettes(demande.etiquettes());
        ConversationMembre moi = conversation.membre(expediteur.id()).orElseThrow();
        Collaborateur auteur = new Collaborateur(moi.getMembreId(), moi.getNom(), moi.getFonction(), Set.of(), null, true);
        UUID messageId = ajouterMessage(conversation, auteur, contenu, etiquettes, pieces, maintenant());
        return new Envoi(conversationId, messageId, false);
    }

    @Override
    public void marquerLu(String membreId, UUID conversationId) {
        ConversationMembre membre = membres
            .findByConversationIdAndMembreId(conversationId, membreId)
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        messages.findFirstByConversationIdOrderByEnvoyeLeDescIdDesc(conversationId).ifPresent(m -> membre.marquerLu(m.getId(), m.getEnvoyeLe()));
    }

    private UUID ajouterMessage(
        Conversation conversation,
        Collaborateur auteur,
        String contenu,
        List<Etiquette> etiquettes,
        List<FichierJoint> pieces,
        Instant envoyeLe
    ) {
        UUID id = UUID.randomUUID();
        MasquagePan.Resultat masque = masquage.masquer(contenu);
        if (masque.masques() > 0) {
            LOG.info("RG-52 : {} numéro(s) de carte masqué(s) avant l'enregistrement du message {}", masque.masques(), id);
        }
        Message message = new Message(id, conversation, auteur, chiffrement.chiffrer(masque.texte(), id), envoyeLe);
        etiquettes.forEach(message::etiqueter);
        for (FichierJoint piece : pieces) {
            DocumentDepose depot = documents.deposer(piece, id, auteur.id());
            message.joindre(depot.id(), piece.nom(), depot.taille(), piece.typeMime(), depot.sha256());
        }
        messages.save(message);
        conversation.messageAjoute(id, envoyeLe);
        conversation.membre(auteur.id()).ifPresent(m -> m.marquerLu(id, envoyeLe));
        List<String> destinataires = conversation
            .getMembres()
            .stream()
            .map(ConversationMembre::getMembreId)
            .filter(m -> !m.equals(auteur.id()))
            .toList();
        outbox.publish(
            "conversation",
            conversation.getId(),
            MessageEnvoyePayload.TYPE,
            new MessageEnvoyePayload(conversation.getId(), id, auteur.id(), destinataires, envoyeLe)
        );
        return id;
    }

    private Collaborateur destinataire(String id) {
        return annuaire
            .trouver(id)
            .filter(Collaborateur::joignable)
            .orElseThrow(() ->
                CollaborationException.invalide("COL_RECIPIENT_UNKNOWN", "Destinataire absent de l'annuaire des comptes internes actifs", Map.of("id", id))
            );
    }

    /** Liaison de rattachement : référence fournie, sinon celle d'une étiquette citant la même liaison. */
    private String referenceLiaison(NouvelleConversation demande, List<Etiquette> etiquettes) {
        if (demande.liaisonId() == null) {
            return null;
        }
        if (demande.liaisonReference() != null && !demande.liaisonReference().isBlank()) {
            return regles.etiquettes(List.of(new Etiquette(demande.liaisonId(), demande.liaisonReference()))).get(0).reference();
        }
        return etiquettes.stream().filter(e -> e.liaisonId().equals(demande.liaisonId())).map(Etiquette::reference).findFirst().orElse(null);
    }

    private static Collaborateur depuisJeton(Expediteur e) {
        String nom = e.nomJeton() == null || e.nomJeton().isBlank() ? e.id() : e.nomJeton();
        return new Collaborateur(e.id(), nom, null, Set.of(), null, true);
    }

    /** Précision de la base (microseconde) : l'instant enregistré sert de curseur de pagination. */
    private Instant maintenant() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
