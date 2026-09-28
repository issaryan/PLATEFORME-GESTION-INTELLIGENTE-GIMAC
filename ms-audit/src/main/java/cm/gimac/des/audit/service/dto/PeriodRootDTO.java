package cm.gimac.des.audit.service.dto;

import cm.gimac.des.audit.domain.enumeration.PeriodRootState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.PeriodRoot} entity.
 */
@Schema(
    description = "Racine de période : scellement quotidien, horodaté (RFC 3161) et publié\nhors de la plateforme (RG-08). Le jeton d'horodatage y est intégré."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PeriodRootDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    @Schema(description = "Période scellée, au format AAAA-MM-JJ.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String period;

    @NotNull
    @Min(value = 0L)
    private Long treeSize;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String rootHash;

    @NotNull
    private Instant sealedAt;

    @NotNull
    private PeriodRootState state;

    @Schema(description = "Jeton RFC 3161 encodé en DER.")
    @Lob
    private byte[] timestampToken;

    private String timestampTokenContentType;

    @Size(max = 100)
    private String timestampSerialNumber;

    private Instant timestampGenTime;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256(racine || taille || période), soumis à l'autorité d'horodatage.")
    private String timestampImprint;

    @Size(max = 255)
    @Schema(description = "Référence rendue par le dépôt de publication des racines.")
    private String publicationReference;

    private Instant publishedAt;

    @Schema(description = "Chaque racine référence la précédente (cohérence entre périodes).")
    private PeriodRootDTO previousRoot;

    @Schema(description = "Preuve de cohérence avec la racine précédente, calculée au scellement.")
    private ConsistencyProofDTO consistencyWithPrevious;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getTreeSize() {
        return treeSize;
    }

    public void setTreeSize(Long treeSize) {
        this.treeSize = treeSize;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Instant getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public PeriodRootState getState() {
        return state;
    }

    public void setState(PeriodRootState state) {
        this.state = state;
    }

    public byte[] getTimestampToken() {
        return timestampToken;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return timestampTokenContentType;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public String getTimestampSerialNumber() {
        return timestampSerialNumber;
    }

    public void setTimestampSerialNumber(String timestampSerialNumber) {
        this.timestampSerialNumber = timestampSerialNumber;
    }

    public Instant getTimestampGenTime() {
        return timestampGenTime;
    }

    public void setTimestampGenTime(Instant timestampGenTime) {
        this.timestampGenTime = timestampGenTime;
    }

    public String getTimestampImprint() {
        return timestampImprint;
    }

    public void setTimestampImprint(String timestampImprint) {
        this.timestampImprint = timestampImprint;
    }

    public String getPublicationReference() {
        return publicationReference;
    }

    public void setPublicationReference(String publicationReference) {
        this.publicationReference = publicationReference;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public PeriodRootDTO getPreviousRoot() {
        return previousRoot;
    }

    public void setPreviousRoot(PeriodRootDTO previousRoot) {
        this.previousRoot = previousRoot;
    }

    public ConsistencyProofDTO getConsistencyWithPrevious() {
        return consistencyWithPrevious;
    }

    public void setConsistencyWithPrevious(ConsistencyProofDTO consistencyWithPrevious) {
        this.consistencyWithPrevious = consistencyWithPrevious;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PeriodRootDTO)) {
            return false;
        }

        PeriodRootDTO periodRootDTO = (PeriodRootDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, periodRootDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PeriodRootDTO{" +
            "id='" + getId() + "'" +
            ", period='" + getPeriod() + "'" +
            ", treeSize=" + getTreeSize() +
            ", rootHash='" + getRootHash() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", state='" + getState() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampSerialNumber='" + getTimestampSerialNumber() + "'" +
            ", timestampGenTime='" + getTimestampGenTime() + "'" +
            ", timestampImprint='" + getTimestampImprint() + "'" +
            ", publicationReference='" + getPublicationReference() + "'" +
            ", publishedAt='" + getPublishedAt() + "'" +
            ", previousRoot=" + getPreviousRoot() +
            ", consistencyWithPrevious=" + getConsistencyWithPrevious() +
            "}";
    }
}
