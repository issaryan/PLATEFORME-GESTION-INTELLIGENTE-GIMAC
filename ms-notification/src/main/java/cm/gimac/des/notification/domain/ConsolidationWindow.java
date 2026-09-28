package cm.gimac.des.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Fenêtre de consolidation anti-tempête d'une clé de regroupement (algorithme 6.24).
 */
@Entity
@Table(name = "consolidation_window")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsolidationWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 255)
    @Column(name = "group_key", length = 255, nullable = false)
    private String groupKey;

    @NotNull
    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @NotNull
    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    /**
     * Durée courante de la fenêtre : 5 min, doublée jusqu'à 30 min.
     */
    @NotNull
    @Column(name = "window_duration", nullable = false)
    private Duration windowDuration;

    @NotNull
    @Min(value = 0)
    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @NotNull
    @Column(name = "closed", nullable = false)
    private Boolean closed;

    /**
     * Clôture anticipée par l'arrivée d'une occurrence urgente ou critique.
     */
    @NotNull
    @Column(name = "closed_early", nullable = false)
    private Boolean closedEarly;

    /**
     * Synthèse remise à la fermeture de la fenêtre.
     */
    @JsonIgnoreProperties(value = { "consolidationWindow", "summarizedWindow" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private Notification summaryNotification;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ConsolidationWindow id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGroupKey() {
        return this.groupKey;
    }

    public ConsolidationWindow groupKey(String groupKey) {
        this.setGroupKey(groupKey);
        return this;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public Instant getOpensAt() {
        return this.opensAt;
    }

    public ConsolidationWindow opensAt(Instant opensAt) {
        this.setOpensAt(opensAt);
        return this;
    }

    public void setOpensAt(Instant opensAt) {
        this.opensAt = opensAt;
    }

    public Instant getClosesAt() {
        return this.closesAt;
    }

    public ConsolidationWindow closesAt(Instant closesAt) {
        this.setClosesAt(closesAt);
        return this;
    }

    public void setClosesAt(Instant closesAt) {
        this.closesAt = closesAt;
    }

    public Duration getWindowDuration() {
        return this.windowDuration;
    }

    public ConsolidationWindow windowDuration(Duration windowDuration) {
        this.setWindowDuration(windowDuration);
        return this;
    }

    public void setWindowDuration(Duration windowDuration) {
        this.windowDuration = windowDuration;
    }

    public Integer getMemberCount() {
        return this.memberCount;
    }

    public ConsolidationWindow memberCount(Integer memberCount) {
        this.setMemberCount(memberCount);
        return this;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Boolean getClosed() {
        return this.closed;
    }

    public ConsolidationWindow closed(Boolean closed) {
        this.setClosed(closed);
        return this;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public Boolean getClosedEarly() {
        return this.closedEarly;
    }

    public ConsolidationWindow closedEarly(Boolean closedEarly) {
        this.setClosedEarly(closedEarly);
        return this;
    }

    public void setClosedEarly(Boolean closedEarly) {
        this.closedEarly = closedEarly;
    }

    public Notification getSummaryNotification() {
        return this.summaryNotification;
    }

    public void setSummaryNotification(Notification notification) {
        this.summaryNotification = notification;
    }

    public ConsolidationWindow summaryNotification(Notification notification) {
        this.setSummaryNotification(notification);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsolidationWindow)) {
            return false;
        }
        return getId() != null && getId().equals(((ConsolidationWindow) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsolidationWindow{" +
            "id=" + getId() +
            ", groupKey='" + getGroupKey() + "'" +
            ", opensAt='" + getOpensAt() + "'" +
            ", closesAt='" + getClosesAt() + "'" +
            ", windowDuration='" + getWindowDuration() + "'" +
            ", memberCount=" + getMemberCount() +
            ", closed='" + getClosed() + "'" +
            ", closedEarly='" + getClosedEarly() + "'" +
            "}";
    }
}
