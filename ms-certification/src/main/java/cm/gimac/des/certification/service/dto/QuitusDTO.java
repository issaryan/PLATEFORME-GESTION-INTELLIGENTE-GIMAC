package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.QuitusDecision;
import cm.gimac.des.certification.domain.enumeration.QuitusStatus;
import cm.gimac.des.certification.domain.enumeration.QuitusType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.Quitus} entity.
 */
@Schema(
    description = "Quitus : décision motivée, signée, horodatée et scellée (algorithme 6.35).\nÉcart CC corrigé (D-22 prime) : la signature personnelle n'est pas calculée\ndans le coffre de clés ; c'est l'assertion WebAuthn produite par\nl'authentificateur FIDO2 du signataire sur le défi δq, vérifiée par MS-IAM.\nLa clé reste sur l'authentificateur ; signerCertRef devient l'identifiant de\nla clé d'accès enregistrée."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QuitusDTO implements Serializable {

    private UUID id;

    @NotNull
    private QuitusType type;

    @NotNull
    private QuitusStatus status;

    private QuitusDecision decision;

    @Schema(description = "Signataire (collaborateur MS-IAM), distinct pour chacun des quatre quitus (RG-73).")
    private UUID signerId;

    @Size(max = 2000)
    @Schema(description = "Motif, obligatoire à la signature.")
    private String reason;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(
        description = "β du socle signé ; un quitus favorable reste valable tant que β est inchangé.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String basisDigest;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "δq : empreinte de la décision canonique, présentée comme défi WebAuthn.")
    private String decisionDigest;

    @Size(max = 1024)
    @Schema(description = "Identifiant de la clé d'accès WebAuthn (base64url).")
    private String credentialId;

    @Schema(description = "Données de l'authentificateur de l'assertion, brutes.")
    @Lob
    private byte[] authenticatorData;

    private String authenticatorDataContentType;

    @Schema(description = "clientDataJSON de l'assertion, contenant le défi.")
    @Lob
    private String clientDataJson;

    @Schema(description = "Signature de l'assertion (σq).")
    @Lob
    private byte[] signature;

    private String signatureContentType;

    @Min(value = 0L)
    @Schema(description = "Compteur de signature, strictement croissant.")
    private Long signCount;

    @Schema(description = "Jeton RFC 3161 sur SHA256(δq || σq), encodé en DER.")
    @Lob
    private byte[] timestampToken;

    private String timestampTokenContentType;

    private Instant signedAt;

    @Schema(description = "Échéance de signature, pour les relances de l'ordonnanceur.")
    private Instant dueAt;

    private Instant invalidatedAt;

    @NotNull
    private CommissioningFileDTO file;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public QuitusType getType() {
        return type;
    }

    public void setType(QuitusType type) {
        this.type = type;
    }

    public QuitusStatus getStatus() {
        return status;
    }

    public void setStatus(QuitusStatus status) {
        this.status = status;
    }

    public QuitusDecision getDecision() {
        return decision;
    }

    public void setDecision(QuitusDecision decision) {
        this.decision = decision;
    }

    public UUID getSignerId() {
        return signerId;
    }

    public void setSignerId(UUID signerId) {
        this.signerId = signerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBasisDigest() {
        return basisDigest;
    }

    public void setBasisDigest(String basisDigest) {
        this.basisDigest = basisDigest;
    }

    public String getDecisionDigest() {
        return decisionDigest;
    }

    public void setDecisionDigest(String decisionDigest) {
        this.decisionDigest = decisionDigest;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getAuthenticatorData() {
        return authenticatorData;
    }

    public void setAuthenticatorData(byte[] authenticatorData) {
        this.authenticatorData = authenticatorData;
    }

    public String getAuthenticatorDataContentType() {
        return authenticatorDataContentType;
    }

    public void setAuthenticatorDataContentType(String authenticatorDataContentType) {
        this.authenticatorDataContentType = authenticatorDataContentType;
    }

    public String getClientDataJson() {
        return clientDataJson;
    }

    public void setClientDataJson(String clientDataJson) {
        this.clientDataJson = clientDataJson;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getSignatureContentType() {
        return signatureContentType;
    }

    public void setSignatureContentType(String signatureContentType) {
        this.signatureContentType = signatureContentType;
    }

    public Long getSignCount() {
        return signCount;
    }

    public void setSignCount(Long signCount) {
        this.signCount = signCount;
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

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Instant invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
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
        if (!(o instanceof QuitusDTO)) {
            return false;
        }

        QuitusDTO quitusDTO = (QuitusDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, quitusDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QuitusDTO{" +
            "id='" + getId() + "'" +
            ", type='" + getType() + "'" +
            ", status='" + getStatus() + "'" +
            ", decision='" + getDecision() + "'" +
            ", signerId='" + getSignerId() + "'" +
            ", reason='" + getReason() + "'" +
            ", basisDigest='" + getBasisDigest() + "'" +
            ", decisionDigest='" + getDecisionDigest() + "'" +
            ", credentialId='" + getCredentialId() + "'" +
            ", authenticatorData='" + getAuthenticatorData() + "'" +
            ", clientDataJson='" + getClientDataJson() + "'" +
            ", signature='" + getSignature() + "'" +
            ", signCount=" + getSignCount() +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", signedAt='" + getSignedAt() + "'" +
            ", dueAt='" + getDueAt() + "'" +
            ", invalidatedAt='" + getInvalidatedAt() + "'" +
            ", file=" + getFile() +
            "}";
    }
}
