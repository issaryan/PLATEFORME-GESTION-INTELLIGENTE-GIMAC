package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.ChangeApproval;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Fiche de changement lue, jamais écrite (RG-34) : l'ingénieur d'homologation
 * en saisit la référence ; l'état est relu en mTLS dans le dispositif de
 * gestion des changements, à nouveau au-delà de 15 min (algorithme 6.36).
 */
@Entity
@Table(name = "change_record")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChangeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence dans le dispositif, par exemple CHG-2026-0412.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "external_ref", length = 50, nullable = false, unique = true)
    private String externalRef;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "approval", nullable = false)
    private ChangeApproval approval;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    @NotNull
    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "content_digest", length = 64, nullable = false)
    private String contentDigest;

    /**
     * Auteur de la saisie de la référence (collaborateur MS-IAM).
     */
    @NotNull
    @Column(name = "referenced_by", nullable = false)
    private UUID referencedBy;

    @JsonIgnoreProperties(value = { "changeRecord", "attestation", "commissioningReport", "quituses", "pieces" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "changeRecord")
    private CommissioningFile commissioningFile;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ChangeRecord id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExternalRef() {
        return this.externalRef;
    }

    public ChangeRecord externalRef(String externalRef) {
        this.setExternalRef(externalRef);
        return this;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public ChangeApproval getApproval() {
        return this.approval;
    }

    public ChangeRecord approval(ChangeApproval approval) {
        this.setApproval(approval);
        return this;
    }

    public void setApproval(ChangeApproval approval) {
        this.approval = approval;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public ChangeRecord approvedAt(Instant approvedAt) {
        this.setApprovedAt(approvedAt);
        return this;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getWindowStart() {
        return this.windowStart;
    }

    public ChangeRecord windowStart(Instant windowStart) {
        this.setWindowStart(windowStart);
        return this;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return this.windowEnd;
    }

    public ChangeRecord windowEnd(Instant windowEnd) {
        this.setWindowEnd(windowEnd);
        return this;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Instant getReadAt() {
        return this.readAt;
    }

    public ChangeRecord readAt(Instant readAt) {
        this.setReadAt(readAt);
        return this;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public String getContentDigest() {
        return this.contentDigest;
    }

    public ChangeRecord contentDigest(String contentDigest) {
        this.setContentDigest(contentDigest);
        return this;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public UUID getReferencedBy() {
        return this.referencedBy;
    }

    public ChangeRecord referencedBy(UUID referencedBy) {
        this.setReferencedBy(referencedBy);
        return this;
    }

    public void setReferencedBy(UUID referencedBy) {
        this.referencedBy = referencedBy;
    }

    public CommissioningFile getCommissioningFile() {
        return this.commissioningFile;
    }

    public void setCommissioningFile(CommissioningFile commissioningFile) {
        if (this.commissioningFile != null) {
            this.commissioningFile.setChangeRecord(null);
        }
        if (commissioningFile != null) {
            commissioningFile.setChangeRecord(this);
        }
        this.commissioningFile = commissioningFile;
    }

    public ChangeRecord commissioningFile(CommissioningFile commissioningFile) {
        this.setCommissioningFile(commissioningFile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeRecord)) {
            return false;
        }
        return getId() != null && getId().equals(((ChangeRecord) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChangeRecord{" +
            "id=" + getId() +
            ", externalRef='" + getExternalRef() + "'" +
            ", approval='" + getApproval() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", windowStart='" + getWindowStart() + "'" +
            ", windowEnd='" + getWindowEnd() + "'" +
            ", readAt='" + getReadAt() + "'" +
            ", contentDigest='" + getContentDigest() + "'" +
            ", referencedBy='" + getReferencedBy() + "'" +
            "}";
    }
}
