package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.application.port.out.StockageObjet.Partie;
import cm.gimac.des.document.domain.DepotFragmente;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import cm.gimac.des.document.domain.service.ValidationDepot;
import cm.gimac.des.document.repository.DepotFragmenteRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dépôts fragmentés (claim check, D-74, docs/lot1/depots-fragmentes.md §3), côté client :
 * ouverture (admission, permission selon la finalité, téléversement multipart dans le compartiment
 * des dépôts en cours), réception des parties (taille exacte, renvoi accepté pour la reprise),
 * fin (assemblage, puis vérification par {@link VerificationDepotService}), abandon, et abandon
 * des dépôts ouverts restés sans activité.
 *
 * Les octets d'une partie sont envoyés au dépôt objet hors transaction ; la partie n'est inscrite
 * qu'ensuite, la ligne du dépôt verrouillée, après avoir revérifié qu'il est toujours ouvert. Fin
 * et abandon verrouillent la même ligne : une partie arrivée après la fin n'est jamais inscrite.
 */
@Service
public class DepotFragmenteService implements DepotFragmenteUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(DepotFragmenteService.class);

    /** Permissions du dépôt direct (POST /api/v1/documents), exigées pour toute finalité autre que la messagerie. */
    static final Set<String> PERMISSIONS_DEPOT = Set.of(
        "PAR_ADHESION_INSTRUCT",
        "HOM_EVIDENCE_SUBMIT",
        "HOM_STEP_MANAGE",
        "CER_CHANGE_RECORD_LINK",
        "HOM_PORTAL_SUBMIT",
        "DOC_DEPOSIT"
    );
    static final String FINALITE_MESSAGERIE = "MESSAGERIE";
    static final String PERMISSION_MESSAGERIE = "COL_MESSAGE_SEND";
    /** Rapport d'activité déposé en mode DOCUMENT (D-76, docs/lot1/mon-activite.md §2.4), référence = identifiant du rapport. */
    static final String FINALITE_RAPPORT_ACTIVITE = "RAPPORT_ACTIVITE";
    static final String PERMISSION_RAPPORT_ACTIVITE = "PIL_ACTIVITY_OWN";
    /**
     * Livrable d'une étape de processus d'unité (D-87, docs/lot1/processus-unite.md §5.3), déposé par l'exécutant,
     * référence = identifiant de l'instance ; MS-PILOTAGE le vérifie par GET /api/v1/interne/documents/{id}.
     */
    static final String FINALITE_PROCESSUS_UNITE = "PROCESSUS_UNITE";
    /** Finalités réservées à l'activité interne (MS-PILOTAGE) : PIL_ACTIVITY_OWN seulement. */
    static final Set<String> FINALITES_ACTIVITE = Set.of(FINALITE_RAPPORT_ACTIVITE, FINALITE_PROCESSUS_UNITE);
    private static final int LOT = 100;

    private final StockageObjet stockage;
    private final DepotFragmenteRepository depots;
    private final TransactionTemplate transactions;
    private final TransactionTemplate lectures;
    private final DepotFragmenteProprietes proprietes;
    private final Clock horloge;

    @Autowired
    public DepotFragmenteService(
        StockageObjet stockage,
        DepotFragmenteRepository depots,
        PlatformTransactionManager transactions,
        DepotFragmenteProprietes proprietes
    ) {
        this(stockage, depots, transactions, proprietes.verifier(), Clock.systemUTC());
    }

    DepotFragmenteService(
        StockageObjet stockage,
        DepotFragmenteRepository depots,
        PlatformTransactionManager transactions,
        DepotFragmenteProprietes proprietes,
        Clock horloge
    ) {
        this.stockage = stockage;
        this.depots = depots;
        this.transactions = new TransactionTemplate(transactions);
        this.lectures = new TransactionTemplate(transactions);
        this.lectures.setReadOnly(true);
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    @Override
    public Etat ouvrir(Ouverture ouverture, Demandeur demandeur) {
        String finalite = ValidationDepot.finalite(ouverture.finalite());
        autoriser(finalite, demandeur.permissions());
        String reference = ValidationDepot.reference(ouverture.reference());
        long taille = taille(ouverture.taille());
        String nomFichier = ValidationDepot.nomFichier(ouverture.nomFichier());
        String typeMime = ValidationDepot.typeMime(ouverture.typeMime(), null);
        long taillePartie = proprietes.taillePartie().toBytes();

        UUID id = UUID.randomUUID();
        Instant maintenant = horloge.instant();
        String cle = id.toString();
        String envoi = stockage.ouvrirDepot(cle, typeMime);
        DepotFragmente d = new DepotFragmente()
            .id(id)
            .etat(EtatDepotFragmente.OUVERT)
            .finalite(finalite)
            .reference(reference)
            .nomFichier(nomFichier)
            .typeMime(typeMime)
            .taille(taille)
            .taillePartie(taillePartie)
            .nombreParties((int) proprietes.nombreParties(taille))
            .envoi(envoi)
            .cleDepot(cle)
            .deposant(demandeur.id())
            .institutionId(demandeur.institutionId())
            .ouvertLe(maintenant)
            .actifLe(maintenant);
        // Si l'inscription échoue, le téléversement ouvert est abandonné par la règle de cycle de vie du compartiment.
        Etat etat = transactions.execute(s -> vue(depots.save(d)));
        LOG.info("Dépôt fragmenté {} ouvert : finalité {}, {} octets en {} parties", id, finalite, taille, d.getNombreParties());
        return etat;
    }

    @Override
    public void recevoirPartie(UUID id, int numero, InputStream corps, long longueurAnnoncee, String deposant) {
        DepotFragmente d = depots.findByIdAndDeposant(id, deposant).orElseThrow(() -> DocumentException.depotIntrouvable(id));
        ouvert(d);
        if (numero < 1 || numero > d.getNombreParties()) {
            throw DocumentException.invalide(
                "DOC_PART_NUMBER_INVALID",
                "Numéro de partie hors du dépôt",
                Map.of("numero", numero, "nombreParties", d.getNombreParties())
            );
        }
        long attendue = d.tailleAttendue(numero);
        if (longueurAnnoncee >= 0 && longueurAnnoncee != attendue) {
            throw tailleDePartie(numero, attendue, longueurAnnoncee);
        }
        byte[] octets = lire(corps, attendue);
        if (octets.length != attendue) {
            throw tailleDePartie(numero, attendue, octets.length);
        }
        String etag = stockage.deposerPartie(d.getCleDepot(), d.getEnvoi(), numero, octets);
        transactions.executeWithoutResult(s -> {
            DepotFragmente v = depots.verrouiller(id, deposant).orElseThrow(() -> DocumentException.depotIntrouvable(id));
            ouvert(v);
            v.getParties().put(numero, etag);
            v.actifLe(horloge.instant());
        });
    }

    @Override
    public Etat etat(UUID id, String deposant) {
        return lectures.execute(s -> vue(depots.findByIdAndDeposant(id, deposant).orElseThrow(() -> DocumentException.depotIntrouvable(id))));
    }

    @Override
    public Etat terminer(UUID id, String deposant) {
        return transactions.execute(s -> {
            DepotFragmente d = depots.verrouiller(id, deposant).orElseThrow(() -> DocumentException.depotIntrouvable(id));
            if (d.getEtat() == EtatDepotFragmente.EN_VERIFICATION || d.getEtat() == EtatDepotFragmente.DISPONIBLE) {
                // Fin renvoyée après une réponse perdue : l'état courant, sans nouvel assemblage.
                return vue(d);
            }
            ouvert(d);
            List<Integer> manquantes = d.partiesManquantes();
            if (!manquantes.isEmpty()) {
                throw DocumentException.conflit(
                    "DOC_UPLOAD_INCOMPLETE",
                    "Des parties du fichier n'ont pas été reçues",
                    Map.of("manquantes", manquantes, "nombreParties", d.getNombreParties())
                );
            }
            List<Partie> parties = d.partiesRecues().stream().map(n -> new Partie(n, d.getParties().get(n))).toList();
            stockage.assemblerDepot(d.getCleDepot(), d.getEnvoi(), parties);
            d.etat(EtatDepotFragmente.EN_VERIFICATION).actifLe(horloge.instant()).repriseLe(null);
            LOG.info("Dépôt fragmenté {} assemblé ({} parties) : vérification en attente", id, parties.size());
            return vue(d);
        });
    }

    @Override
    public void abandonner(UUID id, String deposant) {
        DepotFragmente d = transactions.execute(s -> {
            DepotFragmente v = depots.verrouiller(id, deposant).orElseThrow(() -> DocumentException.depotIntrouvable(id));
            if (v.getEtat() == EtatDepotFragmente.ABANDONNE) {
                return null;
            }
            ouvert(v);
            v.etat(EtatDepotFragmente.ABANDONNE).actifLe(horloge.instant());
            return v;
        });
        if (d != null) {
            abandonnerEnvoi(d);
            LOG.info("Dépôt fragmenté {} abandonné par le déposant", id);
        }
    }

    /**
     * Abandon des dépôts ouverts restés sans activité au-delà de {@code gimac.document.depot.inactivite}
     * (24 h par défaut) ; leur téléversement multipart est abandonné.
     */
    @Scheduled(
        fixedDelayString = "${gimac.document.depot.intervalle-expiration:PT10M}",
        initialDelayString = "${gimac.document.depot.intervalle-expiration:PT10M}"
    )
    public int abandonnerInactifs() {
        Instant seuil = horloge.instant().minus(proprietes.inactivite());
        int n = 0;
        for (UUID id : depots.inactifs(EtatDepotFragmente.OUVERT, seuil, PageRequest.of(0, LOT))) {
            DepotFragmente d = transactions.execute(s -> {
                DepotFragmente v = depots.verrouiller(id).orElse(null);
                if (v == null || v.getEtat() != EtatDepotFragmente.OUVERT || !v.getActifLe().isBefore(seuil)) {
                    return null;
                }
                return v.etat(EtatDepotFragmente.ABANDONNE);
            });
            if (d != null) {
                abandonnerEnvoi(d);
                n++;
            }
        }
        if (n > 0) {
            LOG.info("{} dépôt(s) fragmenté(s) abandonné(s) faute d'activité depuis {}", n, proprietes.inactivite());
        }
        return n;
    }

    /** Le téléversement abandonné est aussi purgé par la règle de cycle de vie du compartiment : un échec n'est que journalisé. */
    private void abandonnerEnvoi(DepotFragmente d) {
        try {
            stockage.abandonnerDepot(d.getCleDepot(), d.getEnvoi());
        } catch (DocumentException e) {
            LOG.warn("Téléversement du dépôt {} non abandonné ({}) : purgé par le cycle de vie du compartiment", d.getId(), e.code());
        }
    }

    /**
     * MESSAGERIE exige COL_MESSAGE_SEND ; RAPPORT_ACTIVITE (D-76) et PROCESSUS_UNITE (D-87) exigent
     * PIL_ACTIVITY_OWN ; toute autre finalité, l'une des permissions du dépôt direct.
     */
    static void autoriser(String finalite, Set<String> permissions) {
        boolean autorise;
        if (FINALITE_MESSAGERIE.equals(finalite)) {
            autorise = permissions.contains(PERMISSION_MESSAGERIE);
        } else if (FINALITES_ACTIVITE.contains(finalite)) {
            autorise = permissions.contains(PERMISSION_RAPPORT_ACTIVITE);
        } else {
            autorise = permissions.stream().anyMatch(PERMISSIONS_DEPOT::contains);
        }
        if (!autorise) {
            throw DocumentException.interdit("DOC_PURPOSE_FORBIDDEN", "Dépôt non autorisé pour cette finalité");
        }
    }

    private long taille(Long taille) {
        if (taille == null || taille < 0) {
            throw DocumentException.invalide("DOC_SIZE_INVALID", "Taille du fichier absente ou négative");
        }
        if (taille == 0) {
            throw DocumentException.invalide("DOC_FILE_EMPTY", "Le fichier est vide");
        }
        long max = proprietes.tailleMaxFragmente().toBytes();
        if (taille > max) {
            throw DocumentException.tropVolumineux(taille, max);
        }
        return taille;
    }

    private static void ouvert(DepotFragmente d) {
        if (d.getEtat() != EtatDepotFragmente.OUVERT) {
            throw DocumentException.conflit("DOC_UPLOAD_CLOSED", "Le dépôt n'accepte plus de parties", Map.of("etat", d.getEtat().name()));
        }
    }

    /** Lit au plus {@code attendue} + 1 octets : une partie trop longue est refusée sans être lue en entier. */
    private static byte[] lire(InputStream corps, long attendue) {
        try {
            return corps.readNBytes((int) attendue + 1);
        } catch (IOException e) {
            throw DocumentException.invalide("DOC_FILE_UNREADABLE", "La partie transmise est illisible");
        }
    }

    private static DocumentException tailleDePartie(int numero, long attendue, long recue) {
        return DocumentException.invalide(
            "DOC_PART_SIZE",
            "Taille de la partie différente de la taille attendue",
            Map.of("numero", numero, "attendue", attendue, "recue", recue)
        );
    }

    private Etat vue(DepotFragmente d) {
        boolean disponible = d.getEtat() == EtatDepotFragmente.DISPONIBLE;
        return new Etat(
            d.getId(),
            d.getEtat(),
            d.getNomFichier(),
            d.getTypeMime(),
            d.getTaille(),
            d.getTaillePartie(),
            d.getNombreParties(),
            d.partiesRecues(),
            disponible ? d.getDocumentId() : null,
            disponible ? d.getSha256() : null,
            d.isDedoublonne(),
            d.getEtat() == EtatDepotFragmente.REJETE ? d.getMotif() : null,
            d.getEtat() == EtatDepotFragmente.OUVERT ? d.getActifLe().plus(proprietes.inactivite()) : null
        );
    }
}
