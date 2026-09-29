package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.Verdict;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.CampaignResult} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CampaignResultDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 80)
    private String campagneId;

    @NotNull
    @Size(max = 80)
    private String executionId;

    @NotNull
    @Size(max = 80)
    private String idScenario;

    @NotNull
    private Verdict verdict;

    @NotNull
    private Instant dateExecution;

    @Size(max = 40)
    private String versionScenario;

    @Size(max = 120)
    private String referenceTrace;

    @NotNull
    @Size(max = 64)
    private String empreinteReponse;

    @NotNull
    private Instant dateReprise;

    @NotNull
    private LiaisonDTO liaison;

    private OperationInstanceDTO operation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getIdScenario() {
        return idScenario;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Instant getDateExecution() {
        return dateExecution;
    }

    public void setDateExecution(Instant dateExecution) {
        this.dateExecution = dateExecution;
    }

    public String getVersionScenario() {
        return versionScenario;
    }

    public void setVersionScenario(String versionScenario) {
        this.versionScenario = versionScenario;
    }

    public String getReferenceTrace() {
        return referenceTrace;
    }

    public void setReferenceTrace(String referenceTrace) {
        this.referenceTrace = referenceTrace;
    }

    public String getEmpreinteReponse() {
        return empreinteReponse;
    }

    public void setEmpreinteReponse(String empreinteReponse) {
        this.empreinteReponse = empreinteReponse;
    }

    public Instant getDateReprise() {
        return dateReprise;
    }

    public void setDateReprise(Instant dateReprise) {
        this.dateReprise = dateReprise;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    public OperationInstanceDTO getOperation() {
        return operation;
    }

    public void setOperation(OperationInstanceDTO operation) {
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampaignResultDTO)) {
            return false;
        }

        CampaignResultDTO campaignResultDTO = (CampaignResultDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, campaignResultDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CampaignResultDTO{" +
            "id='" + getId() + "'" +
            ", campagneId='" + getCampagneId() + "'" +
            ", executionId='" + getExecutionId() + "'" +
            ", idScenario='" + getIdScenario() + "'" +
            ", verdict='" + getVerdict() + "'" +
            ", dateExecution='" + getDateExecution() + "'" +
            ", versionScenario='" + getVersionScenario() + "'" +
            ", referenceTrace='" + getReferenceTrace() + "'" +
            ", empreinteReponse='" + getEmpreinteReponse() + "'" +
            ", dateReprise='" + getDateReprise() + "'" +
            ", liaison=" + getLiaison() +
            ", operation=" + getOperation() +
            "}";
    }
}
