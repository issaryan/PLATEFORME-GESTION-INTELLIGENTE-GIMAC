package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.TicketOrigin;
import cm.gimac.des.incident.domain.enumeration.TicketState;
import cm.gimac.des.incident.domain.enumeration.TicketType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Ticket : toute sollicitation adressée au DES (CC §6.1.9.3).
 * L'état ne change que par l'automate (algorithme 6.39), sous contrôle de version.
 */
@Entity
@Table(name = "ticket")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Numéro lisible, par exemple TCK-000318 (number du CC, renommé : mot réservé SQL).
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^TCK-[0-9]{6,}$")
    @Column(name = "ticket_number", length = 20, nullable = false, unique = true)
    private String ticketNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TicketType type;

    @NotNull
    @Size(max = 255)
    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Lob
    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private TicketOrigin origin;

    /**
     * Identifiant de l'objet d'origine (alerte, suspens, notification, divergence).
     */
    @Size(max = 100)
    @Column(name = "origin_ref", length = 100)
    private String originRef;

    /**
     * Demandeur (collaborateur ou correspondant, MS-IAM).
     */
    @NotNull
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    /**
     * Institution concernée (MS-PARTICIPANT) ; filtre RLS du portail (RG-59).
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    /**
     * Liaison supervisée concernée (MS-MONITORING), par exemple LIA-2026-017.
     */
    @Size(max = 50)
    @Column(name = "link_id", length = 50)
    private String linkId;

    /**
     * Alerte d'origine (MS-MONITORING), pour le lien bidirectionnel alerte-ticket.
     */
    @Column(name = "alert_id")
    private UUID alertId;

    /**
     * Priorité 1 à 5, exigée pour qualifier un ticket qui n'est pas un incident.
     */
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "priority")
    private Integer priority;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TicketState state;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @NotNull
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Auteur de la résolution : le valideur de clôture doit en être distinct (6.43).
     */
    @Column(name = "resolver_id")
    private UUID resolverId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    /**
     * Version pour le verrouillage optimiste de l'automate.
     */
    @NotNull
    @Min(value = 0L)
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Ticket lié : anomalie d'escalade épuisée, réouverture au-delà de trente jours.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "relatedTicket", "incident" }, allowSetters = true)
    private Ticket relatedTicket;

    @JsonIgnoreProperties(value = { "ticket", "matrix" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "ticket")
    private Incident incident;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Ticket id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return this.ticketNumber;
    }

    public Ticket ticketNumber(String ticketNumber) {
        this.setTicketNumber(ticketNumber);
        return this;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public TicketType getType() {
        return this.type;
    }

    public Ticket type(TicketType type) {
        this.setType(type);
        return this;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getSubject() {
        return this.subject;
    }

    public Ticket subject(String subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return this.description;
    }

    public Ticket description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketOrigin getOrigin() {
        return this.origin;
    }

    public Ticket origin(TicketOrigin origin) {
        this.setOrigin(origin);
        return this;
    }

    public void setOrigin(TicketOrigin origin) {
        this.origin = origin;
    }

    public String getOriginRef() {
        return this.originRef;
    }

    public Ticket originRef(String originRef) {
        this.setOriginRef(originRef);
        return this;
    }

    public void setOriginRef(String originRef) {
        this.originRef = originRef;
    }

    public UUID getRequesterId() {
        return this.requesterId;
    }

    public Ticket requesterId(UUID requesterId) {
        this.setRequesterId(requesterId);
        return this;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Ticket institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getLinkId() {
        return this.linkId;
    }

    public Ticket linkId(String linkId) {
        this.setLinkId(linkId);
        return this;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public UUID getAlertId() {
        return this.alertId;
    }

    public Ticket alertId(UUID alertId) {
        this.setAlertId(alertId);
        return this;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public Ticket priority(Integer priority) {
        this.setPriority(priority);
        return this;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public TicketState getState() {
        return this.state;
    }

    public Ticket state(TicketState state) {
        this.setState(state);
        return this;
    }

    public void setState(TicketState state) {
        this.state = state;
    }

    public UUID getAssigneeId() {
        return this.assigneeId;
    }

    public Ticket assigneeId(UUID assigneeId) {
        this.setAssigneeId(assigneeId);
        return this;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Instant getOpenedAt() {
        return this.openedAt;
    }

    public Ticket openedAt(Instant openedAt) {
        this.setOpenedAt(openedAt);
        return this;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getResolvedAt() {
        return this.resolvedAt;
    }

    public Ticket resolvedAt(Instant resolvedAt) {
        this.setResolvedAt(resolvedAt);
        return this;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getResolverId() {
        return this.resolverId;
    }

    public Ticket resolverId(UUID resolverId) {
        this.setResolverId(resolverId);
        return this;
    }

    public void setResolverId(UUID resolverId) {
        this.resolverId = resolverId;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public Ticket closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public UUID getClosedBy() {
        return this.closedBy;
    }

    public Ticket closedBy(UUID closedBy) {
        this.setClosedBy(closedBy);
        return this;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Long getVersion() {
        return this.version;
    }

    public Ticket version(Long version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Ticket getRelatedTicket() {
        return this.relatedTicket;
    }

    public void setRelatedTicket(Ticket ticket) {
        this.relatedTicket = ticket;
    }

    public Ticket relatedTicket(Ticket ticket) {
        this.setRelatedTicket(ticket);
        return this;
    }

    public Incident getIncident() {
        return this.incident;
    }

    public void setIncident(Incident incident) {
        if (this.incident != null) {
            this.incident.setTicket(null);
        }
        if (incident != null) {
            incident.setTicket(this);
        }
        this.incident = incident;
    }

    public Ticket incident(Incident incident) {
        this.setIncident(incident);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticket)) {
            return false;
        }
        return getId() != null && getId().equals(((Ticket) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Ticket{" +
            "id=" + getId() +
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
            "}";
    }
}
