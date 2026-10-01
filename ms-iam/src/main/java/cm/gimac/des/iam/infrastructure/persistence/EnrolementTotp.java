package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Enrôlement TOTP en cours (D-65) : secret chiffré (AES-256-GCM) proposé à l'utilisateur, gardé 10 min
 * au plus, le temps qu'il saisisse un premier code. Jamais relu en clair hors de la vérification.
 */
@Entity
@Table(name = "enrolement_totp")
public class EnrolementTotp {

    @Id
    private UUID id;

    @Column(name = "sujet", nullable = false, length = 100)
    private String sujet;

    @Column(name = "secret_chiffre", nullable = false, length = 512)
    private String secretChiffre;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    protected EnrolementTotp() {}

    public EnrolementTotp(String sujet, String secretChiffre, Instant creeLe, Instant expireLe) {
        this.id = UUID.randomUUID();
        this.sujet = sujet;
        this.secretChiffre = secretChiffre;
        this.creeLe = creeLe;
        this.expireLe = expireLe;
    }

    public boolean utilisable(String sujet, Instant maintenant) {
        return this.sujet.equals(sujet) && maintenant.isBefore(expireLe);
    }

    public UUID getId() { return id; }
    public String getSujet() { return sujet; }
    public String getSecretChiffre() { return secretChiffre; }
    public Instant getCreeLe() { return creeLe; }
    public Instant getExpireLe() { return expireLe; }
}
