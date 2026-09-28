package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fait mesurable du magasin. La clé de dimension (objet-valeur DimensionKey
 * du CC) est aplatie ; timeKey = date(e.time), jamais la date de réception.
 * Aucun texte libre ni donnée de carte (RG-48, BNF-16).
 */
@Entity
@Table(name = "fact")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Fact implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false)
    private FactDomain domain;

    @NotNull
    @Column(name = "time_key", nullable = false)
    private LocalDate timeKey;

    /**
     * Institution (MS-PARTICIPANT).
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    /**
     * Service du DES : EXPLOITATION, SUPERVISION, HOMOLOGATION, CONFORMITE.
     */
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Column(name = "service_code", length = 30)
    private String serviceCode;

    /**
     * Sévérité ou urgence, par exemple S2.
     */
    @Size(max = 20)
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * Mesures : objet JSON {nom: valeur décimale}, par exemple {\"resolus\": 1}.
     */
    @Lob
    @Column(name = "measures", nullable = false)
    private String measures;

    /**
     * Identifiant CloudEvents de l'événement source, pour un fait événementiel.
     */
    @Column(name = "source_event_id")
    private UUID sourceEventId;

    @NotNull
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /**
     * Fait déclaré remplacé par une version ultérieure de l'état (jamais supprimé).
     */
    @NotNull
    @Column(name = "superseded", nullable = false)
    private Boolean superseded;

    /**
     * Agrégat quotidien issu de la compaction au-delà de 90 jours.
     */
    @NotNull
    @Column(name = "compacted", nullable = false)
    private Boolean compacted;

    /**
     * État déclaré dont provient un fait de domaine ETAT_DECLARE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "supersededBy", "importedStatement" }, allowSetters = true)
    private ImportedStatement statement;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Fact id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FactDomain getDomain() {
        return this.domain;
    }

    public Fact domain(FactDomain domain) {
        this.setDomain(domain);
        return this;
    }

    public void setDomain(FactDomain domain) {
        this.domain = domain;
    }

    public LocalDate getTimeKey() {
        return this.timeKey;
    }

    public Fact timeKey(LocalDate timeKey) {
        this.setTimeKey(timeKey);
        return this;
    }

    public void setTimeKey(LocalDate timeKey) {
        this.timeKey = timeKey;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Fact institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public Fact serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getSeverity() {
        return this.severity;
    }

    public Fact severity(String severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMeasures() {
        return this.measures;
    }

    public Fact measures(String measures) {
        this.setMeasures(measures);
        return this;
    }

    public void setMeasures(String measures) {
        this.measures = measures;
    }

    public UUID getSourceEventId() {
        return this.sourceEventId;
    }

    public Fact sourceEventId(UUID sourceEventId) {
        this.setSourceEventId(sourceEventId);
        return this;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Instant getRecordedAt() {
        return this.recordedAt;
    }

    public Fact recordedAt(Instant recordedAt) {
        this.setRecordedAt(recordedAt);
        return this;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Boolean getSuperseded() {
        return this.superseded;
    }

    public Fact superseded(Boolean superseded) {
        this.setSuperseded(superseded);
        return this;
    }

    public void setSuperseded(Boolean superseded) {
        this.superseded = superseded;
    }

    public Boolean getCompacted() {
        return this.compacted;
    }

    public Fact compacted(Boolean compacted) {
        this.setCompacted(compacted);
        return this;
    }

    public void setCompacted(Boolean compacted) {
        this.compacted = compacted;
    }

    public ImportedStatement getStatement() {
        return this.statement;
    }

    public void setStatement(ImportedStatement importedStatement) {
        this.statement = importedStatement;
    }

    public Fact statement(ImportedStatement importedStatement) {
        this.setStatement(importedStatement);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fact)) {
            return false;
        }
        return getId() != null && getId().equals(((Fact) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Fact{" +
            "id=" + getId() +
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
            "}";
    }
}
