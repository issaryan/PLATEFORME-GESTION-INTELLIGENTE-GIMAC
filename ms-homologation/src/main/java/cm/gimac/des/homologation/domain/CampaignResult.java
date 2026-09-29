package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.Verdict;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A CampaignResult.
 */
@Entity
@Table(name = "campaign_result")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CampaignResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 80)
    @Column(name = "campagne_id", length = 80, nullable = false)
    private String campagneId;

    @NotNull
    @Size(max = 80)
    @Column(name = "execution_id", length = 80, nullable = false)
    private String executionId;

    @NotNull
    @Size(max = 80)
    @Column(name = "id_scenario", length = 80, nullable = false)
    private String idScenario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false)
    private Verdict verdict;

    @NotNull
    @Column(name = "date_execution", nullable = false)
    private Instant dateExecution;

    @Size(max = 40)
    @Column(name = "version_scenario", length = 40)
    private String versionScenario;

    @Size(max = 120)
    @Column(name = "reference_trace", length = 120)
    private String referenceTrace;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte_reponse", length = 64, nullable = false)
    private String empreinteReponse;

    @NotNull
    @Column(name = "date_reprise", nullable = false)
    private Instant dateReprise;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
    private Liaison liaison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "liaison", "referenceOperation" }, allowSetters = true)
    private OperationInstance operation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CampaignResult id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampagneId() {
        return this.campagneId;
    }

    public CampaignResult campagneId(String campagneId) {
        this.setCampagneId(campagneId);
        return this;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public String getExecutionId() {
        return this.executionId;
    }

    public CampaignResult executionId(String executionId) {
        this.setExecutionId(executionId);
        return this;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getIdScenario() {
        return this.idScenario;
    }

    public CampaignResult idScenario(String idScenario) {
        this.setIdScenario(idScenario);
        return this;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public Verdict getVerdict() {
        return this.verdict;
    }

    public CampaignResult verdict(Verdict verdict) {
        this.setVerdict(verdict);
        return this;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Instant getDateExecution() {
        return this.dateExecution;
    }

    public CampaignResult dateExecution(Instant dateExecution) {
        this.setDateExecution(dateExecution);
        return this;
    }

    public void setDateExecution(Instant dateExecution) {
        this.dateExecution = dateExecution;
    }

    public String getVersionScenario() {
        return this.versionScenario;
    }

    public CampaignResult versionScenario(String versionScenario) {
        this.setVersionScenario(versionScenario);
        return this;
    }

    public void setVersionScenario(String versionScenario) {
        this.versionScenario = versionScenario;
    }

    public String getReferenceTrace() {
        return this.referenceTrace;
    }

    public CampaignResult referenceTrace(String referenceTrace) {
        this.setReferenceTrace(referenceTrace);
        return this;
    }

    public void setReferenceTrace(String referenceTrace) {
        this.referenceTrace = referenceTrace;
    }

    public String getEmpreinteReponse() {
        return this.empreinteReponse;
    }

    public CampaignResult empreinteReponse(String empreinteReponse) {
        this.setEmpreinteReponse(empreinteReponse);
        return this;
    }

    public void setEmpreinteReponse(String empreinteReponse) {
        this.empreinteReponse = empreinteReponse;
    }

    public Instant getDateReprise() {
        return this.dateReprise;
    }

    public CampaignResult dateReprise(Instant dateReprise) {
        this.setDateReprise(dateReprise);
        return this;
    }

    public void setDateReprise(Instant dateReprise) {
        this.dateReprise = dateReprise;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public CampaignResult liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    public OperationInstance getOperation() {
        return this.operation;
    }

    public void setOperation(OperationInstance operationInstance) {
        this.operation = operationInstance;
    }

    public CampaignResult operation(OperationInstance operationInstance) {
        this.setOperation(operationInstance);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampaignResult)) {
            return false;
        }
        return getId() != null && getId().equals(((CampaignResult) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CampaignResult{" +
            "id=" + getId() +
            ", campagneId='" + getCampagneId() + "'" +
            ", executionId='" + getExecutionId() + "'" +
            ", idScenario='" + getIdScenario() + "'" +
            ", verdict='" + getVerdict() + "'" +
            ", dateExecution='" + getDateExecution() + "'" +
            ", versionScenario='" + getVersionScenario() + "'" +
            ", referenceTrace='" + getReferenceTrace() + "'" +
            ", empreinteReponse='" + getEmpreinteReponse() + "'" +
            ", dateReprise='" + getDateReprise() + "'" +
            "}";
    }
}
