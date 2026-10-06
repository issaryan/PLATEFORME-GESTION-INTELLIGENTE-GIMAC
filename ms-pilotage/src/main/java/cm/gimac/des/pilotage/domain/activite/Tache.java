package cm.gimac.des.pilotage.domain.activite;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tâche (§2.3 ; CP PIL-04, PIL-08) : un responsable unique (RG-20), un créateur, une origine (tâche
 * interne ou créée par un autre service) et un état soumis aux transitions du CC (p.238, p.245).
 * Tâches confiées (D-81 §6) : une tâche confiée à plusieurs personnes appartient à un groupe
 * ({@code groupeId}, une tâche par personne) ; une tâche confiée à une unité ({@code uniteCible}) naît
 * À PRENDRE, sans responsable, jusqu'à sa prise ou son attribution ; une relance au plus par 24 h.
 * Processus d'unité (D-87) : une tâche à prendre peut être réservée aux porteurs d'un profil
 * ({@code profilRequis}) ; la tâche d'une étape (origine PROCESSUS) suit son étape : le service la confie, la
 * remet à prendre, la termine ou l'annule.
 */
@Entity
@Table(name = "tache")
public class Tache extends EntitePilotage {

    public static final int MOTIF_MAX = 1000;

    /** Délai minimal entre deux relances d'une tâche (D-81 §6). */
    public static final Duration DELAI_RELANCE = Duration.ofHours(24);

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

    /** Null tant qu'une tâche confiée à une unité est à prendre. */
    @Column(name = "responsable_id", length = 80)
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

    /** Tâches d'un même envoi à plusieurs personnes. */
    @Column(name = "groupe_id")
    private UUID groupeId;

    /** Unité à laquelle la tâche a été confiée (code d'unité de MS-IAM). */
    @Column(name = "unite_cible", length = 32)
    private String uniteCible;

    @Column(name = "relancee_le")
    private Instant relanceeLe;

    /** Profil exigé pour prendre la tâche (étape d'un processus confiée par profil, D-87). */
    @Column(name = "profil_requis", length = 80)
    private String profilRequis;

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

    /** Tâche confiée à une unité : à prendre, sans responsable. */
    public static Tache pourUnite(String createurId, String uniteCible, Instant le) {
        Tache t = new Tache(createurId, null, le);
        t.uniteCible = uniteCible;
        t.etat = EtatTache.A_PRENDRE;
        return t;
    }

    /** Tâche à prendre réservée aux membres de l'unité portant {@code profil} (null : tout membre). */
    public static Tache pourUniteEtProfil(String createurId, String uniteCible, String profil, Instant le) {
        Tache t = pourUnite(createurId, uniteCible, le);
        t.profilRequis = profil;
        return t;
    }

    /**
     * Remise à prendre par le service (départ du responsable, D-87) : la tâche encore ouverte perd son
     * responsable et redevient à prendre par les membres de {@code unite} portant {@code profil}.
     *
     * @return l'état quitté, ou null si la tâche n'est plus ouverte.
     */
    public EtatTache remettreAPrendre(String unite, String profil, Instant le) {
        if (!etat.estOuvert()) {
            return null;
        }
        EtatTache avant = etat;
        this.responsableId = null;
        this.uniteCible = unite;
        this.profilRequis = profil;
        this.etat = EtatTache.A_PRENDRE;
        this.modifieLe = le;
        return avant;
    }

    /**
     * Affectation par le service (réaffectation d'une étape, D-87) : une tâche à prendre devient OUVERTE avec ce
     * responsable ; une tâche déjà confiée change de responsable sans changer d'état.
     */
    public void confierParService(String responsable, Instant le) {
        if (etat == EtatTache.A_PRENDRE) {
            this.etat = EtatTache.OUVERTE;
        }
        this.responsableId = responsable;
        this.modifieLe = le;
    }

    /**
     * Annulation par le service (instance annulée, D-87) : une tâche encore ouverte est annulée.
     *
     * @return l'état quitté, ou null si la tâche n'a pas changé.
     */
    public EtatTache annulerParService(Instant le) {
        if (!etat.estOuvert()) {
            return null;
        }
        EtatTache avant = etat;
        etat = EtatTache.ANNULEE;
        termineeLe = null;
        modifieLe = le;
        return avant;
    }

    public void rattacherAuGroupe(UUID groupe) {
        this.groupeId = groupe;
    }

    /**
     * Prise par un membre de l'unité, ou attribution à un membre : la tâche à prendre devient OUVERTE avec
     * ce responsable ; une tâche déjà prise ou annulée : 409 PIL_TRANSITION_INTERDITE.
     */
    public void confierA(String responsable, Instant le) {
        if (etat != EtatTache.A_PRENDRE) {
            throw PilotageException.conflit("PIL_TRANSITION_INTERDITE", "La tâche n'est plus à prendre", Map.of("de", etat.name(), "vers", EtatTache.OUVERTE.name()));
        }
        if (responsable == null || responsable.isBlank()) {
            throw PilotageException.champ("responsableId", "Responsable absent");
        }
        this.responsableId = responsable;
        this.etat = EtatTache.OUVERTE;
        this.modifieLe = le;
    }

    /**
     * Relance du responsable : tâche ouverte et confiée (sinon 409 PIL_RELANCE_IMPOSSIBLE), au plus une par
     * 24 h (429 PIL_RELANCE_RECENTE, avec la date de la prochaine relance possible).
     */
    public void relancer(Instant le) {
        if (responsableId == null || etat == EtatTache.A_PRENDRE || !etat.estOuvert()) {
            throw PilotageException.conflit("PIL_RELANCE_IMPOSSIBLE", "Seule une tâche confiée et encore ouverte se relance", Map.of("etat", etat.name()));
        }
        if (relanceeLe != null && le.isBefore(relanceeLe.plus(DELAI_RELANCE))) {
            Map<String, Object> details = new HashMap<>();
            details.put("derniereRelance", relanceeLe.toString());
            details.put("prochaineRelance", relanceeLe.plus(DELAI_RELANCE).toString());
            throw PilotageException.tropTot("PIL_RELANCE_RECENTE", "Une relance a déjà été adressée dans les dernières 24 heures", details);
        }
        this.relanceeLe = le;
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
        boolean responsable = acteurId != null && acteurId.equals(responsableId);
        boolean createur = createurId.equals(acteurId);
        if (etat == EtatTache.A_PRENDRE && cible == EtatTache.OUVERTE) {
            throw PilotageException.reservee("Une tâche à prendre s'ouvre par sa prise ou son attribution");
        }
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

    public UUID getGroupeId() {
        return groupeId;
    }

    public String getUniteCible() {
        return uniteCible;
    }

    public Instant getRelanceeLe() {
        return relanceeLe;
    }

    public String getProfilRequis() {
        return profilRequis;
    }
}
