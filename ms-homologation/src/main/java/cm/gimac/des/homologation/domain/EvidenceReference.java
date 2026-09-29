package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A EvidenceReference.
 */
@Entity
@Table(name = "evidence_reference")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceReference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 120)
    @Column(name = "document_ref", length = 120, nullable = false)
    private String documentRef;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte", length = 64, nullable = false)
    private String empreinte;

    @NotNull
    @Column(name = "version", nullable = false)
    private Integer version;

    @NotNull
    @Size(max = 60)
    @Column(name = "type_piece", length = 60, nullable = false)
    private String typePiece;

    @NotNull
    @Column(name = "deposant_id", nullable = false)
    private UUID deposantId;

    @NotNull
    @Column(name = "date_depot", nullable = false)
    private Instant dateDepot;

    @NotNull
    @Column(name = "scelle", nullable = false)
    private Boolean scelle;

    @Column(name = "valide")
    private Boolean valide;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "liaison", "referenceOperation" }, allowSetters = true)
    private OperationInstance operation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public EvidenceReference id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentRef() {
        return this.documentRef;
    }

    public EvidenceReference documentRef(String documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getEmpreinte() {
        return this.empreinte;
    }

    public EvidenceReference empreinte(String empreinte) {
        this.setEmpreinte(empreinte);
        return this;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public Integer getVersion() {
        return this.version;
    }

    public EvidenceReference version(Integer version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTypePiece() {
        return this.typePiece;
    }

    public EvidenceReference typePiece(String typePiece) {
        this.setTypePiece(typePiece);
        return this;
    }

    public void setTypePiece(String typePiece) {
        this.typePiece = typePiece;
    }

    public UUID getDeposantId() {
        return this.deposantId;
    }

    public EvidenceReference deposantId(UUID deposantId) {
        this.setDeposantId(deposantId);
        return this;
    }

    public void setDeposantId(UUID deposantId) {
        this.deposantId = deposantId;
    }

    public Instant getDateDepot() {
        return this.dateDepot;
    }

    public EvidenceReference dateDepot(Instant dateDepot) {
        this.setDateDepot(dateDepot);
        return this;
    }

    public void setDateDepot(Instant dateDepot) {
        this.dateDepot = dateDepot;
    }

    public Boolean getScelle() {
        return this.scelle;
    }

    public EvidenceReference scelle(Boolean scelle) {
        this.setScelle(scelle);
        return this;
    }

    public void setScelle(Boolean scelle) {
        this.scelle = scelle;
    }

    public Boolean getValide() {
        return this.valide;
    }

    public EvidenceReference valide(Boolean valide) {
        this.setValide(valide);
        return this;
    }

    public void setValide(Boolean valide) {
        this.valide = valide;
    }

    public OperationInstance getOperation() {
        return this.operation;
    }

    public void setOperation(OperationInstance operationInstance) {
        this.operation = operationInstance;
    }

    public EvidenceReference operation(OperationInstance operationInstance) {
        this.setOperation(operationInstance);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvidenceReference)) {
            return false;
        }
        return getId() != null && getId().equals(((EvidenceReference) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceReference{" +
            "id=" + getId() +
            ", documentRef='" + getDocumentRef() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", version=" + getVersion() +
            ", typePiece='" + getTypePiece() + "'" +
            ", deposantId='" + getDeposantId() + "'" +
            ", dateDepot='" + getDateDepot() + "'" +
            ", scelle='" + getScelle() + "'" +
            ", valide='" + getValide() + "'" +
            "}";
    }
}
