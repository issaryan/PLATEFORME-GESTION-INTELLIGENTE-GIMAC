package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.ReportState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.ShiftReport} entity.
 */
@Schema(
    description = "Rapport de quart (algorithme 6.46). Jamais modifié une fois scellé : une\ncorrection produit une nouvelle version visée par le chef de quart."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftReportDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Schema(description = "Référence lisible, par exemple RQ-2026-0927-N.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reference;

    @Schema(description = "Forme canonique J (JCS, RFC 8785) du contenu figé.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String content;

    @Schema(description = "Consignes annotées par l'opérateur sortant.")
    @Lob
    private String instructions;

    @NotNull
    private ReportState state;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Tête de chaîne de la main courante reprise dans le rapport.")
    private String logHeadHash;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "H1 = SHA-256(J), défi de la signature sortante.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentHash;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Hf = SHA-256(J || σs || σe || σc).")
    private String digest;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "D : empreinte du PDF/A-3 déposé.")
    private String documentDigest;

    @Schema(description = "Jeton RFC 3161 encodé en DER.")
    @Lob
    private byte[] timestampToken;

    private String timestampTokenContentType;

    @Size(max = 100)
    private String timestampSerialNumber;

    @Size(max = 100)
    @Schema(description = "Référence du document déposé dans MS-DOCUMENT, par exemple DOC-2026-11902.")
    private String documentRef;

    @Schema(description = "Transfert de responsabilité de la salle : instant de la contresignature.")
    private Instant responsibilityTransferredAt;

    private Instant sealedAt;

    @NotNull
    @Schema(description = "Un rapport par vacation.")
    private ShiftDTO shift;

    @Schema(description = "Nouvelle version d'un rapport scellé, remplaçant la précédente.")
    private ShiftReportDTO supersedes;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public ReportState getState() {
        return state;
    }

    public void setState(ReportState state) {
        this.state = state;
    }

    public String getLogHeadHash() {
        return logHeadHash;
    }

    public void setLogHeadHash(String logHeadHash) {
        this.logHeadHash = logHeadHash;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getDocumentDigest() {
        return documentDigest;
    }

    public void setDocumentDigest(String documentDigest) {
        this.documentDigest = documentDigest;
    }

    public byte[] getTimestampToken() {
        return timestampToken;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return timestampTokenContentType;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public String getTimestampSerialNumber() {
        return timestampSerialNumber;
    }

    public void setTimestampSerialNumber(String timestampSerialNumber) {
        this.timestampSerialNumber = timestampSerialNumber;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public Instant getResponsibilityTransferredAt() {
        return responsibilityTransferredAt;
    }

    public void setResponsibilityTransferredAt(Instant responsibilityTransferredAt) {
        this.responsibilityTransferredAt = responsibilityTransferredAt;
    }

    public Instant getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public ShiftDTO getShift() {
        return shift;
    }

    public void setShift(ShiftDTO shift) {
        this.shift = shift;
    }

    public ShiftReportDTO getSupersedes() {
        return supersedes;
    }

    public void setSupersedes(ShiftReportDTO supersedes) {
        this.supersedes = supersedes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShiftReportDTO)) {
            return false;
        }

        ShiftReportDTO shiftReportDTO = (ShiftReportDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, shiftReportDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftReportDTO{" +
            "id='" + getId() + "'" +
            ", reference='" + getReference() + "'" +
            ", content='" + getContent() + "'" +
            ", instructions='" + getInstructions() + "'" +
            ", state='" + getState() + "'" +
            ", logHeadHash='" + getLogHeadHash() + "'" +
            ", contentHash='" + getContentHash() + "'" +
            ", digest='" + getDigest() + "'" +
            ", documentDigest='" + getDocumentDigest() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampSerialNumber='" + getTimestampSerialNumber() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", responsibilityTransferredAt='" + getResponsibilityTransferredAt() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", shift=" + getShift() +
            ", supersedes=" + getSupersedes() +
            "}";
    }
}
