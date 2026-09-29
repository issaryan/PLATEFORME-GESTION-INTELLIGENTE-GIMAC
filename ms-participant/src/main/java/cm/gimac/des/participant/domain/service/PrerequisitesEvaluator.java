package cm.gimac.des.participant.domain.service;

import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Prérequis de chaque état d'arrivée (CC algorithme 6.25, étape 4 ; PAR-03), évalués à la
 * proposition puis de nouveau à l'approbation.
 *
 * <p>ADHERENT (D-57, CR-CERT rang 2) : pièces obligatoires du dossier validées, code participant
 * attribué, mode d'adhésion connu ; adhésion directe avec compte de règlement (RG-11), indirecte
 * avec compte de cantonnement (RG-12) et banque marraine membre direct actif (PAR-01, critère 4).
 * ACTIF : attestation et autorisation de mise en service reçues, conformité CONFORME.
 * SUSPENDU : pièce de décision prise hors plateforme. RETIRE : ni liaison ni suspens ouverts.
 * CLOS_SANS_SUITE : aucune liaison ouverte.
 *
 * <p>Service de domaine pur : il ne lit que l'instantané qu'on lui passe.
 */
public final class PrerequisitesEvaluator {

    /** Pièce du dossier d'adhésion, vue par l'évaluateur. */
    public record Piece(String code, String libelle, boolean obligatoire, AdhesionPieceState etat, UUID documentId) {}

    /** Banque marraine, vue par l'évaluateur. */
    public record Marraine(String participantCode, MembershipMode mode, InstitutionStatus statut) {}

    /** Instantané du dossier d'une institution. */
    public record Dossier(
        InstitutionStatus statut,
        String participantCode,
        MembershipMode mode,
        String compteReglement,
        String compteCantonnement,
        Marraine marraine,
        List<Piece> pieces,
        Instant attestationRecue,
        Instant miseEnServiceAutorisee,
        ComplianceState conformite,
        int liaisonsOuvertes,
        int suspensOuverts
    ) {
        public Dossier {
            pieces = pieces == null ? List.of() : List.copyOf(pieces);
        }
    }

    /** Un prérequis évalué : satisfait ou non, avec la référence qui le fonde. */
    public record Prerequis(String code, String libelle, boolean satisfait, String reference) {}

    private PrerequisitesEvaluator() {}

    public static List<Prerequis> evaluer(Dossier d, InstitutionStatus cible, UUID pieceDecision) {
        List<Prerequis> r = new ArrayList<>();
        switch (cible) {
            case ADHERENT -> adhesion(d, r);
            case ACTIF -> {
                r.add(p("ATTESTATION_RECUE", "Attestation de certification reçue", d.attestationRecue() != null, texte(d.attestationRecue())));
                r.add(p("MISE_EN_SERVICE_AUTORISEE", "Mise en service autorisée", d.miseEnServiceAutorisee() != null, texte(d.miseEnServiceAutorisee())));
                r.add(p("CONFORMITE", "État de conformité documentaire CONFORME", d.conformite() == ComplianceState.CONFORME, texte(d.conformite())));
            }
            case SUSPENDU -> r.add(p("PIECE_DECISION", "Pièce de la décision de suspension déposée", pieceDecision != null, texte(pieceDecision)));
            case RETIRE -> {
                r.add(p("AUCUNE_LIAISON_OUVERTE", "Aucune liaison ouverte", d.liaisonsOuvertes() == 0, String.valueOf(d.liaisonsOuvertes())));
                r.add(p("AUCUN_SUSPENS_OUVERT", "Aucun suspens ouvert", d.suspensOuverts() == 0, String.valueOf(d.suspensOuverts())));
                r.add(p("PIECE_DECISION", "Pièce de la décision de retrait déposée", pieceDecision != null, texte(pieceDecision)));
            }
            case CLOS_SANS_SUITE -> r.add(
                p("AUCUNE_LIAISON_OUVERTE", "Aucune liaison ouverte", d.liaisonsOuvertes() == 0, String.valueOf(d.liaisonsOuvertes()))
            );
            default -> {
                // EN_HOMOLOGATION est automatique ; A_REGULARISER n'a pas de prérequis (D-13).
            }
        }
        return List.copyOf(r);
    }

    public static List<Prerequis> manquants(List<Prerequis> prerequis) {
        return prerequis.stream().filter(x -> !x.satisfait()).toList();
    }

    private static void adhesion(Dossier d, List<Prerequis> r) {
        for (Piece piece : d.pieces()) {
            if (piece.obligatoire()) {
                r.add(
                    p(
                        "PIECE_" + piece.code(),
                        piece.libelle(),
                        piece.etat() == AdhesionPieceState.VALIDEE,
                        piece.documentId() == null ? piece.etat().name() : piece.documentId().toString()
                    )
                );
            }
        }
        r.add(p("CODE_PARTICIPANT", "Code participant attribué", present(d.participantCode()), d.participantCode()));
        r.add(p("MODE_ADHESION", "Mode d'adhésion défini", d.mode() != null, texte(d.mode())));
        if (d.mode() == MembershipMode.DIRECT) {
            r.add(p("COMPTE_REGLEMENT", "Compte de règlement à la BEAC (RG-11)", present(d.compteReglement()), d.compteReglement()));
        } else if (d.mode() == MembershipMode.INDIRECT) {
            Marraine m = d.marraine();
            boolean eligible = m != null && m.mode() == MembershipMode.DIRECT && m.statut() == InstitutionStatus.ACTIF;
            r.add(p("BANQUE_MARRAINE", "Banque marraine membre direct actif", eligible, m == null ? null : m.participantCode()));
            r.add(p("COMPTE_CANTONNEMENT", "Compte de cantonnement (RG-12)", present(d.compteCantonnement()), d.compteCantonnement()));
        }
    }

    private static Prerequis p(String code, String libelle, boolean ok, String reference) {
        return new Prerequis(code, libelle, ok, reference);
    }

    private static boolean present(String s) {
        return s != null && !s.isBlank();
    }

    private static String texte(Object o) {
        return o == null ? null : o.toString();
    }
}
