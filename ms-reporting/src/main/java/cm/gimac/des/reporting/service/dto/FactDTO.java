package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.Fact} entity.
 */
@Schema(
    description = "Fait mesurable du magasin. La clé de dimension (objet-valeur DimensionKey\ndu CC) est aplatie ; timeKey = date(e.time), jamais la date de réception.\nAucun texte libre ni donnée de carte (RG-48, BNF-16)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FactDTO implements Serializable {

    private UUID id;

    @NotNull
    private FactDomain domain;

    @NotNull
    private LocalDate timeKey;

    @Schema(description = "Institution (MS-PARTICIPANT).")
    private UUID institutionId;

    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Schema(description = "Service du DES : EXPLOITATION, SUPERVISION, HOMOLOGATION, CONFORMITE.")
    private String serviceCode;

    @Size(max = 20)
    @Schema(description = "Sévérité ou urgence, par exemple S2.")
    private String severity;

    @Schema(
        description = "Mesures : objet JSON {nom: valeur décimale}, par exemple {\"resolus\": 1}.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String measures;

    @Schema(description = "Identifiant CloudEvents de l'événement source, pour un fait événementiel.")
    private UUID sourceEventId;

    @NotNull
    private Instant recordedAt;

    @NotNull
    @Schema(
        description = "Fait déclaré remplacé par une version ultérieure de l'état (jamais supprimé).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean superseded;

    @NotNull
    @Schema(description = "Agrégat quotidien issu de la compaction au-delà de 90 jours.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean compacted;

    @Schema(description = "État déclaré dont provient un fait de domaine ETAT_DECLARE.")
    private ImportedStatementDTO statement;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FactDomain getDomain() {
        return domain;
    }

    public void setDomain(FactDomain domain) {
        this.domain = domain;
    }

    public LocalDate getTimeKey() {
        return timeKey;
    }

    public void setTimeKey(LocalDate timeKey) {
        this.timeKey = timeKey;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMeasures() {
        return measures;
    }

    public void setMeasures(String measures) {
        this.measures = measures;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Boolean getSuperseded() {
        return superseded;
    }

    public void setSuperseded(Boolean superseded) {
        this.superseded = superseded;
    }

    public Boolean getCompacted() {
        return compacted;
    }

    public void setCompacted(Boolean compacted) {
        this.compacted = compacted;
    }

    public ImportedStatementDTO getStatement() {
        return statement;
    }

    public void setStatement(ImportedStatementDTO statement) {
        this.statement = statement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FactDTO)) {
            return false;
        }

        FactDTO factDTO = (FactDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, factDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FactDTO{" +
            "id='" + getId() + "'" +
            ", domain='" + getDomain() + "'" +
            ", timeKey='" + getTimeKey() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", severity='" + getSeverity() + "'" +
            ", measures='" + getMeasures() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            ", superseded='" + getSuperseded() + "'" +
            ", compacted='" + getCompacted() + "'" +
            ", statement=" + getStatement() +
            "}";
    }
}
