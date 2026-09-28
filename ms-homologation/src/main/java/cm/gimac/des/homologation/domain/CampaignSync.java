package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Curseur de reprise d'une campagne (tlast de l'algorithme 6.32) et état de la
 * lecture planifiée du simulateur (D-16). Écart CC corrigé : le CC ne modélise
 * pas ce curseur, pourtant persisté dans la transaction du dernier scénario.
 */
@Entity
@Table(name = "campaign_sync")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CampaignSync implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Plus grand horodatage d'exécution déjà repris.
     */
    @Column(name = "cursor")
    private Instant cursor;

    @Column(name = "last_polled_at")
    private Instant lastPolledAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @NotNull
    @Min(value = 0)
    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures;

    /**
     * Fin d'ouverture du disjoncteur en cas d'indisponibilité.
     */
    @Column(name = "circuit_open_until")
    private Instant circuitOpenUntil;

    /**
     * Curseur de reprise propre à chaque liaison.
     */
    @JsonIgnoreProperties(value = { "catalog", "campaignSync" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CampaignSync id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCursor() {
        return this.cursor;
    }

    public CampaignSync cursor(Instant cursor) {
        this.setCursor(cursor);
        return this;
    }

    public void setCursor(Instant cursor) {
        this.cursor = cursor;
    }

    public Instant getLastPolledAt() {
        return this.lastPolledAt;
    }

    public CampaignSync lastPolledAt(Instant lastPolledAt) {
        this.setLastPolledAt(lastPolledAt);
        return this;
    }

    public void setLastPolledAt(Instant lastPolledAt) {
        this.lastPolledAt = lastPolledAt;
    }

    public Instant getLastSuccessAt() {
        return this.lastSuccessAt;
    }

    public CampaignSync lastSuccessAt(Instant lastSuccessAt) {
        this.setLastSuccessAt(lastSuccessAt);
        return this;
    }

    public void setLastSuccessAt(Instant lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public Integer getConsecutiveFailures() {
        return this.consecutiveFailures;
    }

    public CampaignSync consecutiveFailures(Integer consecutiveFailures) {
        this.setConsecutiveFailures(consecutiveFailures);
        return this;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public Instant getCircuitOpenUntil() {
        return this.circuitOpenUntil;
    }

    public CampaignSync circuitOpenUntil(Instant circuitOpenUntil) {
        this.setCircuitOpenUntil(circuitOpenUntil);
        return this;
    }

    public void setCircuitOpenUntil(Instant circuitOpenUntil) {
        this.circuitOpenUntil = circuitOpenUntil;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public CampaignSync liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampaignSync)) {
            return false;
        }
        return getId() != null && getId().equals(((CampaignSync) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CampaignSync{" +
            "id=" + getId() +
            ", cursor='" + getCursor() + "'" +
            ", lastPolledAt='" + getLastPolledAt() + "'" +
            ", lastSuccessAt='" + getLastSuccessAt() + "'" +
            ", consecutiveFailures=" + getConsecutiveFailures() +
            ", circuitOpenUntil='" + getCircuitOpenUntil() + "'" +
            "}";
    }
}
