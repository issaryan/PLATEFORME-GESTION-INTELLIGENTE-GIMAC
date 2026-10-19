package cm.gimac.des.monitoring.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Échange d'astreinte entre deux opérateurs (MON-12, table on_call_swap). */
@Entity
@Table(name = "on_call_swap")
public class EchangeAstreinte {

    /** Circuit : proposition, accord ou refus du collègue, approbation ou rejet du chef du DES. */
    public enum Etat {
        PROPOSE,
        ACCEPTE,
        REFUSE,
        APPROUVE,
        REJETE,
    }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "roster_id", nullable = false)
    private UUID grilleId;

    @Column(name = "assignment_id", nullable = false)
    private UUID affectationId;

    @Column(name = "target_assignment_id", nullable = false)
    private UUID affectationCibleId;

    @Column(name = "requester_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "target_operator_id", nullable = false)
    private UUID cibleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private Etat etat;

    @Column(name = "reason", length = 500)
    private String motif;

    @Column(name = "created_at", nullable = false)
    private Instant creeLe;

    @Column(name = "decided_at")
    private Instant decideLe;

    @Column(name = "decided_by")
    private UUID decidePar;

    @Column(name = "new_roster_id")
    private UUID nouvelleGrilleId;

    protected EchangeAstreinte() {}

    public EchangeAstreinte(UUID grilleId, UUID affectationId, UUID affectationCibleId, UUID demandeurId, UUID cibleId, String motif, Instant creeLe) {
        this.id = UUID.randomUUID();
        this.grilleId = grilleId;
        this.affectationId = affectationId;
        this.affectationCibleId = affectationCibleId;
        this.demandeurId = demandeurId;
        this.cibleId = cibleId;
        this.motif = motif;
        this.creeLe = creeLe;
        this.etat = Etat.PROPOSE;
    }

    /** Fait passer l'échange à l'état donné, par la personne donnée. */
    public void decider(Etat nouvel, UUID par, Instant le) {
        this.etat = nouvel;
        this.decidePar = par;
        this.decideLe = le;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGrilleId() {
        return grilleId;
    }

    public UUID getAffectationId() {
        return affectationId;
    }

    public UUID getAffectationCibleId() {
        return affectationCibleId;
    }

    public UUID getDemandeurId() {
        return demandeurId;
    }

    public UUID getCibleId() {
        return cibleId;
    }

    public Etat getEtat() {
        return etat;
    }

    public String getMotif() {
        return motif;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getDecideLe() {
        return decideLe;
    }

    public UUID getDecidePar() {
        return decidePar;
    }

    public UUID getNouvelleGrilleId() {
        return nouvelleGrilleId;
    }

    public void setNouvelleGrilleId(UUID nouvelleGrilleId) {
        this.nouvelleGrilleId = nouvelleGrilleId;
    }
}
