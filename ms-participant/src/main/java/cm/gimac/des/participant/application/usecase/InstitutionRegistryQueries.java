package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Institution_;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.domain.service.InstitutionLifecycle;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Prerequis;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectures du registre pour les écrans et tableaux de bord de la DDRM (D-58) : liste filtrée et
 * paginée, dossier numérique d'une institution, compteurs.
 */
@Service
@Transactional(readOnly = true)
public class InstitutionRegistryQueries {

    /** Filtre de la liste : statuts, recherche libre, dossiers portés par un chargé, groupe (D-104, sans égard à la casse). */
    public record Filtre(Set<InstitutionStatus> statuts, String recherche, UUID chargeDdrmId, String groupe) {
        public Filtre(Set<InstitutionStatus> statuts, String recherche, UUID chargeDdrmId) {
            this(statuts, recherche, chargeDdrmId, null);
        }
    }

    /** Dossier numérique de l'institution pour les étapes 1 et 2 (CR-CERT Fig. 7.3). */
    public record Dossier(
        Institution institution,
        List<AdhesionPiece> pieces,
        List<StatusTransition> transitions,
        List<Prerequis> prerequisAdhesion,
        Set<InstitutionStatus> transitionsPermises
    ) {}

    /** Compteurs du tableau de bord de la DDRM, et flux des 12 derniers mois (mois courant compris). */
    public record Indicateurs(
        long candidats,
        long piecesAExaminer,
        long propositionsEnAttente,
        long adherents,
        long enHomologation,
        long closSansSuite,
        List<FluxMensuel> fluxParMois
    ) {
        public Indicateurs(long candidats, long piecesAExaminer, long propositionsEnAttente, long adherents, long enHomologation, long closSansSuite) {
            this(candidats, piecesAExaminer, propositionsEnAttente, adherents, enHomologation, closSansSuite, List.of());
        }
    }

    /** Flux d'un mois : prises de contact, adhésions prononcées, clôtures sans suite. */
    public record FluxMensuel(YearMonth mois, long contacts, long adhesions, long clotures) {}

    /** Profondeur du flux mensuel. */
    public static final int MOIS_FLUX = 12;
    /** Fuseau des mois : celui de la CEMAC (UTC+1). */
    public static final ZoneId FUSEAU = ZoneId.of("Africa/Douala");

    private final InstitutionRepository institutions;
    private final AdhesionPieceRepository pieces;
    private final StatusTransitionRepository transitions;
    private final DossierSnapshots snapshots;
    private final Clock clock;

    public InstitutionRegistryQueries(
        InstitutionRepository institutions,
        AdhesionPieceRepository pieces,
        StatusTransitionRepository transitions,
        DossierSnapshots snapshots,
        Clock clock
    ) {
        this.institutions = institutions;
        this.pieces = pieces;
        this.transitions = transitions;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    public Page<Institution> lister(Filtre f, int page, int taille) {
        int t = Math.clamp(taille, 1, 100);
        return institutions.findAll(specification(f), PageRequest.of(Math.max(page, 0), t, Sort.by(Sort.Direction.DESC, Institution_.CREATED_AT)));
    }

    public Dossier dossier(UUID id) {
        Institution i = institutions.findById(id).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", id));
        return new Dossier(
            i,
            pieces.findByInstitutionIdOrderByCode(id),
            transitions.findByInstitutionIdOrderByRequestedAtDesc(id),
            PrerequisitesEvaluator.evaluer(snapshots.de(i), InstitutionStatus.ADHERENT, null),
            InstitutionLifecycle.transitionsPermises(i.getStatus())
        );
    }

    public Indicateurs indicateurs() {
        return new Indicateurs(
            institutions.countByStatus(InstitutionStatus.CANDIDAT),
            pieces.countByStateAndInstitutionStatus(AdhesionPieceState.RECUE, InstitutionStatus.CANDIDAT),
            transitions.countByState(TransitionState.PROPOSEE),
            institutions.countByStatus(InstitutionStatus.ADHERENT),
            institutions.countByStatus(InstitutionStatus.EN_HOMOLOGATION),
            institutions.countByStatus(InstitutionStatus.CLOS_SANS_SUITE),
            fluxParMois()
        );
    }

    /** Un point par mois sur les 12 derniers mois, zéro les mois sans mouvement. */
    List<FluxMensuel> fluxParMois() {
        YearMonth fin = YearMonth.now(clock.withZone(FUSEAU));
        YearMonth debut = fin.minusMonths(MOIS_FLUX - 1L);
        Instant depuis = debut.atDay(1).atStartOfDay(FUSEAU).toInstant();
        Map<YearMonth, Long> contacts = parMois(institutions.creationsDepuis(depuis));
        Map<YearMonth, Long> adhesions = parMois(institutions.adhesionsDepuis(depuis));
        Map<YearMonth, Long> clotures = parMois(institutions.cloturesDepuis(depuis));
        List<FluxMensuel> l = new ArrayList<>(MOIS_FLUX);
        for (YearMonth m = debut; !m.isAfter(fin); m = m.plusMonths(1)) {
            l.add(new FluxMensuel(m, contacts.getOrDefault(m, 0L), adhesions.getOrDefault(m, 0L), clotures.getOrDefault(m, 0L)));
        }
        return l;
    }

    private static Map<YearMonth, Long> parMois(List<Instant> instants) {
        Map<YearMonth, Long> m = new HashMap<>();
        instants.stream().filter(Objects::nonNull).forEach(i -> m.merge(YearMonth.from(i.atZone(FUSEAU)), 1L, Long::sum));
        return m;
    }

    static Specification<Institution> specification(Filtre f) {
        Specification<Institution> s = Specification.unrestricted();
        if (f.statuts() != null && !f.statuts().isEmpty()) {
            s = s.and((root, q, cb) -> root.get(Institution_.status).in(f.statuts()));
        }
        if (f.chargeDdrmId() != null) {
            s = s.and((root, q, cb) -> cb.equal(root.get(Institution_.ddrmOfficerId), f.chargeDdrmId()));
        }
        if (f.recherche() != null && !f.recherche().isBlank()) {
            String motif = "%" + f.recherche().trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
            s = s.and((root, q, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get(Institution_.name)), motif, '\\'),
                    cb.like(cb.lower(root.get(Institution_.acronym)), motif, '\\'),
                    cb.like(cb.lower(root.get(Institution_.participantCode)), motif, '\\'),
                    cb.like(cb.lower(root.get(Institution_.groupName)), motif, '\\')
                )
            );
        }
        if (f.groupe() != null && !f.groupe().isBlank()) {
            String groupe = f.groupe().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            s = s.and((root, q, cb) -> cb.equal(cb.lower(root.get(Institution_.groupName)), groupe));
        }
        return s;
    }

    /** Groupes déclarés au registre (D-104), triés : valeurs du filtre de la liste et suggestions de la saisie. */
    public List<String> groupes() {
        return institutions.groupes();
    }
}
