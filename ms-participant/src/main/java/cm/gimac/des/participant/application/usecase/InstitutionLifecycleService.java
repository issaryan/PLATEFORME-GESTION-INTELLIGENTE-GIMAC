package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase;
import cm.gimac.des.participant.application.port.out.DocumentRegistry;
import cm.gimac.des.participant.application.port.out.StrongReauthVerifier;
import cm.gimac.des.participant.application.AdhesionProperties;
import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.domain.event.AdhesionPieceRecordedPayload;
import cm.gimac.des.participant.domain.event.InstitutionRegisteredPayload;
import cm.gimac.des.participant.domain.event.InstitutionUpdatedPayload;
import cm.gimac.des.participant.domain.event.StatusChangedPayload;
import cm.gimac.des.participant.domain.event.TransitionRejectedPayload;
import cm.gimac.des.participant.domain.event.TransitionRequestedPayload;
import cm.gimac.des.participant.domain.service.InstitutionLifecycle;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Prerequis;
import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Étapes 1 et 2 du parcours (prise de contact, adhésion : CR-CERT §4.2.1, D-57) et automate du
 * cycle de vie avec maker-checker (PAR-01 à PAR-04, algorithme 6.25).
 *
 * <p>Chaque écriture publie son événement par l'outbox dans la même transaction ; chaque décision
 * sur une institution prend un verrou pessimiste sur sa ligne.
 */
