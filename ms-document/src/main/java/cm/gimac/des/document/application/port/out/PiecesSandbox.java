package cm.gimac.des.document.application.port.out;

import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Catalogue des types de pièces de la sandbox, pièces inscrites et événements à publier (US-EXI-08, D-120). */
public interface PiecesSandbox {
    // ------------------------------------------------------------------ catalogue
    List<TypePieceSandbox> types();

    Optional<TypePieceSandbox> type(String code);

    /** Enregistre la rétention modifiée si la version lue est toujours la version courante ; faux sinon. */
    boolean modifierRetention(TypePieceSandbox modifie, int versionLue, String auteur, String motif, Instant le);

    // ------------------------------------------------------------------ pièces
    /** Pièce scellée de même institution, même type et même empreinte (déduplication des dépôts rejoués). */
    Optional<PieceSandbox> scellee(String codeParticipant, String type, String sha256);

    void inscrire(PieceSandbox piece);

    /** Pièces en quarantaine, les plus récentes d'abord. */
    List<PieceSandbox> quarantaines(int page, int taille);

    long nombreQuarantaines();

    // ------------------------------------------------------------------ outbox
    /** Écrit l'événement dans l'outbox, dans la transaction de la pièce. */
    void publier(String type, String cle, String donneesJson, Instant le);
}
