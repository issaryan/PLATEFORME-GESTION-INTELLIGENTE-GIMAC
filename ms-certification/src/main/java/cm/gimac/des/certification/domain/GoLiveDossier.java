package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.EtatDossier;
import cm.gimac.des.certification.domain.enumeration.EtatScellement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A GoLiveDossier.
 */
@Entity
@Table(name = "go_live_dossier")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GoLiveDossier implements Serializable {

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
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatDossier etat;

    @Column(name = "date_ouverture")
    private Instant dateOuverture;

    @Column(name = "date_autorisation")
    private Instant dateAutorisation;

    @Column(name = "date_mise_en_service")
    private Instant dateMiseEnService;

    @Column(name = "date_completude")
    private Instant dateCompletude;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_scellement")
    private EtatScellement etatScellement;

    @Size(max = 120)
    @Column(name = "reference_fiche_changement", length = 120)
    private String referenceFicheChangement;

    @JsonIgnoreProperties(value = { "goLiveDossier" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private CertificationCircuit circuit;

    @JsonIgnoreProperties(value = { "dossier" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "dossier")
    private GoLiveReport goLiveReport;

    @JsonIgnoreProperties(value = { "dossier" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "dossier")
    private Attestation attestation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public GoLiveDossier id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiaisonId() {
        return this.liaisonId;
    }

    public GoLiveDossier liaisonId(UUID liaisonId) {
        this.setLiaisonId(liaisonId);
        return this;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public GoLiveDossier institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public EtatDossier getEtat() {
        return this.etat;
    }

    public GoLiveDossier etat(EtatDossier etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatDossier etat) {
        this.etat = etat;
    }

    public Instant getDateOuverture() {
        return this.dateOuverture;
    }

    public GoLiveDossier dateOuverture(Instant dateOuverture) {
        this.setDateOuverture(dateOuverture);
        return this;
    }

    public void setDateOuverture(Instant dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public Instant getDateAutorisation() {
        return this.dateAutorisation;
    }

    public GoLiveDossier dateAutorisation(Instant dateAutorisation) {
        this.setDateAutorisation(dateAutorisation);
        return this;
    }

    public void setDateAutorisation(Instant dateAutorisation) {
        this.dateAutorisation = dateAutorisation;
    }

    public Instant getDateMiseEnService() {
        return this.dateMiseEnService;
    }

    public GoLiveDossier dateMiseEnService(Instant dateMiseEnService) {
        this.setDateMiseEnService(dateMiseEnService);
        return this;
    }

    public void setDateMiseEnService(Instant dateMiseEnService) {
        this.dateMiseEnService = dateMiseEnService;
    }

    public Instant getDateCompletude() {
        return this.dateCompletude;
    }

    public GoLiveDossier dateCompletude(Instant dateCompletude) {
        this.setDateCompletude(dateCompletude);
        return this;
    }

    public void setDateCompletude(Instant dateCompletude) {
        this.dateCompletude = dateCompletude;
    }

    public EtatScellement getEtatScellement() {
        return this.etatScellement;
    }

    public GoLiveDossier etatScellement(EtatScellement etatScellement) {
        this.setEtatScellement(etatScellement);
        return this;
    }

    public void setEtatScellement(EtatScellement etatScellement) {
        this.etatScellement = etatScellement;
    }

    public String getReferenceFicheChangement() {
        return this.referenceFicheChangement;
    }

    public GoLiveDossier referenceFicheChangement(String referenceFicheChangement) {
        this.setReferenceFicheChangement(referenceFicheChangement);
        return this;
    }

    public void setReferenceFicheChangement(String referenceFicheChangement) {
        this.referenceFicheChangement = referenceFicheChangement;
    }

    public CertificationCircuit getCircuit() {
        return this.circuit;
    }

    public void setCircuit(CertificationCircuit certificationCircuit) {
        this.circuit = certificationCircuit;
    }

    public GoLiveDossier circuit(CertificationCircuit certificationCircuit) {
        this.setCircuit(certificationCircuit);
        return this;
    }

    public GoLiveReport getGoLiveReport() {
        return this.goLiveReport;
    }

    public void setGoLiveReport(GoLiveReport goLiveReport) {
        if (this.goLiveReport != null) {
            this.goLiveReport.setDossier(null);
        }
        if (goLiveReport != null) {
            goLiveReport.setDossier(this);
        }
        this.goLiveReport = goLiveReport;
    }

    public GoLiveDossier goLiveReport(GoLiveReport goLiveReport) {
        this.setGoLiveReport(goLiveReport);
        return this;
    }

    public Attestation getAttestation() {
        return this.attestation;
    }

    public void setAttestation(Attestation attestation) {
        if (this.attestation != null) {
            this.attestation.setDossier(null);
        }
        if (attestation != null) {
            attestation.setDossier(this);
        }
        this.attestation = attestation;
    }

    public GoLiveDossier attestation(Attestation attestation) {
        this.setAttestation(attestation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoLiveDossier)) {
            return false;
        }
        return getId() != null && getId().equals(((GoLiveDossier) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GoLiveDossier{" +
            "id=" + getId() +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateOuverture='" + getDateOuverture() + "'" +
            ", dateAutorisation='" + getDateAutorisation() + "'" +
            ", dateMiseEnService='" + getDateMiseEnService() + "'" +
            ", dateCompletude='" + getDateCompletude() + "'" +
            ", etatScellement='" + getEtatScellement() + "'" +
            ", referenceFicheChangement='" + getReferenceFicheChangement() + "'" +
            "}";
    }
}
