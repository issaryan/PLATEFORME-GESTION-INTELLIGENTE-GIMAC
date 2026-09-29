package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.ConditionCode;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.CompletenessCondition} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CompletenessConditionDTO implements Serializable {

    private UUID id;

    @NotNull
    private ConditionCode code;

    @NotNull
    private Boolean satisfaite;

    @Size(max = 60)
    private String source;

    @Size(max = 500)
    private String detail;

    private Instant dateVerification;

    @NotNull
    private GoLiveDossierDTO dossier;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConditionCode getCode() {
        return code;
    }

    public void setCode(ConditionCode code) {
        this.code = code;
    }

    public Boolean getSatisfaite() {
        return satisfaite;
    }

    public void setSatisfaite(Boolean satisfaite) {
        this.satisfaite = satisfaite;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getDateVerification() {
        return dateVerification;
    }

    public void setDateVerification(Instant dateVerification) {
        this.dateVerification = dateVerification;
    }

    public GoLiveDossierDTO getDossier() {
        return dossier;
    }

    public void setDossier(GoLiveDossierDTO dossier) {
        this.dossier = dossier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompletenessConditionDTO)) {
            return false;
        }

        CompletenessConditionDTO completenessConditionDTO = (CompletenessConditionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, completenessConditionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CompletenessConditionDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", satisfaite='" + getSatisfaite() + "'" +
            ", source='" + getSource() + "'" +
            ", detail='" + getDetail() + "'" +
            ", dateVerification='" + getDateVerification() + "'" +
            ", dossier=" + getDossier() +
            "}";
    }
}
