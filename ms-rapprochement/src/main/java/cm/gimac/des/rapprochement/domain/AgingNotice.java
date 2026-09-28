package cm.gimac.des.rapprochement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Application d'une règle à un suspens, mémorisée pour n'avoir lieu qu'une fois.
 */
@Entity
@Table(name = "aging_notice")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AgingNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @NotNull
    @Min(value = 0)
    @Column(name = "age_in_days", nullable = false)
    private Integer ageInDays;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "reconciliation", "regularizationAttestation" }, allowSetters = true)
    private Suspens suspens;

    @ManyToOne(optional = false)
    @NotNull
    private AgingRule rule;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AgingNotice id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getAppliedAt() {
        return this.appliedAt;
    }

    public AgingNotice appliedAt(Instant appliedAt) {
        this.setAppliedAt(appliedAt);
        return this;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Integer getAgeInDays() {
        return this.ageInDays;
    }

    public AgingNotice ageInDays(Integer ageInDays) {
        this.setAgeInDays(ageInDays);
        return this;
    }

    public void setAgeInDays(Integer ageInDays) {
        this.ageInDays = ageInDays;
    }

    public Suspens getSuspens() {
        return this.suspens;
    }

    public void setSuspens(Suspens suspens) {
        this.suspens = suspens;
    }

    public AgingNotice suspens(Suspens suspens) {
        this.setSuspens(suspens);
        return this;
    }

    public AgingRule getRule() {
        return this.rule;
    }

    public void setRule(AgingRule agingRule) {
        this.rule = agingRule;
    }

    public AgingNotice rule(AgingRule agingRule) {
        this.setRule(agingRule);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgingNotice)) {
            return false;
        }
        return getId() != null && getId().equals(((AgingNotice) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AgingNotice{" +
            "id=" + getId() +
            ", appliedAt='" + getAppliedAt() + "'" +
            ", ageInDays=" + getAgeInDays() +
            "}";
    }
}
