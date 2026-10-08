package cm.gimac.des.pilotage.domain.processus;

import cm.gimac.des.pilotage.domain.activite.EntitePilotage;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Instance d'un processus lancé (§2) : garde la version utilisée, l'intitulé donné par le lanceur, les
 * désignations faites au lancement (clé : CODE_ETAPE pour l'exécutant, CODE_ETAPE.validateur,
 * CODE_ETAPE.signature.CODE), le n+1 du lanceur résolu au lancement ; EN_COURS, SUSPENDUE, CLOSE ou ANNULEE.
 */
@Entity
@Table(name = "processus_instance")
public class InstanceProcessus extends EntitePilotage {

    @Column(name = "processus_id", nullable = false, updatable = false)
    private UUID processusId;

    @Column(name = "code", nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "version_processus", nullable = false, updatable = false)
    private int versionProcessus;

    @Column(name = "unite", nullable = false, length = 32, updatable = false)
    private String unite;

    @Column(name = "libelle", nullable = false, length = 200, updatable = false)
    private String libelle;

    @Column(name = "intitule", nullable = false, length = 200)
    private String intitule;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 16)
    private EtatInstance etat = EtatInstance.EN_COURS;

    @Column(name = "lanceur_id", nullable = false, length = 80, updatable = false)
    private String lanceurId;

    @Column(name = "n_plus_1_lanceur_id", length = 80, updatable = false)
    private String nPlus1LanceurId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "designations", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> designations = Map.of();

    /**
     * Valeurs du formulaire de lancement (D-98 §2), par code de champ : texte, nombre (texte décimal), date
     * AAAA-MM-JJ, période { debut, fin }, valeur de liste, booléen, pièce jointe { documentId, nomFichier, typeMime,
     * taille, sha256 } ; fixées au lancement.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formulaire", columnDefinition = "jsonb", nullable = false, updatable = false)
    private Map<String, Object> formulaire = Map.of();

    @Column(name = "lancee_le", nullable = false, updatable = false)
    private Instant lanceeLe;

    @Column(name = "close_le")
    private Instant closeLe;

    @Column(name = "motif", length = Processus.MOTIF_MAX)
    private String motif;

    @Column(name = "modifiee_le", nullable = false)
    private Instant modifieeLe;

    @Version
    @Column(name = "verrou", nullable = false)
    private long verrou;

    protected InstanceProcessus() {}

    public InstanceProcessus(Processus p, String intitule, String lanceurId, String nPlus1LanceurId, Map<String, String> designations, Instant le) {
        this.processusId = p.getId();
        this.code = p.getCode();
        this.versionProcessus = p.getVersion();
        this.unite = p.getUnite();
        this.libelle = p.getLibelle();
        this.intitule = intitule;
        this.lanceurId = lanceurId;
        this.nPlus1LanceurId = nPlus1LanceurId;
        this.designations = designations == null ? Map.of() : Map.copyOf(designations);
        this.lanceeLe = le;
        this.modifieeLe = le;
    }

    /** Valeurs du formulaire de lancement, fixées avant l'enregistrement de l'instance. */
    public void renseignerFormulaire(Map<String, Object> valeurs) {
        this.formulaire = valeurs == null ? Map.of() : new java.util.LinkedHashMap<>(valeurs);
    }

    /** Actes sur les étapes : instance en cours (409 PIL_PROCESSUS_INSTANCE_ETAT sinon). */
    public void exigerEnCours() {
        if (etat != EtatInstance.EN_COURS) {
            throw PilotageException.conflit("PIL_PROCESSUS_INSTANCE_ETAT", "L'instance n'est pas en cours", Map.of("etat", etat.name()));
        }
    }

    public void suspendre(String motif, Instant le) {
        exigerEnCours();
        this.etat = EtatInstance.SUSPENDUE;
        this.motif = motif;
        this.modifieeLe = le;
    }

    public void reprendre(String motif, Instant le) {
        if (etat != EtatInstance.SUSPENDUE) {
            throw PilotageException.conflit("PIL_PROCESSUS_INSTANCE_ETAT", "Seule une instance suspendue reprend", Map.of("etat", etat.name()));
        }
        this.etat = EtatInstance.EN_COURS;
        this.motif = motif;
        this.modifieeLe = le;
    }

    public void annuler(String motif, Instant le) {
        if (etat.estFinal()) {
            throw PilotageException.conflit("PIL_PROCESSUS_INSTANCE_ETAT", "L'instance est déjà close ou annulée", Map.of("etat", etat.name()));
        }
        this.etat = EtatInstance.ANNULEE;
        this.motif = motif;
        this.closeLe = le;
        this.modifieeLe = le;
    }

    /** Toutes les étapes terminées : l'instance est close. */
    public void clore(Instant le) {
        this.etat = EtatInstance.CLOSE;
        this.closeLe = le;
        this.modifieeLe = le;
    }

    public void toucher(Instant le) {
        this.modifieeLe = le;
    }

    public UUID getProcessusId() {
        return processusId;
    }

    public String getCode() {
        return code;
    }

    public int getVersionProcessus() {
        return versionProcessus;
    }

    public String getUnite() {
        return unite;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getIntitule() {
        return intitule;
    }

    public EtatInstance getEtat() {
        return etat;
    }

    public String getLanceurId() {
        return lanceurId;
    }

    public String getNPlus1LanceurId() {
        return nPlus1LanceurId;
    }

    public Map<String, String> getDesignations() {
        return designations;
    }

    public Map<String, Object> getFormulaire() {
        return formulaire == null ? Map.of() : formulaire;
    }

    public Instant getLanceeLe() {
        return lanceeLe;
    }

    public Instant getCloseLe() {
        return closeLe;
    }

    public String getMotif() {
        return motif;
    }

    public Instant getModifieeLe() {
        return modifieeLe;
    }
}
