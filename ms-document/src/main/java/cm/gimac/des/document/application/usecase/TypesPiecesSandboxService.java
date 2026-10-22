package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.out.PiecesSandbox;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import java.time.Clock;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue des types de pièces de la sandbox depuis la console (US-EXI-08, D-120) : lecture des types et de leur
 * politique de rétention, modification de la rétention (motif, version lue, plancher de dix ans pour un type
 * probant), supervision des pièces en quarantaine.
 */
public class TypesPiecesSandboxService {

    private static final Logger LOG = LoggerFactory.getLogger(TypesPiecesSandboxService.class);
    public static final int TAILLE_MAX = 100;

    public record PageQuarantaines(List<PieceSandbox> contenu, long total, int page, int taille) {}

    private final PiecesSandbox pieces;
    private final Clock horloge;

    public TypesPiecesSandboxService(PiecesSandbox pieces, Clock horloge) {
        this.pieces = pieces;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public List<TypePieceSandbox> types() {
        return pieces.types();
    }

    /** Nouvelle rétention (période ISO 8601, ex. P10Y) ; 409 si le type a changé depuis sa lecture. */
    @Transactional
    public TypePieceSandbox modifierRetention(String code, String retention, String motif, int versionLue, String auteur) {
        TypePieceSandbox type = pieces.type(code).orElseThrow(() -> DocumentException.introuvable(code));
        if (motif == null || motif.isBlank() || motif.length() > 500) {
            throw DocumentException.invalide("DOC_SBX_MOTIF_REQUIS", "Motif obligatoire, 500 caractères au plus");
        }
        Period p;
        try {
            p = Period.parse(retention == null ? "" : retention.trim());
        } catch (DateTimeParseException e) {
            throw DocumentException.invalide("DOC_SBX_RETENTION_INVALIDE", "Rétention attendue en période ISO 8601 (P10Y)");
        }
        String refus = TypePieceSandbox.refusRetention(p, type.probante());
        if (refus != null) {
            throw DocumentException.invalide(refus, "Rétention refusée pour ce type", Map.of("minimum", TypePieceSandbox.RETENTION_MIN_PROBANTE.toString()));
        }
        TypePieceSandbox modifie = type.avecRetention(p);
        if (type.version() != versionLue || !pieces.modifierRetention(modifie, versionLue, auteur, motif.trim(), horloge.instant())) {
            throw DocumentException.conflit("DOC_SBX_VERSION_PERIMEE", "Le type a été modifié entre-temps", Map.of("version", type.version()));
        }
        LOG.info("Rétention du type {} portée à {} par {} : {}", code, p, auteur, motif.trim());
        return modifie;
    }

    @Transactional(readOnly = true)
    public PageQuarantaines quarantaines(int page, int taille) {
        if (page < 0 || taille < 1 || taille > TAILLE_MAX) {
            throw DocumentException.invalide("DOC_SBX_PAGINATION_INVALIDE", "Page ou taille invalide");
        }
        return new PageQuarantaines(pieces.quarantaines(page, taille), pieces.nombreQuarantaines(), page, taille);
    }
}
