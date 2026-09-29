package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * A ScopeItem.
 */
@Entity
@Table(name = "scope_item")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ScopeItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 40)
    @Column(name = "service_code", length = 40, nullable = false)
    private String serviceCode;

    @Size(max = 200)
    @Column(name = "service_libelle", length = 200)
    private String serviceLibelle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatScopeItem etat;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "liaison" }, allowSetters = true)
    private ContractualScope scope;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ScopeItem id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public ScopeItem serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceLibelle() {
        return this.serviceLibelle;
    }

    public ScopeItem serviceLibelle(String serviceLibelle) {
        this.setServiceLibelle(serviceLibelle);
        return this;
    }

    public void setServiceLibelle(String serviceLibelle) {
        this.serviceLibelle = serviceLibelle;
    }

    public EtatScopeItem getEtat() {
        return this.etat;
    }

    public ScopeItem etat(EtatScopeItem etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatScopeItem etat) {
        this.etat = etat;
    }

    public ContractualScope getScope() {
        return this.scope;
    }

    public void setScope(ContractualScope contractualScope) {
        this.scope = contractualScope;
    }

    public ScopeItem scope(ContractualScope contractualScope) {
        this.setScope(contractualScope);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScopeItem)) {
            return false;
        }
        return getId() != null && getId().equals(((ScopeItem) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ScopeItem{" +
            "id=" + getId() +
            ", serviceCode='" + getServiceCode() + "'" +
            ", serviceLibelle='" + getServiceLibelle() + "'" +
            ", etat='" + getEtat() + "'" +
            "}";
    }
}
