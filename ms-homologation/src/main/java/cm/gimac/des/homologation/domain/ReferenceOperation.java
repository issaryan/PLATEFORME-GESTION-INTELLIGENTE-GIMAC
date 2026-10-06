package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import cm.gimac.des.homologation.domain.service.NoeudReferentiel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A ReferenceOperation.
 */
@Entity
@Table(name = "reference_operation")
@EntityListeners(ReferenceOperationImmutabilityListener.class)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceOperation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 40)
    @Column(name = "code", length = 40, nullable = false)
    private String code;

    @Size(max = 300)
    @Column(name = "libelle", length = 300)
    private String libelle;

    @Size(max = 60)
    @Column(name = "phase", length = 60)
    private String phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment")
    private Segment segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "partie")
    private Partie partie;

    @Size(max = 40)
    @Column(name = "service_code", length = 40)
    private String serviceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature_couverture")
    private NatureCouverture natureCouverture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preuve_attendue", columnDefinition = "jsonb")
    private String preuveAttendue;

    @Size(max = 80)
    @Column(name = "id_scenario", length = 80)
    private String idScenario;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predecesseurs", columnDefinition = "jsonb")
    private String predecesseurs;

    @Size(max = 60)
    @Column(name = "fonction_responsable", length = 60)
    private String fonctionResponsable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grille", columnDefinition = "jsonb")
    private String grille;

    @ManyToOne(optional = false)
    @NotNull
    private ReferentialVersion version;

    // ------------------------------------------------------------------ modèle du modélisateur (D-77, §1)

    @Size(max = 4000)
    @Column(name = "description", length = 4000)
    private String description;

    /** Code d'une étape de la version (D-89) ; la progression passe par son étape canonique. */
    @Column(name = "etape", length = 40)
    private String etape;

    @Column(name = "ordre")
    private Integer ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsable", length = 40)
    private Fonction responsable;

    @Enumerated(EnumType.STRING)
    @Column(name = "validateur", length = 40)
    private Fonction validateur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consultes", columnDefinition = "jsonb")
    private String consultes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "informes", columnDefinition = "jsonb")
    private String informes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "externes", columnDefinition = "jsonb")
    private String externes;

    @Column(name = "delai_jours")
    private Integer delaiJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "grain", length = 20)
    private GrainPreuve grain;

    @Enumerated(EnumType.STRING)
    @Column(name = "plateforme", length = 40)
    private PlateformeExecution plateforme;

    @Column(name = "cle", nullable = false)
    private boolean cle;

    @Column(name = "secret", nullable = false)
    private boolean secret;

    @Column(name = "oubli_sans_symptome", nullable = false)
    private boolean oubliSansSymptome;

    @Column(name = "pivot", nullable = false)
    private boolean pivot;

    @Size(max = 120)
    @Column(name = "valeur_reference", length = 120)
    private String valeurReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "livrables", columnDefinition = "jsonb")
    private String livrables;

    @Enumerated(EnumType.STRING)
    @Column(name = "geste", length = 40)
    private Geste geste;

    @Enumerated(EnumType.STRING)
    @Column(name = "jalon", length = 40)
    private Jalon jalon;

    @Size(max = 120)
    @Column(name = "declencheur", length = 120)
    private String declencheur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "position", columnDefinition = "jsonb")
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "particularite", length = 40)
    private Particularite particularite;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReferenceOperation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public ReferenceOperation code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return this.libelle;
    }

    public ReferenceOperation libelle(String libelle) {
        this.setLibelle(libelle);
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getPhase() {
        return this.phase;
    }

    public ReferenceOperation phase(String phase) {
        this.setPhase(phase);
        return this;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Segment getSegment() {
        return this.segment;
    }

    public ReferenceOperation segment(Segment segment) {
        this.setSegment(segment);
        return this;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Partie getPartie() {
        return this.partie;
    }

    public ReferenceOperation partie(Partie partie) {
        this.setPartie(partie);
        return this;
    }

    public void setPartie(Partie partie) {
        this.partie = partie;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public ReferenceOperation serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public NatureCouverture getNatureCouverture() {
        return this.natureCouverture;
    }

    public ReferenceOperation natureCouverture(NatureCouverture natureCouverture) {
        this.setNatureCouverture(natureCouverture);
        return this;
    }

    public void setNatureCouverture(NatureCouverture natureCouverture) {
        this.natureCouverture = natureCouverture;
    }

    public String getPreuveAttendue() {
        return this.preuveAttendue;
    }

    public ReferenceOperation preuveAttendue(String preuveAttendue) {
        this.setPreuveAttendue(preuveAttendue);
        return this;
    }

    public void setPreuveAttendue(String preuveAttendue) {
        this.preuveAttendue = preuveAttendue;
    }

    public String getIdScenario() {
        return this.idScenario;
    }

    public ReferenceOperation idScenario(String idScenario) {
        this.setIdScenario(idScenario);
        return this;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public String getPredecesseurs() {
        return this.predecesseurs;
    }

    public ReferenceOperation predecesseurs(String predecesseurs) {
        this.setPredecesseurs(predecesseurs);
        return this;
    }

    public void setPredecesseurs(String predecesseurs) {
        this.predecesseurs = predecesseurs;
    }

    public String getFonctionResponsable() {
        return this.fonctionResponsable;
    }

    public ReferenceOperation fonctionResponsable(String fonctionResponsable) {
        this.setFonctionResponsable(fonctionResponsable);
        return this;
    }

    public void setFonctionResponsable(String fonctionResponsable) {
        this.fonctionResponsable = fonctionResponsable;
    }

    public String getGrille() {
        return this.grille;
    }

    public ReferenceOperation grille(String grille) {
        this.setGrille(grille);
        return this;
    }

    public void setGrille(String grille) {
        this.grille = grille;
    }

    public ReferentialVersion getVersion() {
        return this.version;
    }

    public void setVersion(ReferentialVersion referentialVersion) {
        this.version = referentialVersion;
    }

    public ReferenceOperation version(ReferentialVersion referentialVersion) {
        this.setVersion(referentialVersion);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    // ------------------------------------------------------------------ HOM-01 : comportement

    /** Nouvelle opération d'un brouillon, rattachée à sa version. */
    public static ReferenceOperation nouvelle(ReferentialVersion version, String code) {
        ReferenceOperation op = new ReferenceOperation();
        op.code = code;
        op.version = version;
        op.predecesseurs = "[]";
        return op;
    }

    /**
     * Remplace les attributs de l'opération par la saisie (écriture complète d'un brouillon). La position et la
     * particularité absentes de la saisie sont conservées (elles ont leurs propres gestes dans le modélisateur).
     * Quand l'étape est saisie, les attributs historiques absents sont dérivés du modèle (§1).
     */
    public void appliquer(ReferenceOperationData data) {
        this.libelle = data.libelle();
        this.phase = data.phase();
        this.segment = data.segment();
        this.partie = data.partie();
        this.serviceCode = data.serviceCode();
        this.natureCouverture = data.natureCouverture();
        this.preuveAttendue = JsonTexte.ecrire(data.preuveAttendue());
        this.idScenario = data.idScenario();
        this.predecesseurs = JsonTexte.ecrire(data.predecesseurs() == null ? List.of() : data.predecesseurs().stream().distinct().toList());
        this.fonctionResponsable = data.fonctionResponsable();
        this.grille = JsonTexte.ecrire(data.grille());
        this.description = data.description();
        this.etape = data.etape();
        this.ordre = data.ordre();
        this.responsable = data.responsable();
        this.validateur = data.validateur();
        this.consultes = JsonTexte.ecrire(data.consultes() == null ? List.of() : data.consultes().stream().distinct().toList());
        this.informes = JsonTexte.ecrire(data.informes() == null ? List.of() : data.informes().stream().distinct().toList());
        this.externes = JsonTexte.ecrire(data.externes() == null ? List.of() : data.externes().stream().distinct().toList());
        this.delaiJours = data.delaiJours();
        this.grain = data.grain();
        this.plateforme = data.plateforme();
        this.cle = Boolean.TRUE.equals(data.cle());
        this.secret = Boolean.TRUE.equals(data.secret());
        this.oubliSansSymptome = Boolean.TRUE.equals(data.oubliSansSymptome());
        this.pivot = Boolean.TRUE.equals(data.pivot());
        this.valeurReference = data.valeurReference() == null || data.valeurReference().isBlank() ? null : data.valeurReference().trim();
        this.livrables = JsonTexte.ecrire(data.livrables() == null ? List.of() : data.livrables());
        this.geste = data.geste();
        this.jalon = data.jalon();
        this.declencheur = data.declencheur() == null || data.declencheur().isBlank() ? null : data.declencheur().trim();
        if (data.position() != null) {
            this.position = JsonTexte.ecrire(data.position());
        }
        if (data.particularite() != null) {
            this.particularite = data.particularite();
        }
        if (etape != null) {
            deriverAttributsHistoriques();
        }
    }

    /** Attributs historiques (couverture, HOM-01) déduits du modèle quand la saisie ne les porte pas. */
    private void deriverAttributsHistoriques() {
        if (phase == null || phase.isBlank()) {
            phase = etape;
        }
        if (partie == null && responsable != null) {
            partie = responsable.externe() ? Partie.PARTICIPANT : Partie.DES;
        }
        if (serviceCode == null || serviceCode.isBlank()) {
            serviceCode = "TRANSVERSE";
        }
        if (natureCouverture == null) {
            natureCouverture = NatureCouverture.AUCUNE;
        }
        if ((fonctionResponsable == null || fonctionResponsable.isBlank()) && responsable != null) {
            fonctionResponsable = responsable.name();
        }
        if (preuveAttendue == null || preuveAttendue.isBlank() || "null".equals(preuveAttendue) || "{}".equals(preuveAttendue.replaceAll("\\s", ""))) {
            Map<String, Object> pa = new java.util.LinkedHashMap<>();
            pa.put("grain", grainEffectif().name());
            if (plateforme != null) {
                pa.put("plateforme", plateforme.name());
            }
            if (cle) {
                pa.put("cle", true);
            }
            if (secret) {
                pa.put("secret", true);
            }
            if (valeurReference != null) {
                pa.put("valeur_reference", true);
            }
            if (pivot) {
                pa.put("pivot", true);
            }
            preuveAttendue = JsonTexte.ecrire(pa);
        }
    }

    /** Copie profonde vers une autre version (dérivation d'un brouillon, HOM-01). */
    public ReferenceOperation copierVers(ReferentialVersion cible) {
        ReferenceOperation copie = nouvelle(cible, code);
        copie.libelle = libelle;
        copie.phase = phase;
        copie.segment = segment;
        copie.partie = partie;
        copie.serviceCode = serviceCode;
        copie.natureCouverture = natureCouverture;
        copie.preuveAttendue = preuveAttendue;
        copie.idScenario = idScenario;
        copie.predecesseurs = predecesseurs;
        copie.fonctionResponsable = fonctionResponsable;
        copie.grille = grille;
        copie.description = description;
        copie.etape = etape;
        copie.ordre = ordre;
        copie.responsable = responsable;
        copie.validateur = validateur;
        copie.consultes = consultes;
        copie.informes = informes;
        copie.externes = externes;
        copie.delaiJours = delaiJours;
        copie.grain = grain;
        copie.plateforme = plateforme;
        copie.cle = cle;
        copie.secret = secret;
        copie.oubliSansSymptome = oubliSansSymptome;
        copie.pivot = pivot;
        copie.valeurReference = valeurReference;
        copie.livrables = livrables;
        copie.geste = geste;
        copie.jalon = jalon;
        copie.declencheur = declencheur;
        copie.position = position;
        copie.particularite = particularite;
        return copie;
    }

    public List<String> predecesseursListe() {
        return JsonTexte.listeDeTextes(predecesseurs);
    }

    /** Ajoute ou retire un prédécesseur (arc du modélisateur). */
    public void basculerPredecesseur(String de, boolean present) {
        List<String> l = new ArrayList<>(predecesseursListe());
        l.remove(de);
        if (present) {
            l.add(de);
        }
        this.predecesseurs = JsonTexte.ecrire(l);
    }

    /** Retire un code de la liste des prédécesseurs (opération supprimée). */
    public void retirerPredecesseur(String de) {
        if (predecesseursListe().contains(de)) {
            basculerPredecesseur(de, false);
        }
    }

    public void placer(Position p) {
        this.position = JsonTexte.ecrire(p);
    }

    public NoeudReferentiel noeud() {
        return new NoeudReferentiel(
            code,
            libelle,
            segment,
            predecesseursListe(),
            new NoeudReferentiel.Modele(
                responsable != null ? responsable : fonctionResponsableConnue(),
                validateur,
                gesteEffectif(),
                jalon,
                declencheur,
                secret,
                valeurReference,
                getLivrables().stream().map(Livrable::code).toList(),
                delaiEffectif()
            )
        );
    }

    // ------------------------------------------------------------------ lecture du modèle (§1)

    public String getDescription() {
        return description;
    }

    /** Code de l'étape de la version où l'opération est rangée. */
    public String getEtape() {
        return etape;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public Fonction getResponsable() {
        return responsable;
    }

    public Fonction getValidateur() {
        return validateur;
    }

    public List<Fonction> getConsultes() {
        return JsonTexte.liste(consultes, Fonction.class);
    }

    public List<Fonction> getInformes() {
        return JsonTexte.liste(informes, Fonction.class);
    }

    public List<Fonction> getExternes() {
        return JsonTexte.liste(externes, Fonction.class);
    }

    public Integer getDelaiJours() {
        return delaiJours;
    }

    public GrainPreuve getGrain() {
        return grain;
    }

    public PlateformeExecution getPlateforme() {
        return plateforme;
    }

    public boolean isCle() {
        return cle;
    }

    public boolean isSecret() {
        return secret;
    }

    public boolean isOubliSansSymptome() {
        return oubliSansSymptome;
    }

    public boolean isPivot() {
        return pivot;
    }

    public String getValeurReference() {
        return valeurReference;
    }

    public List<Livrable> getLivrables() {
        return JsonTexte.liste(livrables, Livrable.class);
    }

    public Geste getGeste() {
        return geste;
    }

    public Jalon getJalon() {
        return jalon;
    }

    public String getDeclencheur() {
        return declencheur;
    }

    public Position getPosition() {
        return JsonTexte.lire(position, Position.class);
    }

    public Particularite getParticularite() {
        return particularite;
    }

    // ------------------------------------------------------------------ valeurs effectives (version antérieure au modèle)

    /** Attribut historique de la preuve attendue (jeu d'initialisation D-59), lu seulement quand l'étape n'est pas saisie. */
    private Object historique(String cle) {
        return etape == null ? JsonTexte.objet(preuveAttendue).get(cle) : null;
    }

    private Fonction fonctionResponsableConnue() {
        if (fonctionResponsable == null) {
            return null;
        }
        try {
            return Fonction.valueOf(fonctionResponsable.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Étape canonique de la progression (D-89) : celle de l'étape saisie dans sa version (ou de l'étape qui la
     * précède), sinon la phase quand elle nomme une étape, sinon l'étape du segment.
     */
    public EtapeCode etapeEffective() {
        if (etape != null) {
            java.util.Optional<EtapeCode> c = version != null ? version.canoniqueDe(etape) : EtapeCode.de(etape);
            if (c.isPresent()) {
                return c.orElseThrow();
            }
        }
        if (phase != null) {
            try {
                return EtapeCode.valueOf(phase.trim());
            } catch (IllegalArgumentException e) {
                // phase libre d'une version antérieure au modèle
            }
        }
        return segment == null || segment == Segment.TRONC_COMMUN_AMONT ? EtapeCode.CADRAGE : EtapeCode.MISE_EN_PRODUCTION;
    }

    public SegmentParcours segmentEffectif() {
        return segment == null ? SegmentParcours.AMONT : segment.parcours();
    }

    /** Responsable (R) saisi, sinon la fonction responsable historique, sinon selon la partie. */
    public Fonction responsableEffectif() {
        if (responsable != null) {
            return responsable;
        }
        Fonction f = fonctionResponsableConnue();
        if (f != null) {
            return f;
        }
        return partie == Partie.PARTICIPANT ? Fonction.PARTICIPANT : Fonction.CONDUCTEUR;
    }

    /** Validateur (V) saisi, sinon la règle du CR : le conducteur, ou le chef du DES pour le conducteur. */
    public Fonction validateurEffectif() {
        if (validateur != null) {
            return validateur;
        }
        return responsableEffectif() == Fonction.CONDUCTEUR ? Fonction.CHEF_DES : Fonction.CONDUCTEUR;
    }

    /** Informés : saisis, sinon (version antérieure au modèle) le chef de la DDRM. */
    public List<Fonction> informesEffectifs() {
        return etape == null && responsable == null && getInformes().isEmpty() ? List.of(Fonction.CHEF_DDRM) : getInformes();
    }

    public GrainPreuve grainEffectif() {
        if (grain != null) {
            return grain;
        }
        Object g = historique("grain");
        return "PHASE".equals(g) ? GrainPreuve.PHASE : GrainPreuve.OPERATION;
    }

    public PlateformeExecution plateformeEffective() {
        if (plateforme != null) {
            return plateforme;
        }
        Object p = historique("plateforme");
        try {
            return p == null ? null : PlateformeExecution.valueOf(p.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean cleEffective() {
        return cle || Boolean.TRUE.equals(historique("cle"));
    }

    public boolean secretEffectif() {
        return secret || Boolean.TRUE.equals(historique("secret"));
    }

    public boolean pivotEffectif() {
        return pivot || Boolean.TRUE.equals(historique("pivot"));
    }

    public boolean valeurReferenceAttendue() {
        return valeurReference != null || Boolean.TRUE.equals(historique("valeur_reference"));
    }

    public Geste gesteEffectif() {
        if (geste != null) {
            return geste;
        }
        return grainEffectif() == GrainPreuve.PHASE ? Geste.DEPOSER_LIVRABLE : Geste.DEPOSER_PREUVE;
    }

    public int delaiEffectif() {
        return delaiJours == null ? 3 : delaiJours;
    }

    public int ordreEffectif() {
        return ordre == null ? Integer.MAX_VALUE : ordre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceOperation)) {
            return false;
        }
        return getId() != null && getId().equals(((ReferenceOperation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceOperation{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +
            ", phase='" + getPhase() + "'" +
            ", segment='" + getSegment() + "'" +
            ", partie='" + getPartie() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", natureCouverture='" + getNatureCouverture() + "'" +
            ", preuveAttendue='" + getPreuveAttendue() + "'" +
            ", idScenario='" + getIdScenario() + "'" +
            ", predecesseurs='" + getPredecesseurs() + "'" +
            ", fonctionResponsable='" + getFonctionResponsable() + "'" +
            ", grille='" + getGrille() + "'" +
            "}";
    }
}
