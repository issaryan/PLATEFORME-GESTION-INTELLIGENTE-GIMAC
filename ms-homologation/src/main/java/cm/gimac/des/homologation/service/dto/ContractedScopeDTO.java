package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.ScopeProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ContractedScope} entity.
 */
@Schema(
    description = "Service du périmètre contractualisé, dérivé de participant.service-subscribed.v1.\nUn service hors périmètre ne peut être certifié (RG-72). Unicité\n(liaison, serviceCode) posée par migration Liquibase."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ContractedScopeDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String serviceCode;

    @NotNull
    private Boolean included;

    @NotNull
    private ScopeProgress progress;

    @NotNull
    private Instant updatedAt;

    @NotNull
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Boolean getIncluded() {
        return included;
    }

    public void setIncluded(Boolean included) {
        this.included = included;
    }

    public ScopeProgress getProgress() {
        return progress;
    }

    public void setProgress(ScopeProgress progress) {
        this.progress = progress;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContractedScopeDTO)) {
            return false;
        }

        ContractedScopeDTO contractedScopeDTO = (ContractedScopeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, contractedScopeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ContractedScopeDTO{" +
            "id='" + getId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", included='" + getIncluded() + "'" +
            ", progress='" + getProgress() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
