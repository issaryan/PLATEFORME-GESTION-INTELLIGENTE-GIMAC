package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Piece;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import cm.gimac.des.homologation.domain.sandbox.PieceSandbox.Statut;
import cm.gimac.des.homologation.repository.PieceSandboxRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pièces d'une campagne reprise (US-EXI-14, INT-08, UC-LEC-05) : pour chaque version de pièce encore inconnue,
 * l'empreinte annoncée par la liste, celle annoncée par HEAD et celle des octets téléchargés doivent concorder ; sinon
 * rien n'est déposé et l'écart est consigné. Une pièce de même empreinte déjà conservée (déposée à la clôture par
 * SBX-CAMPAGNE, ou par une relève antérieure) n'est jamais redéposée : MS-HOMOLOGATION ne garde que sa référence.
 */
@Component
public class ConservationPiecesSandbox {

    static final String FINALITE = "SANDBOX_CAMPAGNE";
    private static final Logger LOG = LoggerFactory.getLogger(ConservationPiecesSandbox.class);

    private final LectureSandboxPort lecture;
    private final DocumentRegistryPort documents;
    private final PieceSandboxRepository pieces;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ConservationPiecesSandbox(LectureSandboxPort lecture, DocumentRegistryPort documents, PieceSandboxRepository pieces, TransactionTemplate transaction, Clock clock) {
        this.lecture = lecture;
        this.documents = documents;
        this.pieces = pieces;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** Conserve les pièces nouvelles de la campagne ; rend le nombre de pièces nouvellement inscrites. */
    public int conserver(String campagneId) {
        int n = 0;
        for (Piece p : lecture.pieces(campagneId)) {
            if (!pieces.existsByCampagneIdAndNatureAndVersion(campagneId, p.nature(), p.version()) && inscrire(campagneId, p)) {
                n++;
            }
        }
        return n;
    }

    private boolean inscrire(String campagneId, Piece p) {
        String annonce = lecture.empreinte(campagneId, p.nature(), p.version());
        byte[] octets = lecture.telecharger(campagneId, p.nature(), p.version());
        String calculee = sha256(octets);
        UUID document = null;
        Statut statut;
        if (!p.empreinte().equalsIgnoreCase(annonce) || !p.empreinte().equalsIgnoreCase(calculee)) {
            LOG.warn("Pièce {} v{} de la campagne {} : empreintes divergentes (liste {}, HEAD {}, octets {}), rien n'est déposé", p.nature(), p.version(), campagneId, p.empreinte(), annonce, calculee);
            statut = Statut.EMPREINTE_DIVERGENTE;
        } else {
            Optional<UUID> conservee = pieces.findFirstByEmpreinteAndDocumentIdIsNotNull(calculee).map(PieceSandbox::getDocumentId).or(() -> documents.parEmpreinte(calculee));
            if (conservee.isPresent()) {
                document = conservee.orElseThrow();
                statut = Statut.DEJA_CONSERVEE;
            } else {
                String nom = p.nom() == null ? p.nature().toLowerCase(java.util.Locale.ROOT) + "-v" + p.version() : p.nom();
                document = documents.deposer(octets, nom, "application/json", FINALITE, campagneId + "/" + p.nature() + "/" + p.version(), null).id();
                statut = Statut.DEPOSEE;
            }
        }
        PieceSandbox piece = new PieceSandbox(campagneId, p.nature(), p.version(), calculee, octets.length, p.nom(), document, statut, clock.instant());
        try {
            transaction.executeWithoutResult(s -> pieces.saveAndFlush(piece));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Relève concurrente (notification et relève périodique) : la pièce est déjà inscrite.
            return false;
        }
    }

    static String sha256(byte[] octets) {
        return condense(octets, "SHA-256");
    }

    /** Condensé hexadécimal par l'algorithme nommé ; algorithme absent de la plateforme Java : IllegalStateException. */
    static String condense(byte[] octets, String algorithme) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithme).digest(octets));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithme + " indisponible", e);
        }
    }
}
