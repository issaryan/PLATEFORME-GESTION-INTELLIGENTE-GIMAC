package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.AccessReviewDecision} entity.
 */
@Schema(description = "Décision de réexamen pour un collaborateur.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AccessReviewDecisionDTO implements Serializable {

    private UUID id;

    @NotNull
    private ReviewDecision decision;

    @Size(max = 500)
    private String comment;

    @NotNull
    private UUID decidedBy;

    @NotNull
    private Instant decidedAt;

    @NotNull
    private AccessReviewDTO review;

    @NotNull
    private CollaboratorDTO collaborator;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public AccessReviewDTO getReview() {
        return review;
    }

    public void setReview(AccessReviewDTO review) {
        this.review = review;
    }

    public CollaboratorDTO getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(CollaboratorDTO collaborator) {
        this.collaborator = collaborator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessReviewDecisionDTO)) {
            return false;
        }

        AccessReviewDecisionDTO accessReviewDecisionDTO = (AccessReviewDecisionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, accessReviewDecisionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AccessReviewDecisionDTO{" +
            "id='" + getId() + "'" +
            ", decision='" + getDecision() + "'" +
            ", comment='" + getComment() + "'" +
            ", decidedBy='" + getDecidedBy() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            ", review=" + getReview() +
            ", collaborator=" + getCollaborator() +
            "}";
    }
}
