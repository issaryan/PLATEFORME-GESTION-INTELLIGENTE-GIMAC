package cm.gimac.des.iam.service.criteria;

import cm.gimac.des.iam.domain.enumeration.SessionState;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.iam.domain.UserSession} entity. This class is used
 * in {@link cm.gimac.des.iam.web.rest.UserSessionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /user-sessions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserSessionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering SessionState
     */
    public static class SessionStateFilter extends Filter<SessionState> {

        public SessionStateFilter() {}

        public SessionStateFilter(SessionStateFilter filter) {
            super(filter);
        }

        @Override
        public SessionStateFilter copy() {
            return new SessionStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter certificateThumbprint;

    private StringFilter authMethods;

    private InstantFilter issuedAt;

    private InstantFilter expiresAt;

    private SessionStateFilter state;

    private InstantFilter revokedAt;

    private StringFilter revocationReason;

    private UUIDFilter collaboratorId;

    private Boolean distinct;

    public UserSessionCriteria() {}

    public UserSessionCriteria(UserSessionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.certificateThumbprint = other.optionalCertificateThumbprint().map(StringFilter::copy).orElse(null);
        this.authMethods = other.optionalAuthMethods().map(StringFilter::copy).orElse(null);
        this.issuedAt = other.optionalIssuedAt().map(InstantFilter::copy).orElse(null);
        this.expiresAt = other.optionalExpiresAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(SessionStateFilter::copy).orElse(null);
        this.revokedAt = other.optionalRevokedAt().map(InstantFilter::copy).orElse(null);
        this.revocationReason = other.optionalRevocationReason().map(StringFilter::copy).orElse(null);
        this.collaboratorId = other.optionalCollaboratorId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public UserSessionCriteria copy() {
        return new UserSessionCriteria(this);
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

    public StringFilter getCertificateThumbprint() {
        return certificateThumbprint;
    }

    public Optional<StringFilter> optionalCertificateThumbprint() {
        return Optional.ofNullable(certificateThumbprint);
    }

    public StringFilter certificateThumbprint() {
        if (certificateThumbprint == null) {
            setCertificateThumbprint(new StringFilter());
        }
        return certificateThumbprint;
    }

    public void setCertificateThumbprint(StringFilter certificateThumbprint) {
        this.certificateThumbprint = certificateThumbprint;
    }

    public StringFilter getAuthMethods() {
        return authMethods;
    }

    public Optional<StringFilter> optionalAuthMethods() {
        return Optional.ofNullable(authMethods);
    }

    public StringFilter authMethods() {
        if (authMethods == null) {
            setAuthMethods(new StringFilter());
        }
        return authMethods;
    }

    public void setAuthMethods(StringFilter authMethods) {
        this.authMethods = authMethods;
    }

    public InstantFilter getIssuedAt() {
        return issuedAt;
    }

    public Optional<InstantFilter> optionalIssuedAt() {
        return Optional.ofNullable(issuedAt);
    }

    public InstantFilter issuedAt() {
        if (issuedAt == null) {
            setIssuedAt(new InstantFilter());
        }
        return issuedAt;
    }

    public void setIssuedAt(InstantFilter issuedAt) {
        this.issuedAt = issuedAt;
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

    public SessionStateFilter getState() {
        return state;
    }

    public Optional<SessionStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public SessionStateFilter state() {
        if (state == null) {
            setState(new SessionStateFilter());
        }
        return state;
    }

    public void setState(SessionStateFilter state) {
        this.state = state;
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
        final UserSessionCriteria that = (UserSessionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(certificateThumbprint, that.certificateThumbprint) &&
            Objects.equals(authMethods, that.authMethods) &&
            Objects.equals(issuedAt, that.issuedAt) &&
            Objects.equals(expiresAt, that.expiresAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(revokedAt, that.revokedAt) &&
            Objects.equals(revocationReason, that.revocationReason) &&
            Objects.equals(collaboratorId, that.collaboratorId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            certificateThumbprint,
            authMethods,
            issuedAt,
            expiresAt,
            state,
            revokedAt,
            revocationReason,
            collaboratorId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserSessionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCertificateThumbprint().map(f -> "certificateThumbprint=" + f + ", ").orElse("") +
            optionalAuthMethods().map(f -> "authMethods=" + f + ", ").orElse("") +
            optionalIssuedAt().map(f -> "issuedAt=" + f + ", ").orElse("") +
            optionalExpiresAt().map(f -> "expiresAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalRevokedAt().map(f -> "revokedAt=" + f + ", ").orElse("") +
            optionalRevocationReason().map(f -> "revocationReason=" + f + ", ").orElse("") +
            optionalCollaboratorId().map(f -> "collaboratorId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
