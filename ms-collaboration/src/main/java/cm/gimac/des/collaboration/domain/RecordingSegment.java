package cm.gimac.des.collaboration.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Segment enregistré, avec la liste des consentements qui l'autorisent.
 * Écart CC corrigé : le segment est cité (§6.1.14.1, algorithme 6.61) sans
 * être modélisé.
 */
@Entity
@Table(name = "recording_segment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Identifiant de l'enregistrement (egress LiveKit).
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "egress_reference", length = 255, nullable = false)
    private String egressReference;

    /**
     * Participants consentants : identifiants (MS-IAM), séparés par des virgules.
     */
    @NotNull
    @Size(max = 4000)
    @Column(name = "consenting_user_ids", length = 4000, nullable = false)
    private String consentingUserIds;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "room" }, allowSetters = true)
    private Conference conference;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RecordingSegment id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public RecordingSegment startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return this.endedAt;
    }

    public RecordingSegment endedAt(Instant endedAt) {
        this.setEndedAt(endedAt);
        return this;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getEgressReference() {
        return this.egressReference;
    }

    public RecordingSegment egressReference(String egressReference) {
        this.setEgressReference(egressReference);
        return this;
    }

    public void setEgressReference(String egressReference) {
        this.egressReference = egressReference;
    }

    public String getConsentingUserIds() {
        return this.consentingUserIds;
    }

    public RecordingSegment consentingUserIds(String consentingUserIds) {
        this.setConsentingUserIds(consentingUserIds);
        return this;
    }

    public void setConsentingUserIds(String consentingUserIds) {
        this.consentingUserIds = consentingUserIds;
    }

    public Conference getConference() {
        return this.conference;
    }

    public void setConference(Conference conference) {
        this.conference = conference;
    }

    public RecordingSegment conference(Conference conference) {
        this.setConference(conference);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordingSegment)) {
            return false;
        }
        return getId() != null && getId().equals(((RecordingSegment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingSegment{" +
            "id=" + getId() +
            ", startedAt='" + getStartedAt() + "'" +
            ", endedAt='" + getEndedAt() + "'" +
            ", egressReference='" + getEgressReference() + "'" +
            ", consentingUserIds='" + getConsentingUserIds() + "'" +
            "}";
    }
}
