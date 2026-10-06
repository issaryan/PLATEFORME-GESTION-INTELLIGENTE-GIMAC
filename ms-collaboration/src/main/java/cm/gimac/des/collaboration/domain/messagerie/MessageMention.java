package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Membre mentionné par un message (D-82) : identifiant IAM et nom à l'envoi (instantané, comme celui
 * de l'auteur), dans l'ordre choisi par l'expéditeur. Immuable comme le message.
 */
@Entity
@Immutable
@Table(name = "message_mention")
public class MessageMention extends EntiteMessagerie {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "membre_id", nullable = false, length = 64)
    private String membreId;

    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "rang", nullable = false)
    private int rang;

    protected MessageMention() {}

    MessageMention(Message message, String membreId, String nom, int rang) {
        this.message = message;
        this.membreId = membreId;
        this.nom = nom;
        this.rang = rang;
    }

    public Message getMessage() {
        return message;
    }

    public String getMembreId() {
        return membreId;
    }

    public String getNom() {
        return nom;
    }

    public int getRang() {
        return rang;
    }
}
