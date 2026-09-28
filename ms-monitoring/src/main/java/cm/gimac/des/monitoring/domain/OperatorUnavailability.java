package cm.gimac.des.monitoring.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Indisponibilité validée d'un opérateur, projetée depuis
 * iam.availability-validated.v1 (RG-74) ; ensemble U de l'algorithme 6.45.
 */
@Entity
@Table(name = "operator_unavailability")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperatorUnavailability implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant de la disponibilité dans MS-IAM (clé d'idempotence).
     */
    @NotNull
    @Column(name = "availability_id", nullable = false, unique = true)
    private UUID availabilityId;

    @NotNull
    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @NotNull
    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    /**
     * Annulation reçue de MS-IAM : l'indisponibilité ne compte plus.
     */
    @NotNull
    @Column(name = "cancelled", nullable = false)
    private Boolean cancelled;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public OperatorUnavailability id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAvailabilityId() {
        return this.availabilityId;
    }

    public OperatorUnavailability availabilityId(UUID availabilityId) {
        this.setAvailabilityId(availabilityId);
        return this;
    }

    public void setAvailabilityId(UUID availabilityId) {
        this.availabilityId = availabilityId;
    }

    public UUID getOperatorId() {
        return this.operatorId;
    }

    public OperatorUnavailability operatorId(UUID operatorId) {
        this.setOperatorId(operatorId);
        return this;
    }

    public void setOperatorId(UUID operatorId) {
        this.operatorId = operatorId;
    }

    public Instant getStartsAt() {
        return this.startsAt;
    }

    public OperatorUnavailability startsAt(Instant startsAt) {
        this.setStartsAt(startsAt);
        return this;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return this.endsAt;
    }

    public OperatorUnavailability endsAt(Instant endsAt) {
        this.setEndsAt(endsAt);
        return this;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Instant getValidatedAt() {
        return this.validatedAt;
    }

    public OperatorUnavailability validatedAt(Instant validatedAt) {
        this.setValidatedAt(validatedAt);
        return this;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Boolean getCancelled() {
        return this.cancelled;
    }

    public OperatorUnavailability cancelled(Boolean cancelled) {
        this.setCancelled(cancelled);
        return this;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperatorUnavailability)) {
            return false;
        }
        return getId() != null && getId().equals(((OperatorUnavailability) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperatorUnavailability{" +
            "id=" + getId() +
            ", availabilityId='" + getAvailabilityId() + "'" +
            ", operatorId='" + getOperatorId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", cancelled='" + getCancelled() + "'" +
            "}";
    }
}
