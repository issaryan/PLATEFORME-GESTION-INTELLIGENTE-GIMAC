package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.OperationPhase;
import cm.gimac.des.homologation.domain.enumeration.ResponsibleParty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Opération de référence : nœud du graphe (CC §6.1.7.3). Le code est unique
 * dans une version ; un nœud de branche porte un service ; un nœud d'essai
 * porte un identifiant de scénario unique dans la version (algorithme 6.34).
 */
@Entity
@Table(name = "reference_operation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9]+-[0-9]{2,3}$")
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    /**
     * « TRONC_COMMUN » pour le tronc commun, sinon code de la branche métier.
     */
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "branch", length = 50, nullable = false)
    private String branch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private OperationPhase phase;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "owner", nullable = false)
    private ResponsibleParty owner;

    @NotNull
    @Size(max = 255)
    @Column(name = "expected_evidence", length = 255, nullable = false)
    private String expectedEvidence;

    /**
     * Service contribué (code tenu par MS-PARTICIPANT) ; vide pour le tronc commun.
     */
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "service_code", length = 80)
    private String serviceCode;

    /**
     * Identifiant du scénario dans le simulateur, pour une opération d'essai.
     */
    @Size(max = 50)
    @Column(name = "scenario_ref", length = 50)
    private String scenarioRef;

    /**
     * Appartenance au chemin critique (UC-HOM-01 étape 5, RG-28). Écart CC
     * corrigé : absente du diagramme de classes du CC.
     */
    @NotNull
    @Column(name = "critical_path", nullable = false)
    private Boolean criticalPath;

    @ManyToOne(optional = false)
    @NotNull
    private ReferenceCatalog catalog;

    /**
     * Arcs du graphe : prédécesseurs d'une opération de référence, dans la même version (RG-26, RG-27).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_reference_operation__predecessors",
        joinColumns = @JoinColumn(name = "reference_operation_id"),
        inverseJoinColumns = @JoinColumn(name = "predecessors_id")
    )
    @JsonIgnoreProperties(value = { "catalog", "predecessors", "successors" }, allowSetters = true)
    private Set<ReferenceOperation> predecessors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "predecessors")
    @JsonIgnoreProperties(value = { "catalog", "predecessors", "successors" }, allowSetters = true)
    private Set<ReferenceOperation> successors = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReferenceOperation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public ReferenceOperation code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public ReferenceOperation label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBranch() {
        return this.branch;
    }

    public ReferenceOperation branch(String branch) {
        this.setBranch(branch);
        return this;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public OperationPhase getPhase() {
        return this.phase;
    }

    public ReferenceOperation phase(OperationPhase phase) {
        this.setPhase(phase);
        return this;
    }

    public void setPhase(OperationPhase phase) {
        this.phase = phase;
    }

    public ResponsibleParty getOwner() {
        return this.owner;
    }

    public ReferenceOperation owner(ResponsibleParty owner) {
        this.setOwner(owner);
        return this;
    }

    public void setOwner(ResponsibleParty owner) {
        this.owner = owner;
    }

    public String getExpectedEvidence() {
        return this.expectedEvidence;
    }

    public ReferenceOperation expectedEvidence(String expectedEvidence) {
        this.setExpectedEvidence(expectedEvidence);
        return this;
    }

    public void setExpectedEvidence(String expectedEvidence) {
        this.expectedEvidence = expectedEvidence;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public ReferenceOperation serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getScenarioRef() {
        return this.scenarioRef;
    }

    public ReferenceOperation scenarioRef(String scenarioRef) {
        this.setScenarioRef(scenarioRef);
        return this;
    }

    public void setScenarioRef(String scenarioRef) {
        this.scenarioRef = scenarioRef;
    }

    public Boolean getCriticalPath() {
        return this.criticalPath;
    }

    public ReferenceOperation criticalPath(Boolean criticalPath) {
        this.setCriticalPath(criticalPath);
        return this;
    }

    public void setCriticalPath(Boolean criticalPath) {
        this.criticalPath = criticalPath;
    }

    public ReferenceCatalog getCatalog() {
        return this.catalog;
    }

    public void setCatalog(ReferenceCatalog referenceCatalog) {
        this.catalog = referenceCatalog;
    }

    public ReferenceOperation catalog(ReferenceCatalog referenceCatalog) {
        this.setCatalog(referenceCatalog);
        return this;
    }

    public Set<ReferenceOperation> getPredecessors() {
        return this.predecessors;
    }

    public void setPredecessors(Set<ReferenceOperation> referenceOperations) {
        this.predecessors = referenceOperations;
    }

    public ReferenceOperation predecessors(Set<ReferenceOperation> referenceOperations) {
        this.setPredecessors(referenceOperations);
        return this;
    }

    public ReferenceOperation addPredecessors(ReferenceOperation referenceOperation) {
        this.predecessors.add(referenceOperation);
        return this;
    }

    public ReferenceOperation removePredecessors(ReferenceOperation referenceOperation) {
        this.predecessors.remove(referenceOperation);
        return this;
    }

    public Set<ReferenceOperation> getSuccessors() {
        return this.successors;
    }

    public void setSuccessors(Set<ReferenceOperation> referenceOperations) {
        if (this.successors != null) {
            this.successors.forEach(i -> i.removePredecessors(this));
        }
        if (referenceOperations != null) {
            referenceOperations.forEach(i -> i.addPredecessors(this));
        }
        this.successors = referenceOperations;
    }

    public ReferenceOperation successors(Set<ReferenceOperation> referenceOperations) {
        this.setSuccessors(referenceOperations);
        return this;
    }

    public ReferenceOperation addSuccessors(ReferenceOperation referenceOperation) {
        this.successors.add(referenceOperation);
        referenceOperation.getPredecessors().add(this);
        return this;
    }

    public ReferenceOperation removeSuccessors(ReferenceOperation referenceOperation) {
        this.successors.remove(referenceOperation);
        referenceOperation.getPredecessors().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceOperation)) {
            return false;
        }
        return getId() != null && getId().equals(((ReferenceOperation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceOperation{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", branch='" + getBranch() + "'" +
            ", phase='" + getPhase() + "'" +
            ", owner='" + getOwner() + "'" +
            ", expectedEvidence='" + getExpectedEvidence() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", scenarioRef='" + getScenarioRef() + "'" +
            ", criticalPath='" + getCriticalPath() + "'" +
            "}";
    }
}
