package cm.gimac.des.monitoring.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Fenêtre de mesures agrégées reçue des sources (aucune donnée de transaction).
 * Début et fin renommés : start et end sont réservés en SQL.
 */
@Entity
@Table(name = "measure_window")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeasureWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @NotNull
    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    /**
     * Valeurs agrégées, objet JSON indicateur → valeur.
     */
    @Lob
    @Column(name = "measure_values", nullable = false)
    private String measureValues;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "parentPlatform" }, allowSetters = true)
    private SupervisedLink link;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MeasureWindow id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getWindowStart() {
        return this.windowStart;
    }

    public MeasureWindow windowStart(Instant windowStart) {
        this.setWindowStart(windowStart);
        return this;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return this.windowEnd;
    }

    public MeasureWindow windowEnd(Instant windowEnd) {
        this.setWindowEnd(windowEnd);
        return this;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getMeasureValues() {
        return this.measureValues;
    }

    public MeasureWindow measureValues(String measureValues) {
        this.setMeasureValues(measureValues);
        return this;
    }

    public void setMeasureValues(String measureValues) {
        this.measureValues = measureValues;
    }

    public Instant getReceivedAt() {
        return this.receivedAt;
    }

    public MeasureWindow receivedAt(Instant receivedAt) {
        this.setReceivedAt(receivedAt);
        return this;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public SupervisedLink getLink() {
        return this.link;
    }

    public void setLink(SupervisedLink supervisedLink) {
        this.link = supervisedLink;
    }

    public MeasureWindow link(SupervisedLink supervisedLink) {
        this.setLink(supervisedLink);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeasureWindow)) {
            return false;
        }
        return getId() != null && getId().equals(((MeasureWindow) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeasureWindow{" +
            "id=" + getId() +
            ", windowStart='" + getWindowStart() + "'" +
            ", windowEnd='" + getWindowEnd() + "'" +
            ", measureValues='" + getMeasureValues() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            "}";
    }
}
