package cm.gimac.des.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Métadonnées d'un fichier déposé par POST /api/v1/documents (dépôt direct de la tranche DDRM)
 * ou par POST /api/v1/interne/documents (dépôt par un service pour le compte d'un utilisateur).
 * Le contenu est dans le dépôt objet WORM (D-50) sous {@code cleObjet} ; la base n'en garde que
 * l'empreinte SHA-256 calculée par le serveur, la taille, le type et la finalité.
 *
 * Table dédiée plutôt que l'entité Document du JDL : celle-ci suppose la chaîne complète de
 * l'algorithme 6.17 (catalogue des types de pièces, lignée, antivirus, rétention copiée de la
 * politique), qui n'est pas encore écrite. Les lignes sont en insertion seule : un déclencheur
 * PostgreSQL refuse toute modification et toute suppression (RG-04, RG-16).
 */
@Entity
@Table(name = "depot_document")
public class DepotDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "finalite", length = 50, nullable = false, updatable = false)
    private String finalite;

    @Column(name = "reference", length = 255, nullable = false, updatable = false)
    private String reference;

    @Column(name = "nom_fichier", length = 255, nullable = false, updatable = false)
    private String nomFichier;

    @Column(name = "taille", nullable = false, updatable = false)
    private long taille;

    @Column(name = "type_mime", length = 100, nullable = false, updatable = false)
    private String typeMime;

    @Column(name = "sha256", length = 64, nullable = false, updatable = false)
    private String sha256;

    /** Clé de l'objet WORM ; plusieurs documents de même contenu peuvent la partager (déduplication, D-74). */
    @Column(name = "cle_objet", length = 255, nullable = false, updatable = false)
    private String cleObjet;

    @Column(name = "version_objet", length = 255, updatable = false)
    private String versionObjet;

    @Column(name = "deposant", length = 255, nullable = false, updatable = false)
    private String deposant;

    @Column(name = "institution_id", length = 64, updatable = false)
    private String institutionId;

    /** Client du service qui a déposé pour le compte de l'utilisateur (interface interne), sinon absent. */
    @Column(name = "service_deposant", length = 100, updatable = false)
    private String serviceDeposant;

    @Column(name = "depose_le", nullable = false, updatable = false)
    private Instant deposeLe;

    public UUID getId() {
        return id;
    }

    public DepotDocument id(UUID id) {
        this.id = id;
        return this;
    }

    public String getFinalite() {
        return finalite;
    }

    public DepotDocument finalite(String finalite) {
        this.finalite = finalite;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public DepotDocument reference(String reference) {
        this.reference = reference;
        return this;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public DepotDocument nomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
        return this;
    }

    public long getTaille() {
        return taille;
    }

    public DepotDocument taille(long taille) {
        this.taille = taille;
        return this;
    }

    public String getTypeMime() {
        return typeMime;
    }

    public DepotDocument typeMime(String typeMime) {
        this.typeMime = typeMime;
        return this;
    }

    public String getSha256() {
        return sha256;
    }

    public DepotDocument sha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }

    public String getCleObjet() {
        return cleObjet;
    }

    public DepotDocument cleObjet(String cleObjet) {
        this.cleObjet = cleObjet;
        return this;
    }

    public String getVersionObjet() {
        return versionObjet;
    }

    public DepotDocument versionObjet(String versionObjet) {
        this.versionObjet = versionObjet;
        return this;
    }

    public String getDeposant() {
        return deposant;
    }

    public DepotDocument deposant(String deposant) {
        this.deposant = deposant;
        return this;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public DepotDocument institutionId(String institutionId) {
        this.institutionId = institutionId;
        return this;
    }

    public String getServiceDeposant() {
        return serviceDeposant;
    }

    public DepotDocument serviceDeposant(String serviceDeposant) {
        this.serviceDeposant = serviceDeposant;
        return this;
    }

    public Instant getDeposeLe() {
        return deposeLe;
    }

    public DepotDocument deposeLe(Instant deposeLe) {
        this.deposeLe = deposeLe;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof DepotDocument autre && id != null && id.equals(autre.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DepotDocument{id=" + id + ", finalite=" + finalite + ", taille=" + taille + ", typeMime=" + typeMime + ", sha256=" + sha256 + "}";
    }
}
