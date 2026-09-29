package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A EvaluationBase.
 */
@Entity
@Table(name = "evaluation_base")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvaluationBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte", length = 64, nullable = false)
    private String empreinte;

    @NotNull
    @Column(name = "cycle", nullable = false)
    private Integer cycle;

    @NotNull
    @Column(name = "date_calcul", nullable = false)
    private Instant dateCalcul;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "composition", columnDefinition = "jsonb")
    private String composition;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public EvaluationBase id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmpreinte() {
        return this.empreinte;
    }

    public EvaluationBase empreinte(String empreinte) {
        this.setEmpreinte(empreinte);
        return this;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public Integer getCycle() {
        return this.cycle;
    }

    public EvaluationBase cycle(Integer cycle) {
        this.setCycle(cycle);
        return this;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public Instant getDateCalcul() {
        return this.dateCalcul;
    }

    public EvaluationBase dateCalcul(Instant dateCalcul) {
        this.setDateCalcul(dateCalcul);
        return this;
    }

    public void setDateCalcul(Instant dateCalcul) {
        this.dateCalcul = dateCalcul;
    }

    public String getComposition() {
        return this.composition;
    }

    public EvaluationBase composition(String composition) {
        this.setComposition(composition);
        return this;
    }

    public void setComposition(String composition) {
        this.composition = composition;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public EvaluationBase liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationBase)) {
            return false;
        }
        return getId() != null && getId().equals(((EvaluationBase) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvaluationBase{" +
            "id=" + getId() +
            ", empreinte='" + getEmpreinte() + "'" +
            ", cycle=" + getCycle() +
            ", dateCalcul='" + getDateCalcul() + "'" +
            ", composition='" + getComposition() + "'" +
            "}";
    }
}
