package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.RetentionAnchor;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Politique de rétention paramétrée par type de salon (D-11). Valeurs par
 * défaut du CC (tableau 6.66) : QUART 180 jours ; THEME 1 an ; INCIDENT 1 an
 * après clôture du ticket ; CRISE 90 jours après scellement, archive 10 ans.
 */
@Entity
@Table(name = "room_retention_policy")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomRetentionPolicy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, unique = true)
    private RoomType roomType;

    /**
     * Durée de conservation des messages chiffrés sur le serveur de messagerie.
     */
    @NotNull
    @Column(name = "message_retention", nullable = false)
    private Duration messageRetention;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "anchor", nullable = false)
    private RetentionAnchor anchor;

    /**
     * Archivage par le terminal d'archivage (D-28 : salons de crise et salons liés à un ticket).
     */
    @NotNull
    @Column(name = "archived", nullable = false)
    private Boolean archived;

    /**
     * Rétention de l'archive en années ; 10 ans au moins pour une archive de crise (D-11, RG-53).
     */
    @Min(value = 1)
    @Max(value = 99)
    @Column(name = "archive_retention_years")
    private Integer archiveRetentionYears;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RoomRetentionPolicy id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoomType getRoomType() {
        return this.roomType;
    }

    public RoomRetentionPolicy roomType(RoomType roomType) {
        this.setRoomType(roomType);
        return this;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Duration getMessageRetention() {
        return this.messageRetention;
    }

    public RoomRetentionPolicy messageRetention(Duration messageRetention) {
        this.setMessageRetention(messageRetention);
        return this;
    }

    public void setMessageRetention(Duration messageRetention) {
        this.messageRetention = messageRetention;
    }

    public RetentionAnchor getAnchor() {
        return this.anchor;
    }

    public RoomRetentionPolicy anchor(RetentionAnchor anchor) {
        this.setAnchor(anchor);
        return this;
    }

    public void setAnchor(RetentionAnchor anchor) {
        this.anchor = anchor;
    }

    public Boolean getArchived() {
        return this.archived;
    }

    public RoomRetentionPolicy archived(Boolean archived) {
        this.setArchived(archived);
        return this;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Integer getArchiveRetentionYears() {
        return this.archiveRetentionYears;
    }

    public RoomRetentionPolicy archiveRetentionYears(Integer archiveRetentionYears) {
        this.setArchiveRetentionYears(archiveRetentionYears);
        return this;
    }

    public void setArchiveRetentionYears(Integer archiveRetentionYears) {
        this.archiveRetentionYears = archiveRetentionYears;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public RoomRetentionPolicy updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomRetentionPolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((RoomRetentionPolicy) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomRetentionPolicy{" +
            "id=" + getId() +
            ", roomType='" + getRoomType() + "'" +
            ", messageRetention='" + getMessageRetention() + "'" +
            ", anchor='" + getAnchor() + "'" +
            ", archived='" + getArchived() + "'" +
            ", archiveRetentionYears=" + getArchiveRetentionYears() +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
