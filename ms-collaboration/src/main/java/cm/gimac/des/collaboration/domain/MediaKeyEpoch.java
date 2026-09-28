package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.MediaKeyRotationReason;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Changement d'époque des clés médias, consigné sans aucune clé (algorithme 6.61).
 */
@Entity
@Table(name = "media_key_epoch")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MediaKeyEpoch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 0)
    @Column(name = "epoch", nullable = false)
    private Integer epoch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private MediaKeyRotationReason reason;

    @NotNull
    @Column(name = "rotated_at", nullable = false)
    private Instant rotatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "room" }, allowSetters = true)
    private Conference conference;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MediaKeyEpoch id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getEpoch() {
        return this.epoch;
    }

    public MediaKeyEpoch epoch(Integer epoch) {
        this.setEpoch(epoch);
        return this;
    }

    public void setEpoch(Integer epoch) {
        this.epoch = epoch;
    }

    public MediaKeyRotationReason getReason() {
        return this.reason;
    }

    public MediaKeyEpoch reason(MediaKeyRotationReason reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(MediaKeyRotationReason reason) {
        this.reason = reason;
    }

    public Instant getRotatedAt() {
        return this.rotatedAt;
    }

    public MediaKeyEpoch rotatedAt(Instant rotatedAt) {
        this.setRotatedAt(rotatedAt);
        return this;
    }

    public void setRotatedAt(Instant rotatedAt) {
        this.rotatedAt = rotatedAt;
    }

    public Conference getConference() {
        return this.conference;
    }

    public void setConference(Conference conference) {
        this.conference = conference;
    }

    public MediaKeyEpoch conference(Conference conference) {
        this.setConference(conference);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaKeyEpoch)) {
            return false;
        }
        return getId() != null && getId().equals(((MediaKeyEpoch) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MediaKeyEpoch{" +
            "id=" + getId() +
            ", epoch=" + getEpoch() +
            ", reason='" + getReason() + "'" +
            ", rotatedAt='" + getRotatedAt() + "'" +
            "}";
    }
}
