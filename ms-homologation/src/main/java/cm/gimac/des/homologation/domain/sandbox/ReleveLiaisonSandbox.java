package cm.gimac.des.homologation.domain.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * État de la relève des résultats de la sandbox pour une liaison (US-EXI-14, US-EXI-17) : dernière relève réussie,
 * dernier échec et son motif, date avant laquelle la relève ne reprend pas (délai indicatif Retry-After de SBX-LECTURE).
 */
@Entity
@Table(name = "sbx_releve_liaison")
public class ReleveLiaisonSandbox {

    @Id
    @Column(name = "liaison_id")
    private UUID liaisonId;

    @Column(name = "derniere_releve_reussie")
    private Instant derniereReleveReussie;

    @Column(name = "dernier_echec")
    private Instant dernierEchec;

    @Column(name = "motif_echec", length = 300)
    private String motifEchec;

    @Column(name = "reprendre_apres")
    private Instant reprendreApres;

    protected ReleveLiaisonSandbox() {}

    public ReleveLiaisonSandbox(UUID liaisonId) {
        this.liaisonId = Objects.requireNonNull(liaisonId, "liaisonId");
    }

    /** La relève est reportée tant que le délai indicatif de la dernière indisponibilité n'est pas écoulé. */
    public boolean reportee(Instant t) {
        return reprendreApres != null && t.isBefore(reprendreApres);
    }

    public void reussie(Instant t) {
        derniereReleveReussie = t;
        reprendreApres = null;
    }

    public void echec(Instant t, String motif, Duration delai) {
        dernierEchec = t;
        motifEchec = motif == null ? null : motif.substring(0, Math.min(motif.length(), 300));
        reprendreApres = delai == null ? null : t.plus(delai);
    }

    /** Vrai si la dernière relève a échoué (aucune réussite depuis le dernier échec). */
    public boolean enEchec() {
        return dernierEchec != null && (derniereReleveReussie == null || dernierEchec.isAfter(derniereReleveReussie));
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public Instant getDerniereReleveReussie() {
        return derniereReleveReussie;
    }

    public Instant getDernierEchec() {
        return dernierEchec;
    }

    public String getMotifEchec() {
        return motifEchec;
    }

    public Instant getReprendreApres() {
        return reprendreApres;
    }
}
