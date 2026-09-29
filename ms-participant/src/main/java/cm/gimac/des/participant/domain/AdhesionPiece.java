package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Pièce du dossier d'adhésion (CR-CERT rang 2). La liste des pièces attendues est
 * paramétrée ; la pièce est déposée dans MS-DOCUMENT, seules sa référence et son
 * empreinte sont conservées. L'adhésion ne peut être prononcée que si toutes les
 * pièces obligatoires sont validées.
 */
@Entity
@Table(name = "adhesion_piece")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AdhesionPiece implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code de la pièce dans la liste paramétrée, par exemple STATUTS.
     */
    @NotNull
    @Size(max = 40)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "code", length = 40, nullable = false)
    private String code;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @NotNull
    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private AdhesionPieceState state;

    @Column(name = "document_id")
    private UUID documentId;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "document_sha_256", length = 64)
    private String documentSha256;

    @Size(max = 255)
    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "deposited_at")
    private Instant depositedAt;

    @Column(name = "deposited_by_id")
    private UUID depositedById;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_id")
    private UUID reviewedById;

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AdhesionPiece id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public AdhesionPiece code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public AdhesionPiece label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getMandatory() {
        return this.mandatory;
    }

    public AdhesionPiece mandatory(Boolean mandatory) {
        this.setMandatory(mandatory);
        return this;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public AdhesionPieceState getState() {
        return this.state;
    }

    public AdhesionPiece state(AdhesionPieceState state) {
        this.setState(state);
        return this;
    }

    public void setState(AdhesionPieceState state) {
        this.state = state;
    }

    public UUID getDocumentId() {
        return this.documentId;
    }

    public AdhesionPiece documentId(UUID documentId) {
        this.setDocumentId(documentId);
        return this;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentSha256() {
        return this.documentSha256;
    }

    public AdhesionPiece documentSha256(String documentSha256) {
        this.setDocumentSha256(documentSha256);
        return this;
    }

    public void setDocumentSha256(String documentSha256) {
        this.documentSha256 = documentSha256;
    }

    public String getFileName() {
        return this.fileName;
    }

    public AdhesionPiece fileName(String fileName) {
        this.setFileName(fileName);
        return this;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Instant getDepositedAt() {
        return this.depositedAt;
    }

    public AdhesionPiece depositedAt(Instant depositedAt) {
        this.setDepositedAt(depositedAt);
        return this;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
    }

    public UUID getDepositedById() {
        return this.depositedById;
    }

    public AdhesionPiece depositedById(UUID depositedById) {
        this.setDepositedById(depositedById);
        return this;
    }

    public void setDepositedById(UUID depositedById) {
        this.depositedById = depositedById;
    }

    public Instant getReviewedAt() {
        return this.reviewedAt;
    }

    public AdhesionPiece reviewedAt(Instant reviewedAt) {
        this.setReviewedAt(reviewedAt);
        return this;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedById() {
        return this.reviewedById;
    }

    public AdhesionPiece reviewedById(UUID reviewedById) {
        this.setReviewedById(reviewedById);
        return this;
    }

    public void setReviewedById(UUID reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public AdhesionPiece rejectionReason(String rejectionReason) {
        this.setRejectionReason(rejectionReason);
        return this;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public AdhesionPiece institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdhesionPiece)) {
            return false;
        }
        return getId() != null && getId().equals(((AdhesionPiece) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AdhesionPiece{" +
            "id=" + getId() +
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
            "}";
    }
}
