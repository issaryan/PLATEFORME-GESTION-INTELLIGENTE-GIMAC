package cm.gimac.des.homologation.service.criteria;

import cm.gimac.des.homologation.domain.enumeration.OperationPhase;
import cm.gimac.des.homologation.domain.enumeration.ResponsibleParty;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.ReferenceOperation} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.ReferenceOperationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /reference-operations?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceOperationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OperationPhase
     */
    public static class OperationPhaseFilter extends Filter<OperationPhase> {

        public OperationPhaseFilter() {}

        public OperationPhaseFilter(OperationPhaseFilter filter) {
            super(filter);
        }

        @Override
        public OperationPhaseFilter copy() {
            return new OperationPhaseFilter(this);
        }
    }

    /**
     * Class for filtering ResponsibleParty
     */
    public static class ResponsiblePartyFilter extends Filter<ResponsibleParty> {

        public ResponsiblePartyFilter() {}

        public ResponsiblePartyFilter(ResponsiblePartyFilter filter) {
            super(filter);
        }

        @Override
        public ResponsiblePartyFilter copy() {
            return new ResponsiblePartyFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter code;

    private StringFilter label;

    private StringFilter branch;

    private OperationPhaseFilter phase;

    private ResponsiblePartyFilter owner;

    private StringFilter expectedEvidence;

    private StringFilter serviceCode;

    private StringFilter scenarioRef;

    private BooleanFilter criticalPath;

    private UUIDFilter catalogId;

    private UUIDFilter predecessorsId;

    private UUIDFilter successorsId;

    private Boolean distinct;

    public ReferenceOperationCriteria() {}

    public ReferenceOperationCriteria(ReferenceOperationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.code = other.optionalCode().map(StringFilter::copy).orElse(null);
        this.label = other.optionalLabel().map(StringFilter::copy).orElse(null);
        this.branch = other.optionalBranch().map(StringFilter::copy).orElse(null);
        this.phase = other.optionalPhase().map(OperationPhaseFilter::copy).orElse(null);
        this.owner = other.optionalOwner().map(ResponsiblePartyFilter::copy).orElse(null);
        this.expectedEvidence = other.optionalExpectedEvidence().map(StringFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.scenarioRef = other.optionalScenarioRef().map(StringFilter::copy).orElse(null);
        this.criticalPath = other.optionalCriticalPath().map(BooleanFilter::copy).orElse(null);
        this.catalogId = other.optionalCatalogId().map(UUIDFilter::copy).orElse(null);
        this.predecessorsId = other.optionalPredecessorsId().map(UUIDFilter::copy).orElse(null);
        this.successorsId = other.optionalSuccessorsId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ReferenceOperationCriteria copy() {
        return new ReferenceOperationCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public StringFilter getCode() {
        return code;
    }

    public Optional<StringFilter> optionalCode() {
        return Optional.ofNullable(code);
    }

    public StringFilter code() {
        if (code == null) {
            setCode(new StringFilter());
        }
        return code;
    }

    public void setCode(StringFilter code) {
        this.code = code;
    }

    public StringFilter getLabel() {
        return label;
    }

    public Optional<StringFilter> optionalLabel() {
        return Optional.ofNullable(label);
    }

    public StringFilter label() {
        if (label == null) {
            setLabel(new StringFilter());
        }
        return label;
    }

    public void setLabel(StringFilter label) {
        this.label = label;
    }

    public StringFilter getBranch() {
        return branch;
    }

    public Optional<StringFilter> optionalBranch() {
        return Optional.ofNullable(branch);
    }

    public StringFilter branch() {
        if (branch == null) {
            setBranch(new StringFilter());
        }
        return branch;
    }

    public void setBranch(StringFilter branch) {
        this.branch = branch;
    }

    public OperationPhaseFilter getPhase() {
        return phase;
    }

    public Optional<OperationPhaseFilter> optionalPhase() {
        return Optional.ofNullable(phase);
    }

    public OperationPhaseFilter phase() {
        if (phase == null) {
            setPhase(new OperationPhaseFilter());
        }
        return phase;
    }

    public void setPhase(OperationPhaseFilter phase) {
        this.phase = phase;
    }

    public ResponsiblePartyFilter getOwner() {
        return owner;
    }

    public Optional<ResponsiblePartyFilter> optionalOwner() {
        return Optional.ofNullable(owner);
    }

    public ResponsiblePartyFilter owner() {
        if (owner == null) {
            setOwner(new ResponsiblePartyFilter());
        }
        return owner;
    }

    public void setOwner(ResponsiblePartyFilter owner) {
        this.owner = owner;
    }

    public StringFilter getExpectedEvidence() {
        return expectedEvidence;
    }

    public Optional<StringFilter> optionalExpectedEvidence() {
        return Optional.ofNullable(expectedEvidence);
    }

    public StringFilter expectedEvidence() {
        if (expectedEvidence == null) {
            setExpectedEvidence(new StringFilter());
        }
        return expectedEvidence;
    }

    public void setExpectedEvidence(StringFilter expectedEvidence) {
        this.expectedEvidence = expectedEvidence;
    }

    public StringFilter getServiceCode() {
        return serviceCode;
    }

    public Optional<StringFilter> optionalServiceCode() {
        return Optional.ofNullable(serviceCode);
    }

    public StringFilter serviceCode() {
        if (serviceCode == null) {
            setServiceCode(new StringFilter());
        }
        return serviceCode;
    }

    public void setServiceCode(StringFilter serviceCode) {
        this.serviceCode = serviceCode;
    }

    public StringFilter getScenarioRef() {
        return scenarioRef;
    }

    public Optional<StringFilter> optionalScenarioRef() {
        return Optional.ofNullable(scenarioRef);
    }

    public StringFilter scenarioRef() {
        if (scenarioRef == null) {
            setScenarioRef(new StringFilter());
        }
        return scenarioRef;
    }

    public void setScenarioRef(StringFilter scenarioRef) {
        this.scenarioRef = scenarioRef;
    }

    public BooleanFilter getCriticalPath() {
        return criticalPath;
    }

    public Optional<BooleanFilter> optionalCriticalPath() {
        return Optional.ofNullable(criticalPath);
    }

    public BooleanFilter criticalPath() {
        if (criticalPath == null) {
            setCriticalPath(new BooleanFilter());
        }
        return criticalPath;
    }

    public void setCriticalPath(BooleanFilter criticalPath) {
        this.criticalPath = criticalPath;
    }

    public UUIDFilter getCatalogId() {
        return catalogId;
    }

    public Optional<UUIDFilter> optionalCatalogId() {
        return Optional.ofNullable(catalogId);
    }

    public UUIDFilter catalogId() {
        if (catalogId == null) {
            setCatalogId(new UUIDFilter());
        }
        return catalogId;
    }

    public void setCatalogId(UUIDFilter catalogId) {
        this.catalogId = catalogId;
    }

    public UUIDFilter getPredecessorsId() {
        return predecessorsId;
    }

    public Optional<UUIDFilter> optionalPredecessorsId() {
        return Optional.ofNullable(predecessorsId);
    }

    public UUIDFilter predecessorsId() {
        if (predecessorsId == null) {
            setPredecessorsId(new UUIDFilter());
        }
        return predecessorsId;
    }

    public void setPredecessorsId(UUIDFilter predecessorsId) {
        this.predecessorsId = predecessorsId;
    }

    public UUIDFilter getSuccessorsId() {
        return successorsId;
    }

    public Optional<UUIDFilter> optionalSuccessorsId() {
        return Optional.ofNullable(successorsId);
    }

    public UUIDFilter successorsId() {
        if (successorsId == null) {
            setSuccessorsId(new UUIDFilter());
        }
        return successorsId;
    }

    public void setSuccessorsId(UUIDFilter successorsId) {
        this.successorsId = successorsId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReferenceOperationCriteria that = (ReferenceOperationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(code, that.code) &&
            Objects.equals(label, that.label) &&
            Objects.equals(branch, that.branch) &&
            Objects.equals(phase, that.phase) &&
            Objects.equals(owner, that.owner) &&
            Objects.equals(expectedEvidence, that.expectedEvidence) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(scenarioRef, that.scenarioRef) &&
            Objects.equals(criticalPath, that.criticalPath) &&
            Objects.equals(catalogId, that.catalogId) &&
            Objects.equals(predecessorsId, that.predecessorsId) &&
            Objects.equals(successorsId, that.successorsId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            code,
            label,
            branch,
            phase,
            owner,
            expectedEvidence,
            serviceCode,
            scenarioRef,
            criticalPath,
            catalogId,
            predecessorsId,
            successorsId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceOperationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCode().map(f -> "code=" + f + ", ").orElse("") +
            optionalLabel().map(f -> "label=" + f + ", ").orElse("") +
            optionalBranch().map(f -> "branch=" + f + ", ").orElse("") +
            optionalPhase().map(f -> "phase=" + f + ", ").orElse("") +
            optionalOwner().map(f -> "owner=" + f + ", ").orElse("") +
            optionalExpectedEvidence().map(f -> "expectedEvidence=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalScenarioRef().map(f -> "scenarioRef=" + f + ", ").orElse("") +
            optionalCriticalPath().map(f -> "criticalPath=" + f + ", ").orElse("") +
            optionalCatalogId().map(f -> "catalogId=" + f + ", ").orElse("") +
            optionalPredecessorsId().map(f -> "predecessorsId=" + f + ", ").orElse("") +
            optionalSuccessorsId().map(f -> "successorsId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
