package cm.gimac.des.homologation.service.criteria;

import cm.gimac.des.homologation.domain.enumeration.ScopeProgress;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.ContractedScope} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.ContractedScopeResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /contracted-scopes?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ContractedScopeCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ScopeProgress
     */
    public static class ScopeProgressFilter extends Filter<ScopeProgress> {

        public ScopeProgressFilter() {}

        public ScopeProgressFilter(ScopeProgressFilter filter) {
            super(filter);
        }

        @Override
        public ScopeProgressFilter copy() {
            return new ScopeProgressFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter serviceCode;

    private BooleanFilter included;

    private ScopeProgressFilter progress;

    private InstantFilter updatedAt;

    private UUIDFilter liaisonId;

    private Boolean distinct;

    public ContractedScopeCriteria() {}

    public ContractedScopeCriteria(ContractedScopeCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.included = other.optionalIncluded().map(BooleanFilter::copy).orElse(null);
        this.progress = other.optionalProgress().map(ScopeProgressFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ContractedScopeCriteria copy() {
        return new ContractedScopeCriteria(this);
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

    public BooleanFilter getIncluded() {
        return included;
    }

    public Optional<BooleanFilter> optionalIncluded() {
        return Optional.ofNullable(included);
    }

    public BooleanFilter included() {
        if (included == null) {
            setIncluded(new BooleanFilter());
        }
        return included;
    }

    public void setIncluded(BooleanFilter included) {
        this.included = included;
    }

    public ScopeProgressFilter getProgress() {
        return progress;
    }

    public Optional<ScopeProgressFilter> optionalProgress() {
        return Optional.ofNullable(progress);
    }

    public ScopeProgressFilter progress() {
        if (progress == null) {
            setProgress(new ScopeProgressFilter());
        }
        return progress;
    }

    public void setProgress(ScopeProgressFilter progress) {
        this.progress = progress;
    }

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUIDFilter getLiaisonId() {
        return liaisonId;
    }

    public Optional<UUIDFilter> optionalLiaisonId() {
        return Optional.ofNullable(liaisonId);
    }

    public UUIDFilter liaisonId() {
        if (liaisonId == null) {
            setLiaisonId(new UUIDFilter());
        }
        return liaisonId;
    }

    public void setLiaisonId(UUIDFilter liaisonId) {
        this.liaisonId = liaisonId;
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
        final ContractedScopeCriteria that = (ContractedScopeCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(included, that.included) &&
            Objects.equals(progress, that.progress) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceCode, included, progress, updatedAt, liaisonId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ContractedScopeCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalIncluded().map(f -> "included=" + f + ", ").orElse("") +
            optionalProgress().map(f -> "progress=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
