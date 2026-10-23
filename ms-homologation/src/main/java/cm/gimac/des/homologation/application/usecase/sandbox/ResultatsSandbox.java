package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Taux;
import cm.gimac.des.homologation.application.usecase.ParcoursDepot;
import cm.gimac.des.homologation.domain.sandbox.CampagneSandbox;
import cm.gimac.des.homologation.domain.sandbox.ExecutionSandbox;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import cm.gimac.des.homologation.domain.sandbox.ReleveLiaisonSandbox;
import cm.gimac.des.homologation.repository.CampagneSandboxRepository;
import cm.gimac.des.homologation.repository.ExecutionSandboxRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.PieceSandboxRepository;
import cm.gimac.des.homologation.repository.ReleveLiaisonSandboxRepository;
import cm.gimac.des.homologation.repository.ScenariosSandboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Résultats de la sandbox repris pour une liaison (D-118 point 1, US-EXI-14, US-EXI-17) : lecture seule, servie à
 * l'onglet « Sandbox » du dossier de liaison. Rien n'est recalculé : verdicts natif et normalisé, version de la table,
 * taux (numérateur, dénominateur, version de formule, horodatage) tels que restitués par SBX-LECTURE ; pièces avec leur
 * empreinte ; scénarios non rapprochés ; état de la dernière relève (dernière réussite, dernier échec).
 */
@Service
@Transactional(readOnly = true)
public class ResultatsSandbox {

    /** Mention permanente de l'écran : résultats repris de la sandbox, jamais recalculés. */
    public static final String MENTION = "REPRIS_NON_RECALCULE";
    static final int TAILLE_MAX = 100;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public record Resultats(
        UUID liaisonId,
        String mention,
        Instant derniereReleveReussie,
        Instant dernierEchec,
        String motifEchec,
        boolean enEchec,
        List<CampagneVue> campagnes,
        List<ScenarioVue> scenariosNonRapproches
    ) {}

    public record CampagneVue(
        String campagneId,
        String code,
        String etat,
        String interfaceTest,
        List<String> services,
        Integer versionBilan,
        String versionTable,
        String signalement,
        List<Taux> taux,
        long executions,
        long nonRapprochees,
        List<PieceVue> pieces,
        Instant releveLe
    ) {}

    public record PieceVue(String nature, int version, String empreinte, long taille, String nom, UUID documentId, String statut) {}

    public record ScenarioVue(UUID id, String idScenario, String campagneId, String qualification, Instant dateDetection) {}

    public record ExecutionVue(
        String identifiantExecution,
        String identifiantCas,
        String identifiantScenario,
        String versionScenario,
        Instant dateExecution,
        String verdictNatif,
        String verdictNormalise,
        String motif,
        String modeExecution,
        String referenceTrace,
        String nature,
        String versionTable,
        String origine,
        String operationCode
    ) {}

    public record PageExecutions(List<ExecutionVue> contenu, long total, int page, int taille) {}

    private final LiaisonRepository liaisons;
    private final CampagneSandboxRepository campagnes;
    private final ExecutionSandboxRepository executions;
    private final PieceSandboxRepository pieces;
    private final ScenariosSandboxRepository scenarios;
    private final ReleveLiaisonSandboxRepository releves;

    public ResultatsSandbox(
        LiaisonRepository liaisons,
        CampagneSandboxRepository campagnes,
        ExecutionSandboxRepository executions,
        PieceSandboxRepository pieces,
        ScenariosSandboxRepository scenarios,
        ReleveLiaisonSandboxRepository releves
    ) {
        this.liaisons = liaisons;
        this.campagnes = campagnes;
        this.executions = executions;
        this.pieces = pieces;
        this.scenarios = scenarios;
        this.releves = releves;
    }

    public Resultats resultats(UUID liaisonId) {
        exiger(liaisonId);
        ReleveLiaisonSandbox r = releves.findById(liaisonId).orElseGet(() -> new ReleveLiaisonSandbox(liaisonId));
        List<CampagneVue> c = campagnes.findByLiaisonIdOrderByCampagneId(liaisonId).stream().map(this::vue).toList();
        List<ScenarioVue> s = scenarios
            .findByLiaison_IdOrderByDateDetectionAsc(liaisonId)
            .stream()
            .map(u -> new ScenarioVue(u.getId(), u.getIdScenario(), u.getCampagneId(), u.getQualification().name(), u.getDateDetection()))
            .toList();
        return new Resultats(liaisonId, MENTION, r.getDerniereReleveReussie(), r.getDernierEchec(), r.getMotifEchec(), r.enEchec(), c, s);
    }

    /** Exécutions d'une campagne de la liaison, paginées (page à partir de 0, 1 à 100 par page), par date puis identifiant. */
    public PageExecutions executions(UUID liaisonId, String campagneId, int page, int taille) {
        exiger(liaisonId);
        int t = Math.clamp(taille, 1, TAILLE_MAX);
        Page<ExecutionSandbox> p = executions.findByLiaisonIdAndCampagneId(
            liaisonId,
            campagneId,
            PageRequest.of(Math.max(page, 0), t, Sort.by("dateExecution", "identifiantExecution"))
        );
        return new PageExecutions(p.getContent().stream().map(ResultatsSandbox::vue).toList(), p.getTotalElements(), p.getNumber(), t);
    }

    private void exiger(UUID liaisonId) {
        if (!liaisons.existsById(liaisonId)) {
            throw ParcoursDepot.introuvable(liaisonId);
        }
    }

    private CampagneVue vue(CampagneSandbox c) {
        List<PieceVue> p = pieces
            .findByCampagneIdOrderByNatureAscVersionAsc(c.getCampagneId())
            .stream()
            .map((PieceSandbox x) -> new PieceVue(x.getNature(), x.getVersion(), x.getEmpreinte(), x.getTaille(), x.getNom(), x.getDocumentId(), x.getStatut().name()))
            .toList();
        return new CampagneVue(
            c.getCampagneId(),
            c.getCode(),
            c.getEtat(),
            c.getInterfaceTest(),
            liste(c.getServices(), new TypeReference<List<String>>() {}),
            c.getVersionBilan(),
            c.getVersionTable(),
            c.getSignalement(),
            liste(c.getTaux(), new TypeReference<List<Taux>>() {}),
            executions.countByCampagneId(c.getCampagneId()),
            executions.countByCampagneIdAndOperationCodeIsNull(c.getCampagneId()),
            p,
            c.getReleveLe()
        );
    }

    private static ExecutionVue vue(ExecutionSandbox e) {
        return new ExecutionVue(
            e.getIdentifiantExecution(),
            e.getIdentifiantCas(),
            e.getIdentifiantScenario(),
            e.getVersionScenario(),
            e.getDateExecution(),
            e.getVerdictNatif(),
            e.getVerdictNormalise(),
            e.getMotif(),
            e.getModeExecution(),
            e.getReferenceTrace(),
            e.getNature(),
            e.getVersionTable(),
            e.getOrigine(),
            e.getOperationCode()
        );
    }

    private static <T> List<T> liste(String json, TypeReference<List<T>> type) {
        return json == null ? List.of() : JSON.readValue(json, type);
    }
}
