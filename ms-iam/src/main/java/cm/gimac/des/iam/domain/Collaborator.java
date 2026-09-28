package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.CollaboratorStatus;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Collaborateur projeté depuis l'IdP et l'annuaire (User du CC, renommé pour
 * éviter l'entité User intégrée à JHipster). Aucun mot de passe (RG-65).
 */
@Entity
@Table(name = "collaborator")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Collaborator implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant fédéré issu de l'IdP (claim sub).
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "federated_id", length = 255, nullable = false, unique = true)
    private String federatedId;

    /**
     * Identifiant de l'annuaire (objectGUID), pour détecter les suppressions.
     */
    @Size(max = 255)
    @Column(name = "directory_id", length = 255, unique = true)
    private String directoryId;

    @NotNull
    @Size(max = 255)
    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CollaboratorStatus status;

    /**
     * Plus haut niveau permanent, déduit des rôles approuvés.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private HabilitationLevel level;

    /**
     * Institution de rattachement, pour les correspondants du portail (D-21).
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Size(max = 5)
    @Column(name = "language", length = 5, nullable = false)
    private String language;

    @NotNull
    @Min(value = 0)
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Curseur de synchronisation (uSNChanged) de la dernière mise à jour.
     */
    @Column(name = "directory_cursor")
    private Long directoryCursor;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Collaborator id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFederatedId() {
        return this.federatedId;
    }

    public Collaborator federatedId(String federatedId) {
        this.setFederatedId(federatedId);
        return this;
    }

    public void setFederatedId(String federatedId) {
        this.federatedId = federatedId;
    }

    public String getDirectoryId() {
        return this.directoryId;
    }

    public Collaborator directoryId(String directoryId) {
        this.setDirectoryId(directoryId);
        return this;
    }

    public void setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Collaborator displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return this.email;
    }

    public Collaborator email(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CollaboratorStatus getStatus() {
        return this.status;
    }

    public Collaborator status(CollaboratorStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(CollaboratorStatus status) {
        this.status = status;
    }

    public HabilitationLevel getLevel() {
        return this.level;
    }

    public Collaborator level(HabilitationLevel level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(HabilitationLevel level) {
        this.level = level;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Collaborator institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getLanguage() {
        return this.language;
    }

    public Collaborator language(String language) {
        this.setLanguage(language);
        return this;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getFailedAttempts() {
        return this.failedAttempts;
    }

    public Collaborator failedAttempts(Integer failedAttempts) {
        this.setFailedAttempts(failedAttempts);
        return this;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Instant getLastLoginAt() {
        return this.lastLoginAt;
    }

    public Collaborator lastLoginAt(Instant lastLoginAt) {
        this.setLastLoginAt(lastLoginAt);
        return this;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Long getDirectoryCursor() {
        return this.directoryCursor;
    }

    public Collaborator directoryCursor(Long directoryCursor) {
        this.setDirectoryCursor(directoryCursor);
        return this;
    }

    public void setDirectoryCursor(Long directoryCursor) {
        this.directoryCursor = directoryCursor;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Collaborator)) {
            return false;
        }
        return getId() != null && getId().equals(((Collaborator) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Collaborator{" +
            "id=" + getId() +
            ", federatedId='" + getFederatedId() + "'" +
            ", directoryId='" + getDirectoryId() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", email='" + getEmail() + "'" +
            ", status='" + getStatus() + "'" +
            ", level='" + getLevel() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", language='" + getLanguage() + "'" +
            ", failedAttempts=" + getFailedAttempts() +
            ", lastLoginAt='" + getLastLoginAt() + "'" +
            ", directoryCursor=" + getDirectoryCursor() +
            "}";
    }
}
