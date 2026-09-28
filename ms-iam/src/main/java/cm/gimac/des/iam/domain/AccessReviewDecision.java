package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Décision de réexamen pour un collaborateur.
 */
@Entity
@Table(name = "access_review_decision")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AccessReviewDecision implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private ReviewDecision decision;

    @Size(max = 500)
    @Column(name = "comment", length = 500)
    private String comment;

    @NotNull
    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @NotNull
    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @ManyToOne(optional = false)
    @NotNull
    private AccessReview review;

    @ManyToOne(optional = false)
    @NotNull
    private Collaborator collaborator;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AccessReviewDecision id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ReviewDecision getDecision() {
        return this.decision;
    }

    public AccessReviewDecision decision(ReviewDecision decision) {
        this.setDecision(decision);
        return this;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public String getComment() {
        return this.comment;
    }

    public AccessReviewDecision comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UUID getDecidedBy() {
        return this.decidedBy;
    }

    public AccessReviewDecision decidedBy(UUID decidedBy) {
        this.setDecidedBy(decidedBy);
        return this;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return this.decidedAt;
    }

    public AccessReviewDecision decidedAt(Instant decidedAt) {
        this.setDecidedAt(decidedAt);
        return this;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public AccessReview getReview() {
        return this.review;
    }

    public void setReview(AccessReview accessReview) {
        this.review = accessReview;
    }

    public AccessReviewDecision review(AccessReview accessReview) {
        this.setReview(accessReview);
        return this;
    }

    public Collaborator getCollaborator() {
        return this.collaborator;
    }

    public void setCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
    }

    public AccessReviewDecision collaborator(Collaborator collaborator) {
        this.setCollaborator(collaborator);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessReviewDecision)) {
            return false;
        }
        return getId() != null && getId().equals(((AccessReviewDecision) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AccessReviewDecision{" +
            "id=" + getId() +
            ", decision='" + getDecision() + "'" +
            ", comment='" + getComment() + "'" +
            ", decidedBy='" + getDecidedBy() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            "}";
    }
}
