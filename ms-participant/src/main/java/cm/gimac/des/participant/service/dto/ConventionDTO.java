package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.ConventionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.Convention} entity.
 */
@Schema(
    description = "Convention (adhésion, service, délégation, parrainage) ; la pièce est\ndéposée dans MS-DOCUMENT, seules sa référence et son empreinte sont conservées."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConventionDTO implements Serializable {

    private UUID id;

    @NotNull
    private ConventionType type;

    @NotNull
    @Size(max = 50)
    private String reference;

    @NotNull
    private LocalDate signedOn;

    private LocalDate expiresOn;

    @NotNull
    @Schema(description = "Référence du document dans MS-DOCUMENT (documentRef du CC).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID documentId;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String documentSha256;

    @NotNull
    private InstitutionDTO institution;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConventionType getType() {
        return type;
    }

    public void setType(ConventionType type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getSignedOn() {
        return signedOn;
    }

    public void setSignedOn(LocalDate signedOn) {
        this.signedOn = signedOn;
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

    public String getDocumentSha256() {
        return documentSha256;
    }

    public void setDocumentSha256(String documentSha256) {
        this.documentSha256 = documentSha256;
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
        if (!(o instanceof ConventionDTO)) {
            return false;
        }

        ConventionDTO conventionDTO = (ConventionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, conventionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConventionDTO{" +
            "id='" + getId() + "'" +
            ", type='" + getType() + "'" +
            ", reference='" + getReference() + "'" +
            ", signedOn='" + getSignedOn() + "'" +
            ", expiresOn='" + getExpiresOn() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", documentSha256='" + getDocumentSha256() + "'" +
            ", institution=" + getInstitution() +
            "}";
    }
}
