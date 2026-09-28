package cm.gimac.des.certification.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.CertificationAttestation} entity.
 */
@Schema(
    description = "Attestation de certification PDF/A-3 signée PAdES B-LTA avec la clé de cachet\nde la plateforme, déposée en WORM dans MS-DOCUMENT (algorithme 6.38)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CertificationAttestationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^ATT-CERT-[0-9]{4}-[0-9]{4,}$")
    @Schema(description = "Numéro d'attestation, par exemple ATT-CERT-2026-0009.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String attestationNumber;

    @NotNull
    @Schema(description = "Pièce dans MS-DOCUMENT (référence, BNF-04).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID documentId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String pdfDigest;

    @NotNull
    @Size(max = 20)
    @Schema(description = "Niveau de signature, PAdES B-LTA.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signatureLevel;

    @Schema(
        description = "Pièces jointes embarquées : liste JSON (nom, type de média, relation, empreinte).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String attachments;

    @NotNull
    private Instant issuedAt;

    @NotNull
    @Schema(description = "Confirmation reçue par document.document-sealed.v1 (condition C3).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sealConfirmed;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAttestationNumber() {
        return attestationNumber;
    }

    public void setAttestationNumber(String attestationNumber) {
        this.attestationNumber = attestationNumber;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getPdfDigest() {
        return pdfDigest;
    }

    public void setPdfDigest(String pdfDigest) {
        this.pdfDigest = pdfDigest;
    }

    public String getSignatureLevel() {
        return signatureLevel;
    }

    public void setSignatureLevel(String signatureLevel) {
        this.signatureLevel = signatureLevel;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getSealConfirmed() {
        return sealConfirmed;
    }

    public void setSealConfirmed(Boolean sealConfirmed) {
        this.sealConfirmed = sealConfirmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationAttestationDTO)) {
            return false;
        }

        CertificationAttestationDTO certificationAttestationDTO = (CertificationAttestationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, certificationAttestationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CertificationAttestationDTO{" +
            "id='" + getId() + "'" +
            ", attestationNumber='" + getAttestationNumber() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", pdfDigest='" + getPdfDigest() + "'" +
            ", signatureLevel='" + getSignatureLevel() + "'" +
            ", attachments='" + getAttachments() + "'" +
            ", issuedAt='" + getIssuedAt() + "'" +
            ", sealConfirmed='" + getSealConfirmed() + "'" +
            "}";
    }
}
