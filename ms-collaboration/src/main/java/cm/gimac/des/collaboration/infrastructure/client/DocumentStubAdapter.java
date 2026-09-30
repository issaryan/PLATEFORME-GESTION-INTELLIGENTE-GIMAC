package cm.gimac.des.collaboration.infrastructure.client;

import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution du dépôt documentaire (développement, tests) : documents conservés en
 * mémoire, empreinte SHA-256 calculée comme le ferait MS-DOCUMENT.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "stub", matchIfMissing = true)
public class DocumentStubAdapter implements DocumentStoragePort {

    /** Document déposé : contenu et métadonnées du dépôt (finalité, référence, déposant). */
    public record Depose(byte[] contenu, String finalite, String reference, String deposantId, String typeMime) {}

    private final Map<UUID, Depose> documents = new ConcurrentHashMap<>();

    @Override
    public DocumentDepose deposer(FichierJoint fichier, UUID messageId, String deposantId) {
        byte[] contenu;
        try (InputStream in = fichier.source().ouvrir()) {
            contenu = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        UUID id = UUID.randomUUID();
        documents.put(id, new Depose(contenu, FINALITE, messageId.toString(), deposantId, fichier.typeMime()));
        return new DocumentDepose(id, sha256(contenu), fichier.nom(), contenu.length, fichier.typeMime());
    }

    @Override
    public ContenuDocument lire(UUID documentId) {
        Depose d = documents.get(documentId);
        if (d == null) {
            throw CollaborationException.introuvable("COL_ATTACHMENT_NOT_FOUND", documentId);
        }
        return new ContenuDocument(new ByteArrayInputStream(d.contenu()), d.contenu().length);
    }

    public Depose depose(UUID documentId) {
        return documents.get(documentId);
    }

    static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
