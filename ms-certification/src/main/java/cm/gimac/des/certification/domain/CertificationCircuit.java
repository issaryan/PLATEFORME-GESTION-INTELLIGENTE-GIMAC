package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A CertificationCircuit.
 */
@Entity
@Table(name = "certification_circuit")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CertificationCircuit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Column(name = "cycle", nullable = false)
    private Integer cycle;

    @NotNull
    @Size(max = 64)
    @Column(name = "socle_empreinte", length = 64, nullable = false)
    private String socleEmpreinte;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatCircuit etat;

    @Column(name = "date_ouverture")
    private Instant dateOuverture;

    @Column(name = "date_fin")
    private Instant dateFin;

    @JsonIgnoreProperties(value = { "circuit", "goLiveReport", "attestation" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "circuit")
    private GoLiveDossier goLiveDossier;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CertificationCircuit id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiaisonId() {
        return this.liaisonId;
    }

    public CertificationCircuit liaisonId(UUID liaisonId) {
        this.setLiaisonId(liaisonId);
        return this;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public CertificationCircuit institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public Integer getCycle() {
        return this.cycle;
    }

    public CertificationCircuit cycle(Integer cycle) {
        this.setCycle(cycle);
        return this;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public String getSocleEmpreinte() {
        return this.socleEmpreinte;
    }

    public CertificationCircuit socleEmpreinte(String socleEmpreinte) {
        this.setSocleEmpreinte(socleEmpreinte);
        return this;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public EtatCircuit getEtat() {
        return this.etat;
    }

    public CertificationCircuit etat(EtatCircuit etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatCircuit etat) {
        this.etat = etat;
    }

    public Instant getDateOuverture() {
        return this.dateOuverture;
    }

    public CertificationCircuit dateOuverture(Instant dateOuverture) {
        this.setDateOuverture(dateOuverture);
        return this;
    }

    public void setDateOuverture(Instant dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public Instant getDateFin() {
        return this.dateFin;
    }

    public CertificationCircuit dateFin(Instant dateFin) {
        this.setDateFin(dateFin);
        return this;
    }

    public void setDateFin(Instant dateFin) {
        this.dateFin = dateFin;
    }

    public GoLiveDossier getGoLiveDossier() {
        return this.goLiveDossier;
    }

    public void setGoLiveDossier(GoLiveDossier goLiveDossier) {
        if (this.goLiveDossier != null) {
            this.goLiveDossier.setCircuit(null);
        }
        if (goLiveDossier != null) {
            goLiveDossier.setCircuit(this);
        }
        this.goLiveDossier = goLiveDossier;
    }

    public CertificationCircuit goLiveDossier(GoLiveDossier goLiveDossier) {
        this.setGoLiveDossier(goLiveDossier);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationCircuit)) {
            return false;
        }
        return getId() != null && getId().equals(((CertificationCircuit) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CertificationCircuit{" +
            "id=" + getId() +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", cycle=" + getCycle() +
            ", socleEmpreinte='" + getSocleEmpreinte() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateOuverture='" + getDateOuverture() + "'" +
            ", dateFin='" + getDateFin() + "'" +
            "}";
    }
}
