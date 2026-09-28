package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.ReportState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Rapport de quart (algorithme 6.46). Jamais modifié une fois scellé : une
 * correction produit une nouvelle version visée par le chef de quart.
 */
@Entity
@Table(name = "shift_report")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence lisible, par exemple RQ-2026-0927-N.
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "reference", length = 30, nullable = false, unique = true)
    private String reference;

    /**
     * Forme canonique J (JCS, RFC 8785) du contenu figé.
     */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * Consignes annotées par l'opérateur sortant.
     */
    @Lob
    @Column(name = "instructions")
    private String instructions;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ReportState state;

    /**
     * Tête de chaîne de la main courante reprise dans le rapport.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "log_head_hash", length = 64)
    private String logHeadHash;

    /**
     * H1 = SHA-256(J), défi de la signature sortante.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "content_hash", length = 64, nullable = false)
    private String contentHash;

    /**
     * Hf = SHA-256(J || σs || σe || σc).
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "digest", length = 64)
    private String digest;

    /**
     * D : empreinte du PDF/A-3 déposé.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "document_digest", length = 64)
    private String documentDigest;

    /**
     * Jeton RFC 3161 encodé en DER.
     */
    @Lob
    @Column(name = "timestamp_token")
    private byte[] timestampToken;

    @Column(name = "timestamp_token_content_type")
    private String timestampTokenContentType;

    @Size(max = 100)
    @Column(name = "timestamp_serial_number", length = 100)
    private String timestampSerialNumber;

    /**
     * Référence du document déposé dans MS-DOCUMENT, par exemple DOC-2026-11902.
     */
    @Size(max = 100)
    @Column(name = "document_ref", length = 100)
    private String documentRef;

    /**
     * Transfert de responsabilité de la salle : instant de la contresignature.
     */
    @Column(name = "responsibility_transferred_at")
    private Instant responsibilityTransferredAt;

    @Column(name = "sealed_at")
    private Instant sealedAt;

    /**
     * Un rapport par vacation.
     */
    @JsonIgnoreProperties(value = { "roster", "shiftReport" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Shift shift;

    /**
     * Nouvelle version d'un rapport scellé, remplaçant la précédente.
     */
    @JsonIgnoreProperties(value = { "shift", "supersedes", "shiftReport" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private ShiftReport supersedes;

    @JsonIgnoreProperties(value = { "shift", "supersedes", "shiftReport" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "supersedes")
    private ShiftReport shiftReport;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ShiftReport id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public ShiftReport reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getContent() {
        return this.content;
    }

    public ShiftReport content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getInstructions() {
        return this.instructions;
    }

    public ShiftReport instructions(String instructions) {
        this.setInstructions(instructions);
        return this;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public ReportState getState() {
        return this.state;
    }

    public ShiftReport state(ReportState state) {
        this.setState(state);
        return this;
    }

    public void setState(ReportState state) {
        this.state = state;
    }

    public String getLogHeadHash() {
        return this.logHeadHash;
    }

    public ShiftReport logHeadHash(String logHeadHash) {
        this.setLogHeadHash(logHeadHash);
        return this;
    }

    public void setLogHeadHash(String logHeadHash) {
        this.logHeadHash = logHeadHash;
    }

    public String getContentHash() {
        return this.contentHash;
    }

    public ShiftReport contentHash(String contentHash) {
        this.setContentHash(contentHash);
        return this;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getDigest() {
        return this.digest;
    }

    public ShiftReport digest(String digest) {
        this.setDigest(digest);
        return this;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getDocumentDigest() {
        return this.documentDigest;
    }

    public ShiftReport documentDigest(String documentDigest) {
        this.setDocumentDigest(documentDigest);
        return this;
    }

    public void setDocumentDigest(String documentDigest) {
        this.documentDigest = documentDigest;
    }

    public byte[] getTimestampToken() {
        return this.timestampToken;
    }

    public ShiftReport timestampToken(byte[] timestampToken) {
        this.setTimestampToken(timestampToken);
        return this;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return this.timestampTokenContentType;
    }

    public ShiftReport timestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
        return this;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public String getTimestampSerialNumber() {
        return this.timestampSerialNumber;
    }

    public ShiftReport timestampSerialNumber(String timestampSerialNumber) {
        this.setTimestampSerialNumber(timestampSerialNumber);
        return this;
    }

    public void setTimestampSerialNumber(String timestampSerialNumber) {
        this.timestampSerialNumber = timestampSerialNumber;
    }

    public String getDocumentRef() {
        return this.documentRef;
    }

    public ShiftReport documentRef(String documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public Instant getResponsibilityTransferredAt() {
        return this.responsibilityTransferredAt;
    }

    public ShiftReport responsibilityTransferredAt(Instant responsibilityTransferredAt) {
        this.setResponsibilityTransferredAt(responsibilityTransferredAt);
        return this;
    }

    public void setResponsibilityTransferredAt(Instant responsibilityTransferredAt) {
        this.responsibilityTransferredAt = responsibilityTransferredAt;
    }

    public Instant getSealedAt() {
        return this.sealedAt;
    }

    public ShiftReport sealedAt(Instant sealedAt) {
        this.setSealedAt(sealedAt);
        return this;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Shift getShift() {
        return this.shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public ShiftReport shift(Shift shift) {
        this.setShift(shift);
        return this;
    }

    public ShiftReport getSupersedes() {
        return this.supersedes;
    }

    public void setSupersedes(ShiftReport shiftReport) {
        this.supersedes = shiftReport;
    }

    public ShiftReport supersedes(ShiftReport shiftReport) {
        this.setSupersedes(shiftReport);
        return this;
    }

    public ShiftReport getShiftReport() {
        return this.shiftReport;
    }

    public void setShiftReport(ShiftReport shiftReport) {
        if (this.shiftReport != null) {
            this.shiftReport.setSupersedes(null);
        }
        if (shiftReport != null) {
            shiftReport.setSupersedes(this);
        }
        this.shiftReport = shiftReport;
    }

    public ShiftReport shiftReport(ShiftReport shiftReport) {
        this.setShiftReport(shiftReport);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShiftReport)) {
            return false;
        }
        return getId() != null && getId().equals(((ShiftReport) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftReport{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", content='" + getContent() + "'" +
            ", instructions='" + getInstructions() + "'" +
            ", state='" + getState() + "'" +
            ", logHeadHash='" + getLogHeadHash() + "'" +
            ", contentHash='" + getContentHash() + "'" +
            ", digest='" + getDigest() + "'" +
            ", documentDigest='" + getDocumentDigest() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampTokenContentType='" + getTimestampTokenContentType() + "'" +
            ", timestampSerialNumber='" + getTimestampSerialNumber() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", responsibilityTransferredAt='" + getResponsibilityTransferredAt() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            "}";
    }
}
