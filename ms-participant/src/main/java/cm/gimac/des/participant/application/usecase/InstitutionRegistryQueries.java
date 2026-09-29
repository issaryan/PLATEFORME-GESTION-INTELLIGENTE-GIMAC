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
import java.util.List;
import java.util.Locale;
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

    /** Filtre de la liste : statuts, recherche libre, dossiers portés par un chargé. */
    public record Filtre(Set<InstitutionStatus> statuts, String recherche, UUID chargeDdrmId) {}

    /** Dossier numérique de l'institution pour les étapes 1 et 2 (CR-CERT Fig. 7.3). */
    public record Dossier(
        Institution institution,
        List<AdhesionPiece> pieces,
        List<StatusTransition> transitions,
        List<Prerequis> prerequisAdhesion,
        Set<InstitutionStatus> transitionsPermises
    ) {}

    /** Compteurs du tableau de bord de la DDRM. */
    public record Indicateurs(
        long candidats,
        long piecesAExaminer,
        long propositionsEnAttente,
        long adherents,
        long enHomologation,
        long closSansSuite
    ) {}

    private final InstitutionRepository institutions;
    private final AdhesionPieceRepository pieces;
    private final StatusTransitionRepository transitions;
    private final DossierSnapshots snapshots;

    public InstitutionRegistryQueries(
        InstitutionRepository institutions,
        AdhesionPieceRepository pieces,
        StatusTransitionRepository transitions,
        DossierSnapshots snapshots
    ) {
        this.institutions = institutions;
        this.pieces = pieces;
        this.transitions = transitions;
        this.snapshots = snapshots;
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
            institutions.countByStatus(InstitutionStatus.CLOS_SANS_SUITE)
        );
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
                    cb.like(cb.lower(root.get(Institution_.participantCode)), motif, '\\')
                )
            );
        }
        return s;
    }
}
