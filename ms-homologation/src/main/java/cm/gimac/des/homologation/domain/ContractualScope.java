package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A ContractualScope.
 */
@Entity
@Table(name = "contractual_scope")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ContractualScope implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "date_derivation", nullable = false)
    private Instant dateDerivation;

    @Size(max = 80)
    @Column(name = "source_event_id", length = 80)
    private String sourceEventId;

    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ContractualScope id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getDateDerivation() {
        return this.dateDerivation;
    }

    public ContractualScope dateDerivation(Instant dateDerivation) {
        this.setDateDerivation(dateDerivation);
        return this;
    }

    public void setDateDerivation(Instant dateDerivation) {
        this.dateDerivation = dateDerivation;
    }

    public String getSourceEventId() {
        return this.sourceEventId;
    }

    public ContractualScope sourceEventId(String sourceEventId) {
        this.setSourceEventId(sourceEventId);
        return this;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public ContractualScope liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContractualScope)) {
            return false;
        }
        return getId() != null && getId().equals(((ContractualScope) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ContractualScope{" +
            "id=" + getId() +
            ", dateDerivation='" + getDateDerivation() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            "}";
    }
}
