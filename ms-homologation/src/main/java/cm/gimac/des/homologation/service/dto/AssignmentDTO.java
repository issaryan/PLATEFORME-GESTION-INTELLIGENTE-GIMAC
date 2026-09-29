package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.Partie;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.Assignment} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID responsableId;

    @Size(max = 200)
    private String responsableNom;

    @NotNull
    private Partie partie;

    @Size(max = 500)
    private String motif;

    @NotNull
    private Instant dateAttribution;

    @NotNull
    private Boolean actif;

    @NotNull
    private OperationInstanceDTO operation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(UUID responsableId) {
        this.responsableId = responsableId;
    }

    public String getResponsableNom() {
        return responsableNom;
    }

    public void setResponsableNom(String responsableNom) {
        this.responsableNom = responsableNom;
    }

    public Partie getPartie() {
        return partie;
    }

    public void setPartie(Partie partie) {
        this.partie = partie;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getDateAttribution() {
        return dateAttribution;
    }

    public void setDateAttribution(Instant dateAttribution) {
        this.dateAttribution = dateAttribution;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
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
        if (!(o instanceof AssignmentDTO)) {
            return false;
        }

        AssignmentDTO assignmentDTO = (AssignmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, assignmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AssignmentDTO{" +
            "id='" + getId() + "'" +
            ", responsableId='" + getResponsableId() + "'" +
            ", responsableNom='" + getResponsableNom() + "'" +
            ", partie='" + getPartie() + "'" +
            ", motif='" + getMotif() + "'" +
            ", dateAttribution='" + getDateAttribution() + "'" +
            ", actif='" + getActif() + "'" +
            ", operation=" + getOperation() +
            "}";
    }
}
