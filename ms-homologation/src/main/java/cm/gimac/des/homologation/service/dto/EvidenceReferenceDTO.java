package cm.gimac.des.homologation.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.EvidenceReference} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceReferenceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 120)
    private String documentRef;

    @NotNull
    @Size(max = 64)
    private String empreinte;

    @NotNull
    private Integer version;

    @NotNull
    @Size(max = 60)
    private String typePiece;

    @NotNull
    private UUID deposantId;

    @NotNull
    private Instant dateDepot;

    @NotNull
    private Boolean scelle;

    private Boolean valide;

    @NotNull
    private OperationInstanceDTO operation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTypePiece() {
        return typePiece;
    }

    public void setTypePiece(String typePiece) {
        this.typePiece = typePiece;
    }

    public UUID getDeposantId() {
        return deposantId;
    }

    public void setDeposantId(UUID deposantId) {
        this.deposantId = deposantId;
    }

    public Instant getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(Instant dateDepot) {
        this.dateDepot = dateDepot;
    }

    public Boolean getScelle() {
        return scelle;
    }

    public void setScelle(Boolean scelle) {
        this.scelle = scelle;
    }

    public Boolean getValide() {
        return valide;
    }

    public void setValide(Boolean valide) {
        this.valide = valide;
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
        if (!(o instanceof EvidenceReferenceDTO)) {
            return false;
        }

        EvidenceReferenceDTO evidenceReferenceDTO = (EvidenceReferenceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, evidenceReferenceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceReferenceDTO{" +
            "id='" + getId() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", version=" + getVersion() +
            ", typePiece='" + getTypePiece() + "'" +
            ", deposantId='" + getDeposantId() + "'" +
            ", dateDepot='" + getDateDepot() + "'" +
            ", scelle='" + getScelle() + "'" +
            ", valide='" + getValide() + "'" +
            ", operation=" + getOperation() +
            "}";
    }
}
