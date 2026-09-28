package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.OperationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.Operation} entity.
 */
@Schema(
    description = "Opération instanciée d'une liaison (algorithme 6.30), close par la règle des\nquatre éléments (D-09, RG-32, algorithme 6.31) : responsable, preuve WORM\n(documentId + empreinte), horodatage RFC 3161, validateur distinct du\nresponsable. Une clôture est immuable (RG-33)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^OPE-[0-9]{4}-[0-9]{4,}$")
    @Schema(description = "Identifiant lisible, par exemple OPE-2026-0312.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operationCode;

    @NotNull
    private OperationState state;

    @Schema(description = "Élément 1 : responsable (collaborateur MS-IAM ou représentant du participant).")
    private UUID ownerId;

    @Schema(
        description = "Élément 2 : preuve scellée dans MS-DOCUMENT. Écart CC corrigé : pour une\nopération d'essai, la référence de trace ne remplace pas la preuve WORM ;\nl'archive probante qui la contient est déposée et scellée (UC-HOM-03, D-09)."
    )
    private UUID evidenceDocumentId;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String evidenceDigest;

    private Instant evidenceSealedAt;

    @Schema(description = "Rétention WORM constatée, au moins 10 ans (D-11).")
    private Duration evidenceRetention;

    @Schema(description = "Confirmation reçue par document.document-sealed.v1.")
    private Boolean evidenceSealConfirmed;

    @Size(max = 100)
    @Schema(description = "Référence de trace du simulateur, pour une opération d'essai (RG-30).")
    private String evidenceTraceRef;

    @Schema(description = "Élément 3 : jeton RFC 3161 encodé en DER, sur SHA256(o.id || hp || r).")
    @Lob
    private byte[] timestampToken;

    private String timestampTokenContentType;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String timestampImprint;

    private Instant timestampGenTime;

    @Schema(description = "Élément 4 : validateur habilité, distinct du responsable.")
    private UUID validatorId;

    private Instant validatedAt;

    private Instant closedAt;

    @Size(max = 1000)
    @Schema(description = "Motif d'arbitrage, de réouverture (référentiel modifié, quitus refusé) ou de verrouillage.")
    private String stateReason;

    @NotNull
    private LiaisonDTO liaison;

    @NotNull
    private ReferenceOperationDTO reference;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public OperationState getState() {
        return state;
    }

    public void setState(OperationState state) {
        this.state = state;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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

    public Instant getEvidenceSealedAt() {
        return evidenceSealedAt;
    }

    public void setEvidenceSealedAt(Instant evidenceSealedAt) {
        this.evidenceSealedAt = evidenceSealedAt;
    }

    public Duration getEvidenceRetention() {
        return evidenceRetention;
    }

    public void setEvidenceRetention(Duration evidenceRetention) {
        this.evidenceRetention = evidenceRetention;
    }

    public Boolean getEvidenceSealConfirmed() {
        return evidenceSealConfirmed;
    }

    public void setEvidenceSealConfirmed(Boolean evidenceSealConfirmed) {
        this.evidenceSealConfirmed = evidenceSealConfirmed;
    }

    public String getEvidenceTraceRef() {
        return evidenceTraceRef;
    }

    public void setEvidenceTraceRef(String evidenceTraceRef) {
        this.evidenceTraceRef = evidenceTraceRef;
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

    public String getTimestampImprint() {
        return timestampImprint;
    }

    public void setTimestampImprint(String timestampImprint) {
        this.timestampImprint = timestampImprint;
    }

    public Instant getTimestampGenTime() {
        return timestampGenTime;
    }

    public void setTimestampGenTime(Instant timestampGenTime) {
        this.timestampGenTime = timestampGenTime;
    }

    public UUID getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getStateReason() {
        return stateReason;
    }

    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    public ReferenceOperationDTO getReference() {
        return reference;
    }

    public void setReference(ReferenceOperationDTO reference) {
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationDTO)) {
            return false;
        }

        OperationDTO operationDTO = (OperationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, operationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperationDTO{" +
            "id='" + getId() + "'" +
            ", operationCode='" + getOperationCode() + "'" +
            ", state='" + getState() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", evidenceDocumentId='" + getEvidenceDocumentId() + "'" +
            ", evidenceDigest='" + getEvidenceDigest() + "'" +
            ", evidenceSealedAt='" + getEvidenceSealedAt() + "'" +
            ", evidenceRetention='" + getEvidenceRetention() + "'" +
            ", evidenceSealConfirmed='" + getEvidenceSealConfirmed() + "'" +
            ", evidenceTraceRef='" + getEvidenceTraceRef() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampImprint='" + getTimestampImprint() + "'" +
            ", timestampGenTime='" + getTimestampGenTime() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", stateReason='" + getStateReason() + "'" +
            ", liaison=" + getLiaison() +
            ", reference=" + getReference() +
            "}";
    }
}
