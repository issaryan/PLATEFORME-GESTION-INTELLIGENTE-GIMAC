package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.LireDocumentUseCase;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture d'un document déposé par un service interne : métadonnées en base, contenu dans le
 * dépôt WORM. L'empreinte SHA-256 du contenu relu est recalculée et comparée à celle inscrite au
 * dépôt : un contenu absent ou altéré n'est jamais restitué (erreur 500, journalisée). Chaque
 * lecture restituée est journalisée avec le client du service lecteur.
 */
@Service
public class LectureDocumentService implements LireDocumentUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(LectureDocumentService.class);

    private final DepotDocumentRepository repository;
    private final StockageObjet stockage;

    public LectureDocumentService(DepotDocumentRepository repository, StockageObjet stockage) {
        this.repository = repository;
        this.stockage = stockage;
    }

    @Override
    @Transactional(readOnly = true)
    public Contenu lire(UUID id, String lecteur) {
        DepotDocument d = repository.findById(id).orElseThrow(() -> DocumentException.introuvable(id));
        byte[] octets = stockage
            .lire(d.getCleObjet())
            .orElseThrow(() -> {
                LOG.error("Contenu absent du dépôt WORM pour le document {} (clé {})", id, d.getCleObjet());
                return DocumentException.altere("DOC_CONTENT_MISSING", "Le contenu conservé du document est introuvable", id);
            });
        if (!EmpreinteSha256.de(octets).equals(d.getSha256())) {
            LOG.error("Empreinte du contenu relu différente de l'empreinte inscrite pour le document {} (clé {})", id, d.getCleObjet());
            throw DocumentException.altere("DOC_INTEGRITY_MISMATCH", "Le contenu conservé ne correspond plus à son empreinte", id);
        }
        LOG.info("Document {} lu par le service {} : finalité {}, {} octets", id, lecteur, d.getFinalite(), octets.length);
        return new Contenu(id, d.getNomFichier(), d.getTypeMime(), d.getSha256(), octets);
    }
}
