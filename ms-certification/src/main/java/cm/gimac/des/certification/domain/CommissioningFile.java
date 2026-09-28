package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.FileState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dossier de mise en service d'une liaison, ouvert à la réception de
 * homologation.liaison-ready-for-certification.v1. Transitions autorisées :
 * OUVERT vers QUITUS_EN_COURS ; QUITUS_EN_COURS vers EN_CORRECTION ou CERTIFIE ;
 * EN_CORRECTION vers QUITUS_EN_COURS ; CERTIFIE vers MISE_EN_SERVICE_AUTORISEE
 * (terminal). Un seul dossier par liaison.
 */
@Entity
@Table(name = "commissioning_file")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CommissioningFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Identifiant lisible, par exemple DMS-2026-0009.
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^DMS-[0-9]{4}-[0-9]{4,}$")
    @Column(name = "file_code", length = 20, nullable = false, unique = true)
    private String fileCode;

    /**
     * Liaison tenue par MS-HOMOLOGATION (référence, BNF-04).
     */
    @NotNull
    @Column(name = "liaison_id", nullable = false, unique = true)
    private UUID liaisonId;

    /**
     * Institution tenue par MS-PARTICIPANT (référence, BNF-04).
     */
    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    /**
     * Chef de projet de la liaison, exclu des signataires (algorithme 6.35, étape 3).
     */
    @NotNull
    @Column(name = "project_lead_id", nullable = false)
    private UUID projectLeadId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private FileState state;

    /**
     * β = SHA256(JCS(socle d'évaluation)), RFC 8785.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "basis_digest", length = 64)
    private String basisDigest;

    /**
     * Jeton RFC 3161 du socle, encodé en DER (algorithme 6.37).
     */
    @Lob
    @Column(name = "basis_timestamp_token")
    private byte[] basisTimestampToken;

    @Column(name = "basis_timestamp_token_content_type")
    private String basisTimestampTokenContentType;

    @Column(name = "basis_sealed_at")
    private Instant basisSealedAt;

    @NotNull
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    /**
     * Motif du dernier retour en homologation.
     */
    @Size(max = 1000)
    @Column(name = "correction_reason", length = 1000)
    private String correctionReason;

    /**
     * Empreinte du manifeste MF (algorithme 6.36, étape 5).
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "manifest_digest", length = 64)
    private String manifestDigest;

    /**
     * Jeton RFC 3161 du manifeste, encodé en DER.
     */
    @Lob
    @Column(name = "timestamp_token")
    private byte[] timestampToken;

    @Column(name = "timestamp_token_content_type")
    private String timestampTokenContentType;

    @Column(name = "sealed_at")
    private Instant sealedAt;

    @Column(name = "go_live_authorized_at")
    private Instant goLiveAuthorizedAt;

    /**
     * Fiche de changement lue, au plus une par dossier.
     */
    @JsonIgnoreProperties(value = { "commissioningFile" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private ChangeRecord changeRecord;

    /**
     * Attestation émise, au plus une par dossier.
     */
    @JsonIgnoreProperties(value = { "commissioningFile" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private CertificationAttestation attestation;

    /**
     * Procès-verbal de mise en service, parmi les pièces du dossier (condition C6).
     */
    @JsonIgnoreProperties(value = { "file", "commissioningFile" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private EvidenceItem commissioningReport;

    /**
     * Les quatre quitus du dossier, un par type (RG-73).
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "file")
    @JsonIgnoreProperties(value = { "file" }, allowSetters = true)
    private Set<Quitus> quituses = new HashSet<>();

    /**
     * Pièces du dossier, avec leurs empreintes.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "file")
    @JsonIgnoreProperties(value = { "file", "commissioningFile" }, allowSetters = true)
    private Set<EvidenceItem> pieces = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CommissioningFile id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileCode() {
        return this.fileCode;
    }

    public CommissioningFile fileCode(String fileCode) {
        this.setFileCode(fileCode);
        return this;
    }

    public void setFileCode(String fileCode) {
        this.fileCode = fileCode;
    }

    public UUID getLiaisonId() {
        return this.liaisonId;
    }

    public CommissioningFile liaisonId(UUID liaisonId) {
        this.setLiaisonId(liaisonId);
        return this;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public CommissioningFile institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public UUID getProjectLeadId() {
        return this.projectLeadId;
    }

    public CommissioningFile projectLeadId(UUID projectLeadId) {
        this.setProjectLeadId(projectLeadId);
        return this;
    }

    public void setProjectLeadId(UUID projectLeadId) {
        this.projectLeadId = projectLeadId;
    }

    public FileState getState() {
        return this.state;
    }

    public CommissioningFile state(FileState state) {
        this.setState(state);
        return this;
    }

    public void setState(FileState state) {
        this.state = state;
    }

    public String getBasisDigest() {
        return this.basisDigest;
    }

    public CommissioningFile basisDigest(String basisDigest) {
        this.setBasisDigest(basisDigest);
        return this;
    }

    public void setBasisDigest(String basisDigest) {
        this.basisDigest = basisDigest;
    }

    public byte[] getBasisTimestampToken() {
        return this.basisTimestampToken;
    }

    public CommissioningFile basisTimestampToken(byte[] basisTimestampToken) {
        this.setBasisTimestampToken(basisTimestampToken);
        return this;
    }

    public void setBasisTimestampToken(byte[] basisTimestampToken) {
        this.basisTimestampToken = basisTimestampToken;
    }

    public String getBasisTimestampTokenContentType() {
        return this.basisTimestampTokenContentType;
    }

    public CommissioningFile basisTimestampTokenContentType(String basisTimestampTokenContentType) {
        this.basisTimestampTokenContentType = basisTimestampTokenContentType;
        return this;
    }

    public void setBasisTimestampTokenContentType(String basisTimestampTokenContentType) {
        this.basisTimestampTokenContentType = basisTimestampTokenContentType;
    }

    public Instant getBasisSealedAt() {
        return this.basisSealedAt;
    }

    public CommissioningFile basisSealedAt(Instant basisSealedAt) {
        this.setBasisSealedAt(basisSealedAt);
        return this;
    }

    public void setBasisSealedAt(Instant basisSealedAt) {
        this.basisSealedAt = basisSealedAt;
    }

    public Instant getOpenedAt() {
        return this.openedAt;
    }

    public CommissioningFile openedAt(Instant openedAt) {
        this.setOpenedAt(openedAt);
        return this;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public String getCorrectionReason() {
        return this.correctionReason;
    }

    public CommissioningFile correctionReason(String correctionReason) {
        this.setCorrectionReason(correctionReason);
        return this;
    }

    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }

    public String getManifestDigest() {
        return this.manifestDigest;
    }

    public CommissioningFile manifestDigest(String manifestDigest) {
        this.setManifestDigest(manifestDigest);
        return this;
    }

    public void setManifestDigest(String manifestDigest) {
        this.manifestDigest = manifestDigest;
    }

    public byte[] getTimestampToken() {
        return this.timestampToken;
    }

    public CommissioningFile timestampToken(byte[] timestampToken) {
        this.setTimestampToken(timestampToken);
        return this;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return this.timestampTokenContentType;
    }

    public CommissioningFile timestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
        return this;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public Instant getSealedAt() {
        return this.sealedAt;
    }

    public CommissioningFile sealedAt(Instant sealedAt) {
        this.setSealedAt(sealedAt);
        return this;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Instant getGoLiveAuthorizedAt() {
        return this.goLiveAuthorizedAt;
    }

    public CommissioningFile goLiveAuthorizedAt(Instant goLiveAuthorizedAt) {
        this.setGoLiveAuthorizedAt(goLiveAuthorizedAt);
        return this;
    }

    public void setGoLiveAuthorizedAt(Instant goLiveAuthorizedAt) {
        this.goLiveAuthorizedAt = goLiveAuthorizedAt;
    }

    public ChangeRecord getChangeRecord() {
        return this.changeRecord;
    }

    public void setChangeRecord(ChangeRecord changeRecord) {
        this.changeRecord = changeRecord;
    }

    public CommissioningFile changeRecord(ChangeRecord changeRecord) {
        this.setChangeRecord(changeRecord);
        return this;
    }

    public CertificationAttestation getAttestation() {
        return this.attestation;
    }

    public void setAttestation(CertificationAttestation certificationAttestation) {
        this.attestation = certificationAttestation;
    }

    public CommissioningFile attestation(CertificationAttestation certificationAttestation) {
        this.setAttestation(certificationAttestation);
        return this;
    }

    public EvidenceItem getCommissioningReport() {
        return this.commissioningReport;
    }

    public void setCommissioningReport(EvidenceItem evidenceItem) {
        this.commissioningReport = evidenceItem;
    }

    public CommissioningFile commissioningReport(EvidenceItem evidenceItem) {
        this.setCommissioningReport(evidenceItem);
        return this;
    }

    public Set<Quitus> getQuituses() {
        return this.quituses;
    }

    public void setQuituses(Set<Quitus> quituses) {
        if (this.quituses != null) {
            this.quituses.forEach(i -> i.setFile(null));
        }
        if (quituses != null) {
            quituses.forEach(i -> i.setFile(this));
        }
        this.quituses = quituses;
    }

    public CommissioningFile quituses(Set<Quitus> quituses) {
        this.setQuituses(quituses);
        return this;
    }

    public CommissioningFile addQuitus(Quitus quitus) {
        this.quituses.add(quitus);
        quitus.setFile(this);
        return this;
    }

    public CommissioningFile removeQuitus(Quitus quitus) {
        this.quituses.remove(quitus);
        quitus.setFile(null);
        return this;
    }

    public Set<EvidenceItem> getPieces() {
        return this.pieces;
    }

    public void setPieces(Set<EvidenceItem> evidenceItems) {
        if (this.pieces != null) {
            this.pieces.forEach(i -> i.setFile(null));
        }
        if (evidenceItems != null) {
            evidenceItems.forEach(i -> i.setFile(this));
        }
        this.pieces = evidenceItems;
    }

    public CommissioningFile pieces(Set<EvidenceItem> evidenceItems) {
        this.setPieces(evidenceItems);
        return this;
    }

    public CommissioningFile addPieces(EvidenceItem evidenceItem) {
        this.pieces.add(evidenceItem);
        evidenceItem.setFile(this);
        return this;
    }

    public CommissioningFile removePieces(EvidenceItem evidenceItem) {
        this.pieces.remove(evidenceItem);
        evidenceItem.setFile(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommissioningFile)) {
            return false;
        }
        return getId() != null && getId().equals(((CommissioningFile) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CommissioningFile{" +
            "id=" + getId() +
            ", fileCode='" + getFileCode() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", projectLeadId='" + getProjectLeadId() + "'" +
            ", state='" + getState() + "'" +
            ", basisDigest='" + getBasisDigest() + "'" +
            ", basisTimestampToken='" + getBasisTimestampToken() + "'" +
            ", basisTimestampTokenContentType='" + getBasisTimestampTokenContentType() + "'" +
            ", basisSealedAt='" + getBasisSealedAt() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", correctionReason='" + getCorrectionReason() + "'" +
            ", manifestDigest='" + getManifestDigest() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", timestampTokenContentType='" + getTimestampTokenContentType() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", goLiveAuthorizedAt='" + getGoLiveAuthorizedAt() + "'" +
            "}";
    }
}
