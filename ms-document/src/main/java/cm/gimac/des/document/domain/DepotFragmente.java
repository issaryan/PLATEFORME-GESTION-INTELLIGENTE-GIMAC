package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dépôt fragmenté (claim check, D-74, docs/lot1/depots-fragmentes.md) : fichier de plusieurs
 * gigaoctets envoyé par parties de {@code taillePartie} octets dans un téléversement multipart du
 * compartiment des dépôts en cours ({@code envoi}, clé {@code cleDepot}), puis vérifié par le
 * serveur et conservé dans le dépôt WORM sous l'identifiant {@code documentId} (le ticket).
 *
 * Les parties reçues sont gardées avec leur ETag (nécessaire à l'assemblage). Pendant la
 * vérification, {@code cleObjet} et {@code envoiWorm} désignent le téléversement en cours vers le
 * dépôt WORM, pour l'abandonner si la vérification reprend après un arrêt ; {@code repriseLe} est
 * l'instant à partir duquel la tâche de vérification peut (re)prendre le dépôt. La version sert au
 * verrouillage optimiste entre plusieurs instances du service.
 */
@Entity
@Table(name = "depot_fragmente")
public class DepotFragmente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 20, nullable = false)
    private EtatDepotFragmente etat;

    @Column(name = "finalite", length = 50, nullable = false, updatable = false)
    private String finalite;

    @Column(name = "reference", length = 255, nullable = false, updatable = false)
    private String reference;

    @Column(name = "nom_fichier", length = 255, nullable = false, updatable = false)
    private String nomFichier;

    @Column(name = "type_mime", length = 100, nullable = false)
    private String typeMime;

    @Column(name = "taille", nullable = false, updatable = false)
    private long taille;

    @Column(name = "taille_partie", nullable = false, updatable = false)
    private long taillePartie;

    @Column(name = "nombre_parties", nullable = false, updatable = false)
    private int nombreParties;

    /** Parties reçues : numéro (à partir de 1) et ETag rendu par le dépôt objet. */
    @ElementCollection
    @CollectionTable(name = "depot_fragmente_partie", joinColumns = @JoinColumn(name = "depot_id"))
    @MapKeyColumn(name = "numero")
    @Column(name = "etag", length = 100, nullable = false)
    private Map<Integer, String> parties = new HashMap<>();

    /** Identifiant du téléversement multipart dans le compartiment des dépôts en cours. */
    @Column(name = "envoi", length = 1024, nullable = false, updatable = false)
    private String envoi;

    @Column(name = "cle_depot", length = 255, nullable = false, updatable = false)
    private String cleDepot;

    @Column(name = "deposant", length = 255, nullable = false, updatable = false)
    private String deposant;

    @Column(name = "institution_id", length = 64, updatable = false)
    private String institutionId;

    @Column(name = "ouvert_le", nullable = false, updatable = false)
    private Instant ouvertLe;

    @Column(name = "actif_le", nullable = false)
    private Instant actifLe;

    @Column(name = "reprise_le")
    private Instant repriseLe;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "cle_objet", length = 255)
    private String cleObjet;

    @Column(name = "envoi_worm", length = 1024)
    private String envoiWorm;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "dedoublonne", nullable = false)
    private boolean dedoublonne;

    @Column(name = "motif", length = 50)
    private String motif;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Taille attendue de la partie {@code numero} : la taille d'une partie, ou le reste pour la dernière. */
    public long tailleAttendue(int numero) {
        return numero < nombreParties ? taillePartie : taille - (long) (nombreParties - 1) * taillePartie;
    }

    /** Numéros des parties reçues, en ordre croissant. */
    public List<Integer> partiesRecues() {
        return parties.keySet().stream().sorted().toList();
    }

    /** Numéros des parties encore attendues, en ordre croissant. */
    public List<Integer> partiesManquantes() {
        List<Integer> manquantes = new ArrayList<>();
        for (int n = 1; n <= nombreParties; n++) {
            if (!parties.containsKey(n)) {
                manquantes.add(n);
            }
        }
        return manquantes;
    }

    public UUID getId() {
        return id;
    }

    public DepotFragmente id(UUID id) {
        this.id = id;
        return this;
    }

    public EtatDepotFragmente getEtat() {
        return etat;
    }

    public DepotFragmente etat(EtatDepotFragmente etat) {
        this.etat = etat;
        return this;
    }

    public String getFinalite() {
        return finalite;
    }

    public DepotFragmente finalite(String finalite) {
        this.finalite = finalite;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public DepotFragmente reference(String reference) {
        this.reference = reference;
        return this;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public DepotFragmente nomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
        return this;
    }

    public String getTypeMime() {
        return typeMime;
    }

    public DepotFragmente typeMime(String typeMime) {
        this.typeMime = typeMime;
        return this;
    }

    public long getTaille() {
        return taille;
    }

    public DepotFragmente taille(long taille) {
        this.taille = taille;
        return this;
    }

    public long getTaillePartie() {
        return taillePartie;
    }

    public DepotFragmente taillePartie(long taillePartie) {
        this.taillePartie = taillePartie;
        return this;
    }

    public int getNombreParties() {
        return nombreParties;
    }

    public DepotFragmente nombreParties(int nombreParties) {
        this.nombreParties = nombreParties;
        return this;
    }

    public Map<Integer, String> getParties() {
        return parties;
    }

    public String getEnvoi() {
        return envoi;
    }

    public DepotFragmente envoi(String envoi) {
        this.envoi = envoi;
        return this;
    }

    public String getCleDepot() {
        return cleDepot;
    }

    public DepotFragmente cleDepot(String cleDepot) {
        this.cleDepot = cleDepot;
        return this;
    }

    public String getDeposant() {
        return deposant;
    }

    public DepotFragmente deposant(String deposant) {
        this.deposant = deposant;
        return this;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public DepotFragmente institutionId(String institutionId) {
        this.institutionId = institutionId;
        return this;
    }

    public Instant getOuvertLe() {
        return ouvertLe;
    }

    public DepotFragmente ouvertLe(Instant ouvertLe) {
        this.ouvertLe = ouvertLe;
        return this;
    }

    public Instant getActifLe() {
        return actifLe;
    }

    public DepotFragmente actifLe(Instant actifLe) {
        this.actifLe = actifLe;
        return this;
    }

    public Instant getRepriseLe() {
        return repriseLe;
    }

    public DepotFragmente repriseLe(Instant repriseLe) {
        this.repriseLe = repriseLe;
        return this;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public DepotFragmente documentId(UUID documentId) {
        this.documentId = documentId;
        return this;
    }

    public String getCleObjet() {
        return cleObjet;
    }

    public DepotFragmente cleObjet(String cleObjet) {
        this.cleObjet = cleObjet;
        return this;
    }

    public String getEnvoiWorm() {
        return envoiWorm;
    }

    public DepotFragmente envoiWorm(String envoiWorm) {
        this.envoiWorm = envoiWorm;
        return this;
    }

    public String getSha256() {
        return sha256;
    }

    public DepotFragmente sha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }

    public boolean isDedoublonne() {
        return dedoublonne;
    }

    public DepotFragmente dedoublonne(boolean dedoublonne) {
        this.dedoublonne = dedoublonne;
        return this;
    }

    public String getMotif() {
        return motif;
    }

    public DepotFragmente motif(String motif) {
        this.motif = motif;
        return this;
    }

    /** Version du verrouillage optimiste ; absente tant que le dépôt n'est pas inscrit. */
    public Long getVersion() {
        return version;
    }

    /** Version connue (tests et reprise d'un état lu ailleurs) ; gérée par JPA en temps normal. */
    public DepotFragmente version(Long version) {
        this.version = version;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof DepotFragmente autre && id != null && id.equals(autre.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DepotFragmente{id=" + id + ", etat=" + etat + ", finalite=" + finalite + ", taille=" + taille + ", parties=" + parties.size() + "/" + nombreParties + "}";
    }
}
