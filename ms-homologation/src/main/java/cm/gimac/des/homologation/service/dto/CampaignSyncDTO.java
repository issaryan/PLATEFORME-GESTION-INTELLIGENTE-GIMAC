package cm.gimac.des.homologation.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.CampaignSync} entity.
 */
@Schema(
    description = "Curseur de reprise d'une campagne (tlast de l'algorithme 6.32) et état de la\nlecture planifiée du simulateur (D-16). Écart CC corrigé : le CC ne modélise\npas ce curseur, pourtant persisté dans la transaction du dernier scénario."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CampaignSyncDTO implements Serializable {

    private UUID id;

    @Schema(description = "Plus grand horodatage d'exécution déjà repris.")
    private Instant cursor;

    private Instant lastPolledAt;

    private Instant lastSuccessAt;

    @NotNull
    @Min(value = 0)
    private Integer consecutiveFailures;

    @Schema(description = "Fin d'ouverture du disjoncteur en cas d'indisponibilité.")
    private Instant circuitOpenUntil;

    @NotNull
    @Schema(description = "Curseur de reprise propre à chaque liaison.")
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCursor() {
        return cursor;
    }

    public void setCursor(Instant cursor) {
        this.cursor = cursor;
    }

    public Instant getLastPolledAt() {
        return lastPolledAt;
    }

    public void setLastPolledAt(Instant lastPolledAt) {
        this.lastPolledAt = lastPolledAt;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(Instant lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public Instant getCircuitOpenUntil() {
        return circuitOpenUntil;
    }

    public void setCircuitOpenUntil(Instant circuitOpenUntil) {
        this.circuitOpenUntil = circuitOpenUntil;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampaignSyncDTO)) {
            return false;
        }

        CampaignSyncDTO campaignSyncDTO = (CampaignSyncDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, campaignSyncDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CampaignSyncDTO{" +
            "id='" + getId() + "'" +
            ", cursor='" + getCursor() + "'" +
            ", lastPolledAt='" + getLastPolledAt() + "'" +
            ", lastSuccessAt='" + getLastSuccessAt() + "'" +
            ", consecutiveFailures=" + getConsecutiveFailures() +
            ", circuitOpenUntil='" + getCircuitOpenUntil() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
