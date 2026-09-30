package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
