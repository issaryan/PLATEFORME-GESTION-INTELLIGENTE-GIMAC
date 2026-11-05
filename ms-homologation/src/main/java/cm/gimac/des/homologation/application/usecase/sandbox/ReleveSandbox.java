package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.CampagneListee;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Enregistrement;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Indisponible;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import cm.gimac.des.homologation.domain.EvaluationBase;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.UnmatchedScenario;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.QualificationScenario;
import cm.gimac.des.homologation.domain.event.CampaignResultImportedPayload;
import cm.gimac.des.homologation.domain.sandbox.CampagneSandbox;
import cm.gimac.des.homologation.domain.sandbox.ExecutionSandbox;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import cm.gimac.des.homologation.domain.sandbox.ReleveLiaisonSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.infrastructure.persistence.EvaluationsLiaisonRepository;
import cm.gimac.des.homologation.repository.CampagneSandboxRepository;
import cm.gimac.des.homologation.repository.ExecutionSandboxRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.PieceSandboxRepository;
import cm.gimac.des.homologation.repository.ReleveLiaisonSandboxRepository;
import cm.gimac.des.homologation.repository.ScenariosSandboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Relève des résultats de la sandbox par le contrat de lecture v1 de SBX-LECTURE (US-EXI-14, algorithme 6.52 côté
 * lecteur) : campagnes d'une liaison, détail et taux, enregistrements d'exécution depuis la dernière position retenue,
 * pièces. Les appels à la sandbox sont faits hors transaction ; chaque page est enregistrée avec la position suivante
 * dans une même transaction, de sorte qu'une indisponibilité (503, 429, injoignable) laisse la position à la dernière
 * page enregistrée : la relève reprend de là, sans perte ni doublon, après le délai indicatif. Pour une exécution, le
 * dernier enregistrement lu (de plus haute position) remplace le précédent ; un scénario absent du référentiel de la
 * liaison est isolé parmi les scénarios non rapprochés. Aucune écriture n'est adressée à la sandbox.
 */
@Service
public class ReleveSandbox {

