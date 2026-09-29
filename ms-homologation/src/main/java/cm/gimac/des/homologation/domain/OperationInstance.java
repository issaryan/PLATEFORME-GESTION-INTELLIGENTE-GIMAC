package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.EtatOperation;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A OperationInstance.
 */
@Entity
@Table(name = "operation_instance")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperationInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 40)
    @Column(name = "code", length = 40, nullable = false)
    private String code;

    @Size(max = 200)
    @Column(name = "libelle", length = 200)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment")
    private Segment segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "partie")
    private Partie partie;

    @Size(max = 40)
    @Column(name = "service_code", length = 40)
    private String serviceCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatOperation etat;

    @NotNull
    @Column(name = "degre_entrant", nullable = false)
    private Integer degreEntrant;

    @Column(name = "echeance")
    private LocalDate echeance;

    @Column(name = "date_etat")
    private Instant dateEtat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grille_reponses", columnDefinition = "jsonb")
    private String grilleReponses;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
    private Liaison liaison;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version" }, allowSetters = true)
    private ReferenceOperation referenceOperation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public OperationInstance id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public OperationInstance code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return this.libelle;
    }

    public OperationInstance libelle(String libelle) {
        this.setLibelle(libelle);
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Segment getSegment() {
        return this.segment;
    }

    public OperationInstance segment(Segment segment) {
        this.setSegment(segment);
        return this;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Partie getPartie() {
        return this.partie;
    }

    public OperationInstance partie(Partie partie) {
        this.setPartie(partie);
        return this;
    }

    public void setPartie(Partie partie) {
        this.partie = partie;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public OperationInstance serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public EtatOperation getEtat() {
        return this.etat;
    }

    public OperationInstance etat(EtatOperation etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatOperation etat) {
        this.etat = etat;
    }

    public Integer getDegreEntrant() {
        return this.degreEntrant;
    }

    public OperationInstance degreEntrant(Integer degreEntrant) {
        this.setDegreEntrant(degreEntrant);
        return this;
    }

    public void setDegreEntrant(Integer degreEntrant) {
        this.degreEntrant = degreEntrant;
    }

    public LocalDate getEcheance() {
        return this.echeance;
    }

    public OperationInstance echeance(LocalDate echeance) {
        this.setEcheance(echeance);
        return this;
    }

    public void setEcheance(LocalDate echeance) {
        this.echeance = echeance;
    }

    public Instant getDateEtat() {
        return this.dateEtat;
    }

    public OperationInstance dateEtat(Instant dateEtat) {
        this.setDateEtat(dateEtat);
        return this;
    }

    public void setDateEtat(Instant dateEtat) {
        this.dateEtat = dateEtat;
    }

    public String getGrilleReponses() {
        return this.grilleReponses;
    }

    public OperationInstance grilleReponses(String grilleReponses) {
        this.setGrilleReponses(grilleReponses);
        return this;
    }

    public void setGrilleReponses(String grilleReponses) {
        this.grilleReponses = grilleReponses;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public OperationInstance liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    public ReferenceOperation getReferenceOperation() {
        return this.referenceOperation;
    }

    public void setReferenceOperation(ReferenceOperation referenceOperation) {
        this.referenceOperation = referenceOperation;
    }

    public OperationInstance referenceOperation(ReferenceOperation referenceOperation) {
        this.setReferenceOperation(referenceOperation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationInstance)) {
            return false;
        }
        return getId() != null && getId().equals(((OperationInstance) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperationInstance{" +
            "id=" + getId() +
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
            "}";
    }
}
