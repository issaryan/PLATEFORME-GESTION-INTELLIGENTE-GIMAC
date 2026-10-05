package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.DocumentPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution des documents (développement, tests) : dépôts fragmentés simulés,
 * conservés en mémoire avec leur finalité, leur référence et leur déposant.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "stub", matchIfMissing = true)
public class DocumentStubAdapter implements DocumentPort {

    private record Depose(byte[] contenu, String nomFichier, String typeMime, String finalite, String reference, String deposant) {}

    private final Map<UUID, Depose> documents = new ConcurrentHashMap<>();

    /** Dépôt fragmenté simulé (docs/lot1/depots-fragmentes.md §3) ; rend le ticket. */
    public UUID deposer(String nomFichier, String typeMime, byte[] contenu, String finalite, String reference, String deposant) {
        UUID id = UUID.randomUUID();
        documents.put(id, new Depose(contenu.clone(), nomFichier, typeMime, finalite, reference, deposant));
        return id;
    }

    @Override
    public Optional<Metadonnees> metadonnees(UUID documentId) {
        return Optional.ofNullable(documents.get(documentId)).map(d ->
            new Metadonnees(documentId, d.nomFichier(), d.contenu().length, d.typeMime(), null, d.finalite(), d.reference(), d.deposant())
        );
    }

    @Override
    public Contenu lire(UUID documentId) {
        Depose d = documents.get(documentId);
        if (d == null) {
            throw PilotageException.introuvable("PIL_DOCUMENT_ABSENT", documentId);
        }
        return new Contenu(new ByteArrayInputStream(d.contenu()), d.contenu().length);
    }
}
