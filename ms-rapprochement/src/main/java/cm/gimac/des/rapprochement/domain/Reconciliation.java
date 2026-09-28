package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.ReconciliationState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Rapprochement d'une journée pour une institution ; unicité (journée, institution) posée en base.
 */
@Entity
@Table(name = "reconciliation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Reconciliation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence lisible, par exemple RAP-2026-0913-0042.
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "reference", length = 30, nullable = false, unique = true)
    private String reference;

    @NotNull
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ReconciliationState state;

    /**
     * Tolérance appliquée, conservée pour le déterminisme.
     */
    @Column(name = "timestamp_tolerance")
    private Duration timestampTolerance;

    @Min(value = 0)
    @Column(name = "matched_count")
    private Integer matchedCount;

    @Min(value = 0)
    @Column(name = "discrepancy_count")
    private Integer discrepancyCount;

    /**
     * Nombre d'écarts par typologie, objet JSON.
     */
    @Lob
    @Column(name = "discrepancy_counts_by_type")
    private String discrepancyCountsByType;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * États confrontés : deux, ou trois en extension.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private ImportedStatement switchStatement;

    @ManyToOne(fetch = FetchType.LAZY)
    private ImportedStatement participantStatement;

    @ManyToOne(fetch = FetchType.LAZY)
    private ImportedStatement settlementStatement;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Reconciliation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public Reconciliation reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getBusinessDate() {
        return this.businessDate;
    }

    public Reconciliation businessDate(LocalDate businessDate) {
        this.setBusinessDate(businessDate);
        return this;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Reconciliation institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public ReconciliationState getState() {
        return this.state;
    }

    public Reconciliation state(ReconciliationState state) {
        this.setState(state);
        return this;
    }

    public void setState(ReconciliationState state) {
        this.state = state;
    }

    public Duration getTimestampTolerance() {
        return this.timestampTolerance;
    }

    public Reconciliation timestampTolerance(Duration timestampTolerance) {
        this.setTimestampTolerance(timestampTolerance);
        return this;
    }

    public void setTimestampTolerance(Duration timestampTolerance) {
        this.timestampTolerance = timestampTolerance;
    }

    public Integer getMatchedCount() {
        return this.matchedCount;
    }

    public Reconciliation matchedCount(Integer matchedCount) {
        this.setMatchedCount(matchedCount);
        return this;
    }

    public void setMatchedCount(Integer matchedCount) {
        this.matchedCount = matchedCount;
    }

    public Integer getDiscrepancyCount() {
        return this.discrepancyCount;
    }

    public Reconciliation discrepancyCount(Integer discrepancyCount) {
        this.setDiscrepancyCount(discrepancyCount);
        return this;
    }

    public void setDiscrepancyCount(Integer discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public String getDiscrepancyCountsByType() {
        return this.discrepancyCountsByType;
    }

    public Reconciliation discrepancyCountsByType(String discrepancyCountsByType) {
        this.setDiscrepancyCountsByType(discrepancyCountsByType);
        return this;
    }

    public void setDiscrepancyCountsByType(String discrepancyCountsByType) {
        this.discrepancyCountsByType = discrepancyCountsByType;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public Reconciliation startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }

    public Reconciliation completedAt(Instant completedAt) {
        this.setCompletedAt(completedAt);
        return this;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public UUID getClosedBy() {
        return this.closedBy;
    }

    public Reconciliation closedBy(UUID closedBy) {
        this.setClosedBy(closedBy);
        return this;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public Reconciliation closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public ImportedStatement getSwitchStatement() {
        return this.switchStatement;
    }

    public void setSwitchStatement(ImportedStatement importedStatement) {
        this.switchStatement = importedStatement;
    }

    public Reconciliation switchStatement(ImportedStatement importedStatement) {
        this.setSwitchStatement(importedStatement);
        return this;
    }

    public ImportedStatement getParticipantStatement() {
        return this.participantStatement;
    }

    public void setParticipantStatement(ImportedStatement importedStatement) {
        this.participantStatement = importedStatement;
    }

    public Reconciliation participantStatement(ImportedStatement importedStatement) {
        this.setParticipantStatement(importedStatement);
        return this;
    }

    public ImportedStatement getSettlementStatement() {
        return this.settlementStatement;
    }

    public void setSettlementStatement(ImportedStatement importedStatement) {
        this.settlementStatement = importedStatement;
    }

    public Reconciliation settlementStatement(ImportedStatement importedStatement) {
        this.setSettlementStatement(importedStatement);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reconciliation)) {
            return false;
        }
        return getId() != null && getId().equals(((Reconciliation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Reconciliation{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", businessDate='" + getBusinessDate() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", state='" + getState() + "'" +
            ", timestampTolerance='" + getTimestampTolerance() + "'" +
            ", matchedCount=" + getMatchedCount() +
            ", discrepancyCount=" + getDiscrepancyCount() +
            ", discrepancyCountsByType='" + getDiscrepancyCountsByType() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", completedAt='" + getCompletedAt() + "'" +
            ", closedBy='" + getClosedBy() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            "}";
    }
}
