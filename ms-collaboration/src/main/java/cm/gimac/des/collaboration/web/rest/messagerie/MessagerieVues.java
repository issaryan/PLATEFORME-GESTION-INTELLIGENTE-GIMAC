package cm.gimac.des.collaboration.web.rest.messagerie;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.ConversationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.MessageLu;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Représentations de la messagerie, conformes à docs/lot1/messagerie.md §2. */
public final class MessagerieVues {

    private MessagerieVues() {}

    public record CreationRequest(List<String> destinataires, String titre, UUID liaisonId, String liaisonReference, String contenu, List<Etiquette> etiquettes) {}

    public record EnvoiRequest(String contenu, List<Etiquette> etiquettes) {}

    public record PageVue<T>(List<T> contenu, long total, int page, int taille) {}

    public record PersonneVue(String id, String nom, String fonction, Set<String> profils) {
        static PersonneVue de(Collaborateur c) {
            return new PersonneVue(c.id(), c.nom(), c.fonction(), c.profils());
        }
    }

    public record MembreVue(String id, String nom, String fonction) {
        static MembreVue de(ConversationMembre m) {
            return new MembreVue(m.getMembreId(), m.getNom(), m.getFonction());
        }
    }

    public record LiaisonVue(UUID liaisonId, String reference) {}

    public record DernierMessageVue(UUID id, String auteurNom, String extrait, Instant envoyeLe, int piecesJointes) {}

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
        static ConversationVue de(ConversationLue l) {
            Conversation c = l.conversation();
            var d = l.dernierMessage();
            return new ConversationVue(
                c.getId(),
                c.getType().name(),
                c.getTitre(),
                c.getLiaisonId() == null ? null : new LiaisonVue(c.getLiaisonId(), c.getLiaisonReference()),
                l.membres().stream().map(MembreVue::de).toList(),
                d == null ? null : new DernierMessageVue(d.id(), d.auteurNom(), d.extrait(), d.envoyeLe(), d.piecesJointes()),
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
        List<PieceVue> piecesJointes
    ) {
        static MessageVue de(MessageLu l, String appelantId) {
            var m = l.message();
            return new MessageVue(
                m.getId(),
                m.getConversation().getId(),
                new MembreVue(m.getAuteurId(), m.getAuteurNom(), m.getAuteurFonction()),
                m.getAuteurId().equals(appelantId),
                l.contenu(),
                m.getEnvoyeLe(),
                l.etiquettes(),
                l.piecesJointes().stream().map(PieceVue::de).toList()
            );
        }
    }

    public record PageMessagesVue(List<MessageVue> contenu, boolean plusAnciens) {}

    public record NonLusVue(long total) {}
}
