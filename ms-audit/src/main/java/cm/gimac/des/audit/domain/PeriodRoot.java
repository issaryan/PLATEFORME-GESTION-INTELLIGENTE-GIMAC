package cm.gimac.des.audit.domain;

import cm.gimac.des.audit.domain.enumeration.PeriodRootState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Racine de période : scellement quotidien, horodaté (RFC 3161) et publié
 * hors de la plateforme (RG-08). Le jeton d'horodatage y est intégré.
 */
@Entity
@Table(name = "period_root")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PeriodRoot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Période scellée, au format AAAA-MM-JJ.
     */
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    @Column(name = "period", length = 10, nullable = false, unique = true)
    private String period;

    @NotNull
    @Min(value = 0L)
    @Column(name = "tree_size", nullable = false)
    private Long treeSize;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "root_hash", length = 64, nullable = false)
    private String rootHash;

    @NotNull
    @Column(name = "sealed_at", nullable = false)
    private Instant sealedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private PeriodRootState state;

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

    @Column(name = "timestamp_gen_time")
    private Instant timestampGenTime;

    /**
     * SHA256(racine || taille || période), soumis à l'autorité d'horodatage.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "timestamp_imprint", length = 64)
    private String timestampImprint;

    /**
     * Référence rendue par le dépôt de publication des racines.
     */
    @Size(max = 255)
    @Column(name = "publication_reference", length = 255)
    private String publicationReference;

    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Chaque racine référence la précédente (cohérence entre périodes).
     */
    @JsonIgnoreProperties(value = { "previousRoot", "consistencyWithPrevious", "periodRoot" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private PeriodRoot previousRoot;

    /**
     * Preuve de cohérence avec la racine précédente, calculée au scellement.
     */
    @JsonIgnoreProperties(value = { "periodRoot" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private ConsistencyProof consistencyWithPrevious;

    @JsonIgnoreProperties(value = { "previousRoot", "consistencyWithPrevious", "periodRoot" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "previousRoot")
    private PeriodRoot periodRoot;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public PeriodRoot id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPeriod() {
        return this.period;
    }

    public PeriodRoot period(String period) {
        this.setPeriod(period);
        return this;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getTreeSize() {
        return this.treeSize;
    }

    public PeriodRoot treeSize(Long treeSize) {
        this.setTreeSize(treeSize);
        return this;
    }

    public void setTreeSize(Long treeSize) {
        this.treeSize = treeSize;
    }

    public String getRootHash() {
        return this.rootHash;
    }

    public PeriodRoot rootHash(String rootHash) {
        this.setRootHash(rootHash);
        return this;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Instant getSealedAt() {
        return this.sealedAt;
    }

    public PeriodRoot sealedAt(Instant sealedAt) {
        this.setSealedAt(sealedAt);
        return this;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public PeriodRootState getState() {
        return this.state;
    }

    public PeriodRoot state(PeriodRootState state) {
        this.setState(state);
        return this;
    }

    public void setState(PeriodRootState state) {
        this.state = state;
    }

    public byte[] getTimestampToken() {
        return this.timestampToken;
    }

    public PeriodRoot timestampToken(byte[] timestampToken) {
        this.setTimestampToken(timestampToken);
        return this;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return this.timestampTokenContentType;
    }

    public PeriodRoot timestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
        return this;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public String getTimestampSerialNumber() {
        return this.timestampSerialNumber;
    }

    public PeriodRoot timestampSerialNumber(String timestampSerialNumber) {
        this.setTimestampSerialNumber(timestampSerialNumber);
        return this;
    }

    public void setTimestampSerialNumber(String timestampSerialNumber) {
        this.timestampSerialNumber = timestampSerialNumber;
    }

    public Instant getTimestampGenTime() {
        return this.timestampGenTime;
    }

    public PeriodRoot timestampGenTime(Instant timestampGenTime) {
        this.setTimestampGenTime(timestampGenTime);
        return this;
    }

    public void setTimestampGenTime(Instant timestampGenTime) {
        this.timestampGenTime = timestampGenTime;
    }

    public String getTimestampImprint() {
        return this.timestampImprint;
    }

    public PeriodRoot timestampImprint(String timestampImprint) {
        this.setTimestampImprint(timestampImprint);
        return this;
    }

    public void setTimestampImprint(String timestampImprint) {
        this.timestampImprint = timestampImprint;
    }

    public String getPublicationReference() {
        return this.publicationReference;
    }

    public PeriodRoot publicationReference(String publicationReference) {
        this.setPublicationReference(publicationReference);
        return this;
    }

    public void setPublicationReference(String publicationReference) {
        this.publicationReference = publicationReference;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public PeriodRoot publishedAt(Instant publishedAt) {
        this.setPublishedAt(publishedAt);
        return this;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public PeriodRoot getPreviousRoot() {
        return this.previousRoot;
    }

    public void setPreviousRoot(PeriodRoot periodRoot) {
        this.previousRoot = periodRoot;
    }

    public PeriodRoot previousRoot(PeriodRoot periodRoot) {
        this.setPreviousRoot(periodRoot);
        return this;
    }

    public ConsistencyProof getConsistencyWithPrevious() {
        return this.consistencyWithPrevious;
    }

    public void setConsistencyWithPrevious(ConsistencyProof consistencyProof) {
        this.consistencyWithPrevious = consistencyProof;
    }

    public PeriodRoot consistencyWithPrevious(ConsistencyProof consistencyProof) {
        this.setConsistencyWithPrevious(consistencyProof);
        return this;
    }

    public PeriodRoot getPeriodRoot() {
        return this.periodRoot;
    }

    public void setPeriodRoot(PeriodRoot periodRoot) {
        if (this.periodRoot != null) {
            this.periodRoot.setPreviousRoot(null);
        }
        if (periodRoot != null) {
            periodRoot.setPreviousRoot(this);
        }
        this.periodRoot = periodRoot;
    }

    public PeriodRoot periodRoot(PeriodRoot periodRoot) {
        this.setPeriodRoot(periodRoot);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PeriodRoot)) {
            return false;
        }
        return getId() != null && getId().equals(((PeriodRoot) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PeriodRoot{" +
            "id=" + getId() +
            ", period='" + getPeriod() + "'" +
            ", treeSize=" + getTreeSize() +
            ", rootHash='" + getRootHash() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", state='" + getState() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampTokenContentType='" + getTimestampTokenContentType() + "'" +
            ", timestampSerialNumber='" + getTimestampSerialNumber() + "'" +
            ", timestampGenTime='" + getTimestampGenTime() + "'" +
            ", timestampImprint='" + getTimestampImprint() + "'" +
            ", publicationReference='" + getPublicationReference() + "'" +
            ", publishedAt='" + getPublishedAt() + "'" +
            "}";
    }
}
