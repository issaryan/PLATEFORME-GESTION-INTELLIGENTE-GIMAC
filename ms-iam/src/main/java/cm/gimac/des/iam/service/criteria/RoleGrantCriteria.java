package cm.gimac.des.iam.service.criteria;

import cm.gimac.des.iam.domain.enumeration.GrantState;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.iam.domain.RoleGrant} entity. This class is used
 * in {@link cm.gimac.des.iam.web.rest.RoleGrantResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /role-grants?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoleGrantCriteria implements Serializable, Criteria {

    /**
     * Class for filtering GrantState
     */
    public static class GrantStateFilter extends Filter<GrantState> {

        public GrantStateFilter() {}

        public GrantStateFilter(GrantStateFilter filter) {
            super(filter);
        }

        @Override
        public GrantStateFilter copy() {
            return new GrantStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private GrantStateFilter state;

    private UUIDFilter requestedBy;

    private InstantFilter requestedAt;

    private UUIDFilter approvedBy;

    private InstantFilter approvedAt;

    private InstantFilter revokedAt;

    private StringFilter revocationReason;

    private UUIDFilter collaboratorId;

    private UUIDFilter roleId;

    private Boolean distinct;

    public RoleGrantCriteria() {}

    public RoleGrantCriteria(RoleGrantCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.state = other.optionalState().map(GrantStateFilter::copy).orElse(null);
        this.requestedBy = other.optionalRequestedBy().map(UUIDFilter::copy).orElse(null);
        this.requestedAt = other.optionalRequestedAt().map(InstantFilter::copy).orElse(null);
        this.approvedBy = other.optionalApprovedBy().map(UUIDFilter::copy).orElse(null);
        this.approvedAt = other.optionalApprovedAt().map(InstantFilter::copy).orElse(null);
        this.revokedAt = other.optionalRevokedAt().map(InstantFilter::copy).orElse(null);
        this.revocationReason = other.optionalRevocationReason().map(StringFilter::copy).orElse(null);
        this.collaboratorId = other.optionalCollaboratorId().map(UUIDFilter::copy).orElse(null);
        this.roleId = other.optionalRoleId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RoleGrantCriteria copy() {
        return new RoleGrantCriteria(this);
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

    public GrantStateFilter getState() {
        return state;
    }

    public Optional<GrantStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public GrantStateFilter state() {
        if (state == null) {
            setState(new GrantStateFilter());
        }
        return state;
    }

    public void setState(GrantStateFilter state) {
        this.state = state;
    }

    public UUIDFilter getRequestedBy() {
        return requestedBy;
    }

    public Optional<UUIDFilter> optionalRequestedBy() {
        return Optional.ofNullable(requestedBy);
    }

    public UUIDFilter requestedBy() {
        if (requestedBy == null) {
            setRequestedBy(new UUIDFilter());
        }
        return requestedBy;
    }

    public void setRequestedBy(UUIDFilter requestedBy) {
        this.requestedBy = requestedBy;
    }

    public InstantFilter getRequestedAt() {
        return requestedAt;
    }

    public Optional<InstantFilter> optionalRequestedAt() {
        return Optional.ofNullable(requestedAt);
    }

    public InstantFilter requestedAt() {
        if (requestedAt == null) {
            setRequestedAt(new InstantFilter());
        }
        return requestedAt;
    }

    public void setRequestedAt(InstantFilter requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUIDFilter getApprovedBy() {
        return approvedBy;
    }

    public Optional<UUIDFilter> optionalApprovedBy() {
        return Optional.ofNullable(approvedBy);
    }

    public UUIDFilter approvedBy() {
        if (approvedBy == null) {
            setApprovedBy(new UUIDFilter());
        }
        return approvedBy;
    }

    public void setApprovedBy(UUIDFilter approvedBy) {
        this.approvedBy = approvedBy;
    }

    public InstantFilter getApprovedAt() {
        return approvedAt;
    }

    public Optional<InstantFilter> optionalApprovedAt() {
        return Optional.ofNullable(approvedAt);
    }

    public InstantFilter approvedAt() {
        if (approvedAt == null) {
            setApprovedAt(new InstantFilter());
        }
        return approvedAt;
    }

    public void setApprovedAt(InstantFilter approvedAt) {
        this.approvedAt = approvedAt;
    }

    public InstantFilter getRevokedAt() {
        return revokedAt;
    }

    public Optional<InstantFilter> optionalRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    public InstantFilter revokedAt() {
        if (revokedAt == null) {
            setRevokedAt(new InstantFilter());
        }
        return revokedAt;
    }

    public void setRevokedAt(InstantFilter revokedAt) {
        this.revokedAt = revokedAt;
    }

    public StringFilter getRevocationReason() {
        return revocationReason;
    }

    public Optional<StringFilter> optionalRevocationReason() {
        return Optional.ofNullable(revocationReason);
    }

    public StringFilter revocationReason() {
        if (revocationReason == null) {
            setRevocationReason(new StringFilter());
        }
        return revocationReason;
    }

    public void setRevocationReason(StringFilter revocationReason) {
        this.revocationReason = revocationReason;
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

    public UUIDFilter getRoleId() {
        return roleId;
    }

    public Optional<UUIDFilter> optionalRoleId() {
        return Optional.ofNullable(roleId);
    }

    public UUIDFilter roleId() {
        if (roleId == null) {
            setRoleId(new UUIDFilter());
        }
        return roleId;
    }

    public void setRoleId(UUIDFilter roleId) {
        this.roleId = roleId;
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
        final RoleGrantCriteria that = (RoleGrantCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(state, that.state) &&
            Objects.equals(requestedBy, that.requestedBy) &&
            Objects.equals(requestedAt, that.requestedAt) &&
            Objects.equals(approvedBy, that.approvedBy) &&
            Objects.equals(approvedAt, that.approvedAt) &&
            Objects.equals(revokedAt, that.revokedAt) &&
            Objects.equals(revocationReason, that.revocationReason) &&
            Objects.equals(collaboratorId, that.collaboratorId) &&
            Objects.equals(roleId, that.roleId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            state,
            requestedBy,
            requestedAt,
            approvedBy,
            approvedAt,
            revokedAt,
            revocationReason,
            collaboratorId,
            roleId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoleGrantCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalRequestedBy().map(f -> "requestedBy=" + f + ", ").orElse("") +
            optionalRequestedAt().map(f -> "requestedAt=" + f + ", ").orElse("") +
            optionalApprovedBy().map(f -> "approvedBy=" + f + ", ").orElse("") +
            optionalApprovedAt().map(f -> "approvedAt=" + f + ", ").orElse("") +
            optionalRevokedAt().map(f -> "revokedAt=" + f + ", ").orElse("") +
            optionalRevocationReason().map(f -> "revocationReason=" + f + ", ").orElse("") +
            optionalCollaboratorId().map(f -> "collaboratorId=" + f + ", ").orElse("") +
            optionalRoleId().map(f -> "roleId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
