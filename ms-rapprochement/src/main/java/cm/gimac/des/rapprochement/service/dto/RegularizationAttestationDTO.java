package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.AttestationKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.RegularizationAttestation} entity.
 */
@Schema(
    description = "Attestation de régularisation : pièce déposée dans MS-DOCUMENT, validée par\nun second acteur distinct du déposant (RG-45, algorithme 6.51)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RegularizationAttestationDTO implements Serializable {

    private UUID id;

    @NotNull
    private AttestationKind kind;

    @NotNull
    private UUID documentRef;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String documentDigest;

    @NotNull
    private UUID depositorId;

    @NotNull
    private Instant depositedAt;

    private UUID validatorId;

    private Instant validatedAt;

    @NotNull
    @Schema(description = "Un suspens est clos par au plus une attestation validée.")
    private SuspensDTO suspens;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AttestationKind getKind() {
        return kind;
    }

    public void setKind(AttestationKind kind) {
        this.kind = kind;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public String getDocumentDigest() {
        return documentDigest;
    }

    public void setDocumentDigest(String documentDigest) {
        this.documentDigest = documentDigest;
    }

    public UUID getDepositorId() {
        return depositorId;
    }

    public void setDepositorId(UUID depositorId) {
        this.depositorId = depositorId;
    }

    public Instant getDepositedAt() {
        return depositedAt;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
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

    public SuspensDTO getSuspens() {
        return suspens;
    }

    public void setSuspens(SuspensDTO suspens) {
        this.suspens = suspens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegularizationAttestationDTO)) {
            return false;
        }

        RegularizationAttestationDTO regularizationAttestationDTO = (RegularizationAttestationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, regularizationAttestationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RegularizationAttestationDTO{" +
            "id='" + getId() + "'" +
            ", kind='" + getKind() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", documentDigest='" + getDocumentDigest() + "'" +
            ", depositorId='" + getDepositorId() + "'" +
            ", depositedAt='" + getDepositedAt() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", suspens=" + getSuspens() +
            "}";
    }
}
