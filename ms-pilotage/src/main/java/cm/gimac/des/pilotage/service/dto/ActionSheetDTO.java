package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.PdcaPhase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.ActionSheet} entity.
 */
@Schema(
    description = "Fiche d'action conduite selon le cycle PDCA. Ses actions concrètes sont des\ntâches nominatives d'origine FICHE_ACTION."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ActionSheetDTO implements Serializable {

    private UUID id;

    @NotNull
    private OriginKind originKind;

    @Size(max = 100)
    @Schema(description = "Référence de l'origine, par exemple RSK-2026-011 ou un code d'indicateur.")
    private String originReference;

    @Size(max = 50)
    private String originService;

    @NotNull
    private PdcaPhase phase;

    @NotNull
    @Size(max = 2000)
    private String objective;

    @Size(max = 4000)
    @Schema(description = "Analyse des causes consignée en phase PLANIFIER.")
    private String causeAnalysis;

    @DecimalMin(value = "0")
    @DecimalMax(value = "25")
    @Schema(description = "Objectif Rcible ; par défaut < 10 (borne haute de la zone modérée).")
    private BigDecimal targetScore;

    @NotNull
    @Schema(description = "Responsable : propriétaire du risque (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID ownerId;

    @NotNull
    private LocalDate dueDate;

    @Schema(description = "Fin de la période d'observation de la phase VERIFIER (30 jours par défaut).")
    private LocalDate observationEndDate;

    @Size(max = 4000)
    private String result;

    @NotNull
    @Min(value = 1)
    @Schema(description = "Numéro du cycle PDCA, à partir de 1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer cycle;

    @Schema(description = "Procédure pérennisée en phase AGIR : identifiant du document (MS-DOCUMENT).")
    private UUID procedureDocumentRef;

    private Instant closedAt;

    private RiskDTO risk;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OriginKind getOriginKind() {
        return originKind;
    }

    public void setOriginKind(OriginKind originKind) {
        this.originKind = originKind;
    }

    public String getOriginReference() {
        return originReference;
    }

    public void setOriginReference(String originReference) {
        this.originReference = originReference;
    }

    public String getOriginService() {
        return originService;
    }

    public void setOriginService(String originService) {
        this.originService = originService;
    }

    public PdcaPhase getPhase() {
        return phase;
    }

    public void setPhase(PdcaPhase phase) {
        this.phase = phase;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getCauseAnalysis() {
        return causeAnalysis;
    }

    public void setCauseAnalysis(String causeAnalysis) {
        this.causeAnalysis = causeAnalysis;
    }

    public BigDecimal getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(BigDecimal targetScore) {
        this.targetScore = targetScore;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getObservationEndDate() {
        return observationEndDate;
    }

    public void setObservationEndDate(LocalDate observationEndDate) {
        this.observationEndDate = observationEndDate;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getCycle() {
        return cycle;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public UUID getProcedureDocumentRef() {
        return procedureDocumentRef;
    }

    public void setProcedureDocumentRef(UUID procedureDocumentRef) {
        this.procedureDocumentRef = procedureDocumentRef;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public RiskDTO getRisk() {
        return risk;
    }

    public void setRisk(RiskDTO risk) {
        this.risk = risk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActionSheetDTO)) {
            return false;
        }

        ActionSheetDTO actionSheetDTO = (ActionSheetDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, actionSheetDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ActionSheetDTO{" +
            "id='" + getId() + "'" +
            ", originKind='" + getOriginKind() + "'" +
            ", originReference='" + getOriginReference() + "'" +
            ", originService='" + getOriginService() + "'" +
            ", phase='" + getPhase() + "'" +
            ", objective='" + getObjective() + "'" +
            ", causeAnalysis='" + getCauseAnalysis() + "'" +
            ", targetScore=" + getTargetScore() +
            ", ownerId='" + getOwnerId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", observationEndDate='" + getObservationEndDate() + "'" +
            ", result='" + getResult() + "'" +
            ", cycle=" + getCycle() +
            ", procedureDocumentRef='" + getProcedureDocumentRef() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", risk=" + getRisk() +
            "}";
    }
}
