package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.ConditionCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A CompletenessCondition.
 */
@Entity
@Table(name = "completeness_condition")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CompletenessCondition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false)
    private ConditionCode code;

    @NotNull
    @Column(name = "satisfaite", nullable = false)
    private Boolean satisfaite;

    @Size(max = 60)
    @Column(name = "source", length = 60)
    private String source;

    @Size(max = 500)
    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "date_verification")
    private Instant dateVerification;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "circuit", "goLiveReport", "attestation" }, allowSetters = true)
    private GoLiveDossier dossier;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CompletenessCondition id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConditionCode getCode() {
        return this.code;
    }

    public CompletenessCondition code(ConditionCode code) {
        this.setCode(code);
        return this;
    }

    public void setCode(ConditionCode code) {
        this.code = code;
    }

    public Boolean getSatisfaite() {
        return this.satisfaite;
    }

    public CompletenessCondition satisfaite(Boolean satisfaite) {
        this.setSatisfaite(satisfaite);
        return this;
    }

    public void setSatisfaite(Boolean satisfaite) {
        this.satisfaite = satisfaite;
    }

    public String getSource() {
        return this.source;
    }

    public CompletenessCondition source(String source) {
        this.setSource(source);
        return this;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDetail() {
        return this.detail;
    }

    public CompletenessCondition detail(String detail) {
        this.setDetail(detail);
        return this;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getDateVerification() {
        return this.dateVerification;
    }

    public CompletenessCondition dateVerification(Instant dateVerification) {
        this.setDateVerification(dateVerification);
        return this;
    }

    public void setDateVerification(Instant dateVerification) {
        this.dateVerification = dateVerification;
    }

    public GoLiveDossier getDossier() {
        return this.dossier;
    }

    public void setDossier(GoLiveDossier goLiveDossier) {
        this.dossier = goLiveDossier;
    }

    public CompletenessCondition dossier(GoLiveDossier goLiveDossier) {
        this.setDossier(goLiveDossier);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompletenessCondition)) {
            return false;
        }
        return getId() != null && getId().equals(((CompletenessCondition) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CompletenessCondition{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", satisfaite='" + getSatisfaite() + "'" +
            ", source='" + getSource() + "'" +
            ", detail='" + getDetail() + "'" +
            ", dateVerification='" + getDateVerification() + "'" +
            "}";
    }
}
