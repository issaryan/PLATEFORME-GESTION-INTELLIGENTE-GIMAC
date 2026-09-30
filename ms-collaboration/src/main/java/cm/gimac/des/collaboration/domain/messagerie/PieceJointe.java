package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Pièce jointe déposée dans MS-DOCUMENT (dépôt WORM) : référence, nom, taille, type et empreinte SHA-256. */
@Entity
@Immutable
@Table(name = "piece_jointe")
public class PieceJointe extends EntiteMessagerie {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "document_ref", nullable = false)
    private UUID documentRef;

    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Column(name = "taille", nullable = false)
    private long taille;

    @Column(name = "type_mime", nullable = false, length = 150)
    private String typeMime;

    @Column(name = "empreinte", nullable = false, length = 64)
    private String empreinte;

    protected PieceJointe() {}

    PieceJointe(Message message, UUID documentRef, String nom, long taille, String typeMime, String empreinte) {
        this.message = message;
        this.documentRef = documentRef;
        this.nom = nom;
        this.taille = taille;
        this.typeMime = typeMime;
        this.empreinte = empreinte;
    }

    public Message getMessage() {
        return message;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public String getNom() {
        return nom;
    }

    public long getTaille() {
        return taille;
    }

    public String getTypeMime() {
        return typeMime;
    }

    public String getEmpreinte() {
        return empreinte;
    }
}
