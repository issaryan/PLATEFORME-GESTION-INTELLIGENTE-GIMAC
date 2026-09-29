package cm.gimac.des.homologation.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ClosureRecord} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClosureRecordDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID validateurId;

    @NotNull
    private Instant dateValidation;

    @NotNull
    @Size(max = 64)
    private String empreinteCloture;

    @Lob
    private String jetonHorodatage;

    private Instant dateHorodatage;

    @NotNull
    private Boolean actif;

    @Size(max = 500)
    private String motifReouverture;

    private Instant dateReouverture;

    @NotNull
    private OperationInstanceDTO operation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getValidateurId() {
        return validateurId;
    }

    public void setValidateurId(UUID validateurId) {
        this.validateurId = validateurId;
    }

    public Instant getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(Instant dateValidation) {
        this.dateValidation = dateValidation;
    }

    public String getEmpreinteCloture() {
        return empreinteCloture;
    }

    public void setEmpreinteCloture(String empreinteCloture) {
        this.empreinteCloture = empreinteCloture;
    }

    public String getJetonHorodatage() {
        return jetonHorodatage;
    }

    public void setJetonHorodatage(String jetonHorodatage) {
        this.jetonHorodatage = jetonHorodatage;
    }

    public Instant getDateHorodatage() {
        return dateHorodatage;
    }

    public void setDateHorodatage(Instant dateHorodatage) {
        this.dateHorodatage = dateHorodatage;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public String getMotifReouverture() {
        return motifReouverture;
    }

    public void setMotifReouverture(String motifReouverture) {
        this.motifReouverture = motifReouverture;
    }

    public Instant getDateReouverture() {
        return dateReouverture;
    }

    public void setDateReouverture(Instant dateReouverture) {
        this.dateReouverture = dateReouverture;
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
        if (!(o instanceof ClosureRecordDTO)) {
            return false;
        }

        ClosureRecordDTO closureRecordDTO = (ClosureRecordDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, closureRecordDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ClosureRecordDTO{" +
            "id='" + getId() + "'" +
            ", validateurId='" + getValidateurId() + "'" +
            ", dateValidation='" + getDateValidation() + "'" +
            ", empreinteCloture='" + getEmpreinteCloture() + "'" +
            ", jetonHorodatage='" + getJetonHorodatage() + "'" +
            ", dateHorodatage='" + getDateHorodatage() + "'" +
            ", actif='" + getActif() + "'" +
            ", motifReouverture='" + getMotifReouverture() + "'" +
            ", dateReouverture='" + getDateReouverture() + "'" +
            ", operation=" + getOperation() +
            "}";
    }
}
