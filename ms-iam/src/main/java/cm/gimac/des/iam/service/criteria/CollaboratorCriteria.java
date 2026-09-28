package cm.gimac.des.iam.service.criteria;

import cm.gimac.des.iam.domain.enumeration.CollaboratorStatus;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.iam.domain.Collaborator} entity. This class is used
 * in {@link cm.gimac.des.iam.web.rest.CollaboratorResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /collaborators?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CollaboratorCriteria implements Serializable, Criteria {

    /**
     * Class for filtering CollaboratorStatus
     */
    public static class CollaboratorStatusFilter extends Filter<CollaboratorStatus> {

        public CollaboratorStatusFilter() {}

        public CollaboratorStatusFilter(CollaboratorStatusFilter filter) {
            super(filter);
        }

        @Override
        public CollaboratorStatusFilter copy() {
            return new CollaboratorStatusFilter(this);
        }
    }

    /**
     * Class for filtering HabilitationLevel
     */
    public static class HabilitationLevelFilter extends Filter<HabilitationLevel> {

        public HabilitationLevelFilter() {}

        public HabilitationLevelFilter(HabilitationLevelFilter filter) {
            super(filter);
        }

        @Override
        public HabilitationLevelFilter copy() {
            return new HabilitationLevelFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter federatedId;

    private StringFilter directoryId;

    private StringFilter displayName;

    private StringFilter email;

    private CollaboratorStatusFilter status;

    private HabilitationLevelFilter level;

    private UUIDFilter institutionId;

    private StringFilter language;

    private IntegerFilter failedAttempts;

    private InstantFilter lastLoginAt;

    private LongFilter directoryCursor;

    private Boolean distinct;

    public CollaboratorCriteria() {}

    public CollaboratorCriteria(CollaboratorCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.federatedId = other.optionalFederatedId().map(StringFilter::copy).orElse(null);
        this.directoryId = other.optionalDirectoryId().map(StringFilter::copy).orElse(null);
        this.displayName = other.optionalDisplayName().map(StringFilter::copy).orElse(null);
        this.email = other.optionalEmail().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(CollaboratorStatusFilter::copy).orElse(null);
        this.level = other.optionalLevel().map(HabilitationLevelFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.language = other.optionalLanguage().map(StringFilter::copy).orElse(null);
        this.failedAttempts = other.optionalFailedAttempts().map(IntegerFilter::copy).orElse(null);
        this.lastLoginAt = other.optionalLastLoginAt().map(InstantFilter::copy).orElse(null);
        this.directoryCursor = other.optionalDirectoryCursor().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CollaboratorCriteria copy() {
        return new CollaboratorCriteria(this);
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

    public StringFilter getFederatedId() {
        return federatedId;
    }

    public Optional<StringFilter> optionalFederatedId() {
        return Optional.ofNullable(federatedId);
    }

    public StringFilter federatedId() {
        if (federatedId == null) {
            setFederatedId(new StringFilter());
        }
        return federatedId;
    }

    public void setFederatedId(StringFilter federatedId) {
        this.federatedId = federatedId;
    }

    public StringFilter getDirectoryId() {
        return directoryId;
    }

    public Optional<StringFilter> optionalDirectoryId() {
        return Optional.ofNullable(directoryId);
    }

    public StringFilter directoryId() {
        if (directoryId == null) {
            setDirectoryId(new StringFilter());
        }
        return directoryId;
    }

    public void setDirectoryId(StringFilter directoryId) {
        this.directoryId = directoryId;
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

    public StringFilter getEmail() {
        return email;
    }

    public Optional<StringFilter> optionalEmail() {
        return Optional.ofNullable(email);
    }

    public StringFilter email() {
        if (email == null) {
            setEmail(new StringFilter());
        }
        return email;
    }

    public void setEmail(StringFilter email) {
        this.email = email;
    }

    public CollaboratorStatusFilter getStatus() {
        return status;
    }

    public Optional<CollaboratorStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public CollaboratorStatusFilter status() {
        if (status == null) {
            setStatus(new CollaboratorStatusFilter());
        }
        return status;
    }

    public void setStatus(CollaboratorStatusFilter status) {
        this.status = status;
    }

    public HabilitationLevelFilter getLevel() {
        return level;
    }

    public Optional<HabilitationLevelFilter> optionalLevel() {
        return Optional.ofNullable(level);
    }

    public HabilitationLevelFilter level() {
        if (level == null) {
            setLevel(new HabilitationLevelFilter());
        }
        return level;
    }

    public void setLevel(HabilitationLevelFilter level) {
        this.level = level;
    }

    public UUIDFilter getInstitutionId() {
        return institutionId;
    }

    public Optional<UUIDFilter> optionalInstitutionId() {
        return Optional.ofNullable(institutionId);
    }

    public UUIDFilter institutionId() {
        if (institutionId == null) {
            setInstitutionId(new UUIDFilter());
        }
        return institutionId;
    }

    public void setInstitutionId(UUIDFilter institutionId) {
        this.institutionId = institutionId;
    }

    public StringFilter getLanguage() {
        return language;
    }

    public Optional<StringFilter> optionalLanguage() {
        return Optional.ofNullable(language);
    }

    public StringFilter language() {
        if (language == null) {
            setLanguage(new StringFilter());
        }
        return language;
    }

    public void setLanguage(StringFilter language) {
        this.language = language;
    }

    public IntegerFilter getFailedAttempts() {
        return failedAttempts;
    }

    public Optional<IntegerFilter> optionalFailedAttempts() {
        return Optional.ofNullable(failedAttempts);
    }

    public IntegerFilter failedAttempts() {
        if (failedAttempts == null) {
            setFailedAttempts(new IntegerFilter());
        }
        return failedAttempts;
    }

    public void setFailedAttempts(IntegerFilter failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public InstantFilter getLastLoginAt() {
        return lastLoginAt;
    }

    public Optional<InstantFilter> optionalLastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
    }

    public InstantFilter lastLoginAt() {
        if (lastLoginAt == null) {
            setLastLoginAt(new InstantFilter());
        }
        return lastLoginAt;
    }

    public void setLastLoginAt(InstantFilter lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LongFilter getDirectoryCursor() {
        return directoryCursor;
    }

    public Optional<LongFilter> optionalDirectoryCursor() {
        return Optional.ofNullable(directoryCursor);
    }

    public LongFilter directoryCursor() {
        if (directoryCursor == null) {
            setDirectoryCursor(new LongFilter());
        }
        return directoryCursor;
    }

    public void setDirectoryCursor(LongFilter directoryCursor) {
        this.directoryCursor = directoryCursor;
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
        final CollaboratorCriteria that = (CollaboratorCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(federatedId, that.federatedId) &&
            Objects.equals(directoryId, that.directoryId) &&
            Objects.equals(displayName, that.displayName) &&
            Objects.equals(email, that.email) &&
            Objects.equals(status, that.status) &&
            Objects.equals(level, that.level) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(language, that.language) &&
            Objects.equals(failedAttempts, that.failedAttempts) &&
            Objects.equals(lastLoginAt, that.lastLoginAt) &&
            Objects.equals(directoryCursor, that.directoryCursor) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            federatedId,
            directoryId,
            displayName,
            email,
            status,
            level,
            institutionId,
            language,
            failedAttempts,
            lastLoginAt,
            directoryCursor,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CollaboratorCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFederatedId().map(f -> "federatedId=" + f + ", ").orElse("") +
            optionalDirectoryId().map(f -> "directoryId=" + f + ", ").orElse("") +
            optionalDisplayName().map(f -> "displayName=" + f + ", ").orElse("") +
            optionalEmail().map(f -> "email=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalLevel().map(f -> "level=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalLanguage().map(f -> "language=" + f + ", ").orElse("") +
            optionalFailedAttempts().map(f -> "failedAttempts=" + f + ", ").orElse("") +
            optionalLastLoginAt().map(f -> "lastLoginAt=" + f + ", ").orElse("") +
            optionalDirectoryCursor().map(f -> "directoryCursor=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
