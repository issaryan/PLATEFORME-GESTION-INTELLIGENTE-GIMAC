package cm.gimac.des.collaboration.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Consentement explicite d'un participant à l'enregistrement (RG-55).
 */
@Entity
@Table(name = "recording_consent")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingConsent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Participant (MS-IAM).
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "granted", nullable = false)
    private Boolean granted;

    @NotNull
    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "room" }, allowSetters = true)
    private Conference conference;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RecordingConsent id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public RecordingConsent userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Boolean getGranted() {
        return this.granted;
    }

    public RecordingConsent granted(Boolean granted) {
        this.setGranted(granted);
        return this;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public Instant getDecidedAt() {
        return this.decidedAt;
    }

    public RecordingConsent decidedAt(Instant decidedAt) {
        this.setDecidedAt(decidedAt);
        return this;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Conference getConference() {
        return this.conference;
    }

    public void setConference(Conference conference) {
        this.conference = conference;
    }

    public RecordingConsent conference(Conference conference) {
        this.setConference(conference);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordingConsent)) {
            return false;
        }
        return getId() != null && getId().equals(((RecordingConsent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingConsent{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", granted='" + getGranted() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            "}";
    }
}
