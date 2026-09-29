package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.QualificationScenario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A UnmatchedScenario.
 */
@Entity
@Table(name = "unmatched_scenario")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UnmatchedScenario implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 80)
    @Column(name = "id_scenario", length = 80, nullable = false)
    private String idScenario;

    @Size(max = 80)
    @Column(name = "campagne_id", length = 80)
    private String campagneId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "qualification", nullable = false)
    private QualificationScenario qualification;

    @Size(max = 500)
    @Column(name = "motif", length = 500)
    private String motif;

    @NotNull
    @Column(name = "date_detection", nullable = false)
    private Instant dateDetection;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public UnmatchedScenario id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdScenario() {
        return this.idScenario;
    }

    public UnmatchedScenario idScenario(String idScenario) {
        this.setIdScenario(idScenario);
        return this;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public String getCampagneId() {
        return this.campagneId;
    }

    public UnmatchedScenario campagneId(String campagneId) {
        this.setCampagneId(campagneId);
        return this;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public QualificationScenario getQualification() {
        return this.qualification;
    }

    public UnmatchedScenario qualification(QualificationScenario qualification) {
        this.setQualification(qualification);
        return this;
    }

    public void setQualification(QualificationScenario qualification) {
        this.qualification = qualification;
    }

    public String getMotif() {
        return this.motif;
    }

    public UnmatchedScenario motif(String motif) {
        this.setMotif(motif);
        return this;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getDateDetection() {
        return this.dateDetection;
    }

    public UnmatchedScenario dateDetection(Instant dateDetection) {
        this.setDateDetection(dateDetection);
        return this;
    }

    public void setDateDetection(Instant dateDetection) {
        this.dateDetection = dateDetection;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public UnmatchedScenario liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnmatchedScenario)) {
            return false;
        }
        return getId() != null && getId().equals(((UnmatchedScenario) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UnmatchedScenario{" +
            "id=" + getId() +
            ", idScenario='" + getIdScenario() + "'" +
            ", campagneId='" + getCampagneId() + "'" +
            ", qualification='" + getQualification() + "'" +
            ", motif='" + getMotif() + "'" +
            ", dateDetection='" + getDateDetection() + "'" +
            "}";
    }
}
