package cm.gimac.des.pilotage.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Projection locale de l'annuaire (iam.user-changed.v1), lue sans appel
 * synchrone à MS-IAM pour vérifier qu'un responsable est actif.
 */
@Entity
@Table(name = "collaborator_projection")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CollaboratorProjection implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant du collaborateur dans MS-IAM.
     */
    @NotNull
    @Column(name = "collaborator_id", nullable = false, unique = true)
    private UUID collaboratorId;

    @NotNull
    @Size(max = 255)
    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * Service ou pôle d'affectation, pour choisir le tableau d'une décision.
     */
    @Size(max = 30)
    @Column(name = "service_code", length = 30)
    private String serviceCode;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CollaboratorProjection id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCollaboratorId() {
        return this.collaboratorId;
    }

    public CollaboratorProjection collaboratorId(UUID collaboratorId) {
        this.setCollaboratorId(collaboratorId);
        return this;
    }

    public void setCollaboratorId(UUID collaboratorId) {
        this.collaboratorId = collaboratorId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public CollaboratorProjection displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getActive() {
        return this.active;
    }

    public CollaboratorProjection active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public CollaboratorProjection serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public CollaboratorProjection updatedAt(Instant updatedAt) {
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
        if (!(o instanceof CollaboratorProjection)) {
            return false;
        }
        return getId() != null && getId().equals(((CollaboratorProjection) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CollaboratorProjection{" +
            "id=" + getId() +
            ", collaboratorId='" + getCollaboratorId() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", active='" + getActive() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
