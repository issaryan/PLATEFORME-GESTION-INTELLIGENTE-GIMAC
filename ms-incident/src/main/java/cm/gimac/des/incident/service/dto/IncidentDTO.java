package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.Incident} entity.
 */
@Schema(
    description = "Incident : qualification d'un ticket de type INCIDENT (matrice 5×5),\ncritères de clôture (D-09) et rapport circonstancié (D-10)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IncidentDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer impact;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer urgency;

    @NotNull
    @Min(value = 1)
    @Max(value = 25)
    @Schema(description = "Score impact × urgence.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer score;

    @NotNull
    private Severity severity;

    @NotNull
    @Schema(description = "Incident de sécurité : ajoute le RSSI au niveau N3 d'escalade.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean securityIncident;

    private Boolean dataCompromiseSuspected;

    @NotNull
    @Schema(
        description = "Marqueur réglementaire « à déclarer » (formule 6.42) ; jamais retiré sans visa conformité.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean regulatoryFlag;

    @NotNull
    private Instant qualifiedAt;

    @NotNull
    private UUID qualifiedBy;

    @Size(max = 4000)
    @Schema(description = "Critère 1 : cause racine, rattachée à la taxonomie.")
    private String rootCause;

    @Size(max = 100)
    private String rootCauseCategory;

    @Size(max = 100)
    @Schema(description = "Pièce du tiers exigée pour une cause externe (MS-DOCUMENT).")
    private String rootCauseProofRef;

    @Schema(description = "Critère 2 : plan de remédiation ; les actions sont dans RemediationAction.")
    @Lob
    private String remediationPlan;

    @Size(max = 4000)
    @Schema(description = "Critère 3 : test de résolution prouvé, postérieur à la résolution.")
    private String resolutionTest;

    private Boolean resolutionTestPassed;

    private UUID resolutionTestAuthorId;

    private Instant resolutionTestAt;

    @Size(max = 100)
    private String resolutionTestProofRef;

    @Size(max = 100)
    @Schema(description = "Critère 4 : capitalisation (retour d'expérience), exigée pour S1 et S2.")
    private String lessonsLearnedRef;

    @Size(max = 1000)
    @Schema(description = "Mention « sans objet » justifiée, admise pour S3 et S4 seulement.")
    private String lessonsLearnedWaiver;

    @Schema(description = "Rapport circonstancié : exigible au plus tard 5 min après la résolution pour S1 et S2 (D-10).")
    private Instant reportDueAt;

    private Instant reportGeneratedAt;

    @Size(max = 100)
    @Schema(description = "Référence du rapport déposé dans MS-DOCUMENT.")
    private String reportRef;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String reportDigest;

    @Schema(description = "Visa du responsable conformité, exigé si le marqueur est positionné.")
    private UUID complianceVisaBy;

    private Instant complianceVisaAt;

    @Schema(description = "Validation des quatre critères : fin de mesure du MTTR (D-09).")
    private Instant criteriaValidatedAt;

    private UUID validatorId;

    @Schema(description = "Durée brute, de l'ouverture à la résolution, toujours publiée.")
    private Duration rawDuration;

    @Schema(description = "Somme des gels d'attente de tiers.")
    private Duration frozenDuration;

    @Schema(description = "Délai imputable (durée brute moins les gels), gelé à la clôture (D-09).")
    private Duration imputableDelay;

    @Schema(description = "MTTR réel : de l'ouverture à la validation des critères, sans déduire les gels (D-09).")
    private Duration mttr;

    private Boolean takeChargeMet;

    private Boolean restorationMet;

    @NotNull
    @Schema(description = "Un incident précise un ticket de type INCIDENT.")
    private TicketDTO ticket;

    @NotNull
    @Schema(description = "Version de matrice figée à la qualification.")
    private QualificationMatrixDTO matrix;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getImpact() {
        return impact;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public Integer getUrgency() {
        return urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Boolean getSecurityIncident() {
        return securityIncident;
    }

    public void setSecurityIncident(Boolean securityIncident) {
        this.securityIncident = securityIncident;
    }

    public Boolean getDataCompromiseSuspected() {
        return dataCompromiseSuspected;
    }

    public void setDataCompromiseSuspected(Boolean dataCompromiseSuspected) {
        this.dataCompromiseSuspected = dataCompromiseSuspected;
    }

    public Boolean getRegulatoryFlag() {
        return regulatoryFlag;
    }

    public void setRegulatoryFlag(Boolean regulatoryFlag) {
        this.regulatoryFlag = regulatoryFlag;
    }

    public Instant getQualifiedAt() {
        return qualifiedAt;
    }

    public void setQualifiedAt(Instant qualifiedAt) {
        this.qualifiedAt = qualifiedAt;
    }

    public UUID getQualifiedBy() {
        return qualifiedBy;
    }

    public void setQualifiedBy(UUID qualifiedBy) {
        this.qualifiedBy = qualifiedBy;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getRootCauseCategory() {
        return rootCauseCategory;
    }

    public void setRootCauseCategory(String rootCauseCategory) {
        this.rootCauseCategory = rootCauseCategory;
    }

    public String getRootCauseProofRef() {
        return rootCauseProofRef;
    }

    public void setRootCauseProofRef(String rootCauseProofRef) {
        this.rootCauseProofRef = rootCauseProofRef;
    }

    public String getRemediationPlan() {
        return remediationPlan;
    }

    public void setRemediationPlan(String remediationPlan) {
        this.remediationPlan = remediationPlan;
    }

    public String getResolutionTest() {
        return resolutionTest;
    }

    public void setResolutionTest(String resolutionTest) {
        this.resolutionTest = resolutionTest;
    }

    public Boolean getResolutionTestPassed() {
        return resolutionTestPassed;
    }

    public void setResolutionTestPassed(Boolean resolutionTestPassed) {
        this.resolutionTestPassed = resolutionTestPassed;
    }

    public UUID getResolutionTestAuthorId() {
        return resolutionTestAuthorId;
    }

    public void setResolutionTestAuthorId(UUID resolutionTestAuthorId) {
        this.resolutionTestAuthorId = resolutionTestAuthorId;
    }

    public Instant getResolutionTestAt() {
        return resolutionTestAt;
    }

    public void setResolutionTestAt(Instant resolutionTestAt) {
        this.resolutionTestAt = resolutionTestAt;
    }

    public String getResolutionTestProofRef() {
        return resolutionTestProofRef;
    }

    public void setResolutionTestProofRef(String resolutionTestProofRef) {
        this.resolutionTestProofRef = resolutionTestProofRef;
    }

    public String getLessonsLearnedRef() {
        return lessonsLearnedRef;
    }

    public void setLessonsLearnedRef(String lessonsLearnedRef) {
        this.lessonsLearnedRef = lessonsLearnedRef;
    }

    public String getLessonsLearnedWaiver() {
        return lessonsLearnedWaiver;
    }

    public void setLessonsLearnedWaiver(String lessonsLearnedWaiver) {
        this.lessonsLearnedWaiver = lessonsLearnedWaiver;
    }

    public Instant getReportDueAt() {
        return reportDueAt;
    }

    public void setReportDueAt(Instant reportDueAt) {
        this.reportDueAt = reportDueAt;
    }

    public Instant getReportGeneratedAt() {
        return reportGeneratedAt;
    }

    public void setReportGeneratedAt(Instant reportGeneratedAt) {
        this.reportGeneratedAt = reportGeneratedAt;
    }

    public String getReportRef() {
        return reportRef;
    }

    public void setReportRef(String reportRef) {
        this.reportRef = reportRef;
    }

    public String getReportDigest() {
        return reportDigest;
    }

    public void setReportDigest(String reportDigest) {
        this.reportDigest = reportDigest;
    }

    public UUID getComplianceVisaBy() {
        return complianceVisaBy;
    }

    public void setComplianceVisaBy(UUID complianceVisaBy) {
        this.complianceVisaBy = complianceVisaBy;
    }

    public Instant getComplianceVisaAt() {
        return complianceVisaAt;
    }

    public void setComplianceVisaAt(Instant complianceVisaAt) {
        this.complianceVisaAt = complianceVisaAt;
    }

    public Instant getCriteriaValidatedAt() {
        return criteriaValidatedAt;
    }

    public void setCriteriaValidatedAt(Instant criteriaValidatedAt) {
        this.criteriaValidatedAt = criteriaValidatedAt;
    }

    public UUID getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Duration getRawDuration() {
        return rawDuration;
    }

    public void setRawDuration(Duration rawDuration) {
        this.rawDuration = rawDuration;
    }

    public Duration getFrozenDuration() {
        return frozenDuration;
    }

    public void setFrozenDuration(Duration frozenDuration) {
        this.frozenDuration = frozenDuration;
    }

    public Duration getImputableDelay() {
        return imputableDelay;
    }

    public void setImputableDelay(Duration imputableDelay) {
        this.imputableDelay = imputableDelay;
    }

    public Duration getMttr() {
        return mttr;
    }

    public void setMttr(Duration mttr) {
        this.mttr = mttr;
    }

    public Boolean getTakeChargeMet() {
        return takeChargeMet;
    }

    public void setTakeChargeMet(Boolean takeChargeMet) {
        this.takeChargeMet = takeChargeMet;
    }

    public Boolean getRestorationMet() {
        return restorationMet;
    }

    public void setRestorationMet(Boolean restorationMet) {
        this.restorationMet = restorationMet;
    }

    public TicketDTO getTicket() {
        return ticket;
    }

    public void setTicket(TicketDTO ticket) {
        this.ticket = ticket;
    }

    public QualificationMatrixDTO getMatrix() {
        return matrix;
    }

    public void setMatrix(QualificationMatrixDTO matrix) {
        this.matrix = matrix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IncidentDTO)) {
            return false;
        }

        IncidentDTO incidentDTO = (IncidentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, incidentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IncidentDTO{" +
            "id='" + getId() + "'" +
            ", impact=" + getImpact() +
            ", urgency=" + getUrgency() +
            ", score=" + getScore() +
            ", severity='" + getSeverity() + "'" +
            ", securityIncident='" + getSecurityIncident() + "'" +
            ", dataCompromiseSuspected='" + getDataCompromiseSuspected() + "'" +
            ", regulatoryFlag='" + getRegulatoryFlag() + "'" +
            ", qualifiedAt='" + getQualifiedAt() + "'" +
            ", qualifiedBy='" + getQualifiedBy() + "'" +
            ", rootCause='" + getRootCause() + "'" +
            ", rootCauseCategory='" + getRootCauseCategory() + "'" +
            ", rootCauseProofRef='" + getRootCauseProofRef() + "'" +
            ", remediationPlan='" + getRemediationPlan() + "'" +
            ", resolutionTest='" + getResolutionTest() + "'" +
            ", resolutionTestPassed='" + getResolutionTestPassed() + "'" +
            ", resolutionTestAuthorId='" + getResolutionTestAuthorId() + "'" +
            ", resolutionTestAt='" + getResolutionTestAt() + "'" +
            ", resolutionTestProofRef='" + getResolutionTestProofRef() + "'" +
            ", lessonsLearnedRef='" + getLessonsLearnedRef() + "'" +
            ", lessonsLearnedWaiver='" + getLessonsLearnedWaiver() + "'" +
            ", reportDueAt='" + getReportDueAt() + "'" +
            ", reportGeneratedAt='" + getReportGeneratedAt() + "'" +
            ", reportRef='" + getReportRef() + "'" +
            ", reportDigest='" + getReportDigest() + "'" +
            ", complianceVisaBy='" + getComplianceVisaBy() + "'" +
            ", complianceVisaAt='" + getComplianceVisaAt() + "'" +
            ", criteriaValidatedAt='" + getCriteriaValidatedAt() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", rawDuration='" + getRawDuration() + "'" +
            ", frozenDuration='" + getFrozenDuration() + "'" +
            ", imputableDelay='" + getImputableDelay() + "'" +
            ", mttr='" + getMttr() + "'" +
            ", takeChargeMet='" + getTakeChargeMet() + "'" +
            ", restorationMet='" + getRestorationMet() + "'" +
            ", ticket=" + getTicket() +
            ", matrix=" + getMatrix() +
            "}";
    }
}
