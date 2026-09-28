package cm.gimac.des.certification.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Projection d'une opération close, depuis homologation.operation-closed.v1 :
 * elle entre dans le socle d'évaluation β et permet d'exclure des signataires
 * les responsables d'opérations (algorithme 6.35, étape 3). Écart CC corrigé :
 * projection décrite au CC §6.1.8.1 sans classe dédiée.
 */
@Entity
@Table(name = "closed_operation_snapshot")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClosedOperationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    /**
     * Opération tenue par MS-HOMOLOGATION (référence, BNF-04).
     */
    @NotNull
    @Column(name = "operation_id", nullable = false, unique = true)
    private UUID operationId;

    @NotNull
    @Size(max = 20)
    @Column(name = "operation_code", length = 20, nullable = false)
    private String operationCode;

    @NotNull
    @Size(max = 20)
    @Column(name = "reference_code", length = 20, nullable = false)
    private String referenceCode;

    @Size(max = 80)
    @Column(name = "service_code", length = 80)
    private String serviceCode;

    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotNull
    @Column(name = "validator_id", nullable = false)
    private UUID validatorId;

    @NotNull
    @Column(name = "evidence_document_id", nullable = false)
    private UUID evidenceDocumentId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "evidence_digest", length = 64, nullable = false)
    private String evidenceDigest;

    @NotNull
    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    @NotNull
    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ClosedOperationSnapshot id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiaisonId() {
        return this.liaisonId;
    }

    public ClosedOperationSnapshot liaisonId(UUID liaisonId) {
        this.setLiaisonId(liaisonId);
        return this;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUID getOperationId() {
        return this.operationId;
    }

    public ClosedOperationSnapshot operationId(UUID operationId) {
        this.setOperationId(operationId);
        return this;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public String getOperationCode() {
        return this.operationCode;
    }

    public ClosedOperationSnapshot operationCode(String operationCode) {
        this.setOperationCode(operationCode);
        return this;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public String getReferenceCode() {
        return this.referenceCode;
    }

    public ClosedOperationSnapshot referenceCode(String referenceCode) {
        this.setReferenceCode(referenceCode);
        return this;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public ClosedOperationSnapshot serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public ClosedOperationSnapshot ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getValidatorId() {
        return this.validatorId;
    }

    public ClosedOperationSnapshot validatorId(UUID validatorId) {
        this.setValidatorId(validatorId);
        return this;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public UUID getEvidenceDocumentId() {
        return this.evidenceDocumentId;
    }

    public ClosedOperationSnapshot evidenceDocumentId(UUID evidenceDocumentId) {
        this.setEvidenceDocumentId(evidenceDocumentId);
        return this;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public String getEvidenceDigest() {
        return this.evidenceDigest;
    }

    public ClosedOperationSnapshot evidenceDigest(String evidenceDigest) {
        this.setEvidenceDigest(evidenceDigest);
        return this;
    }

    public void setEvidenceDigest(String evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public ClosedOperationSnapshot closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public UUID getSourceEventId() {
        return this.sourceEventId;
    }

    public ClosedOperationSnapshot sourceEventId(UUID sourceEventId) {
        this.setSourceEventId(sourceEventId);
        return this;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClosedOperationSnapshot)) {
            return false;
        }
        return getId() != null && getId().equals(((ClosedOperationSnapshot) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ClosedOperationSnapshot{" +
            "id=" + getId() +
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
