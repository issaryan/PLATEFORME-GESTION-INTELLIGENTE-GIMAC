package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Clé de sécurité FIDO2 enrôlée par un utilisateur (IAM-07) : identifiant de la clé et données
 * publiques attestées (jamais de secret), compteur de signatures contre le clonage.
 */
@Entity
@Table(name = "cle_securite")
public class CleSecurite {

    @Id
    private UUID id;

    @Column(name = "sujet", nullable = false, length = 100)
    private String sujet;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "identifiant_cle", nullable = false, unique = true, length = 1024)
    private String identifiantCle;

    /** AttestedCredentialData sérialisé (clé publique COSE), en base64url. */
    @Column(name = "donnees_attestees", nullable = false, length = 4000)
    private String donneesAttestees;

    @Column(name = "compteur", nullable = false)
    private long compteur;

    @Column(name = "creee_le", nullable = false)
    private Instant creeeLe;

    @Column(name = "derniere_utilisation")
    private Instant derniereUtilisation;

    protected CleSecurite() {}

    public CleSecurite(String sujet, String nom, String identifiantCle, String donneesAttestees, long compteur, Instant creeeLe) {
        this.id = UUID.randomUUID();
        this.sujet = sujet;
        this.nom = nom;
        this.identifiantCle = identifiantCle;
        this.donneesAttestees = donneesAttestees;
        this.compteur = compteur;
        this.creeeLe = creeeLe;
    }

    public void utilisee(long nouveauCompteur, Instant quand) {
        this.compteur = nouveauCompteur;
        this.derniereUtilisation = quand;
    }

    public UUID getId() { return id; }
    public String getSujet() { return sujet; }
    public String getNom() { return nom; }
    public String getIdentifiantCle() { return identifiantCle; }
    public String getDonneesAttestees() { return donneesAttestees; }
    public long getCompteur() { return compteur; }
    public Instant getCreeeLe() { return creeeLe; }
    public Instant getDerniereUtilisation() { return derniereUtilisation; }
}
