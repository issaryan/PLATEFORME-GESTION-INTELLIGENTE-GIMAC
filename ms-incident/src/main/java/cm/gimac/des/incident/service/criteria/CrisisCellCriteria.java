package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.CrisisState;
import cm.gimac.des.incident.domain.enumeration.Severity;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.CrisisCell} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.CrisisCellResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /crisis-cells?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisCellCriteria implements Serializable, Criteria {

    /**
     * Class for filtering Severity
     */
    public static class SeverityFilter extends Filter<Severity> {

        public SeverityFilter() {}

        public SeverityFilter(SeverityFilter filter) {
            super(filter);
        }

        @Override
        public SeverityFilter copy() {
            return new SeverityFilter(this);
        }
    }

    /**
     * Class for filtering CrisisState
     */
    public static class CrisisStateFilter extends Filter<CrisisState> {

        public CrisisStateFilter() {}

        public CrisisStateFilter(CrisisStateFilter filter) {
            super(filter);
        }

        @Override
        public CrisisStateFilter copy() {
            return new CrisisStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter reference;

    private SeverityFilter level;

    private CrisisStateFilter state;

    private BooleanFilter automaticActivation;

    private StringFilter activationReason;

    private UUIDFilter convenedBy;

    private InstantFilter convenedAt;

    private InstantFilter quorumDeadline;

    private IntegerFilter quorumThreshold;

    private InstantFilter constitutedAt;

    private StringFilter roomRef;

    private InstantFilter postIncidentReportDueAt;

    private StringFilter postIncidentReportRef;

    private StringFilter postIncidentReportDigest;

    private UUIDFilter postIncidentReportSignedBy;

    private InstantFilter postIncidentReportSignedAt;

    private UUIDFilter closedBy;

    private InstantFilter closedAt;

    private StringFilter archiveRef;

    private InstantFilter archivedAt;

    private UUIDFilter incidentId;

    private Boolean distinct;

    public CrisisCellCriteria() {}

    public CrisisCellCriteria(CrisisCellCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.level = other.optionalLevel().map(SeverityFilter::copy).orElse(null);
        this.state = other.optionalState().map(CrisisStateFilter::copy).orElse(null);
        this.automaticActivation = other.optionalAutomaticActivation().map(BooleanFilter::copy).orElse(null);
        this.activationReason = other.optionalActivationReason().map(StringFilter::copy).orElse(null);
        this.convenedBy = other.optionalConvenedBy().map(UUIDFilter::copy).orElse(null);
        this.convenedAt = other.optionalConvenedAt().map(InstantFilter::copy).orElse(null);
        this.quorumDeadline = other.optionalQuorumDeadline().map(InstantFilter::copy).orElse(null);
        this.quorumThreshold = other.optionalQuorumThreshold().map(IntegerFilter::copy).orElse(null);
        this.constitutedAt = other.optionalConstitutedAt().map(InstantFilter::copy).orElse(null);
        this.roomRef = other.optionalRoomRef().map(StringFilter::copy).orElse(null);
        this.postIncidentReportDueAt = other.optionalPostIncidentReportDueAt().map(InstantFilter::copy).orElse(null);
        this.postIncidentReportRef = other.optionalPostIncidentReportRef().map(StringFilter::copy).orElse(null);
        this.postIncidentReportDigest = other.optionalPostIncidentReportDigest().map(StringFilter::copy).orElse(null);
        this.postIncidentReportSignedBy = other.optionalPostIncidentReportSignedBy().map(UUIDFilter::copy).orElse(null);
        this.postIncidentReportSignedAt = other.optionalPostIncidentReportSignedAt().map(InstantFilter::copy).orElse(null);
        this.closedBy = other.optionalClosedBy().map(UUIDFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.archiveRef = other.optionalArchiveRef().map(StringFilter::copy).orElse(null);
        this.archivedAt = other.optionalArchivedAt().map(InstantFilter::copy).orElse(null);
        this.incidentId = other.optionalIncidentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CrisisCellCriteria copy() {
        return new CrisisCellCriteria(this);
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

    public StringFilter getReference() {
        return reference;
    }

    public Optional<StringFilter> optionalReference() {
        return Optional.ofNullable(reference);
    }

    public StringFilter reference() {
        if (reference == null) {
            setReference(new StringFilter());
        }
        return reference;
    }

    public void setReference(StringFilter reference) {
        this.reference = reference;
    }

    public SeverityFilter getLevel() {
        return level;
    }

    public Optional<SeverityFilter> optionalLevel() {
        return Optional.ofNullable(level);
    }

    public SeverityFilter level() {
        if (level == null) {
            setLevel(new SeverityFilter());
        }
        return level;
    }

    public void setLevel(SeverityFilter level) {
        this.level = level;
    }

    public CrisisStateFilter getState() {
        return state;
    }

    public Optional<CrisisStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public CrisisStateFilter state() {
        if (state == null) {
            setState(new CrisisStateFilter());
        }
        return state;
    }

    public void setState(CrisisStateFilter state) {
        this.state = state;
    }

    public BooleanFilter getAutomaticActivation() {
        return automaticActivation;
    }

    public Optional<BooleanFilter> optionalAutomaticActivation() {
        return Optional.ofNullable(automaticActivation);
    }

    public BooleanFilter automaticActivation() {
        if (automaticActivation == null) {
            setAutomaticActivation(new BooleanFilter());
        }
        return automaticActivation;
    }

    public void setAutomaticActivation(BooleanFilter automaticActivation) {
        this.automaticActivation = automaticActivation;
    }

    public StringFilter getActivationReason() {
        return activationReason;
    }

    public Optional<StringFilter> optionalActivationReason() {
        return Optional.ofNullable(activationReason);
    }

    public StringFilter activationReason() {
        if (activationReason == null) {
            setActivationReason(new StringFilter());
        }
        return activationReason;
    }

    public void setActivationReason(StringFilter activationReason) {
        this.activationReason = activationReason;
    }

    public UUIDFilter getConvenedBy() {
        return convenedBy;
    }

    public Optional<UUIDFilter> optionalConvenedBy() {
        return Optional.ofNullable(convenedBy);
    }

    public UUIDFilter convenedBy() {
        if (convenedBy == null) {
            setConvenedBy(new UUIDFilter());
        }
        return convenedBy;
    }

    public void setConvenedBy(UUIDFilter convenedBy) {
        this.convenedBy = convenedBy;
    }

    public InstantFilter getConvenedAt() {
        return convenedAt;
    }

    public Optional<InstantFilter> optionalConvenedAt() {
        return Optional.ofNullable(convenedAt);
    }

    public InstantFilter convenedAt() {
        if (convenedAt == null) {
            setConvenedAt(new InstantFilter());
        }
        return convenedAt;
    }

    public void setConvenedAt(InstantFilter convenedAt) {
        this.convenedAt = convenedAt;
    }

    public InstantFilter getQuorumDeadline() {
        return quorumDeadline;
    }

    public Optional<InstantFilter> optionalQuorumDeadline() {
        return Optional.ofNullable(quorumDeadline);
    }

    public InstantFilter quorumDeadline() {
        if (quorumDeadline == null) {
            setQuorumDeadline(new InstantFilter());
        }
        return quorumDeadline;
    }

    public void setQuorumDeadline(InstantFilter quorumDeadline) {
        this.quorumDeadline = quorumDeadline;
    }

    public IntegerFilter getQuorumThreshold() {
        return quorumThreshold;
    }

    public Optional<IntegerFilter> optionalQuorumThreshold() {
        return Optional.ofNullable(quorumThreshold);
    }

    public IntegerFilter quorumThreshold() {
        if (quorumThreshold == null) {
            setQuorumThreshold(new IntegerFilter());
        }
        return quorumThreshold;
    }

    public void setQuorumThreshold(IntegerFilter quorumThreshold) {
        this.quorumThreshold = quorumThreshold;
    }

    public InstantFilter getConstitutedAt() {
        return constitutedAt;
    }

    public Optional<InstantFilter> optionalConstitutedAt() {
        return Optional.ofNullable(constitutedAt);
    }

    public InstantFilter constitutedAt() {
        if (constitutedAt == null) {
            setConstitutedAt(new InstantFilter());
        }
        return constitutedAt;
    }

    public void setConstitutedAt(InstantFilter constitutedAt) {
        this.constitutedAt = constitutedAt;
    }

    public StringFilter getRoomRef() {
        return roomRef;
    }

    public Optional<StringFilter> optionalRoomRef() {
        return Optional.ofNullable(roomRef);
    }

    public StringFilter roomRef() {
        if (roomRef == null) {
            setRoomRef(new StringFilter());
        }
        return roomRef;
    }

    public void setRoomRef(StringFilter roomRef) {
        this.roomRef = roomRef;
    }

    public InstantFilter getPostIncidentReportDueAt() {
        return postIncidentReportDueAt;
    }

    public Optional<InstantFilter> optionalPostIncidentReportDueAt() {
        return Optional.ofNullable(postIncidentReportDueAt);
    }

    public InstantFilter postIncidentReportDueAt() {
        if (postIncidentReportDueAt == null) {
            setPostIncidentReportDueAt(new InstantFilter());
        }
        return postIncidentReportDueAt;
    }

    public void setPostIncidentReportDueAt(InstantFilter postIncidentReportDueAt) {
        this.postIncidentReportDueAt = postIncidentReportDueAt;
    }

    public StringFilter getPostIncidentReportRef() {
        return postIncidentReportRef;
    }

    public Optional<StringFilter> optionalPostIncidentReportRef() {
        return Optional.ofNullable(postIncidentReportRef);
    }

    public StringFilter postIncidentReportRef() {
        if (postIncidentReportRef == null) {
            setPostIncidentReportRef(new StringFilter());
        }
        return postIncidentReportRef;
    }

    public void setPostIncidentReportRef(StringFilter postIncidentReportRef) {
        this.postIncidentReportRef = postIncidentReportRef;
    }

    public StringFilter getPostIncidentReportDigest() {
        return postIncidentReportDigest;
    }

    public Optional<StringFilter> optionalPostIncidentReportDigest() {
        return Optional.ofNullable(postIncidentReportDigest);
    }

    public StringFilter postIncidentReportDigest() {
        if (postIncidentReportDigest == null) {
            setPostIncidentReportDigest(new StringFilter());
        }
        return postIncidentReportDigest;
    }

    public void setPostIncidentReportDigest(StringFilter postIncidentReportDigest) {
        this.postIncidentReportDigest = postIncidentReportDigest;
    }

    public UUIDFilter getPostIncidentReportSignedBy() {
        return postIncidentReportSignedBy;
    }

    public Optional<UUIDFilter> optionalPostIncidentReportSignedBy() {
        return Optional.ofNullable(postIncidentReportSignedBy);
    }

    public UUIDFilter postIncidentReportSignedBy() {
        if (postIncidentReportSignedBy == null) {
            setPostIncidentReportSignedBy(new UUIDFilter());
        }
        return postIncidentReportSignedBy;
    }

    public void setPostIncidentReportSignedBy(UUIDFilter postIncidentReportSignedBy) {
        this.postIncidentReportSignedBy = postIncidentReportSignedBy;
    }

    public InstantFilter getPostIncidentReportSignedAt() {
        return postIncidentReportSignedAt;
    }

    public Optional<InstantFilter> optionalPostIncidentReportSignedAt() {
        return Optional.ofNullable(postIncidentReportSignedAt);
    }

    public InstantFilter postIncidentReportSignedAt() {
        if (postIncidentReportSignedAt == null) {
            setPostIncidentReportSignedAt(new InstantFilter());
        }
        return postIncidentReportSignedAt;
    }

    public void setPostIncidentReportSignedAt(InstantFilter postIncidentReportSignedAt) {
        this.postIncidentReportSignedAt = postIncidentReportSignedAt;
    }

    public UUIDFilter getClosedBy() {
        return closedBy;
    }

    public Optional<UUIDFilter> optionalClosedBy() {
        return Optional.ofNullable(closedBy);
    }

    public UUIDFilter closedBy() {
        if (closedBy == null) {
            setClosedBy(new UUIDFilter());
        }
        return closedBy;
    }

    public void setClosedBy(UUIDFilter closedBy) {
        this.closedBy = closedBy;
    }

    public InstantFilter getClosedAt() {
        return closedAt;
    }

    public Optional<InstantFilter> optionalClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public InstantFilter closedAt() {
        if (closedAt == null) {
            setClosedAt(new InstantFilter());
        }
        return closedAt;
    }

    public void setClosedAt(InstantFilter closedAt) {
        this.closedAt = closedAt;
    }

    public StringFilter getArchiveRef() {
        return archiveRef;
    }

    public Optional<StringFilter> optionalArchiveRef() {
        return Optional.ofNullable(archiveRef);
    }

    public StringFilter archiveRef() {
        if (archiveRef == null) {
            setArchiveRef(new StringFilter());
        }
        return archiveRef;
    }

    public void setArchiveRef(StringFilter archiveRef) {
        this.archiveRef = archiveRef;
    }

    public InstantFilter getArchivedAt() {
        return archivedAt;
    }

    public Optional<InstantFilter> optionalArchivedAt() {
        return Optional.ofNullable(archivedAt);
    }

    public InstantFilter archivedAt() {
        if (archivedAt == null) {
            setArchivedAt(new InstantFilter());
        }
        return archivedAt;
    }

    public void setArchivedAt(InstantFilter archivedAt) {
        this.archivedAt = archivedAt;
    }

    public UUIDFilter getIncidentId() {
        return incidentId;
    }

    public Optional<UUIDFilter> optionalIncidentId() {
        return Optional.ofNullable(incidentId);
    }

    public UUIDFilter incidentId() {
        if (incidentId == null) {
            setIncidentId(new UUIDFilter());
        }
        return incidentId;
    }

    public void setIncidentId(UUIDFilter incidentId) {
        this.incidentId = incidentId;
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
        final CrisisCellCriteria that = (CrisisCellCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(level, that.level) &&
            Objects.equals(state, that.state) &&
            Objects.equals(automaticActivation, that.automaticActivation) &&
            Objects.equals(activationReason, that.activationReason) &&
            Objects.equals(convenedBy, that.convenedBy) &&
            Objects.equals(convenedAt, that.convenedAt) &&
            Objects.equals(quorumDeadline, that.quorumDeadline) &&
            Objects.equals(quorumThreshold, that.quorumThreshold) &&
            Objects.equals(constitutedAt, that.constitutedAt) &&
            Objects.equals(roomRef, that.roomRef) &&
            Objects.equals(postIncidentReportDueAt, that.postIncidentReportDueAt) &&
            Objects.equals(postIncidentReportRef, that.postIncidentReportRef) &&
            Objects.equals(postIncidentReportDigest, that.postIncidentReportDigest) &&
            Objects.equals(postIncidentReportSignedBy, that.postIncidentReportSignedBy) &&
            Objects.equals(postIncidentReportSignedAt, that.postIncidentReportSignedAt) &&
            Objects.equals(closedBy, that.closedBy) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(archiveRef, that.archiveRef) &&
            Objects.equals(archivedAt, that.archivedAt) &&
            Objects.equals(incidentId, that.incidentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reference,
            level,
            state,
            automaticActivation,
            activationReason,
            convenedBy,
            convenedAt,
            quorumDeadline,
            quorumThreshold,
            constitutedAt,
            roomRef,
            postIncidentReportDueAt,
            postIncidentReportRef,
            postIncidentReportDigest,
            postIncidentReportSignedBy,
            postIncidentReportSignedAt,
            closedBy,
            closedAt,
            archiveRef,
            archivedAt,
            incidentId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisCellCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalLevel().map(f -> "level=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalAutomaticActivation().map(f -> "automaticActivation=" + f + ", ").orElse("") +
            optionalActivationReason().map(f -> "activationReason=" + f + ", ").orElse("") +
            optionalConvenedBy().map(f -> "convenedBy=" + f + ", ").orElse("") +
            optionalConvenedAt().map(f -> "convenedAt=" + f + ", ").orElse("") +
            optionalQuorumDeadline().map(f -> "quorumDeadline=" + f + ", ").orElse("") +
            optionalQuorumThreshold().map(f -> "quorumThreshold=" + f + ", ").orElse("") +
            optionalConstitutedAt().map(f -> "constitutedAt=" + f + ", ").orElse("") +
            optionalRoomRef().map(f -> "roomRef=" + f + ", ").orElse("") +
            optionalPostIncidentReportDueAt().map(f -> "postIncidentReportDueAt=" + f + ", ").orElse("") +
            optionalPostIncidentReportRef().map(f -> "postIncidentReportRef=" + f + ", ").orElse("") +
            optionalPostIncidentReportDigest().map(f -> "postIncidentReportDigest=" + f + ", ").orElse("") +
            optionalPostIncidentReportSignedBy().map(f -> "postIncidentReportSignedBy=" + f + ", ").orElse("") +
            optionalPostIncidentReportSignedAt().map(f -> "postIncidentReportSignedAt=" + f + ", ").orElse("") +
            optionalClosedBy().map(f -> "closedBy=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalArchiveRef().map(f -> "archiveRef=" + f + ", ").orElse("") +
            optionalArchivedAt().map(f -> "archivedAt=" + f + ", ").orElse("") +
            optionalIncidentId().map(f -> "incidentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
