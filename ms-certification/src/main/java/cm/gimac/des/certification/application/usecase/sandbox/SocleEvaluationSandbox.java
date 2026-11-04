package cm.gimac.des.certification.application.usecase.sandbox;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import cm.gimac.des.certification.domain.enumeration.EtatQuitus;
import cm.gimac.des.certification.domain.enumeration.NatureQuitus;
import cm.gimac.des.certification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.certification.infrastructure.persistence.CircuitsLiaisonRepository;
import cm.gimac.des.certification.infrastructure.persistence.QuitusCircuitRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-EXI-25 : rattache le bilan Sandbox repris au circuit de certification existant. Une même empreinte est sans
 * effet ; une nouvelle empreinte ouvre le cycle suivant et rend caducs les seuls quitus favorables du cycle remplacé.
 */
@Service
@Transactional
public class SocleEvaluationSandbox {

    public static final String QUITUS_CADUCS = "certification.quitus-lapsed.v1";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    public enum Issue {
        OUVERT,
        CONSERVE,
        SANS_EMPREINTE,
    }

    public record QuitusCaducs(
        UUID circuitId,
        UUID liaisonId,
        Integer cycle,
        List<NatureQuitus> natures,
        String motif,
        Instant caducLe
    ) {}

    private final CircuitsLiaisonRepository circuits;
    private final QuitusCircuitRepository quitus;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public SocleEvaluationSandbox(CircuitsLiaisonRepository circuits, QuitusCircuitRepository quitus, OutboxPublisher outbox, Clock clock) {
        this.circuits = circuits;
        this.quitus = quitus;
        this.outbox = outbox;
        this.clock = clock;
    }

    /** Les événements historiques sans empreinte restent acceptés, sans créer ni modifier de circuit. */
    public Issue reprendre(UUID liaisonId, Integer versionBilan, String empreinteBilan) {
        if (empreinteBilan == null) {
            return Issue.SANS_EMPREINTE;
        }
        if (!SHA256.matcher(empreinteBilan).matches()) {
            throw new IllegalArgumentException("empreinteBilan doit être un SHA-256 hexadécimal de 64 caractères");
        }
        String empreinte = empreinteBilan.toLowerCase(java.util.Locale.ROOT);
        var precedent = circuits.findFirstByLiaisonIdOrderByCycleDesc(liaisonId);
        if (precedent.map(p -> p.getSocleEmpreinte().equalsIgnoreCase(empreinte)).orElse(false)) {
            return Issue.CONSERVE;
        }

        Instant maintenant = clock.instant();
        CertificationCircuit suivant = new CertificationCircuit()
            .liaisonId(liaisonId)
            .cycle(precedent.map(p -> p.getCycle() + 1).orElse(versionBilan == null ? 1 : versionBilan))
            .socleEmpreinte(empreinte)
            .etat(EtatCircuit.OUVERT)
            .dateOuverture(maintenant);
        suivant = circuits.save(suivant);
        for (NatureQuitus nature : NatureQuitus.values()) {
            quitus.save(
                new Quitus()
                    .circuit(suivant)
                    .nature(nature)
                    .etat(nature == NatureQuitus.TECHNIQUE ? EtatQuitus.ATTENDU : EtatQuitus.NON_REQUIS)
                    .dateAttendu(nature == NatureQuitus.TECHNIQUE ? maintenant : null)
            );
        }
        precedent.ifPresent(p -> remplacer(p, empreinte, maintenant));
        return Issue.OUVERT;
    }

    private void remplacer(CertificationCircuit precedent, String empreinte, Instant maintenant) {
        String motif = "Socle d'évaluation modifié : " + precedent.getSocleEmpreinte() + " remplacé par " + empreinte;
        List<Quitus> favorables = quitus
            .findByCircuit_Id(precedent.getId())
            .stream()
            .filter(q -> q.getEtat() == EtatQuitus.SIGNE_FAVORABLE)
            .toList();
        favorables.forEach(q -> {
            q.setEtat(EtatQuitus.CADUC);
            q.setMotifCaducite(motif);
        });
        quitus.saveAll(favorables);
        precedent.setEtat(EtatCircuit.INTERROMPU);
        precedent.setDateFin(maintenant);
        circuits.save(precedent);
        if (!favorables.isEmpty()) {
            outbox.publish(
                "Quitus",
                precedent.getId(),
                QUITUS_CADUCS,
                new QuitusCaducs(
                    precedent.getId(),
                    precedent.getLiaisonId(),
                    precedent.getCycle(),
                    favorables.stream().map(Quitus::getNature).toList(),
                    motif,
                    maintenant
                )
            );
        }
    }
}
