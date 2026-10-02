package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

/**
 * Message envoyé, immuable (D-64 : une correction passe par un nouveau message). Le contenu, déjà
 * masqué selon RG-52, n'est conservé que chiffré (AES-256-GCM, clé hors base). Un déclencheur de la
 * base refuse aussi toute modification.
 */
@Entity
@Immutable
@Table(name = "message")
public class Message extends EntiteMessagerie {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "auteur_id", nullable = false, length = 64)
    private String auteurId;

    @Column(name = "auteur_nom", nullable = false, length = 200)
    private String auteurNom;

    @Column(name = "auteur_fonction", length = 200)
    private String auteurFonction;

    @Column(name = "contenu_chiffre", nullable = false)
    private byte[] contenuChiffre;

    @Column(name = "envoye_le", nullable = false)
    private Instant envoyeLe;

    /** Événement de groupe (D-68) ; nul pour un message ordinaire. */
    @Enumerated(EnumType.STRING)
    @Column(name = "evenement", length = 30)
    private EvenementGroupe evenement;

    /** Noms concernés par l'événement, séparés par un saut de ligne. */
    @Column(name = "evenement_cibles", length = 4000)
    private String evenementCibles;

    @OneToMany(mappedBy = "message", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @OrderBy("reference ASC")
    @BatchSize(size = 50)
    private List<MessageEtiquette> etiquettes = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @OrderBy("nom ASC")
    @BatchSize(size = 50)
    private List<PieceJointe> piecesJointes = new ArrayList<>();

    protected Message() {}

    public Message(UUID id, Conversation conversation, Collaborateur auteur, byte[] contenuChiffre, Instant envoyeLe) {
        super(id);
        this.conversation = conversation;
        this.auteurId = auteur.id();
        this.auteurNom = auteur.nom();
        this.auteurFonction = auteur.fonction();
        this.contenuChiffre = contenuChiffre.clone();
        this.envoyeLe = envoyeLe;
    }

    /** Événement de groupe : contenu chiffré vide, cibles en clair (noms et nom du groupe, jamais de données de carte). */
    public static Message evenement(UUID id, Conversation conversation, Collaborateur auteur, byte[] contenuVideChiffre, Instant le, EvenementGroupe e, List<String> cibles) {
        Message m = new Message(id, conversation, auteur, contenuVideChiffre, le);
        m.evenement = e;
        m.evenementCibles = cibles.isEmpty() ? null : String.join("\n", cibles);
        return m;
    }

    public EvenementGroupe getEvenement() {
        return evenement;
    }

    public List<String> getEvenementCibles() {
        return evenementCibles == null ? List.of() : List.of(evenementCibles.split("\n"));
    }

    public void etiqueter(Etiquette e) {
        etiquettes.add(new MessageEtiquette(this, e.liaisonId(), e.reference()));
    }

    public PieceJointe joindre(UUID documentRef, String nom, long taille, String typeMime, String empreinte) {
        PieceJointe p = new PieceJointe(this, documentRef, nom, taille, typeMime, empreinte);
        piecesJointes.add(p);
        return p;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public String getAuteurId() {
        return auteurId;
    }

    public String getAuteurNom() {
        return auteurNom;
    }

    public String getAuteurFonction() {
        return auteurFonction;
    }

    public byte[] getContenuChiffre() {
        return contenuChiffre.clone();
    }

    public Instant getEnvoyeLe() {
        return envoyeLe;
    }

    public List<MessageEtiquette> getEtiquettes() {
        return List.copyOf(etiquettes);
    }

    public List<PieceJointe> getPiecesJointes() {
        return List.copyOf(piecesJointes);
    }
}
