package cm.gimac.des.participant.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;

/**
 * Contact de l'annuaire d'escalade, par sévérité (S1 à S4) et rang, lu par
 * MS-NOTIFICATION et MS-INCIDENT. Délai par défaut : 15 min (D-14).
 */
@Entity
@Table(name = "escalation_contact")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EscalationContact implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Sévérité de 1 (S1) à 4 (S4).
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 4)
    @Column(name = "severity", nullable = false)
    private Integer severity;

    @NotNull
    @Min(value = 1)
    @Column(name = "escalation_rank", nullable = false)
    private Integer escalationRank;

    /**
     * Délai d'acquittement (ISO 8601), PT15M par défaut.
     */
    @NotNull
    @Column(name = "ack_delay", nullable = false)
    private Duration ackDelay;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "institution" }, allowSetters = true)
    private Correspondent correspondent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "institution" }, allowSetters = true)
    private Correspondent deputy;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public EscalationContact id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getSeverity() {
        return this.severity;
    }

    public EscalationContact severity(Integer severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public Integer getEscalationRank() {
        return this.escalationRank;
    }

    public EscalationContact escalationRank(Integer escalationRank) {
        this.setEscalationRank(escalationRank);
        return this;
    }

    public void setEscalationRank(Integer escalationRank) {
        this.escalationRank = escalationRank;
    }

    public Duration getAckDelay() {
        return this.ackDelay;
    }

    public EscalationContact ackDelay(Duration ackDelay) {
        this.setAckDelay(ackDelay);
        return this;
    }

    public void setAckDelay(Duration ackDelay) {
        this.ackDelay = ackDelay;
    }

    public Boolean getActive() {
        return this.active;
    }

    public EscalationContact active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public EscalationContact institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    public Correspondent getCorrespondent() {
        return this.correspondent;
    }

    public void setCorrespondent(Correspondent correspondent) {
        this.correspondent = correspondent;
    }

    public EscalationContact correspondent(Correspondent correspondent) {
        this.setCorrespondent(correspondent);
        return this;
    }

    public Correspondent getDeputy() {
        return this.deputy;
    }

    public void setDeputy(Correspondent correspondent) {
        this.deputy = correspondent;
    }

    public EscalationContact deputy(Correspondent correspondent) {
        this.setDeputy(correspondent);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EscalationContact)) {
            return false;
        }
        return getId() != null && getId().equals(((EscalationContact) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EscalationContact{" +
            "id=" + getId() +
            ", severity=" + getSeverity() +
            ", escalationRank=" + getEscalationRank() +
            ", ackDelay='" + getAckDelay() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
