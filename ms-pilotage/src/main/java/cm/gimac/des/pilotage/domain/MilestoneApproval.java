package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.ApprovalAuthority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Visa de franchissement d'un jalon (RG-46) : un visa par autorité (Projets,
 * Sécurité, DES) ; le jalon ne passe ATTEINT qu'avec les trois.
 */
@Entity
@Table(name = "milestone_approval")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MilestoneApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "authority", nullable = false)
    private ApprovalAuthority authority;

    /**
     * Signataire du visa (MS-IAM).
     */
    @NotNull
    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @NotNull
    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @Size(max = 1000)
    @Column(name = "comment", length = 1000)
    private String comment;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "project", "predecessors", "successors" }, allowSetters = true)
    private Milestone milestone;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MilestoneApproval id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ApprovalAuthority getAuthority() {
        return this.authority;
    }

    public MilestoneApproval authority(ApprovalAuthority authority) {
        this.setAuthority(authority);
        return this;
    }

    public void setAuthority(ApprovalAuthority authority) {
        this.authority = authority;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public MilestoneApproval approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public MilestoneApproval approvedAt(Instant approvedAt) {
        this.setApprovedAt(approvedAt);
        return this;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getComment() {
        return this.comment;
    }

    public MilestoneApproval comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Milestone getMilestone() {
        return this.milestone;
    }

    public void setMilestone(Milestone milestone) {
        this.milestone = milestone;
    }

    public MilestoneApproval milestone(Milestone milestone) {
        this.setMilestone(milestone);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MilestoneApproval)) {
            return false;
        }
        return getId() != null && getId().equals(((MilestoneApproval) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MilestoneApproval{" +
            "id=" + getId() +
            ", authority='" + getAuthority() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", comment='" + getComment() + "'" +
            "}";
    }
}
