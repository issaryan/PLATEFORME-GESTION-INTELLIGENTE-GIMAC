package cm.gimac.des.homologation.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.Coverage} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.CoverageResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /coverages?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CoverageCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter serviceCode;

    private BigDecimalFilter certificationRate;

    private BigDecimalFilter commissioningRate;

    private IntegerFilter requiredCount;

    private IntegerFilter closedCount;

    private InstantFilter computedAt;

    private UUIDFilter liaisonId;

    private Boolean distinct;

    public CoverageCriteria() {}

    public CoverageCriteria(CoverageCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.certificationRate = other.optionalCertificationRate().map(BigDecimalFilter::copy).orElse(null);
        this.commissioningRate = other.optionalCommissioningRate().map(BigDecimalFilter::copy).orElse(null);
        this.requiredCount = other.optionalRequiredCount().map(IntegerFilter::copy).orElse(null);
        this.closedCount = other.optionalClosedCount().map(IntegerFilter::copy).orElse(null);
        this.computedAt = other.optionalComputedAt().map(InstantFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CoverageCriteria copy() {
        return new CoverageCriteria(this);
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

    public BigDecimalFilter getCertificationRate() {
        return certificationRate;
    }

    public Optional<BigDecimalFilter> optionalCertificationRate() {
        return Optional.ofNullable(certificationRate);
    }

    public BigDecimalFilter certificationRate() {
        if (certificationRate == null) {
            setCertificationRate(new BigDecimalFilter());
        }
        return certificationRate;
    }

    public void setCertificationRate(BigDecimalFilter certificationRate) {
        this.certificationRate = certificationRate;
    }

    public BigDecimalFilter getCommissioningRate() {
        return commissioningRate;
    }

    public Optional<BigDecimalFilter> optionalCommissioningRate() {
        return Optional.ofNullable(commissioningRate);
    }

    public BigDecimalFilter commissioningRate() {
        if (commissioningRate == null) {
            setCommissioningRate(new BigDecimalFilter());
        }
        return commissioningRate;
    }

    public void setCommissioningRate(BigDecimalFilter commissioningRate) {
        this.commissioningRate = commissioningRate;
    }

    public IntegerFilter getRequiredCount() {
        return requiredCount;
    }

    public Optional<IntegerFilter> optionalRequiredCount() {
        return Optional.ofNullable(requiredCount);
    }

    public IntegerFilter requiredCount() {
        if (requiredCount == null) {
            setRequiredCount(new IntegerFilter());
        }
        return requiredCount;
    }

    public void setRequiredCount(IntegerFilter requiredCount) {
        this.requiredCount = requiredCount;
    }

    public IntegerFilter getClosedCount() {
        return closedCount;
    }

    public Optional<IntegerFilter> optionalClosedCount() {
        return Optional.ofNullable(closedCount);
    }

    public IntegerFilter closedCount() {
        if (closedCount == null) {
            setClosedCount(new IntegerFilter());
        }
        return closedCount;
    }

    public void setClosedCount(IntegerFilter closedCount) {
        this.closedCount = closedCount;
    }

    public InstantFilter getComputedAt() {
        return computedAt;
    }

    public Optional<InstantFilter> optionalComputedAt() {
        return Optional.ofNullable(computedAt);
    }

    public InstantFilter computedAt() {
        if (computedAt == null) {
            setComputedAt(new InstantFilter());
        }
        return computedAt;
    }

    public void setComputedAt(InstantFilter computedAt) {
        this.computedAt = computedAt;
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
        final CoverageCriteria that = (CoverageCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(certificationRate, that.certificationRate) &&
            Objects.equals(commissioningRate, that.commissioningRate) &&
            Objects.equals(requiredCount, that.requiredCount) &&
            Objects.equals(closedCount, that.closedCount) &&
            Objects.equals(computedAt, that.computedAt) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            serviceCode,
            certificationRate,
            commissioningRate,
            requiredCount,
            closedCount,
            computedAt,
            liaisonId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CoverageCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalCertificationRate().map(f -> "certificationRate=" + f + ", ").orElse("") +
            optionalCommissioningRate().map(f -> "commissioningRate=" + f + ", ").orElse("") +
            optionalRequiredCount().map(f -> "requiredCount=" + f + ", ").orElse("") +
            optionalClosedCount().map(f -> "closedCount=" + f + ", ").orElse("") +
            optionalComputedAt().map(f -> "computedAt=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
