package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.OperationState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Opération instanciée d'une liaison (algorithme 6.30), close par la règle des
 * quatre éléments (D-09, RG-32, algorithme 6.31) : responsable, preuve WORM
 * (documentId + empreinte), horodatage RFC 3161, validateur distinct du
 * responsable. Une clôture est immuable (RG-33).
 */
@Entity
@Table(name = "operation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant lisible, par exemple OPE-2026-0312.
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^OPE-[0-9]{4}-[0-9]{4,}$")
    @Column(name = "operation_code", length = 20, nullable = false, unique = true)
    private String operationCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private OperationState state;

    /**
     * Élément 1 : responsable (collaborateur MS-IAM ou représentant du participant).
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * Élément 2 : preuve scellée dans MS-DOCUMENT. Écart CC corrigé : pour une
     * opération d'essai, la référence de trace ne remplace pas la preuve WORM ;
     * l'archive probante qui la contient est déposée et scellée (UC-HOM-03, D-09).
     */
    @Column(name = "evidence_document_id")
    private UUID evidenceDocumentId;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "evidence_digest", length = 64)
    private String evidenceDigest;

    @Column(name = "evidence_sealed_at")
    private Instant evidenceSealedAt;

    /**
     * Rétention WORM constatée, au moins 10 ans (D-11).
     */
    @Column(name = "evidence_retention")
    private Duration evidenceRetention;

    /**
     * Confirmation reçue par document.document-sealed.v1.
     */
    @Column(name = "evidence_seal_confirmed")
    private Boolean evidenceSealConfirmed;

    /**
     * Référence de trace du simulateur, pour une opération d'essai (RG-30).
     */
    @Size(max = 100)
    @Column(name = "evidence_trace_ref", length = 100)
    private String evidenceTraceRef;

    /**
     * Élément 3 : jeton RFC 3161 encodé en DER, sur SHA256(o.id || hp || r).
     */
    @Lob
    @Column(name = "timestamp_token")
    private byte[] timestampToken;

    @Column(name = "timestamp_token_content_type")
    private String timestampTokenContentType;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "timestamp_imprint", length = 64)
    private String timestampImprint;

    @Column(name = "timestamp_gen_time")
    private Instant timestampGenTime;

    /**
     * Élément 4 : validateur habilité, distinct du responsable.
     */
    @Column(name = "validator_id")
    private UUID validatorId;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Motif d'arbitrage, de réouverture (référentiel modifié, quitus refusé) ou de verrouillage.
     */
    @Size(max = 1000)
    @Column(name = "state_reason", length = 1000)
    private String stateReason;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "catalog", "campaignSync" }, allowSetters = true)
    private Liaison liaison;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "catalog", "predecessors", "successors" }, allowSetters = true)
    private ReferenceOperation reference;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Operation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOperationCode() {
        return this.operationCode;
    }

    public Operation operationCode(String operationCode) {
        this.setOperationCode(operationCode);
        return this;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public OperationState getState() {
        return this.state;
    }

    public Operation state(OperationState state) {
        this.setState(state);
        return this;
    }

    public void setState(OperationState state) {
        this.state = state;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public Operation ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getEvidenceDocumentId() {
        return this.evidenceDocumentId;
    }

    public Operation evidenceDocumentId(UUID evidenceDocumentId) {
        this.setEvidenceDocumentId(evidenceDocumentId);
        return this;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public String getEvidenceDigest() {
        return this.evidenceDigest;
    }

    public Operation evidenceDigest(String evidenceDigest) {
        this.setEvidenceDigest(evidenceDigest);
        return this;
    }

    public void setEvidenceDigest(String evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public Instant getEvidenceSealedAt() {
        return this.evidenceSealedAt;
    }

    public Operation evidenceSealedAt(Instant evidenceSealedAt) {
        this.setEvidenceSealedAt(evidenceSealedAt);
        return this;
    }

    public void setEvidenceSealedAt(Instant evidenceSealedAt) {
        this.evidenceSealedAt = evidenceSealedAt;
    }

    public Duration getEvidenceRetention() {
        return this.evidenceRetention;
    }

    public Operation evidenceRetention(Duration evidenceRetention) {
        this.setEvidenceRetention(evidenceRetention);
        return this;
    }

    public void setEvidenceRetention(Duration evidenceRetention) {
        this.evidenceRetention = evidenceRetention;
    }

    public Boolean getEvidenceSealConfirmed() {
        return this.evidenceSealConfirmed;
    }

    public Operation evidenceSealConfirmed(Boolean evidenceSealConfirmed) {
        this.setEvidenceSealConfirmed(evidenceSealConfirmed);
        return this;
    }

    public void setEvidenceSealConfirmed(Boolean evidenceSealConfirmed) {
        this.evidenceSealConfirmed = evidenceSealConfirmed;
    }

    public String getEvidenceTraceRef() {
        return this.evidenceTraceRef;
    }

    public Operation evidenceTraceRef(String evidenceTraceRef) {
        this.setEvidenceTraceRef(evidenceTraceRef);
        return this;
    }

    public void setEvidenceTraceRef(String evidenceTraceRef) {
        this.evidenceTraceRef = evidenceTraceRef;
    }

    public byte[] getTimestampToken() {
        return this.timestampToken;
    }

    public Operation timestampToken(byte[] timestampToken) {
        this.setTimestampToken(timestampToken);
        return this;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return this.timestampTokenContentType;
    }

    public Operation timestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
        return this;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public String getTimestampImprint() {
        return this.timestampImprint;
    }

    public Operation timestampImprint(String timestampImprint) {
        this.setTimestampImprint(timestampImprint);
        return this;
    }

    public void setTimestampImprint(String timestampImprint) {
        this.timestampImprint = timestampImprint;
    }

    public Instant getTimestampGenTime() {
        return this.timestampGenTime;
    }

    public Operation timestampGenTime(Instant timestampGenTime) {
        this.setTimestampGenTime(timestampGenTime);
        return this;
    }

    public void setTimestampGenTime(Instant timestampGenTime) {
        this.timestampGenTime = timestampGenTime;
    }

    public UUID getValidatorId() {
        return this.validatorId;
    }

    public Operation validatorId(UUID validatorId) {
        this.setValidatorId(validatorId);
        return this;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Instant getValidatedAt() {
        return this.validatedAt;
    }

    public Operation validatedAt(Instant validatedAt) {
        this.setValidatedAt(validatedAt);
        return this;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public Operation closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getStateReason() {
        return this.stateReason;
    }

    public Operation stateReason(String stateReason) {
        this.setStateReason(stateReason);
        return this;
    }

    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public Operation liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    public ReferenceOperation getReference() {
        return this.reference;
    }

    public void setReference(ReferenceOperation referenceOperation) {
        this.reference = referenceOperation;
    }

    public Operation reference(ReferenceOperation referenceOperation) {
        this.setReference(referenceOperation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Operation)) {
            return false;
        }
        return getId() != null && getId().equals(((Operation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Operation{" +
            "id=" + getId() +
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
            ", timestampTokenContentType='" + getTimestampTokenContentType() + "'" +
            ", timestampImprint='" + getTimestampImprint() + "'" +
            ", timestampGenTime='" + getTimestampGenTime() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", stateReason='" + getStateReason() + "'" +
            "}";
    }
}
