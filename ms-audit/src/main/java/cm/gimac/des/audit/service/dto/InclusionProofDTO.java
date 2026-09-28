package cm.gimac.des.audit.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.InclusionProof} entity.
 */
@Schema(description = "Preuve d'inclusion d'une feuille dans un arbre de taille donnée (algorithme 6.13, RG-09).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InclusionProofDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 0L)
    private Long eventIndex;

    @NotNull
    @Min(value = 1L)
    private Long treeSize;

    @Schema(
        description = "Chemin d'audit : liste JSON des empreintes sœurs, du bas vers le haut.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String auditPath;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String rootHash;

    @NotNull
    private Instant generatedAt;

    @NotNull
    private PeriodRootDTO periodRoot;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getEventIndex() {
        return eventIndex;
    }

    public void setEventIndex(Long eventIndex) {
        this.eventIndex = eventIndex;
    }

    public Long getTreeSize() {
        return treeSize;
    }

    public void setTreeSize(Long treeSize) {
        this.treeSize = treeSize;
    }

    public String getAuditPath() {
        return auditPath;
    }

    public void setAuditPath(String auditPath) {
        this.auditPath = auditPath;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public PeriodRootDTO getPeriodRoot() {
        return periodRoot;
    }

    public void setPeriodRoot(PeriodRootDTO periodRoot) {
        this.periodRoot = periodRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InclusionProofDTO)) {
            return false;
        }

        InclusionProofDTO inclusionProofDTO = (InclusionProofDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, inclusionProofDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InclusionProofDTO{" +
            "id='" + getId() + "'" +
            ", eventIndex=" + getEventIndex() +
            ", treeSize=" + getTreeSize() +
            ", auditPath='" + getAuditPath() + "'" +
            ", rootHash='" + getRootHash() + "'" +
            ", generatedAt='" + getGeneratedAt() + "'" +
            ", periodRoot=" + getPeriodRoot() +
            "}";
    }
}
