package cm.gimac.des.homologation.service.criteria;

import cm.gimac.des.homologation.domain.enumeration.QrForm;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.QrVerification} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.QrVerificationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /qr-verifications?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QrVerificationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering QrForm
     */
    public static class QrFormFilter extends Filter<QrForm> {

        public QrFormFilter() {}

        public QrFormFilter(QrFormFilter filter) {
            super(filter);
        }

        @Override
        public QrFormFilter copy() {
            return new QrFormFilter(this);
        }
    }

    /**
     * Class for filtering ScenarioOutcome
     */
    public static class ScenarioOutcomeFilter extends Filter<ScenarioOutcome> {

        public ScenarioOutcomeFilter() {}

        public ScenarioOutcomeFilter(ScenarioOutcomeFilter filter) {
            super(filter);
        }

        @Override
        public ScenarioOutcomeFilter copy() {
            return new ScenarioOutcomeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private QrFormFilter form;

    private ScenarioOutcomeFilter outcome;

    private LocalDateFilter verifiedOn;

    private UUIDFilter verifierId;

    private UUIDFilter evidenceDocumentId;

    private StringFilter evidenceDigest;

    private InstantFilter evidenceSealedAt;

    private UUIDFilter liaisonId;

    private UUIDFilter operationId;

    private Boolean distinct;

    public QrVerificationCriteria() {}

    public QrVerificationCriteria(QrVerificationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.form = other.optionalForm().map(QrFormFilter::copy).orElse(null);
        this.outcome = other.optionalOutcome().map(ScenarioOutcomeFilter::copy).orElse(null);
        this.verifiedOn = other.optionalVerifiedOn().map(LocalDateFilter::copy).orElse(null);
        this.verifierId = other.optionalVerifierId().map(UUIDFilter::copy).orElse(null);
        this.evidenceDocumentId = other.optionalEvidenceDocumentId().map(UUIDFilter::copy).orElse(null);
        this.evidenceDigest = other.optionalEvidenceDigest().map(StringFilter::copy).orElse(null);
        this.evidenceSealedAt = other.optionalEvidenceSealedAt().map(InstantFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.operationId = other.optionalOperationId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public QrVerificationCriteria copy() {
        return new QrVerificationCriteria(this);
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

    public QrFormFilter getForm() {
        return form;
    }

    public Optional<QrFormFilter> optionalForm() {
        return Optional.ofNullable(form);
    }

    public QrFormFilter form() {
        if (form == null) {
            setForm(new QrFormFilter());
        }
        return form;
    }

    public void setForm(QrFormFilter form) {
        this.form = form;
    }

    public ScenarioOutcomeFilter getOutcome() {
        return outcome;
    }

    public Optional<ScenarioOutcomeFilter> optionalOutcome() {
        return Optional.ofNullable(outcome);
    }

    public ScenarioOutcomeFilter outcome() {
        if (outcome == null) {
            setOutcome(new ScenarioOutcomeFilter());
        }
        return outcome;
    }

    public void setOutcome(ScenarioOutcomeFilter outcome) {
        this.outcome = outcome;
    }

    public LocalDateFilter getVerifiedOn() {
        return verifiedOn;
    }

    public Optional<LocalDateFilter> optionalVerifiedOn() {
        return Optional.ofNullable(verifiedOn);
    }

    public LocalDateFilter verifiedOn() {
        if (verifiedOn == null) {
            setVerifiedOn(new LocalDateFilter());
        }
        return verifiedOn;
    }

    public void setVerifiedOn(LocalDateFilter verifiedOn) {
        this.verifiedOn = verifiedOn;
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

    public UUIDFilter getEvidenceDocumentId() {
        return evidenceDocumentId;
    }

    public Optional<UUIDFilter> optionalEvidenceDocumentId() {
        return Optional.ofNullable(evidenceDocumentId);
    }

    public UUIDFilter evidenceDocumentId() {
        if (evidenceDocumentId == null) {
            setEvidenceDocumentId(new UUIDFilter());
        }
        return evidenceDocumentId;
    }

    public void setEvidenceDocumentId(UUIDFilter evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public StringFilter getEvidenceDigest() {
        return evidenceDigest;
    }

    public Optional<StringFilter> optionalEvidenceDigest() {
        return Optional.ofNullable(evidenceDigest);
    }

    public StringFilter evidenceDigest() {
        if (evidenceDigest == null) {
            setEvidenceDigest(new StringFilter());
        }
        return evidenceDigest;
    }

    public void setEvidenceDigest(StringFilter evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public InstantFilter getEvidenceSealedAt() {
        return evidenceSealedAt;
    }

    public Optional<InstantFilter> optionalEvidenceSealedAt() {
        return Optional.ofNullable(evidenceSealedAt);
    }

    public InstantFilter evidenceSealedAt() {
        if (evidenceSealedAt == null) {
            setEvidenceSealedAt(new InstantFilter());
        }
        return evidenceSealedAt;
    }

    public void setEvidenceSealedAt(InstantFilter evidenceSealedAt) {
        this.evidenceSealedAt = evidenceSealedAt;
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

    public UUIDFilter getOperationId() {
        return operationId;
    }

    public Optional<UUIDFilter> optionalOperationId() {
        return Optional.ofNullable(operationId);
    }

    public UUIDFilter operationId() {
        if (operationId == null) {
            setOperationId(new UUIDFilter());
        }
        return operationId;
    }

    public void setOperationId(UUIDFilter operationId) {
        this.operationId = operationId;
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
        final QrVerificationCriteria that = (QrVerificationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(form, that.form) &&
            Objects.equals(outcome, that.outcome) &&
            Objects.equals(verifiedOn, that.verifiedOn) &&
            Objects.equals(verifierId, that.verifierId) &&
            Objects.equals(evidenceDocumentId, that.evidenceDocumentId) &&
            Objects.equals(evidenceDigest, that.evidenceDigest) &&
            Objects.equals(evidenceSealedAt, that.evidenceSealedAt) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(operationId, that.operationId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            form,
            outcome,
            verifiedOn,
            verifierId,
            evidenceDocumentId,
            evidenceDigest,
            evidenceSealedAt,
            liaisonId,
            operationId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QrVerificationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalForm().map(f -> "form=" + f + ", ").orElse("") +
            optionalOutcome().map(f -> "outcome=" + f + ", ").orElse("") +
            optionalVerifiedOn().map(f -> "verifiedOn=" + f + ", ").orElse("") +
            optionalVerifierId().map(f -> "verifierId=" + f + ", ").orElse("") +
            optionalEvidenceDocumentId().map(f -> "evidenceDocumentId=" + f + ", ").orElse("") +
            optionalEvidenceDigest().map(f -> "evidenceDigest=" + f + ", ").orElse("") +
            optionalEvidenceSealedAt().map(f -> "evidenceSealedAt=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalOperationId().map(f -> "operationId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
