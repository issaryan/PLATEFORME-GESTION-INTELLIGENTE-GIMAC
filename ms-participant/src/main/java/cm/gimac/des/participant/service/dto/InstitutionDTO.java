package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.Institution} entity.
 */
@Schema(
    description = "Fiche institutionnelle (BF-07). Le statut ne change que par une\ntransition approuvée (StatusTransition) ; aucune donnée de routage."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InstitutionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String name;

    @Size(max = 50)
    private String acronym;

    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$")
    @Schema(description = "Code participant GIMAC, par exemple EP-A-017 ; attribué au prononcé de l'adhésion (D-57).")
    private String participantCode;

    @Size(max = 11)
    @Pattern(regexp = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")
    @Schema(description = "Code d'identification ISO 9362 (BIC), s'il existe (BF-07).")
    private String identificationCode;

    @Size(max = 10)
    @Pattern(regexp = "^[A-Z0-9]+$")
    @Schema(description = "Code banque BEAC, unique (RG-10).")
    private String beacBankCode;

    @NotNull
    @Size(max = 2)
    @Pattern(regexp = "^(CM|CF|CG|GA|GQ|TD)$")
    @Schema(description = "Pays CEMAC, code ISO 3166-1 alpha-2.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String country;

    @NotNull
    private InstitutionCategory category;

    @Schema(description = "Mode d'adhésion ; exigé au prononcé de l'adhésion, inconnu à la prise de contact.")
    private MembershipMode membershipMode;

    @Size(max = 50)
    @Schema(description = "Référence du compte de règlement à la BEAC, obligatoire en adhésion directe (RG-11).")
    private String settlementAccountRef;

    @Size(max = 50)
    @Schema(description = "Référence du compte de cantonnement, en adhésion indirecte (RG-12).")
    private String ringFencedAccountRef;

    private LocalDate joinedOn;

    @NotNull
    private InstitutionStatus status;

    @NotNull
    private ComplianceState complianceState;

    @Schema(description = "Réception de certification.attestation-issued.v1 (prérequis de ACTIF).")
    private Instant attestationIssuedAt;

    @Schema(description = "Réception de certification.go-live-authorized.v1 (RG-13).")
    private Instant goLiveAuthorizedAt;

    @NotNull
    @Min(value = 0)
    @Schema(
        description = "Liaisons ouvertes dans MS-HOMOLOGATION, connues par événement (prérequis de RETIRE).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer openLinkCount;

    @NotNull
    @Min(value = 0)
    @Schema(
        description = "Suspens ouverts dans MS-RAPPROCHEMENT, connus par événement (prérequis de RETIRE).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer openSuspenseCount;

    @NotNull
    private Instant createdAt;

    @Schema(description = "Date de la prise de contact (rang 1).")
    private LocalDate contactedOn;

    @Size(max = 255)
    @Schema(description = "Origine de la prise de contact : courrier, réunion, salon, recommandation...")
    private String contactOrigin;

    @Size(max = 1000)
    @Schema(description = "Projet envisagé par le candidat, tel qu'exprimé à la prise de contact.")
    private String intendedProject;

    @Schema(description = "Chargé de la DDRM qui porte le dossier.")
    private UUID ddrmOfficerId;

    @Size(max = 200)
    private String ddrmOfficerName;

    @Schema(description = "Prononcé de l'adhésion (rang 2) : date et auteur.")
    private Instant admittedAt;

    private UUID admittedById;

    @Size(max = 500)
    @Schema(description = "Clôture sans suite : motif et date.")
    private String closureReason;

    private Instant closedAt;

    @Schema(description = "Banque de règlement et banque marraine : institutions du même registre.")
    private InstitutionDTO settlementBank;

    private InstitutionDTO sponsorBank;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getParticipantCode() {
        return participantCode;
    }

    public void setParticipantCode(String participantCode) {
        this.participantCode = participantCode;
    }

    public String getIdentificationCode() {
        return identificationCode;
    }

    public void setIdentificationCode(String identificationCode) {
        this.identificationCode = identificationCode;
    }

    public String getBeacBankCode() {
        return beacBankCode;
    }

    public void setBeacBankCode(String beacBankCode) {
        this.beacBankCode = beacBankCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public InstitutionCategory getCategory() {
        return category;
    }

    public void setCategory(InstitutionCategory category) {
        this.category = category;
    }

    public MembershipMode getMembershipMode() {
        return membershipMode;
    }

    public void setMembershipMode(MembershipMode membershipMode) {
        this.membershipMode = membershipMode;
    }

    public String getSettlementAccountRef() {
        return settlementAccountRef;
    }

    public void setSettlementAccountRef(String settlementAccountRef) {
        this.settlementAccountRef = settlementAccountRef;
    }

    public String getRingFencedAccountRef() {
        return ringFencedAccountRef;
    }

    public void setRingFencedAccountRef(String ringFencedAccountRef) {
        this.ringFencedAccountRef = ringFencedAccountRef;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public void setJoinedOn(LocalDate joinedOn) {
        this.joinedOn = joinedOn;
    }

    public InstitutionStatus getStatus() {
        return status;
    }

    public void setStatus(InstitutionStatus status) {
        this.status = status;
    }

    public ComplianceState getComplianceState() {
        return complianceState;
    }

    public void setComplianceState(ComplianceState complianceState) {
        this.complianceState = complianceState;
    }

    public Instant getAttestationIssuedAt() {
        return attestationIssuedAt;
    }

    public void setAttestationIssuedAt(Instant attestationIssuedAt) {
        this.attestationIssuedAt = attestationIssuedAt;
    }

    public Instant getGoLiveAuthorizedAt() {
        return goLiveAuthorizedAt;
    }

    public void setGoLiveAuthorizedAt(Instant goLiveAuthorizedAt) {
        this.goLiveAuthorizedAt = goLiveAuthorizedAt;
    }

    public Integer getOpenLinkCount() {
        return openLinkCount;
    }

    public void setOpenLinkCount(Integer openLinkCount) {
        this.openLinkCount = openLinkCount;
    }

    public Integer getOpenSuspenseCount() {
        return openSuspenseCount;
    }

    public void setOpenSuspenseCount(Integer openSuspenseCount) {
        this.openSuspenseCount = openSuspenseCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getContactedOn() {
        return contactedOn;
    }

    public void setContactedOn(LocalDate contactedOn) {
        this.contactedOn = contactedOn;
    }

    public String getContactOrigin() {
        return contactOrigin;
    }

    public void setContactOrigin(String contactOrigin) {
        this.contactOrigin = contactOrigin;
    }

    public String getIntendedProject() {
        return intendedProject;
    }

    public void setIntendedProject(String intendedProject) {
        this.intendedProject = intendedProject;
    }

    public UUID getDdrmOfficerId() {
        return ddrmOfficerId;
    }

    public void setDdrmOfficerId(UUID ddrmOfficerId) {
        this.ddrmOfficerId = ddrmOfficerId;
    }

    public String getDdrmOfficerName() {
        return ddrmOfficerName;
    }

    public void setDdrmOfficerName(String ddrmOfficerName) {
        this.ddrmOfficerName = ddrmOfficerName;
    }

    public Instant getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(Instant admittedAt) {
        this.admittedAt = admittedAt;
    }

    public UUID getAdmittedById() {
        return admittedById;
    }

    public void setAdmittedById(UUID admittedById) {
        this.admittedById = admittedById;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public InstitutionDTO getSettlementBank() {
        return settlementBank;
    }

    public void setSettlementBank(InstitutionDTO settlementBank) {
        this.settlementBank = settlementBank;
    }

    public InstitutionDTO getSponsorBank() {
        return sponsorBank;
    }

    public void setSponsorBank(InstitutionDTO sponsorBank) {
        this.sponsorBank = sponsorBank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InstitutionDTO)) {
            return false;
        }

        InstitutionDTO institutionDTO = (InstitutionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, institutionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InstitutionDTO{" +
            "id='" + getId() + "'" +
            ", name='" + getName() + "'" +
            ", acronym='" + getAcronym() + "'" +
            ", participantCode='" + getParticipantCode() + "'" +
            ", identificationCode='" + getIdentificationCode() + "'" +
            ", beacBankCode='" + getBeacBankCode() + "'" +
            ", country='" + getCountry() + "'" +
            ", category='" + getCategory() + "'" +
            ", membershipMode='" + getMembershipMode() + "'" +
            ", settlementAccountRef='" + getSettlementAccountRef() + "'" +
            ", ringFencedAccountRef='" + getRingFencedAccountRef() + "'" +
            ", joinedOn='" + getJoinedOn() + "'" +
            ", status='" + getStatus() + "'" +
            ", complianceState='" + getComplianceState() + "'" +
            ", attestationIssuedAt='" + getAttestationIssuedAt() + "'" +
            ", goLiveAuthorizedAt='" + getGoLiveAuthorizedAt() + "'" +
            ", openLinkCount=" + getOpenLinkCount() +
            ", openSuspenseCount=" + getOpenSuspenseCount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", contactedOn='" + getContactedOn() + "'" +
            ", contactOrigin='" + getContactOrigin() + "'" +
            ", intendedProject='" + getIntendedProject() + "'" +
            ", ddrmOfficerId='" + getDdrmOfficerId() + "'" +
            ", ddrmOfficerName='" + getDdrmOfficerName() + "'" +
            ", admittedAt='" + getAdmittedAt() + "'" +
            ", admittedById='" + getAdmittedById() + "'" +
            ", closureReason='" + getClosureReason() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", settlementBank=" + getSettlementBank() +
            ", sponsorBank=" + getSponsorBank() +
            "}";
    }
}
