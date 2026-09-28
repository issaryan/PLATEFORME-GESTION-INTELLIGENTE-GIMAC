package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.AttestationKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Attestation de régularisation : pièce déposée dans MS-DOCUMENT, validée par
 * un second acteur distinct du déposant (RG-45, algorithme 6.51).
 */
@Entity
@Table(name = "regularization_attestation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RegularizationAttestation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private AttestationKind kind;

    @NotNull
    @Column(name = "document_ref", nullable = false)
    private UUID documentRef;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "document_digest", length = 64, nullable = false)
    private String documentDigest;

    @NotNull
    @Column(name = "depositor_id", nullable = false)
    private UUID depositorId;

    @NotNull
    @Column(name = "deposited_at", nullable = false)
    private Instant depositedAt;

    @Column(name = "validator_id")
    private UUID validatorId;

    @Column(name = "validated_at")
    private Instant validatedAt;

    /**
     * Un suspens est clos par au plus une attestation validée.
     */
    @JsonIgnoreProperties(value = { "reconciliation", "regularizationAttestation" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Suspens suspens;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RegularizationAttestation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AttestationKind getKind() {
        return this.kind;
    }

    public RegularizationAttestation kind(AttestationKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(AttestationKind kind) {
        this.kind = kind;
    }

    public UUID getDocumentRef() {
        return this.documentRef;
    }

    public RegularizationAttestation documentRef(UUID documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public String getDocumentDigest() {
        return this.documentDigest;
    }

    public RegularizationAttestation documentDigest(String documentDigest) {
        this.setDocumentDigest(documentDigest);
        return this;
    }

    public void setDocumentDigest(String documentDigest) {
        this.documentDigest = documentDigest;
    }

    public UUID getDepositorId() {
        return this.depositorId;
    }

    public RegularizationAttestation depositorId(UUID depositorId) {
        this.setDepositorId(depositorId);
        return this;
    }

    public void setDepositorId(UUID depositorId) {
        this.depositorId = depositorId;
    }

    public Instant getDepositedAt() {
        return this.depositedAt;
    }

    public RegularizationAttestation depositedAt(Instant depositedAt) {
        this.setDepositedAt(depositedAt);
        return this;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
    }

    public UUID getValidatorId() {
        return this.validatorId;
    }

    public RegularizationAttestation validatorId(UUID validatorId) {
        this.setValidatorId(validatorId);
        return this;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Instant getValidatedAt() {
        return this.validatedAt;
    }

    public RegularizationAttestation validatedAt(Instant validatedAt) {
        this.setValidatedAt(validatedAt);
        return this;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Suspens getSuspens() {
        return this.suspens;
    }

    public void setSuspens(Suspens suspens) {
        this.suspens = suspens;
    }

    public RegularizationAttestation suspens(Suspens suspens) {
        this.setSuspens(suspens);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegularizationAttestation)) {
            return false;
        }
        return getId() != null && getId().equals(((RegularizationAttestation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RegularizationAttestation{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", documentDigest='" + getDocumentDigest() + "'" +
            ", depositorId='" + getDepositorId() + "'" +
            ", depositedAt='" + getDepositedAt() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            "}";
    }
}
