package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.CrisisActivation;
import cm.gimac.des.incident.domain.enumeration.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.ServiceCommitment} entity.
 */
@Schema(description = "Engagement de service par sévérité (tableau 6.42, valeurs paramétrables).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ServiceCommitmentDTO implements Serializable {

    private UUID id;

    @NotNull
    private Severity severity;

    @NotNull
    private Duration takeCharge;

    @NotNull
    private Duration restoration;

    @Schema(
        description = "Paliers d'escalade, liste JSON : durées absolues ISO-8601 pour S1,\nfractions du délai de rétablissement pour S2 à S4.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String escalationSteps;

    @NotNull
    private CrisisActivation crisisActivation;

    @Schema(description = "Période d'observation après résolution (24 h pour S1 et S2, algorithme 6.43).")
    private Duration observationPeriod;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Duration getTakeCharge() {
        return takeCharge;
    }

    public void setTakeCharge(Duration takeCharge) {
        this.takeCharge = takeCharge;
    }

    public Duration getRestoration() {
        return restoration;
    }

    public void setRestoration(Duration restoration) {
        this.restoration = restoration;
    }

    public String getEscalationSteps() {
        return escalationSteps;
    }

    public void setEscalationSteps(String escalationSteps) {
        this.escalationSteps = escalationSteps;
    }

    public CrisisActivation getCrisisActivation() {
        return crisisActivation;
    }

    public void setCrisisActivation(CrisisActivation crisisActivation) {
        this.crisisActivation = crisisActivation;
    }

    public Duration getObservationPeriod() {
        return observationPeriod;
    }

    public void setObservationPeriod(Duration observationPeriod) {
        this.observationPeriod = observationPeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceCommitmentDTO)) {
            return false;
        }

        ServiceCommitmentDTO serviceCommitmentDTO = (ServiceCommitmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, serviceCommitmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ServiceCommitmentDTO{" +
            "id='" + getId() + "'" +
            ", severity='" + getSeverity() + "'" +
            ", takeCharge='" + getTakeCharge() + "'" +
            ", restoration='" + getRestoration() + "'" +
            ", escalationSteps='" + getEscalationSteps() + "'" +
            ", crisisActivation='" + getCrisisActivation() + "'" +
            ", observationPeriod='" + getObservationPeriod() + "'" +
            "}";
    }
}
