package cm.gimac.des.audit.domain;

import cm.gimac.des.audit.domain.enumeration.VerificationResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Procès-verbal de vérification par l'auditeur (niveau AUDIT_SECURITE, D-31).
 * Il devient lui-même un événement d'audit.
 */
@Entity
@Table(name = "verification_record")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VerificationRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "verifier_id", nullable = false)
    private UUID verifierId;

    @NotNull
    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "expected_root", length = 64, nullable = false)
    private String expectedRoot;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "recomputed_root", length = 64)
    private String recomputedRoot;

    @Column(name = "token_valid")
    private Boolean tokenValid;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private VerificationResult result;

    @Size(max = 1000)
    @Column(name = "comment", length = 1000)
    private String comment;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "periodRoot" }, allowSetters = true)
    private InclusionProof proof;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public VerificationRecord id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVerifierId() {
        return this.verifierId;
    }

    public VerificationRecord verifierId(UUID verifierId) {
        this.setVerifierId(verifierId);
        return this;
    }

    public void setVerifierId(UUID verifierId) {
        this.verifierId = verifierId;
    }

    public Instant getVerifiedAt() {
        return this.verifiedAt;
    }

    public VerificationRecord verifiedAt(Instant verifiedAt) {
        this.setVerifiedAt(verifiedAt);
        return this;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getExpectedRoot() {
        return this.expectedRoot;
    }

    public VerificationRecord expectedRoot(String expectedRoot) {
        this.setExpectedRoot(expectedRoot);
        return this;
    }

    public void setExpectedRoot(String expectedRoot) {
        this.expectedRoot = expectedRoot;
    }

    public String getRecomputedRoot() {
        return this.recomputedRoot;
    }

    public VerificationRecord recomputedRoot(String recomputedRoot) {
        this.setRecomputedRoot(recomputedRoot);
        return this;
    }

    public void setRecomputedRoot(String recomputedRoot) {
        this.recomputedRoot = recomputedRoot;
    }

    public Boolean getTokenValid() {
        return this.tokenValid;
    }

    public VerificationRecord tokenValid(Boolean tokenValid) {
        this.setTokenValid(tokenValid);
        return this;
    }

    public void setTokenValid(Boolean tokenValid) {
        this.tokenValid = tokenValid;
    }

    public VerificationResult getResult() {
        return this.result;
    }

    public VerificationRecord result(VerificationResult result) {
        this.setResult(result);
        return this;
    }

    public void setResult(VerificationResult result) {
        this.result = result;
    }

    public String getComment() {
        return this.comment;
    }

    public VerificationRecord comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public InclusionProof getProof() {
        return this.proof;
    }

    public void setProof(InclusionProof inclusionProof) {
        this.proof = inclusionProof;
    }

    public VerificationRecord proof(InclusionProof inclusionProof) {
        this.setProof(inclusionProof);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerificationRecord)) {
            return false;
        }
        return getId() != null && getId().equals(((VerificationRecord) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VerificationRecord{" +
            "id=" + getId() +
            ", verifierId='" + getVerifierId() + "'" +
            ", verifiedAt='" + getVerifiedAt() + "'" +
            ", expectedRoot='" + getExpectedRoot() + "'" +
            ", recomputedRoot='" + getRecomputedRoot() + "'" +
            ", tokenValid='" + getTokenValid() + "'" +
            ", result='" + getResult() + "'" +
            ", comment='" + getComment() + "'" +
            "}";
    }
}
