package cm.gimac.des.pilotage.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.WipOverride} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.WipOverrideResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /wip-overrides?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WipOverrideCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter requesterId;

    private UUIDFilter approverId;

    private StringFilter reason;

    private InstantFilter grantedAt;

    private InstantFilter expiresAt;

    private InstantFilter expiryNotifiedAt;

    private UUIDFilter boardColumnId;

    private UUIDFilter taskId;

    private Boolean distinct;

    public WipOverrideCriteria() {}

    public WipOverrideCriteria(WipOverrideCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.requesterId = other.optionalRequesterId().map(UUIDFilter::copy).orElse(null);
        this.approverId = other.optionalApproverId().map(UUIDFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.grantedAt = other.optionalGrantedAt().map(InstantFilter::copy).orElse(null);
        this.expiresAt = other.optionalExpiresAt().map(InstantFilter::copy).orElse(null);
        this.expiryNotifiedAt = other.optionalExpiryNotifiedAt().map(InstantFilter::copy).orElse(null);
        this.boardColumnId = other.optionalBoardColumnId().map(UUIDFilter::copy).orElse(null);
        this.taskId = other.optionalTaskId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public WipOverrideCriteria copy() {
        return new WipOverrideCriteria(this);
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

    public UUIDFilter getRequesterId() {
        return requesterId;
    }

    public Optional<UUIDFilter> optionalRequesterId() {
        return Optional.ofNullable(requesterId);
    }

    public UUIDFilter requesterId() {
        if (requesterId == null) {
            setRequesterId(new UUIDFilter());
        }
        return requesterId;
    }

    public void setRequesterId(UUIDFilter requesterId) {
        this.requesterId = requesterId;
    }

    public UUIDFilter getApproverId() {
        return approverId;
    }

    public Optional<UUIDFilter> optionalApproverId() {
        return Optional.ofNullable(approverId);
    }

    public UUIDFilter approverId() {
        if (approverId == null) {
            setApproverId(new UUIDFilter());
        }
        return approverId;
    }

    public void setApproverId(UUIDFilter approverId) {
        this.approverId = approverId;
    }

    public StringFilter getReason() {
        return reason;
    }

    public Optional<StringFilter> optionalReason() {
        return Optional.ofNullable(reason);
    }

    public StringFilter reason() {
        if (reason == null) {
            setReason(new StringFilter());
        }
        return reason;
    }

    public void setReason(StringFilter reason) {
        this.reason = reason;
    }

    public InstantFilter getGrantedAt() {
        return grantedAt;
    }

    public Optional<InstantFilter> optionalGrantedAt() {
        return Optional.ofNullable(grantedAt);
    }

    public InstantFilter grantedAt() {
        if (grantedAt == null) {
            setGrantedAt(new InstantFilter());
        }
        return grantedAt;
    }

    public void setGrantedAt(InstantFilter grantedAt) {
        this.grantedAt = grantedAt;
    }

    public InstantFilter getExpiresAt() {
        return expiresAt;
    }

    public Optional<InstantFilter> optionalExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public InstantFilter expiresAt() {
        if (expiresAt == null) {
            setExpiresAt(new InstantFilter());
        }
        return expiresAt;
    }

    public void setExpiresAt(InstantFilter expiresAt) {
        this.expiresAt = expiresAt;
    }

    public InstantFilter getExpiryNotifiedAt() {
        return expiryNotifiedAt;
    }

    public Optional<InstantFilter> optionalExpiryNotifiedAt() {
        return Optional.ofNullable(expiryNotifiedAt);
    }

    public InstantFilter expiryNotifiedAt() {
        if (expiryNotifiedAt == null) {
            setExpiryNotifiedAt(new InstantFilter());
        }
        return expiryNotifiedAt;
    }

    public void setExpiryNotifiedAt(InstantFilter expiryNotifiedAt) {
        this.expiryNotifiedAt = expiryNotifiedAt;
    }

    public UUIDFilter getBoardColumnId() {
        return boardColumnId;
    }

    public Optional<UUIDFilter> optionalBoardColumnId() {
        return Optional.ofNullable(boardColumnId);
    }

    public UUIDFilter boardColumnId() {
        if (boardColumnId == null) {
            setBoardColumnId(new UUIDFilter());
        }
        return boardColumnId;
    }

    public void setBoardColumnId(UUIDFilter boardColumnId) {
        this.boardColumnId = boardColumnId;
    }

    public UUIDFilter getTaskId() {
        return taskId;
    }

    public Optional<UUIDFilter> optionalTaskId() {
        return Optional.ofNullable(taskId);
    }

    public UUIDFilter taskId() {
        if (taskId == null) {
            setTaskId(new UUIDFilter());
        }
        return taskId;
    }

    public void setTaskId(UUIDFilter taskId) {
        this.taskId = taskId;
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
        final WipOverrideCriteria that = (WipOverrideCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(requesterId, that.requesterId) &&
            Objects.equals(approverId, that.approverId) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(grantedAt, that.grantedAt) &&
            Objects.equals(expiresAt, that.expiresAt) &&
            Objects.equals(expiryNotifiedAt, that.expiryNotifiedAt) &&
            Objects.equals(boardColumnId, that.boardColumnId) &&
            Objects.equals(taskId, that.taskId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requesterId, approverId, reason, grantedAt, expiresAt, expiryNotifiedAt, boardColumnId, taskId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WipOverrideCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalRequesterId().map(f -> "requesterId=" + f + ", ").orElse("") +
            optionalApproverId().map(f -> "approverId=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalGrantedAt().map(f -> "grantedAt=" + f + ", ").orElse("") +
            optionalExpiresAt().map(f -> "expiresAt=" + f + ", ").orElse("") +
            optionalExpiryNotifiedAt().map(f -> "expiryNotifiedAt=" + f + ", ").orElse("") +
            optionalBoardColumnId().map(f -> "boardColumnId=" + f + ", ").orElse("") +
            optionalTaskId().map(f -> "taskId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
