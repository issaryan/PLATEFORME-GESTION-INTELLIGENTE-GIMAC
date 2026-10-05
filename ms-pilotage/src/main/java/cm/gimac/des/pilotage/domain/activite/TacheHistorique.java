package cm.gimac.des.pilotage.domain.activite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Ligne de l'historique d'une tâche (§2.3), en ajout seul : jamais modifiée ni supprimée (entité
 * immuable ; la base refuse aussi toute mise à jour ou suppression).
 */
@Entity
@Immutable
@Table(name = "tache_historique")
public class TacheHistorique extends EntitePilotage {

    @Column(name = "tache_id", nullable = false, updatable = false)
    private UUID tacheId;

    @Column(name = "quand", nullable = false, updatable = false)
    private Instant quand;

    @Column(name = "acteur_id", nullable = false, updatable = false, length = 80)
    private String acteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 16)
    private ActionHistorique action;

    @Column(name = "detail", updatable = false, length = 1200)
    private String detail;

    protected TacheHistorique() {}

    public TacheHistorique(UUID tacheId, Instant quand, String acteurId, ActionHistorique action, String detail) {
        this.tacheId = tacheId;
        this.quand = quand;
        this.acteurId = acteurId;
        this.action = action;
        this.detail = detail;
    }

    public UUID getTacheId() {
        return tacheId;
    }

    public Instant getQuand() {
        return quand;
    }

    public String getActeurId() {
        return acteurId;
    }

    public ActionHistorique getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }
}
