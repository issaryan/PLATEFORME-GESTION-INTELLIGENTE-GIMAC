package cm.gimac.des.participant.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.EscalationContact} entity.
 */
@Schema(
    description = "Contact de l'annuaire d'escalade, par sévérité (S1 à S4) et rang, lu par\nMS-NOTIFICATION et MS-INCIDENT. Délai par défaut : 15 min (D-14)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EscalationContactDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1)
    @Max(value = 4)
    @Schema(description = "Sévérité de 1 (S1) à 4 (S4).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer severity;

    @NotNull
    @Min(value = 1)
    private Integer escalationRank;

    @NotNull
    @Schema(description = "Délai d'acquittement (ISO 8601), PT15M par défaut.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration ackDelay;

    @NotNull
    private Boolean active;

    @NotNull
    private InstitutionDTO institution;

    @NotNull
    private CorrespondentDTO correspondent;

    private CorrespondentDTO deputy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public Integer getEscalationRank() {
        return escalationRank;
    }

    public void setEscalationRank(Integer escalationRank) {
        this.escalationRank = escalationRank;
    }

    public Duration getAckDelay() {
        return ackDelay;
    }

    public void setAckDelay(Duration ackDelay) {
        this.ackDelay = ackDelay;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    public CorrespondentDTO getCorrespondent() {
        return correspondent;
    }

    public void setCorrespondent(CorrespondentDTO correspondent) {
        this.correspondent = correspondent;
    }

    public CorrespondentDTO getDeputy() {
        return deputy;
    }

    public void setDeputy(CorrespondentDTO deputy) {
        this.deputy = deputy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EscalationContactDTO)) {
            return false;
        }

        EscalationContactDTO escalationContactDTO = (EscalationContactDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, escalationContactDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EscalationContactDTO{" +
            "id='" + getId() + "'" +
            ", severity=" + getSeverity() +
            ", escalationRank=" + getEscalationRank() +
            ", ackDelay='" + getAckDelay() + "'" +
            ", active='" + getActive() + "'" +
            ", institution=" + getInstitution() +
            ", correspondent=" + getCorrespondent() +
            ", deputy=" + getDeputy() +
            "}";
    }
}
