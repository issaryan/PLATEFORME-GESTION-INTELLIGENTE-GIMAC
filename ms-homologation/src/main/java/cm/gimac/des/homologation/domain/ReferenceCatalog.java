package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Version du référentiel d'opérations d'un type de projet : graphe orienté
 * acyclique (RG-26). Une version publiée est figée ; sa publication exige un
 * approbateur distinct de l'auteur (algorithme 6.34, maker-checker).
 */
@Entity
@Table(name = "reference_catalog")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false)
    private ProjectType projectType;

    /**
     * Libellé de version, par exemple PM-2.1 ; unique tous types confondus.
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z]{2,4}-[0-9]+\\.[0-9]+$")
    @Column(name = "version", length = 20, nullable = false, unique = true)
    private String version;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @NotNull
    @Column(name = "published", nullable = false)
    private Boolean published;

    @NotNull
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Différentiel avec la version précédente (JSON : conservés, ajoutés, retirés, modifiés).
     */
    @Lob
    @Column(name = "diff_summary")
    private String diffSummary;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReferenceCatalog id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProjectType getProjectType() {
        return this.projectType;
    }

    public ReferenceCatalog projectType(ProjectType projectType) {
        this.setProjectType(projectType);
        return this;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public String getVersion() {
        return this.version;
    }

    public ReferenceCatalog version(String version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getEffectiveFrom() {
        return this.effectiveFrom;
    }

    public ReferenceCatalog effectiveFrom(LocalDate effectiveFrom) {
        this.setEffectiveFrom(effectiveFrom);
        return this;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Boolean getPublished() {
        return this.published;
    }

    public ReferenceCatalog published(Boolean published) {
        this.setPublished(published);
        return this;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public ReferenceCatalog authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public ReferenceCatalog approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public ReferenceCatalog publishedAt(Instant publishedAt) {
        this.setPublishedAt(publishedAt);
        return this;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getDiffSummary() {
        return this.diffSummary;
    }

    public ReferenceCatalog diffSummary(String diffSummary) {
        this.setDiffSummary(diffSummary);
        return this;
    }

    public void setDiffSummary(String diffSummary) {
        this.diffSummary = diffSummary;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceCatalog)) {
            return false;
        }
        return getId() != null && getId().equals(((ReferenceCatalog) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceCatalog{" +
            "id=" + getId() +
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
