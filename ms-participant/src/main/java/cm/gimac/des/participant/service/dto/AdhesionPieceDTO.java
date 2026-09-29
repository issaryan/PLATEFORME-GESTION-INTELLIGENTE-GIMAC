package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.AdhesionPiece} entity.
 */
@Schema(
    description = "Pièce du dossier d'adhésion (CR-CERT rang 2). La liste des pièces attendues est\nparamétrée ; la pièce est déposée dans MS-DOCUMENT, seules sa référence et son\nempreinte sont conservées. L'adhésion ne peut être prononcée que si toutes les\npièces obligatoires sont validées."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AdhesionPieceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 40)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Schema(description = "Code de la pièce dans la liste paramétrée, par exemple STATUTS.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    private Boolean mandatory;

    @NotNull
    private AdhesionPieceState state;

    private UUID documentId;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String documentSha256;

    @Size(max = 255)
    private String fileName;

    private Instant depositedAt;

    private UUID depositedById;

    private Instant reviewedAt;

    private UUID reviewedById;

    @Size(max = 500)
    private String rejectionReason;

    @NotNull
    private InstitutionDTO institution;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public AdhesionPieceState getState() {
        return state;
    }

    public void setState(AdhesionPieceState state) {
        this.state = state;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentSha256() {
        return documentSha256;
    }

    public void setDocumentSha256(String documentSha256) {
        this.documentSha256 = documentSha256;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Instant getDepositedAt() {
        return depositedAt;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
    }

    public UUID getDepositedById() {
        return depositedById;
    }

    public void setDepositedById(UUID depositedById) {
        this.depositedById = depositedById;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(UUID reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdhesionPieceDTO)) {
            return false;
        }

        AdhesionPieceDTO adhesionPieceDTO = (AdhesionPieceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, adhesionPieceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AdhesionPieceDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", mandatory='" + getMandatory() + "'" +
            ", state='" + getState() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", documentSha256='" + getDocumentSha256() + "'" +
            ", fileName='" + getFileName() + "'" +
            ", depositedAt='" + getDepositedAt() + "'" +
            ", depositedById='" + getDepositedById() + "'" +
            ", reviewedAt='" + getReviewedAt() + "'" +
            ", reviewedById='" + getReviewedById() + "'" +
            ", rejectionReason='" + getRejectionReason() + "'" +
            ", institution=" + getInstitution() +
            "}";
    }
}
