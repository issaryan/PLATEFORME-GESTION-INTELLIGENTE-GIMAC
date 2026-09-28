package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.IndicatorSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Définition d'un indicateur (code, source, unité, fenêtre, seuil à deux
 * niveaux). Écart CC corrigé : l'entité Indicator du CC mêle définition et
 * valeur ; elle est scindée en IndicatorDefinition et IndicatorValue, ce qui
 * évite aussi « Indicator », mot réservé SQL signalé par JHipster.
 */
@Entity
@Table(name = "indicator_definition")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IndicatorDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code, par exemple EXP-MTTR-S2, RES-DISPO-DECL.
     */
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9-]+$")
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private IndicatorSource source;

    @NotNull
    @Size(max = 20)
    @Column(name = "unit", length = 20, nullable = false)
    private String unit;

    /**
     * Fenêtre W, par exemple 30 jours.
     */
    @NotNull
    @Column(name = "jhi_window", nullable = false)
    private Duration window;

    /**
     * Nombre K de segments du tampon circulaire (K = W / S).
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 1000)
    @Column(name = "segment_count", nullable = false)
    private Integer segmentCount;

    /**
     * Pas S d'un segment.
     */
    @NotNull
    @Column(name = "segment_length", nullable = false)
    private Duration segmentLength;

    /**
     * Seuil d'alerte (objet-valeur Threshold du CC).
     */
    @Column(name = "warning_threshold", precision = 21, scale = 2)
    private BigDecimal warningThreshold;

    /**
     * Seuil critique.
     */
    @Column(name = "critical_threshold", precision = 21, scale = 2)
    private BigDecimal criticalThreshold;

    /**
     * Sens de lecture : une valeur plus élevée est plus défavorable.
     */
    @NotNull
    @Column(name = "higher_is_worse", nullable = false)
    private Boolean higherIsWorse;

    /**
     * Audience : rôles ou services destinataires, séparés par des virgules.
     */
    @Size(max = 500)
    @Column(name = "audience", length = 500)
    private String audience;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "indicators")
    @JsonIgnoreProperties(value = { "indicators" }, allowSetters = true)
    private Set<CockpitView> cockpits = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public IndicatorDefinition id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public IndicatorDefinition code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public IndicatorDefinition label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public IndicatorSource getSource() {
        return this.source;
    }

    public IndicatorDefinition source(IndicatorSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(IndicatorSource source) {
        this.source = source;
    }

    public String getUnit() {
        return this.unit;
    }

    public IndicatorDefinition unit(String unit) {
        this.setUnit(unit);
        return this;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Duration getWindow() {
        return this.window;
    }

    public IndicatorDefinition window(Duration window) {
        this.setWindow(window);
        return this;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public Integer getSegmentCount() {
        return this.segmentCount;
    }

    public IndicatorDefinition segmentCount(Integer segmentCount) {
        this.setSegmentCount(segmentCount);
        return this;
    }

    public void setSegmentCount(Integer segmentCount) {
        this.segmentCount = segmentCount;
    }

    public Duration getSegmentLength() {
        return this.segmentLength;
    }

    public IndicatorDefinition segmentLength(Duration segmentLength) {
        this.setSegmentLength(segmentLength);
        return this;
    }

    public void setSegmentLength(Duration segmentLength) {
        this.segmentLength = segmentLength;
    }

    public BigDecimal getWarningThreshold() {
        return this.warningThreshold;
    }

    public IndicatorDefinition warningThreshold(BigDecimal warningThreshold) {
        this.setWarningThreshold(warningThreshold);
        return this;
    }

    public void setWarningThreshold(BigDecimal warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public BigDecimal getCriticalThreshold() {
        return this.criticalThreshold;
    }

    public IndicatorDefinition criticalThreshold(BigDecimal criticalThreshold) {
        this.setCriticalThreshold(criticalThreshold);
        return this;
    }

    public void setCriticalThreshold(BigDecimal criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public Boolean getHigherIsWorse() {
        return this.higherIsWorse;
    }

    public IndicatorDefinition higherIsWorse(Boolean higherIsWorse) {
        this.setHigherIsWorse(higherIsWorse);
        return this;
    }

    public void setHigherIsWorse(Boolean higherIsWorse) {
        this.higherIsWorse = higherIsWorse;
    }

    public String getAudience() {
        return this.audience;
    }

    public IndicatorDefinition audience(String audience) {
        this.setAudience(audience);
        return this;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Boolean getActive() {
        return this.active;
    }

    public IndicatorDefinition active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<CockpitView> getCockpits() {
        return this.cockpits;
    }

    public void setCockpits(Set<CockpitView> cockpitViews) {
        if (this.cockpits != null) {
            this.cockpits.forEach(i -> i.removeIndicators(this));
        }
        if (cockpitViews != null) {
            cockpitViews.forEach(i -> i.addIndicators(this));
        }
        this.cockpits = cockpitViews;
    }

    public IndicatorDefinition cockpits(Set<CockpitView> cockpitViews) {
        this.setCockpits(cockpitViews);
        return this;
    }

    public IndicatorDefinition addCockpits(CockpitView cockpitView) {
        this.cockpits.add(cockpitView);
        cockpitView.getIndicators().add(this);
        return this;
    }

    public IndicatorDefinition removeCockpits(CockpitView cockpitView) {
        this.cockpits.remove(cockpitView);
        cockpitView.getIndicators().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndicatorDefinition)) {
            return false;
        }
        return getId() != null && getId().equals(((IndicatorDefinition) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IndicatorDefinition{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", source='" + getSource() + "'" +
            ", unit='" + getUnit() + "'" +
            ", window='" + getWindow() + "'" +
            ", segmentCount=" + getSegmentCount() +
            ", segmentLength='" + getSegmentLength() + "'" +
            ", warningThreshold=" + getWarningThreshold() +
            ", criticalThreshold=" + getCriticalThreshold() +
            ", higherIsWorse='" + getHigherIsWorse() + "'" +
            ", audience='" + getAudience() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
