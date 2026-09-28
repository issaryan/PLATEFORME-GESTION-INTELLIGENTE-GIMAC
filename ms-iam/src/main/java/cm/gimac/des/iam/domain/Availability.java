package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.AvailabilityType;
import cm.gimac.des.iam.domain.enumeration.ValidationState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Congé ou absence déclaré puis validé (BF-ADMIN, RG-74, IAM-09).
 * Le justificatif est stocké dans MS-DOCUMENT, à accès restreint (D-29).
 */
@Entity
@Table(name = "availability")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Availability implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AvailabilityType type;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Size(max = 500)
    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "justification_document_id")
    private UUID justificationDocumentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_state", nullable = false)
    private ValidationState validationState;

    @Column(name = "validator_id")
    private UUID validatorId;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @ManyToOne(optional = false)
    @NotNull
    private Collaborator collaborator;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Availability id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AvailabilityType getType() {
        return this.type;
    }

    public Availability type(AvailabilityType type) {
        this.setType(type);
        return this;
    }

    public void setType(AvailabilityType type) {
        this.type = type;
    }

    public Instant getStartsAt() {
        return this.startsAt;
    }

    public Availability startsAt(Instant startsAt) {
        this.setStartsAt(startsAt);
        return this;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return this.endsAt;
    }

    public Availability endsAt(Instant endsAt) {
        this.setEndsAt(endsAt);
        return this;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public String getReason() {
        return this.reason;
    }

    public Availability reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getJustificationDocumentId() {
        return this.justificationDocumentId;
    }

    public Availability justificationDocumentId(UUID justificationDocumentId) {
        this.setJustificationDocumentId(justificationDocumentId);
        return this;
    }

    public void setJustificationDocumentId(UUID justificationDocumentId) {
        this.justificationDocumentId = justificationDocumentId;
    }

    public ValidationState getValidationState() {
        return this.validationState;
    }

    public Availability validationState(ValidationState validationState) {
        this.setValidationState(validationState);
        return this;
    }

    public void setValidationState(ValidationState validationState) {
        this.validationState = validationState;
    }

    public UUID getValidatorId() {
        return this.validatorId;
    }

    public Availability validatorId(UUID validatorId) {
        this.setValidatorId(validatorId);
        return this;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Instant getValidatedAt() {
        return this.validatedAt;
    }

    public Availability validatedAt(Instant validatedAt) {
        this.setValidatedAt(validatedAt);
        return this;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Collaborator getCollaborator() {
        return this.collaborator;
    }

    public void setCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
    }

    public Availability collaborator(Collaborator collaborator) {
        this.setCollaborator(collaborator);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Availability)) {
            return false;
        }
        return getId() != null && getId().equals(((Availability) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Availability{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", reason='" + getReason() + "'" +
            ", justificationDocumentId='" + getJustificationDocumentId() + "'" +
            ", validationState='" + getValidationState() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            "}";
    }
}
