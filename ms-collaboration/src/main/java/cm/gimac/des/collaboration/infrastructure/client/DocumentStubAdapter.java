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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution du dépôt documentaire (développement, tests) : documents conservés en
 * mémoire, empreinte SHA-256 calculée comme le ferait MS-DOCUMENT. Les métadonnées rendues sont celles
 * des documents déposés ici, par {@link #deposer} ou par {@link #deposerFragmente} (dépôt fragmenté
 * simulé, D-74).
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "stub", matchIfMissing = true)
public class DocumentStubAdapter implements DocumentStoragePort {

    /** Document déposé : contenu et métadonnées du dépôt (nom, finalité, référence, déposant, type, empreinte). */
    public record Depose(byte[] contenu, String finalite, String reference, String deposantId, String typeMime, String nomFichier, String sha256) {}

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
        String empreinte = sha256(contenu);
        documents.put(id, new Depose(contenu, FINALITE, messageId.toString(), deposantId, fichier.typeMime(), fichier.nom(), empreinte));
        return new DocumentDepose(id, empreinte, fichier.nom(), contenu.length, fichier.typeMime());
    }

    /**
     * Dépôt fragmenté simulé (docs/lot1/depots-fragmentes.md §3) : document vérifié, de la finalité et
     * du déposant donnés ; rend le ticket (identifiant du document).
     */
    public UUID deposerFragmente(String nomFichier, String typeMime, byte[] contenu, String finalite, String deposantId) {
        UUID id = UUID.randomUUID();
        documents.put(id, new Depose(contenu.clone(), finalite, null, deposantId, typeMime, nomFichier, sha256(contenu)));
        return id;
    }

    @Override
    public Optional<MetadonneesDocument> metadonnees(UUID documentId) {
        return Optional.ofNullable(documents.get(documentId)).map(d ->
            new MetadonneesDocument(documentId, d.nomFichier(), d.contenu().length, d.typeMime(), d.sha256(), d.finalite(), d.reference(), d.deposantId())
        );
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
