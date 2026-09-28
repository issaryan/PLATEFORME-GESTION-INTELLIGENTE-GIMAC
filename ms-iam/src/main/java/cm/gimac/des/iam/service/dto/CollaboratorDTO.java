package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.CollaboratorStatus;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.Collaborator} entity.
 */
@Schema(
    description = "Collaborateur projeté depuis l'IdP et l'annuaire (User du CC, renommé pour\néviter l'entité User intégrée à JHipster). Aucun mot de passe (RG-65)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CollaboratorDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Identifiant fédéré issu de l'IdP (claim sub).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String federatedId;

    @Size(max = 255)
    @Schema(description = "Identifiant de l'annuaire (objectGUID), pour détecter les suppressions.")
    private String directoryId;

    @NotNull
    @Size(max = 255)
    private String displayName;

    @Size(max = 255)
    private String email;

    @NotNull
    private CollaboratorStatus status;

    @NotNull
    @Schema(description = "Plus haut niveau permanent, déduit des rôles approuvés.", requiredMode = Schema.RequiredMode.REQUIRED)
    private HabilitationLevel level;

    @Schema(description = "Institution de rattachement, pour les correspondants du portail (D-21).")
    private UUID institutionId;

    @NotNull
    @Size(max = 5)
    private String language;

    @NotNull
    @Min(value = 0)
    private Integer failedAttempts;

    private Instant lastLoginAt;

    @Schema(description = "Curseur de synchronisation (uSNChanged) de la dernière mise à jour.")
    private Long directoryCursor;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFederatedId() {
        return federatedId;
    }

    public void setFederatedId(String federatedId) {
        this.federatedId = federatedId;
    }

    public String getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CollaboratorStatus getStatus() {
        return status;
    }

    public void setStatus(CollaboratorStatus status) {
        this.status = status;
    }

    public HabilitationLevel getLevel() {
        return level;
    }

    public void setLevel(HabilitationLevel level) {
        this.level = level;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Long getDirectoryCursor() {
        return directoryCursor;
    }

    public void setDirectoryCursor(Long directoryCursor) {
        this.directoryCursor = directoryCursor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CollaboratorDTO)) {
            return false;
        }

        CollaboratorDTO collaboratorDTO = (CollaboratorDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, collaboratorDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CollaboratorDTO{" +
            "id='" + getId() + "'" +
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
