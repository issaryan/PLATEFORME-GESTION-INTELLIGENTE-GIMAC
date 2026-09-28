package cm.gimac.des.certification.service.criteria;

import cm.gimac.des.certification.domain.enumeration.FileState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.certification.domain.CommissioningFile} entity. This class is used
 * in {@link cm.gimac.des.certification.web.rest.CommissioningFileResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /commissioning-files?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CommissioningFileCriteria implements Serializable, Criteria {

    /**
     * Class for filtering FileState
     */
    public static class FileStateFilter extends Filter<FileState> {

        public FileStateFilter() {}

        public FileStateFilter(FileStateFilter filter) {
            super(filter);
        }

        @Override
        public FileStateFilter copy() {
            return new FileStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter fileCode;

    private UUIDFilter liaisonId;

    private UUIDFilter institutionId;

    private UUIDFilter projectLeadId;

    private FileStateFilter state;

    private StringFilter basisDigest;

    private InstantFilter basisSealedAt;

    private InstantFilter openedAt;

    private StringFilter correctionReason;

    private StringFilter manifestDigest;

    private InstantFilter sealedAt;

    private InstantFilter goLiveAuthorizedAt;

    private UUIDFilter changeRecordId;

    private UUIDFilter attestationId;

    private UUIDFilter commissioningReportId;

    private UUIDFilter quitusId;

    private UUIDFilter piecesId;

    private Boolean distinct;

    public CommissioningFileCriteria() {}

    public CommissioningFileCriteria(CommissioningFileCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.fileCode = other.optionalFileCode().map(StringFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.projectLeadId = other.optionalProjectLeadId().map(UUIDFilter::copy).orElse(null);
        this.state = other.optionalState().map(FileStateFilter::copy).orElse(null);
        this.basisDigest = other.optionalBasisDigest().map(StringFilter::copy).orElse(null);
        this.basisSealedAt = other.optionalBasisSealedAt().map(InstantFilter::copy).orElse(null);
        this.openedAt = other.optionalOpenedAt().map(InstantFilter::copy).orElse(null);
        this.correctionReason = other.optionalCorrectionReason().map(StringFilter::copy).orElse(null);
        this.manifestDigest = other.optionalManifestDigest().map(StringFilter::copy).orElse(null);
        this.sealedAt = other.optionalSealedAt().map(InstantFilter::copy).orElse(null);
        this.goLiveAuthorizedAt = other.optionalGoLiveAuthorizedAt().map(InstantFilter::copy).orElse(null);
        this.changeRecordId = other.optionalChangeRecordId().map(UUIDFilter::copy).orElse(null);
        this.attestationId = other.optionalAttestationId().map(UUIDFilter::copy).orElse(null);
        this.commissioningReportId = other.optionalCommissioningReportId().map(UUIDFilter::copy).orElse(null);
        this.quitusId = other.optionalQuitusId().map(UUIDFilter::copy).orElse(null);
        this.piecesId = other.optionalPiecesId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CommissioningFileCriteria copy() {
        return new CommissioningFileCriteria(this);
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

    public StringFilter getFileCode() {
        return fileCode;
    }

    public Optional<StringFilter> optionalFileCode() {
        return Optional.ofNullable(fileCode);
    }

    public StringFilter fileCode() {
        if (fileCode == null) {
            setFileCode(new StringFilter());
        }
        return fileCode;
    }

    public void setFileCode(StringFilter fileCode) {
        this.fileCode = fileCode;
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

    public UUIDFilter getProjectLeadId() {
        return projectLeadId;
    }

    public Optional<UUIDFilter> optionalProjectLeadId() {
        return Optional.ofNullable(projectLeadId);
    }

    public UUIDFilter projectLeadId() {
        if (projectLeadId == null) {
            setProjectLeadId(new UUIDFilter());
        }
        return projectLeadId;
    }

    public void setProjectLeadId(UUIDFilter projectLeadId) {
        this.projectLeadId = projectLeadId;
    }

    public FileStateFilter getState() {
        return state;
    }

    public Optional<FileStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public FileStateFilter state() {
        if (state == null) {
            setState(new FileStateFilter());
        }
        return state;
    }

    public void setState(FileStateFilter state) {
        this.state = state;
    }

    public StringFilter getBasisDigest() {
        return basisDigest;
    }

    public Optional<StringFilter> optionalBasisDigest() {
        return Optional.ofNullable(basisDigest);
    }

    public StringFilter basisDigest() {
        if (basisDigest == null) {
            setBasisDigest(new StringFilter());
        }
        return basisDigest;
    }

    public void setBasisDigest(StringFilter basisDigest) {
        this.basisDigest = basisDigest;
    }

    public InstantFilter getBasisSealedAt() {
        return basisSealedAt;
    }

    public Optional<InstantFilter> optionalBasisSealedAt() {
        return Optional.ofNullable(basisSealedAt);
    }

    public InstantFilter basisSealedAt() {
        if (basisSealedAt == null) {
            setBasisSealedAt(new InstantFilter());
        }
        return basisSealedAt;
    }

    public void setBasisSealedAt(InstantFilter basisSealedAt) {
        this.basisSealedAt = basisSealedAt;
    }

    public InstantFilter getOpenedAt() {
        return openedAt;
    }

    public Optional<InstantFilter> optionalOpenedAt() {
        return Optional.ofNullable(openedAt);
    }

    public InstantFilter openedAt() {
        if (openedAt == null) {
            setOpenedAt(new InstantFilter());
        }
        return openedAt;
    }

    public void setOpenedAt(InstantFilter openedAt) {
        this.openedAt = openedAt;
    }

    public StringFilter getCorrectionReason() {
        return correctionReason;
    }

    public Optional<StringFilter> optionalCorrectionReason() {
        return Optional.ofNullable(correctionReason);
    }

    public StringFilter correctionReason() {
        if (correctionReason == null) {
            setCorrectionReason(new StringFilter());
        }
        return correctionReason;
    }

    public void setCorrectionReason(StringFilter correctionReason) {
        this.correctionReason = correctionReason;
    }

    public StringFilter getManifestDigest() {
        return manifestDigest;
    }

    public Optional<StringFilter> optionalManifestDigest() {
        return Optional.ofNullable(manifestDigest);
    }

    public StringFilter manifestDigest() {
        if (manifestDigest == null) {
            setManifestDigest(new StringFilter());
        }
        return manifestDigest;
    }

    public void setManifestDigest(StringFilter manifestDigest) {
        this.manifestDigest = manifestDigest;
    }

    public InstantFilter getSealedAt() {
        return sealedAt;
    }

    public Optional<InstantFilter> optionalSealedAt() {
        return Optional.ofNullable(sealedAt);
    }

    public InstantFilter sealedAt() {
        if (sealedAt == null) {
            setSealedAt(new InstantFilter());
        }
        return sealedAt;
    }

    public void setSealedAt(InstantFilter sealedAt) {
        this.sealedAt = sealedAt;
    }

    public InstantFilter getGoLiveAuthorizedAt() {
        return goLiveAuthorizedAt;
    }

    public Optional<InstantFilter> optionalGoLiveAuthorizedAt() {
        return Optional.ofNullable(goLiveAuthorizedAt);
    }

    public InstantFilter goLiveAuthorizedAt() {
        if (goLiveAuthorizedAt == null) {
            setGoLiveAuthorizedAt(new InstantFilter());
        }
        return goLiveAuthorizedAt;
    }

    public void setGoLiveAuthorizedAt(InstantFilter goLiveAuthorizedAt) {
        this.goLiveAuthorizedAt = goLiveAuthorizedAt;
    }

    public UUIDFilter getChangeRecordId() {
        return changeRecordId;
    }

    public Optional<UUIDFilter> optionalChangeRecordId() {
        return Optional.ofNullable(changeRecordId);
    }

    public UUIDFilter changeRecordId() {
        if (changeRecordId == null) {
            setChangeRecordId(new UUIDFilter());
        }
        return changeRecordId;
    }

    public void setChangeRecordId(UUIDFilter changeRecordId) {
        this.changeRecordId = changeRecordId;
    }

    public UUIDFilter getAttestationId() {
        return attestationId;
    }

    public Optional<UUIDFilter> optionalAttestationId() {
        return Optional.ofNullable(attestationId);
    }

    public UUIDFilter attestationId() {
        if (attestationId == null) {
            setAttestationId(new UUIDFilter());
        }
        return attestationId;
    }

    public void setAttestationId(UUIDFilter attestationId) {
        this.attestationId = attestationId;
    }

    public UUIDFilter getCommissioningReportId() {
        return commissioningReportId;
    }

    public Optional<UUIDFilter> optionalCommissioningReportId() {
        return Optional.ofNullable(commissioningReportId);
    }

    public UUIDFilter commissioningReportId() {
        if (commissioningReportId == null) {
            setCommissioningReportId(new UUIDFilter());
        }
        return commissioningReportId;
    }

    public void setCommissioningReportId(UUIDFilter commissioningReportId) {
        this.commissioningReportId = commissioningReportId;
    }

    public UUIDFilter getQuitusId() {
        return quitusId;
    }

    public Optional<UUIDFilter> optionalQuitusId() {
        return Optional.ofNullable(quitusId);
    }

    public UUIDFilter quitusId() {
        if (quitusId == null) {
            setQuitusId(new UUIDFilter());
        }
        return quitusId;
    }

    public void setQuitusId(UUIDFilter quitusId) {
        this.quitusId = quitusId;
    }

    public UUIDFilter getPiecesId() {
        return piecesId;
    }

    public Optional<UUIDFilter> optionalPiecesId() {
        return Optional.ofNullable(piecesId);
    }

    public UUIDFilter piecesId() {
        if (piecesId == null) {
            setPiecesId(new UUIDFilter());
        }
        return piecesId;
    }

    public void setPiecesId(UUIDFilter piecesId) {
        this.piecesId = piecesId;
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
        final CommissioningFileCriteria that = (CommissioningFileCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(fileCode, that.fileCode) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(projectLeadId, that.projectLeadId) &&
            Objects.equals(state, that.state) &&
            Objects.equals(basisDigest, that.basisDigest) &&
            Objects.equals(basisSealedAt, that.basisSealedAt) &&
            Objects.equals(openedAt, that.openedAt) &&
            Objects.equals(correctionReason, that.correctionReason) &&
            Objects.equals(manifestDigest, that.manifestDigest) &&
            Objects.equals(sealedAt, that.sealedAt) &&
            Objects.equals(goLiveAuthorizedAt, that.goLiveAuthorizedAt) &&
            Objects.equals(changeRecordId, that.changeRecordId) &&
            Objects.equals(attestationId, that.attestationId) &&
            Objects.equals(commissioningReportId, that.commissioningReportId) &&
            Objects.equals(quitusId, that.quitusId) &&
            Objects.equals(piecesId, that.piecesId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            fileCode,
            liaisonId,
            institutionId,
            projectLeadId,
            state,
            basisDigest,
            basisSealedAt,
            openedAt,
            correctionReason,
            manifestDigest,
            sealedAt,
            goLiveAuthorizedAt,
            changeRecordId,
            attestationId,
            commissioningReportId,
            quitusId,
            piecesId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CommissioningFileCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFileCode().map(f -> "fileCode=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalProjectLeadId().map(f -> "projectLeadId=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalBasisDigest().map(f -> "basisDigest=" + f + ", ").orElse("") +
            optionalBasisSealedAt().map(f -> "basisSealedAt=" + f + ", ").orElse("") +
            optionalOpenedAt().map(f -> "openedAt=" + f + ", ").orElse("") +
            optionalCorrectionReason().map(f -> "correctionReason=" + f + ", ").orElse("") +
            optionalManifestDigest().map(f -> "manifestDigest=" + f + ", ").orElse("") +
            optionalSealedAt().map(f -> "sealedAt=" + f + ", ").orElse("") +
            optionalGoLiveAuthorizedAt().map(f -> "goLiveAuthorizedAt=" + f + ", ").orElse("") +
            optionalChangeRecordId().map(f -> "changeRecordId=" + f + ", ").orElse("") +
            optionalAttestationId().map(f -> "attestationId=" + f + ", ").orElse("") +
            optionalCommissioningReportId().map(f -> "commissioningReportId=" + f + ", ").orElse("") +
            optionalQuitusId().map(f -> "quitusId=" + f + ", ").orElse("") +
            optionalPiecesId().map(f -> "piecesId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
