package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.LegalDocumentState;
import cm.gimac.des.participant.domain.enumeration.LegalDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.LegalDocument} entity.
 */
@Schema(
    description = "Pièce légale (agrément, arrêté, attestation) déposée dans MS-DOCUMENT ;\nson dépôt arme quatre échéances (algorithme 6.26)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LegalDocumentDTO implements Serializable {

    private UUID id;

    @NotNull
    private LegalDocumentType type;

    @NotNull
    @Size(max = 100)
    private String reference;

    @Size(max = 100)
    @Schema(description = "Autorité émettrice, par exemple COBAC ou MinFi.")
    private String issuingAuthority;

    @NotNull
    private LocalDate issuedOn;

    @NotNull
    private LocalDate expiresOn;

    @NotNull
    @Schema(description = "Référence du document dans MS-DOCUMENT.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID documentId;

    @Min(value = 1)
    private Integer documentVersion;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Empreinte SHA-256 confirmée par document.document-sealed.v1.")
    private String digest;

    @NotNull
    private Boolean sealed;

    @NotNull
    private LegalDocumentState state;

    @Schema(description = "Pièce remplacée lors d'un renouvellement (algorithme 6.26, étape 5).")
    private LegalDocumentDTO previousDocument;

    @NotNull
    private InstitutionDTO institution;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LegalDocumentType getType() {
        return type;
    }

    public void setType(LegalDocumentType type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDate issuedOn) {
        this.issuedOn = issuedOn;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public Integer getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(Integer documentVersion) {
        this.documentVersion = documentVersion;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Boolean getSealed() {
        return sealed;
    }

    public void setSealed(Boolean sealed) {
        this.sealed = sealed;
    }

    public LegalDocumentState getState() {
        return state;
    }

    public void setState(LegalDocumentState state) {
        this.state = state;
    }

    public LegalDocumentDTO getPreviousDocument() {
        return previousDocument;
    }

    public void setPreviousDocument(LegalDocumentDTO previousDocument) {
        this.previousDocument = previousDocument;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LegalDocumentDTO)) {
            return false;
        }

        LegalDocumentDTO legalDocumentDTO = (LegalDocumentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, legalDocumentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LegalDocumentDTO{" +
            "id='" + getId() + "'" +
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
            ", previousDocument=" + getPreviousDocument() +
            ", institution=" + getInstitution() +
            "}";
    }
}
