package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Dossier;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Marraine;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Piece;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Instantané d'une institution et de ses pièces, lu par l'évaluateur de prérequis. */
@Component
public class DossierSnapshots {

    private final AdhesionPieceRepository pieces;

    public DossierSnapshots(AdhesionPieceRepository pieces) {
        this.pieces = pieces;
    }

    public Dossier de(Institution i) {
        Institution m = i.getSponsorBank();
        List<Piece> vues = pieces
            .findByInstitutionIdOrderByCode(i.getId())
            .stream()
            .map(DossierSnapshots::vue)
            .toList();
        return new Dossier(
            i.getStatus(),
            i.getParticipantCode(),
            i.getMembershipMode(),
            i.getSettlementAccountRef(),
            i.getRingFencedAccountRef(),
            m == null ? null : new Marraine(m.getParticipantCode(), m.getMembershipMode(), m.getStatus()),
            vues,
            i.getAttestationIssuedAt(),
            i.getGoLiveAuthorizedAt(),
            i.getComplianceState(),
            Objects.requireNonNullElse(i.getOpenLinkCount(), 0),
            Objects.requireNonNullElse(i.getOpenSuspenseCount(), 0)
        );
    }

    private static Piece vue(AdhesionPiece p) {
        return new Piece(p.getCode(), p.getLabel(), Boolean.TRUE.equals(p.getMandatory()), p.getState(), p.getDocumentId());
    }
}
