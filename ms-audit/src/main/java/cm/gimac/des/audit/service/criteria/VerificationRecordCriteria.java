package cm.gimac.des.audit.service.criteria;

import cm.gimac.des.audit.domain.enumeration.VerificationResult;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.audit.domain.VerificationRecord} entity. This class is used
 * in {@link cm.gimac.des.audit.web.rest.VerificationRecordResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /verification-records?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VerificationRecordCriteria implements Serializable, Criteria {

    /**
     * Class for filtering VerificationResult
     */
    public static class VerificationResultFilter extends Filter<VerificationResult> {

        public VerificationResultFilter() {}

        public VerificationResultFilter(VerificationResultFilter filter) {
            super(filter);
        }

        @Override
        public VerificationResultFilter copy() {
            return new VerificationResultFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter verifierId;

    private InstantFilter verifiedAt;

    private StringFilter expectedRoot;

    private StringFilter recomputedRoot;

    private BooleanFilter tokenValid;

    private VerificationResultFilter result;

    private StringFilter comment;

    private UUIDFilter proofId;

    private Boolean distinct;

    public VerificationRecordCriteria() {}

    public VerificationRecordCriteria(VerificationRecordCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.verifierId = other.optionalVerifierId().map(UUIDFilter::copy).orElse(null);
        this.verifiedAt = other.optionalVerifiedAt().map(InstantFilter::copy).orElse(null);
        this.expectedRoot = other.optionalExpectedRoot().map(StringFilter::copy).orElse(null);
        this.recomputedRoot = other.optionalRecomputedRoot().map(StringFilter::copy).orElse(null);
        this.tokenValid = other.optionalTokenValid().map(BooleanFilter::copy).orElse(null);
        this.result = other.optionalResult().map(VerificationResultFilter::copy).orElse(null);
        this.comment = other.optionalComment().map(StringFilter::copy).orElse(null);
        this.proofId = other.optionalProofId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public VerificationRecordCriteria copy() {
        return new VerificationRecordCriteria(this);
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

    public UUIDFilter getVerifierId() {
        return verifierId;
    }

    public Optional<UUIDFilter> optionalVerifierId() {
        return Optional.ofNullable(verifierId);
    }

    public UUIDFilter verifierId() {
        if (verifierId == null) {
            setVerifierId(new UUIDFilter());
        }
        return verifierId;
    }

    public void setVerifierId(UUIDFilter verifierId) {
        this.verifierId = verifierId;
    }

    public InstantFilter getVerifiedAt() {
        return verifiedAt;
    }

    public Optional<InstantFilter> optionalVerifiedAt() {
        return Optional.ofNullable(verifiedAt);
    }

    public InstantFilter verifiedAt() {
        if (verifiedAt == null) {
            setVerifiedAt(new InstantFilter());
        }
        return verifiedAt;
    }

    public void setVerifiedAt(InstantFilter verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public StringFilter getExpectedRoot() {
        return expectedRoot;
    }

    public Optional<StringFilter> optionalExpectedRoot() {
        return Optional.ofNullable(expectedRoot);
    }

    public StringFilter expectedRoot() {
        if (expectedRoot == null) {
            setExpectedRoot(new StringFilter());
        }
        return expectedRoot;
    }

    public void setExpectedRoot(StringFilter expectedRoot) {
        this.expectedRoot = expectedRoot;
    }

    public StringFilter getRecomputedRoot() {
        return recomputedRoot;
    }

    public Optional<StringFilter> optionalRecomputedRoot() {
        return Optional.ofNullable(recomputedRoot);
    }

    public StringFilter recomputedRoot() {
        if (recomputedRoot == null) {
            setRecomputedRoot(new StringFilter());
        }
        return recomputedRoot;
    }

    public void setRecomputedRoot(StringFilter recomputedRoot) {
        this.recomputedRoot = recomputedRoot;
    }

    public BooleanFilter getTokenValid() {
        return tokenValid;
    }

    public Optional<BooleanFilter> optionalTokenValid() {
        return Optional.ofNullable(tokenValid);
    }

    public BooleanFilter tokenValid() {
        if (tokenValid == null) {
            setTokenValid(new BooleanFilter());
        }
        return tokenValid;
    }

    public void setTokenValid(BooleanFilter tokenValid) {
        this.tokenValid = tokenValid;
    }

    public VerificationResultFilter getResult() {
        return result;
    }

    public Optional<VerificationResultFilter> optionalResult() {
        return Optional.ofNullable(result);
    }

    public VerificationResultFilter result() {
        if (result == null) {
            setResult(new VerificationResultFilter());
        }
        return result;
    }

    public void setResult(VerificationResultFilter result) {
        this.result = result;
    }

    public StringFilter getComment() {
        return comment;
    }

    public Optional<StringFilter> optionalComment() {
        return Optional.ofNullable(comment);
    }

    public StringFilter comment() {
        if (comment == null) {
            setComment(new StringFilter());
        }
        return comment;
    }

    public void setComment(StringFilter comment) {
        this.comment = comment;
    }

    public UUIDFilter getProofId() {
        return proofId;
    }

    public Optional<UUIDFilter> optionalProofId() {
        return Optional.ofNullable(proofId);
    }

    public UUIDFilter proofId() {
        if (proofId == null) {
            setProofId(new UUIDFilter());
        }
        return proofId;
    }

    public void setProofId(UUIDFilter proofId) {
        this.proofId = proofId;
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
        final VerificationRecordCriteria that = (VerificationRecordCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(verifierId, that.verifierId) &&
            Objects.equals(verifiedAt, that.verifiedAt) &&
            Objects.equals(expectedRoot, that.expectedRoot) &&
            Objects.equals(recomputedRoot, that.recomputedRoot) &&
            Objects.equals(tokenValid, that.tokenValid) &&
            Objects.equals(result, that.result) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(proofId, that.proofId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, verifierId, verifiedAt, expectedRoot, recomputedRoot, tokenValid, result, comment, proofId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VerificationRecordCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalVerifierId().map(f -> "verifierId=" + f + ", ").orElse("") +
            optionalVerifiedAt().map(f -> "verifiedAt=" + f + ", ").orElse("") +
            optionalExpectedRoot().map(f -> "expectedRoot=" + f + ", ").orElse("") +
            optionalRecomputedRoot().map(f -> "recomputedRoot=" + f + ", ").orElse("") +
            optionalTokenValid().map(f -> "tokenValid=" + f + ", ").orElse("") +
            optionalResult().map(f -> "result=" + f + ", ").orElse("") +
            optionalComment().map(f -> "comment=" + f + ", ").orElse("") +
            optionalProofId().map(f -> "proofId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
