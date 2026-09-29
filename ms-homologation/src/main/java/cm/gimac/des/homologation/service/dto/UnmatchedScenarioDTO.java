package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.QualificationScenario;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.UnmatchedScenario} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UnmatchedScenarioDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 80)
    private String idScenario;

    @Size(max = 80)
    private String campagneId;

    @NotNull
    private QualificationScenario qualification;

    @Size(max = 500)
    private String motif;

    @NotNull
    private Instant dateDetection;

    @NotNull
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdScenario() {
        return idScenario;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public QualificationScenario getQualification() {
        return qualification;
    }

    public void setQualification(QualificationScenario qualification) {
        this.qualification = qualification;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getDateDetection() {
        return dateDetection;
    }

    public void setDateDetection(Instant dateDetection) {
        this.dateDetection = dateDetection;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnmatchedScenarioDTO)) {
            return false;
        }

        UnmatchedScenarioDTO unmatchedScenarioDTO = (UnmatchedScenarioDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, unmatchedScenarioDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UnmatchedScenarioDTO{" +
            "id='" + getId() + "'" +
            ", idScenario='" + getIdScenario() + "'" +
            ", campagneId='" + getCampagneId() + "'" +
            ", qualification='" + getQualification() + "'" +
            ", motif='" + getMotif() + "'" +
            ", dateDetection='" + getDateDetection() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
