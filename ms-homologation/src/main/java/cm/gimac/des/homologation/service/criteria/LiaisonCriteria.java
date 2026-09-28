package cm.gimac.des.homologation.service.criteria;

import cm.gimac.des.homologation.domain.enumeration.LiaisonState;
import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.Liaison} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.LiaisonResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /liaisons?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LiaisonCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ProjectType
     */
    public static class ProjectTypeFilter extends Filter<ProjectType> {

        public ProjectTypeFilter() {}

        public ProjectTypeFilter(ProjectTypeFilter filter) {
            super(filter);
        }

        @Override
        public ProjectTypeFilter copy() {
            return new ProjectTypeFilter(this);
        }
    }

    /**
     * Class for filtering LiaisonState
     */
    public static class LiaisonStateFilter extends Filter<LiaisonState> {

        public LiaisonStateFilter() {}

        public LiaisonStateFilter(LiaisonStateFilter filter) {
            super(filter);
        }

        @Override
        public LiaisonStateFilter copy() {
            return new LiaisonStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter liaisonCode;

    private UUIDFilter institutionId;

    private ProjectTypeFilter projectType;

    private LocalDateFilter openedOn;

    private UUIDFilter projectLeadId;

    private LiaisonStateFilter state;

    private StringFilter campaignRef;

    private InstantFilter readyForCertificationAt;

    private LocalDateFilter commissionedOn;

    private StringFilter stateReason;

    private UUIDFilter catalogId;

    private UUIDFilter campaignSyncId;

    private Boolean distinct;

    public LiaisonCriteria() {}

    public LiaisonCriteria(LiaisonCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.liaisonCode = other.optionalLiaisonCode().map(StringFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.projectType = other.optionalProjectType().map(ProjectTypeFilter::copy).orElse(null);
        this.openedOn = other.optionalOpenedOn().map(LocalDateFilter::copy).orElse(null);
        this.projectLeadId = other.optionalProjectLeadId().map(UUIDFilter::copy).orElse(null);
        this.state = other.optionalState().map(LiaisonStateFilter::copy).orElse(null);
        this.campaignRef = other.optionalCampaignRef().map(StringFilter::copy).orElse(null);
        this.readyForCertificationAt = other.optionalReadyForCertificationAt().map(InstantFilter::copy).orElse(null);
        this.commissionedOn = other.optionalCommissionedOn().map(LocalDateFilter::copy).orElse(null);
        this.stateReason = other.optionalStateReason().map(StringFilter::copy).orElse(null);
        this.catalogId = other.optionalCatalogId().map(UUIDFilter::copy).orElse(null);
        this.campaignSyncId = other.optionalCampaignSyncId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public LiaisonCriteria copy() {
        return new LiaisonCriteria(this);
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

    public StringFilter getLiaisonCode() {
        return liaisonCode;
    }

    public Optional<StringFilter> optionalLiaisonCode() {
        return Optional.ofNullable(liaisonCode);
    }

    public StringFilter liaisonCode() {
        if (liaisonCode == null) {
            setLiaisonCode(new StringFilter());
        }
        return liaisonCode;
    }

    public void setLiaisonCode(StringFilter liaisonCode) {
        this.liaisonCode = liaisonCode;
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

    public ProjectTypeFilter getProjectType() {
        return projectType;
    }

    public Optional<ProjectTypeFilter> optionalProjectType() {
        return Optional.ofNullable(projectType);
    }

    public ProjectTypeFilter projectType() {
        if (projectType == null) {
            setProjectType(new ProjectTypeFilter());
        }
        return projectType;
    }

    public void setProjectType(ProjectTypeFilter projectType) {
        this.projectType = projectType;
    }

    public LocalDateFilter getOpenedOn() {
        return openedOn;
    }

    public Optional<LocalDateFilter> optionalOpenedOn() {
        return Optional.ofNullable(openedOn);
    }

    public LocalDateFilter openedOn() {
        if (openedOn == null) {
            setOpenedOn(new LocalDateFilter());
        }
        return openedOn;
    }

    public void setOpenedOn(LocalDateFilter openedOn) {
        this.openedOn = openedOn;
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

    public LiaisonStateFilter getState() {
        return state;
    }

    public Optional<LiaisonStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public LiaisonStateFilter state() {
        if (state == null) {
            setState(new LiaisonStateFilter());
        }
        return state;
    }

    public void setState(LiaisonStateFilter state) {
        this.state = state;
    }

    public StringFilter getCampaignRef() {
        return campaignRef;
    }

    public Optional<StringFilter> optionalCampaignRef() {
        return Optional.ofNullable(campaignRef);
    }

    public StringFilter campaignRef() {
        if (campaignRef == null) {
            setCampaignRef(new StringFilter());
        }
        return campaignRef;
    }

    public void setCampaignRef(StringFilter campaignRef) {
        this.campaignRef = campaignRef;
    }

    public InstantFilter getReadyForCertificationAt() {
        return readyForCertificationAt;
    }

    public Optional<InstantFilter> optionalReadyForCertificationAt() {
        return Optional.ofNullable(readyForCertificationAt);
    }

    public InstantFilter readyForCertificationAt() {
        if (readyForCertificationAt == null) {
            setReadyForCertificationAt(new InstantFilter());
        }
        return readyForCertificationAt;
    }

    public void setReadyForCertificationAt(InstantFilter readyForCertificationAt) {
        this.readyForCertificationAt = readyForCertificationAt;
    }

    public LocalDateFilter getCommissionedOn() {
        return commissionedOn;
    }

    public Optional<LocalDateFilter> optionalCommissionedOn() {
        return Optional.ofNullable(commissionedOn);
    }

    public LocalDateFilter commissionedOn() {
        if (commissionedOn == null) {
            setCommissionedOn(new LocalDateFilter());
        }
        return commissionedOn;
    }

    public void setCommissionedOn(LocalDateFilter commissionedOn) {
        this.commissionedOn = commissionedOn;
    }

    public StringFilter getStateReason() {
        return stateReason;
    }

    public Optional<StringFilter> optionalStateReason() {
        return Optional.ofNullable(stateReason);
    }

    public StringFilter stateReason() {
        if (stateReason == null) {
            setStateReason(new StringFilter());
        }
        return stateReason;
    }

    public void setStateReason(StringFilter stateReason) {
        this.stateReason = stateReason;
    }

    public UUIDFilter getCatalogId() {
        return catalogId;
    }

    public Optional<UUIDFilter> optionalCatalogId() {
        return Optional.ofNullable(catalogId);
    }

    public UUIDFilter catalogId() {
        if (catalogId == null) {
            setCatalogId(new UUIDFilter());
        }
        return catalogId;
    }

    public void setCatalogId(UUIDFilter catalogId) {
        this.catalogId = catalogId;
    }

    public UUIDFilter getCampaignSyncId() {
        return campaignSyncId;
    }

    public Optional<UUIDFilter> optionalCampaignSyncId() {
        return Optional.ofNullable(campaignSyncId);
    }

    public UUIDFilter campaignSyncId() {
        if (campaignSyncId == null) {
            setCampaignSyncId(new UUIDFilter());
        }
        return campaignSyncId;
    }

    public void setCampaignSyncId(UUIDFilter campaignSyncId) {
        this.campaignSyncId = campaignSyncId;
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
        final LiaisonCriteria that = (LiaisonCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(liaisonCode, that.liaisonCode) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(projectType, that.projectType) &&
            Objects.equals(openedOn, that.openedOn) &&
            Objects.equals(projectLeadId, that.projectLeadId) &&
            Objects.equals(state, that.state) &&
            Objects.equals(campaignRef, that.campaignRef) &&
            Objects.equals(readyForCertificationAt, that.readyForCertificationAt) &&
            Objects.equals(commissionedOn, that.commissionedOn) &&
            Objects.equals(stateReason, that.stateReason) &&
            Objects.equals(catalogId, that.catalogId) &&
            Objects.equals(campaignSyncId, that.campaignSyncId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            liaisonCode,
            institutionId,
            projectType,
            openedOn,
            projectLeadId,
            state,
            campaignRef,
            readyForCertificationAt,
            commissionedOn,
            stateReason,
            catalogId,
            campaignSyncId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LiaisonCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLiaisonCode().map(f -> "liaisonCode=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalProjectType().map(f -> "projectType=" + f + ", ").orElse("") +
            optionalOpenedOn().map(f -> "openedOn=" + f + ", ").orElse("") +
            optionalProjectLeadId().map(f -> "projectLeadId=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalCampaignRef().map(f -> "campaignRef=" + f + ", ").orElse("") +
            optionalReadyForCertificationAt().map(f -> "readyForCertificationAt=" + f + ", ").orElse("") +
            optionalCommissionedOn().map(f -> "commissionedOn=" + f + ", ").orElse("") +
            optionalStateReason().map(f -> "stateReason=" + f + ", ").orElse("") +
            optionalCatalogId().map(f -> "catalogId=" + f + ", ").orElse("") +
            optionalCampaignSyncId().map(f -> "campaignSyncId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
