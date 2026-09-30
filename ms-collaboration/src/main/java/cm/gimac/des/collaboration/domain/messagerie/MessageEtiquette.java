package cm.gimac.des.collaboration.domain.messagerie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Liaison citée par un message ; ouvre le dossier /des/parcours/{liaisonId}, droits vérifiés à l'ouverture. */
@Entity
@Immutable
@Table(name = "message_etiquette")
public class MessageEtiquette extends EntiteMessagerie {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "reference", nullable = false, length = 30)
    private String reference;

    protected MessageEtiquette() {}

    MessageEtiquette(Message message, UUID liaisonId, String reference) {
        this.message = message;
        this.liaisonId = liaisonId;
        this.reference = reference;
    }

    public Message getMessage() {
        return message;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getReference() {
        return reference;
    }

    public Etiquette vue() {
        return new Etiquette(liaisonId, reference);
    }
}
