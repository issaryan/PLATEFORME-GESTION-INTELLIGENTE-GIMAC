package cm.gimac.des.incident.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Matrice de qualification impact × urgence, versionnée (objet-valeur du CC,
 * persisté car paramétrable). Une version déjà appliquée n'est jamais modifiée.
 */
@Entity
@Table(name = "qualification_matrix")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QualificationMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Version, par exemple MQ-2026.1.
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "matrix_version", length = 20, nullable = false, unique = true)
    private String matrixVersion;

    /**
     * Tableau JSON 5×5 des sévérités, indexé [impact][urgence].
     */
    @Lob
    @Column(name = "cells", nullable = false)
    private String cells;

    /**
     * Liste JSON des règles du marqueur réglementaire (seuil Dreg...).
     */
    @Lob
    @Column(name = "regulatory_rules")
    private String regulatoryRules;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public QualificationMatrix id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMatrixVersion() {
        return this.matrixVersion;
    }

    public QualificationMatrix matrixVersion(String matrixVersion) {
        this.setMatrixVersion(matrixVersion);
        return this;
    }

    public void setMatrixVersion(String matrixVersion) {
        this.matrixVersion = matrixVersion;
    }

    public String getCells() {
        return this.cells;
    }

    public QualificationMatrix cells(String cells) {
        this.setCells(cells);
        return this;
    }

    public void setCells(String cells) {
        this.cells = cells;
    }

    public String getRegulatoryRules() {
        return this.regulatoryRules;
    }

    public QualificationMatrix regulatoryRules(String regulatoryRules) {
        this.setRegulatoryRules(regulatoryRules);
        return this;
    }

    public void setRegulatoryRules(String regulatoryRules) {
        this.regulatoryRules = regulatoryRules;
    }

    public Instant getEffectiveFrom() {
        return this.effectiveFrom;
    }

    public QualificationMatrix effectiveFrom(Instant effectiveFrom) {
        this.setEffectiveFrom(effectiveFrom);
        return this;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Boolean getActive() {
        return this.active;
    }

    public QualificationMatrix active(Boolean active) {
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
        if (!(o instanceof QualificationMatrix)) {
            return false;
        }
        return getId() != null && getId().equals(((QualificationMatrix) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QualificationMatrix{" +
            "id=" + getId() +
            ", matrixVersion='" + getMatrixVersion() + "'" +
            ", cells='" + getCells() + "'" +
            ", regulatoryRules='" + getRegulatoryRules() + "'" +
            ", effectiveFrom='" + getEffectiveFrom() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
