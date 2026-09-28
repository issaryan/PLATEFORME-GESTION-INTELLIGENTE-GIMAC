package cm.gimac.des.incident.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Gel d'engagement en attente de tiers (RG-37) : motivé et appuyé d'une pièce ;
 * un seul gel ouvert par ticket à un instant donné.
 */
@Entity
@Table(name = "engagement_freeze")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EngagementFreeze implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 500)
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @NotNull
    @Size(max = 255)
    @Column(name = "third_party", length = 255, nullable = false)
    private String thirdParty;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Pièce justificative déposée dans MS-DOCUMENT.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "proof_ref", length = 100, nullable = false)
    private String proofRef;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "relatedTicket", "incident" }, allowSetters = true)
    private Ticket ticket;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public EngagementFreeze id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReason() {
        return this.reason;
    }

    public EngagementFreeze reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getThirdParty() {
        return this.thirdParty;
    }

    public EngagementFreeze thirdParty(String thirdParty) {
        this.setThirdParty(thirdParty);
        return this;
    }

    public void setThirdParty(String thirdParty) {
        this.thirdParty = thirdParty;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public EngagementFreeze startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return this.endedAt;
    }

    public EngagementFreeze endedAt(Instant endedAt) {
        this.setEndedAt(endedAt);
        return this;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getProofRef() {
        return this.proofRef;
    }

    public EngagementFreeze proofRef(String proofRef) {
        this.setProofRef(proofRef);
        return this;
    }

    public void setProofRef(String proofRef) {
        this.proofRef = proofRef;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public EngagementFreeze ticket(Ticket ticket) {
        this.setTicket(ticket);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EngagementFreeze)) {
            return false;
        }
        return getId() != null && getId().equals(((EngagementFreeze) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EngagementFreeze{" +
            "id=" + getId() +
            ", reason='" + getReason() + "'" +
            ", thirdParty='" + getThirdParty() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", endedAt='" + getEndedAt() + "'" +
            ", proofRef='" + getProofRef() + "'" +
            "}";
    }
}
