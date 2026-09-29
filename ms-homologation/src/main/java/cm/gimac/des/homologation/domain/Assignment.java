package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.Partie;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A Assignment.
 */
@Entity
@Table(name = "assignment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Assignment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "responsable_id", nullable = false)
    private UUID responsableId;

    @Size(max = 200)
    @Column(name = "responsable_nom", length = 200)
    private String responsableNom;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "partie", nullable = false)
    private Partie partie;

    @Size(max = 500)
    @Column(name = "motif", length = 500)
    private String motif;

    @NotNull
    @Column(name = "date_attribution", nullable = false)
    private Instant dateAttribution;

    @NotNull
    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "liaison", "referenceOperation" }, allowSetters = true)
    private OperationInstance operation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Assignment id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResponsableId() {
        return this.responsableId;
    }

    public Assignment responsableId(UUID responsableId) {
        this.setResponsableId(responsableId);
        return this;
    }

    public void setResponsableId(UUID responsableId) {
        this.responsableId = responsableId;
    }

    public String getResponsableNom() {
        return this.responsableNom;
    }

    public Assignment responsableNom(String responsableNom) {
        this.setResponsableNom(responsableNom);
        return this;
    }

    public void setResponsableNom(String responsableNom) {
        this.responsableNom = responsableNom;
    }

    public Partie getPartie() {
        return this.partie;
    }

    public Assignment partie(Partie partie) {
        this.setPartie(partie);
        return this;
    }

    public void setPartie(Partie partie) {
        this.partie = partie;
    }

    public String getMotif() {
        return this.motif;
    }

    public Assignment motif(String motif) {
        this.setMotif(motif);
        return this;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getDateAttribution() {
        return this.dateAttribution;
    }

    public Assignment dateAttribution(Instant dateAttribution) {
        this.setDateAttribution(dateAttribution);
        return this;
    }

    public void setDateAttribution(Instant dateAttribution) {
        this.dateAttribution = dateAttribution;
    }

    public Boolean getActif() {
        return this.actif;
    }

    public Assignment actif(Boolean actif) {
        this.setActif(actif);
        return this;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public OperationInstance getOperation() {
        return this.operation;
    }

    public void setOperation(OperationInstance operationInstance) {
        this.operation = operationInstance;
    }

    public Assignment operation(OperationInstance operationInstance) {
        this.setOperation(operationInstance);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Assignment)) {
            return false;
        }
        return getId() != null && getId().equals(((Assignment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Assignment{" +
            "id=" + getId() +
            ", responsableId='" + getResponsableId() + "'" +
            ", responsableNom='" + getResponsableNom() + "'" +
            ", partie='" + getPartie() + "'" +
            ", motif='" + getMotif() + "'" +
            ", dateAttribution='" + getDateAttribution() + "'" +
            ", actif='" + getActif() + "'" +
            "}";
    }
}
