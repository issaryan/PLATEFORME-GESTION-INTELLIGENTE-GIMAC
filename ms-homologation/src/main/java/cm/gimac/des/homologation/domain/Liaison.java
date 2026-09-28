package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.LiaisonState;
import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Liaison d'une institution pour un type de projet (CC §6.1.7.1). Elle fige la
 * version du référentiel en vigueur à son ouverture. Ouverte à la suite de
 * participant.status-changed.v1 vers EN_HOMOLOGATION (D-18).
 */
@Entity
@Table(name = "liaison")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Liaison implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant lisible, par exemple LIA-2026-017.
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^LIA-[0-9]{4}-[0-9]{3,}$")
    @Column(name = "liaison_code", length = 20, nullable = false, unique = true)
    private String liaisonCode;

    /**
     * Institution tenue par MS-PARTICIPANT (référence, BNF-04).
     */
    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false)
    private ProjectType projectType;

    @NotNull
    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    /**
     * Chef de projet de la liaison (collaborateur de MS-IAM).
     */
    @NotNull
    @Column(name = "project_lead_id", nullable = false)
    private UUID projectLeadId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private LiaisonState state;

    /**
     * Référence de la campagne dans le simulateur (κL de l'algorithme 6.32).
     * Écart CC corrigé : absente de la classe Liaison, requise par l'algorithme.
     */
    @Size(max = 100)
    @Column(name = "campaign_ref", length = 100)
    private String campaignRef;

    @Column(name = "ready_for_certification_at")
    private Instant readyForCertificationAt;

    @Column(name = "commissioned_on")
    private LocalDate commissionedOn;

    /**
     * Motif du gel (HOM-10) ou du retour en homologation (HOM-11).
     */
    @Size(max = 1000)
    @Column(name = "state_reason", length = 1000)
    private String stateReason;

    /**
     * Version du référentiel figée sur la liaison (migrée seulement par l'algorithme 6.34).
     */
    @ManyToOne(optional = false)
    @NotNull
    private ReferenceCatalog catalog;

    @JsonIgnoreProperties(value = { "liaison" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "liaison")
    private CampaignSync campaignSync;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Liaison id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLiaisonCode() {
        return this.liaisonCode;
    }

    public Liaison liaisonCode(String liaisonCode) {
        this.setLiaisonCode(liaisonCode);
        return this;
    }

    public void setLiaisonCode(String liaisonCode) {
        this.liaisonCode = liaisonCode;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Liaison institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public ProjectType getProjectType() {
        return this.projectType;
    }

    public Liaison projectType(ProjectType projectType) {
        this.setProjectType(projectType);
        return this;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public LocalDate getOpenedOn() {
        return this.openedOn;
    }

    public Liaison openedOn(LocalDate openedOn) {
        this.setOpenedOn(openedOn);
        return this;
    }

    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    public UUID getProjectLeadId() {
        return this.projectLeadId;
    }

    public Liaison projectLeadId(UUID projectLeadId) {
        this.setProjectLeadId(projectLeadId);
        return this;
    }

    public void setProjectLeadId(UUID projectLeadId) {
        this.projectLeadId = projectLeadId;
    }

    public LiaisonState getState() {
        return this.state;
    }

    public Liaison state(LiaisonState state) {
        this.setState(state);
        return this;
    }

    public void setState(LiaisonState state) {
        this.state = state;
    }

    public String getCampaignRef() {
        return this.campaignRef;
    }

    public Liaison campaignRef(String campaignRef) {
        this.setCampaignRef(campaignRef);
        return this;
    }

    public void setCampaignRef(String campaignRef) {
        this.campaignRef = campaignRef;
    }

    public Instant getReadyForCertificationAt() {
        return this.readyForCertificationAt;
    }

    public Liaison readyForCertificationAt(Instant readyForCertificationAt) {
        this.setReadyForCertificationAt(readyForCertificationAt);
        return this;
    }

    public void setReadyForCertificationAt(Instant readyForCertificationAt) {
        this.readyForCertificationAt = readyForCertificationAt;
    }

    public LocalDate getCommissionedOn() {
        return this.commissionedOn;
    }

    public Liaison commissionedOn(LocalDate commissionedOn) {
        this.setCommissionedOn(commissionedOn);
        return this;
    }

    public void setCommissionedOn(LocalDate commissionedOn) {
        this.commissionedOn = commissionedOn;
    }

    public String getStateReason() {
        return this.stateReason;
    }

    public Liaison stateReason(String stateReason) {
        this.setStateReason(stateReason);
        return this;
    }

    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }

    public ReferenceCatalog getCatalog() {
        return this.catalog;
    }

    public void setCatalog(ReferenceCatalog referenceCatalog) {
        this.catalog = referenceCatalog;
    }

    public Liaison catalog(ReferenceCatalog referenceCatalog) {
        this.setCatalog(referenceCatalog);
        return this;
    }

    public CampaignSync getCampaignSync() {
        return this.campaignSync;
    }

    public void setCampaignSync(CampaignSync campaignSync) {
        if (this.campaignSync != null) {
            this.campaignSync.setLiaison(null);
        }
        if (campaignSync != null) {
            campaignSync.setLiaison(this);
        }
        this.campaignSync = campaignSync;
    }

    public Liaison campaignSync(CampaignSync campaignSync) {
        this.setCampaignSync(campaignSync);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Liaison)) {
            return false;
        }
        return getId() != null && getId().equals(((Liaison) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Liaison{" +
            "id=" + getId() +
            ", liaisonCode='" + getLiaisonCode() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", projectType='" + getProjectType() + "'" +
            ", openedOn='" + getOpenedOn() + "'" +
            ", projectLeadId='" + getProjectLeadId() + "'" +
            ", state='" + getState() + "'" +
            ", campaignRef='" + getCampaignRef() + "'" +
            ", readyForCertificationAt='" + getReadyForCertificationAt() + "'" +
            ", commissionedOn='" + getCommissionedOn() + "'" +
            ", stateReason='" + getStateReason() + "'" +
            "}";
    }
}
