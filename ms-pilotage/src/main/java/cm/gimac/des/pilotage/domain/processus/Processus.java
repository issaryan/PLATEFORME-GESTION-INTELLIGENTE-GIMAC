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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Une version d'un processus d'unité (§1) : code stable, numéro de version, état (BROUILLON, PUBLIEE, RETIREE),
 * unité propriétaire, graphe d'étapes (JSONB). Seul un brouillon se modifie ou se supprime ; une version publiée
 * est immuable (les instances lancées la gardent) ; une nouvelle version naît en brouillon d'une version publiée.
 * Exception (D-103) : le public (qui peut voir et lancer) est un réglage d'accès, modifiable sur un brouillon comme sur
 * une version publiée ; étapes, formulaire de lancement et séparation des fonctions restent figés.
 */
@Entity
@Table(name = "processus")
public class Processus extends EntitePilotage {

    public static final int MOTIF_MAX = 1000;

    @Column(name = "code", nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 16)
    private EtatProcessus etat = EtatProcessus.BROUILLON;

    @Column(name = "unite", nullable = false, length = 32, updatable = false)
    private String unite;

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "categorie", length = 80)
    private String categorie;

    @Column(name = "delai_cible_jours")
    private Integer delaiCibleJours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "etapes", columnDefinition = "jsonb", nullable = false)
    private List<Etape> etapes = List.of();

    /** Qui peut lancer cette version (D-98 §1) ; défaut : les membres de l'unité propriétaire. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initiateurs", columnDefinition = "jsonb", nullable = false)
    private Initiateurs initiateurs = Initiateurs.parDefaut();

    /** Formulaire de lancement (D-98 §2) : champs typés remplis par l'initiateur. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formulaire", columnDefinition = "jsonb", nullable = false)
    private Formulaire formulaire = Formulaire.vide();

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    @Column(name = "cree_par", nullable = false, length = 80, updatable = false)
    private String creePar;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Column(name = "modifie_par", nullable = false, length = 80)
    private String modifiePar;

    @Column(name = "publiee_le")
    private Instant publieeLe;

    @Column(name = "publiee_par", length = 80)
    private String publieePar;

    @Column(name = "motif_publication", length = MOTIF_MAX)
    private String motifPublication;

    @Column(name = "avertissements_leves", nullable = false)
    private boolean avertissementsLeves;

    @Column(name = "retiree_le")
    private Instant retireeLe;

    @Column(name = "retiree_par", length = 80)
    private String retireePar;

    @Column(name = "motif_retrait", length = MOTIF_MAX)
    private String motifRetrait;

    @Version
    @Column(name = "verrou", nullable = false)
    private long verrou;

    protected Processus() {}

    private Processus(String code, int version, String unite, String acteur, Instant le) {
        this.code = code;
        this.version = version;
        this.unite = unite;
        this.creeLe = le;
        this.creePar = acteur;
        this.modifieLe = le;
        this.modifiePar = acteur;
    }

    /** Nouveau processus : brouillon de la version 1. */
    public static Processus brouillon(String code, String unite, String acteur, Instant le) {
        return new Processus(code, 1, unite, acteur, le);
    }

    /** Brouillon de la version {@code numero}, copie de cette version publiée ou retirée. */
    public Processus nouvelleVersion(int numero, String acteur, Instant le) {
        if (etat == EtatProcessus.BROUILLON) {
            throw PilotageException.conflit("PIL_PROCESSUS_ETAT", "Une nouvelle version naît d'une version publiée ou retirée", Map.of("etat", etat.name()));
        }
        Processus p = new Processus(code, numero, unite, acteur, le);
        p.libelle = libelle;
        p.description = description;
        p.categorie = categorie;
        p.delaiCibleJours = delaiCibleJours;
        p.etapes = etapes;
        p.initiateurs = getInitiateurs();
        p.formulaire = getFormulaire();
        return p;
    }

    /** Description et graphe d'un brouillon ; 409 PIL_PROCESSUS_ETAT hors brouillon. */
    public void decrire(String libelle, String description, String categorie, Integer delaiCibleJours, List<Etape> etapes, String acteur, Instant le) {
        exigerBrouillon();
        this.libelle = libelle;
        this.description = description;
        this.categorie = categorie;
        this.delaiCibleJours = delaiCibleJours;
        this.etapes = etapes == null ? List.of() : List.copyOf(etapes);
        this.modifieLe = le;
        this.modifiePar = acteur;
    }

    /** Initiateurs et formulaire de lancement d'un brouillon (D-98) ; absents : valeurs par défaut. */
    public void definirLancement(Initiateurs initiateurs, Formulaire formulaire, String acteur, Instant le) {
        exigerBrouillon();
        this.initiateurs = initiateurs == null ? Initiateurs.parDefaut() : initiateurs;
        this.formulaire = formulaire == null ? Formulaire.vide() : formulaire;
        this.modifieLe = le;
        this.modifiePar = acteur;
    }

    /**
     * Public d'un brouillon ou d'une version publiée (D-103) : règles et paramètres remplacés, séparation des fonctions
     * ({@code initiateurIntervenant}) conservée telle quelle ; 409 PIL_PROCESSUS_NON_MODIFIABLE sur une version retirée.
     * Vaut pour les prochains lancements : les instances déjà lancées ne relisent pas le public.
     */
    public void reglerPublic(Initiateurs nouveau, String acteur, Instant le) {
        if (etat == EtatProcessus.RETIREE) {
            throw PilotageException.conflit(
                "PIL_PROCESSUS_NON_MODIFIABLE",
                "Une version retirée ne se modifie plus, son public compris",
                Map.of("etat", etat.name())
            );
        }
        Initiateurs n = nouveau == null ? Initiateurs.parDefaut() : nouveau;
        this.initiateurs = new Initiateurs(n.regles(), n.unites(), n.profils(), n.permissions(), n.personnes(), getInitiateurs().initiateurIntervenant());
        this.modifieLe = le;
        this.modifiePar = acteur;
    }

    public void publier(String acteur, String motif, boolean leves, Instant le) {
        exigerBrouillon();
        this.etat = EtatProcessus.PUBLIEE;
        this.publieeLe = le;
        this.publieePar = acteur;
        this.motifPublication = motif;
        this.avertissementsLeves = leves;
        this.modifieLe = le;
        this.modifiePar = acteur;
    }

    /** Retrait d'une version publiée (motif) ; les instances lancées continuent sur cette version. */
    public void retirer(String acteur, String motif, Instant le) {
        if (etat != EtatProcessus.PUBLIEE) {
            throw PilotageException.conflit("PIL_PROCESSUS_ETAT", "Seule une version publiée se retire", Map.of("etat", etat.name()));
        }
        this.etat = EtatProcessus.RETIREE;
        this.retireeLe = le;
        this.retireePar = acteur;
        this.motifRetrait = motif;
        this.modifieLe = le;
        this.modifiePar = acteur;
    }

    public void exigerBrouillon() {
        if (etat != EtatProcessus.BROUILLON) {
            throw PilotageException.conflit("PIL_PROCESSUS_ETAT", "Seul un brouillon se modifie ou se supprime", Map.of("etat", etat.name()));
        }
    }

    public Optional<Etape> etape(String codeEtape) {
        return etapes.stream().filter(e -> e.code().equals(codeEtape)).findFirst();
    }

    public String getCode() {
        return code;
    }

    public int getVersion() {
        return version;
    }

    public EtatProcessus getEtat() {
        return etat;
    }

    public String getUnite() {
        return unite;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }

    public String getCategorie() {
        return categorie;
    }

    public Integer getDelaiCibleJours() {
        return delaiCibleJours;
    }

    public List<Etape> getEtapes() {
        return etapes;
    }

    public Initiateurs getInitiateurs() {
        return initiateurs == null ? Initiateurs.parDefaut() : initiateurs;
    }

    public Formulaire getFormulaire() {
        return formulaire == null ? Formulaire.vide() : formulaire;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public String getCreePar() {
        return creePar;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }

    public String getModifiePar() {
        return modifiePar;
    }

    public Instant getPublieeLe() {
        return publieeLe;
    }

    public String getPublieePar() {
        return publieePar;
    }

    public String getMotifPublication() {
        return motifPublication;
    }

    public boolean isAvertissementsLeves() {
        return avertissementsLeves;
    }

    public Instant getRetireeLe() {
        return retireeLe;
    }

    public String getRetireePar() {
        return retireePar;
    }

    public String getMotifRetrait() {
        return motifRetrait;
    }
}
