package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.ControleNumerosCarte;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.domain.service.ValidationDepot;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

/**
 * Dépôt direct d'un fichier (tranche DDRM) : admission (présence, taille, finalité, référence),
 * empreinte SHA-256 calculée par le serveur, contrôle de masquage des numéros de carte (BNF-16),
 * conservation dans le dépôt objet WORM, puis inscription des métadonnées. Même chaîne pour le
 * dépôt direct d'un utilisateur et pour le dépôt d'un service interne pour le compte d'un
 * utilisateur (pièces jointes de la messagerie, D-64) : le service déposant est alors inscrit.
 *
 * L'objet est écrit avant la ligne de métadonnées : si l'écriture en base échoue, l'objet reste
 * orphelin dans le dépôt WORM (il ne peut pas être supprimé) et sa clé est journalisée pour le
 * contrôle d'intégrité ; l'inverse laisserait une référence vers un contenu absent.
 */
@Service
public class DepotDocumentService implements DeposerDocumentUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(DepotDocumentService.class);
    private static final DateTimeFormatter DATE_CLE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final StockageObjet stockage;
    private final ExtracteurTexte extracteur;
    private final DepotDocumentRepository repository;
    private final long tailleMax;
    private final Clock horloge;

    @Autowired
    public DepotDocumentService(
        StockageObjet stockage,
        ExtracteurTexte extracteur,
        DepotDocumentRepository repository,
        @Value("${gimac.document.depot.taille-max:20MB}") DataSize tailleMax
    ) {
        this(stockage, extracteur, repository, tailleMax, Clock.systemUTC());
    }

    DepotDocumentService(StockageObjet stockage, ExtracteurTexte extracteur, DepotDocumentRepository repository, DataSize tailleMax, Clock horloge) {
        this.stockage = stockage;
        this.extracteur = extracteur;
        this.repository = repository;
        this.tailleMax = tailleMax.toBytes();
        this.horloge = horloge;
    }

    @Override
    @Transactional
    public Resultat deposer(Commande commande, Deposant deposant) {
        Fichier fichier = commande.fichier();
        byte[] contenu = fichier == null ? null : fichier.contenu();
        ValidationDepot.verifierContenu(contenu, tailleMax);
        String finalite = ValidationDepot.finalite(commande.finalite());
        String reference = ValidationDepot.reference(commande.reference());
        String nomFichier = ValidationDepot.nomFichier(fichier.nomFichier());
        String typeMime = ValidationDepot.typeMime(fichier.typeMime(), contenu);
        // Dépôt par un service interne : l'utilisateur pour le compte duquel il dépose est transmis, donc contrôlé.
        String deposantId = deposant.service() == null ? deposant.id() : ValidationDepot.deposantId(deposant.id());

        // BNF-16 : aucune donnée de carte dans une pièce ; le rejet ne cite jamais le numéro trouvé.
        int numeros = extracteur.extraire(contenu, typeMime).map(ControleNumerosCarte::compter).orElse(0);
        if (numeros > 0) {
            LOG.warn("Dépôt refusé : {} numéro(s) de carte détecté(s), finalité {}", numeros, finalite);
            throw DocumentException.refuse(
                "DOC_PAN_DETECTED",
                "La pièce contient un numéro de carte non masqué",
                Map.of("occurrences", numeros)
            );
        }

        String sha256 = EmpreinteSha256.de(contenu);
        UUID id = UUID.randomUUID();
        Instant maintenant = horloge.instant();
        String cle = finalite.toLowerCase(Locale.ROOT) + "/" + DATE_CLE.format(maintenant) + "/" + id;
        StockageObjet.ObjetDepose objet = stockage.deposer(cle, contenu, typeMime, sha256);
        try {
            repository.saveAndFlush(
                new DepotDocument()
                    .id(id)
                    .finalite(finalite)
                    .reference(reference)
                    .nomFichier(nomFichier)
                    .taille(contenu.length)
                    .typeMime(typeMime)
                    .sha256(sha256)
                    .cleObjet(objet.cle())
                    .versionObjet(objet.version())
                    .deposant(deposantId)
                    .institutionId(deposant.institutionId())
                    .serviceDeposant(deposant.service())
                    .deposeLe(maintenant)
            );
        } catch (RuntimeException e) {
            LOG.error("Objet orphelin dans le dépôt WORM : métadonnées non inscrites pour la clé {}", objet.cle());
            throw e;
        }
        if (deposant.service() == null) {
            LOG.info("Document {} déposé : finalité {}, {} octets, {}", id, finalite, contenu.length, typeMime);
        } else {
            LOG.info("Document {} déposé par le service {} : finalité {}, {} octets, {}", id, deposant.service(), finalite, contenu.length, typeMime);
        }
        return new Resultat(id, sha256, nomFichier, contenu.length, typeMime);
    }

    @Override
    @Transactional(readOnly = true)
    public String empreinte(UUID id) {
        return repository.findSha256ById(id).orElseThrow(() -> DocumentException.introuvable(id));
    }
}
