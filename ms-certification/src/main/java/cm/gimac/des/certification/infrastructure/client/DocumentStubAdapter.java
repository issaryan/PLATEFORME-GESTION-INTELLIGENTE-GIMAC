package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.DocumentPort;
import cm.gimac.des.certification.domain.service.DecisionCanonique;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de MS-DOCUMENT (développement, tests) : le dépôt calcule l'empreinte et garde la référence en
 * mémoire ; l'empreinte d'un document déposé ailleurs (interface branchée sur le vrai MS-DOCUMENT) est
 * admise telle que déclarée, faute de pouvoir la relire.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.document-deposit", havingValue = "stub", matchIfMissing = true)
public class DocumentStubAdapter implements DocumentPort {

    private final Map<UUID, String> empreintes = new ConcurrentHashMap<>();

    @Override
    public boolean empreinteConforme(UUID documentId, String sha256) {
        String connue = empreintes.get(documentId);
        return connue == null || connue.equalsIgnoreCase(sha256);
    }

    @Override
    public DocumentDepose deposer(Fichier fichier, String finalite, String reference, String deposantId) {
        UUID id = UUID.randomUUID();
        String sha = DecisionCanonique.sha256(fichier.octets());
        empreintes.put(id, sha);
        return new DocumentDepose(id.toString(), sha);
    }

    /** Tests : document connu avec son empreinte. */
    public void connaitre(UUID documentId, String sha256) {
        empreintes.put(documentId, sha256);
    }
}
