package cm.gimac.des.pilotage.domain.activite;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Tâche (§2.3 ; CP PIL-04, PIL-08) : un responsable unique (RG-20), un créateur, une origine (tâche
 * interne ou créée par un autre service) et un état soumis aux transitions du CC (p.238, p.245).
 */
@Entity
@Table(name = "tache")
public class Tache extends EntitePilotage {

    public static final int MOTIF_MAX = 1000;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "echeance")
    private LocalDate echeance;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false, length = 16)
    private PrioriteTache priorite = PrioriteTache.NORMALE;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 16)
    private EtatTache etat = EtatTache.OUVERTE;

    @Column(name = "responsable_id", nullable = false, length = 80)
    private String responsableId;

    @Column(name = "createur_id", nullable = false, length = 80)
    private String createurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origine_type", nullable = false, length = 16)
    private TypeOrigine origineType = TypeOrigine.INTERNE;

    @Column(name = "origine_reference", length = 200)
    private String origineReference;

    @Column(name = "origine_service", length = 60)
    private String origineService;

    @Column(name = "origine_lien", length = 500)
    private String origineLien;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Column(name = "terminee_le")
    private Instant termineeLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Tache() {}

    public Tache(String createurId, String responsableId, Instant le) {
        this.createurId = createurId;
        this.responsableId = responsableId;
        this.creeLe = le;
        this.modifieLe = le;
    }

    public void decrire(String titre, String description, LocalDate echeance, PrioriteTache priorite, Instant le) {
        this.titre = titre;
        this.description = description;
        this.echeance = echeance;
        this.priorite = priorite;
        this.modifieLe = le;
    }

    public void provenirDe(TypeOrigine type, String reference, String service, String lien) {
        this.origineType = type;
        this.origineReference = reference;
        this.origineService = service;
        this.origineLien = lien;
    }

    public void reaffecter(String responsableId, Instant le) {
        this.responsableId = responsableId;
        this.modifieLe = le;
    }

    /**
     * Transition demandée par {@code acteurId} : le responsable fait les transitions, le créateur peut
     * annuler (403 PIL_ACTION_RESERVEE sinon) ; hors transition permise, 409 PIL_TRANSITION_INTERDITE ;
     * BLOQUEE, ANNULEE et la réouverture exigent un motif (422 PIL_MOTIF_OBLIGATOIRE).
     *
     * @return l'état quitté.
     */
    public EtatTache transiter(EtatTache cible, String motif, String acteurId, Instant le) {
        if (cible == null) {
            throw PilotageException.champ("etat", "État cible absent ou inconnu");
        }
        boolean responsable = responsableId.equals(acteurId);
        boolean createur = createurId.equals(acteurId);
        if (!responsable && !(createur && cible == EtatTache.ANNULEE)) {
            throw PilotageException.reservee("Seul le responsable fait les transitions ; le créateur peut annuler");
        }
        if (!etat.peutPasserA(cible)) {
            throw PilotageException.conflit(
                "PIL_TRANSITION_INTERDITE",
                "Transition non permise",
                Map.of("de", etat.name(), "vers", cible.name())
            );
        }
        if (etat.motifExigePour(cible) && (motif == null || motif.isBlank())) {
            throw PilotageException.regle("PIL_MOTIF_OBLIGATOIRE", "Un motif est exigé pour cette transition", Map.of("vers", cible.name()));
        }
        if (motif != null && motif.length() > MOTIF_MAX) {
            throw PilotageException.champ("motif", "Motif de " + MOTIF_MAX + " caractères au plus");
        }
        EtatTache avant = etat;
        etat = cible;
        termineeLe = cible == EtatTache.TERMINEE ? le : null;
        modifieLe = le;
        return avant;
    }

    /**
     * Clôture demandée par le service qui a créé la tâche (D-77) : une tâche encore à traiter est terminée,
     * quel que soit son état ouvert ; terminée ou annulée, elle reste inchangée.
     *
     * @return l'état quitté, ou null si la tâche n'a pas changé.
     */
    public EtatTache terminerParService(Instant le) {
        if (!etat.estOuvert()) {
            return null;
        }
        EtatTache avant = etat;
        etat = EtatTache.TERMINEE;
        termineeLe = le;
        modifieLe = le;
        return avant;
    }

    /**
     * Réouverture demandée par le service qui a créé la tâche (D-77) : une tâche terminée reprend EN_COURS
     * (transition de réouverture du CC) ; ouverte ou annulée, elle reste inchangée.
     *
     * @return l'état quitté, ou null si la tâche n'a pas changé.
     */
    public EtatTache rouvrirParService(Instant le) {
        if (etat != EtatTache.TERMINEE) {
            return null;
        }
        etat = EtatTache.EN_COURS;
        termineeLe = null;
        modifieLe = le;
        return EtatTache.TERMINEE;
    }

    /** En retard : échéance dépassée (fuseau Africa/Douala) et tâche encore ouverte. */
    public boolean enRetard(LocalDate aujourdhui) {
        return echeance != null && echeance.isBefore(aujourdhui) && etat.estOuvert();
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEcheance() {
        return echeance;
    }

    public PrioriteTache getPriorite() {
        return priorite;
    }

    public EtatTache getEtat() {
        return etat;
    }

    public String getResponsableId() {
        return responsableId;
    }

    public String getCreateurId() {
        return createurId;
    }

    public TypeOrigine getOrigineType() {
        return origineType;
    }

    public String getOrigineReference() {
        return origineReference;
    }

    public String getOrigineService() {
        return origineService;
    }

    public String getOrigineLien() {
        return origineLien;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }

    public Instant getTermineeLe() {
        return termineeLe;
    }
}
