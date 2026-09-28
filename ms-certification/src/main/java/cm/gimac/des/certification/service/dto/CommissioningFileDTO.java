package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.FileState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.CommissioningFile} entity.
 */
@Schema(
    description = "Dossier de mise en service d'une liaison, ouvert à la réception de\nhomologation.liaison-ready-for-certification.v1. Transitions autorisées :\nOUVERT vers QUITUS_EN_COURS ; QUITUS_EN_COURS vers EN_CORRECTION ou CERTIFIE ;\nEN_CORRECTION vers QUITUS_EN_COURS ; CERTIFIE vers MISE_EN_SERVICE_AUTORISEE\n(terminal). Un seul dossier par liaison."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CommissioningFileDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^DMS-[0-9]{4}-[0-9]{4,}$")
    @Schema(description = "Identifiant lisible, par exemple DMS-2026-0009.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileCode;

    @NotNull
    @Schema(description = "Liaison tenue par MS-HOMOLOGATION (référence, BNF-04).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID liaisonId;

    @NotNull
    @Schema(description = "Institution tenue par MS-PARTICIPANT (référence, BNF-04).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID institutionId;

    @NotNull
    @Schema(
        description = "Chef de projet de la liaison, exclu des signataires (algorithme 6.35, étape 3).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID projectLeadId;

    @NotNull
    private FileState state;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "β = SHA256(JCS(socle d'évaluation)), RFC 8785.")
    private String basisDigest;

    @Schema(description = "Jeton RFC 3161 du socle, encodé en DER (algorithme 6.37).")
    @Lob
    private byte[] basisTimestampToken;

    private String basisTimestampTokenContentType;

    private Instant basisSealedAt;

    @NotNull
    private Instant openedAt;

    @Size(max = 1000)
    @Schema(description = "Motif du dernier retour en homologation.")
    private String correctionReason;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Empreinte du manifeste MF (algorithme 6.36, étape 5).")
    private String manifestDigest;

    @Schema(description = "Jeton RFC 3161 du manifeste, encodé en DER.")
    @Lob
    private byte[] timestampToken;

    private String timestampTokenContentType;

    private Instant sealedAt;

    private Instant goLiveAuthorizedAt;

    @Schema(description = "Fiche de changement lue, au plus une par dossier.")
    private ChangeRecordDTO changeRecord;

    @Schema(description = "Attestation émise, au plus une par dossier.")
    private CertificationAttestationDTO attestation;

    @Schema(description = "Procès-verbal de mise en service, parmi les pièces du dossier (condition C6).")
    private EvidenceItemDTO commissioningReport;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileCode() {
        return fileCode;
    }

    public void setFileCode(String fileCode) {
        this.fileCode = fileCode;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public UUID getProjectLeadId() {
        return projectLeadId;
    }

    public void setProjectLeadId(UUID projectLeadId) {
        this.projectLeadId = projectLeadId;
    }

    public FileState getState() {
        return state;
    }

    public void setState(FileState state) {
        this.state = state;
    }

    public String getBasisDigest() {
        return basisDigest;
    }

    public void setBasisDigest(String basisDigest) {
        this.basisDigest = basisDigest;
    }

    public byte[] getBasisTimestampToken() {
        return basisTimestampToken;
    }

    public void setBasisTimestampToken(byte[] basisTimestampToken) {
        this.basisTimestampToken = basisTimestampToken;
    }

    public String getBasisTimestampTokenContentType() {
        return basisTimestampTokenContentType;
    }

    public void setBasisTimestampTokenContentType(String basisTimestampTokenContentType) {
        this.basisTimestampTokenContentType = basisTimestampTokenContentType;
    }

    public Instant getBasisSealedAt() {
        return basisSealedAt;
    }

    public void setBasisSealedAt(Instant basisSealedAt) {
        this.basisSealedAt = basisSealedAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }

    public String getManifestDigest() {
        return manifestDigest;
    }

    public void setManifestDigest(String manifestDigest) {
        this.manifestDigest = manifestDigest;
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

    public Instant getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Instant getGoLiveAuthorizedAt() {
        return goLiveAuthorizedAt;
    }

    public void setGoLiveAuthorizedAt(Instant goLiveAuthorizedAt) {
        this.goLiveAuthorizedAt = goLiveAuthorizedAt;
    }

    public ChangeRecordDTO getChangeRecord() {
        return changeRecord;
    }

    public void setChangeRecord(ChangeRecordDTO changeRecord) {
        this.changeRecord = changeRecord;
    }

    public CertificationAttestationDTO getAttestation() {
        return attestation;
    }

    public void setAttestation(CertificationAttestationDTO attestation) {
        this.attestation = attestation;
    }

    public EvidenceItemDTO getCommissioningReport() {
        return commissioningReport;
    }

    public void setCommissioningReport(EvidenceItemDTO commissioningReport) {
        this.commissioningReport = commissioningReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommissioningFileDTO)) {
            return false;
        }

        CommissioningFileDTO commissioningFileDTO = (CommissioningFileDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, commissioningFileDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CommissioningFileDTO{" +
            "id='" + getId() + "'" +
            ", fileCode='" + getFileCode() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", projectLeadId='" + getProjectLeadId() + "'" +
            ", state='" + getState() + "'" +
            ", basisDigest='" + getBasisDigest() + "'" +
            ", basisTimestampToken='" + getBasisTimestampToken() + "'" +
            ", basisSealedAt='" + getBasisSealedAt() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", correctionReason='" + getCorrectionReason() + "'" +
            ", manifestDigest='" + getManifestDigest() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", goLiveAuthorizedAt='" + getGoLiveAuthorizedAt() + "'" +
            ", changeRecord=" + getChangeRecord() +
            ", attestation=" + getAttestation() +
            ", commissioningReport=" + getCommissioningReport() +
            "}";
    }
}
