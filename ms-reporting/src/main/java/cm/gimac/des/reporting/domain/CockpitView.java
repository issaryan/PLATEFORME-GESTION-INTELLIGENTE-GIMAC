package cm.gimac.des.reporting.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cockpit ou vue par service (cockpit DG, cockpit DES, vues par service).
 */
@Entity
@Table(name = "cockpit_view")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CockpitView implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code, par exemple COCKPIT-DG, COCKPIT-DES.
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
    @Size(max = 255)
    @Column(name = "audience", length = 255, nullable = false)
    private String audience;

    /**
     * Intervalle de rafraîchissement, 60 s par défaut.
     */
    @NotNull
    @Column(name = "refresh_interval", nullable = false)
    private Duration refreshInterval;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_cockpit_view__indicators",
        joinColumns = @JoinColumn(name = "cockpit_view_id"),
        inverseJoinColumns = @JoinColumn(name = "indicators_id")
    )
    @JsonIgnoreProperties(value = { "cockpits" }, allowSetters = true)
    private Set<IndicatorDefinition> indicators = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CockpitView id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public CockpitView code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public CockpitView label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAudience() {
        return this.audience;
    }

    public CockpitView audience(String audience) {
        this.setAudience(audience);
        return this;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getRefreshInterval() {
        return this.refreshInterval;
    }

    public CockpitView refreshInterval(Duration refreshInterval) {
        this.setRefreshInterval(refreshInterval);
        return this;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public Set<IndicatorDefinition> getIndicators() {
        return this.indicators;
    }

    public void setIndicators(Set<IndicatorDefinition> indicatorDefinitions) {
        this.indicators = indicatorDefinitions;
    }

    public CockpitView indicators(Set<IndicatorDefinition> indicatorDefinitions) {
        this.setIndicators(indicatorDefinitions);
        return this;
    }

    public CockpitView addIndicators(IndicatorDefinition indicatorDefinition) {
        this.indicators.add(indicatorDefinition);
        return this;
    }

    public CockpitView removeIndicators(IndicatorDefinition indicatorDefinition) {
        this.indicators.remove(indicatorDefinition);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CockpitView)) {
            return false;
        }
        return getId() != null && getId().equals(((CockpitView) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CockpitView{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", audience='" + getAudience() + "'" +
            ", refreshInterval='" + getRefreshInterval() + "'" +
            "}";
    }
}
