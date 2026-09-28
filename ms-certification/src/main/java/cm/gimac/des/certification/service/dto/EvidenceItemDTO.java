package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.EvidenceKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.EvidenceItem} entity.
 */
@Schema(
    description = "Pièce du dossier, référencée par identifiant et empreinte ; l'empreinte est\nrecontrôlée contre MS-DOCUMENT avant autorisation (algorithme 6.36, étape 4)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceItemDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID documentId;

    @NotNull
    private EvidenceKind kind;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String digest;

    private Instant sealedAt;

    @NotNull
    @Schema(description = "Confirmation reçue par document.document-sealed.v1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sealConfirmed;

    private Instant lastVerifiedAt;

    @NotNull
    private UUID attachedBy;

    @NotNull
    private Instant attachedAt;

    @NotNull
    private CommissioningFileDTO file;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public EvidenceKind getKind() {
        return kind;
    }

    public void setKind(EvidenceKind kind) {
        this.kind = kind;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Instant getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Boolean getSealConfirmed() {
        return sealConfirmed;
    }

    public void setSealConfirmed(Boolean sealConfirmed) {
        this.sealConfirmed = sealConfirmed;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public UUID getAttachedBy() {
        return attachedBy;
    }

    public void setAttachedBy(UUID attachedBy) {
        this.attachedBy = attachedBy;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    public void setAttachedAt(Instant attachedAt) {
        this.attachedAt = attachedAt;
    }

    public CommissioningFileDTO getFile() {
        return file;
    }

    public void setFile(CommissioningFileDTO file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvidenceItemDTO)) {
            return false;
        }

        EvidenceItemDTO evidenceItemDTO = (EvidenceItemDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, evidenceItemDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceItemDTO{" +
            "id='" + getId() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", kind='" + getKind() + "'" +
            ", digest='" + getDigest() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", sealConfirmed='" + getSealConfirmed() + "'" +
            ", lastVerifiedAt='" + getLastVerifiedAt() + "'" +
            ", attachedBy='" + getAttachedBy() + "'" +
            ", attachedAt='" + getAttachedAt() + "'" +
            ", file=" + getFile() +
            "}";
    }
}
