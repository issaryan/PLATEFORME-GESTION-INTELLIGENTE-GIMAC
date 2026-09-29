package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ScopeItem} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ScopeItemDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 40)
    private String serviceCode;

    @Size(max = 200)
    private String serviceLibelle;

    @NotNull
    private EtatScopeItem etat;

    @NotNull
    private ContractualScopeDTO scope;

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

    public String getServiceLibelle() {
        return serviceLibelle;
    }

    public void setServiceLibelle(String serviceLibelle) {
        this.serviceLibelle = serviceLibelle;
    }

    public EtatScopeItem getEtat() {
        return etat;
    }

    public void setEtat(EtatScopeItem etat) {
        this.etat = etat;
    }

    public ContractualScopeDTO getScope() {
        return scope;
    }

    public void setScope(ContractualScopeDTO scope) {
        this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScopeItemDTO)) {
            return false;
        }

        ScopeItemDTO scopeItemDTO = (ScopeItemDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, scopeItemDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ScopeItemDTO{" +
            "id='" + getId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", serviceLibelle='" + getServiceLibelle() + "'" +
            ", etat='" + getEtat() + "'" +
            ", scope=" + getScope() +
            "}";
    }
}
