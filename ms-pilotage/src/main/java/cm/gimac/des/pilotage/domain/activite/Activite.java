package cm.gimac.des.pilotage.domain.activite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Activité du jour d'un compte interne (§2.2). */
@Entity
@Table(name = "activite")
public class Activite extends EntitePilotage {

    @Column(name = "auteur_id", nullable = false, length = 80)
    private String auteurId;

    @Column(name = "jour", nullable = false)
    private LocalDate jour;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 16)
    private StatutActivite statut = StatutActivite.A_FAIRE;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Column(name = "tache_id")
    private UUID tacheId;

    @Column(name = "reportee_au")
    private LocalDate reporteeAu;

    /** Activité reportée dont celle-ci est la copie (§2.2). */
    @Column(name = "origine_id")
    private UUID origineId;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Activite() {}

    public Activite(String auteurId, Instant le) {
        this.auteurId = auteurId;
        this.creeLe = le;
        this.modifieLe = le;
    }

    public void decrire(LocalDate jour, String titre, String description, Integer dureeMinutes, int ordre, UUID tacheId, Instant le) {
        this.jour = jour;
        this.titre = titre;
        this.description = description;
        this.dureeMinutes = dureeMinutes;
        this.ordre = ordre;
        this.tacheId = tacheId;
        this.modifieLe = le;
    }

    public void changerStatut(StatutActivite statut, LocalDate reporteeAu, Instant le) {
        this.statut = statut;
        this.reporteeAu = statut == StatutActivite.REPORTEE ? reporteeAu : null;
        this.modifieLe = le;
    }

    /** Copie A_FAIRE d'une activité reportée, au jour indiqué, en fin de journée (§2.2). */
    public Activite copieReportee(int ordre, Instant le) {
        Activite copie = new Activite(auteurId, le);
        copie.decrire(reporteeAu, titre, description, null, ordre, tacheId, le);
        copie.origineId = getId();
        return copie;
    }

    public String getAuteurId() {
        return auteurId;
    }

    public LocalDate getJour() {
        return jour;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public StatutActivite getStatut() {
        return statut;
    }

    public Integer getDureeMinutes() {
        return dureeMinutes;
    }

    public int getOrdre() {
        return ordre;
    }

    public UUID getTacheId() {
        return tacheId;
    }

    public LocalDate getReporteeAu() {
        return reporteeAu;
    }

    public UUID getOrigineId() {
        return origineId;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }
}
