package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.CrisisActivation;
import cm.gimac.des.incident.domain.enumeration.Severity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;

/**
 * Engagement de service par sévérité (tableau 6.42, valeurs paramétrables).
 */
@Entity
@Table(name = "service_commitment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ServiceCommitment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, unique = true)
    private Severity severity;

    @NotNull
    @Column(name = "take_charge", nullable = false)
    private Duration takeCharge;

    @NotNull
    @Column(name = "restoration", nullable = false)
    private Duration restoration;

    /**
     * Paliers d'escalade, liste JSON : durées absolues ISO-8601 pour S1,
     * fractions du délai de rétablissement pour S2 à S4.
     */
    @Lob
    @Column(name = "escalation_steps", nullable = false)
    private String escalationSteps;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "crisis_activation", nullable = false)
    private CrisisActivation crisisActivation;

    /**
     * Période d'observation après résolution (24 h pour S1 et S2, algorithme 6.43).
     */
    @Column(name = "observation_period")
    private Duration observationPeriod;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ServiceCommitment id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public ServiceCommitment severity(Severity severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Duration getTakeCharge() {
        return this.takeCharge;
    }

    public ServiceCommitment takeCharge(Duration takeCharge) {
        this.setTakeCharge(takeCharge);
        return this;
    }

    public void setTakeCharge(Duration takeCharge) {
        this.takeCharge = takeCharge;
    }

    public Duration getRestoration() {
        return this.restoration;
    }

    public ServiceCommitment restoration(Duration restoration) {
        this.setRestoration(restoration);
        return this;
    }

    public void setRestoration(Duration restoration) {
        this.restoration = restoration;
    }

    public String getEscalationSteps() {
        return this.escalationSteps;
    }

    public ServiceCommitment escalationSteps(String escalationSteps) {
        this.setEscalationSteps(escalationSteps);
        return this;
    }

    public void setEscalationSteps(String escalationSteps) {
        this.escalationSteps = escalationSteps;
    }

    public CrisisActivation getCrisisActivation() {
        return this.crisisActivation;
    }

    public ServiceCommitment crisisActivation(CrisisActivation crisisActivation) {
        this.setCrisisActivation(crisisActivation);
        return this;
    }

    public void setCrisisActivation(CrisisActivation crisisActivation) {
        this.crisisActivation = crisisActivation;
    }

    public Duration getObservationPeriod() {
        return this.observationPeriod;
    }

    public ServiceCommitment observationPeriod(Duration observationPeriod) {
        this.setObservationPeriod(observationPeriod);
        return this;
    }

    public void setObservationPeriod(Duration observationPeriod) {
        this.observationPeriod = observationPeriod;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceCommitment)) {
            return false;
        }
        return getId() != null && getId().equals(((ServiceCommitment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ServiceCommitment{" +
            "id=" + getId() +
            ", severity='" + getSeverity() + "'" +
            ", takeCharge='" + getTakeCharge() + "'" +
            ", restoration='" + getRestoration() + "'" +
            ", escalationSteps='" + getEscalationSteps() + "'" +
            ", crisisActivation='" + getCrisisActivation() + "'" +
            ", observationPeriod='" + getObservationPeriod() + "'" +
            "}";
    }
}
