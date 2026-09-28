package cm.gimac.des.reporting.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.ReportVisa} entity.
 */
@Schema(
    description = "Visa du responsable sur un rapport. Écart CC corrigé : le CC le dit\n« enregistré comme événement distinct » sans le modéliser ; il n'altère\npas le document signé."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReportVisaDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Responsable qui vise (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID approverId;

    @NotNull
    private Instant approvedAt;

    @Size(max = 1000)
    private String comment;

    @NotNull
    private PeriodicReportDTO report;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PeriodicReportDTO getReport() {
        return report;
    }

    public void setReport(PeriodicReportDTO report) {
        this.report = report;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportVisaDTO)) {
            return false;
        }

        ReportVisaDTO reportVisaDTO = (ReportVisaDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, reportVisaDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReportVisaDTO{" +
            "id='" + getId() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", comment='" + getComment() + "'" +
            ", report=" + getReport() +
            "}";
    }
}
