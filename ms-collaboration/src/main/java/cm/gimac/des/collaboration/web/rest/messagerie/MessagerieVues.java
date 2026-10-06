package cm.gimac.des.collaboration.web.rest.messagerie;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.CitationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.ConversationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.LectureMembre;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.MessageLu;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe;
import cm.gimac.des.collaboration.domain.messagerie.Lecture;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import com.fasterxml.jackson.annotation.JsonInclude;
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
     * « documents » et « transferts » (D-74), « reponseA » et « mentions » (D-82) : voir {@link EnvoiRequest}.
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
        List<UUID> transferts,
        UUID reponseA,
        List<String> mentions
    ) {}

    /** Nouveau nom d'un groupe (D-68). */
    public record RenommageRequest(String titre) {}

    /** Membres ajoutés à un groupe (D-68), identifiants de l'annuaire. */
    public record AjoutMembresRequest(List<String> membres) {}

    /**
     * Nouveau message. « documents » (D-74) : tickets de documents déposés par envoi fragmenté ;
     * « transferts » : identifiants de pièces jointes existantes à repartager. « reponseA » (D-82) : message
     * cité ; « mentions » : identifiants des membres mentionnés avec « @ ». Facultatifs.
     */
    public record EnvoiRequest(String contenu, List<Etiquette> etiquettes, List<UUID> documents, List<UUID> transferts, UUID reponseA, List<String> mentions) {}

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

    /**
     * État de lecture d'un message de l'appelant (D-88, docs/lot1/accuses-lecture.md §1) : ENVOYE, LU_PAR_CERTAINS
     * ou LU ; « lus » sur « destinataires » (membres présents à l'envoi, hors auteur, encore membres).
     */
    public record LectureVue(String etat, int lus, int destinataires) {
        static LectureVue de(Lecture l) {
            return l == null ? null : new LectureVue(l.etat().name(), l.lus(), l.destinataires());
        }
    }

    /** Destinataire du détail « Lu par » (D-88) : « luLe » nul s'il n'a pas lu ou si l'heure n'est pas connue. */
    public record LectureMembreVue(PersonneCiteeVue membre, boolean lu, Instant luLe) {
        static LectureMembreVue de(LectureMembre l) {
            return new LectureMembreVue(new PersonneCiteeVue(l.id(), l.nom()), l.lu(), l.luLe());
        }
    }

    /** « lecture » (D-88) : présent seulement pour un message de l'appelant (absent sinon et pour un événement). */
    public record DernierMessageVue(
        UUID id,
        String auteurNom,
        String extrait,
        Instant envoyeLe,
        int piecesJointes,
        EvenementVue evenement,
        @JsonInclude(JsonInclude.Include.NON_NULL) LectureVue lecture
    ) {}

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
                d == null
                    ? null
                    : new DernierMessageVue(
                        d.id(),
                        d.auteurNom(),
                        d.extrait(),
                        d.envoyeLe(),
                        d.piecesJointes(),
                        EvenementVue.de(d.evenement(), d.cibles()),
                        LectureVue.de(d.lecture())
                    ),
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

    /** Personne citée ou mentionnée (D-82) : identifiant et nom. */
    public record PersonneCiteeVue(String id, String nom) {}

    /**
     * Message cité (D-82) : extrait de 120 caractères, nombre de pièces jointes. « supprime » : message
     * cité purgé ou illisible ; seuls l'identifiant et l'extrait vide restent (auteur et date nuls).
     */
    public record ReponseAVue(UUID id, PersonneCiteeVue auteur, String extrait, int piecesJointes, Instant envoyeLe, boolean supprime) {
        static ReponseAVue de(CitationLue c) {
            if (c == null) {
                return null;
            }
            return new ReponseAVue(
                c.id(),
                c.supprime() ? null : new PersonneCiteeVue(c.auteurId(), c.auteurNom()),
                c.extrait(),
                c.piecesJointes(),
                c.envoyeLe(),
                c.supprime()
            );
        }
    }

    /**
     * « reponseA », « mentions » et « mentionneMoi » (D-82) : message cité, membres mentionnés ; vrai si le
     * lecteur est mentionné ou auteur du message cité, jamais pour son propre message. « lecture » (D-88) :
     * état de lecture, présent seulement pour un vrai message de l'appelant.
     */
    public record MessageVue(
        UUID id,
        UUID conversationId,
        MembreVue auteur,
        boolean moi,
        String contenu,
        Instant envoyeLe,
        List<Etiquette> etiquettes,
        List<PieceVue> piecesJointes,
        EvenementVue evenement,
        ReponseAVue reponseA,
        List<PersonneCiteeVue> mentions,
        boolean mentionneMoi,
        @JsonInclude(JsonInclude.Include.NON_NULL) LectureVue lecture
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
                EvenementVue.de(m.getEvenement(), m.getEvenementCibles()),
                ReponseAVue.de(l.reponseA()),
                l.mentions().stream().map(x -> new PersonneCiteeVue(x.id(), x.nom())).toList(),
                l.concerne(appelantId),
                LectureVue.de(l.lecture())
            );
        }
    }

    public record PageMessagesVue(List<MessageVue> contenu, boolean plusAnciens) {}

    public record NonLusVue(long total) {}
}
