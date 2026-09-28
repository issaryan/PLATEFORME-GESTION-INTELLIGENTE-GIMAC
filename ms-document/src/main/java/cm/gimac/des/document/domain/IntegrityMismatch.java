package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.MismatchKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Anomalie consignée par un contrôle d'intégrité, avec les deux empreintes.
 */
@Entity
@Table(name = "integrity_mismatch")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrityMismatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private MismatchKind kind;

    @NotNull
    @Size(max = 255)
    @Column(name = "object_key", length = 255, nullable = false)
    private String objectKey;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "expected_sha_256", length = 64, nullable = false)
    private String expectedSha256;

    /**
     * Empreinte recalculée ; absente pour un objet illisible.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "actual_sha_256", length = 64)
    private String actualSha256;

    @NotNull
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @ManyToOne(optional = false)
    @NotNull
    private IntegrityCheck integrityCheck;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "lineage", "policy", "documentLineage" }, allowSetters = true)
    private Document document;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public IntegrityMismatch id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MismatchKind getKind() {
        return this.kind;
    }

    public IntegrityMismatch kind(MismatchKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(MismatchKind kind) {
        this.kind = kind;
    }

    public String getObjectKey() {
        return this.objectKey;
    }

    public IntegrityMismatch objectKey(String objectKey) {
        this.setObjectKey(objectKey);
        return this;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getExpectedSha256() {
        return this.expectedSha256;
    }

    public IntegrityMismatch expectedSha256(String expectedSha256) {
        this.setExpectedSha256(expectedSha256);
        return this;
    }

    public void setExpectedSha256(String expectedSha256) {
        this.expectedSha256 = expectedSha256;
    }

    public String getActualSha256() {
        return this.actualSha256;
    }

    public IntegrityMismatch actualSha256(String actualSha256) {
        this.setActualSha256(actualSha256);
        return this;
    }

    public void setActualSha256(String actualSha256) {
        this.actualSha256 = actualSha256;
    }

    public Instant getDetectedAt() {
        return this.detectedAt;
    }

    public IntegrityMismatch detectedAt(Instant detectedAt) {
        this.setDetectedAt(detectedAt);
        return this;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public IntegrityCheck getIntegrityCheck() {
        return this.integrityCheck;
    }

    public void setIntegrityCheck(IntegrityCheck integrityCheck) {
        this.integrityCheck = integrityCheck;
    }

    public IntegrityMismatch integrityCheck(IntegrityCheck integrityCheck) {
        this.setIntegrityCheck(integrityCheck);
        return this;
    }

    public Document getDocument() {
        return this.document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public IntegrityMismatch document(Document document) {
        this.setDocument(document);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrityMismatch)) {
            return false;
        }
        return getId() != null && getId().equals(((IntegrityMismatch) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrityMismatch{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", objectKey='" + getObjectKey() + "'" +
            ", expectedSha256='" + getExpectedSha256() + "'" +
            ", actualSha256='" + getActualSha256() + "'" +
            ", detectedAt='" + getDetectedAt() + "'" +
            "}";
    }
}
