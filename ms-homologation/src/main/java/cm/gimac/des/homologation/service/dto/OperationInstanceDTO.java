package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.EtatOperation;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.OperationInstance} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperationInstanceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 40)
    private String code;

    @Size(max = 200)
    private String libelle;

    private Segment segment;

    private Partie partie;

    @Size(max = 40)
    private String serviceCode;

    @NotNull
    private EtatOperation etat;

    @NotNull
    private Integer degreEntrant;

    private LocalDate echeance;

    private Instant dateEtat;

    @Lob
    private String grilleReponses;

    @NotNull
    private LiaisonDTO liaison;

    @NotNull
    private ReferenceOperationDTO referenceOperation;

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

    public EtatOperation getEtat() {
        return etat;
    }

    public void setEtat(EtatOperation etat) {
        this.etat = etat;
    }

    public Integer getDegreEntrant() {
        return degreEntrant;
    }

    public void setDegreEntrant(Integer degreEntrant) {
        this.degreEntrant = degreEntrant;
    }

    public LocalDate getEcheance() {
        return echeance;
    }

    public void setEcheance(LocalDate echeance) {
        this.echeance = echeance;
    }

    public Instant getDateEtat() {
        return dateEtat;
    }

    public void setDateEtat(Instant dateEtat) {
        this.dateEtat = dateEtat;
    }

    public String getGrilleReponses() {
        return grilleReponses;
    }

    public void setGrilleReponses(String grilleReponses) {
        this.grilleReponses = grilleReponses;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    public ReferenceOperationDTO getReferenceOperation() {
        return referenceOperation;
    }

    public void setReferenceOperation(ReferenceOperationDTO referenceOperation) {
        this.referenceOperation = referenceOperation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationInstanceDTO)) {
            return false;
        }

        OperationInstanceDTO operationInstanceDTO = (OperationInstanceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, operationInstanceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperationInstanceDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +
            ", segment='" + getSegment() + "'" +
            ", partie='" + getPartie() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", etat='" + getEtat() + "'" +
            ", degreEntrant=" + getDegreEntrant() +
            ", echeance='" + getEcheance() + "'" +
            ", dateEtat='" + getDateEtat() + "'" +
            ", grilleReponses='" + getGrilleReponses() + "'" +
            ", liaison=" + getLiaison() +
            ", referenceOperation=" + getReferenceOperation() +
            "}";
    }
}
