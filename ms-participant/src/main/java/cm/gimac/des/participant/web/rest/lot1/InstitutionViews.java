package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.usecase.InstitutionRegistryQueries;
import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Prerequis;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Vues JSON des routes de la DDRM ; les noms de champs sont ceux des descripteurs de l'interface. */
final class InstitutionViews {

    private InstitutionViews() {}

    record PageVue<T>(List<T> contenu, long total) {}

    record Personne(UUID id, String nom) {}

    record InstitutionVue(
        UUID id,
        String nom,
        String sigle,
        String pays,
        String categorie,
        String statut,
        String codeParticipant,
        String codeIdentification,
        String codeBanqueBeac,
        String modeAdhesion,
        LocalDate datePriseDeContact,
        String originePriseDeContact,
        String projetEnvisage,
        Personne chargeDdrm,
        String conformite,
        Instant creeLe
    ) {
        static InstitutionVue de(Institution i) {
            return new InstitutionVue(
                i.getId(),
                i.getName(),
                i.getAcronym(),
                i.getCountry(),
                i.getCategory().name(),
                i.getStatus().name(),
                i.getParticipantCode(),
                i.getIdentificationCode(),
                i.getBeacBankCode(),
                i.getMembershipMode() == null ? null : i.getMembershipMode().name(),
                i.getContactedOn(),
                i.getContactOrigin(),
                i.getIntendedProject(),
                i.getDdrmOfficerId() == null ? null : new Personne(i.getDdrmOfficerId(), i.getDdrmOfficerName()),
                i.getComplianceState().name(),
                i.getCreatedAt()
            );
        }
    }

    record AdhesionVue(
        String codeParticipant,
        String mode,
        UUID banqueReglementId,
        UUID banqueMarraineId,
        String compteReglement,
        String compteCantonnement,
        LocalDate dateAdhesion,
        Instant prononceeLe,
        UUID prononceePar,
        String motifCloture,
        Instant closLe
    ) {
        static AdhesionVue de(Institution i) {
            return new AdhesionVue(
                i.getParticipantCode(),
                i.getMembershipMode() == null ? null : i.getMembershipMode().name(),
                i.getSettlementBank() == null ? null : i.getSettlementBank().getId(),
                i.getSponsorBank() == null ? null : i.getSponsorBank().getId(),
                i.getSettlementAccountRef(),
                i.getRingFencedAccountRef(),
                i.getJoinedOn(),
                i.getAdmittedAt(),
                i.getAdmittedById(),
                i.getClosureReason(),
                i.getClosedAt()
            );
        }
    }

    record PieceVue(
        UUID id,
        String code,
        String libelle,
        boolean obligatoire,
        String etat,
        UUID documentId,
        String documentSha256,
        String nomFichier,
        Instant deposeeLe,
        Instant examineeLe,
        String motifRejet
    ) {
        static PieceVue de(AdhesionPiece p) {
            return new PieceVue(
                p.getId(),
                p.getCode(),
                p.getLabel(),
                Boolean.TRUE.equals(p.getMandatory()),
                p.getState().name(),
                p.getDocumentId(),
                p.getDocumentSha256(),
                p.getFileName(),
                p.getDepositedAt(),
                p.getReviewedAt(),
                p.getRejectionReason()
            );
        }
    }

    record TransitionVue(
        UUID id,
        String de,
        String vers,
        String motif,
        String etat,
        UUID demandeurId,
        Instant demandeeLe,
        int approbationsRequises,
        UUID approbateurId,
        UUID secondApprobateurId,
        Instant decideeLe,
        String motifRejet,
        UUID pieceDecisionId,
        boolean automatique,
        UUID institutionId
    ) {
        static TransitionVue de(StatusTransition t) {
            return new TransitionVue(
                t.getId(),
                t.getFromStatus().name(),
                t.getToStatus().name(),
                t.getReason(),
                t.getState().name(),
                t.getRequesterId(),
                t.getRequestedAt(),
                t.getRequiredApprovals(),
                t.getApproverId(),
                t.getSecondApproverId(),
                t.getDecidedAt(),
                t.getRejectionReason(),
                t.getEvidenceDocumentId(),
                Boolean.TRUE.equals(t.getAutomaticTransition()),
                t.getInstitution() == null ? null : t.getInstitution().getId()
            );
        }
    }

    record PrerequisVue(String code, String libelle, boolean satisfait, String reference) {
        static PrerequisVue de(Prerequis p) {
            return new PrerequisVue(p.code(), p.libelle(), p.satisfait(), p.reference());
        }
    }

    record DossierVue(
        InstitutionVue institution,
        AdhesionVue adhesion,
        List<PieceVue> pieces,
        List<TransitionVue> transitions,
        List<PrerequisVue> prerequisAdhesion,
        List<String> transitionsPermises
    ) {
        static DossierVue de(InstitutionRegistryQueries.Dossier d) {
            return new DossierVue(
                InstitutionVue.de(d.institution()),
                AdhesionVue.de(d.institution()),
                d.pieces().stream().map(PieceVue::de).toList(),
                d.transitions().stream().map(TransitionVue::de).toList(),
                d.prerequisAdhesion().stream().map(PrerequisVue::de).toList(),
                d.transitionsPermises().stream().map(Enum::name).sorted().toList()
            );
        }
    }
}
