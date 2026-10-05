package cm.gimac.des.collaboration.application.usecase;

import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase;
import cm.gimac.des.collaboration.application.port.in.GererGroupeUseCase;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.DocumentDepose;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.MetadonneesDocument;
import cm.gimac.des.collaboration.domain.event.MessageEnvoyePayload;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.domain.service.MasquagePan;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.collaboration.repository.ConversationMembreRepository;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.MessageRepository;
import cm.gimac.des.collaboration.repository.PieceJointeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
 * pièces déposées dans MS-DOCUMENT, événement collaboration.message-sent.v1 par l'outbox. Groupes de
 * discussion (D-68) : création explicite, administrateur, nom, membres ajoutés ou retirés, départ.
 * Dépôts fragmentés (D-74, docs/lot1/depots-fragmentes.md §5) : tickets de documents déjà déposés par
 * l'expéditeur et pièces repartagées, joints sans nouvel envoi ni copie.
 */
@Service
@Transactional
public class MessagerieService implements EnvoyerMessageUseCase, GererGroupeUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(MessagerieService.class);

    private final ConversationRepository conversations;
    private final ConversationMembreRepository membres;
    private final MessageRepository messages;
    private final PieceJointeRepository piecesJointes;
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
        PieceJointeRepository piecesJointes,
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
        this.piecesJointes = piecesJointes;
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
        boolean groupe = demande.groupe();
        // Tickets et transferts vérifiés avant toute écriture (D-74).
        List<PieceExistante> existantes = existantes(expediteur, demande.documents(), demande.transferts(), pieces.size());
        int nombrePieces = pieces.size() + existantes.size();
        // Groupe nommé (D-68) : le premier message est facultatif, la création suffit à l'ouvrir.
        boolean avecMessage = !groupe || (demande.contenu() != null && !demande.contenu().isBlank()) || nombrePieces > 0;
        String contenu = avecMessage ? regles.contenu(demande.contenu(), nombrePieces) : null;
        List<Etiquette> etiquettes = regles.etiquettes(demande.etiquettes());
        List<String> ids = regles.destinataires(demande.destinataires(), expediteur.id());
        String titre = groupe ? regles.titreGroupe(demande.titre()) : regles.titre(demande.titre());
        String reference = referenceLiaison(demande, etiquettes);
        List<Collaborateur> destinataires = ids.stream().map(this::destinataire).toList();
        Collaborateur auteur = annuaire.trouver(expediteur.id()).orElseGet(() -> depuisJeton(expediteur));
        Instant maintenant = maintenant();

        Conversation conversation;
        boolean creee;
        if (!groupe && regles.estDirecte(ids, titre, demande.liaisonId())) {
            String cle = regles.cleDirecte(expediteur.id(), ids.get(0));
            Optional<Conversation> existante = conversations.findByCleDirecte(cle);
            creee = existante.isEmpty();
            conversation = existante.orElseGet(() -> new Conversation(TypeConversation.DIRECTE, null, null, null, cle, expediteur.id(), maintenant));
        } else {
            creee = true;
            conversation = new Conversation(TypeConversation.GROUPE, titre, demande.liaisonId(), reference, null, expediteur.id(), maintenant);
        }
        if (creee) {
            ConversationMembre createur = conversation.ajouterMembre(auteur, maintenant);
            if (conversation.estGroupe()) {
                createur.nommerAdministrateur();
            }
            destinataires.forEach(d -> conversation.ajouterMembre(d, maintenant));
            conversations.save(conversation);
        }
        UUID messageId = null;
        Instant le = maintenant;
        if (groupe) {
            messageId = evenement(conversation, auteur, EvenementGroupe.GROUPE_CREE, List.of(titre), le, List.of());
            le = le.plus(1, ChronoUnit.MICROS);
        }
        if (avecMessage) {
            messageId = ajouterMessage(conversation, auteur, contenu, etiquettes, pieces, existantes, le);
        }
        return new Envoi(conversation.getId(), messageId, creee);
    }

    @Override
    public void renommer(Expediteur appelant, UUID conversationId, String titre) {
        Conversation c = groupeAdministre(appelant, conversationId);
        String nom = regles.titreGroupe(titre);
        if (nom.equals(c.getTitre())) {
            return;
        }
        Instant le = maintenant();
        c.renommer(nom, le);
        evenement(c, membreAuteur(c, appelant), EvenementGroupe.GROUPE_RENOMME, List.of(nom), le, List.of());
    }

    @Override
    public void ajouterMembres(Expediteur appelant, UUID conversationId, List<String> membres) {
        Conversation c = groupeAdministre(appelant, conversationId);
        List<String> nouveaux = regles
            .destinataires(membres, appelant.id())
            .stream()
            .filter(id -> c.membre(id).isEmpty())
            .toList();
        if (nouveaux.isEmpty()) {
            return;
        }
        int total = c.getMembres().size() + nouveaux.size();
        if (total > ReglesMessagerie.DESTINATAIRES_MAX + 1) {
            throw CollaborationException.invalide(
                "COL_TOO_MANY_RECIPIENTS",
                "Au plus " + (ReglesMessagerie.DESTINATAIRES_MAX + 1) + " membres par groupe",
                Map.of("nombre", total, "maximum", ReglesMessagerie.DESTINATAIRES_MAX + 1)
            );
        }
        List<Collaborateur> ajoutes = nouveaux.stream().map(this::destinataire).toList();
        Instant le = maintenant();
        ajoutes.forEach(a -> c.ajouterMembre(a, le));
        evenement(c, membreAuteur(c, appelant), EvenementGroupe.MEMBRES_AJOUTES, ajoutes.stream().map(Collaborateur::nom).toList(), le, List.of());
    }

    @Override
    public void retirerMembre(Expediteur appelant, UUID conversationId, String membreId) {
        Conversation c = conversations
            .pourMembre(conversationId, appelant.id())
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        if (!c.estGroupe()) {
            throw CollaborationException.invalide("COL_NOT_A_GROUP", "Seul un groupe de discussion se quitte ou perd des membres");
        }
        ConversationMembre moi = c.membre(appelant.id()).orElseThrow();
        ConversationMembre cible = c.membre(membreId).orElseThrow(() -> CollaborationException.introuvable("COL_MEMBER_NOT_FOUND", membreId));
        boolean depart = membreId.equals(appelant.id());
        if (!depart && !moi.isAdministrateur()) {
            throw CollaborationException.interdit("COL_GROUP_ADMIN_REQUIRED", "Réservé à l'administrateur du groupe");
        }
        Collaborateur auteur = membreAuteur(c, appelant);
        String nomCible = cible.getNom();
        c.retirerMembre(cible);
        evenement(
            c,
            auteur,
            depart ? EvenementGroupe.MEMBRE_PARTI : EvenementGroupe.MEMBRE_RETIRE,
            depart ? List.of() : List.of(nomCible),
            maintenant(),
            // Le membre retiré est prévenu : sa liste se met à jour et le fil lui est fermé.
            depart ? List.of() : List.of(membreId)
        );
    }

    /** Groupe dont l'appelant est l'administrateur ; sinon 404 (non-membre), 400 (directe) ou 403. */
    private Conversation groupeAdministre(Expediteur appelant, UUID conversationId) {
        Conversation c = conversations
            .pourMembre(conversationId, appelant.id())
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        if (!c.estGroupe()) {
            throw CollaborationException.invalide("COL_NOT_A_GROUP", "Seul un groupe de discussion se renomme ou change de membres");
        }
        if (!c.membre(appelant.id()).map(ConversationMembre::isAdministrateur).orElse(false)) {
            throw CollaborationException.interdit("COL_GROUP_ADMIN_REQUIRED", "Réservé à l'administrateur du groupe");
        }
        return c;
    }

    private static Collaborateur membreAuteur(Conversation c, Expediteur appelant) {
        ConversationMembre m = c.membre(appelant.id()).orElseThrow();
        return new Collaborateur(m.getMembreId(), m.getNom(), m.getFonction(), Set.of(), null, true);
    }

    @Override
    public Envoi envoyer(Expediteur expediteur, UUID conversationId, NouveauMessage demande, List<FichierJoint> fichiers) {
        Conversation conversation = conversations
            .pourMembre(conversationId, expediteur.id())
            .orElseThrow(() -> CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", conversationId));
        List<FichierJoint> pieces = regles.pieces(fichiers);
        List<PieceExistante> existantes = existantes(expediteur, demande.documents(), demande.transferts(), pieces.size());
        String contenu = regles.contenu(demande.contenu(), pieces.size() + existantes.size());
        List<Etiquette> etiquettes = regles.etiquettes(demande.etiquettes());
        UUID messageId = ajouterMessage(
            conversation,
            membreAuteur(conversation, expediteur),
            contenu,
            etiquettes,
            pieces,
            existantes,
            maintenant()
        );
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
        List<PieceExistante> existantes,
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
        // Ticket ou transfert (D-74) : la pièce référence le document existant, rien n'est envoyé ni copié.
        existantes.forEach(p -> message.joindre(p.documentRef(), p.nom(), p.taille(), p.typeMime(), p.empreinte()));
        messages.save(message);
        enregistrer(conversation, auteur, id, envoyeLe, List.of(), null);
        return id;
    }

    /** Pièce dont le document est déjà dans MS-DOCUMENT (ticket d'un dépôt fragmenté ou pièce repartagée, D-74). */
    private record PieceExistante(UUID documentRef, String nom, long taille, String typeMime, String empreinte) {}

    /**
     * Tickets et transferts d'un message (D-74), sans doublon ; 5 pièces au plus avec les fichiers
     * (COL_TOO_MANY_ATTACHMENTS), contrôlé avant tout appel à MS-DOCUMENT.
     */
    private List<PieceExistante> existantes(Expediteur expediteur, List<UUID> documents, List<UUID> transferts, int nombreFichiers) {
        List<UUID> tickets = regles.identifiants(documents);
        List<UUID> repartages = regles.identifiants(transferts);
        regles.nombrePieces(nombreFichiers + tickets.size() + repartages.size());
        List<PieceExistante> r = new ArrayList<>();
        tickets.forEach(t -> r.add(document(expediteur.id(), t)));
        repartages.forEach(p -> r.add(transfert(expediteur.id(), p)));
        return r;
    }

    /**
     * Ticket d'un dépôt fragmenté : document connu de MS-DOCUMENT, de finalité MESSAGERIE, déposé par
     * l'expéditeur et vérifié (empreinte) ; sinon COL_DOCUMENT_INVALID, sans rien révéler du document
     * d'un autre. Puis formats admis pour les documents (COL_ATTACHMENT_TYPE).
     */
    private PieceExistante document(String expediteurId, UUID ticket) {
        MetadonneesDocument m = documents
            .metadonnees(ticket)
            .filter(d -> DocumentStoragePort.FINALITE.equals(d.finalite()) && expediteurId.equals(d.deposant()))
            .filter(d -> d.sha256() != null && d.sha256().matches("^[0-9a-fA-F]{64}$"))
            .orElseThrow(() ->
                CollaborationException.invalide(
                    "COL_DOCUMENT_INVALID",
                    "Document inconnu, ou non déposé par l'expéditeur pour la messagerie",
                    Map.of("id", ticket.toString())
                )
            );
        ReglesMessagerie.NomEtType retenu = regles.document(m.nomFichier(), m.typeMime(), m.taille());
        return new PieceExistante(ticket, retenu.nom(), m.taille(), retenu.typeMime(), m.sha256().toLowerCase(Locale.ROOT));
    }

    /** Pièce repartagée : l'expéditeur doit être membre de la conversation d'origine ; sinon 404 COL_ATTACHMENT_NOT_FOUND. */
    private PieceExistante transfert(String expediteurId, UUID pieceId) {
        PieceJointe p = piecesJointes
            .avecConversation(pieceId)
            .filter(pj -> conversations.pourMembre(pj.getMessage().getConversation().getId(), expediteurId).isPresent())
            .orElseThrow(() -> CollaborationException.introuvable("COL_ATTACHMENT_NOT_FOUND", pieceId));
        return new PieceExistante(p.getDocumentRef(), p.getNom(), p.getTaille(), p.getTypeMime(), p.getEmpreinte());
    }

    /** Événement de groupe inscrit dans le fil (D-68) : sans contenu, ni étiquette, ni pièce. */
    private UUID evenement(Conversation c, Collaborateur auteur, EvenementGroupe e, List<String> cibles, Instant le, Collection<String> aussiPrevenir) {
        UUID id = UUID.randomUUID();
        messages.save(Message.evenement(id, c, auteur, chiffrement.chiffrer("", id), le, e, cibles));
        enregistrer(c, auteur, id, le, aussiPrevenir, e);
        return id;
    }

    /**
     * Dernier message de la conversation, lu par son auteur ; collaboration.message-sent.v1 aux autres membres,
     * avec l'événement de groupe éventuel (null pour un vrai message, D-80).
     */
    private void enregistrer(
        Conversation conversation,
        Collaborateur auteur,
        UUID id,
        Instant envoyeLe,
        Collection<String> aussiPrevenir,
        EvenementGroupe evenement
    ) {
        conversation.messageAjoute(id, envoyeLe);
        conversation.membre(auteur.id()).ifPresent(m -> m.marquerLu(id, envoyeLe));
        List<String> destinataires = new ArrayList<>(
            conversation.getMembres().stream().map(ConversationMembre::getMembreId).filter(m -> !m.equals(auteur.id())).toList()
        );
        aussiPrevenir.stream().filter(m -> !destinataires.contains(m)).forEach(destinataires::add);
        outbox.publish(
            "conversation",
            conversation.getId(),
            MessageEnvoyePayload.TYPE,
            new MessageEnvoyePayload(
                conversation.getId(),
                id,
                auteur.id(),
                List.copyOf(destinataires),
                envoyeLe,
                evenement == null ? null : evenement.name()
            )
        );
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
