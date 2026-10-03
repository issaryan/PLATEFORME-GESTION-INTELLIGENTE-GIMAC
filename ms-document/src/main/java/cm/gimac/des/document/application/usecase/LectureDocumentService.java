package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.LireDocumentUseCase;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture d'un document par un service interne : métadonnées en base, contenu dans le dépôt
 * WORM, servi en flux (D-74 : documents de plusieurs gigaoctets, jamais chargés en mémoire). Un
 * objet absent ou d'une autre taille que la taille inscrite n'est pas servi (erreur 500,
 * journalisée). L'empreinte SHA-256 est recalculée au fil de la lecture et comparée à celle
 * inscrite avant les derniers octets ({@link FluxVerifie}) : un contenu altéré interrompt la
 * réponse et n'est jamais restitué en entier ; l'écart est journalisé en erreur. Chaque lecture
 * est journalisée avec le client du service lecteur.
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
        Flux flux = stockage
            .lireFlux(d.getCleObjet())
            .orElseThrow(() -> {
                LOG.error("Contenu absent du dépôt WORM pour le document {} (clé {})", id, d.getCleObjet());
                return DocumentException.altere("DOC_CONTENT_MISSING", "Le contenu conservé du document est introuvable", id);
            });
        if (flux.taille() != d.getTaille()) {
            fermer(flux);
            LOG.error("Taille du contenu conservé ({}) différente de la taille inscrite ({}) pour le document {} (clé {})", flux.taille(), d.getTaille(), id, d.getCleObjet());
            throw DocumentException.altere("DOC_INTEGRITY_MISMATCH", "Le contenu conservé ne correspond plus à son empreinte", id);
        }
        LOG.info("Document {} lu par le service {} : finalité {}, {} octets", id, lecteur, d.getFinalite(), d.getTaille());
        FluxVerifie verifie = new FluxVerifie(flux.contenu(), d.getSha256(), d.getTaille(), motif ->
            LOG.error("Lecture du document {} interrompue (clé {}) : {}", id, d.getCleObjet(), motif)
        );
        return new Contenu(id, d.getNomFichier(), d.getTypeMime(), d.getSha256(), d.getTaille(), verifie);
    }

    @Override
    @Transactional(readOnly = true)
    public Metadonnees metadonnees(UUID id) {
        DepotDocument d = repository.findById(id).orElseThrow(() -> DocumentException.introuvable(id));
        return new Metadonnees(
            d.getId(),
            d.getNomFichier(),
            d.getTaille(),
            d.getTypeMime(),
            d.getSha256(),
            d.getFinalite(),
            d.getReference(),
            d.getDeposant(),
            d.getInstitutionId(),
            d.getDeposeLe()
        );
    }

    private static void fermer(Flux flux) {
        try {
            flux.contenu().close();
        } catch (IOException e) {
            LOG.debug("Fermeture du flux : {}", e.getMessage());
        }
    }
}
