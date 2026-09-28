package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.ReviewState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Campagne trimestrielle de réexamen des accès, scellée à la clôture (RG-66, IAM-10).
 */
@Entity
@Table(name = "access_review")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AccessReview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 20)
    @Column(name = "period", length = 20, nullable = false, unique = true)
    private String period;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ReviewState state;

    @NotNull
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Empreinte SHA-256 du réexamen scellé, en hexadécimal.
     */
    @Size(max = 64)
    @Column(name = "digest", length = 64)
    private String digest;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AccessReview id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPeriod() {
        return this.period;
    }

    public AccessReview period(String period) {
        this.setPeriod(period);
        return this;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public ReviewState getState() {
        return this.state;
    }

    public AccessReview state(ReviewState state) {
        this.setState(state);
        return this;
    }

    public void setState(ReviewState state) {
        this.state = state;
    }

    public Instant getOpenedAt() {
        return this.openedAt;
    }

    public AccessReview openedAt(Instant openedAt) {
        this.setOpenedAt(openedAt);
        return this;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public AccessReview approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public AccessReview closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getDigest() {
        return this.digest;
    }

    public AccessReview digest(String digest) {
        this.setDigest(digest);
        return this;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessReview)) {
            return false;
        }
        return getId() != null && getId().equals(((AccessReview) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AccessReview{" +
            "id=" + getId() +
            ", period='" + getPeriod() + "'" +
            ", state='" + getState() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", digest='" + getDigest() + "'" +
            "}";
    }
}
