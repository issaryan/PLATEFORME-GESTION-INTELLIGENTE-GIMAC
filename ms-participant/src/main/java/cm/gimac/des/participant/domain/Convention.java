package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.ConventionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Convention (adhésion, service, délégation, parrainage) ; la pièce est
 * déposée dans MS-DOCUMENT, seules sa référence et son empreinte sont conservées.
 */
@Entity
@Table(name = "convention")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Convention implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConventionType type;

    @NotNull
    @Size(max = 50)
    @Column(name = "reference", length = 50, nullable = false, unique = true)
    private String reference;

    @NotNull
    @Column(name = "signed_on", nullable = false)
    private LocalDate signedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    /**
     * Référence du document dans MS-DOCUMENT (documentRef du CC).
     */
    @NotNull
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "document_sha_256", length = 64)
    private String documentSha256;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Convention id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConventionType getType() {
        return this.type;
    }

    public Convention type(ConventionType type) {
        this.setType(type);
        return this;
    }

    public void setType(ConventionType type) {
        this.type = type;
    }

    public String getReference() {
        return this.reference;
    }

    public Convention reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getSignedOn() {
        return this.signedOn;
    }

    public Convention signedOn(LocalDate signedOn) {
        this.setSignedOn(signedOn);
        return this;
    }

    public void setSignedOn(LocalDate signedOn) {
        this.signedOn = signedOn;
    }

    public LocalDate getExpiresOn() {
        return this.expiresOn;
    }

    public Convention expiresOn(LocalDate expiresOn) {
        this.setExpiresOn(expiresOn);
        return this;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public UUID getDocumentId() {
        return this.documentId;
    }

    public Convention documentId(UUID documentId) {
        this.setDocumentId(documentId);
        return this;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentSha256() {
        return this.documentSha256;
    }

    public Convention documentSha256(String documentSha256) {
        this.setDocumentSha256(documentSha256);
        return this;
    }

    public void setDocumentSha256(String documentSha256) {
        this.documentSha256 = documentSha256;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Convention institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Convention)) {
            return false;
        }
        return getId() != null && getId().equals(((Convention) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Convention{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", reference='" + getReference() + "'" +
            ", signedOn='" + getSignedOn() + "'" +
            ", expiresOn='" + getExpiresOn() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", documentSha256='" + getDocumentSha256() + "'" +
            "}";
    }
}
