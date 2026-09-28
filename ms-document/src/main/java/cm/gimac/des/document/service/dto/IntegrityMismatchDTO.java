package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.MismatchKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.IntegrityMismatch} entity.
 */
@Schema(description = "Anomalie consignée par un contrôle d'intégrité, avec les deux empreintes.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrityMismatchDTO implements Serializable {

    private UUID id;

    @NotNull
    private MismatchKind kind;

    @NotNull
    @Size(max = 255)
    private String objectKey;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String expectedSha256;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Empreinte recalculée ; absente pour un objet illisible.")
    private String actualSha256;

    @NotNull
    private Instant detectedAt;

    @NotNull
    private IntegrityCheckDTO integrityCheck;

    @NotNull
    private DocumentDTO document;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MismatchKind getKind() {
        return kind;
    }

    public void setKind(MismatchKind kind) {
        this.kind = kind;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public void setExpectedSha256(String expectedSha256) {
        this.expectedSha256 = expectedSha256;
    }

    public String getActualSha256() {
        return actualSha256;
    }

    public void setActualSha256(String actualSha256) {
        this.actualSha256 = actualSha256;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public IntegrityCheckDTO getIntegrityCheck() {
        return integrityCheck;
    }

    public void setIntegrityCheck(IntegrityCheckDTO integrityCheck) {
        this.integrityCheck = integrityCheck;
    }

    public DocumentDTO getDocument() {
        return document;
    }

    public void setDocument(DocumentDTO document) {
        this.document = document;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrityMismatchDTO)) {
            return false;
        }

        IntegrityMismatchDTO integrityMismatchDTO = (IntegrityMismatchDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, integrityMismatchDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrityMismatchDTO{" +
            "id='" + getId() + "'" +
            ", kind='" + getKind() + "'" +
            ", objectKey='" + getObjectKey() + "'" +
            ", expectedSha256='" + getExpectedSha256() + "'" +
            ", actualSha256='" + getActualSha256() + "'" +
            ", detectedAt='" + getDetectedAt() + "'" +
            ", integrityCheck=" + getIntegrityCheck() +
            ", document=" + getDocument() +
            "}";
    }
}
