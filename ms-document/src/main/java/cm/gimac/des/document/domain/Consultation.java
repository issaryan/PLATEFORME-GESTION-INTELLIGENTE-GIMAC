package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.ConsultationAction;
import cm.gimac.des.document.domain.enumeration.ConsultationChannel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Journal de consultation en insertion seule (RG-06, algorithme 6.19) :
 * inscrit avant toute restitution, y compris les refus.
 */
@Entity
@Table(name = "consultation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Consultation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Institution du sujet, pour un représentant d'un participant.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ConsultationAction action;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private ConsultationChannel channel;

    @NotNull
    @Column(name = "consulted_at", nullable = false)
    private Instant consultedAt;

    @NotNull
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    /**
     * Faux pour une consultation refusée, journalisée et publiée elle aussi.
     */
    @NotNull
    @Column(name = "granted", nullable = false)
    private Boolean granted;

    @Size(max = 255)
    @Column(name = "refusal_reason", length = 255)
    private String refusalReason;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "lineage", "policy", "documentLineage" }, allowSetters = true)
    private Document document;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Consultation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public Consultation userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Consultation institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public ConsultationAction getAction() {
        return this.action;
    }

    public Consultation action(ConsultationAction action) {
        this.setAction(action);
        return this;
    }

    public void setAction(ConsultationAction action) {
        this.action = action;
    }

    public ConsultationChannel getChannel() {
        return this.channel;
    }

    public Consultation channel(ConsultationChannel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(ConsultationChannel channel) {
        this.channel = channel;
    }

    public Instant getConsultedAt() {
        return this.consultedAt;
    }

    public Consultation consultedAt(Instant consultedAt) {
        this.setConsultedAt(consultedAt);
        return this;
    }

    public void setConsultedAt(Instant consultedAt) {
        this.consultedAt = consultedAt;
    }

    public UUID getCorrelationId() {
        return this.correlationId;
    }

    public Consultation correlationId(UUID correlationId) {
        this.setCorrelationId(correlationId);
        return this;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public Boolean getGranted() {
        return this.granted;
    }

    public Consultation granted(Boolean granted) {
        this.setGranted(granted);
        return this;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public String getRefusalReason() {
        return this.refusalReason;
    }

    public Consultation refusalReason(String refusalReason) {
        this.setRefusalReason(refusalReason);
        return this;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public Document getDocument() {
        return this.document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Consultation document(Document document) {
        this.setDocument(document);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Consultation)) {
            return false;
        }
        return getId() != null && getId().equals(((Consultation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Consultation{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", action='" + getAction() + "'" +
            ", channel='" + getChannel() + "'" +
            ", consultedAt='" + getConsultedAt() + "'" +
            ", correlationId='" + getCorrelationId() + "'" +
            ", granted='" + getGranted() + "'" +
            ", refusalReason='" + getRefusalReason() + "'" +
            "}";
    }
}
