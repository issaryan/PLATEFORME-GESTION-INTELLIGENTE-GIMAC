package cm.gimac.des.certification.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.ChangeRecordSnapshot} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChangeRecordSnapshotDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 120)
    private String reference;

    @NotNull
    @Size(max = 40)
    private String etatApprobation;

    private Instant fenetreDebut;

    private Instant fenetreFin;

    @NotNull
    private Instant dateLecture;

    @Size(max = 64)
    private String empreinteReponse;

    @NotNull
    private GoLiveDossierDTO dossier;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getEtatApprobation() {
        return etatApprobation;
    }

    public void setEtatApprobation(String etatApprobation) {
        this.etatApprobation = etatApprobation;
    }

    public Instant getFenetreDebut() {
        return fenetreDebut;
    }

    public void setFenetreDebut(Instant fenetreDebut) {
        this.fenetreDebut = fenetreDebut;
    }

    public Instant getFenetreFin() {
        return fenetreFin;
    }

    public void setFenetreFin(Instant fenetreFin) {
        this.fenetreFin = fenetreFin;
    }

    public Instant getDateLecture() {
        return dateLecture;
    }

    public void setDateLecture(Instant dateLecture) {
        this.dateLecture = dateLecture;
    }

    public String getEmpreinteReponse() {
        return empreinteReponse;
    }

    public void setEmpreinteReponse(String empreinteReponse) {
        this.empreinteReponse = empreinteReponse;
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
        if (!(o instanceof ChangeRecordSnapshotDTO)) {
            return false;
        }

        ChangeRecordSnapshotDTO changeRecordSnapshotDTO = (ChangeRecordSnapshotDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, changeRecordSnapshotDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChangeRecordSnapshotDTO{" +
            "id='" + getId() + "'" +
            ", reference='" + getReference() + "'" +
            ", etatApprobation='" + getEtatApprobation() + "'" +
            ", fenetreDebut='" + getFenetreDebut() + "'" +
            ", fenetreFin='" + getFenetreFin() + "'" +
            ", dateLecture='" + getDateLecture() + "'" +
            ", empreinteReponse='" + getEmpreinteReponse() + "'" +
            ", dossier=" + getDossier() +
            "}";
    }
}
