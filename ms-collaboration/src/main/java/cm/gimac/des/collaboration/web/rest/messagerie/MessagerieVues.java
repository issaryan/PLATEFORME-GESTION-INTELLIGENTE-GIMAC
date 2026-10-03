package cm.gimac.des.collaboration.web.rest.messagerie;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.ConversationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.MessageLu;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Représentations de la messagerie, conformes à docs/lot1/messagerie.md §2. */
public final class MessagerieVues {

    private MessagerieVues() {}

    /**
     * « groupe » (D-68) : groupe de discussion nommé ; absent ou faux, conduite d'avant (directe si possible).
     * « documents » et « transferts » (D-74) : voir {@link EnvoiRequest}.
     */
    public record CreationRequest(
        List<String> destinataires,
        String titre,
        UUID liaisonId,
        String liaisonReference,
        String contenu,
        List<Etiquette> etiquettes,
        Boolean groupe,
        List<UUID> documents,
        List<UUID> transferts
    ) {}

    /** Nouveau nom d'un groupe (D-68). */
    public record RenommageRequest(String titre) {}

    /** Membres ajoutés à un groupe (D-68), identifiants de l'annuaire. */
    public record AjoutMembresRequest(List<String> membres) {}

    /**
     * Nouveau message. « documents » (D-74) : tickets de documents déposés par envoi fragmenté ;
     * « transferts » : identifiants de pièces jointes existantes à repartager. Facultatifs.
     */
    public record EnvoiRequest(String contenu, List<Etiquette> etiquettes, List<UUID> documents, List<UUID> transferts) {}

    public record PageVue<T>(List<T> contenu, long total, int page, int taille) {}

    /** « avatarVersion » : version de la photo de profil, nulle sans photo (docs/lot1/mon-compte.md §1). */
    public record PersonneVue(String id, String nom, String fonction, Set<String> profils, String avatarVersion) {
        static PersonneVue de(Collaborateur c) {
            return new PersonneVue(c.id(), c.nom(), c.fonction(), c.profils(), c.avatarVersion());
        }
    }

    /**
     * Membre ou auteur : nom et fonction figés à l'entrée, photo de profil courante (avatarVersion) ;
     * « administrateur » : administrateur du groupe (D-68), toujours faux pour un auteur de message.
     */
    public record MembreVue(String id, String nom, String fonction, String avatarVersion, boolean administrateur) {
        static MembreVue de(ConversationMembre m, Map<String, String> avatars) {
            return new MembreVue(m.getMembreId(), m.getNom(), m.getFonction(), avatars.get(m.getMembreId()), m.isAdministrateur());
        }
    }

    /** Événement de groupe (D-68) : type et noms concernés. */
    public record EvenementVue(String type, List<String> cibles) {
        static EvenementVue de(EvenementGroupe e, List<String> cibles) {
            return e == null ? null : new EvenementVue(e.name(), cibles);
        }
    }

    public record LiaisonVue(UUID liaisonId, String reference) {}

    public record DernierMessageVue(UUID id, String auteurNom, String extrait, Instant envoyeLe, int piecesJointes, EvenementVue evenement) {}

    public record ConversationVue(
        UUID id,
        String type,
        String titre,
        LiaisonVue liaison,
        List<MembreVue> membres,
        DernierMessageVue dernierMessage,
        long nonLus,
        Instant misAJourLe,
        Instant creeeLe
    ) {
        static ConversationVue de(ConversationLue l, Map<String, String> avatars) {
            Conversation c = l.conversation();
            var d = l.dernierMessage();
            return new ConversationVue(
                c.getId(),
                c.getType().name(),
                c.getTitre(),
                c.getLiaisonId() == null ? null : new LiaisonVue(c.getLiaisonId(), c.getLiaisonReference()),
                l.membres().stream().map(m -> MembreVue.de(m, avatars)).toList(),
                d == null ? null : new DernierMessageVue(d.id(), d.auteurNom(), d.extrait(), d.envoyeLe(), d.piecesJointes(), EvenementVue.de(d.evenement(), d.cibles())),
                l.nonLus(),
                c.getMisAJourLe(),
                c.getCreeeLe()
            );
        }
    }

    public record PieceVue(UUID id, String nom, long taille, String typeMime, String empreinte) {
        static PieceVue de(PieceJointe p) {
            return new PieceVue(p.getId(), p.getNom(), p.getTaille(), p.getTypeMime(), p.getEmpreinte());
        }
    }

    public record MessageVue(
        UUID id,
        UUID conversationId,
        MembreVue auteur,
        boolean moi,
        String contenu,
        Instant envoyeLe,
        List<Etiquette> etiquettes,
        List<PieceVue> piecesJointes,
        EvenementVue evenement
    ) {
        static MessageVue de(MessageLu l, String appelantId, Map<String, String> avatars) {
            var m = l.message();
            return new MessageVue(
                m.getId(),
                m.getConversation().getId(),
                new MembreVue(m.getAuteurId(), m.getAuteurNom(), m.getAuteurFonction(), avatars.get(m.getAuteurId()), false),
                m.getAuteurId().equals(appelantId),
                l.contenu(),
                m.getEnvoyeLe(),
                l.etiquettes(),
                l.piecesJointes().stream().map(PieceVue::de).toList(),
                EvenementVue.de(m.getEvenement(), m.getEvenementCibles())
            );
        }
    }

    public record PageMessagesVue(List<MessageVue> contenu, boolean plusAnciens) {}

    public record NonLusVue(long total) {}
}
