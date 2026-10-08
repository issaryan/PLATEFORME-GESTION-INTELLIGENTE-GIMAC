package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import cm.gimac.des.homologation.domain.parcours.CouleurEtape;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.service.NoeudReferentiel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A ReferentialVersion.
 */
@Entity
@Table(name = "referential_version")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferentialVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Convert(converter = ConvertisseursCodes.TypeProjetEnTexte.class)
    @Column(name = "type_projet", nullable = false)
    private TypeProjet typeProjet;

    /** Attribué à la publication ; vide tant que la version est un brouillon. */
    @Column(name = "numero")
    private Integer numero;

    @NotNull
    @Size(max = 200)
    @Column(name = "libelle", length = 200, nullable = false)
    private String libelle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatVersionReferentiel etat;

    @NotNull
    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_publication")
    private Instant datePublication;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ordre_topologique", columnDefinition = "jsonb")
    private String ordreTopologique;

    @Column(name = "version_source_id")
    private UUID versionSourceId;

    /**
     * Opérations de la version. Cascade limitée à la persistance : le CRUD générique reconstruit
     * l'entité sans ses opérations, une suppression des orphelins les effacerait.
     */
    @OneToMany(mappedBy = "version", cascade = CascadeType.PERSIST)
    @OrderBy("code")
    private List<ReferenceOperation> operations = new ArrayList<>();

    /** Pilote du brouillon (D-77, §5) : responsable affiché et notifié (tâche) ; ne verrouille rien (D-89). */
    @Column(name = "pilote_id")
    private UUID piloteId;

    /**
     * Étapes de la version (D-89, §2), dans leur ordre. Cascade limitée à la persistance, comme les opérations :
     * une étape retirée est supprimée explicitement. Vide pour une version créée hors du cycle de vie : les onze
     * étapes canoniques s'appliquent alors (voir {@link #etapes()}).
     */
    @OneToMany(mappedBy = "version", cascade = CascadeType.PERSIST)
    @OrderBy("ordre")
    private List<EtapeReferentiel> etapes = new ArrayList<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReferentialVersion id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeProjet getTypeProjet() {
        return this.typeProjet;
    }

    public ReferentialVersion typeProjet(TypeProjet typeProjet) {
        this.setTypeProjet(typeProjet);
        return this;
    }

    public void setTypeProjet(TypeProjet typeProjet) {
        this.typeProjet = typeProjet;
    }

    public Integer getNumero() {
        return this.numero;
    }

    public ReferentialVersion numero(Integer numero) {
        this.setNumero(numero);
        return this;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getLibelle() {
        return this.libelle;
    }

    public ReferentialVersion libelle(String libelle) {
        this.setLibelle(libelle);
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public EtatVersionReferentiel getEtat() {
        return this.etat;
    }

    public ReferentialVersion etat(EtatVersionReferentiel etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatVersionReferentiel etat) {
        this.etat = etat;
    }

    public Instant getDateCreation() {
        return this.dateCreation;
    }

    public ReferentialVersion dateCreation(Instant dateCreation) {
        this.setDateCreation(dateCreation);
        return this;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Instant getDatePublication() {
        return this.datePublication;
    }

    public ReferentialVersion datePublication(Instant datePublication) {
        this.setDatePublication(datePublication);
        return this;
    }

    public void setDatePublication(Instant datePublication) {
        this.datePublication = datePublication;
    }

    public String getOrdreTopologique() {
        return this.ordreTopologique;
    }

    public ReferentialVersion ordreTopologique(String ordreTopologique) {
        this.setOrdreTopologique(ordreTopologique);
        return this;
    }

    public void setOrdreTopologique(String ordreTopologique) {
        this.ordreTopologique = ordreTopologique;
    }

    public UUID getVersionSourceId() {
        return this.versionSourceId;
    }

    public ReferentialVersion versionSourceId(UUID versionSourceId) {
        this.setVersionSourceId(versionSourceId);
        return this;
    }

    public void setVersionSourceId(UUID versionSourceId) {
        this.versionSourceId = versionSourceId;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    // ------------------------------------------------------------------ HOM-01 : cycle de vie

    public List<ReferenceOperation> getOperations() {
        return operations;
    }

    public static ReferentialVersion nouveauBrouillon(TypeProjet type, String libelle, Instant instant) {
        ReferentialVersion v = new ReferentialVersion();
        v.typeProjet = type;
        v.libelle = libelle;
        v.etat = EtatVersionReferentiel.BROUILLON;
        v.dateCreation = instant;
        for (EtapeCode e : EtapeCode.values()) {
            v.etapes.add(EtapeReferentiel.canonique(v, e));
        }
        return v;
    }

    /** Dérivation : copie profonde des opérations d'une version dans un nouveau brouillon. */
    public static ReferentialVersion deriverDe(ReferentialVersion source, String libelle, Instant instant) {
        ReferentialVersion v = nouveauBrouillon(source.typeProjet, libelle, instant);
        v.versionSourceId = source.id;
        v.etapes.clear();
        source.etapes().forEach(e -> v.etapes.add(e.copierVers(v)));
        source.operations.forEach(op -> v.operations.add(op.copierVers(v)));
        return v;
    }

    /**
     * Brouillon v1 d'un autre type de projet copié d'une version (D-91, départ COPIE) : étapes, opérations, rôles,
     * livrables et signatures ; la version source n'est pas reprise comme origine (autre type).
     */
    public static ReferentialVersion copierVers(ReferentialVersion source, TypeProjet cible, String libelle, Instant instant) {
        ReferentialVersion v = nouveauBrouillon(cible, libelle, instant);
        v.etapes.clear();
        source.etapes().forEach(e -> v.etapes.add(e.copierVers(v)));
        source.operations.forEach(op -> v.operations.add(op.copierVers(v)));
        return v;
    }

    /** Garde unique : seule une version à l'état BROUILLON accepte une écriture. */
    public void assertModifiable() {
        if (etat != EtatVersionReferentiel.BROUILLON) {
            throw HomologationException.conflict(
                "HOM_REFERENTIAL_FROZEN",
                "La version " + numero + " est figée ; un brouillon dérivé doit être créé",
                Map.of(
                    "versionId",
                    String.valueOf(id),
                    "brouillonDerivePropose",
                    Map.of("methode", "POST", "chemin", "/referentiels", "versionSourceId", String.valueOf(id))
                )
            );
        }
    }

    /** Code d'opération (§1) : 1 à 40 caractères, majuscules, chiffres, tiret et souligné, unique dans la version. */
    public static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,39}$");

    public UUID getPiloteId() {
        return piloteId;
    }

    public void designerPilote(UUID compteId) {
        assertModifiable();
        this.piloteId = compteId;
    }

    /** Contrôle de la saisie d'une opération : code bien formé, secret sans valeur de référence (D-59). */
    static void verifierSaisie(String code, ReferenceOperationData data) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw HomologationException.unprocessable("HOM_OPERATION_CODE_INVALID", "Le code d'une opération compte 1 à 40 majuscules, chiffres, tirets ou soulignés.");
        }
        if (Boolean.TRUE.equals(data.secret()) && data.valeurReference() != null && !data.valeurReference().isBlank()) {
            throw HomologationException.unprocessable("HOM_OPERATION_SECRET_VALUE", "Une opération secrète ne relève aucune valeur de référence (code de contrôle seulement).");
        }
        if (data.predecesseurs() != null && data.predecesseurs().contains(code)) {
            throw HomologationException.unprocessable("HOM_OPERATION_SELF_PREDECESSOR", "Une opération ne peut pas se précéder elle-même.");
        }
        verifierSignatures(data.signatures());
    }

    /**
     * Forme des signatures saisies (D-91) : 30 au plus ; code (majuscule puis 1 à 39 majuscules, chiffres ou
     * soulignés), libellé 1..200, signataire, ordre 1..99, 30 livrables portés au plus (422 HOM_SIGNATURE_INVALIDE,
     * {@code details.champ}). L'unicité, le catalogue et les livrables portés sont contrôlés à la publication.
     */
    static void verifierSignatures(List<cm.gimac.des.homologation.domain.parcours.SignatureOperation> signatures) {
        if (signatures == null) {
            return;
        }
        if (signatures.size() > 30) {
            throw signatureInvalide("signatures", "Une opération porte 30 signatures au plus.");
        }
        for (int i = 0; i < signatures.size(); i++) {
            var s = signatures.get(i);
            String champ = "signatures[" + i + "]";
            if (s == null || s.code() == null || !cm.gimac.des.homologation.domain.parcours.SignatureOperation.CODE.matcher(s.code()).matches()) {
                throw signatureInvalide(champ + ".code", "Le code d'une signature compte 2 à 40 majuscules, chiffres ou soulignés et commence par une lettre.");
            }
            if (s.libelle() == null || s.libelle().isBlank() || s.libelle().length() > 200) {
                throw signatureInvalide(champ + ".libelle", "Le libellé d'une signature compte 1 à 200 caractères.");
            }
            if (s.signataire() == null) {
                throw signatureInvalide(champ + ".signataire", "Le signataire d'une signature est une fonction du catalogue.");
            }
            if (s.ordre() != null && (s.ordre() < 1 || s.ordre() > cm.gimac.des.homologation.domain.parcours.SignatureOperation.ORDRE_MAX)) {
                throw signatureInvalide(champ + ".ordre", "L'ordre d'une signature va de 1 à 99.");
            }
            if (s.porte().size() > 30 || s.porte().stream().anyMatch(r -> r == null || r.isBlank() || r.length() > 81)) {
                throw signatureInvalide(champ + ".porte", "Une signature porte au plus 30 livrables, désignés par leur code.");
            }
        }
    }

    private static HomologationException signatureInvalide(String champ, String message) {
        return HomologationException.unprocessableDetails("HOM_SIGNATURE_INVALIDE", message, Map.of("champ", champ));
    }

    public java.util.Optional<ReferenceOperation> operation(String code) {
        return operations.stream().filter(o -> o.getCode().equals(code)).findFirst();
    }

    private ReferenceOperation exiger(String code) {
        return operation(code).orElseThrow(() -> HomologationException.notFound("HOM_OPERATION_NOT_FOUND", "Opération introuvable dans la version.", code));
    }

    /** Création d'une opération au code fourni (modélisateur) ; un code déjà pris est refusé. */
    public ReferenceOperation creerOperation(String code, ReferenceOperationData data) {
        assertModifiable();
        if (operation(code).isPresent()) {
            throw HomologationException.conflict("HOM_OPERATION_CODE_DUPLICATE", "Ce code est déjà pris dans la version.", Map.of("code", String.valueOf(code)));
        }
        return upsertOperation(code, data);
    }

    /** Retrait d'une opération : son code disparaît aussi des prédécesseurs des autres opérations. */
    public ReferenceOperation retirerOperation(String code) {
        assertModifiable();
        ReferenceOperation op = exiger(code);
        operations.remove(op);
        operations.forEach(o -> o.retirerPredecesseur(code));
        return op;
    }

    /** Placement des opérations dans le canevas du modélisateur. */
    public void placer(Map<String, cm.gimac.des.homologation.domain.parcours.Position> positions) {
        assertModifiable();
        positions.forEach((code, p) -> exiger(code).placer(p));
    }

    /** Ajout ou retrait d'un arc : {@code de} devient (ou cesse d'être) prédécesseur de {@code vers}. */
    public ReferenceOperation arc(String de, String vers, boolean present) {
        assertModifiable();
        ReferenceOperation cible = exiger(vers);
        exiger(de);
        if (de.equals(vers)) {
            throw HomologationException.unprocessable("HOM_OPERATION_SELF_PREDECESSOR", "Une opération ne peut pas se précéder elle-même.");
        }
        cible.basculerPredecesseur(de, present);
        return cible;
    }

    public ReferenceOperation upsertOperation(String code, ReferenceOperationData data) {
        assertModifiable();
        verifierSaisie(code, data);
        if (data.etape() != null && etape(data.etape()).isEmpty()) {
            throw HomologationException.unprocessable("HOM_ETAPE_INCONNUE", "L'étape « " + data.etape() + " » n'existe pas dans cette version du référentiel.");
        }
        ReferenceOperation op = operations
            .stream()
            .filter(o -> o.getCode().equals(code))
            .findFirst()
            .orElseGet(() -> {
                ReferenceOperation n = ReferenceOperation.nouvelle(this, code);
                operations.add(n);
                return n;
            });
        op.appliquer(data);
        return op;
    }

    /** Fige la version : numéro, date et ordre topologique calculé par le contrôle de HOM-02. */
    public void publier(int numeroAttribue, Instant instant, List<String> ordre) {
        assertModifiable();
        this.numero = numeroAttribue;
        this.datePublication = instant;
        this.ordreTopologique = JsonTexte.ecrire(List.copyOf(ordre));
        this.etat = EtatVersionReferentiel.PUBLIEE;
    }

    public void retirer() {
        if (etat == EtatVersionReferentiel.PUBLIEE) {
            etat = EtatVersionReferentiel.RETIREE;
        }
    }

    public List<NoeudReferentiel> noeuds() {
        return operations.stream().map(ReferenceOperation::noeud).toList();
    }

    public List<String> ordreTopologiqueListe() {
        return JsonTexte.listeDeTextes(ordreTopologique);
    }

    /**
     * Actions proposées à l'interface (DES-12) selon l'état : l'écran ne recalcule pas la règle. Un brouillon est
     * modifiable par tout éditeur, pilote ou non (D-89) : {@code MODIFIER} est le nom lu par l'interface (descripteur
     * DES-12 et modélisateur), {@code EDITER} celui du plan (PI-GIMAC-01 HOM-01), conservé.
     */
    public List<String> actionsPermises() {
        if (etat == EtatVersionReferentiel.BROUILLON) {
            return List.of("MODIFIER", "EDITER", "CONTROLER", "PUBLIER");
        }
        // Version en vigueur : cible d'une migration des liaisons ouvertes sur une version antérieure (HOM-03).
        return etat == EtatVersionReferentiel.PUBLIEE ? List.of("DERIVER", "MIGRER") : List.of("DERIVER");
    }

    // ------------------------------------------------------------------ D-89 : étapes configurables

    /** Étapes de la version dans leur ordre ; à défaut d'étapes enregistrées, les onze étapes canoniques. */
    public List<EtapeReferentiel> etapes() {
        if (etapes.isEmpty()) {
            return java.util.Arrays.stream(EtapeCode.values()).map(e -> EtapeReferentiel.canonique(this, e)).toList();
        }
        return etapes.stream().sorted(java.util.Comparator.comparingInt(EtapeReferentiel::getOrdre)).toList();
    }

    public java.util.Optional<EtapeReferentiel> etape(String code) {
        return etapes().stream().filter(e -> e.getCode().equals(code)).findFirst();
    }

    /**
     * Étape canonique d'une étape de la version, pour la progression des liaisons : la sienne, sinon celle de
     * l'étape qui la précède ; une étape placée avant le cadrage sans canonique est rattachée à la première étape
     * canonique instanciée qui la suit (les étapes 1 et 2 ne sont jamais instanciées). Code inconnu de la
     * version : l'étape canonique de ce nom, le cas échéant.
     */
    public java.util.Optional<EtapeCode> canoniqueDe(String code) {
        List<EtapeReferentiel> l = etapes();
        int i = 0;
        while (i < l.size() && !l.get(i).getCode().equals(code)) {
            i++;
        }
        if (i == l.size()) {
            return EtapeCode.de(code);
        }
        if (l.get(i).getCanonique() != null) {
            return java.util.Optional.of(l.get(i).getCanonique());
        }
        for (int j = i - 1; j >= 0; j--) {
            EtapeCode c = l.get(j).getCanonique();
            if (c != null) {
                if (!c.horsParcours()) {
                    return java.util.Optional.of(c);
                }
                break;
            }
        }
        for (int j = i + 1; j < l.size(); j++) {
            EtapeCode c = l.get(j).getCanonique();
            if (c != null && !c.horsParcours()) {
                return java.util.Optional.of(c);
            }
        }
        return java.util.Optional.of(EtapeCode.CADRAGE);
    }

    /** Opérations de la version rangées dans une étape. */
    public long nombreOperations(String etapeCode) {
        return operations.stream().filter(o -> etapeCode.equals(o.getEtape())).count();
    }

    /** Les étapes canoniques deviennent des données de la version avant sa première modification. */
    private void materialiserEtapes() {
        if (etapes.isEmpty()) {
            etapes.addAll(etapes());
        }
    }

    private EtapeReferentiel exigerEtape(String code) {
        return etape(code).orElseThrow(() -> HomologationException.notFound("HOM_ETAPE_NOT_FOUND", "Étape introuvable dans la version.", code));
    }

    private static String libelleEtape(String libelle) {
        String l = libelle == null ? "" : libelle.trim();
        if (l.isEmpty() || l.length() > 120) {
            throw HomologationException.unprocessable("HOM_ETAPE_LIBELLE_INVALIDE", "Le libellé d'une étape compte 1 à 120 caractères.");
        }
        return l;
    }

    /** Ajout d'une étape à la position {@code ordre} (1 = première ; absente ou hors bornes : à la fin). */
    public EtapeReferentiel ajouterEtape(String code, String libelle, CouleurEtape couleur, EtapeCode canonique, Integer ordre) {
        assertModifiable();
        if (code == null || !EtapeReferentiel.CODE.matcher(code).matches()) {
            throw HomologationException.unprocessable("HOM_ETAPE_CODE_INVALIDE", "Le code d'une étape compte 2 à 40 majuscules, chiffres ou soulignés et commence par une lettre.");
        }
        materialiserEtapes();
        if (etape(code).isPresent()) {
            throw HomologationException.conflict("HOM_ETAPE_CODE_DUPLIQUE", "Ce code d'étape est déjà pris dans la version.", Map.of("code", code));
        }
        List<EtapeReferentiel> l = new ArrayList<>(etapes());
        int position = ordre == null || ordre < 1 || ordre > l.size() ? l.size() : ordre - 1;
        EtapeReferentiel e = new EtapeReferentiel(this, code, libelleEtape(libelle), position + 1, couleur == null ? CouleurEtape.NEUTRE : couleur, canonique);
        l.add(position, e);
        etapes.add(e);
        renumeroter(l);
        return e;
    }

    /** Renommage, couleur et canonique d'une étape ; une étape protégée garde sa canonique (409 HOM_ETAPE_CANONIQUE). */
    public EtapeReferentiel modifierEtape(String code, String libelle, CouleurEtape couleur, EtapeCode canonique) {
        assertModifiable();
        materialiserEtapes();
        EtapeReferentiel e = exigerEtape(code);
        if (e.protegee() && canonique != e.getCanonique()) {
            throw etapeCanonique(e);
        }
        e.modifier(libelleEtape(libelle), couleur == null ? e.getCouleur() : couleur, canonique);
        return e;
    }

    /**
     * Retrait d'une étape vide (409 HOM_ETAPE_NON_VIDE sinon) qui n'est pas liée à une étape canonique porteuse
     * d'un jalon ou du verrou 2 (409 HOM_ETAPE_CANONIQUE) ; rend l'étape retirée, à supprimer.
     */
    public EtapeReferentiel retirerEtape(String code) {
        assertModifiable();
        materialiserEtapes();
        EtapeReferentiel e = exigerEtape(code);
        if (e.protegee()) {
            throw etapeCanonique(e);
        }
        long n = nombreOperations(code);
        if (n > 0) {
            throw HomologationException.conflict(
                "HOM_ETAPE_NON_VIDE",
                "L'étape « " + e.getLibelle() + " » contient " + n + " opération(s) : déplacez-les avant de la supprimer.",
                Map.of("code", code, "operations", n)
            );
        }
        etapes.remove(e);
        renumeroter(etapes());
        return e;
    }

    /** Nouvel ordre des étapes : permutation exacte des codes de la version (422 HOM_ETAPE_ORDRE_INVALIDE sinon). */
    public List<EtapeReferentiel> ordonnerEtapes(List<String> codes) {
        assertModifiable();
        materialiserEtapes();
        List<EtapeReferentiel> actuelles = etapes();
        if (codes == null || codes.size() != actuelles.size() || !new java.util.HashSet<>(codes).equals(actuelles.stream().map(EtapeReferentiel::getCode).collect(java.util.stream.Collectors.toSet()))) {
            throw HomologationException.unprocessable("HOM_ETAPE_ORDRE_INVALIDE", "Le nouvel ordre doit citer chaque étape de la version une fois et une seule.");
        }
        renumeroter(codes.stream().map(this::exigerEtape).toList());
        return etapes();
    }

    private static void renumeroter(List<EtapeReferentiel> ordonnees) {
        for (int i = 0; i < ordonnees.size(); i++) {
            if (ordonnees.get(i).getOrdre() != i + 1) {
                ordonnees.get(i).ordonner(i + 1);
            }
        }
    }

    private static HomologationException etapeCanonique(EtapeReferentiel e) {
        return HomologationException.conflict(
            "HOM_ETAPE_CANONIQUE",
            "L'étape « " + e.getLibelle() + " » porte un jalon ou le verrou 2 du parcours : elle ne se supprime pas et garde son étape canonique.",
            Map.of("code", e.getCode(), "canonique", e.getCanonique().name())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferentialVersion)) {
            return false;
        }
        return getId() != null && getId().equals(((ReferentialVersion) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferentialVersion{" +
            "id=" + getId() +
            ", typeProjet='" + getTypeProjet() + "'" +
            ", numero=" + getNumero() +
            ", libelle='" + getLibelle() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateCreation='" + getDateCreation() + "'" +
            ", datePublication='" + getDatePublication() + "'" +
            ", ordreTopologique='" + getOrdreTopologique() + "'" +
            ", versionSourceId='" + getVersionSourceId() + "'" +
            "}";
    }
}
