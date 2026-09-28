package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.DepositContract} entity.
 */
@Schema(
    description = "Contrat de dépôt d'une source : format, colonnes, taille, tolérance\nd'horodatage (écart CC complété : paramètres cités par 6.48 et 6.49)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DepositContractDTO implements Serializable {

    private UUID id;

    @NotNull
    private StatementSource source;

    @Schema(description = "Vide : contrat par défaut de la source ; sinon propre à une institution.")
    private UUID institutionId;

    @NotNull
    private StatementFormat format;

    @NotNull
    @Size(max = 1000)
    @Schema(description = "Colonnes obligatoires, séparées par des virgules.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requiredColumns;

    @NotNull
    @Size(max = 20)
    private String encoding;

    @Size(max = 1)
    @Schema(description = "Séparateur pour le format CSV.")
    private String separator;

    @NotNull
    @Min(value = 1L)
    private Long maxSizeBytes;

    @NotNull
    @Schema(description = "Tolérance τ d'horodatage : 120 s par défaut.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration timestampTolerance;

    @NotNull
    private Boolean active;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatementSource getSource() {
        return source;
    }

    public void setSource(StatementSource source) {
        this.source = source;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public StatementFormat getFormat() {
        return format;
    }

    public void setFormat(StatementFormat format) {
        this.format = format;
    }

    public String getRequiredColumns() {
        return requiredColumns;
    }

    public void setRequiredColumns(String requiredColumns) {
        this.requiredColumns = requiredColumns;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Duration getTimestampTolerance() {
        return timestampTolerance;
    }

    public void setTimestampTolerance(Duration timestampTolerance) {
        this.timestampTolerance = timestampTolerance;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DepositContractDTO)) {
            return false;
        }

        DepositContractDTO depositContractDTO = (DepositContractDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, depositContractDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DepositContractDTO{" +
            "id='" + getId() + "'" +
            ", source='" + getSource() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", format='" + getFormat() + "'" +
            ", requiredColumns='" + getRequiredColumns() + "'" +
            ", encoding='" + getEncoding() + "'" +
            ", separator='" + getSeparator() + "'" +
            ", maxSizeBytes=" + getMaxSizeBytes() +
            ", timestampTolerance='" + getTimestampTolerance() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
