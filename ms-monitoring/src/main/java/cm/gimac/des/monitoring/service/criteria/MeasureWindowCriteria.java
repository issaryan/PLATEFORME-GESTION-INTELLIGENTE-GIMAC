package cm.gimac.des.monitoring.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.MeasureWindow} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.MeasureWindowResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /measure-windows?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeasureWindowCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private InstantFilter windowStart;

    private InstantFilter windowEnd;

    private InstantFilter receivedAt;

    private UUIDFilter linkId;

    private Boolean distinct;

    public MeasureWindowCriteria() {}

    public MeasureWindowCriteria(MeasureWindowCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.windowStart = other.optionalWindowStart().map(InstantFilter::copy).orElse(null);
        this.windowEnd = other.optionalWindowEnd().map(InstantFilter::copy).orElse(null);
        this.receivedAt = other.optionalReceivedAt().map(InstantFilter::copy).orElse(null);
        this.linkId = other.optionalLinkId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MeasureWindowCriteria copy() {
        return new MeasureWindowCriteria(this);
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

    public InstantFilter getWindowStart() {
        return windowStart;
    }

    public Optional<InstantFilter> optionalWindowStart() {
        return Optional.ofNullable(windowStart);
    }

    public InstantFilter windowStart() {
        if (windowStart == null) {
            setWindowStart(new InstantFilter());
        }
        return windowStart;
    }

    public void setWindowStart(InstantFilter windowStart) {
        this.windowStart = windowStart;
    }

    public InstantFilter getWindowEnd() {
        return windowEnd;
    }

    public Optional<InstantFilter> optionalWindowEnd() {
        return Optional.ofNullable(windowEnd);
    }

    public InstantFilter windowEnd() {
        if (windowEnd == null) {
            setWindowEnd(new InstantFilter());
        }
        return windowEnd;
    }

    public void setWindowEnd(InstantFilter windowEnd) {
        this.windowEnd = windowEnd;
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

    public UUIDFilter getLinkId() {
        return linkId;
    }

    public Optional<UUIDFilter> optionalLinkId() {
        return Optional.ofNullable(linkId);
    }

    public UUIDFilter linkId() {
        if (linkId == null) {
            setLinkId(new UUIDFilter());
        }
        return linkId;
    }

    public void setLinkId(UUIDFilter linkId) {
        this.linkId = linkId;
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
        final MeasureWindowCriteria that = (MeasureWindowCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(windowStart, that.windowStart) &&
            Objects.equals(windowEnd, that.windowEnd) &&
            Objects.equals(receivedAt, that.receivedAt) &&
            Objects.equals(linkId, that.linkId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, windowStart, windowEnd, receivedAt, linkId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeasureWindowCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalWindowStart().map(f -> "windowStart=" + f + ", ").orElse("") +
            optionalWindowEnd().map(f -> "windowEnd=" + f + ", ").orElse("") +
            optionalReceivedAt().map(f -> "receivedAt=" + f + ", ").orElse("") +
            optionalLinkId().map(f -> "linkId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
