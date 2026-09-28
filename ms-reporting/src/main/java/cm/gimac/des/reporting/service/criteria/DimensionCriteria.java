package cm.gimac.des.reporting.service.criteria;

import cm.gimac.des.reporting.domain.enumeration.DimensionKind;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.Dimension} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.DimensionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /dimensions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DimensionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DimensionKind
     */
    public static class DimensionKindFilter extends Filter<DimensionKind> {

        public DimensionKindFilter() {}

        public DimensionKindFilter(DimensionKindFilter filter) {
            super(filter);
        }

        @Override
        public DimensionKindFilter copy() {
            return new DimensionKindFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private DimensionKindFilter kind;

    private StringFilter key;

    private StringFilter label;

    private StringFilter parentKey;

    private InstantFilter updatedAt;

    private Boolean distinct;

    public DimensionCriteria() {}

    public DimensionCriteria(DimensionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.kind = other.optionalKind().map(DimensionKindFilter::copy).orElse(null);
        this.key = other.optionalKey().map(StringFilter::copy).orElse(null);
        this.label = other.optionalLabel().map(StringFilter::copy).orElse(null);
        this.parentKey = other.optionalParentKey().map(StringFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DimensionCriteria copy() {
        return new DimensionCriteria(this);
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

    public DimensionKindFilter getKind() {
        return kind;
    }

    public Optional<DimensionKindFilter> optionalKind() {
        return Optional.ofNullable(kind);
    }

    public DimensionKindFilter kind() {
        if (kind == null) {
            setKind(new DimensionKindFilter());
        }
        return kind;
    }

    public void setKind(DimensionKindFilter kind) {
        this.kind = kind;
    }

    public StringFilter getKey() {
        return key;
    }

    public Optional<StringFilter> optionalKey() {
        return Optional.ofNullable(key);
    }

    public StringFilter key() {
        if (key == null) {
            setKey(new StringFilter());
        }
        return key;
    }

    public void setKey(StringFilter key) {
        this.key = key;
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

    public StringFilter getParentKey() {
        return parentKey;
    }

    public Optional<StringFilter> optionalParentKey() {
        return Optional.ofNullable(parentKey);
    }

    public StringFilter parentKey() {
        if (parentKey == null) {
            setParentKey(new StringFilter());
        }
        return parentKey;
    }

    public void setParentKey(StringFilter parentKey) {
        this.parentKey = parentKey;
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
        final DimensionCriteria that = (DimensionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(kind, that.kind) &&
            Objects.equals(key, that.key) &&
            Objects.equals(label, that.label) &&
            Objects.equals(parentKey, that.parentKey) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kind, key, label, parentKey, updatedAt, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DimensionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalKind().map(f -> "kind=" + f + ", ").orElse("") +
            optionalKey().map(f -> "key=" + f + ", ").orElse("") +
            optionalLabel().map(f -> "label=" + f + ", ").orElse("") +
            optionalParentKey().map(f -> "parentKey=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
