package cm.gimac.des.certification.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.ClosedOperationSnapshot} entity.
 */
@Schema(
    description = "Projection d'une opération close, depuis homologation.operation-closed.v1 :\nelle entre dans le socle d'évaluation β et permet d'exclure des signataires\nles responsables d'opérations (algorithme 6.35, étape 3). Écart CC corrigé :\nprojection décrite au CC §6.1.8.1 sans classe dédiée."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClosedOperationSnapshotDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID liaisonId;

    @NotNull
    @Schema(description = "Opération tenue par MS-HOMOLOGATION (référence, BNF-04).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID operationId;

    @NotNull
    @Size(max = 20)
    private String operationCode;

    @NotNull
    @Size(max = 20)
    private String referenceCode;

    @Size(max = 80)
    private String serviceCode;

    @NotNull
    private UUID ownerId;

    @NotNull
    private UUID validatorId;

    @NotNull
    private UUID evidenceDocumentId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String evidenceDigest;

    @NotNull
    private Instant closedAt;

    @NotNull
    private UUID sourceEventId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public UUID getEvidenceDocumentId() {
        return evidenceDocumentId;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public String getEvidenceDigest() {
        return evidenceDigest;
    }

    public void setEvidenceDigest(String evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClosedOperationSnapshotDTO)) {
            return false;
        }

        ClosedOperationSnapshotDTO closedOperationSnapshotDTO = (ClosedOperationSnapshotDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, closedOperationSnapshotDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ClosedOperationSnapshotDTO{" +
            "id='" + getId() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", operationId='" + getOperationId() + "'" +
            ", operationCode='" + getOperationCode() + "'" +
            ", referenceCode='" + getReferenceCode() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", evidenceDocumentId='" + getEvidenceDocumentId() + "'" +
            ", evidenceDigest='" + getEvidenceDigest() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            "}";
    }
}
