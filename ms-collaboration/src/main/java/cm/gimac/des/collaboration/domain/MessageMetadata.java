package cm.gimac.des.collaboration.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Métadonnées d'un message chiffré. Écart CC corrigé : l'entité Message du CC
 * porte le chiffré ; conformément à D-28, le chiffré reste dans Matrix Synapse
 * et seule la référence de l'événement est conservée (renommage en
 * MessageMetadata, qui évite aussi toute confusion avec Message de Spring).
 */
@Entity
@Table(name = "message_metadata")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant de l'événement dans Matrix Synapse.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "matrix_event_id", length = 255, nullable = false, unique = true)
    private String matrixEventId;

    /**
     * Auteur (MS-IAM).
     */
    @NotNull
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @NotNull
    @Size(max = 100)
    @Column(name = "terminal_id", length = 100, nullable = false)
    private String terminalId;

    @NotNull
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /**
     * Identifiant de la session de groupe utilisée.
     */
    @Size(max = 255)
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * Indice de cliquet du message dans la chaîne.
     */
    @Min(value = 0L)
    @Column(name = "ratchet_index")
    private Long ratchetIndex;

    /**
     * Taille du chiffré, en octets.
     */
    @Min(value = 0)
    @Column(name = "ciphertext_size")
    private Integer ciphertextSize;

    /**
     * Pièces jointes : identifiants de documents (MS-DOCUMENT), séparés par des virgules.
     */
    @Size(max = 2000)
    @Column(name = "attachment_refs", length = 2000)
    private String attachmentRefs;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "roomArchive" }, allowSetters = true)
    private Room room;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MessageMetadata id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMatrixEventId() {
        return this.matrixEventId;
    }

    public MessageMetadata matrixEventId(String matrixEventId) {
        this.setMatrixEventId(matrixEventId);
        return this;
    }

    public void setMatrixEventId(String matrixEventId) {
        this.matrixEventId = matrixEventId;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public MessageMetadata authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getTerminalId() {
        return this.terminalId;
    }

    public MessageMetadata terminalId(String terminalId) {
        this.setTerminalId(terminalId);
        return this;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Instant getSentAt() {
        return this.sentAt;
    }

    public MessageMetadata sentAt(Instant sentAt) {
        this.setSentAt(sentAt);
        return this;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public MessageMetadata sessionId(String sessionId) {
        this.setSessionId(sessionId);
        return this;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getRatchetIndex() {
        return this.ratchetIndex;
    }

    public MessageMetadata ratchetIndex(Long ratchetIndex) {
        this.setRatchetIndex(ratchetIndex);
        return this;
    }

    public void setRatchetIndex(Long ratchetIndex) {
        this.ratchetIndex = ratchetIndex;
    }

    public Integer getCiphertextSize() {
        return this.ciphertextSize;
    }

    public MessageMetadata ciphertextSize(Integer ciphertextSize) {
        this.setCiphertextSize(ciphertextSize);
        return this;
    }

    public void setCiphertextSize(Integer ciphertextSize) {
        this.ciphertextSize = ciphertextSize;
    }

    public String getAttachmentRefs() {
        return this.attachmentRefs;
    }

    public MessageMetadata attachmentRefs(String attachmentRefs) {
        this.setAttachmentRefs(attachmentRefs);
        return this;
    }

    public void setAttachmentRefs(String attachmentRefs) {
        this.attachmentRefs = attachmentRefs;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public MessageMetadata room(Room room) {
        this.setRoom(room);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageMetadata)) {
            return false;
        }
        return getId() != null && getId().equals(((MessageMetadata) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageMetadata{" +
            "id=" + getId() +
            ", matrixEventId='" + getMatrixEventId() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", terminalId='" + getTerminalId() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            ", sessionId='" + getSessionId() + "'" +
            ", ratchetIndex=" + getRatchetIndex() +
            ", ciphertextSize=" + getCiphertextSize() +
            ", attachmentRefs='" + getAttachmentRefs() + "'" +
            "}";
    }
}
