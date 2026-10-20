package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.PiecesPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Substitut des pièces de MS-DOCUMENT (développement, tests ; jamais en production) : pièces posées par les tests. */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "stub", matchIfMissing = true)
public class PiecesStubAdapter implements PiecesPort {

    private final Map<String, Piece> pieces = new ConcurrentHashMap<>();
    private final Map<String, byte[]> contenus = new ConcurrentHashMap<>();

    public void poser(Piece piece, byte[] contenu) {
        pieces.put(piece.id(), piece);
        contenus.put(piece.id(), contenu);
    }

    public void effacer() {
        pieces.clear();
        contenus.clear();
    }

    @Override
    public Optional<Piece> lire(String id) {
        return Optional.ofNullable(id == null ? null : pieces.get(id));
    }

    @Override
    public byte[] contenu(String id) {
        byte[] c = contenus.get(id);
        if (c == null) {
            throw MonitoringException.introuvable("MON_PIECE_INTROUVABLE", id);
        }
        return c.clone();
    }
}
