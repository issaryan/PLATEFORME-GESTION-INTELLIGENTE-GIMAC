package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ReferenceOperation} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceOperationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 40)
    private String code;

    @NotNull
    @Size(max = 200)
    private String libelle;

    @NotNull
    @Size(max = 60)
    private String phase;

    @NotNull
    private Segment segment;

    @NotNull
    private Partie partie;

    @Size(max = 40)
    private String serviceCode;

    @NotNull
    private NatureCouverture natureCouverture;

    @Lob
    private String preuveAttendue;

    @Size(max = 80)
    private String idScenario;

    @Lob
    private String predecesseurs;

    @Size(max = 60)
    private String fonctionResponsable;

    @Lob
    private String grille;

    @NotNull
    private ReferentialVersionDTO version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Partie getPartie() {
        return partie;
    }

    public void setPartie(Partie partie) {
        this.partie = partie;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public NatureCouverture getNatureCouverture() {
        return natureCouverture;
    }

    public void setNatureCouverture(NatureCouverture natureCouverture) {
        this.natureCouverture = natureCouverture;
    }

    public String getPreuveAttendue() {
        return preuveAttendue;
    }

    public void setPreuveAttendue(String preuveAttendue) {
        this.preuveAttendue = preuveAttendue;
    }

    public String getIdScenario() {
        return idScenario;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public String getPredecesseurs() {
        return predecesseurs;
    }

    public void setPredecesseurs(String predecesseurs) {
        this.predecesseurs = predecesseurs;
    }

    public String getFonctionResponsable() {
        return fonctionResponsable;
    }

    public void setFonctionResponsable(String fonctionResponsable) {
        this.fonctionResponsable = fonctionResponsable;
    }

    public String getGrille() {
        return grille;
    }

    public void setGrille(String grille) {
        this.grille = grille;
    }

    public ReferentialVersionDTO getVersion() {
        return version;
    }

    public void setVersion(ReferentialVersionDTO version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceOperationDTO)) {
            return false;
        }

        ReferenceOperationDTO referenceOperationDTO = (ReferenceOperationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, referenceOperationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceOperationDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +
            ", phase='" + getPhase() + "'" +
            ", segment='" + getSegment() + "'" +
            ", partie='" + getPartie() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", natureCouverture='" + getNatureCouverture() + "'" +
            ", preuveAttendue='" + getPreuveAttendue() + "'" +
            ", idScenario='" + getIdScenario() + "'" +
            ", predecesseurs='" + getPredecesseurs() + "'" +
            ", fonctionResponsable='" + getFonctionResponsable() + "'" +
            ", grille='" + getGrille() + "'" +
            ", version=" + getVersion() +
            "}";
    }
}
