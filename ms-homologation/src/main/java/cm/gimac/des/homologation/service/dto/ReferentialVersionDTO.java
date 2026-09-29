package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ReferentialVersion} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferentialVersionDTO implements Serializable {

    private UUID id;

    @NotNull
    private TypeProjet typeProjet;

    @NotNull
    private Integer numero;

    @NotNull
    @Size(max = 200)
    private String libelle;

    @NotNull
    private EtatVersionReferentiel etat;

    @NotNull
    private Instant dateCreation;

    private Instant datePublication;

    @Lob
    private String ordreTopologique;

    private UUID versionSourceId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeProjet getTypeProjet() {
        return typeProjet;
    }

    public void setTypeProjet(TypeProjet typeProjet) {
        this.typeProjet = typeProjet;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public EtatVersionReferentiel getEtat() {
        return etat;
    }

    public void setEtat(EtatVersionReferentiel etat) {
        this.etat = etat;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Instant getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(Instant datePublication) {
        this.datePublication = datePublication;
    }

    public String getOrdreTopologique() {
        return ordreTopologique;
    }

    public void setOrdreTopologique(String ordreTopologique) {
        this.ordreTopologique = ordreTopologique;
    }

    public UUID getVersionSourceId() {
        return versionSourceId;
    }

    public void setVersionSourceId(UUID versionSourceId) {
        this.versionSourceId = versionSourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferentialVersionDTO)) {
            return false;
        }

        ReferentialVersionDTO referentialVersionDTO = (ReferentialVersionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, referentialVersionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferentialVersionDTO{" +
            "id='" + getId() + "'" +
            ", typeProjet='" + getTypeProjet() + "'" +
            ", numero=" + getNumero() +
            ", libelle='" + getLibelle() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateCreation='" + getDateCreation() + "'" +
            ", datePublication='" + getDatePublication() + "'" +
            ", ordreTopologique='" + getOrdreTopologique() + "'" +
            ", versionSourceId='" + getVersionSourceId() + "'" +
            "}";
    }
}
