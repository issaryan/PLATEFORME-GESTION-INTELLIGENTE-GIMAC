package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service souscrit, publié vers MS-HOMOLOGATION (périmètre contractualisé).
 */
@Entity
@Table(name = "subscribed_service")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SubscribedService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code du service, par exemple TRANSFERT_COMPTE_A_COMPTE.
     */
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "service_code", length = 50, nullable = false)
    private String serviceCode;

    @NotNull
    @Column(name = "subscribed_on", nullable = false)
    private LocalDate subscribedOn;

    @Column(name = "terminated_on")
    private LocalDate terminatedOn;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SubscriptionState state;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public SubscribedService id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public SubscribedService serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public LocalDate getSubscribedOn() {
        return this.subscribedOn;
    }

    public SubscribedService subscribedOn(LocalDate subscribedOn) {
        this.setSubscribedOn(subscribedOn);
        return this;
    }

    public void setSubscribedOn(LocalDate subscribedOn) {
        this.subscribedOn = subscribedOn;
    }

    public LocalDate getTerminatedOn() {
        return this.terminatedOn;
    }

    public SubscribedService terminatedOn(LocalDate terminatedOn) {
        this.setTerminatedOn(terminatedOn);
        return this;
    }

    public void setTerminatedOn(LocalDate terminatedOn) {
        this.terminatedOn = terminatedOn;
    }

    public SubscriptionState getState() {
        return this.state;
    }

    public SubscribedService state(SubscriptionState state) {
        this.setState(state);
        return this;
    }

    public void setState(SubscriptionState state) {
        this.state = state;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public SubscribedService institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscribedService)) {
            return false;
        }
        return getId() != null && getId().equals(((SubscribedService) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SubscribedService{" +
            "id=" + getId() +
            ", serviceCode='" + getServiceCode() + "'" +
            ", subscribedOn='" + getSubscribedOn() + "'" +
            ", terminatedOn='" + getTerminatedOn() + "'" +
            ", state='" + getState() + "'" +
            "}";
    }
}
