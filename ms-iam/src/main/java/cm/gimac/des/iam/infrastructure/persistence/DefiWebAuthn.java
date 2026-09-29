package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Défi WebAuthn à usage unique (enrôlement ou réauthentification), lié à un sujet et à une
 * finalité, de courte durée. Il est consommé à la première vérification, réussie ou non.
 */
@Entity
@Table(name = "defi_webauthn")
public class DefiWebAuthn {

    public static final String ENROLEMENT = "ENROLEMENT";

    @Id
    private UUID id;

    @Column(name = "sujet", nullable = false, length = 100)
    private String sujet;

    @Column(name = "finalite", nullable = false, length = 60)
    private String finalite;

    @Column(name = "defi", nullable = false, length = 100)
    private String defi;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "consomme", nullable = false)
    private boolean consomme;

    protected DefiWebAuthn() {}

    public DefiWebAuthn(String sujet, String finalite, String defi, Instant expireLe) {
        this.id = UUID.randomUUID();
        this.sujet = sujet;
        this.finalite = finalite;
        this.defi = defi;
        this.expireLe = expireLe;
    }

    public void consommer() {
        this.consomme = true;
    }

    public boolean utilisable(Instant maintenant) {
        return !consomme && maintenant.isBefore(expireLe);
    }

    public UUID getId() { return id; }
    public String getSujet() { return sujet; }
    public String getFinalite() { return finalite; }
    public String getDefi() { return defi; }
    public Instant getExpireLe() { return expireLe; }
    public boolean isConsomme() { return consomme; }
}
