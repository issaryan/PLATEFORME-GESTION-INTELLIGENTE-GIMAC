package cm.gimac.des.pilotage.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Dérogation motivée à la limite d'en-cours (algorithme 6.54, étape 4) :
 * approbateur distinct du demandeur, expiration au plus 72 h après l'octroi,
 * une seule dérogation active par colonne.
 */
@Entity
@Table(name = "wip_override")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WipOverride implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Demandeur (MS-IAM).
     */
    @NotNull
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    /**
     * Approbateur, responsable ou chef du pôle, distinct du demandeur (MS-IAM).
     */
    @NotNull
    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @NotNull
    @Size(max = 500)
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @NotNull
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Avis d'expiration émis par la veille quotidienne (étape 6).
     */
    @Column(name = "expiry_notified_at")
    private Instant expiryNotifiedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "board" }, allowSetters = true)
    private BoardColumn boardColumn;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "board", "boardColumn", "actionSheet", "decision" }, allowSetters = true)
    private Task task;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public WipOverride id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return this.requesterId;
    }

    public WipOverride requesterId(UUID requesterId) {
        this.setRequesterId(requesterId);
        return this;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public WipOverride approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public String getReason() {
        return this.reason;
    }

    public WipOverride reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getGrantedAt() {
        return this.grantedAt;
    }

    public WipOverride grantedAt(Instant grantedAt) {
        this.setGrantedAt(grantedAt);
        return this;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public WipOverride expiresAt(Instant expiresAt) {
        this.setExpiresAt(expiresAt);
        return this;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getExpiryNotifiedAt() {
        return this.expiryNotifiedAt;
    }

    public WipOverride expiryNotifiedAt(Instant expiryNotifiedAt) {
        this.setExpiryNotifiedAt(expiryNotifiedAt);
        return this;
    }

    public void setExpiryNotifiedAt(Instant expiryNotifiedAt) {
        this.expiryNotifiedAt = expiryNotifiedAt;
    }

    public BoardColumn getBoardColumn() {
        return this.boardColumn;
    }

    public void setBoardColumn(BoardColumn boardColumn) {
        this.boardColumn = boardColumn;
    }

    public WipOverride boardColumn(BoardColumn boardColumn) {
        this.setBoardColumn(boardColumn);
        return this;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public WipOverride task(Task task) {
        this.setTask(task);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WipOverride)) {
            return false;
        }
        return getId() != null && getId().equals(((WipOverride) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WipOverride{" +
            "id=" + getId() +
            ", requesterId='" + getRequesterId() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", reason='" + getReason() + "'" +
            ", grantedAt='" + getGrantedAt() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", expiryNotifiedAt='" + getExpiryNotifiedAt() + "'" +
            "}";
    }
}
