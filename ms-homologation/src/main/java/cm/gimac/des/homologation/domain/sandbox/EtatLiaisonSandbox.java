package cm.gimac.des.homologation.domain.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Dernier état d'une liaison publié pour la Sandbox GIMAC (US-EXI-16) : statut vu par la sandbox, version d'état
 * strictement croissante, code du participant. Une publication n'a lieu que si le statut change ; la version n'est
 * jamais réutilisée (la ligne est lue sous verrou avant d'être incrémentée).
 */
@Entity
@Table(name = "liaison_sandbox")
public class EtatLiaisonSandbox {

    @Id
    @Column(name = "liaison_id")
    private UUID liaisonId;

    @Column(name = "code_participant", length = 20)
    private String codeParticipant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 10, nullable = false)
    private StatutSandbox statut;

    @Column(name = "version_etat", nullable = false)
    private long versionEtat;

    @Column(name = "publie_le", nullable = false)
    private Instant publieLe;

    protected EtatLiaisonSandbox() {}

    /** Première publication d'une liaison : version 1. */
    public EtatLiaisonSandbox(UUID liaisonId, String codeParticipant, StatutSandbox statut, Instant t) {
        this.liaisonId = Objects.requireNonNull(liaisonId, "liaisonId");
        this.codeParticipant = codeParticipant;
        this.statut = Objects.requireNonNull(statut, "statut");
        this.versionEtat = 1;
        this.publieLe = t;
    }

    /** Vrai si le statut change : la version d'état est alors incrémentée et la date de publication posée. */
    public boolean constater(StatutSandbox nouveau, Instant t) {
        if (nouveau == statut) {
            return false;
        }
        statut = nouveau;
        versionEtat++;
        publieLe = t;
        return true;
    }

    /** Code connu ensuite (liaison antérieure à US-EXI-16, registre relu) : jamais remplacé une fois connu. */
    public void completerCode(String code) {
        if (codeParticipant == null) {
            codeParticipant = code;
        }
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getCodeParticipant() {
        return codeParticipant;
    }

    public StatutSandbox getStatut() {
        return statut;
    }

    public long getVersionEtat() {
        return versionEtat;
    }

    public Instant getPublieLe() {
        return publieLe;
    }
}
