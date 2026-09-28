package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrée de main courante de quart : ajout seul, chaînée par empreinte ;
 * une correction est une nouvelle entrée qui cite la précédente.
 */
@Entity
@Table(name = "shift_log_entry")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 1L)
    @Column(name = "chain_index", nullable = false)
    private Long chainIndex;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @NotNull
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false)
    private ShiftLogNature nature;

    @NotNull
    @Size(max = 4000)
    @Column(name = "text", length = 4000, nullable = false)
    private String text;

    /**
     * Référence d'alerte, de ticket ou de liaison.
     */
    @Size(max = 100)
    @Column(name = "reference", length = 100)
    private String reference;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "previous_hash", length = 64, nullable = false)
    private String previousHash;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "hash", length = 64, nullable = false, unique = true)
    private String hash;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "roster", "shiftReport" }, allowSetters = true)
    private Shift shift;

    /**
     * Entrée corrigée, citée par une entrée de correction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "shift", "corrects" }, allowSetters = true)
    private ShiftLogEntry corrects;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ShiftLogEntry id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getChainIndex() {
        return this.chainIndex;
    }

    public ShiftLogEntry chainIndex(Long chainIndex) {
        this.setChainIndex(chainIndex);
        return this;
    }

    public void setChainIndex(Long chainIndex) {
        this.chainIndex = chainIndex;
    }

    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    public ShiftLogEntry occurredAt(Instant occurredAt) {
        this.setOccurredAt(occurredAt);
        return this;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public ShiftLogEntry authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public ShiftLogNature getNature() {
        return this.nature;
    }

    public ShiftLogEntry nature(ShiftLogNature nature) {
        this.setNature(nature);
        return this;
    }

    public void setNature(ShiftLogNature nature) {
        this.nature = nature;
    }

    public String getText() {
        return this.text;
    }

    public ShiftLogEntry text(String text) {
        this.setText(text);
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getReference() {
        return this.reference;
    }

    public ShiftLogEntry reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPreviousHash() {
        return this.previousHash;
    }

    public ShiftLogEntry previousHash(String previousHash) {
        this.setPreviousHash(previousHash);
        return this;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return this.hash;
    }

    public ShiftLogEntry hash(String hash) {
        this.setHash(hash);
        return this;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Shift getShift() {
        return this.shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public ShiftLogEntry shift(Shift shift) {
        this.setShift(shift);
        return this;
    }

    public ShiftLogEntry getCorrects() {
        return this.corrects;
    }

    public void setCorrects(ShiftLogEntry shiftLogEntry) {
        this.corrects = shiftLogEntry;
    }

    public ShiftLogEntry corrects(ShiftLogEntry shiftLogEntry) {
        this.setCorrects(shiftLogEntry);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShiftLogEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((ShiftLogEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftLogEntry{" +
            "id=" + getId() +
            ", chainIndex=" + getChainIndex() +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", nature='" + getNature() + "'" +
            ", text='" + getText() + "'" +
            ", reference='" + getReference() + "'" +
            ", previousHash='" + getPreviousHash() + "'" +
            ", hash='" + getHash() + "'" +
            "}";
    }
}
