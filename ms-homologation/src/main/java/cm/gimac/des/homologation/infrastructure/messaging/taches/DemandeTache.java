package cm.gimac.des.homologation.infrastructure.messaging.taches;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Demande adressée à MS-PILOTAGE (création, clôture, réouverture d'une tâche), écrite dans la transaction du
 * geste métier et relayée de façon asynchrone avec réessai (docs/lot1/modelisateur-processus.md §5) : un échec
 * de MS-PILOTAGE ne bloque jamais le geste. Le numéro d'ordre garantit que les demandes d'une même tâche
 * (origine et responsable) partent dans l'ordre où elles ont été écrites.
 */
@Entity
@Table(name = "demande_tache_pilotage")
public class DemandeTache {

    public enum Nature {
        CREER,
        CLOTURER,
        ROUVRIR,
    }

    public enum Statut {
        EN_ATTENTE,
        ENVOYEE,
        REFUSEE,
        ECHEC,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "numero")
    private Long numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 16)
    private Nature nature;

    /** Clé de la tâche visée : origine.reference et responsable. */
    @Column(name = "cle", nullable = false, length = 300)
    private String cle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "corps", columnDefinition = "jsonb", nullable = false)
    private String corps;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 16)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(name = "tentatives", nullable = false)
    private int tentatives;

    @Column(name = "prochain_essai", nullable = false)
    private Instant prochainEssai;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "traitee_le")
    private Instant traiteeLe;

    @Column(name = "derniere_erreur", length = 500)
    private String derniereErreur;

    protected DemandeTache() {}

    public DemandeTache(Nature nature, String cle, String corps, Instant le) {
        this.nature = nature;
        this.cle = cle;
        this.corps = corps;
        this.creeLe = le;
        this.prochainEssai = le;
    }

    public Long getNumero() {
        return numero;
    }

    public Nature getNature() {
        return nature;
    }

    public String getCle() {
        return cle;
    }

    public String getCorps() {
        return corps;
    }

    public Statut getStatut() {
        return statut;
    }

    public int getTentatives() {
        return tentatives;
    }

    public Instant getProchainEssai() {
        return prochainEssai;
    }

    public Instant getTraiteeLe() {
        return traiteeLe;
    }

    public String getDerniereErreur() {
        return derniereErreur;
    }

    public void traitee(Statut s, Instant le) {
        this.statut = s;
        this.traiteeLe = le;
    }

    /** Échec transitoire : nouvel essai après un délai croissant, abandon (ECHEC) au-delà du nombre maximal. */
    public void echec(String erreur, Instant le, int maximum) {
        tentatives++;
        derniereErreur = erreur == null ? null : erreur.length() > 500 ? erreur.substring(0, 500) : erreur;
        if (tentatives >= maximum) {
            statut = Statut.ECHEC;
            traiteeLe = le;
        } else {
            prochainEssai = le.plusSeconds(Math.min(3600L, 5L * (1L << Math.min(tentatives, 10))));
        }
    }
}
