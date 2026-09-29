package cm.gimac.des.homologation.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ContractualScope} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ContractualScopeDTO implements Serializable {

    private UUID id;

    @NotNull
    private Instant dateDerivation;

    @Size(max = 80)
    private String sourceEventId;

    @NotNull
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getDateDerivation() {
        return dateDerivation;
    }

    public void setDateDerivation(Instant dateDerivation) {
        this.dateDerivation = dateDerivation;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
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
        if (!(o instanceof ContractualScopeDTO)) {
            return false;
        }

        ContractualScopeDTO contractualScopeDTO = (ContractualScopeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, contractualScopeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ContractualScopeDTO{" +
            "id='" + getId() + "'" +
            ", dateDerivation='" + getDateDerivation() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
