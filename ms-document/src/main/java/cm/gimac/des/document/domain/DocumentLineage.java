package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.DocumentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Lignée : versions successives d'une même pièce logique (algorithme 6.18).
 * La version courante est résolue par currentVersion et currentDocument.
 */
@Entity
@Table(name = "document_lineage")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentLineage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DocumentType type;

    /**
     * Service propriétaire, par exemple MS-MONITORING ; vide pour un dépôt utilisateur.
     */
    @Size(max = 50)
    @Column(name = "owner_service", length = 50)
    private String ownerService;

    /**
     * Utilisateur propriétaire, pour un dépôt depuis une couche de présentation.
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * Institution concernée (MS-PARTICIPANT), pour le cloisonnement RLS.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Min(value = 0)
    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Version courante de la lignée (résolution déterministe, algorithme 6.18).
     */
    @JsonIgnoreProperties(value = { "lineage", "policy", "documentLineage" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private Document currentDocument;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DocumentLineage id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentType getType() {
        return this.type;
    }

    public DocumentLineage type(DocumentType type) {
        this.setType(type);
        return this;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getOwnerService() {
        return this.ownerService;
    }

    public DocumentLineage ownerService(String ownerService) {
        this.setOwnerService(ownerService);
        return this;
    }

    public void setOwnerService(String ownerService) {
        this.ownerService = ownerService;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public DocumentLineage ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public DocumentLineage institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public Integer getCurrentVersion() {
        return this.currentVersion;
    }

    public DocumentLineage currentVersion(Integer currentVersion) {
        this.setCurrentVersion(currentVersion);
        return this;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public DocumentLineage createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Document getCurrentDocument() {
        return this.currentDocument;
    }

    public void setCurrentDocument(Document document) {
        this.currentDocument = document;
    }

    public DocumentLineage currentDocument(Document document) {
        this.setCurrentDocument(document);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentLineage)) {
            return false;
        }
        return getId() != null && getId().equals(((DocumentLineage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentLineage{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", ownerService='" + getOwnerService() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", currentVersion=" + getCurrentVersion() +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
