package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.event.LiaisonStatusChangedPayload;
import cm.gimac.des.homologation.domain.sandbox.EtatLiaisonSandbox;
import cm.gimac.des.homologation.domain.sandbox.StatutSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.EtatLiaisonSandboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publication de l'ouverture et des changements d'état d'une liaison pour la Sandbox GIMAC (US-EXI-16, D-122), par
 * l'outbox, dans la transaction du geste :
 * <ul>
 * <li>ouverture (cadrage, reprise de l'existant, service supplémentaire) : homologation.liaison-opened.v1 complété du
 * code participant et de la version d'état 1, puis homologation.liaison-status-changed.v1 de version 1 ;</li>
 * <li>toute liaison chargée pour une écriture est observée ; avant la validation, si son statut vu par la sandbox
 * (ouverte, gelée, close) a changé depuis la dernière publication, homologation.liaison-status-changed.v1 part avec
 * une version d'état strictement supérieure. Une liaison ouverte avant US-EXI-16 reçoit sa version 1 à sa première
 * écriture.</li>
 * </ul>
 * Aucune écriture de la sandbox vers MS-HOMOLOGATION : la sandbox ne fait que lire ces événements.
 */
@Component
public class PublicationEtatsLiaison {

    static final String AGREGAT = "liaison";
    private static final Logger LOG = LoggerFactory.getLogger(PublicationEtatsLiaison.class);

    private final EtatLiaisonSandboxRepository etats;
    private final OutboxPublisher outbox;
    private final ParticipantDirectoryPort participants;
    private final Clock clock;

    public PublicationEtatsLiaison(EtatLiaisonSandboxRepository etats, OutboxPublisher outbox, ParticipantDirectoryPort participants, Clock clock) {
        this.etats = etats;
        this.outbox = outbox;
        this.participants = participants;
        this.clock = clock;
    }

    /** Liaison ouverte : les deux événements de version 1, puis la liaison est observée jusqu'à la validation. */
    public void ouverture(Liaison l, String codeParticipant, LiaisonOpenedPayload charge) {
        String code = codeParticipant != null ? codeParticipant : code(l);
        EtatLiaisonSandbox e = etats.save(new EtatLiaisonSandbox(l.getId(), code, StatutSandbox.de(l.getEtat()), clock.instant()));
        outbox.publish(AGREGAT, l.getId(), LiaisonOpenedPayload.TYPE, charge.avecEtat(code, e.getVersionEtat()), l.getInstitutionId().toString());
        publier(l, e, null);
        observer(l);
    }

    /**
     * Service supplémentaire (rang 35) : nouvelle liaison proposable aux campagnes de la sandbox, avec le code déjà
     * publié pour la liaison principale (sinon relu au registre).
     */
    public void ouvertureServiceSupplementaire(Liaison n, UUID principale, List<String> perimetre, UUID acteur) {
        String code = etats.findById(principale).map(EtatLiaisonSandbox::getCodeParticipant).orElse(null);
        ReferentialVersion v = n.getVersion();
        ouverture(
            n,
            code,
            new LiaisonOpenedPayload(n.getId(), n.getInstitutionId(), n.getTypeProjet().name(), v.getId(), v.getNumero(), n.getChefDeProjetId(), n.getBanquePartenaireId(), perimetre, acteur, n.modeleTypeProjet().name())
        );
    }

    /** Liaison chargée pour une écriture : son statut est comparé au dernier publié avant la validation. */
    public void observer(Liaison l) {
        if (!TransactionSynchronizationManager.isSynchronizationActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return;
        }
        Synchronisation s = TransactionSynchronizationManager.getSynchronizations()
            .stream()
            .filter(Synchronisation.class::isInstance)
            .map(Synchronisation.class::cast)
            .findFirst()
            .orElseGet(() -> {
                Synchronisation n = new Synchronisation();
                TransactionSynchronizationManager.registerSynchronization(n);
                return n;
            });
        s.liaisons.putIfAbsent(l.getId(), l);
    }

    /** Publie homologation.liaison-status-changed.v1 si le statut a changé ; vrai si un événement est écrit. */
    public boolean constater(Liaison l) {
        StatutSandbox statut = StatutSandbox.de(l.getEtat());
        Instant t = clock.instant();
        Optional<EtatLiaisonSandbox> connu = etats.verrouiller(l.getId());
        if (connu.isEmpty()) {
            publier(l, etats.save(new EtatLiaisonSandbox(l.getId(), code(l), statut, t)), null);
            return true;
        }
        EtatLiaisonSandbox e = connu.orElseThrow();
        StatutSandbox avant = e.getStatut();
        if (!e.constater(statut, t)) {
            return false;
        }
        if (e.getCodeParticipant() == null) {
            e.completerCode(code(l));
        }
        publier(l, e, avant);
        return true;
    }

    /**
     * Republication (exploitation, projection vide d'un consommateur de la sandbox) : l'état courant part de nouveau
     * avec la même version d'état, que les lecteurs à jour ignorent ; un état non encore publié reçoit sa version
     * suivante. Vrai si une nouvelle version a été attribuée.
     */
    public boolean republier(Liaison l) {
        if (constater(l)) {
            return true;
        }
        publier(l, etats.verrouiller(l.getId()).orElseThrow(), null);
        return false;
    }

    private void publier(Liaison l, EtatLiaisonSandbox e, StatutSandbox avant) {
        outbox.publish(
            AGREGAT,
            l.getId(),
            LiaisonStatusChangedPayload.TYPE,
            new LiaisonStatusChangedPayload(
                l.getId(),
                l.getInstitutionId(),
                e.getCodeParticipant(),
                l.getTypeProjet().name(),
                e.getStatut().name(),
                avant == null ? null : avant.name(),
                l.getEtat().name(),
                e.getVersionEtat(),
                e.getPublieLe(),
                l.getReferenceOrigine(),
                l.getInstitutionNom()
            ),
            l.getInstitutionId().toString()
        );
        LOG.info("Liaison {} publiée {} pour la sandbox (version d'état {})", l.getId(), e.getStatut(), e.getVersionEtat());
    }

    /** Code participant relu au registre ; registre injoignable : l'événement part sans code, jamais retenu. */
    private String code(Liaison l) {
        try {
            return participants.getInstitution(l.getInstitutionId()).codeParticipant();
        } catch (RuntimeException ex) {
            LOG.warn("Code participant de l'institution {} non lu : {}", l.getInstitutionId(), ex.toString());
            return null;
        }
    }

    private final class Synchronisation implements TransactionSynchronization {

        private final Map<UUID, Liaison> liaisons = new LinkedHashMap<>();

        @Override
        public void beforeCommit(boolean readOnly) {
            if (!readOnly) {
                liaisons.values().forEach(PublicationEtatsLiaison.this::constater);
            }
        }
    }
}
