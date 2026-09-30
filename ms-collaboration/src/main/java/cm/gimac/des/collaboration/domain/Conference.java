package cm.gimac.des.collaboration.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Conférence audio et vidéo sur le SFU local, SFrame obligatoire (RG-54).
 */
@Entity
@Table(name = "conference")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Conference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Nom de la salle LiveKit, par exemple SFU-C-0142.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "sfu_room", length = 100, nullable = false, unique = true)
    private String sfuRoom;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Sur-chiffrement SFrame exigé (toujours vrai).
     */
    @NotNull
    @Column(name = "sframe_required", nullable = false)
    private Boolean sframeRequired;

    @NotNull
    @Column(name = "screen_share", nullable = false)
    private Boolean screenShare;

    /**
     * Enregistrement actif, seulement avec le consentement de chaque présent (RG-55).
     */
    @NotNull
    @Column(name = "recording", nullable = false)
    private Boolean recording;

    /**
     * Époque courante des clés médias.
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "key_epoch", nullable = false)
    private Integer keyEpoch;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "roomArchive" }, allowSetters = true)
    private Room room;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Conference id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSfuRoom() {
        return this.sfuRoom;
    }

    public Conference sfuRoom(String sfuRoom) {
        this.setSfuRoom(sfuRoom);
        return this;
    }

    public void setSfuRoom(String sfuRoom) {
        this.sfuRoom = sfuRoom;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public Conference startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return this.endedAt;
    }

    public Conference endedAt(Instant endedAt) {
        this.setEndedAt(endedAt);
        return this;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Boolean getSframeRequired() {
        return this.sframeRequired;
    }

    public Conference sframeRequired(Boolean sframeRequired) {
        this.setSframeRequired(sframeRequired);
        return this;
    }

    public void setSframeRequired(Boolean sframeRequired) {
        this.sframeRequired = sframeRequired;
    }

    public Boolean getScreenShare() {
        return this.screenShare;
    }

    public Conference screenShare(Boolean screenShare) {
        this.setScreenShare(screenShare);
        return this;
    }

    public void setScreenShare(Boolean screenShare) {
        this.screenShare = screenShare;
    }

    public Boolean getRecording() {
        return this.recording;
    }

    public Conference recording(Boolean recording) {
        this.setRecording(recording);
        return this;
    }

    public void setRecording(Boolean recording) {
        this.recording = recording;
    }

    public Integer getKeyEpoch() {
        return this.keyEpoch;
    }

    public Conference keyEpoch(Integer keyEpoch) {
        this.setKeyEpoch(keyEpoch);
        return this;
    }

    public void setKeyEpoch(Integer keyEpoch) {
        this.keyEpoch = keyEpoch;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Conference room(Room room) {
        this.setRoom(room);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Conference)) {
            return false;
        }
        return getId() != null && getId().equals(((Conference) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Conference{" +
            "id=" + getId() +
            ", sfuRoom='" + getSfuRoom() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", endedAt='" + getEndedAt() + "'" +
            ", sframeRequired='" + getSframeRequired() + "'" +
            ", screenShare='" + getScreenShare() + "'" +
            ", recording='" + getRecording() + "'" +
            ", keyEpoch=" + getKeyEpoch() +
            "}";
    }
}
