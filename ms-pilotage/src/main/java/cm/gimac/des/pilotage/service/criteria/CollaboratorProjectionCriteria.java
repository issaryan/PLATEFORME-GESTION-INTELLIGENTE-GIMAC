package cm.gimac.des.pilotage.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.CollaboratorProjection} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.CollaboratorProjectionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /collaborator-projections?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CollaboratorProjectionCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter collaboratorId;

    private StringFilter displayName;

    private BooleanFilter active;

    private StringFilter serviceCode;

    private InstantFilter updatedAt;

    private Boolean distinct;

    public CollaboratorProjectionCriteria() {}

    public CollaboratorProjectionCriteria(CollaboratorProjectionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.collaboratorId = other.optionalCollaboratorId().map(UUIDFilter::copy).orElse(null);
        this.displayName = other.optionalDisplayName().map(StringFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CollaboratorProjectionCriteria copy() {
        return new CollaboratorProjectionCriteria(this);
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

    public UUIDFilter getCollaboratorId() {
        return collaboratorId;
    }

    public Optional<UUIDFilter> optionalCollaboratorId() {
        return Optional.ofNullable(collaboratorId);
    }

    public UUIDFilter collaboratorId() {
        if (collaboratorId == null) {
            setCollaboratorId(new UUIDFilter());
        }
        return collaboratorId;
    }

    public void setCollaboratorId(UUIDFilter collaboratorId) {
        this.collaboratorId = collaboratorId;
    }

    public StringFilter getDisplayName() {
        return displayName;
    }

    public Optional<StringFilter> optionalDisplayName() {
        return Optional.ofNullable(displayName);
    }

    public StringFilter displayName() {
        if (displayName == null) {
            setDisplayName(new StringFilter());
        }
        return displayName;
    }

    public void setDisplayName(StringFilter displayName) {
        this.displayName = displayName;
    }

    public BooleanFilter getActive() {
        return active;
    }

    public Optional<BooleanFilter> optionalActive() {
        return Optional.ofNullable(active);
    }

    public BooleanFilter active() {
        if (active == null) {
            setActive(new BooleanFilter());
        }
        return active;
    }

    public void setActive(BooleanFilter active) {
        this.active = active;
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
        final CollaboratorProjectionCriteria that = (CollaboratorProjectionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(collaboratorId, that.collaboratorId) &&
            Objects.equals(displayName, that.displayName) &&
            Objects.equals(active, that.active) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, collaboratorId, displayName, active, serviceCode, updatedAt, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CollaboratorProjectionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCollaboratorId().map(f -> "collaboratorId=" + f + ", ").orElse("") +
            optionalDisplayName().map(f -> "displayName=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
