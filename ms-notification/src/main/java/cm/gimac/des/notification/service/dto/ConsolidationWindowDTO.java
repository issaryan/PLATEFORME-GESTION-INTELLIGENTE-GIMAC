package cm.gimac.des.notification.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.ConsolidationWindow} entity.
 */
@Schema(description = "Fenêtre de consolidation anti-tempête d'une clé de regroupement (algorithme 6.24).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsolidationWindowDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String groupKey;

    @NotNull
    private Instant opensAt;

    @NotNull
    private Instant closesAt;

    @NotNull
    @Schema(description = "Durée courante de la fenêtre : 5 min, doublée jusqu'à 30 min.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration windowDuration;

    @NotNull
    @Min(value = 0)
    private Integer memberCount;

    @NotNull
    private Boolean closed;

    @NotNull
    @Schema(
        description = "Clôture anticipée par l'arrivée d'une occurrence urgente ou critique.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean closedEarly;

    @Schema(description = "Synthèse remise à la fermeture de la fenêtre.")
    private NotificationDTO summaryNotification;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public Instant getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(Instant opensAt) {
        this.opensAt = opensAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(Instant closesAt) {
        this.closesAt = closesAt;
    }

    public Duration getWindowDuration() {
        return windowDuration;
    }

    public void setWindowDuration(Duration windowDuration) {
        this.windowDuration = windowDuration;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public Boolean getClosedEarly() {
        return closedEarly;
    }

    public void setClosedEarly(Boolean closedEarly) {
        this.closedEarly = closedEarly;
    }

    public NotificationDTO getSummaryNotification() {
        return summaryNotification;
    }

    public void setSummaryNotification(NotificationDTO summaryNotification) {
        this.summaryNotification = summaryNotification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsolidationWindowDTO)) {
            return false;
        }

        ConsolidationWindowDTO consolidationWindowDTO = (ConsolidationWindowDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, consolidationWindowDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsolidationWindowDTO{" +
            "id='" + getId() + "'" +
            ", groupKey='" + getGroupKey() + "'" +
            ", opensAt='" + getOpensAt() + "'" +
            ", closesAt='" + getClosesAt() + "'" +
            ", windowDuration='" + getWindowDuration() + "'" +
            ", memberCount=" + getMemberCount() +
            ", closed='" + getClosed() + "'" +
            ", closedEarly='" + getClosedEarly() + "'" +
            ", summaryNotification=" + getSummaryNotification() +
            "}";
    }
}
