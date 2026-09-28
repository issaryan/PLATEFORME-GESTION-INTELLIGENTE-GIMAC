package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.ReconciliationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.Reconciliation} entity.
 */
@Schema(description = "Rapprochement d'une journée pour une institution ; unicité (journée, institution) posée en base.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReconciliationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Schema(description = "Référence lisible, par exemple RAP-2026-0913-0042.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reference;

    @NotNull
    private LocalDate businessDate;

    @NotNull
    private UUID institutionId;

    @NotNull
    private ReconciliationState state;

    @Schema(description = "Tolérance appliquée, conservée pour le déterminisme.")
    private Duration timestampTolerance;

    @Min(value = 0)
    private Integer matchedCount;

    @Min(value = 0)
    private Integer discrepancyCount;

    @Schema(description = "Nombre d'écarts par typologie, objet JSON.")
    @Lob
    private String discrepancyCountsByType;

    private Instant startedAt;

    private Instant completedAt;

    private UUID closedBy;

    private Instant closedAt;

    @Schema(description = "États confrontés : deux, ou trois en extension.")
    private ImportedStatementDTO switchStatement;

    private ImportedStatementDTO participantStatement;

    private ImportedStatementDTO settlementStatement;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public ReconciliationState getState() {
        return state;
    }

    public void setState(ReconciliationState state) {
        this.state = state;
    }

    public Duration getTimestampTolerance() {
        return timestampTolerance;
    }

    public void setTimestampTolerance(Duration timestampTolerance) {
        this.timestampTolerance = timestampTolerance;
    }

    public Integer getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(Integer matchedCount) {
        this.matchedCount = matchedCount;
    }

    public Integer getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(Integer discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public String getDiscrepancyCountsByType() {
        return discrepancyCountsByType;
    }

    public void setDiscrepancyCountsByType(String discrepancyCountsByType) {
        this.discrepancyCountsByType = discrepancyCountsByType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public ImportedStatementDTO getSwitchStatement() {
        return switchStatement;
    }

    public void setSwitchStatement(ImportedStatementDTO switchStatement) {
        this.switchStatement = switchStatement;
    }

    public ImportedStatementDTO getParticipantStatement() {
        return participantStatement;
    }

    public void setParticipantStatement(ImportedStatementDTO participantStatement) {
        this.participantStatement = participantStatement;
    }

    public ImportedStatementDTO getSettlementStatement() {
        return settlementStatement;
    }

    public void setSettlementStatement(ImportedStatementDTO settlementStatement) {
        this.settlementStatement = settlementStatement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReconciliationDTO)) {
            return false;
        }

        ReconciliationDTO reconciliationDTO = (ReconciliationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, reconciliationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReconciliationDTO{" +
            "id='" + getId() + "'" +
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
            ", switchStatement=" + getSwitchStatement() +
            ", participantStatement=" + getParticipantStatement() +
            ", settlementStatement=" + getSettlementStatement() +
            "}";
    }
}
