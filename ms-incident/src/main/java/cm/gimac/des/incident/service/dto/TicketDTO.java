package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.TicketOrigin;
import cm.gimac.des.incident.domain.enumeration.TicketState;
import cm.gimac.des.incident.domain.enumeration.TicketType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.Ticket} entity.
 */
@Schema(
    description = "Ticket : toute sollicitation adressée au DES (CC §6.1.9.3).\nL'état ne change que par l'automate (algorithme 6.39), sous contrôle de version."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TicketDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^TCK-[0-9]{6,}$")
    @Schema(
        description = "Numéro lisible, par exemple TCK-000318 (number du CC, renommé : mot réservé SQL).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String ticketNumber;

    @NotNull
    private TicketType type;

    @NotNull
    @Size(max = 255)
    private String subject;

    @Lob
    private String description;

    @NotNull
    private TicketOrigin origin;

    @Size(max = 100)
    @Schema(description = "Identifiant de l'objet d'origine (alerte, suspens, notification, divergence).")
    private String originRef;

    @NotNull
    @Schema(description = "Demandeur (collaborateur ou correspondant, MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID requesterId;

    @Schema(description = "Institution concernée (MS-PARTICIPANT) ; filtre RLS du portail (RG-59).")
    private UUID institutionId;

    @Size(max = 50)
    @Schema(description = "Liaison supervisée concernée (MS-MONITORING), par exemple LIA-2026-017.")
    private String linkId;

    @Schema(description = "Alerte d'origine (MS-MONITORING), pour le lien bidirectionnel alerte-ticket.")
    private UUID alertId;

    @Min(value = 1)
    @Max(value = 5)
    @Schema(description = "Priorité 1 à 5, exigée pour qualifier un ticket qui n'est pas un incident.")
    private Integer priority;

    @NotNull
    private TicketState state;

    private UUID assigneeId;

    @NotNull
    private Instant openedAt;

    private Instant resolvedAt;

    @Schema(description = "Auteur de la résolution : le valideur de clôture doit en être distinct (6.43).")
    private UUID resolverId;

    private Instant closedAt;

    private UUID closedBy;

    @NotNull
    @Min(value = 0L)
    @Schema(description = "Version pour le verrouillage optimiste de l'automate.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long version;

    @Schema(description = "Ticket lié : anomalie d'escalade épuisée, réouverture au-delà de trente jours.")
    private TicketDTO relatedTicket;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(TicketOrigin origin) {
        this.origin = origin;
    }

    public String getOriginRef() {
        return originRef;
    }

    public void setOriginRef(String originRef) {
        this.originRef = originRef;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public TicketState getState() {
        return state;
    }

    public void setState(TicketState state) {
        this.state = state;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getResolverId() {
        return resolverId;
    }

    public void setResolverId(UUID resolverId) {
        this.resolverId = resolverId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public TicketDTO getRelatedTicket() {
        return relatedTicket;
    }

    public void setRelatedTicket(TicketDTO relatedTicket) {
        this.relatedTicket = relatedTicket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketDTO)) {
            return false;
        }

        TicketDTO ticketDTO = (TicketDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, ticketDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TicketDTO{" +
            "id='" + getId() + "'" +
            ", ticketNumber='" + getTicketNumber() + "'" +
            ", type='" + getType() + "'" +
            ", subject='" + getSubject() + "'" +
            ", description='" + getDescription() + "'" +
            ", origin='" + getOrigin() + "'" +
            ", originRef='" + getOriginRef() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", linkId='" + getLinkId() + "'" +
            ", alertId='" + getAlertId() + "'" +
            ", priority=" + getPriority() +
            ", state='" + getState() + "'" +
            ", assigneeId='" + getAssigneeId() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", resolverId='" + getResolverId() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", closedBy='" + getClosedBy() + "'" +
            ", version=" + getVersion() +
            ", relatedTicket=" + getRelatedTicket() +
            "}";
    }
}
