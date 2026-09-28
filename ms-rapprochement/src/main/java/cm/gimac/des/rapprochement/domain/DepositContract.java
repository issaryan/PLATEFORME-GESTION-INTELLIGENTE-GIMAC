package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;

/**
 * Contrat de dépôt d'une source : format, colonnes, taille, tolérance
 * d'horodatage (écart CC complété : paramètres cités par 6.48 et 6.49).
 */
@Entity
@Table(name = "deposit_contract")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DepositContract implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private StatementSource source;

    /**
     * Vide : contrat par défaut de la source ; sinon propre à une institution.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private StatementFormat format;

    /**
     * Colonnes obligatoires, séparées par des virgules.
     */
    @NotNull
    @Size(max = 1000)
    @Column(name = "required_columns", length = 1000, nullable = false)
    private String requiredColumns;

    @NotNull
    @Size(max = 20)
    @Column(name = "encoding", length = 20, nullable = false)
    private String encoding;

    /**
     * Séparateur pour le format CSV.
     */
    @Size(max = 1)
    @Column(name = "separator", length = 1)
    private String separator;

    @NotNull
    @Min(value = 1L)
    @Column(name = "max_size_bytes", nullable = false)
    private Long maxSizeBytes;

    /**
     * Tolérance τ d'horodatage : 120 s par défaut.
     */
    @NotNull
    @Column(name = "timestamp_tolerance", nullable = false)
    private Duration timestampTolerance;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DepositContract id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatementSource getSource() {
        return this.source;
    }

    public DepositContract source(StatementSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(StatementSource source) {
        this.source = source;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public DepositContract institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public StatementFormat getFormat() {
        return this.format;
    }

    public DepositContract format(StatementFormat format) {
        this.setFormat(format);
        return this;
    }

    public void setFormat(StatementFormat format) {
        this.format = format;
    }

    public String getRequiredColumns() {
        return this.requiredColumns;
    }

    public DepositContract requiredColumns(String requiredColumns) {
        this.setRequiredColumns(requiredColumns);
        return this;
    }

    public void setRequiredColumns(String requiredColumns) {
        this.requiredColumns = requiredColumns;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public DepositContract encoding(String encoding) {
        this.setEncoding(encoding);
        return this;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getSeparator() {
        return this.separator;
    }

    public DepositContract separator(String separator) {
        this.setSeparator(separator);
        return this;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public Long getMaxSizeBytes() {
        return this.maxSizeBytes;
    }

    public DepositContract maxSizeBytes(Long maxSizeBytes) {
        this.setMaxSizeBytes(maxSizeBytes);
        return this;
    }

    public void setMaxSizeBytes(Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Duration getTimestampTolerance() {
        return this.timestampTolerance;
    }

    public DepositContract timestampTolerance(Duration timestampTolerance) {
        this.setTimestampTolerance(timestampTolerance);
        return this;
    }

    public void setTimestampTolerance(Duration timestampTolerance) {
        this.timestampTolerance = timestampTolerance;
    }

    public Boolean getActive() {
        return this.active;
    }

    public DepositContract active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DepositContract)) {
            return false;
        }
        return getId() != null && getId().equals(((DepositContract) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DepositContract{" +
            "id=" + getId() +
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
