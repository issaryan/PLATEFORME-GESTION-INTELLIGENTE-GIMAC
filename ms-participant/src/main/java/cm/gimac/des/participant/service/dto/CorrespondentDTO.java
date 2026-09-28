package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.CorrespondentFunction;
import cm.gimac.des.participant.domain.enumeration.Reachability;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.Correspondent} entity.
 */
@Schema(
    description = "Correspondant d'une institution, par fonction (UC-REF-04). Les mises à jour\nproposées depuis le portail restent en attente jusqu'à validation par le DES (PAR-04, PAR-11)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CorrespondentDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String fullName;

    @NotNull
    @Schema(description = "Fonction (function du CC, mot réservé).", requiredMode = Schema.RequiredMode.REQUIRED)
    private CorrespondentFunction duty;

    @NotNull
    @Size(max = 255)
    @Schema(
        description = "Canaux déclarés, séparés par des virgules, par exemple messagerie,téléphone.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String channels;

    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    @Schema(description = "Référence de la clé publique déposée, pour vérifier les pièces signées.")
    private String publicKeyRef;

    @NotNull
    private Reachability reachability;

    private Instant lastReachabilityCheckAt;

    @NotNull
    @Schema(description = "Contact d'astreinte 24/7 (RG-70 : au moins deux par participant).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean onCall247;

    @Schema(description = "Compte du portail (identifiant dans MS-IAM), s'il existe.")
    private UUID userId;

    @NotNull
    private Boolean active;

    private UUID proposedBy;

    private UUID validatedBy;

    private Instant validatedAt;

    @NotNull
    private InstitutionDTO institution;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public CorrespondentFunction getDuty() {
        return duty;
    }

    public void setDuty(CorrespondentFunction duty) {
        this.duty = duty;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPublicKeyRef() {
        return publicKeyRef;
    }

    public void setPublicKeyRef(String publicKeyRef) {
        this.publicKeyRef = publicKeyRef;
    }

    public Reachability getReachability() {
        return reachability;
    }

    public void setReachability(Reachability reachability) {
        this.reachability = reachability;
    }

    public Instant getLastReachabilityCheckAt() {
        return lastReachabilityCheckAt;
    }

    public void setLastReachabilityCheckAt(Instant lastReachabilityCheckAt) {
        this.lastReachabilityCheckAt = lastReachabilityCheckAt;
    }

    public Boolean getOnCall247() {
        return onCall247;
    }

    public void setOnCall247(Boolean onCall247) {
        this.onCall247 = onCall247;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UUID getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(UUID proposedBy) {
        this.proposedBy = proposedBy;
    }

    public UUID getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(UUID validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
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
        if (!(o instanceof CorrespondentDTO)) {
            return false;
        }

        CorrespondentDTO correspondentDTO = (CorrespondentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, correspondentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CorrespondentDTO{" +
            "id='" + getId() + "'" +
            ", fullName='" + getFullName() + "'" +
            ", duty='" + getDuty() + "'" +
            ", channels='" + getChannels() + "'" +
            ", email='" + getEmail() + "'" +
            ", phone='" + getPhone() + "'" +
            ", publicKeyRef='" + getPublicKeyRef() + "'" +
            ", reachability='" + getReachability() + "'" +
            ", lastReachabilityCheckAt='" + getLastReachabilityCheckAt() + "'" +
            ", onCall247='" + getOnCall247() + "'" +
            ", userId='" + getUserId() + "'" +
            ", active='" + getActive() + "'" +
            ", proposedBy='" + getProposedBy() + "'" +
            ", validatedBy='" + getValidatedBy() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", institution=" + getInstitution() +
            "}";
    }
}
