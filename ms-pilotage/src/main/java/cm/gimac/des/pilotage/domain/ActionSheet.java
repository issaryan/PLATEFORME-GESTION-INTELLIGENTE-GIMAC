package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.PdcaPhase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fiche d'action conduite selon le cycle PDCA. Ses actions concrètes sont des
 * tâches nominatives d'origine FICHE_ACTION.
 */
@Entity
@Table(name = "action_sheet")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ActionSheet implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_kind", nullable = false)
    private OriginKind originKind;

    /**
     * Référence de l'origine, par exemple RSK-2026-011 ou un code d'indicateur.
     */
    @Size(max = 100)
    @Column(name = "origin_reference", length = 100)
    private String originReference;

    @Size(max = 50)
    @Column(name = "origin_service", length = 50)
    private String originService;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private PdcaPhase phase;

    @NotNull
    @Size(max = 2000)
    @Column(name = "objective", length = 2000, nullable = false)
    private String objective;

    /**
     * Analyse des causes consignée en phase PLANIFIER.
     */
    @Size(max = 4000)
    @Column(name = "cause_analysis", length = 4000)
    private String causeAnalysis;

    /**
     * Objectif Rcible ; par défaut < 10 (borne haute de la zone modérée).
     */
    @DecimalMin(value = "0")
    @DecimalMax(value = "25")
    @Column(name = "target_score", precision = 21, scale = 2)
    private BigDecimal targetScore;

    /**
     * Responsable : propriétaire du risque (MS-IAM).
     */
    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Fin de la période d'observation de la phase VERIFIER (30 jours par défaut).
     */
    @Column(name = "observation_end_date")
    private LocalDate observationEndDate;

    @Size(max = 4000)
    @Column(name = "result", length = 4000)
    private String result;

    /**
     * Numéro du cycle PDCA, à partir de 1.
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "cycle", nullable = false)
    private Integer cycle;

    /**
     * Procédure pérennisée en phase AGIR : identifiant du document (MS-DOCUMENT).
     */
    @Column(name = "procedure_document_ref")
    private UUID procedureDocumentRef;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Risk risk;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ActionSheet id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OriginKind getOriginKind() {
        return this.originKind;
    }

    public ActionSheet originKind(OriginKind originKind) {
        this.setOriginKind(originKind);
        return this;
    }

    public void setOriginKind(OriginKind originKind) {
        this.originKind = originKind;
    }

    public String getOriginReference() {
        return this.originReference;
    }

    public ActionSheet originReference(String originReference) {
        this.setOriginReference(originReference);
        return this;
    }

    public void setOriginReference(String originReference) {
        this.originReference = originReference;
    }

    public String getOriginService() {
        return this.originService;
    }

    public ActionSheet originService(String originService) {
        this.setOriginService(originService);
        return this;
    }

    public void setOriginService(String originService) {
        this.originService = originService;
    }

    public PdcaPhase getPhase() {
        return this.phase;
    }

    public ActionSheet phase(PdcaPhase phase) {
        this.setPhase(phase);
        return this;
    }

    public void setPhase(PdcaPhase phase) {
        this.phase = phase;
    }

    public String getObjective() {
        return this.objective;
    }

    public ActionSheet objective(String objective) {
        this.setObjective(objective);
        return this;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getCauseAnalysis() {
        return this.causeAnalysis;
    }

    public ActionSheet causeAnalysis(String causeAnalysis) {
        this.setCauseAnalysis(causeAnalysis);
        return this;
    }

    public void setCauseAnalysis(String causeAnalysis) {
        this.causeAnalysis = causeAnalysis;
    }

    public BigDecimal getTargetScore() {
        return this.targetScore;
    }

    public ActionSheet targetScore(BigDecimal targetScore) {
        this.setTargetScore(targetScore);
        return this;
    }

    public void setTargetScore(BigDecimal targetScore) {
        this.targetScore = targetScore;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public ActionSheet ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public ActionSheet dueDate(LocalDate dueDate) {
        this.setDueDate(dueDate);
        return this;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getObservationEndDate() {
        return this.observationEndDate;
    }

    public ActionSheet observationEndDate(LocalDate observationEndDate) {
        this.setObservationEndDate(observationEndDate);
        return this;
    }

    public void setObservationEndDate(LocalDate observationEndDate) {
        this.observationEndDate = observationEndDate;
    }

    public String getResult() {
        return this.result;
    }

    public ActionSheet result(String result) {
        this.setResult(result);
        return this;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getCycle() {
        return this.cycle;
    }

    public ActionSheet cycle(Integer cycle) {
        this.setCycle(cycle);
        return this;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public UUID getProcedureDocumentRef() {
        return this.procedureDocumentRef;
    }

    public ActionSheet procedureDocumentRef(UUID procedureDocumentRef) {
        this.setProcedureDocumentRef(procedureDocumentRef);
        return this;
    }

    public void setProcedureDocumentRef(UUID procedureDocumentRef) {
        this.procedureDocumentRef = procedureDocumentRef;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public ActionSheet closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Risk getRisk() {
        return this.risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public ActionSheet risk(Risk risk) {
        this.setRisk(risk);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActionSheet)) {
            return false;
        }
        return getId() != null && getId().equals(((ActionSheet) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ActionSheet{" +
            "id=" + getId() +
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
            "}";
    }
}
