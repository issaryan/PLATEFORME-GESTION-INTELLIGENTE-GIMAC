package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.ReceiptKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Accusé de réception (remis, lu), métadonnée visible du serveur.
 */
@Entity
@Table(name = "message_receipt")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageReceipt implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Membre destinataire (MS-IAM).
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private ReceiptKind kind;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "room" }, allowSetters = true)
    private MessageMetadata message;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MessageReceipt id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public MessageReceipt userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ReceiptKind getKind() {
        return this.kind;
    }

    public MessageReceipt kind(ReceiptKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(ReceiptKind kind) {
        this.kind = kind;
    }

    public Instant getReceivedAt() {
        return this.receivedAt;
    }

    public MessageReceipt receivedAt(Instant receivedAt) {
        this.setReceivedAt(receivedAt);
        return this;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public MessageMetadata getMessage() {
        return this.message;
    }

    public void setMessage(MessageMetadata messageMetadata) {
        this.message = messageMetadata;
    }

    public MessageReceipt message(MessageMetadata messageMetadata) {
        this.setMessage(messageMetadata);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageReceipt)) {
            return false;
        }
        return getId() != null && getId().equals(((MessageReceipt) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageReceipt{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", kind='" + getKind() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            "}";
    }
}