@Service
@Transactional
public class InstitutionLifecycleService implements ManageInstitutionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(InstitutionLifecycleService.class);

    static final String AGREGAT_INSTITUTION = "institution";
    static final String AGREGAT_PIECE = "adhesion";
    static final String AGREGAT_TRANSITION = "transition";
    static final String FINALITE_APPROBATION = "PAR_TRANSITION_APPROVE";

    private final InstitutionRepository institutions;
    private final AdhesionPieceRepository pieces;
    private final StatusTransitionRepository transitions;
    private final InteractionRepository interactions;
    private final DocumentRegistry documents;
    private final StrongReauthVerifier reauth;
    private final OutboxPublisher outbox;
    private final AdhesionProperties adhesion;
    private final DossierSnapshots snapshots;
    private final Clock clock;

    public InstitutionLifecycleService(
        InstitutionRepository institutions,
        AdhesionPieceRepository pieces,
        StatusTransitionRepository transitions,
        InteractionRepository interactions,
        DocumentRegistry documents,
        StrongReauthVerifier reauth,
        OutboxPublisher outbox,
        AdhesionProperties adhesion,
        DossierSnapshots snapshots,
        Clock clock
    ) {
        this.institutions = institutions;
        this.pieces = pieces;
        this.transitions = transitions;
        this.interactions = interactions;
        this.documents = documents;
        this.reauth = reauth;
        this.outbox = outbox;
        this.adhesion = adhesion;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ étape 1 : prise de contact

    @Override
    public Institution enregistrerCandidat(Candidature c, Acteur acteur) {
        verifierUnicite(c.codeIdentification(), c.codeBanqueBeac(), null, null);
        Instant maintenant = clock.instant();
        Institution i = new Institution()
            .status(InstitutionStatus.CANDIDAT)
            // Dossier d'adhésion incomplet par construction : à régulariser jusqu'au prononcé.
            .complianceState(ComplianceState.A_REGULARISER)
            .openLinkCount(0)
            .openSuspenseCount(0)
            .createdAt(maintenant)
            .ddrmOfficerId(acteur.id())
            .ddrmOfficerName(acteur.nom());
        appliquerFiche(i, c);
        i.setContactedOn(c.datePriseDeContact() != null ? c.datePriseDeContact() : LocalDate.now(clock));
        Institution enregistree = institutions.save(i);
        for (AdhesionProperties.PieceDefinition def : adhesion.pieces()) {
            pieces.save(
                new AdhesionPiece()
                    .code(def.code())
                    .label(def.libelle())
                    .mandatory(def.obligatoire())
                    .state(AdhesionPieceState.ATTENDUE)
                    .institution(enregistree)
            );
        }
        journaliser(enregistree, InteractionNature.AUTRE, "Prise de contact enregistrée", acteur, null);
        outbox.publish(
            AGREGAT_INSTITUTION,
            enregistree.getId(),
            InstitutionRegisteredPayload.TYPE,
            new InstitutionRegisteredPayload(
                enregistree.getId(),
                enregistree.getName(),
                enregistree.getAcronym(),
                enregistree.getCountry(),
                enregistree.getCategory().name(),
                enregistree.getStatus().name(),
                enregistree.getContactedOn(),
                acteur.id()
            )
        );
        return enregistree;
    }

    @Override
    public Institution modifierFiche(UUID institutionId, Candidature c, Acteur acteur) {
        Institution i = verrouiller(institutionId);
        exigerNonTerminal(i);
        verifierUnicite(c.codeIdentification(), c.codeBanqueBeac(), null, i.getId());
        appliquerFiche(i, c);
        if (c.datePriseDeContact() != null) {
            i.setContactedOn(c.datePriseDeContact());
        }
        publierMiseAJour(i, List.of("fiche"), acteur);
        return i;
    }

    // ------------------------------------------------------------------ étape 2 : adhésion

    @Override
    public Institution definirConditionsAdhesion(UUID institutionId, ConditionsAdhesion c, Acteur acteur) {
        Institution i = verrouiller(institutionId);
        exigerStatut(i, InstitutionStatus.CANDIDAT);
        verifierUnicite(null, null, c.codeParticipant(), i.getId());
        i.setParticipantCode(c.codeParticipant());
        i.setMembershipMode(c.mode());
        i.setSettlementAccountRef(c.mode() == MembershipMode.DIRECT ? c.compteReglement() : null);
        i.setRingFencedAccountRef(c.mode() == MembershipMode.INDIRECT ? c.compteCantonnement() : null);
        i.setSettlementBank(c.banqueReglementId() == null ? null : autre(i, c.banqueReglementId()));
        i.setSponsorBank(c.mode() == MembershipMode.INDIRECT && c.banqueMarraineId() != null ? autre(i, c.banqueMarraineId()) : null);
        publierMiseAJour(i, List.of("conditionsAdhesion"), acteur);
        return i;
    }

    @Override
    public AdhesionPiece deposerPiece(UUID institutionId, String code, Depot depot, Acteur acteur) {
        Institution i = verrouiller(institutionId);
        exigerStatut(i, InstitutionStatus.CANDIDAT);
        AdhesionPiece p = piece(institutionId, code);
        if (p.getState() == AdhesionPieceState.VALIDEE) {
            throw ParticipantException.conflict("PAR_PIECE_ALREADY_VALIDATED", "Pièce déjà validée", Map.of("code", code));
        }
        switch (documents.verifier(depot.documentId(), depot.documentSha256())) {
            case INCONNUE -> throw ParticipantException.conflict(
                "PAR_DOCUMENT_UNKNOWN",
                "Pièce inconnue du registre documentaire",
                Map.of("documentId", depot.documentId().toString())
            );
            case DIVERGENTE -> throw ParticipantException.conflict(
                "PAR_DOCUMENT_HASH_MISMATCH",
                "L'empreinte déclarée diffère de celle du registre documentaire",
                Map.of("documentId", depot.documentId().toString())
            );
            case CONFORME -> {
                // référence rattachée ci-dessous
            }
        }
        p.setState(AdhesionPieceState.RECUE);
        p.setDocumentId(depot.documentId());
        p.setDocumentSha256(depot.documentSha256());
        p.setFileName(depot.nomFichier());
        p.setDepositedAt(clock.instant());
        p.setDepositedById(acteur.id());
        p.setReviewedAt(null);
        p.setReviewedById(null);
        p.setRejectionReason(null);
        publierPiece(p, null, acteur);
        return p;
    }

    @Override
    public AdhesionPiece validerPiece(UUID institutionId, String code, Acteur acteur) {
        return examiner(institutionId, code, AdhesionPieceState.VALIDEE, null, acteur);
    }

    @Override
    public AdhesionPiece rejeterPiece(UUID institutionId, String code, String motif, Acteur acteur) {
        exigerMotif(motif);
        return examiner(institutionId, code, AdhesionPieceState.REJETEE, motif, acteur);
    }

    private AdhesionPiece examiner(UUID institutionId, String code, AdhesionPieceState verdict, String motif, Acteur acteur) {
        Institution i = verrouiller(institutionId);
        exigerStatut(i, InstitutionStatus.CANDIDAT);
        AdhesionPiece p = piece(institutionId, code);
        if (p.getState() != AdhesionPieceState.RECUE) {
            throw ParticipantException.conflict(
                "PAR_PIECE_NOT_RECEIVED",
                "Seule une pièce reçue peut être examinée",
                Map.of("code", code, "etat", p.getState().name())
            );
        }
        p.setState(verdict);
        p.setReviewedAt(clock.instant());
        p.setReviewedById(acteur.id());
        p.setRejectionReason(motif);
        publierPiece(p, motif, acteur);
        return p;
    }

    // ------------------------------------------------------------------ automate et maker-checker (6.25)

    @Override
    @Transactional(readOnly = true)
    public List<Prerequis> prerequis(UUID institutionId, InstitutionStatus cible) {
        Institution i = institutions.findById(institutionId).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", institutionId));
        return PrerequisitesEvaluator.evaluer(snapshots.de(i), cible, null);
    }

    @Override
    public StatusTransition proposerTransition(UUID institutionId, InstitutionStatus vers, String motif, UUID pieceDecision, Acteur acteur) {
        exigerMotif(motif);
        Institution i = verrouiller(institutionId);
        InstitutionLifecycle.verifierProposable(i.getStatus(), vers);
        if (transitions.existsByInstitutionIdAndState(institutionId, TransitionState.PROPOSEE)) {
            throw ParticipantException.conflict("PAR_TRANSITION_PENDING", "Une proposition est déjà en attente pour cette institution", Map.of());
        }
        exigerPrerequis(i, vers, pieceDecision);
        int requises = InstitutionLifecycle.approbationsRequises(i.getStatus(), vers);
        StatusTransition t = transitions.save(
            new StatusTransition()
                .fromStatus(i.getStatus())
                .toStatus(vers)
                .reason(motif)
                .requesterId(acteur.id())
                .requestedAt(clock.instant())
                .automaticTransition(false)
                .requiredApprovals(requises)
                .state(TransitionState.PROPOSEE)
                .evidenceDocumentId(pieceDecision)
                .institution(i)
        );
        outbox.publish(
            AGREGAT_TRANSITION,
            i.getId(),
            TransitionRequestedPayload.TYPE,
            new TransitionRequestedPayload(i.getId(), t.getId(), t.getFromStatus().name(), vers.name(), motif, acteur.id(), requises)
        );
        return t;
    }

    @Override
    public StatusTransition approuverTransition(UUID institutionId, UUID transitionId, Acteur acteur, String assertionReauth) {
        Institution i = verrouiller(institutionId);
        StatusTransition t = enAttente(institutionId, transitionId);
        if (acteur.id().equals(t.getRequesterId()) || acteur.id().equals(t.getApproverId())) {
            LOG.warn(
                "Approbation refusée (séparation des pouvoirs) : acteur {} sur la transition {} de l'institution {}",
                acteur.id(),
                transitionId,
                institutionId
            );
            throw ParticipantException.forbidden("PAR_SEPARATION_OF_DUTIES", "L'approbateur doit être distinct du demandeur et du premier approbateur");
        }
        if (assertionReauth == null || assertionReauth.isBlank()) {
            throw ParticipantException.reauthRequired();
        }
        if (!reauth.verifier(assertionReauth, acteur.id(), FINALITE_APPROBATION)) {
            throw ParticipantException.forbidden("PAR_REAUTH_INVALID", "Réauthentification forte invalide");
        }
        if (i.getStatus() != t.getFromStatus()) {
            throw ParticipantException.conflict(
                "PAR_TRANSITION_STALE",
                "Le statut de l'institution a changé depuis la proposition",
                Map.of("statutActuel", i.getStatus().name(), "de", t.getFromStatus().name())
            );
        }
        exigerPrerequis(i, t.getToStatus(), t.getEvidenceDocumentId());
        Instant maintenant = clock.instant();
        if (t.getRequiredApprovals() > 1 && t.getApproverId() == null) {
            t.setApproverId(acteur.id());
            return t;
        }
        if (t.getApproverId() == null) {
            t.setApproverId(acteur.id());
        } else {
            t.setSecondApproverId(acteur.id());
            t.setSecondApprovedAt(maintenant);
        }
        t.setState(TransitionState.APPROUVEE);
        t.setDecidedAt(maintenant);
        appliquer(i, t, maintenant);
        journaliser(i, InteractionNature.CHANGEMENT_STATUT, t.getFromStatus() + " vers " + t.getToStatus() + " : " + t.getReason(), acteur, t.getId().toString());
        outbox.publish(
            AGREGAT_INSTITUTION,
            i.getId(),
            StatusChangedPayload.TYPE,
            new StatusChangedPayload(i.getId(), i.getParticipantCode(), t.getFromStatus().name(), i.getStatus().name(), t.getReason(), t.getId(), maintenant)
        );
        return t;
    }

    @Override
    public StatusTransition rejeterTransition(UUID institutionId, UUID transitionId, String motif, Acteur acteur) {
        exigerMotif(motif);
        Institution i = verrouiller(institutionId);
        StatusTransition t = enAttente(institutionId, transitionId);
        if (acteur.id().equals(t.getRequesterId())) {
            throw ParticipantException.forbidden("PAR_SEPARATION_OF_DUTIES", "Le demandeur ne peut pas statuer sur sa propre proposition");
        }
        t.setState(TransitionState.REJETEE);
        t.setRejectionReason(motif);
        t.setDecidedAt(clock.instant());
        if (t.getApproverId() == null) {
            t.setApproverId(acteur.id());
        } else {
            t.setSecondApproverId(acteur.id());
        }
        outbox.publish(
            AGREGAT_TRANSITION,
            i.getId(),
            TransitionRejectedPayload.TYPE,
            new TransitionRejectedPayload(i.getId(), t.getId(), t.getFromStatus().name(), t.getToStatus().name(), motif, acteur.id())
        );
        return t;
    }

    // ------------------------------------------------------------------ outils

    private void appliquer(Institution i, StatusTransition t, Instant maintenant) {
        i.setStatus(t.getToStatus());
        switch (t.getToStatus()) {
            case ADHERENT -> {
                i.setAdmittedAt(maintenant);
                // Le prononcé revient à l'approbateur (chef de la DDRM), pas au chargé qui l'a proposé.
                i.setAdmittedById(t.getSecondApproverId() != null ? t.getSecondApproverId() : t.getApproverId());
                i.setJoinedOn(LocalDate.now(clock));
                i.setComplianceState(ComplianceState.CONFORME);
            }
            case CLOS_SANS_SUITE -> {
                i.setClosureReason(t.getReason());
                i.setClosedAt(maintenant);
            }
            default -> {
                // Les autres états n'ont pas de donnée propre dans la fiche.
            }
        }
    }

    private void exigerPrerequis(Institution i, InstitutionStatus vers, UUID pieceDecision) {
        List<Prerequis> manquants = PrerequisitesEvaluator.manquants(PrerequisitesEvaluator.evaluer(snapshots.de(i), vers, pieceDecision));
        if (!manquants.isEmpty()) {
            throw ParticipantException.conflict(
                "PAR_PREREQUISITES_MISSING",
                "Prérequis de l'état " + vers + " non réunis",
                Map.of("vers", vers.name(), "manquants", manquants.stream().map(p -> Map.of("code", p.code(), "libelle", p.libelle())).toList())
            );
        }
    }

    private StatusTransition enAttente(UUID institutionId, UUID transitionId) {
        StatusTransition t = transitions
            .findById(transitionId)
            .filter(x -> x.getInstitution() != null && institutionId.equals(x.getInstitution().getId()))
            .orElseThrow(() -> ParticipantException.notFound("PAR_TRANSITION_NOT_FOUND", transitionId));
        if (t.getState() != TransitionState.PROPOSEE) {
            throw ParticipantException.conflict("PAR_TRANSITION_NOT_PENDING", "Cette proposition a déjà été tranchée", Map.of("etat", t.getState().name()));
        }
        return t;
    }

    private Institution verrouiller(UUID id) {
        return institutions.findByIdForUpdate(id).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", id));
    }

    private Institution autre(Institution i, UUID id) {
        if (id.equals(i.getId())) {
            throw ParticipantException.conflict("PAR_SPONSOR_INVALID", "Une institution ne peut être sa propre banque de règlement ou marraine", Map.of());
        }
        return institutions.findById(id).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", id));
    }

    private AdhesionPiece piece(UUID institutionId, String code) {
        return pieces
            .findByInstitutionIdAndCode(institutionId, code)
            .orElseThrow(() -> ParticipantException.notFound("PAR_PIECE_NOT_FOUND", code));
    }

    private void verifierUnicite(String bic, String codeBeac, String codeParticipant, UUID soi) {
        UUID id = soi == null ? new UUID(0, 0) : soi;
        if (bic != null && institutions.existsByIdentificationCodeAndIdNot(bic, id)) {
            throw doublon("codeIdentification", bic);
        }
        if (codeBeac != null && institutions.existsByBeacBankCodeAndIdNot(codeBeac, id)) {
            throw doublon("codeBanqueBeac", codeBeac);
        }
        if (codeParticipant != null && institutions.existsByParticipantCodeAndIdNot(codeParticipant, id)) {
            throw doublon("codeParticipant", codeParticipant);
        }
    }

    private static ParticipantException doublon(String champ, String valeur) {
        return ParticipantException.conflict("PAR_CODE_ALREADY_USED", "Code déjà attribué à une autre institution", Map.of("champ", champ, "valeur", valeur));
    }

    private static void appliquerFiche(Institution i, Candidature c) {
        i.setName(c.nom());
        i.setAcronym(c.sigle());
        i.setCountry(c.pays());
        i.setCategory(c.categorie());
        i.setIdentificationCode(c.codeIdentification());
        i.setBeacBankCode(c.codeBanqueBeac());
        i.setContactOrigin(c.originePriseDeContact());
        i.setIntendedProject(c.projetEnvisage());
    }

    private static void exigerStatut(Institution i, InstitutionStatus attendu) {
        if (i.getStatus() != attendu) {
            throw ParticipantException.conflict(
                "PAR_INSTITUTION_STATUS",
                "Action possible seulement à l'état " + attendu,
                Map.of("statut", i.getStatus().name(), "attendu", attendu.name())
            );
        }
    }

    private static void exigerNonTerminal(Institution i) {
        if (InstitutionLifecycle.estTerminal(i.getStatus())) {
            throw ParticipantException.conflict("PAR_INSTITUTION_CLOSED", "Institution close : fiche en lecture seule", Map.of("statut", i.getStatus().name()));
        }
    }

    private static void exigerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw ParticipantException.conflict("PAR_REASON_REQUIRED", "Motif obligatoire", Map.of());
        }
    }

    private void publierMiseAJour(Institution i, List<String> champs, Acteur acteur) {
        outbox.publish(AGREGAT_INSTITUTION, i.getId(), InstitutionUpdatedPayload.TYPE, new InstitutionUpdatedPayload(i.getId(), champs, acteur.id()));
    }

    private void publierPiece(AdhesionPiece p, String motif, Acteur acteur) {
        outbox.publish(
            AGREGAT_PIECE,
            p.getInstitution().getId(),
            AdhesionPieceRecordedPayload.TYPE,
            new AdhesionPieceRecordedPayload(
                p.getInstitution().getId(),
                p.getId(),
                p.getCode(),
                p.getState().name(),
                p.getDocumentId(),
                p.getDocumentSha256(),
                motif,
                acteur.id()
            )
        );
    }

    private void journaliser(Institution i, InteractionNature nature, String resume, Acteur acteur, String reference) {
        interactions.save(
            new Interaction()
                .nature(nature)
                .summary(resume.length() > 500 ? resume.substring(0, 500) : resume)
                .occurredAt(clock.instant())
                .authorId(acteur.id())
                .reference(reference)
                .institution(i)
        );
    }
}
