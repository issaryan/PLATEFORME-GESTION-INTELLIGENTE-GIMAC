package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fiche institutionnelle (BF-07). Le statut ne change que par une
 * transition approuvée (StatusTransition) ; aucune donnée de routage.
 */
@Entity
@Table(name = "institution")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Institution implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Généré à la création, ou attribué par la reprise de l'existant (D-107, UUID déjà porté par des comptes). */
    @Id
    @IdentifiantAttribuable
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Size(max = 50)
    @Column(name = "acronym", length = 50)
    private String acronym;

    /**
     * Code participant GIMAC, par exemple EP-A-017 ; attribué au prononcé de l'adhésion (D-57).
     */
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9-]+$")
    @Column(name = "participant_code", length = 20, unique = true)
    private String participantCode;

    /**
     * Code d'identification ISO 9362 (BIC), s'il existe (BF-07).
     */
    @Size(max = 11)
    @Pattern(regexp = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")
    @Column(name = "identification_code", length = 11, unique = true)
    private String identificationCode;

    /**
     * Code banque BEAC, unique (RG-10).
     */
    @Size(max = 10)
    @Pattern(regexp = "^[A-Z0-9]+$")
    @Column(name = "beac_bank_code", length = 10, unique = true)
    private String beacBankCode;

    /**
     * Pays, code ISO 3166-1 alpha-2 : CEMAC à la saisie (formulaire du registre) ; la reprise de l'existant admet un
     * partenaire hors CEMAC (remittance par un agrégateur, D-107).
     */
    @NotNull
    @Size(max = 2)
    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(name = "country", length = 2, nullable = false)
    private String country;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private InstitutionCategory category;

    /**
     * Mode d'adhésion ; exigé au prononcé de l'adhésion, inconnu à la prise de contact.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_mode")
    private MembershipMode membershipMode;

    /**
     * Référence du compte de règlement à la BEAC, obligatoire en adhésion directe (RG-11).
     */
    @Size(max = 50)
    @Column(name = "settlement_account_ref", length = 50)
    private String settlementAccountRef;

    /**
     * Référence du compte de cantonnement, en adhésion indirecte (RG-12).
     */
    @Size(max = 50)
    @Column(name = "ring_fenced_account_ref", length = 50)
    private String ringFencedAccountRef;

    @Column(name = "joined_on")
    private LocalDate joinedOn;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstitutionStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_state", nullable = false)
    private ComplianceState complianceState;

    /**
     * Réception de certification.attestation-issued.v1 (prérequis de ACTIF).
     */
    @Column(name = "attestation_issued_at")
    private Instant attestationIssuedAt;

    /**
     * Réception de certification.go-live-authorized.v1 (RG-13).
     */
    @Column(name = "go_live_authorized_at")
    private Instant goLiveAuthorizedAt;

    /**
     * Liaisons ouvertes dans MS-HOMOLOGATION, connues par événement (prérequis de RETIRE).
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "open_link_count", nullable = false)
    private Integer openLinkCount;

    /**
     * Suspens ouverts dans MS-RAPPROCHEMENT, connus par événement (prérequis de RETIRE).
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "open_suspense_count", nullable = false)
    private Integer openSuspenseCount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Date de la prise de contact (rang 1).
     */
    @Column(name = "contacted_on")
    private LocalDate contactedOn;

    /**
     * Origine de la prise de contact : courrier, réunion, salon, recommandation...
     */
    @Size(max = 255)
    @Column(name = "contact_origin", length = 255)
    private String contactOrigin;

    /**
     * Projet envisagé par le candidat, tel qu'exprimé à la prise de contact.
     */
    @Size(max = 1000)
    @Column(name = "intended_project", length = 1000)
    private String intendedProject;

    /**
     * Chargé de la DDRM qui porte le dossier.
     */
    @Column(name = "ddrm_officer_id")
    private UUID ddrmOfficerId;

    @Size(max = 200)
    @Column(name = "ddrm_officer_name", length = 200)
    private String ddrmOfficerName;

    /**
     * Prononcé de l'adhésion (rang 2) : date et auteur.
     */
    @Column(name = "admitted_at")
    private Instant admittedAt;

    @Column(name = "admitted_by_id")
    private UUID admittedById;

    /**
     * Clôture sans suite : motif et date.
     */
    @Size(max = 500)
    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Banque de règlement et banque marraine : institutions du même registre.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution settlementBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution sponsorBank;

    /**
     * Groupe ou maison mère (D-104), facultatif, par exemple « Groupe Afriland First Bank » : chaque filiale est une
     * institution à part entière (son propre code membre) ; le groupe les rassemble. Libellé et non référence à une
     * institution du registre : la maison mère est souvent hors CEMAC ou non participante.
     */
    @Size(max = 100)
    @Column(name = "group_name", length = 100)
    private String groupName;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Institution id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Institution name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return this.acronym;
    }

    public Institution acronym(String acronym) {
        this.setAcronym(acronym);
        return this;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getParticipantCode() {
        return this.participantCode;
    }

    public Institution participantCode(String participantCode) {
        this.setParticipantCode(participantCode);
        return this;
    }

    public void setParticipantCode(String participantCode) {
        this.participantCode = participantCode;
    }

    public String getIdentificationCode() {
        return this.identificationCode;
    }

    public Institution identificationCode(String identificationCode) {
        this.setIdentificationCode(identificationCode);
        return this;
    }

    public void setIdentificationCode(String identificationCode) {
        this.identificationCode = identificationCode;
    }

    public String getBeacBankCode() {
        return this.beacBankCode;
    }

    public Institution beacBankCode(String beacBankCode) {
        this.setBeacBankCode(beacBankCode);
        return this;
    }

    public void setBeacBankCode(String beacBankCode) {
        this.beacBankCode = beacBankCode;
    }

    public String getCountry() {
        return this.country;
    }

    public Institution country(String country) {
        this.setCountry(country);
        return this;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public InstitutionCategory getCategory() {
        return this.category;
    }

    public Institution category(InstitutionCategory category) {
        this.setCategory(category);
        return this;
    }

    public void setCategory(InstitutionCategory category) {
        this.category = category;
    }

    public MembershipMode getMembershipMode() {
        return this.membershipMode;
    }

    public Institution membershipMode(MembershipMode membershipMode) {
        this.setMembershipMode(membershipMode);
        return this;
    }

    public void setMembershipMode(MembershipMode membershipMode) {
        this.membershipMode = membershipMode;
    }

    public String getSettlementAccountRef() {
        return this.settlementAccountRef;
    }

    public Institution settlementAccountRef(String settlementAccountRef) {
        this.setSettlementAccountRef(settlementAccountRef);
        return this;
    }

    public void setSettlementAccountRef(String settlementAccountRef) {
        this.settlementAccountRef = settlementAccountRef;
    }

    public String getRingFencedAccountRef() {
        return this.ringFencedAccountRef;
    }

    public Institution ringFencedAccountRef(String ringFencedAccountRef) {
        this.setRingFencedAccountRef(ringFencedAccountRef);
        return this;
    }

    public void setRingFencedAccountRef(String ringFencedAccountRef) {
        this.ringFencedAccountRef = ringFencedAccountRef;
    }

    public LocalDate getJoinedOn() {
        return this.joinedOn;
    }

    public Institution joinedOn(LocalDate joinedOn) {
        this.setJoinedOn(joinedOn);
        return this;
    }

    public void setJoinedOn(LocalDate joinedOn) {
        this.joinedOn = joinedOn;
    }

    public InstitutionStatus getStatus() {
        return this.status;
    }

    public Institution status(InstitutionStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(InstitutionStatus status) {
        this.status = status;
    }

    public ComplianceState getComplianceState() {
        return this.complianceState;
    }

    public Institution complianceState(ComplianceState complianceState) {
        this.setComplianceState(complianceState);
        return this;
    }

    public void setComplianceState(ComplianceState complianceState) {
        this.complianceState = complianceState;
    }

    public Instant getAttestationIssuedAt() {
        return this.attestationIssuedAt;
    }

    public Institution attestationIssuedAt(Instant attestationIssuedAt) {
        this.setAttestationIssuedAt(attestationIssuedAt);
        return this;
    }

    public void setAttestationIssuedAt(Instant attestationIssuedAt) {
        this.attestationIssuedAt = attestationIssuedAt;
    }

    public Instant getGoLiveAuthorizedAt() {
        return this.goLiveAuthorizedAt;
    }

    public Institution goLiveAuthorizedAt(Instant goLiveAuthorizedAt) {
        this.setGoLiveAuthorizedAt(goLiveAuthorizedAt);
        return this;
    }

    public void setGoLiveAuthorizedAt(Instant goLiveAuthorizedAt) {
        this.goLiveAuthorizedAt = goLiveAuthorizedAt;
    }

    public Integer getOpenLinkCount() {
        return this.openLinkCount;
    }

    public Institution openLinkCount(Integer openLinkCount) {
        this.setOpenLinkCount(openLinkCount);
        return this;
    }

    public void setOpenLinkCount(Integer openLinkCount) {
        this.openLinkCount = openLinkCount;
    }

    public Integer getOpenSuspenseCount() {
        return this.openSuspenseCount;
    }

    public Institution openSuspenseCount(Integer openSuspenseCount) {
        this.setOpenSuspenseCount(openSuspenseCount);
        return this;
    }

    public void setOpenSuspenseCount(Integer openSuspenseCount) {
        this.openSuspenseCount = openSuspenseCount;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Institution createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getContactedOn() {
        return this.contactedOn;
    }

    public Institution contactedOn(LocalDate contactedOn) {
        this.setContactedOn(contactedOn);
        return this;
    }

    public void setContactedOn(LocalDate contactedOn) {
        this.contactedOn = contactedOn;
    }

    public String getContactOrigin() {
        return this.contactOrigin;
    }

    public Institution contactOrigin(String contactOrigin) {
        this.setContactOrigin(contactOrigin);
        return this;
    }

    public void setContactOrigin(String contactOrigin) {
        this.contactOrigin = contactOrigin;
    }

    public String getIntendedProject() {
        return this.intendedProject;
    }

    public Institution intendedProject(String intendedProject) {
        this.setIntendedProject(intendedProject);
        return this;
    }

    public void setIntendedProject(String intendedProject) {
        this.intendedProject = intendedProject;
    }

    public UUID getDdrmOfficerId() {
        return this.ddrmOfficerId;
    }

    public Institution ddrmOfficerId(UUID ddrmOfficerId) {
        this.setDdrmOfficerId(ddrmOfficerId);
        return this;
    }

    public void setDdrmOfficerId(UUID ddrmOfficerId) {
        this.ddrmOfficerId = ddrmOfficerId;
    }

    public String getDdrmOfficerName() {
        return this.ddrmOfficerName;
    }

    public Institution ddrmOfficerName(String ddrmOfficerName) {
        this.setDdrmOfficerName(ddrmOfficerName);
        return this;
    }

    public void setDdrmOfficerName(String ddrmOfficerName) {
        this.ddrmOfficerName = ddrmOfficerName;
    }

    public Instant getAdmittedAt() {
        return this.admittedAt;
    }

    public Institution admittedAt(Instant admittedAt) {
        this.setAdmittedAt(admittedAt);
        return this;
    }

    public void setAdmittedAt(Instant admittedAt) {
        this.admittedAt = admittedAt;
    }

    public UUID getAdmittedById() {
        return this.admittedById;
    }

    public Institution admittedById(UUID admittedById) {
        this.setAdmittedById(admittedById);
        return this;
    }

    public void setAdmittedById(UUID admittedById) {
        this.admittedById = admittedById;
    }

    public String getClosureReason() {
        return this.closureReason;
    }

    public Institution closureReason(String closureReason) {
        this.setClosureReason(closureReason);
        return this;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public Institution closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Institution getSettlementBank() {
        return this.settlementBank;
    }

    public void setSettlementBank(Institution institution) {
        this.settlementBank = institution;
    }

    public Institution settlementBank(Institution institution) {
        this.setSettlementBank(institution);
        return this;
    }

    public Institution getSponsorBank() {
        return this.sponsorBank;
    }

    public void setSponsorBank(Institution institution) {
        this.sponsorBank = institution;
    }

    public Institution sponsorBank(Institution institution) {
        this.setSponsorBank(institution);
        return this;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Institution groupName(String groupName) {
        this.setGroupName(groupName);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Institution)) {
            return false;
        }
        return getId() != null && getId().equals(((Institution) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Institution{" +
            "id=" + getId() +
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
            "}";
    }
}
