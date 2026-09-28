package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.SubscribedService} entity.
 */
@Schema(description = "Service souscrit, publié vers MS-HOMOLOGATION (périmètre contractualisé).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SubscribedServiceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Schema(description = "Code du service, par exemple TRANSFERT_COMPTE_A_COMPTE.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceCode;

    @NotNull
    private LocalDate subscribedOn;

    private LocalDate terminatedOn;

    @NotNull
    private SubscriptionState state;

    @NotNull
    private InstitutionDTO institution;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public LocalDate getSubscribedOn() {
        return subscribedOn;
    }

    public void setSubscribedOn(LocalDate subscribedOn) {
        this.subscribedOn = subscribedOn;
    }

    public LocalDate getTerminatedOn() {
        return terminatedOn;
    }

    public void setTerminatedOn(LocalDate terminatedOn) {
        this.terminatedOn = terminatedOn;
    }

    public SubscriptionState getState() {
        return state;
    }

    public void setState(SubscriptionState state) {
        this.state = state;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscribedServiceDTO)) {
            return false;
        }

        SubscribedServiceDTO subscribedServiceDTO = (SubscribedServiceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, subscribedServiceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SubscribedServiceDTO{" +
            "id='" + getId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", subscribedOn='" + getSubscribedOn() + "'" +
            ", terminatedOn='" + getTerminatedOn() + "'" +
            ", state='" + getState() + "'" +
            ", institution=" + getInstitution() +
            "}";
    }
}
