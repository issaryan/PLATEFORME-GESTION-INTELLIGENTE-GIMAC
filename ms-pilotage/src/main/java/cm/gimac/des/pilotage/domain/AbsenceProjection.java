package cm.gimac.des.pilotage.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Absence validée (iam.availability-validated.v1), pour vérifier qu'un
 * responsable est disponible à l'échéance (algorithme 6.55, étape 2).
 */
@Entity
@Table(name = "absence_projection")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AbsenceProjection implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant de la disponibilité dans MS-IAM.
     */
    @NotNull
    @Column(name = "availability_id", nullable = false, unique = true)
    private UUID availabilityId;

    /**
     * Identifiant du collaborateur dans MS-IAM.
     */
    @NotNull
    @Column(name = "collaborator_id", nullable = false)
    private UUID collaboratorId;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AbsenceProjection id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAvailabilityId() {
        return this.availabilityId;
    }

    public AbsenceProjection availabilityId(UUID availabilityId) {
        this.setAvailabilityId(availabilityId);
        return this;
    }

    public void setAvailabilityId(UUID availabilityId) {
        this.availabilityId = availabilityId;
    }

    public UUID getCollaboratorId() {
        return this.collaboratorId;
    }

    public AbsenceProjection collaboratorId(UUID collaboratorId) {
        this.setCollaboratorId(collaboratorId);
        return this;
    }

    public void setCollaboratorId(UUID collaboratorId) {
        this.collaboratorId = collaboratorId;
    }

    public Instant getStartsAt() {
        return this.startsAt;
    }

    public AbsenceProjection startsAt(Instant startsAt) {
        this.setStartsAt(startsAt);
        return this;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return this.endsAt;
    }

    public AbsenceProjection endsAt(Instant endsAt) {
        this.setEndsAt(endsAt);
        return this;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public AbsenceProjection updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbsenceProjection)) {
            return false;
        }
        return getId() != null && getId().equals(((AbsenceProjection) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AbsenceProjection{" +
            "id=" + getId() +
            ", availabilityId='" + getAvailabilityId() + "'" +
            ", collaboratorId='" + getCollaboratorId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
