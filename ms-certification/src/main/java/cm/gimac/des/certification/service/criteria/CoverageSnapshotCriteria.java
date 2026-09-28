package cm.gimac.des.certification.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.certification.domain.CoverageSnapshot} entity. This class is used
 * in {@link cm.gimac.des.certification.web.rest.CoverageSnapshotResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /coverage-snapshots?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CoverageSnapshotCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter liaisonId;

    private StringFilter serviceCode;

    private BooleanFilter included;

    private BigDecimalFilter certificationRate;

    private BigDecimalFilter commissioningRate;

    private UUIDFilter sourceEventId;

    private InstantFilter computedAt;

    private InstantFilter receivedAt;

    private Boolean distinct;

    public CoverageSnapshotCriteria() {}

    public CoverageSnapshotCriteria(CoverageSnapshotCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.included = other.optionalIncluded().map(BooleanFilter::copy).orElse(null);
        this.certificationRate = other.optionalCertificationRate().map(BigDecimalFilter::copy).orElse(null);
        this.commissioningRate = other.optionalCommissioningRate().map(BigDecimalFilter::copy).orElse(null);
        this.sourceEventId = other.optionalSourceEventId().map(UUIDFilter::copy).orElse(null);
        this.computedAt = other.optionalComputedAt().map(InstantFilter::copy).orElse(null);
        this.receivedAt = other.optionalReceivedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CoverageSnapshotCriteria copy() {
        return new CoverageSnapshotCriteria(this);
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

    public UUIDFilter getSourceEventId() {
        return sourceEventId;
    }

    public Optional<UUIDFilter> optionalSourceEventId() {
        return Optional.ofNullable(sourceEventId);
    }

    public UUIDFilter sourceEventId() {
        if (sourceEventId == null) {
            setSourceEventId(new UUIDFilter());
        }
        return sourceEventId;
    }

    public void setSourceEventId(UUIDFilter sourceEventId) {
        this.sourceEventId = sourceEventId;
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

    public InstantFilter getReceivedAt() {
        return receivedAt;
    }

    public Optional<InstantFilter> optionalReceivedAt() {
        return Optional.ofNullable(receivedAt);
    }

    public InstantFilter receivedAt() {
        if (receivedAt == null) {
            setReceivedAt(new InstantFilter());
        }
        return receivedAt;
    }

    public void setReceivedAt(InstantFilter receivedAt) {
        this.receivedAt = receivedAt;
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
        final CoverageSnapshotCriteria that = (CoverageSnapshotCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(included, that.included) &&
            Objects.equals(certificationRate, that.certificationRate) &&
            Objects.equals(commissioningRate, that.commissioningRate) &&
            Objects.equals(sourceEventId, that.sourceEventId) &&
            Objects.equals(computedAt, that.computedAt) &&
            Objects.equals(receivedAt, that.receivedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            liaisonId,
            serviceCode,
            included,
            certificationRate,
            commissioningRate,
            sourceEventId,
            computedAt,
            receivedAt,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CoverageSnapshotCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalIncluded().map(f -> "included=" + f + ", ").orElse("") +
            optionalCertificationRate().map(f -> "certificationRate=" + f + ", ").orElse("") +
            optionalCommissioningRate().map(f -> "commissioningRate=" + f + ", ").orElse("") +
            optionalSourceEventId().map(f -> "sourceEventId=" + f + ", ").orElse("") +
            optionalComputedAt().map(f -> "computedAt=" + f + ", ").orElse("") +
            optionalReceivedAt().map(f -> "receivedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
