package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.DocumentLineage} entity.
 */
@Schema(
    description = "Lignée : versions successives d'une même pièce logique (algorithme 6.18).\nLa version courante est résolue par currentVersion et currentDocument."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentLineageDTO implements Serializable {

    private UUID id;

    @NotNull
    private DocumentType type;

    @Size(max = 50)
    @Schema(description = "Service propriétaire, par exemple MS-MONITORING ; vide pour un dépôt utilisateur.")
    private String ownerService;

    @Schema(description = "Utilisateur propriétaire, pour un dépôt depuis une couche de présentation.")
    private UUID ownerId;

    @Schema(description = "Institution concernée (MS-PARTICIPANT), pour le cloisonnement RLS.")
    private UUID institutionId;

    @NotNull
    @Min(value = 0)
    private Integer currentVersion;

    @NotNull
    private Instant createdAt;

    @Schema(description = "Version courante de la lignée (résolution déterministe, algorithme 6.18).")
    private DocumentDTO currentDocument;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getOwnerService() {
        return ownerService;
    }

    public void setOwnerService(String ownerService) {
        this.ownerService = ownerService;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public DocumentDTO getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(DocumentDTO currentDocument) {
        this.currentDocument = currentDocument;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentLineageDTO)) {
            return false;
        }

        DocumentLineageDTO documentLineageDTO = (DocumentLineageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, documentLineageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentLineageDTO{" +
            "id='" + getId() + "'" +
            ", type='" + getType() + "'" +
            ", ownerService='" + getOwnerService() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", currentVersion=" + getCurrentVersion() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", currentDocument=" + getCurrentDocument() +
            "}";
    }
}
