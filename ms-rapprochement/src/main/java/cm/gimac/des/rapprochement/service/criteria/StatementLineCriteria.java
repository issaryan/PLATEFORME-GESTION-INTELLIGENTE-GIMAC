package cm.gimac.des.rapprochement.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.rapprochement.domain.StatementLine} entity. This class is used
 * in {@link cm.gimac.des.rapprochement.web.rest.StatementLineResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /statement-lines?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StatementLineCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private IntegerFilter lineNumber;

    private StringFilter transactionRef;

    private StringFilter authorizationRef;

    private InstantFilter operationAt;

    private BigDecimalFilter amount;

    private StringFilter participantCode;

    private StringFilter lineStatus;

    private StringFilter currency;

    private UUIDFilter statementId;

    private Boolean distinct;

    public StatementLineCriteria() {}

    public StatementLineCriteria(StatementLineCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.lineNumber = other.optionalLineNumber().map(IntegerFilter::copy).orElse(null);
        this.transactionRef = other.optionalTransactionRef().map(StringFilter::copy).orElse(null);
        this.authorizationRef = other.optionalAuthorizationRef().map(StringFilter::copy).orElse(null);
        this.operationAt = other.optionalOperationAt().map(InstantFilter::copy).orElse(null);
        this.amount = other.optionalAmount().map(BigDecimalFilter::copy).orElse(null);
        this.participantCode = other.optionalParticipantCode().map(StringFilter::copy).orElse(null);
        this.lineStatus = other.optionalLineStatus().map(StringFilter::copy).orElse(null);
        this.currency = other.optionalCurrency().map(StringFilter::copy).orElse(null);
        this.statementId = other.optionalStatementId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public StatementLineCriteria copy() {
        return new StatementLineCriteria(this);
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

    public IntegerFilter getLineNumber() {
        return lineNumber;
    }

    public Optional<IntegerFilter> optionalLineNumber() {
        return Optional.ofNullable(lineNumber);
    }

    public IntegerFilter lineNumber() {
        if (lineNumber == null) {
            setLineNumber(new IntegerFilter());
        }
        return lineNumber;
    }

    public void setLineNumber(IntegerFilter lineNumber) {
        this.lineNumber = lineNumber;
    }

    public StringFilter getTransactionRef() {
        return transactionRef;
    }

    public Optional<StringFilter> optionalTransactionRef() {
        return Optional.ofNullable(transactionRef);
    }

    public StringFilter transactionRef() {
        if (transactionRef == null) {
            setTransactionRef(new StringFilter());
        }
        return transactionRef;
    }

    public void setTransactionRef(StringFilter transactionRef) {
        this.transactionRef = transactionRef;
    }

    public StringFilter getAuthorizationRef() {
        return authorizationRef;
    }

    public Optional<StringFilter> optionalAuthorizationRef() {
        return Optional.ofNullable(authorizationRef);
    }

    public StringFilter authorizationRef() {
        if (authorizationRef == null) {
            setAuthorizationRef(new StringFilter());
        }
        return authorizationRef;
    }

    public void setAuthorizationRef(StringFilter authorizationRef) {
        this.authorizationRef = authorizationRef;
    }

    public InstantFilter getOperationAt() {
        return operationAt;
    }

    public Optional<InstantFilter> optionalOperationAt() {
        return Optional.ofNullable(operationAt);
    }

    public InstantFilter operationAt() {
        if (operationAt == null) {
            setOperationAt(new InstantFilter());
        }
        return operationAt;
    }

    public void setOperationAt(InstantFilter operationAt) {
        this.operationAt = operationAt;
    }

    public BigDecimalFilter getAmount() {
        return amount;
    }

    public Optional<BigDecimalFilter> optionalAmount() {
        return Optional.ofNullable(amount);
    }

    public BigDecimalFilter amount() {
        if (amount == null) {
            setAmount(new BigDecimalFilter());
        }
        return amount;
    }

    public void setAmount(BigDecimalFilter amount) {
        this.amount = amount;
    }

    public StringFilter getParticipantCode() {
        return participantCode;
    }

    public Optional<StringFilter> optionalParticipantCode() {
        return Optional.ofNullable(participantCode);
    }

    public StringFilter participantCode() {
        if (participantCode == null) {
            setParticipantCode(new StringFilter());
        }
        return participantCode;
    }

    public void setParticipantCode(StringFilter participantCode) {
        this.participantCode = participantCode;
    }

    public StringFilter getLineStatus() {
        return lineStatus;
    }

    public Optional<StringFilter> optionalLineStatus() {
        return Optional.ofNullable(lineStatus);
    }

    public StringFilter lineStatus() {
        if (lineStatus == null) {
            setLineStatus(new StringFilter());
        }
        return lineStatus;
    }

    public void setLineStatus(StringFilter lineStatus) {
        this.lineStatus = lineStatus;
    }

    public StringFilter getCurrency() {
        return currency;
    }

    public Optional<StringFilter> optionalCurrency() {
        return Optional.ofNullable(currency);
    }

    public StringFilter currency() {
        if (currency == null) {
            setCurrency(new StringFilter());
        }
        return currency;
    }

    public void setCurrency(StringFilter currency) {
        this.currency = currency;
    }

    public UUIDFilter getStatementId() {
        return statementId;
    }

    public Optional<UUIDFilter> optionalStatementId() {
        return Optional.ofNullable(statementId);
    }

    public UUIDFilter statementId() {
        if (statementId == null) {
            setStatementId(new UUIDFilter());
        }
        return statementId;
    }

    public void setStatementId(UUIDFilter statementId) {
        this.statementId = statementId;
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
        final StatementLineCriteria that = (StatementLineCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(lineNumber, that.lineNumber) &&
            Objects.equals(transactionRef, that.transactionRef) &&
            Objects.equals(authorizationRef, that.authorizationRef) &&
            Objects.equals(operationAt, that.operationAt) &&
            Objects.equals(amount, that.amount) &&
            Objects.equals(participantCode, that.participantCode) &&
            Objects.equals(lineStatus, that.lineStatus) &&
            Objects.equals(currency, that.currency) &&
            Objects.equals(statementId, that.statementId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            lineNumber,
            transactionRef,
            authorizationRef,
            operationAt,
            amount,
            participantCode,
            lineStatus,
            currency,
            statementId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StatementLineCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLineNumber().map(f -> "lineNumber=" + f + ", ").orElse("") +
            optionalTransactionRef().map(f -> "transactionRef=" + f + ", ").orElse("") +
            optionalAuthorizationRef().map(f -> "authorizationRef=" + f + ", ").orElse("") +
            optionalOperationAt().map(f -> "operationAt=" + f + ", ").orElse("") +
            optionalAmount().map(f -> "amount=" + f + ", ").orElse("") +
            optionalParticipantCode().map(f -> "participantCode=" + f + ", ").orElse("") +
            optionalLineStatus().map(f -> "lineStatus=" + f + ", ").orElse("") +
            optionalCurrency().map(f -> "currency=" + f + ", ").orElse("") +
            optionalStatementId().map(f -> "statementId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
