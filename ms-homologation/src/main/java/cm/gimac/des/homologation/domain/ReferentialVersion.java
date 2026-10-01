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
    @Enumerated(EnumType.STRING)
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
        return v;
    }

    /** Dérivation : copie profonde des opérations d'une version dans un nouveau brouillon. */
    public static ReferentialVersion deriverDe(ReferentialVersion source, String libelle, Instant instant) {
        ReferentialVersion v = nouveauBrouillon(source.typeProjet, libelle, instant);
        v.versionSourceId = source.id;
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

    public ReferenceOperation upsertOperation(String code, ReferenceOperationData data) {
        assertModifiable();
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

    /** Actions proposées à l'interface (DES-12) selon l'état : l'écran ne recalcule pas la règle. */
    public List<String> actionsPermises() {
        if (etat == EtatVersionReferentiel.BROUILLON) {
            return List.of("EDITER", "CONTROLER", "PUBLIER");
        }
        // Version en vigueur : cible d'une migration des liaisons ouvertes sur une version antérieure (HOM-03).
        return etat == EtatVersionReferentiel.PUBLIEE ? List.of("DERIVER", "MIGRER") : List.of("DERIVER");
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
