package cm.gimac.des.collaboration.service.criteria;

import cm.gimac.des.collaboration.domain.enumeration.VerificationState;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.collaboration.domain.VerifiedTerminal} entity. This class is used
 * in {@link cm.gimac.des.collaboration.web.rest.VerifiedTerminalResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /verified-terminals?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VerifiedTerminalCriteria implements Serializable, Criteria {

    /**
     * Class for filtering VerificationState
     */
    public static class VerificationStateFilter extends Filter<VerificationState> {

        public VerificationStateFilter() {}

        public VerificationStateFilter(VerificationStateFilter filter) {
            super(filter);
        }

        @Override
        public VerificationStateFilter copy() {
            return new VerificationStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter userId;

    private StringFilter terminalId;

    private StringFilter identityKey;

    private StringFilter agreementKey;

    private StringFilter signedPreKey;

    private StringFilter signedPreKeySignature;

    private IntegerFilter oneTimePreKeys;

    private StringFilter crossSignature;

    private VerificationStateFilter state;

    private BooleanFilter archiveTerminal;

    private InstantFilter registeredAt;

    private InstantFilter verifiedAt;

    private InstantFilter revokedAt;

    private Boolean distinct;

    public VerifiedTerminalCriteria() {}

    public VerifiedTerminalCriteria(VerifiedTerminalCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.terminalId = other.optionalTerminalId().map(StringFilter::copy).orElse(null);
        this.identityKey = other.optionalIdentityKey().map(StringFilter::copy).orElse(null);
        this.agreementKey = other.optionalAgreementKey().map(StringFilter::copy).orElse(null);
        this.signedPreKey = other.optionalSignedPreKey().map(StringFilter::copy).orElse(null);
        this.signedPreKeySignature = other.optionalSignedPreKeySignature().map(StringFilter::copy).orElse(null);
        this.oneTimePreKeys = other.optionalOneTimePreKeys().map(IntegerFilter::copy).orElse(null);
        this.crossSignature = other.optionalCrossSignature().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(VerificationStateFilter::copy).orElse(null);
        this.archiveTerminal = other.optionalArchiveTerminal().map(BooleanFilter::copy).orElse(null);
        this.registeredAt = other.optionalRegisteredAt().map(InstantFilter::copy).orElse(null);
        this.verifiedAt = other.optionalVerifiedAt().map(InstantFilter::copy).orElse(null);
        this.revokedAt = other.optionalRevokedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public VerifiedTerminalCriteria copy() {
        return new VerifiedTerminalCriteria(this);
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

    public UUIDFilter getUserId() {
        return userId;
    }

    public Optional<UUIDFilter> optionalUserId() {
        return Optional.ofNullable(userId);
    }

    public UUIDFilter userId() {
        if (userId == null) {
            setUserId(new UUIDFilter());
        }
        return userId;
    }

    public void setUserId(UUIDFilter userId) {
        this.userId = userId;
    }

    public StringFilter getTerminalId() {
        return terminalId;
    }

    public Optional<StringFilter> optionalTerminalId() {
        return Optional.ofNullable(terminalId);
    }

    public StringFilter terminalId() {
        if (terminalId == null) {
            setTerminalId(new StringFilter());
        }
        return terminalId;
    }

    public void setTerminalId(StringFilter terminalId) {
        this.terminalId = terminalId;
    }

    public StringFilter getIdentityKey() {
        return identityKey;
    }

    public Optional<StringFilter> optionalIdentityKey() {
        return Optional.ofNullable(identityKey);
    }

    public StringFilter identityKey() {
        if (identityKey == null) {
            setIdentityKey(new StringFilter());
        }
        return identityKey;
    }

    public void setIdentityKey(StringFilter identityKey) {
        this.identityKey = identityKey;
    }

    public StringFilter getAgreementKey() {
        return agreementKey;
    }

    public Optional<StringFilter> optionalAgreementKey() {
        return Optional.ofNullable(agreementKey);
    }

    public StringFilter agreementKey() {
        if (agreementKey == null) {
            setAgreementKey(new StringFilter());
        }
        return agreementKey;
    }

    public void setAgreementKey(StringFilter agreementKey) {
        this.agreementKey = agreementKey;
    }

    public StringFilter getSignedPreKey() {
        return signedPreKey;
    }

    public Optional<StringFilter> optionalSignedPreKey() {
        return Optional.ofNullable(signedPreKey);
    }

    public StringFilter signedPreKey() {
        if (signedPreKey == null) {
            setSignedPreKey(new StringFilter());
        }
        return signedPreKey;
    }

    public void setSignedPreKey(StringFilter signedPreKey) {
        this.signedPreKey = signedPreKey;
    }

    public StringFilter getSignedPreKeySignature() {
        return signedPreKeySignature;
    }

    public Optional<StringFilter> optionalSignedPreKeySignature() {
        return Optional.ofNullable(signedPreKeySignature);
    }

    public StringFilter signedPreKeySignature() {
        if (signedPreKeySignature == null) {
            setSignedPreKeySignature(new StringFilter());
        }
        return signedPreKeySignature;
    }

    public void setSignedPreKeySignature(StringFilter signedPreKeySignature) {
        this.signedPreKeySignature = signedPreKeySignature;
    }

    public IntegerFilter getOneTimePreKeys() {
        return oneTimePreKeys;
    }

    public Optional<IntegerFilter> optionalOneTimePreKeys() {
        return Optional.ofNullable(oneTimePreKeys);
    }

    public IntegerFilter oneTimePreKeys() {
        if (oneTimePreKeys == null) {
            setOneTimePreKeys(new IntegerFilter());
        }
        return oneTimePreKeys;
    }

    public void setOneTimePreKeys(IntegerFilter oneTimePreKeys) {
        this.oneTimePreKeys = oneTimePreKeys;
    }

    public StringFilter getCrossSignature() {
        return crossSignature;
    }

    public Optional<StringFilter> optionalCrossSignature() {
        return Optional.ofNullable(crossSignature);
    }

    public StringFilter crossSignature() {
        if (crossSignature == null) {
            setCrossSignature(new StringFilter());
        }
        return crossSignature;
    }

    public void setCrossSignature(StringFilter crossSignature) {
        this.crossSignature = crossSignature;
    }

    public VerificationStateFilter getState() {
        return state;
    }

    public Optional<VerificationStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public VerificationStateFilter state() {
        if (state == null) {
            setState(new VerificationStateFilter());
        }
        return state;
    }

    public void setState(VerificationStateFilter state) {
        this.state = state;
    }

    public BooleanFilter getArchiveTerminal() {
        return archiveTerminal;
    }

    public Optional<BooleanFilter> optionalArchiveTerminal() {
        return Optional.ofNullable(archiveTerminal);
    }

    public BooleanFilter archiveTerminal() {
        if (archiveTerminal == null) {
            setArchiveTerminal(new BooleanFilter());
        }
        return archiveTerminal;
    }

    public void setArchiveTerminal(BooleanFilter archiveTerminal) {
        this.archiveTerminal = archiveTerminal;
    }

    public InstantFilter getRegisteredAt() {
        return registeredAt;
    }

    public Optional<InstantFilter> optionalRegisteredAt() {
        return Optional.ofNullable(registeredAt);
    }

    public InstantFilter registeredAt() {
        if (registeredAt == null) {
            setRegisteredAt(new InstantFilter());
        }
        return registeredAt;
    }

    public void setRegisteredAt(InstantFilter registeredAt) {
        this.registeredAt = registeredAt;
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
        final VerifiedTerminalCriteria that = (VerifiedTerminalCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(terminalId, that.terminalId) &&
            Objects.equals(identityKey, that.identityKey) &&
            Objects.equals(agreementKey, that.agreementKey) &&
            Objects.equals(signedPreKey, that.signedPreKey) &&
            Objects.equals(signedPreKeySignature, that.signedPreKeySignature) &&
            Objects.equals(oneTimePreKeys, that.oneTimePreKeys) &&
            Objects.equals(crossSignature, that.crossSignature) &&
            Objects.equals(state, that.state) &&
            Objects.equals(archiveTerminal, that.archiveTerminal) &&
            Objects.equals(registeredAt, that.registeredAt) &&
            Objects.equals(verifiedAt, that.verifiedAt) &&
            Objects.equals(revokedAt, that.revokedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            userId,
            terminalId,
            identityKey,
            agreementKey,
            signedPreKey,
            signedPreKeySignature,
            oneTimePreKeys,
            crossSignature,
            state,
            archiveTerminal,
            registeredAt,
            verifiedAt,
            revokedAt,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VerifiedTerminalCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalTerminalId().map(f -> "terminalId=" + f + ", ").orElse("") +
            optionalIdentityKey().map(f -> "identityKey=" + f + ", ").orElse("") +
            optionalAgreementKey().map(f -> "agreementKey=" + f + ", ").orElse("") +
            optionalSignedPreKey().map(f -> "signedPreKey=" + f + ", ").orElse("") +
            optionalSignedPreKeySignature().map(f -> "signedPreKeySignature=" + f + ", ").orElse("") +
            optionalOneTimePreKeys().map(f -> "oneTimePreKeys=" + f + ", ").orElse("") +
            optionalCrossSignature().map(f -> "crossSignature=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalArchiveTerminal().map(f -> "archiveTerminal=" + f + ", ").orElse("") +
            optionalRegisteredAt().map(f -> "registeredAt=" + f + ", ").orElse("") +
            optionalVerifiedAt().map(f -> "verifiedAt=" + f + ", ").orElse("") +
            optionalRevokedAt().map(f -> "revokedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
