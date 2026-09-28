package cm.gimac.des.incident.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Alerte reçue sur un objet (monitoring.alert-raised.v1), projetée pour
 * vérifier la période d'observation du test de résolution (algorithme 6.43).
 */
@Entity
@Table(name = "alert_observation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertObservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "alert_id", nullable = false, unique = true)
    private UUID alertId;

    @Size(max = 50)
    @Column(name = "link_id", length = 50)
    private String linkId;

    @Column(name = "institution_id")
    private UUID institutionId;

    /**
     * Gravité proposée par MS-MONITORING (MINEURE, MAJEURE, CRITIQUE).
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "alert_severity", length = 20, nullable = false)
    private String alertSeverity;

    @NotNull
    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AlertObservation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAlertId() {
        return this.alertId;
    }

    public AlertObservation alertId(UUID alertId) {
        this.setAlertId(alertId);
        return this;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public String getLinkId() {
        return this.linkId;
    }

    public AlertObservation linkId(String linkId) {
        this.setLinkId(linkId);
        return this;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public AlertObservation institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getAlertSeverity() {
        return this.alertSeverity;
    }

    public AlertObservation alertSeverity(String alertSeverity) {
        this.setAlertSeverity(alertSeverity);
        return this;
    }

    public void setAlertSeverity(String alertSeverity) {
        this.alertSeverity = alertSeverity;
    }

    public Instant getRaisedAt() {
        return this.raisedAt;
    }

    public AlertObservation raisedAt(Instant raisedAt) {
        this.setRaisedAt(raisedAt);
        return this;
    }

    public void setRaisedAt(Instant raisedAt) {
        this.raisedAt = raisedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AlertObservation)) {
            return false;
        }
        return getId() != null && getId().equals(((AlertObservation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertObservation{" +
            "id=" + getId() +
            ", alertId='" + getAlertId() + "'" +
            ", linkId='" + getLinkId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", alertSeverity='" + getAlertSeverity() + "'" +
            ", raisedAt='" + getRaisedAt() + "'" +
            "}";
    }
}
