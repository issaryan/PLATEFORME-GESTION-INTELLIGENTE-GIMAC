package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.LinkScope;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Liaison d'institution ou plateforme du groupement supervisée (CC §6.1.10.3).
 */
@Entity
@Table(name = "supervised_link")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SupervisedLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant lisible du CC, par exemple LIA-2026-017.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private LinkScope scope;

    /**
     * Institution (MS-PARTICIPANT), pour une liaison de portée INSTITUTION.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    /**
     * Type, par exemple « liaison de paiement mobile » ou « commutateur ».
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "type", length = 100, nullable = false)
    private String type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private LinkState state;

    @NotNull
    @Column(name = "state_since", nullable = false)
    private Instant stateSince;

    @Column(name = "last_measure_at")
    private Instant lastMeasureAt;

    /**
     * Période de mesure attendue ; au-delà de 3 périodes sans mesure : INJOIGNABLE.
     */
    @NotNull
    @Column(name = "measure_period", nullable = false)
    private Duration measurePeriod;

    /**
     * Fenêtre de maintenance déclarée par le chef de quart (algorithme 6.47, étape 1).
     */
    @Column(name = "maintenance_from")
    private Instant maintenanceFrom;

    @Column(name = "maintenance_until")
    private Instant maintenanceUntil;

    /**
     * Plateforme dont dépend la liaison, utilisée par la corrélation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "parentPlatform" }, allowSetters = true)
    private SupervisedLink parentPlatform;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public SupervisedLink id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public SupervisedLink code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public SupervisedLink label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LinkScope getScope() {
        return this.scope;
    }

    public SupervisedLink scope(LinkScope scope) {
        this.setScope(scope);
        return this;
    }

    public void setScope(LinkScope scope) {
        this.scope = scope;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public SupervisedLink institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getType() {
        return this.type;
    }

    public SupervisedLink type(String type) {
        this.setType(type);
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LinkState getState() {
        return this.state;
    }

    public SupervisedLink state(LinkState state) {
        this.setState(state);
        return this;
    }

    public void setState(LinkState state) {
        this.state = state;
    }

    public Instant getStateSince() {
        return this.stateSince;
    }

    public SupervisedLink stateSince(Instant stateSince) {
        this.setStateSince(stateSince);
        return this;
    }

    public void setStateSince(Instant stateSince) {
        this.stateSince = stateSince;
    }

    public Instant getLastMeasureAt() {
        return this.lastMeasureAt;
    }

    public SupervisedLink lastMeasureAt(Instant lastMeasureAt) {
        this.setLastMeasureAt(lastMeasureAt);
        return this;
    }

    public void setLastMeasureAt(Instant lastMeasureAt) {
        this.lastMeasureAt = lastMeasureAt;
    }

    public Duration getMeasurePeriod() {
        return this.measurePeriod;
    }

    public SupervisedLink measurePeriod(Duration measurePeriod) {
        this.setMeasurePeriod(measurePeriod);
        return this;
    }

    public void setMeasurePeriod(Duration measurePeriod) {
        this.measurePeriod = measurePeriod;
    }

    public Instant getMaintenanceFrom() {
        return this.maintenanceFrom;
    }

    public SupervisedLink maintenanceFrom(Instant maintenanceFrom) {
        this.setMaintenanceFrom(maintenanceFrom);
        return this;
    }

    public void setMaintenanceFrom(Instant maintenanceFrom) {
        this.maintenanceFrom = maintenanceFrom;
    }

    public Instant getMaintenanceUntil() {
        return this.maintenanceUntil;
    }

    public SupervisedLink maintenanceUntil(Instant maintenanceUntil) {
        this.setMaintenanceUntil(maintenanceUntil);
        return this;
    }

    public void setMaintenanceUntil(Instant maintenanceUntil) {
        this.maintenanceUntil = maintenanceUntil;
    }

    public SupervisedLink getParentPlatform() {
        return this.parentPlatform;
    }

    public void setParentPlatform(SupervisedLink supervisedLink) {
        this.parentPlatform = supervisedLink;
    }

    public SupervisedLink parentPlatform(SupervisedLink supervisedLink) {
        this.setParentPlatform(supervisedLink);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SupervisedLink)) {
            return false;
        }
        return getId() != null && getId().equals(((SupervisedLink) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SupervisedLink{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", scope='" + getScope() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", type='" + getType() + "'" +
            ", state='" + getState() + "'" +
            ", stateSince='" + getStateSince() + "'" +
            ", lastMeasureAt='" + getLastMeasureAt() + "'" +
            ", measurePeriod='" + getMeasurePeriod() + "'" +
            ", maintenanceFrom='" + getMaintenanceFrom() + "'" +
            ", maintenanceUntil='" + getMaintenanceUntil() + "'" +
            "}";
    }
}
