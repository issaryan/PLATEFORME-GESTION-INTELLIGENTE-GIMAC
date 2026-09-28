package cm.gimac.des.monitoring.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.MeasureWindow} entity.
 */
@Schema(
    description = "Fenêtre de mesures agrégées reçue des sources (aucune donnée de transaction).\nDébut et fin renommés : start et end sont réservés en SQL."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeasureWindowDTO implements Serializable {

    private UUID id;

    @NotNull
    private Instant windowStart;

    @NotNull
    private Instant windowEnd;

    @Schema(description = "Valeurs agrégées, objet JSON indicateur → valeur.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String measureValues;

    @NotNull
    private Instant receivedAt;

    @NotNull
    private SupervisedLinkDTO link;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getMeasureValues() {
        return measureValues;
    }

    public void setMeasureValues(String measureValues) {
        this.measureValues = measureValues;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public SupervisedLinkDTO getLink() {
        return link;
    }

    public void setLink(SupervisedLinkDTO link) {
        this.link = link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeasureWindowDTO)) {
            return false;
        }

        MeasureWindowDTO measureWindowDTO = (MeasureWindowDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, measureWindowDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeasureWindowDTO{" +
            "id='" + getId() + "'" +
            ", windowStart='" + getWindowStart() + "'" +
            ", windowEnd='" + getWindowEnd() + "'" +
            ", measureValues='" + getMeasureValues() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            ", link=" + getLink() +
            "}";
    }
}
