package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Journal des interactions, en ajout seul (PAR-10).
 */
@Entity
@Table(name = "interaction")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Interaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false)
    private InteractionNature nature;

    @NotNull
    @Size(max = 500)
    @Column(name = "summary", length = 500, nullable = false)
    private String summary;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @NotNull
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    /**
     * Référence de l'objet : ticket, liaison, réunion ou pièce, par exemple PIE-2026-118.
     */
    @Size(max = 100)
    @Column(name = "reference", length = 100)
    private String reference;

    /**
     * Identifiant de remise d'un signalement (algorithme 6.29).
     */
    @Column(name = "notification_id")
    private UUID notificationId;

    /**
     * Identifiant CloudEvents, pour une interaction reçue par événement (idempotence).
     */
    @Column(name = "source_event_id", unique = true)
    private UUID sourceEventId;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Interaction id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InteractionNature getNature() {
        return this.nature;
    }

    public Interaction nature(InteractionNature nature) {
        this.setNature(nature);
        return this;
    }

    public void setNature(InteractionNature nature) {
        this.nature = nature;
    }

    public String getSummary() {
        return this.summary;
    }

    public Interaction summary(String summary) {
        this.setSummary(summary);
        return this;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    public Interaction occurredAt(Instant occurredAt) {
        this.setOccurredAt(occurredAt);
        return this;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public Interaction authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getReference() {
        return this.reference;
    }

    public Interaction reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public UUID getNotificationId() {
        return this.notificationId;
    }

    public Interaction notificationId(UUID notificationId) {
        this.setNotificationId(notificationId);
        return this;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getSourceEventId() {
        return this.sourceEventId;
    }

    public Interaction sourceEventId(UUID sourceEventId) {
        this.setSourceEventId(sourceEventId);
        return this;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Interaction institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Interaction)) {
            return false;
        }
        return getId() != null && getId().equals(((Interaction) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Interaction{" +
            "id=" + getId() +
            ", nature='" + getNature() + "'" +
            ", summary='" + getSummary() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", reference='" + getReference() + "'" +
            ", notificationId='" + getNotificationId() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            "}";
    }
}