    static final int TAILLE_PAGE = 40;
    private static final Logger LOG = LoggerFactory.getLogger(ReleveSandbox.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public enum Issue {
        REUSSIE,
        REPORTEE,
        ECHEC,
    }

    /** Bilan d'une relève de liaison : issue, campagnes relues, enregistrements lus. */
    public record Bilan(Issue issue, int campagnes, int enregistrements) {}

    private final LectureSandboxPort lecture;
    private final LiaisonRepository liaisons;
    private final CampagneSandboxRepository campagnes;
    private final ExecutionSandboxRepository executions;
    private final ScenariosSandboxRepository scenarios;
    private final ReleveLiaisonSandboxRepository releves;
    private final EvaluationsLiaisonRepository evaluationBases;
    private final PieceSandboxRepository piecesSandbox;
    private final ConservationPiecesSandbox pieces;
    private final CouverturesSandbox couvertures;
    private final OutboxPublisher outbox;
    private final TransactionTemplate transaction;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public ReleveSandbox(
        LectureSandboxPort lecture,
        LiaisonRepository liaisons,
        CampagneSandboxRepository campagnes,
        ExecutionSandboxRepository executions,
        ScenariosSandboxRepository scenarios,
        ReleveLiaisonSandboxRepository releves,
        EvaluationsLiaisonRepository evaluationBases,
        PieceSandboxRepository piecesSandbox,
        ConservationPiecesSandbox pieces,
        CouverturesSandbox couvertures,
        OutboxPublisher outbox,
        TransactionTemplate transaction,
        Clock clock
    ) {
        this.lecture = lecture;
        this.liaisons = liaisons;
        this.campagnes = campagnes;
        this.executions = executions;
        this.scenarios = scenarios;
        this.releves = releves;
        this.evaluationBases = evaluationBases;
        this.piecesSandbox = piecesSandbox;
        this.pieces = pieces;
        this.couvertures = couvertures;
        this.outbox = outbox;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** Relève périodique : toutes les liaisons qui ne sont pas closes. */
    public int releverTout() {
        int n = 0;
        for (Liaison l : liaisons.findAll()) {
            if (l.getEtat() != EtatLiaison.CLOSE && releverLiaison(l.getId()).issue() == Issue.REUSSIE) {
                n++;
            }
        }
        return n;
    }

    /** Relève d'une liaison ; reportée tant que le délai indicatif de la dernière indisponibilité court. */
    public Bilan releverLiaison(UUID liaisonId) {
        Instant t = clock.instant();
        if (releves.findById(liaisonId).map(r -> r.reportee(t)).orElse(false)) {
            return new Bilan(Issue.REPORTEE, 0, 0);
        }
        int n = 0;
        int lus = 0;
        try {
            for (CampagneListee c : lecture.campagnes(liaisonId)) {
                if (c.signalement() != null) {
                    signaler(liaisonId, c);
                    continue;
                }
                try {
                    lus += releverCampagne(liaisonId, c.identifiantCampagne());
                    n++;
                } catch (HorsContrat e) {
                    // Campagne hors périmètre (404), en collision (409) ou réponse écartée : les autres sont relues.
                    LOG.warn("Campagne {} de la liaison {} écartée : {}", c.identifiantCampagne(), liaisonId, e.getMessage());
                }
            }
        } catch (Indisponible e) {
            LOG.warn("Relève de la liaison {} interrompue : {} (reprise dans {})", liaisonId, e.getMessage(), e.delai());
            noter(liaisonId, r -> r.echec(clock.instant(), e.getMessage(), e.delai()));
            return new Bilan(Issue.ECHEC, n, lus);
        } catch (HorsContrat e) {
            LOG.warn("Relève de la liaison {} écartée, réponse hors contrat : {}", liaisonId, e.getMessage());
            noter(liaisonId, r -> r.echec(clock.instant(), e.getMessage(), null));
            return new Bilan(Issue.ECHEC, n, lus);
        }
        noter(liaisonId, r -> r.reussie(clock.instant()));
        return new Bilan(Issue.REUSSIE, n, lus);
    }

    /**
     * Relève d'une campagne (périodique, ou déclenchée par sbx-lecture.campaign-published.v1) : la lecture du détail
     * vaut acquittement de la notification. Rend le nombre d'enregistrements lus.
     */
    public int releverCampagne(UUID liaisonId, String campagneId) {
        return reprendre(liaisonId, lecture.campagne(campagneId));
    }

    /**
     * Relecture immédiate d'une campagne désignée par sbx-lecture.campaign-published.v1 (US-EXI-15, algorithme 6.53) :
     * la notification ne porte que l'identifiant de la campagne ; sa liaison est celle du détail lu (lecture qui vaut
     * acquittement). Une indisponibilité est notée sur la relève de la liaison quand elle est connue ; la relève
     * périodique, indépendante de la notification, reste la source de vérité et reprend sans perte.
     */
    public Bilan releverCampagneNotifiee(String campagneId) {
        UUID liaisonId = null;
        try {
            Campagne c = lecture.campagne(campagneId);
            liaisonId = liaisonDe(c);
            return new Bilan(Issue.REUSSIE, 1, reprendre(liaisonId, c));
        } catch (Indisponible e) {
            LOG.warn("Relecture notifiée de la campagne {} interrompue : {} (reprise par la relève périodique)", campagneId, e.getMessage());
            if (liaisonId != null) {
                noter(liaisonId, r -> r.echec(clock.instant(), e.getMessage(), e.delai()));
            }
            return new Bilan(Issue.ECHEC, 0, 0);
        } catch (HorsContrat e) {
            LOG.warn("Relecture notifiée de la campagne {} écartée : {}", campagneId, e.getMessage());
            return new Bilan(Issue.ECHEC, 0, 0);
        }
    }

    private UUID liaisonDe(Campagne c) {
        String l = c.identifiantLiaison();
        if (l != null && l.matches("[0-9a-fA-F-]{36}")) {
            UUID id = UUID.fromString(l);
            if (liaisons.existsById(id)) {
                return id;
            }
        }
        throw new HorsContrat("campagne " + c.identifiantCampagne() + " d'une liaison inconnue");
    }

    private int reprendre(UUID liaisonId, Campagne c) {
        String campagneId = c.identifiantCampagne();
        if (!liaisonId.toString().equals(c.identifiantLiaison())) {
            throw new HorsContrat("campagne " + campagneId + " d'une autre liaison");
        }
        Liaison liaison = liaisons.findById(liaisonId).orElseThrow(() -> new HorsContrat("liaison inconnue " + liaisonId));
        Map<String, String> referentiel = referentiel(liaison);
        String position = transaction.execute(s -> {
            CampagneSandbox cs = campagnes.verrouiller(campagneId).orElseGet(() -> new CampagneSandbox(campagneId, liaisonId));
            cs.reprendre(c.code(), c.etat(), c.interfaceTest(), JSON.writeValueAsString(c.services()), JSON.writeValueAsString(c.taux()), c.versionBilan(), c.versionTable(), clock.instant());
            campagnes.save(cs);
            couvertures.appliquer(liaison, c);
            return cs.getPosition();
        });
        int lus = 0;
        PageExecutions page;
        do {
            page = lecture.executions(campagneId, position, TAILLE_PAGE);
            PageExecutions p = page;
            lus += p.enregistrements().size();
            transaction.executeWithoutResult(s -> enregistrer(liaison, campagneId, p, referentiel));
            position = p.positionSuivante();
        } while (page.close() && !page.enregistrements().isEmpty());
        int nouvellesPieces = pieces.conserver(campagneId);
        if (lus > 0 || nouvellesPieces > 0) {
            int luesFinal = lus;
            transaction.executeWithoutResult(s -> {
                String empreinteBilan = empreinteBilan(c);
                if (empreinteBilan != null) {
                    ajouterSocle(liaison, c, empreinteBilan);
                }
                outbox.publish(
                    "campaign",
                    campagneId,
                    CampaignResultImportedPayload.TYPE,
                    new CampaignResultImportedPayload(
                        liaisonId,
                        campagneId,
                        c.code(),
                        c.etat(),
                        luesFinal,
                        executions.countByCampagneId(campagneId),
                        executions.countByCampagneIdAndOperationCodeIsNull(campagneId),
                        nouvellesPieces,
                        c.versionBilan(),
                        empreinteBilan,
                        clock.instant()
                    )
                );
            });
        }
        LOG.info("Campagne {} de la liaison {} relue : {} enregistrement(s), {} pièce(s) nouvelle(s)", campagneId, liaisonId, lus, nouvellesPieces);
        return lus;
    }

    /** Empreinte du bilan effectivement contrôlée et conservée ; une pièce divergente ne devient jamais un socle. */
    private String empreinteBilan(Campagne c) {
        if (c.versionBilan() == null) {
            return null;
        }
        return piecesSandbox
            .findByCampagneIdAndNatureAndVersion(c.identifiantCampagne(), "BILAN", c.versionBilan())
            .filter(p -> p.getStatut() != PieceSandbox.Statut.EMPREINTE_DIVERGENTE)
            .map(PieceSandbox::getEmpreinte)
            .orElse(null);
    }

    /** Le socle historise chaque nouvelle empreinte ; une relève du même bilan ne crée pas de cycle. */
    private void ajouterSocle(Liaison liaison, Campagne c, String empreinteBilan) {
        var precedent = evaluationBases.findFirstByLiaison_IdOrderByCycleDesc(liaison.getId());
        if (precedent.map(p -> p.getEmpreinte().equals(empreinteBilan)).orElse(false)) {
            return;
        }
        Map<String, Object> composition = new HashMap<>();
        composition.put("source", "SANDBOX");
        composition.put("campagneId", c.identifiantCampagne());
        composition.put("versionBilan", c.versionBilan());
        evaluationBases.save(
            new EvaluationBase()
                .liaison(liaison)
                .empreinte(empreinteBilan)
                .cycle(precedent.map(p -> p.getCycle() + 1).orElse(1))
                .dateCalcul(clock.instant())
                .composition(JSON.writeValueAsString(composition))
        );
    }

    /** Une page et la position suivante, ensemble : la relève suivante repart de là. */
    private void enregistrer(Liaison liaison, String campagneId, PageExecutions page, Map<String, String> referentiel) {
        Instant t = clock.instant();
        for (Enregistrement e : page.enregistrements()) {
            ExecutionSandbox x = executions
                .findByCampagneIdAndIdentifiantExecution(campagneId, e.identifiantExecution())
                .orElseGet(() -> new ExecutionSandbox(campagneId, liaison.getId(), e.identifiantExecution()));
            String operation = referentiel.get(e.identifiantScenario());
            x.retenir(e.identifiantCas(), e.identifiantScenario(), e.versionScenario(), e.dateExecution(), e.verdictNatif(), e.verdictNormalise(), e.motif(), e.modeExecution(), e.referenceTrace(), e.position(), e.nature(), e.versionTable(), e.origine(), operation, t);
            executions.save(x);
            if (operation == null && !scenarios.existsByLiaison_IdAndCampagneIdAndIdScenario(liaison.getId(), campagneId, e.identifiantScenario())) {
                scenarios.save(
                    new UnmatchedScenario()
                        .idScenario(e.identifiantScenario())
                        .campagneId(campagneId)
                        .qualification(QualificationScenario.A_QUALIFIER)
                        .motif("Scénario absent du référentiel d'opérations de la liaison")
                        .dateDetection(t)
                        .liaison(liaison)
                );
            }
        }
        CampagneSandbox cs = campagnes.verrouiller(campagneId).orElseThrow();
        cs.avancer(page.positionSuivante(), t);
    }

    private Map<String, String> referentiel(Liaison l) {
        Map<String, String> m = new HashMap<>();
        if (l.getVersion() != null) {
            for (Object[] r : scenarios.scenariosDeLaVersion(l.getVersion().getId())) {
                m.putIfAbsent((String) r[0], (String) r[1]);
            }
        }
        return m;
    }

    private void signaler(UUID liaisonId, CampagneListee c) {
        transaction.executeWithoutResult(s -> {
            CampagneSandbox cs = campagnes.verrouiller(c.identifiantCampagne()).orElseGet(() -> new CampagneSandbox(c.identifiantCampagne(), liaisonId));
            cs.signaler(c.code(), c.signalement(), clock.instant());
            campagnes.save(cs);
        });
    }

    private void noter(UUID liaisonId, java.util.function.Consumer<ReleveLiaisonSandbox> f) {
        transaction.executeWithoutResult(s -> {
            ReleveLiaisonSandbox r = releves.findById(liaisonId).orElseGet(() -> new ReleveLiaisonSandbox(liaisonId));
            f.accept(r);
            releves.save(r);
        });
    }
}
