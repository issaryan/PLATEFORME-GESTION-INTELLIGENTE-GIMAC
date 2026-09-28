package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.Severity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Incident : qualification d'un ticket de type INCIDENT (matrice 5×5),
 * critères de clôture (D-09) et rapport circonstancié (D-10).
 */
@Entity
@Table(name = "incident")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Incident implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "impact", nullable = false)
    private Integer impact;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "urgency", nullable = false)
    private Integer urgency;

    /**
     * Score impact × urgence.
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 25)
    @Column(name = "score", nullable = false)
    private Integer score;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    /**
     * Incident de sécurité : ajoute le RSSI au niveau N3 d'escalade.
     */
    @NotNull
    @Column(name = "security_incident", nullable = false)
    private Boolean securityIncident;

    @Column(name = "data_compromise_suspected")
    private Boolean dataCompromiseSuspected;

    /**
     * Marqueur réglementaire « à déclarer » (formule 6.42) ; jamais retiré sans visa conformité.
     */
    @NotNull
    @Column(name = "regulatory_flag", nullable = false)
    private Boolean regulatoryFlag;

    @NotNull
    @Column(name = "qualified_at", nullable = false)
    private Instant qualifiedAt;

    @NotNull
    @Column(name = "qualified_by", nullable = false)
    private UUID qualifiedBy;

    /**
     * Critère 1 : cause racine, rattachée à la taxonomie.
     */
    @Size(max = 4000)
    @Column(name = "root_cause", length = 4000)
    private String rootCause;

    @Size(max = 100)
    @Column(name = "root_cause_category", length = 100)
    private String rootCauseCategory;

    /**
     * Pièce du tiers exigée pour une cause externe (MS-DOCUMENT).
     */
    @Size(max = 100)
    @Column(name = "root_cause_proof_ref", length = 100)
    private String rootCauseProofRef;

    /**
     * Critère 2 : plan de remédiation ; les actions sont dans RemediationAction.
     */
    @Lob
    @Column(name = "remediation_plan")
    private String remediationPlan;

    /**
     * Critère 3 : test de résolution prouvé, postérieur à la résolution.
     */
    @Size(max = 4000)
    @Column(name = "resolution_test", length = 4000)
    private String resolutionTest;

    @Column(name = "resolution_test_passed")
    private Boolean resolutionTestPassed;

    @Column(name = "resolution_test_author_id")
    private UUID resolutionTestAuthorId;

    @Column(name = "resolution_test_at")
    private Instant resolutionTestAt;

    @Size(max = 100)
    @Column(name = "resolution_test_proof_ref", length = 100)
    private String resolutionTestProofRef;

    /**
     * Critère 4 : capitalisation (retour d'expérience), exigée pour S1 et S2.
     */
    @Size(max = 100)
    @Column(name = "lessons_learned_ref", length = 100)
    private String lessonsLearnedRef;

    /**
     * Mention « sans objet » justifiée, admise pour S3 et S4 seulement.
     */
    @Size(max = 1000)
    @Column(name = "lessons_learned_waiver", length = 1000)
    private String lessonsLearnedWaiver;

    /**
     * Rapport circonstancié : exigible au plus tard 5 min après la résolution pour S1 et S2 (D-10).
     */
    @Column(name = "report_due_at")
    private Instant reportDueAt;

    @Column(name = "report_generated_at")
    private Instant reportGeneratedAt;

    /**
     * Référence du rapport déposé dans MS-DOCUMENT.
     */
    @Size(max = 100)
    @Column(name = "report_ref", length = 100)
    private String reportRef;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "report_digest", length = 64)
    private String reportDigest;

    /**
     * Visa du responsable conformité, exigé si le marqueur est positionné.
     */
    @Column(name = "compliance_visa_by")
    private UUID complianceVisaBy;

    @Column(name = "compliance_visa_at")
    private Instant complianceVisaAt;

    /**
     * Validation des quatre critères : fin de mesure du MTTR (D-09).
     */
    @Column(name = "criteria_validated_at")
    private Instant criteriaValidatedAt;

    @Column(name = "validator_id")
    private UUID validatorId;

    /**
     * Durée brute, de l'ouverture à la résolution, toujours publiée.
     */
    @Column(name = "raw_duration")
    private Duration rawDuration;

    /**
     * Somme des gels d'attente de tiers.
     */
    @Column(name = "frozen_duration")
    private Duration frozenDuration;

    /**
     * Délai imputable (durée brute moins les gels), gelé à la clôture (D-09).
     */
    @Column(name = "imputable_delay")
    private Duration imputableDelay;

    /**
     * MTTR réel : de l'ouverture à la validation des critères, sans déduire les gels (D-09).
     */
    @Column(name = "mttr")
    private Duration mttr;

    @Column(name = "take_charge_met")
    private Boolean takeChargeMet;

    @Column(name = "restoration_met")
    private Boolean restorationMet;

    /**
     * Un incident précise un ticket de type INCIDENT.
     */
    @JsonIgnoreProperties(value = { "relatedTicket", "incident" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Ticket ticket;

    /**
     * Version de matrice figée à la qualification.
     */
    @ManyToOne(optional = false)
    @NotNull
    private QualificationMatrix matrix;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Incident id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getImpact() {
        return this.impact;
    }

    public Incident impact(Integer impact) {
        this.setImpact(impact);
        return this;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public Integer getUrgency() {
        return this.urgency;
    }

    public Incident urgency(Integer urgency) {
        this.setUrgency(urgency);
        return this;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getScore() {
        return this.score;
    }

    public Incident score(Integer score) {
        this.setScore(score);
        return this;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public Incident severity(Severity severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Boolean getSecurityIncident() {
        return this.securityIncident;
    }

    public Incident securityIncident(Boolean securityIncident) {
        this.setSecurityIncident(securityIncident);
        return this;
    }

    public void setSecurityIncident(Boolean securityIncident) {
        this.securityIncident = securityIncident;
    }

    public Boolean getDataCompromiseSuspected() {
        return this.dataCompromiseSuspected;
    }

    public Incident dataCompromiseSuspected(Boolean dataCompromiseSuspected) {
        this.setDataCompromiseSuspected(dataCompromiseSuspected);
        return this;
    }

    public void setDataCompromiseSuspected(Boolean dataCompromiseSuspected) {
        this.dataCompromiseSuspected = dataCompromiseSuspected;
    }

    public Boolean getRegulatoryFlag() {
        return this.regulatoryFlag;
    }

    public Incident regulatoryFlag(Boolean regulatoryFlag) {
        this.setRegulatoryFlag(regulatoryFlag);
        return this;
    }

    public void setRegulatoryFlag(Boolean regulatoryFlag) {
        this.regulatoryFlag = regulatoryFlag;
    }

    public Instant getQualifiedAt() {
        return this.qualifiedAt;
    }

    public Incident qualifiedAt(Instant qualifiedAt) {
        this.setQualifiedAt(qualifiedAt);
        return this;
    }

    public void setQualifiedAt(Instant qualifiedAt) {
        this.qualifiedAt = qualifiedAt;
    }

    public UUID getQualifiedBy() {
        return this.qualifiedBy;
    }

    public Incident qualifiedBy(UUID qualifiedBy) {
        this.setQualifiedBy(qualifiedBy);
        return this;
    }

    public void setQualifiedBy(UUID qualifiedBy) {
        this.qualifiedBy = qualifiedBy;
    }

    public String getRootCause() {
        return this.rootCause;
    }

    public Incident rootCause(String rootCause) {
        this.setRootCause(rootCause);
        return this;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getRootCauseCategory() {
        return this.rootCauseCategory;
    }

    public Incident rootCauseCategory(String rootCauseCategory) {
        this.setRootCauseCategory(rootCauseCategory);
        return this;
    }

    public void setRootCauseCategory(String rootCauseCategory) {
        this.rootCauseCategory = rootCauseCategory;
    }

    public String getRootCauseProofRef() {
        return this.rootCauseProofRef;
    }

    public Incident rootCauseProofRef(String rootCauseProofRef) {
        this.setRootCauseProofRef(rootCauseProofRef);
        return this;
    }

    public void setRootCauseProofRef(String rootCauseProofRef) {
        this.rootCauseProofRef = rootCauseProofRef;
    }

    public String getRemediationPlan() {
        return this.remediationPlan;
    }

    public Incident remediationPlan(String remediationPlan) {
        this.setRemediationPlan(remediationPlan);
        return this;
    }

    public void setRemediationPlan(String remediationPlan) {
        this.remediationPlan = remediationPlan;
    }

    public String getResolutionTest() {
        return this.resolutionTest;
    }

    public Incident resolutionTest(String resolutionTest) {
        this.setResolutionTest(resolutionTest);
        return this;
    }

    public void setResolutionTest(String resolutionTest) {
        this.resolutionTest = resolutionTest;
    }

    public Boolean getResolutionTestPassed() {
        return this.resolutionTestPassed;
    }

    public Incident resolutionTestPassed(Boolean resolutionTestPassed) {
        this.setResolutionTestPassed(resolutionTestPassed);
        return this;
    }

    public void setResolutionTestPassed(Boolean resolutionTestPassed) {
        this.resolutionTestPassed = resolutionTestPassed;
    }

    public UUID getResolutionTestAuthorId() {
        return this.resolutionTestAuthorId;
    }

    public Incident resolutionTestAuthorId(UUID resolutionTestAuthorId) {
        this.setResolutionTestAuthorId(resolutionTestAuthorId);
        return this;
    }

    public void setResolutionTestAuthorId(UUID resolutionTestAuthorId) {
        this.resolutionTestAuthorId = resolutionTestAuthorId;
    }

    public Instant getResolutionTestAt() {
        return this.resolutionTestAt;
    }

    public Incident resolutionTestAt(Instant resolutionTestAt) {
        this.setResolutionTestAt(resolutionTestAt);
        return this;
    }

    public void setResolutionTestAt(Instant resolutionTestAt) {
        this.resolutionTestAt = resolutionTestAt;
    }

    public String getResolutionTestProofRef() {
        return this.resolutionTestProofRef;
    }

    public Incident resolutionTestProofRef(String resolutionTestProofRef) {
        this.setResolutionTestProofRef(resolutionTestProofRef);
        return this;
    }

    public void setResolutionTestProofRef(String resolutionTestProofRef) {
        this.resolutionTestProofRef = resolutionTestProofRef;
    }

    public String getLessonsLearnedRef() {
        return this.lessonsLearnedRef;
    }

    public Incident lessonsLearnedRef(String lessonsLearnedRef) {
        this.setLessonsLearnedRef(lessonsLearnedRef);
        return this;
    }

    public void setLessonsLearnedRef(String lessonsLearnedRef) {
        this.lessonsLearnedRef = lessonsLearnedRef;
    }

    public String getLessonsLearnedWaiver() {
        return this.lessonsLearnedWaiver;
    }

    public Incident lessonsLearnedWaiver(String lessonsLearnedWaiver) {
        this.setLessonsLearnedWaiver(lessonsLearnedWaiver);
        return this;
    }

    public void setLessonsLearnedWaiver(String lessonsLearnedWaiver) {
        this.lessonsLearnedWaiver = lessonsLearnedWaiver;
    }

    public Instant getReportDueAt() {
        return this.reportDueAt;
    }

    public Incident reportDueAt(Instant reportDueAt) {
        this.setReportDueAt(reportDueAt);
        return this;
    }

    public void setReportDueAt(Instant reportDueAt) {
        this.reportDueAt = reportDueAt;
    }

    public Instant getReportGeneratedAt() {
        return this.reportGeneratedAt;
    }

    public Incident reportGeneratedAt(Instant reportGeneratedAt) {
        this.setReportGeneratedAt(reportGeneratedAt);
        return this;
    }

    public void setReportGeneratedAt(Instant reportGeneratedAt) {
        this.reportGeneratedAt = reportGeneratedAt;
    }

    public String getReportRef() {
        return this.reportRef;
    }

    public Incident reportRef(String reportRef) {
        this.setReportRef(reportRef);
        return this;
    }

    public void setReportRef(String reportRef) {
        this.reportRef = reportRef;
    }

    public String getReportDigest() {
        return this.reportDigest;
    }

    public Incident reportDigest(String reportDigest) {
        this.setReportDigest(reportDigest);
        return this;
    }

    public void setReportDigest(String reportDigest) {
        this.reportDigest = reportDigest;
    }

    public UUID getComplianceVisaBy() {
        return this.complianceVisaBy;
    }

    public Incident complianceVisaBy(UUID complianceVisaBy) {
        this.setComplianceVisaBy(complianceVisaBy);
        return this;
    }

    public void setComplianceVisaBy(UUID complianceVisaBy) {
        this.complianceVisaBy = complianceVisaBy;
    }

    public Instant getComplianceVisaAt() {
        return this.complianceVisaAt;
    }

    public Incident complianceVisaAt(Instant complianceVisaAt) {
        this.setComplianceVisaAt(complianceVisaAt);
        return this;
    }

    public void setComplianceVisaAt(Instant complianceVisaAt) {
        this.complianceVisaAt = complianceVisaAt;
    }

    public Instant getCriteriaValidatedAt() {
        return this.criteriaValidatedAt;
    }

    public Incident criteriaValidatedAt(Instant criteriaValidatedAt) {
        this.setCriteriaValidatedAt(criteriaValidatedAt);
        return this;
    }

    public void setCriteriaValidatedAt(Instant criteriaValidatedAt) {
        this.criteriaValidatedAt = criteriaValidatedAt;
    }

    public UUID getValidatorId() {
        return this.validatorId;
    }

    public Incident validatorId(UUID validatorId) {
        this.setValidatorId(validatorId);
        return this;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Duration getRawDuration() {
        return this.rawDuration;
    }

    public Incident rawDuration(Duration rawDuration) {
        this.setRawDuration(rawDuration);
        return this;
    }

    public void setRawDuration(Duration rawDuration) {
        this.rawDuration = rawDuration;
    }

    public Duration getFrozenDuration() {
        return this.frozenDuration;
    }

    public Incident frozenDuration(Duration frozenDuration) {
        this.setFrozenDuration(frozenDuration);
        return this;
    }

    public void setFrozenDuration(Duration frozenDuration) {
        this.frozenDuration = frozenDuration;
    }

    public Duration getImputableDelay() {
        return this.imputableDelay;
    }

    public Incident imputableDelay(Duration imputableDelay) {
        this.setImputableDelay(imputableDelay);
        return this;
    }

    public void setImputableDelay(Duration imputableDelay) {
        this.imputableDelay = imputableDelay;
    }

    public Duration getMttr() {
        return this.mttr;
    }

    public Incident mttr(Duration mttr) {
        this.setMttr(mttr);
        return this;
    }

    public void setMttr(Duration mttr) {
        this.mttr = mttr;
    }

    public Boolean getTakeChargeMet() {
        return this.takeChargeMet;
    }

    public Incident takeChargeMet(Boolean takeChargeMet) {
        this.setTakeChargeMet(takeChargeMet);
        return this;
    }

    public void setTakeChargeMet(Boolean takeChargeMet) {
        this.takeChargeMet = takeChargeMet;
    }

    public Boolean getRestorationMet() {
        return this.restorationMet;
    }

    public Incident restorationMet(Boolean restorationMet) {
        this.setRestorationMet(restorationMet);
        return this;
    }

    public void setRestorationMet(Boolean restorationMet) {
        this.restorationMet = restorationMet;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Incident ticket(Ticket ticket) {
        this.setTicket(ticket);
        return this;
    }

    public QualificationMatrix getMatrix() {
        return this.matrix;
    }

    public void setMatrix(QualificationMatrix qualificationMatrix) {
        this.matrix = qualificationMatrix;
    }

    public Incident matrix(QualificationMatrix qualificationMatrix) {
        this.setMatrix(qualificationMatrix);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Incident)) {
            return false;
        }
        return getId() != null && getId().equals(((Incident) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Incident{" +
            "id=" + getId() +
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
            "}";
    }
}
