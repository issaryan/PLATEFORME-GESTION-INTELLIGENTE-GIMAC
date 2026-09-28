package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.MeasureDirection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Seuil par indicateur, avec hystérésis et confirmation (objet-valeur du CC, persisté).
 */
@Entity
@Table(name = "threshold")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Threshold implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Indicateur, par exemple « latence p95 (ms) ».
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "indicator", length = 100, nullable = false)
    private String indicator;

    @NotNull
    @Column(name = "warning", precision = 21, scale = 2, nullable = false)
    private BigDecimal warning;

    @NotNull
    @Column(name = "critical", precision = 21, scale = 2, nullable = false)
    private BigDecimal critical;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "hysteresis", precision = 21, scale = 2, nullable = false)
    private BigDecimal hysteresis;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private MeasureDirection direction;

    /**
     * Nombre k de fenêtres de confirmation.
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "debounce", nullable = false)
    private Integer debounce;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "parentPlatform" }, allowSetters = true)
    private SupervisedLink link;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Threshold id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIndicator() {
        return this.indicator;
    }

    public Threshold indicator(String indicator) {
        this.setIndicator(indicator);
        return this;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public BigDecimal getWarning() {
        return this.warning;
    }

    public Threshold warning(BigDecimal warning) {
        this.setWarning(warning);
        return this;
    }

    public void setWarning(BigDecimal warning) {
        this.warning = warning;
    }

    public BigDecimal getCritical() {
        return this.critical;
    }

    public Threshold critical(BigDecimal critical) {
        this.setCritical(critical);
        return this;
    }

    public void setCritical(BigDecimal critical) {
        this.critical = critical;
    }

    public BigDecimal getHysteresis() {
        return this.hysteresis;
    }

    public Threshold hysteresis(BigDecimal hysteresis) {
        this.setHysteresis(hysteresis);
        return this;
    }

    public void setHysteresis(BigDecimal hysteresis) {
        this.hysteresis = hysteresis;
    }

    public MeasureDirection getDirection() {
        return this.direction;
    }

    public Threshold direction(MeasureDirection direction) {
        this.setDirection(direction);
        return this;
    }

    public void setDirection(MeasureDirection direction) {
        this.direction = direction;
    }

    public Integer getDebounce() {
        return this.debounce;
    }

    public Threshold debounce(Integer debounce) {
        this.setDebounce(debounce);
        return this;
    }

    public void setDebounce(Integer debounce) {
        this.debounce = debounce;
    }

    public SupervisedLink getLink() {
        return this.link;
    }

    public void setLink(SupervisedLink supervisedLink) {
        this.link = supervisedLink;
    }

    public Threshold link(SupervisedLink supervisedLink) {
        this.setLink(supervisedLink);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Threshold)) {
            return false;
        }
        return getId() != null && getId().equals(((Threshold) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Threshold{" +
            "id=" + getId() +
            ", indicator='" + getIndicator() + "'" +
            ", warning=" + getWarning() +
            ", critical=" + getCritical() +
            ", hysteresis=" + getHysteresis() +
            ", direction='" + getDirection() + "'" +
            ", debounce=" + getDebounce() +
            "}";
    }
}
