package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ReferenceCatalog} entity.
 */
@Schema(
    description = "Version du référentiel d'opérations d'un type de projet : graphe orienté\nacyclique (RG-26). Une version publiée est figée ; sa publication exige un\napprobateur distinct de l'auteur (algorithme 6.34, maker-checker)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceCatalogDTO implements Serializable {

    private UUID id;

    @NotNull
    private ProjectType projectType;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z]{2,4}-[0-9]+\\.[0-9]+$")
    @Schema(
        description = "Libellé de version, par exemple PM-2.1 ; unique tous types confondus.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String version;

    @NotNull
    private LocalDate effectiveFrom;

    @NotNull
    private Boolean published;

    @NotNull
    private UUID authorId;

    private UUID approverId;

    private Instant publishedAt;

    @Schema(description = "Différentiel avec la version précédente (JSON : conservés, ajoutés, retirés, modifiés).")
    @Lob
    private String diffSummary;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getDiffSummary() {
        return diffSummary;
    }

    public void setDiffSummary(String diffSummary) {
        this.diffSummary = diffSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceCatalogDTO)) {
            return false;
        }

        ReferenceCatalogDTO referenceCatalogDTO = (ReferenceCatalogDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, referenceCatalogDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceCatalogDTO{" +
            "id='" + getId() + "'" +
            ", projectType='" + getProjectType() + "'" +
            ", version='" + getVersion() + "'" +
            ", effectiveFrom='" + getEffectiveFrom() + "'" +
            ", published='" + getPublished() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", publishedAt='" + getPublishedAt() + "'" +
            ", diffSummary='" + getDiffSummary() + "'" +
            "}";
    }
}
