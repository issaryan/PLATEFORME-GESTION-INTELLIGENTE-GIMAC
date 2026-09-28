package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.ScopeProgress;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Service du périmètre contractualisé, dérivé de participant.service-subscribed.v1.
 * Un service hors périmètre ne peut être certifié (RG-72). Unicité
 * (liaison, serviceCode) posée par migration Liquibase.
 */
@Entity
@Table(name = "contracted_scope")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ContractedScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "service_code", length = 80, nullable = false)
    private String serviceCode;

    @NotNull
    @Column(name = "included", nullable = false)
    private Boolean included;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "progress", nullable = false)
    private ScopeProgress progress;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "catalog", "campaignSync" }, allowSetters = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ContractedScope id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public ContractedScope serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Boolean getIncluded() {
        return this.included;
    }

    public ContractedScope included(Boolean included) {
        this.setIncluded(included);
        return this;
    }

    public void setIncluded(Boolean included) {
        this.included = included;
    }

    public ScopeProgress getProgress() {
        return this.progress;
    }

    public ContractedScope progress(ScopeProgress progress) {
        this.setProgress(progress);
        return this;
    }

    public void setProgress(ScopeProgress progress) {
        this.progress = progress;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public ContractedScope updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public ContractedScope liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContractedScope)) {
            return false;
        }
        return getId() != null && getId().equals(((ContractedScope) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ContractedScope{" +
            "id=" + getId() +
            ", serviceCode='" + getServiceCode() + "'" +
            ", included='" + getIncluded() + "'" +
            ", progress='" + getProgress() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
