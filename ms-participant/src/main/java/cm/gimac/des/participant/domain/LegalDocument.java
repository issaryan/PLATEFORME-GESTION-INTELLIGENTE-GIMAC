package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.LegalDocumentState;
import cm.gimac.des.participant.domain.enumeration.LegalDocumentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pièce légale (agrément, arrêté, attestation) déposée dans MS-DOCUMENT ;
 * son dépôt arme quatre échéances (algorithme 6.26).
 */
@Entity
@Table(name = "legal_document")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LegalDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LegalDocumentType type;

    @NotNull
    @Size(max = 100)
    @Column(name = "reference", length = 100, nullable = false)
    private String reference;

    /**
     * Autorité émettrice, par exemple COBAC ou MinFi.
     */
    @Size(max = 100)
    @Column(name = "issuing_authority", length = 100)
    private String issuingAuthority;

    @NotNull
    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @NotNull
    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    /**
     * Référence du document dans MS-DOCUMENT.
     */
    @NotNull
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Min(value = 1)
    @Column(name = "document_version")
    private Integer documentVersion;

    /**
     * Empreinte SHA-256 confirmée par document.document-sealed.v1.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "digest", length = 64)
    private String digest;

    @NotNull
    @Column(name = "sealed", nullable = false)
    private Boolean sealed;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private LegalDocumentState state;

    /**
     * Pièce remplacée lors d'un renouvellement (algorithme 6.26, étape 5).
     */
    @JsonIgnoreProperties(value = { "previousDocument", "institution", "legalDocument" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private LegalDocument previousDocument;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    @JsonIgnoreProperties(value = { "previousDocument", "institution", "legalDocument" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "previousDocument")
    private LegalDocument legalDocument;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public LegalDocument id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LegalDocumentType getType() {
        return this.type;
    }

    public LegalDocument type(LegalDocumentType type) {
        this.setType(type);
        return this;
    }

    public void setType(LegalDocumentType type) {
        this.type = type;
    }

    public String getReference() {
        return this.reference;
    }

    public LegalDocument reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIssuingAuthority() {
        return this.issuingAuthority;
    }

    public LegalDocument issuingAuthority(String issuingAuthority) {
        this.setIssuingAuthority(issuingAuthority);
        return this;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDate getIssuedOn() {
        return this.issuedOn;
    }

    public LegalDocument issuedOn(LocalDate issuedOn) {
        this.setIssuedOn(issuedOn);
        return this;
    }

    public void setIssuedOn(LocalDate issuedOn) {
        this.issuedOn = issuedOn;
    }

    public LocalDate getExpiresOn() {
        return this.expiresOn;
    }

    public LegalDocument expiresOn(LocalDate expiresOn) {
        this.setExpiresOn(expiresOn);
        return this;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public UUID getDocumentId() {
        return this.documentId;
    }

    public LegalDocument documentId(UUID documentId) {
        this.setDocumentId(documentId);
        return this;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public Integer getDocumentVersion() {
        return this.documentVersion;
    }

    public LegalDocument documentVersion(Integer documentVersion) {
        this.setDocumentVersion(documentVersion);
        return this;
    }

    public void setDocumentVersion(Integer documentVersion) {
        this.documentVersion = documentVersion;
    }

    public String getDigest() {
        return this.digest;
    }

    public LegalDocument digest(String digest) {
        this.setDigest(digest);
        return this;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Boolean getSealed() {
        return this.sealed;
    }

    public LegalDocument sealed(Boolean sealed) {
        this.setSealed(sealed);
        return this;
    }

    public void setSealed(Boolean sealed) {
        this.sealed = sealed;
    }

    public LegalDocumentState getState() {
        return this.state;
    }

    public LegalDocument state(LegalDocumentState state) {
        this.setState(state);
        return this;
    }

    public void setState(LegalDocumentState state) {
        this.state = state;
    }

    public LegalDocument getPreviousDocument() {
        return this.previousDocument;
    }

    public void setPreviousDocument(LegalDocument legalDocument) {
        this.previousDocument = legalDocument;
    }

    public LegalDocument previousDocument(LegalDocument legalDocument) {
        this.setPreviousDocument(legalDocument);
        return this;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public LegalDocument institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    public LegalDocument getLegalDocument() {
        return this.legalDocument;
    }

    public void setLegalDocument(LegalDocument legalDocument) {
        if (this.legalDocument != null) {
            this.legalDocument.setPreviousDocument(null);
        }
        if (legalDocument != null) {
            legalDocument.setPreviousDocument(this);
        }
        this.legalDocument = legalDocument;
    }

    public LegalDocument legalDocument(LegalDocument legalDocument) {
        this.setLegalDocument(legalDocument);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LegalDocument)) {
            return false;
        }
        return getId() != null && getId().equals(((LegalDocument) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LegalDocument{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", reference='" + getReference() + "'" +
            ", issuingAuthority='" + getIssuingAuthority() + "'" +
            ", issuedOn='" + getIssuedOn() + "'" +
            ", expiresOn='" + getExpiresOn() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", documentVersion=" + getDocumentVersion() +
            ", digest='" + getDigest() + "'" +
            ", sealed='" + getSealed() + "'" +
            ", state='" + getState() + "'" +
            "}";
    }
}
