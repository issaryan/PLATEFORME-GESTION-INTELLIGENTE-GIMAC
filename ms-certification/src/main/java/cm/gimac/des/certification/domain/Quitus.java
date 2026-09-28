package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.QuitusDecision;
import cm.gimac.des.certification.domain.enumeration.QuitusStatus;
import cm.gimac.des.certification.domain.enumeration.QuitusType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Quitus : décision motivée, signée, horodatée et scellée (algorithme 6.35).
 * Écart CC corrigé (D-22 prime) : la signature personnelle n'est pas calculée
 * dans le coffre de clés ; c'est l'assertion WebAuthn produite par
 * l'authentificateur FIDO2 du signataire sur le défi δq, vérifiée par MS-IAM.
 * La clé reste sur l'authentificateur ; signerCertRef devient l'identifiant de
 * la clé d'accès enregistrée.
 */
@Entity
@Table(name = "quitus")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Quitus implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private QuitusType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuitusStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private QuitusDecision decision;

    /**
     * Signataire (collaborateur MS-IAM), distinct pour chacun des quatre quitus (RG-73).
     */
    @Column(name = "signer_id")
    private UUID signerId;

    /**
     * Motif, obligatoire à la signature.
     */
    @Size(max = 2000)
    @Column(name = "reason", length = 2000)
    private String reason;

    /**
     * β du socle signé ; un quitus favorable reste valable tant que β est inchangé.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "basis_digest", length = 64, nullable = false)
    private String basisDigest;

    /**
     * δq : empreinte de la décision canonique, présentée comme défi WebAuthn.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "decision_digest", length = 64)
    private String decisionDigest;

    /**
     * Identifiant de la clé d'accès WebAuthn (base64url).
     */
    @Size(max = 1024)
    @Column(name = "credential_id", length = 1024)
    private String credentialId;

    /**
     * Données de l'authentificateur de l'assertion, brutes.
     */
    @Lob
    @Column(name = "authenticator_data")
    private byte[] authenticatorData;

    @Column(name = "authenticator_data_content_type")
    private String authenticatorDataContentType;

    /**
     * clientDataJSON de l'assertion, contenant le défi.
     */
    @Lob
    @Column(name = "client_data_json")
    private String clientDataJson;

    /**
     * Signature de l'assertion (σq).
     */
    @Lob
    @Column(name = "signature")
    private byte[] signature;

    @Column(name = "signature_content_type")
    private String signatureContentType;

    /**
     * Compteur de signature, strictement croissant.
     */
    @Min(value = 0L)
    @Column(name = "sign_count")
    private Long signCount;

    /**
     * Jeton RFC 3161 sur SHA256(δq || σq), encodé en DER.
     */
    @Lob
    @Column(name = "timestamp_token")
    private byte[] timestampToken;

    @Column(name = "timestamp_token_content_type")
    private String timestampTokenContentType;

    @Column(name = "signed_at")
    private Instant signedAt;

    /**
     * Échéance de signature, pour les relances de l'ordonnanceur.
     */
    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "changeRecord", "attestation", "commissioningReport", "quituses", "pieces" }, allowSetters = true)
    private CommissioningFile file;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Quitus id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public QuitusType getType() {
        return this.type;
    }

    public Quitus type(QuitusType type) {
        this.setType(type);
        return this;
    }

    public void setType(QuitusType type) {
        this.type = type;
    }

    public QuitusStatus getStatus() {
        return this.status;
    }

    public Quitus status(QuitusStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(QuitusStatus status) {
        this.status = status;
    }

    public QuitusDecision getDecision() {
        return this.decision;
    }

    public Quitus decision(QuitusDecision decision) {
        this.setDecision(decision);
        return this;
    }

    public void setDecision(QuitusDecision decision) {
        this.decision = decision;
    }

    public UUID getSignerId() {
        return this.signerId;
    }

    public Quitus signerId(UUID signerId) {
        this.setSignerId(signerId);
        return this;
    }

    public void setSignerId(UUID signerId) {
        this.signerId = signerId;
    }

    public String getReason() {
        return this.reason;
    }

    public Quitus reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBasisDigest() {
        return this.basisDigest;
    }

    public Quitus basisDigest(String basisDigest) {
        this.setBasisDigest(basisDigest);
        return this;
    }

    public void setBasisDigest(String basisDigest) {
        this.basisDigest = basisDigest;
    }

    public String getDecisionDigest() {
        return this.decisionDigest;
    }

    public Quitus decisionDigest(String decisionDigest) {
        this.setDecisionDigest(decisionDigest);
        return this;
    }

    public void setDecisionDigest(String decisionDigest) {
        this.decisionDigest = decisionDigest;
    }

    public String getCredentialId() {
        return this.credentialId;
    }

    public Quitus credentialId(String credentialId) {
        this.setCredentialId(credentialId);
        return this;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getAuthenticatorData() {
        return this.authenticatorData;
    }

    public Quitus authenticatorData(byte[] authenticatorData) {
        this.setAuthenticatorData(authenticatorData);
        return this;
    }

    public void setAuthenticatorData(byte[] authenticatorData) {
        this.authenticatorData = authenticatorData;
    }

    public String getAuthenticatorDataContentType() {
        return this.authenticatorDataContentType;
    }

    public Quitus authenticatorDataContentType(String authenticatorDataContentType) {
        this.authenticatorDataContentType = authenticatorDataContentType;
        return this;
    }

    public void setAuthenticatorDataContentType(String authenticatorDataContentType) {
        this.authenticatorDataContentType = authenticatorDataContentType;
    }

    public String getClientDataJson() {
        return this.clientDataJson;
    }

    public Quitus clientDataJson(String clientDataJson) {
        this.setClientDataJson(clientDataJson);
        return this;
    }

    public void setClientDataJson(String clientDataJson) {
        this.clientDataJson = clientDataJson;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public Quitus signature(byte[] signature) {
        this.setSignature(signature);
        return this;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getSignatureContentType() {
        return this.signatureContentType;
    }

    public Quitus signatureContentType(String signatureContentType) {
        this.signatureContentType = signatureContentType;
        return this;
    }

    public void setSignatureContentType(String signatureContentType) {
        this.signatureContentType = signatureContentType;
    }

    public Long getSignCount() {
        return this.signCount;
    }

    public Quitus signCount(Long signCount) {
        this.setSignCount(signCount);
        return this;
    }

    public void setSignCount(Long signCount) {
        this.signCount = signCount;
    }

    public byte[] getTimestampToken() {
        return this.timestampToken;
    }

    public Quitus timestampToken(byte[] timestampToken) {
        this.setTimestampToken(timestampToken);
        return this;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return this.timestampTokenContentType;
    }

    public Quitus timestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
        return this;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public Instant getSignedAt() {
        return this.signedAt;
    }

    public Quitus signedAt(Instant signedAt) {
        this.setSignedAt(signedAt);
        return this;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public Instant getDueAt() {
        return this.dueAt;
    }

    public Quitus dueAt(Instant dueAt) {
        this.setDueAt(dueAt);
        return this;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getInvalidatedAt() {
        return this.invalidatedAt;
    }

    public Quitus invalidatedAt(Instant invalidatedAt) {
        this.setInvalidatedAt(invalidatedAt);
        return this;
    }

    public void setInvalidatedAt(Instant invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

    public CommissioningFile getFile() {
        return this.file;
    }

    public void setFile(CommissioningFile commissioningFile) {
        this.file = commissioningFile;
    }

    public Quitus file(CommissioningFile commissioningFile) {
        this.setFile(commissioningFile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quitus)) {
            return false;
        }
        return getId() != null && getId().equals(((Quitus) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Quitus{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", status='" + getStatus() + "'" +
            ", decision='" + getDecision() + "'" +
            ", signerId='" + getSignerId() + "'" +
            ", reason='" + getReason() + "'" +
            ", basisDigest='" + getBasisDigest() + "'" +
            ", decisionDigest='" + getDecisionDigest() + "'" +
            ", credentialId='" + getCredentialId() + "'" +
            ", authenticatorData='" + getAuthenticatorData() + "'" +
            ", authenticatorDataContentType='" + getAuthenticatorDataContentType() + "'" +
            ", clientDataJson='" + getClientDataJson() + "'" +
            ", signature='" + getSignature() + "'" +
            ", signatureContentType='" + getSignatureContentType() + "'" +
            ", signCount=" + getSignCount() +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampTokenContentType='" + getTimestampTokenContentType() + "'" +
            ", signedAt='" + getSignedAt() + "'" +
            ", dueAt='" + getDueAt() + "'" +
            ", invalidatedAt='" + getInvalidatedAt() + "'" +
            "}";
    }
}
