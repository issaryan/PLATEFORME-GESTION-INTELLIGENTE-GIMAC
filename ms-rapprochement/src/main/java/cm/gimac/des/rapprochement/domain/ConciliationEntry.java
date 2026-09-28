package cm.gimac.des.rapprochement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrée de conciliation entre le groupement et le participant (objet-valeur du CC, persisté).
 */
@Entity
@Table(name = "conciliation_entry")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConciliationEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    /**
     * Horodatage (at du CC, renommé : mot réservé SQL).
     */
    @NotNull
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @NotNull
    @Size(max = 4000)
    @Column(name = "text", length = 4000, nullable = false)
    private String text;

    /**
     * Pièce éventuelle déposée dans MS-DOCUMENT.
     */
    @Column(name = "document_ref")
    private UUID documentRef;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "reconciliation", "regularizationAttestation" }, allowSetters = true)
    private Suspens suspens;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ConciliationEntry id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public ConciliationEntry authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public Instant getRecordedAt() {
        return this.recordedAt;
    }

    public ConciliationEntry recordedAt(Instant recordedAt) {
        this.setRecordedAt(recordedAt);
        return this;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getText() {
        return this.text;
    }

    public ConciliationEntry text(String text) {
        this.setText(text);
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public UUID getDocumentRef() {
        return this.documentRef;
    }

    public ConciliationEntry documentRef(UUID documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Suspens getSuspens() {
        return this.suspens;
    }

    public void setSuspens(Suspens suspens) {
        this.suspens = suspens;
    }

    public ConciliationEntry suspens(Suspens suspens) {
        this.setSuspens(suspens);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConciliationEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((ConciliationEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConciliationEntry{" +
            "id=" + getId() +
            ", authorId='" + getAuthorId() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            ", text='" + getText() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            "}";
    }
}
