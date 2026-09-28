package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.LiaisonState;
import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.Liaison} entity.
 */
@Schema(
    description = "Liaison d'une institution pour un type de projet (CC §6.1.7.1). Elle fige la\nversion du référentiel en vigueur à son ouverture. Ouverte à la suite de\nparticipant.status-changed.v1 vers EN_HOMOLOGATION (D-18)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LiaisonDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^LIA-[0-9]{4}-[0-9]{3,}$")
    @Schema(description = "Identifiant lisible, par exemple LIA-2026-017.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String liaisonCode;

    @NotNull
    @Schema(description = "Institution tenue par MS-PARTICIPANT (référence, BNF-04).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID institutionId;

    @NotNull
    private ProjectType projectType;

    @NotNull
    private LocalDate openedOn;

    @NotNull
    @Schema(description = "Chef de projet de la liaison (collaborateur de MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID projectLeadId;

    @NotNull
    private LiaisonState state;

    @Size(max = 100)
    @Schema(
        description = "Référence de la campagne dans le simulateur (κL de l'algorithme 6.32).\nÉcart CC corrigé : absente de la classe Liaison, requise par l'algorithme."
    )
    private String campaignRef;

    private Instant readyForCertificationAt;

    private LocalDate commissionedOn;

    @Size(max = 1000)
    @Schema(description = "Motif du gel (HOM-10) ou du retour en homologation (HOM-11).")
    private String stateReason;

    @NotNull
    @Schema(description = "Version du référentiel figée sur la liaison (migrée seulement par l'algorithme 6.34).")
    private ReferenceCatalogDTO catalog;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLiaisonCode() {
        return liaisonCode;
    }

    public void setLiaisonCode(String liaisonCode) {
        this.liaisonCode = liaisonCode;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    public UUID getProjectLeadId() {
        return projectLeadId;
    }

    public void setProjectLeadId(UUID projectLeadId) {
        this.projectLeadId = projectLeadId;
    }

    public LiaisonState getState() {
        return state;
    }

    public void setState(LiaisonState state) {
        this.state = state;
    }

    public String getCampaignRef() {
        return campaignRef;
    }

    public void setCampaignRef(String campaignRef) {
        this.campaignRef = campaignRef;
    }

    public Instant getReadyForCertificationAt() {
        return readyForCertificationAt;
    }

    public void setReadyForCertificationAt(Instant readyForCertificationAt) {
        this.readyForCertificationAt = readyForCertificationAt;
    }

    public LocalDate getCommissionedOn() {
        return commissionedOn;
    }

    public void setCommissionedOn(LocalDate commissionedOn) {
        this.commissionedOn = commissionedOn;
    }

    public String getStateReason() {
        return stateReason;
    }

    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }

    public ReferenceCatalogDTO getCatalog() {
        return catalog;
    }

    public void setCatalog(ReferenceCatalogDTO catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiaisonDTO)) {
            return false;
        }

        LiaisonDTO liaisonDTO = (LiaisonDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, liaisonDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LiaisonDTO{" +
            "id='" + getId() + "'" +
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
            ", catalog=" + getCatalog() +
            "}";
    }
}
