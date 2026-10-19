package cm.gimac.des.monitoring.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Palier d'escalade atteint par une alerte racine non acquittée (MON-05, table alert_escalation). */
@Entity
@Table(name = "alert_escalation")
public class EscaladeAlerte {

    @Id
    @Column(name = "alert_id")
    private UUID alerteId;

    @Column(name = "palier", nullable = false)
    private int palier;

    @Column(name = "escalade_le", nullable = false)
    private Instant escaladeLe;

    @Column(name = "plage_non_couverte", nullable = false)
    private boolean plageNonCouverte;

    protected EscaladeAlerte() {}

    public EscaladeAlerte(UUID alerteId) {
        this.alerteId = alerteId;
    }

    public UUID getAlerteId() {
        return alerteId;
    }

    public int getPalier() {
        return palier;
    }

    public void setPalier(int palier) {
        this.palier = palier;
    }

    public Instant getEscaladeLe() {
        return escaladeLe;
    }

    public void setEscaladeLe(Instant escaladeLe) {
        this.escaladeLe = escaladeLe;
    }

    public boolean isPlageNonCouverte() {
        return plageNonCouverte;
    }

    public void setPlageNonCouverte(boolean plageNonCouverte) {
        this.plageNonCouverte = plageNonCouverte;
    }
}
