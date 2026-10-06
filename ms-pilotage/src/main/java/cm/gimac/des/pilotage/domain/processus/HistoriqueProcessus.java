package cm.gimac.des.pilotage.domain.processus;

import cm.gimac.des.pilotage.domain.activite.EntitePilotage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Ligne de l'historique d'un processus (définition) ou d'une instance (§2 : qui, quoi, quand), en ajout seul :
 * jamais modifiée ni supprimée (la base refuse aussi toute mise à jour ou suppression).
 */
@Entity
@Immutable
@Table(name = "processus_historique")
public class HistoriqueProcessus extends EntitePilotage {

    public static final int DETAIL_MAX = 1200;

    @Column(name = "processus_id", updatable = false)
    private UUID processusId;

    @Column(name = "instance_id", updatable = false)
    private UUID instanceId;

    @Column(name = "etape", length = 40, updatable = false)
    private String etape;

    @Column(name = "quand", nullable = false, updatable = false)
    private Instant quand;

    @Column(name = "acteur_id", nullable = false, length = 80, updatable = false)
    private String acteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 24, updatable = false)
    private ActionProcessus action;

    @Column(name = "detail", length = DETAIL_MAX, updatable = false)
    private String detail;

    protected HistoriqueProcessus() {}

    public HistoriqueProcessus(UUID processusId, UUID instanceId, String etape, Instant quand, String acteurId, ActionProcessus action, String detail) {
        this.processusId = processusId;
        this.instanceId = instanceId;
        this.etape = etape;
        this.quand = quand;
        this.acteurId = acteurId;
        this.action = action;
        this.detail = detail == null || detail.length() <= DETAIL_MAX ? detail : detail.substring(0, DETAIL_MAX);
    }

    public UUID getProcessusId() {
        return processusId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getEtape() {
        return etape;
    }

    public Instant getQuand() {
        return quand;
    }

    public String getActeurId() {
        return acteurId;
    }

    public ActionProcessus getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }
}
