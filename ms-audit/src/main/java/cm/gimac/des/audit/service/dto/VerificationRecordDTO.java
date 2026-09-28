package cm.gimac.des.audit.service.dto;

import cm.gimac.des.audit.domain.enumeration.VerificationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.VerificationRecord} entity.
 */
@Schema(
    description = "Procès-verbal de vérification par l'auditeur (niveau AUDIT_SECURITE, D-31).\nIl devient lui-même un événement d'audit."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VerificationRecordDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID verifierId;

    @NotNull
    private Instant verifiedAt;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String expectedRoot;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String recomputedRoot;

    private Boolean tokenValid;

    @NotNull
    private VerificationResult result;

    @Size(max = 1000)
    private String comment;

    @NotNull
    private InclusionProofDTO proof;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVerifierId() {
        return verifierId;
    }

    public void setVerifierId(UUID verifierId) {
        this.verifierId = verifierId;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getExpectedRoot() {
        return expectedRoot;
    }

    public void setExpectedRoot(String expectedRoot) {
        this.expectedRoot = expectedRoot;
    }

    public String getRecomputedRoot() {
        return recomputedRoot;
    }

    public void setRecomputedRoot(String recomputedRoot) {
        this.recomputedRoot = recomputedRoot;
    }

    public Boolean getTokenValid() {
        return tokenValid;
    }

    public void setTokenValid(Boolean tokenValid) {
        this.tokenValid = tokenValid;
    }

    public VerificationResult getResult() {
        return result;
    }

    public void setResult(VerificationResult result) {
        this.result = result;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public InclusionProofDTO getProof() {
        return proof;
    }

    public void setProof(InclusionProofDTO proof) {
        this.proof = proof;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerificationRecordDTO)) {
            return false;
        }

        VerificationRecordDTO verificationRecordDTO = (VerificationRecordDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, verificationRecordDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VerificationRecordDTO{" +
            "id='" + getId() + "'" +
            ", verifierId='" + getVerifierId() + "'" +
            ", verifiedAt='" + getVerifiedAt() + "'" +
            ", expectedRoot='" + getExpectedRoot() + "'" +
            ", recomputedRoot='" + getRecomputedRoot() + "'" +
            ", tokenValid='" + getTokenValid() + "'" +
            ", result='" + getResult() + "'" +
            ", comment='" + getComment() + "'" +
            ", proof=" + getProof() +
            "}";
    }
}
