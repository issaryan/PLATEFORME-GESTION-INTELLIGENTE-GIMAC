package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.Periodicity;
import cm.gimac.des.reporting.domain.enumeration.ReportState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.PeriodicReport} entity.
 */
@Schema(
    description = "Rapport périodique engendré à partir d'indicateurs figés, rendu en\nPDF/A-3, signé (PAdES) puis déposé en WORM (algorithme 6.59)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PeriodicReportDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    private String serviceCode;

    @NotNull
    private Periodicity periodicity;

    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}(-[0-9]{2}|-W[0-9]{2})$")
    private String period;

    @NotNull
    @Min(value = 1)
    @Schema(
        description = "Numéro de version : une génération manuelle ajoute une version, jamais un remplacement.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer reportVersion;

    @NotNull
    private Boolean manual;

    @Schema(description = "Indicateurs figés : liste JSON des identifiants IndicatorValue.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String frozenIndicators;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256 du document rendu.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentDigest;

    @NotNull
    private Instant generatedAt;

    @Schema(description = "Signataire désigné, responsable du service (MS-IAM).")
    private UUID signerId;

    private Instant signedAt;

    @Schema(description = "Document signé : identifiant dans MS-DOCUMENT.")
    private UUID documentRef;

    private Instant notifiedAt;

    @NotNull
    private ReportState state;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Periodicity getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Periodicity periodicity) {
        this.periodicity = periodicity;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getReportVersion() {
        return reportVersion;
    }

    public void setReportVersion(Integer reportVersion) {
        this.reportVersion = reportVersion;
    }

    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getFrozenIndicators() {
        return frozenIndicators;
    }

    public void setFrozenIndicators(String frozenIndicators) {
        this.frozenIndicators = frozenIndicators;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public UUID getSignerId() {
        return signerId;
    }

    public void setSignerId(UUID signerId) {
        this.signerId = signerId;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(Instant notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public ReportState getState() {
        return state;
    }

    public void setState(ReportState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PeriodicReportDTO)) {
            return false;
        }

        PeriodicReportDTO periodicReportDTO = (PeriodicReportDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, periodicReportDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PeriodicReportDTO{" +
            "id='" + getId() + "'" +
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
