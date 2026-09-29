package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A ClosureRecord.
 */
@Entity
@Table(name = "closure_record")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClosureRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "validateur_id", nullable = false)
    private UUID validateurId;

    @NotNull
    @Column(name = "date_validation", nullable = false)
    private Instant dateValidation;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte_cloture", length = 64, nullable = false)
    private String empreinteCloture;

    @Lob
    @Column(name = "jeton_horodatage")
    private String jetonHorodatage;

    @Column(name = "date_horodatage")
    private Instant dateHorodatage;

    @NotNull
    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Size(max = 500)
    @Column(name = "motif_reouverture", length = 500)
    private String motifReouverture;

    @Column(name = "date_reouverture")
    private Instant dateReouverture;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "liaison", "referenceOperation" }, allowSetters = true)
    private OperationInstance operation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ClosureRecord id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getValidateurId() {
        return this.validateurId;
    }

    public ClosureRecord validateurId(UUID validateurId) {
        this.setValidateurId(validateurId);
        return this;
    }

    public void setValidateurId(UUID validateurId) {
        this.validateurId = validateurId;
    }

    public Instant getDateValidation() {
        return this.dateValidation;
    }

    public ClosureRecord dateValidation(Instant dateValidation) {
        this.setDateValidation(dateValidation);
        return this;
    }

    public void setDateValidation(Instant dateValidation) {
        this.dateValidation = dateValidation;
    }

    public String getEmpreinteCloture() {
        return this.empreinteCloture;
    }

    public ClosureRecord empreinteCloture(String empreinteCloture) {
        this.setEmpreinteCloture(empreinteCloture);
        return this;
    }

    public void setEmpreinteCloture(String empreinteCloture) {
        this.empreinteCloture = empreinteCloture;
    }

    public String getJetonHorodatage() {
        return this.jetonHorodatage;
    }

    public ClosureRecord jetonHorodatage(String jetonHorodatage) {
        this.setJetonHorodatage(jetonHorodatage);
        return this;
    }

    public void setJetonHorodatage(String jetonHorodatage) {
        this.jetonHorodatage = jetonHorodatage;
    }

    public Instant getDateHorodatage() {
        return this.dateHorodatage;
    }

    public ClosureRecord dateHorodatage(Instant dateHorodatage) {
        this.setDateHorodatage(dateHorodatage);
        return this;
    }

    public void setDateHorodatage(Instant dateHorodatage) {
        this.dateHorodatage = dateHorodatage;
    }

    public Boolean getActif() {
        return this.actif;
    }

    public ClosureRecord actif(Boolean actif) {
        this.setActif(actif);
        return this;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public String getMotifReouverture() {
        return this.motifReouverture;
    }

    public ClosureRecord motifReouverture(String motifReouverture) {
        this.setMotifReouverture(motifReouverture);
        return this;
    }

    public void setMotifReouverture(String motifReouverture) {
        this.motifReouverture = motifReouverture;
    }

    public Instant getDateReouverture() {
        return this.dateReouverture;
    }

    public ClosureRecord dateReouverture(Instant dateReouverture) {
        this.setDateReouverture(dateReouverture);
        return this;
    }

    public void setDateReouverture(Instant dateReouverture) {
        this.dateReouverture = dateReouverture;
    }

    public OperationInstance getOperation() {
        return this.operation;
    }

    public void setOperation(OperationInstance operationInstance) {
        this.operation = operationInstance;
    }

    public ClosureRecord operation(OperationInstance operationInstance) {
        this.setOperation(operationInstance);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClosureRecord)) {
            return false;
        }
        return getId() != null && getId().equals(((ClosureRecord) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ClosureRecord{" +
            "id=" + getId() +
            ", validateurId='" + getValidateurId() + "'" +
            ", dateValidation='" + getDateValidation() + "'" +
            ", empreinteCloture='" + getEmpreinteCloture() + "'" +
            ", jetonHorodatage='" + getJetonHorodatage() + "'" +
            ", dateHorodatage='" + getDateHorodatage() + "'" +
            ", actif='" + getActif() + "'" +
            ", motifReouverture='" + getMotifReouverture() + "'" +
            ", dateReouverture='" + getDateReouverture() + "'" +
            "}";
    }
}
