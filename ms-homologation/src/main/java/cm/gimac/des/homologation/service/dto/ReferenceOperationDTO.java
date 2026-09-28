package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.OperationPhase;
import cm.gimac.des.homologation.domain.enumeration.ResponsibleParty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ReferenceOperation} entity.
 */
@Schema(
    description = "Opération de référence : nœud du graphe (CC §6.1.7.3). Le code est unique\ndans une version ; un nœud de branche porte un service ; un nœud d'essai\nporte un identifiant de scénario unique dans la version (algorithme 6.34)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceOperationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9]+-[0-9]{2,3}$")
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Schema(
        description = "« TRONC_COMMUN » pour le tronc commun, sinon code de la branche métier.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String branch;

    @NotNull
    private OperationPhase phase;

    @NotNull
    private ResponsibleParty owner;

    @NotNull
    @Size(max = 255)
    private String expectedEvidence;

    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Schema(description = "Service contribué (code tenu par MS-PARTICIPANT) ; vide pour le tronc commun.")
    private String serviceCode;

    @Size(max = 50)
    @Schema(description = "Identifiant du scénario dans le simulateur, pour une opération d'essai.")
    private String scenarioRef;

    @NotNull
    @Schema(
        description = "Appartenance au chemin critique (UC-HOM-01 étape 5, RG-28). Écart CC\ncorrigé : absente du diagramme de classes du CC.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean criticalPath;

    @NotNull
    private ReferenceCatalogDTO catalog;

    @Schema(description = "Arcs du graphe : prédécesseurs d'une opération de référence, dans la même version (RG-26, RG-27).")
    private Set<ReferenceOperationDTO> predecessors = new HashSet<>();

    private Set<ReferenceOperationDTO> successors = new HashSet<>();

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

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public OperationPhase getPhase() {
        return phase;
    }

    public void setPhase(OperationPhase phase) {
        this.phase = phase;
    }

    public ResponsibleParty getOwner() {
        return owner;
    }

    public void setOwner(ResponsibleParty owner) {
        this.owner = owner;
    }

    public String getExpectedEvidence() {
        return expectedEvidence;
    }

    public void setExpectedEvidence(String expectedEvidence) {
        this.expectedEvidence = expectedEvidence;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getScenarioRef() {
        return scenarioRef;
    }

    public void setScenarioRef(String scenarioRef) {
        this.scenarioRef = scenarioRef;
    }

    public Boolean getCriticalPath() {
        return criticalPath;
    }

    public void setCriticalPath(Boolean criticalPath) {
        this.criticalPath = criticalPath;
    }

    public ReferenceCatalogDTO getCatalog() {
        return catalog;
    }

    public void setCatalog(ReferenceCatalogDTO catalog) {
        this.catalog = catalog;
    }

    public Set<ReferenceOperationDTO> getPredecessors() {
        return predecessors;
    }

    public void setPredecessors(Set<ReferenceOperationDTO> predecessors) {
        this.predecessors = predecessors;
    }

    public Set<ReferenceOperationDTO> getSuccessors() {
        return successors;
    }

    public void setSuccessors(Set<ReferenceOperationDTO> successors) {
        this.successors = successors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceOperationDTO)) {
            return false;
        }

        ReferenceOperationDTO referenceOperationDTO = (ReferenceOperationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, referenceOperationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceOperationDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", branch='" + getBranch() + "'" +
            ", phase='" + getPhase() + "'" +
            ", owner='" + getOwner() + "'" +
            ", expectedEvidence='" + getExpectedEvidence() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", scenarioRef='" + getScenarioRef() + "'" +
            ", criticalPath='" + getCriticalPath() + "'" +
            ", catalog=" + getCatalog() +
            ", predecessors=" + getPredecessors() +
            ", successors=" + getSuccessors() +
            "}";
    }
}
