package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.Periodicity;
import cm.gimac.des.reporting.domain.enumeration.ReportState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Rapport périodique engendré à partir d'indicateurs figés, rendu en
 * PDF/A-3, signé (PAdES) puis déposé en WORM (algorithme 6.59).
 */
@Entity
@Table(name = "periodic_report")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PeriodicReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Column(name = "service_code", length = 30, nullable = false)
    private String serviceCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "periodicity", nullable = false)
    private Periodicity periodicity;

    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}(-[0-9]{2}|-W[0-9]{2})$")
    @Column(name = "period", length = 10, nullable = false)
    private String period;

    /**
     * Numéro de version : une génération manuelle ajoute une version, jamais un remplacement.
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "report_version", nullable = false)
    private Integer reportVersion;

    @NotNull
    @Column(name = "manual", nullable = false)
    private Boolean manual;

    /**
     * Indicateurs figés : liste JSON des identifiants IndicatorValue.
     */
    @Lob
    @Column(name = "frozen_indicators", nullable = false)
    private String frozenIndicators;

    /**
     * SHA256 du document rendu.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "content_digest", length = 64, nullable = false)
    private String contentDigest;

    @NotNull
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    /**
     * Signataire désigné, responsable du service (MS-IAM).
     */
    @Column(name = "signer_id")
    private UUID signerId;

    @Column(name = "signed_at")
    private Instant signedAt;

    /**
     * Document signé : identifiant dans MS-DOCUMENT.
     */
    @Column(name = "document_ref")
    private UUID documentRef;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ReportState state;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public PeriodicReport id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public PeriodicReport serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Periodicity getPeriodicity() {
        return this.periodicity;
    }

    public PeriodicReport periodicity(Periodicity periodicity) {
        this.setPeriodicity(periodicity);
        return this;
    }

    public void setPeriodicity(Periodicity periodicity) {
        this.periodicity = periodicity;
    }

    public String getPeriod() {
        return this.period;
    }

    public PeriodicReport period(String period) {
        this.setPeriod(period);
        return this;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getReportVersion() {
        return this.reportVersion;
    }

    public PeriodicReport reportVersion(Integer reportVersion) {
        this.setReportVersion(reportVersion);
        return this;
    }

    public void setReportVersion(Integer reportVersion) {
        this.reportVersion = reportVersion;
    }

    public Boolean getManual() {
        return this.manual;
    }

    public PeriodicReport manual(Boolean manual) {
        this.setManual(manual);
        return this;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getFrozenIndicators() {
        return this.frozenIndicators;
    }

    public PeriodicReport frozenIndicators(String frozenIndicators) {
        this.setFrozenIndicators(frozenIndicators);
        return this;
    }

    public void setFrozenIndicators(String frozenIndicators) {
        this.frozenIndicators = frozenIndicators;
    }

    public String getContentDigest() {
        return this.contentDigest;
    }

    public PeriodicReport contentDigest(String contentDigest) {
        this.setContentDigest(contentDigest);
        return this;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public Instant getGeneratedAt() {
        return this.generatedAt;
    }

    public PeriodicReport generatedAt(Instant generatedAt) {
        this.setGeneratedAt(generatedAt);
        return this;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public UUID getSignerId() {
        return this.signerId;
    }

    public PeriodicReport signerId(UUID signerId) {
        this.setSignerId(signerId);
        return this;
    }

    public void setSignerId(UUID signerId) {
        this.signerId = signerId;
    }

    public Instant getSignedAt() {
        return this.signedAt;
    }

    public PeriodicReport signedAt(Instant signedAt) {
        this.setSignedAt(signedAt);
        return this;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public UUID getDocumentRef() {
        return this.documentRef;
    }

    public PeriodicReport documentRef(UUID documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Instant getNotifiedAt() {
        return this.notifiedAt;
    }

    public PeriodicReport notifiedAt(Instant notifiedAt) {
        this.setNotifiedAt(notifiedAt);
        return this;
    }

    public void setNotifiedAt(Instant notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public ReportState getState() {
        return this.state;
    }

    public PeriodicReport state(ReportState state) {
        this.setState(state);
        return this;
    }

    public void setState(ReportState state) {
        this.state = state;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PeriodicReport)) {
            return false;
        }
        return getId() != null && getId().equals(((PeriodicReport) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PeriodicReport{" +
            "id=" + getId() +
            ", serviceCode='" + getServiceCode() + "'" +
            ", periodicity='" + getPeriodicity() + "'" +
            ", period='" + getPeriod() + "'" +
            ", reportVersion=" + getReportVersion() +
            ", manual='" + getManual() + "'" +
            ", frozenIndicators='" + getFrozenIndicators() + "'" +
            ", contentDigest='" + getContentDigest() + "'" +
            ", generatedAt='" + getGeneratedAt() + "'" +
            ", signerId='" + getSignerId() + "'" +
            ", signedAt='" + getSignedAt() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", notifiedAt='" + getNotifiedAt() + "'" +
            ", state='" + getState() + "'" +
            "}";
    }
}
