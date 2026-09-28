package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.ReviewState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.AccessReview} entity.
 */
@Schema(description = "Campagne trimestrielle de réexamen des accès, scellée à la clôture (RG-66, IAM-10).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AccessReviewDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    private String period;

    @NotNull
    private ReviewState state;

    @NotNull
    private Instant openedAt;

    private UUID approverId;

    private Instant closedAt;

    @Size(max = 64)
    @Schema(description = "Empreinte SHA-256 du réexamen scellé, en hexadécimal.")
    private String digest;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public ReviewState getState() {
        return state;
    }

    public void setState(ReviewState state) {
        this.state = state;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessReviewDTO)) {
            return false;
        }

        AccessReviewDTO accessReviewDTO = (AccessReviewDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, accessReviewDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AccessReviewDTO{" +
            "id='" + getId() + "'" +
            ", period='" + getPeriod() + "'" +
            ", state='" + getState() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", digest='" + getDigest() + "'" +
            "}";
    }
}
