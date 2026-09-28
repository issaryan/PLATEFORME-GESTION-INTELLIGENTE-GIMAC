package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Règle de projection d'un sujet en fait (table R de l'algorithme 6.56) :
 * domaine cible, mesures à extraire et champs alimentant les dimensions.
 * Un sujet sans règle est acquitté sans traitement.
 */
@Entity
@Table(name = "projection_rule")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProjectionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Sujet du bus, par exemple incident.incident-resolved.v1.
     */
    @NotNull
    @Size(max = 150)
    @Column(name = "subject", length = 150, nullable = false, unique = true)
    private String subject;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false)
    private FactDomain domain;

    /**
     * Extraction des mesures : JSON (compteur, durée entre deux instants, montant agrégé).
     */
    @Lob
    @Column(name = "measure_mapping", nullable = false)
    private String measureMapping;

    /**
     * Correspondance des champs de la charge utile vers les dimensions : JSON.
     */
    @Lob
    @Column(name = "dimension_mapping", nullable = false)
    private String dimensionMapping;

    /**
     * Codes des indicateurs auxquels le sujet contribue, séparés par des virgules.
     */
    @Size(max = 1000)
    @Column(name = "indicator_codes", length = 1000)
    private String indicatorCodes;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ProjectionRule id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSubject() {
        return this.subject;
    }

    public ProjectionRule subject(String subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public FactDomain getDomain() {
        return this.domain;
    }

    public ProjectionRule domain(FactDomain domain) {
        this.setDomain(domain);
        return this;
    }

    public void setDomain(FactDomain domain) {
        this.domain = domain;
    }

    public String getMeasureMapping() {
        return this.measureMapping;
    }

    public ProjectionRule measureMapping(String measureMapping) {
        this.setMeasureMapping(measureMapping);
        return this;
    }

    public void setMeasureMapping(String measureMapping) {
        this.measureMapping = measureMapping;
    }

    public String getDimensionMapping() {
        return this.dimensionMapping;
    }

    public ProjectionRule dimensionMapping(String dimensionMapping) {
        this.setDimensionMapping(dimensionMapping);
        return this;
    }

    public void setDimensionMapping(String dimensionMapping) {
        this.dimensionMapping = dimensionMapping;
    }

    public String getIndicatorCodes() {
        return this.indicatorCodes;
    }

    public ProjectionRule indicatorCodes(String indicatorCodes) {
        this.setIndicatorCodes(indicatorCodes);
        return this;
    }

    public void setIndicatorCodes(String indicatorCodes) {
        this.indicatorCodes = indicatorCodes;
    }

    public Boolean getActive() {
        return this.active;
    }

    public ProjectionRule active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectionRule)) {
            return false;
        }
        return getId() != null && getId().equals(((ProjectionRule) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProjectionRule{" +
            "id=" + getId() +
            ", subject='" + getSubject() + "'" +
            ", domain='" + getDomain() + "'" +
            ", measureMapping='" + getMeasureMapping() + "'" +
            ", dimensionMapping='" + getDimensionMapping() + "'" +
            ", indicatorCodes='" + getIndicatorCodes() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
