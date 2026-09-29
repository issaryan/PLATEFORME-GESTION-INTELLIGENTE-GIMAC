package cm.gimac.des.certification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A ChangeRecordSnapshot.
 */
@Entity
@Table(name = "change_record_snapshot")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChangeRecordSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 120)
    @Column(name = "reference", length = 120, nullable = false)
    private String reference;

    @NotNull
    @Size(max = 40)
    @Column(name = "etat_approbation", length = 40, nullable = false)
    private String etatApprobation;

    @Column(name = "fenetre_debut")
    private Instant fenetreDebut;

    @Column(name = "fenetre_fin")
    private Instant fenetreFin;

    @NotNull
    @Column(name = "date_lecture", nullable = false)
    private Instant dateLecture;

    @Size(max = 64)
    @Column(name = "empreinte_reponse", length = 64)
    private String empreinteReponse;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "circuit", "goLiveReport", "attestation" }, allowSetters = true)
    private GoLiveDossier dossier;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ChangeRecordSnapshot id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public ChangeRecordSnapshot reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getEtatApprobation() {
        return this.etatApprobation;
    }

    public ChangeRecordSnapshot etatApprobation(String etatApprobation) {
        this.setEtatApprobation(etatApprobation);
        return this;
    }

    public void setEtatApprobation(String etatApprobation) {
        this.etatApprobation = etatApprobation;
    }

    public Instant getFenetreDebut() {
        return this.fenetreDebut;
    }

    public ChangeRecordSnapshot fenetreDebut(Instant fenetreDebut) {
        this.setFenetreDebut(fenetreDebut);
        return this;
    }

    public void setFenetreDebut(Instant fenetreDebut) {
        this.fenetreDebut = fenetreDebut;
    }

    public Instant getFenetreFin() {
        return this.fenetreFin;
    }

    public ChangeRecordSnapshot fenetreFin(Instant fenetreFin) {
        this.setFenetreFin(fenetreFin);
        return this;
    }

    public void setFenetreFin(Instant fenetreFin) {
        this.fenetreFin = fenetreFin;
    }

    public Instant getDateLecture() {
        return this.dateLecture;
    }

    public ChangeRecordSnapshot dateLecture(Instant dateLecture) {
        this.setDateLecture(dateLecture);
        return this;
    }

    public void setDateLecture(Instant dateLecture) {
        this.dateLecture = dateLecture;
    }

    public String getEmpreinteReponse() {
        return this.empreinteReponse;
    }

    public ChangeRecordSnapshot empreinteReponse(String empreinteReponse) {
        this.setEmpreinteReponse(empreinteReponse);
        return this;
    }

    public void setEmpreinteReponse(String empreinteReponse) {
        this.empreinteReponse = empreinteReponse;
    }

    public GoLiveDossier getDossier() {
        return this.dossier;
    }

    public void setDossier(GoLiveDossier goLiveDossier) {
        this.dossier = goLiveDossier;
    }

    public ChangeRecordSnapshot dossier(GoLiveDossier goLiveDossier) {
        this.setDossier(goLiveDossier);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeRecordSnapshot)) {
            return false;
        }
        return getId() != null && getId().equals(((ChangeRecordSnapshot) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChangeRecordSnapshot{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", etatApprobation='" + getEtatApprobation() + "'" +
            ", fenetreDebut='" + getFenetreDebut() + "'" +
            ", fenetreFin='" + getFenetreFin() + "'" +
            ", dateLecture='" + getDateLecture() + "'" +
            ", empreinteReponse='" + getEmpreinteReponse() + "'" +
            "}";
    }
}
