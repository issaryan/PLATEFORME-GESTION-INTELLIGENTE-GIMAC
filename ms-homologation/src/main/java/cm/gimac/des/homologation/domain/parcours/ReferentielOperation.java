package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Opération du référentiel de l'annexe A du CR (jeu d'initialisation, D-59), chargée par Liquibase :
 * étape, segment, porteurs (le premier est le responsable principal), types de projet concernés,
 * plateforme, grain de preuve, attributs cle, secret, valeurReference, oubliSansSymptome, pivot,
 * prédécesseurs et délai en jours. Lecture seule. Depuis D-77, le moteur ne lit plus cette table : elle et le
 * fichier dont elle est chargée ne servent qu'à la reprise en version 1 publiée ({@code ConversionAnnexeA}).
 */
@Entity
@Table(name = "parcours_referentiel_operation")
@IdClass(ReferentielOperation.Cle.class)
public class ReferentielOperation implements Serializable {

    /** Clé composée : version du CR et rang. */
    public record Cle(String versionCr, String rang) implements Serializable {
        public Cle() {
            this(null, null);
        }
    }

    public static final String VERSION_CR = "CR-1.4";

    @Id
    @Column(name = "version_cr")
    private String versionCr;

    @Id
    @Column(name = "rang")
    private String rang;

    @Column(name = "ordre")
    private int ordre;

    @Column(name = "etape")
    private String etape;

    @Column(name = "segment")
    private String segment;

    @Column(name = "porteurs")
    private String porteurs;

    @Column(name = "types")
    private String types;

    @Column(name = "plateforme")
    private String plateforme;

    @Column(name = "grain")
    private String grain;

    @Column(name = "cle")
    private boolean cle;

    @Column(name = "secret")
    private boolean secret;

    @Column(name = "valeur_reference")
    private boolean valeurReference;

    @Column(name = "oubli_sans_symptome")
    private boolean oubliSansSymptome;

    @Column(name = "pivot")
    private boolean pivot;

    @Column(name = "predecesseurs")
    private String predecesseurs;

    @Column(name = "delai_jours")
    private int delaiJours;

    protected ReferentielOperation() {}

    /** Construction explicite (tests unitaires, référentiels de démonstration). */
    public ReferentielOperation(String rang, int ordre, EtapeCode etape, SegmentParcours segment, List<Fonction> porteurs, List<TypeProjet> types, List<String> predecesseurs) {
        this.versionCr = VERSION_CR;
        this.rang = rang;
        this.ordre = ordre;
        this.etape = etape.name();
        this.segment = segment.name();
        this.porteurs = String.join("|", porteurs.stream().map(Fonction::name).toList());
        this.types = String.join("|", types.stream().map(TypeProjet::name).toList());
        this.grain = GrainPreuve.OPERATION.name();
        this.predecesseurs = String.join("|", predecesseurs);
        this.delaiJours = 3;
    }

    public ReferentielOperation attributs(PlateformeExecution p, GrainPreuve g, boolean cle, boolean secret, boolean valeurReference, boolean oubli, boolean pivot) {
        this.plateforme = p == null ? null : p.name();
        this.grain = g.name();
        this.cle = cle;
        this.secret = secret;
        this.valeurReference = valeurReference;
        this.oubliSansSymptome = oubli;
        this.pivot = pivot;
        return this;
    }

    /** Délai en jours lu dans le CSV (reprise). */
    public ReferentielOperation delai(int jours) {
        this.delaiJours = jours;
        return this;
    }

    private static List<String> liste(String s) {
        return s == null || s.isBlank() ? List.of() : Arrays.stream(s.split("\\|")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    public String getRang() {
        return rang;
    }

    public int getOrdre() {
        return ordre;
    }

    public EtapeCode getEtape() {
        return EtapeCode.valueOf(etape);
    }

    public SegmentParcours getSegment() {
        return SegmentParcours.valueOf(segment);
    }

    public List<Fonction> getPorteurs() {
        return liste(porteurs).stream().map(Fonction::valueOf).toList();
    }

    public List<TypeProjet> getTypes() {
        return liste(types).stream().map(TypeProjet::valueOf).toList();
    }

    public PlateformeExecution getPlateforme() {
        return plateforme == null || plateforme.isBlank() ? null : PlateformeExecution.valueOf(plateforme);
    }

    public GrainPreuve getGrain() {
        return GrainPreuve.valueOf(grain);
    }

    public boolean isCle() {
        return cle;
    }

    public boolean isSecret() {
        return secret;
    }

    public boolean isValeurReference() {
        return valeurReference;
    }

    public boolean isOubliSansSymptome() {
        return oubliSansSymptome;
    }

    public boolean isPivot() {
        return pivot;
    }

    public List<String> getPredecesseurs() {
        return liste(predecesseurs);
    }

    public int getDelaiJours() {
        return delaiJours;
    }

    /** Rang numérique (tronc commun) ; -1 pour une opération de branche (C, M, D). */
    public int rangNumerique() {
        return rang.chars().allMatch(Character::isDigit) ? Integer.parseInt(rang) : -1;
    }
}
