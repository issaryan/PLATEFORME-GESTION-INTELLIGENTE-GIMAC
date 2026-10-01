package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Jeton d'un lien envoyé par courriel (D-65) : seule son empreinte SHA-256 est conservée, avec sa
 * finalité (INVITATION, REINITIALISATION), le compte, l'échéance et la date d'usage (usage unique).
 */
@Entity
@Table(name = "jeton_lien")
public class JetonLien {

    @Id
    private UUID id;

    @Column(name = "empreinte", nullable = false, length = 64, unique = true)
    private String empreinte;

    @Column(name = "finalite", nullable = false, length = 20)
    private String finalite;

    @Column(name = "compte_id", nullable = false, length = 100)
    private String compteId;

    @Column(name = "couche", nullable = false, length = 20)
    private String couche;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "utilise_le")
    private Instant utiliseLe;

    protected JetonLien() {}

    public JetonLien(String empreinte, String finalite, String compteId, String couche, Instant creeLe, Instant expireLe) {
        this.id = UUID.randomUUID();
        this.empreinte = empreinte;
        this.finalite = finalite;
        this.compteId = compteId;
        this.couche = couche;
        this.creeLe = creeLe;
        this.expireLe = expireLe;
    }

    public boolean utilisable(Instant maintenant) {
        return utiliseLe == null && maintenant.isBefore(expireLe);
    }

    /** Usage unique : le jeton ne sert plus, qu'il ait été suivi ou remplacé. */
    public void utiliser(Instant maintenant) {
        if (utiliseLe == null) {
            utiliseLe = maintenant;
        }
    }

    public UUID getId() { return id; }
    public String getEmpreinte() { return empreinte; }
    public String getFinalite() { return finalite; }
    public String getCompteId() { return compteId; }
    public String getCouche() { return couche; }
    public Instant getCreeLe() { return creeLe; }
    public Instant getExpireLe() { return expireLe; }
    public Instant getUtiliseLe() { return utiliseLe; }
}
