package cm.gimac.des.reporting.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Visa du responsable sur un rapport. Écart CC corrigé : le CC le dit
 * « enregistré comme événement distinct » sans le modéliser ; il n'altère
 * pas le document signé.
 */
@Entity
@Table(name = "report_visa")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReportVisa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Responsable qui vise (MS-IAM).
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
    private PeriodicReport report;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReportVisa id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public ReportVisa approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public ReportVisa approvedAt(Instant approvedAt) {
        this.setApprovedAt(approvedAt);
        return this;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getComment() {
        return this.comment;
    }

    public ReportVisa comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PeriodicReport getReport() {
        return this.report;
    }

    public void setReport(PeriodicReport periodicReport) {
        this.report = periodicReport;
    }

    public ReportVisa report(PeriodicReport periodicReport) {
        this.setReport(periodicReport);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportVisa)) {
            return false;
        }
        return getId() != null && getId().equals(((ReportVisa) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReportVisa{" +
            "id=" + getId() +
            ", approverId='" + getApproverId() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", comment='" + getComment() + "'" +
            "}";
    }
}
