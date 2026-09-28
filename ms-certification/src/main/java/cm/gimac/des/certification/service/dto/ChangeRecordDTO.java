package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.ChangeApproval;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.ChangeRecord} entity.
 */
@Schema(
    description = "Fiche de changement lue, jamais écrite (RG-34) : l'ingénieur d'homologation\nen saisit la référence ; l'état est relu en mTLS dans le dispositif de\ngestion des changements, à nouveau au-delà de 15 min (algorithme 6.36)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChangeRecordDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Référence dans le dispositif, par exemple CHG-2026-0412.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String externalRef;

    @NotNull
    private ChangeApproval approval;

    private Instant approvedAt;

    private Instant windowStart;

    private Instant windowEnd;

    @NotNull
    private Instant readAt;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentDigest;

    @NotNull
    @Schema(description = "Auteur de la saisie de la référence (collaborateur MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID referencedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public ChangeApproval getApproval() {
        return approval;
    }

    public void setApproval(ChangeApproval approval) {
        this.approval = approval;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public UUID getReferencedBy() {
        return referencedBy;
    }

    public void setReferencedBy(UUID referencedBy) {
        this.referencedBy = referencedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeRecordDTO)) {
            return false;
        }

        ChangeRecordDTO changeRecordDTO = (ChangeRecordDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, changeRecordDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChangeRecordDTO{" +
            "id='" + getId() + "'" +
            ", externalRef='" + getExternalRef() + "'" +
            ", approval='" + getApproval() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", windowStart='" + getWindowStart() + "'" +
            ", windowEnd='" + getWindowEnd() + "'" +
            ", readAt='" + getReadAt() + "'" +
            ", contentDigest='" + getContentDigest() + "'" +
            ", referencedBy='" + getReferencedBy() + "'" +
            "}";
    }
}
