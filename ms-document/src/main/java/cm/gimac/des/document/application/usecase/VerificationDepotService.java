package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.application.port.out.StockageObjet.EcritureWorm;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DepotFragmente;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import cm.gimac.des.document.domain.service.ControleNumerosCarte;
import cm.gimac.des.document.domain.service.ValidationDepot;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import cm.gimac.des.document.repository.DepotFragmenteRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Vérification serveur des dépôts fragmentés (D-74, docs/lot1/depots-fragmentes.md §2 et §3),
 * tâche de fond qui reprend les dépôts EN_VERIFICATION, y compris après un redémarrage.
 *
 * Pour un dépôt : l'objet assemblé est lu une seule fois, en flux ; chaque tranche (taille d'une
 * partie) est écrite comme partie d'un téléversement multipart vers le dépôt WORM, pendant que
 * l'empreinte SHA-256 est calculée et que, pour un fichier d'au plus {@code taille-max} (20 Mo),
 * le contenu est gardé pour le contrôle des numéros de carte (BNF-16). Jamais plus d'une tranche
 * en mémoire au-delà de ce seuil. En fin de lecture :
 * <ul>
 *   <li>taille différente de la taille annoncée : REJETE DOC_SIZE_MISMATCH ; numéro de carte :
 *   REJETE DOC_PAN_DETECTED (PDF illisible : DOC_PDF_UNREADABLE) ; l'écriture WORM est abandonnée ;</li>
 *   <li>document de même empreinte et de même taille déjà conservé : l'écriture WORM est abandonnée
 *   (aucun objet créé) et le nouveau document pointe vers l'objet existant ;</li>
 *   <li>sinon l'écriture WORM est terminée sous {@code finalite/aaaa/mm/jj/documentId}.</li>
 * </ul>
 * Le document est inscrit, le dépôt passe DISPONIBLE, puis l'objet assemblé est supprimé.
 *
 * Plusieurs instances : un dépôt est pris par verrouillage optimiste (version) et réservé pendant
 * {@code bail-verification} ; un téléversement WORM laissé par une tentative interrompue est
 * abandonné à la reprise. Dépôt objet indisponible : le dépôt reste EN_VERIFICATION et est repris
 * après {@code delai-nouvelle-tentative}.
 */
@Service
public class VerificationDepotService {

    private static final Logger LOG = LoggerFactory.getLogger(VerificationDepotService.class);
    private static final DateTimeFormatter DATE_CLE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
    private static final int LOT = 10;
    /** Octets lus en tête de fichier pour reconnaître un PDF (signature %PDF-). */
    private static final int TETE = 8;

    private final StockageObjet stockage;
    private final ExtracteurTexte extracteur;
    private final DepotFragmenteRepository depots;
    private final DepotDocumentRepository documents;
    private final TransactionTemplate transactions;
    private final DepotFragmenteProprietes proprietes;
    private final Clock horloge;

    /** Lecture en une passe : taille lue, empreinte, contenu gardé pour le contrôle des numéros de carte (sinon absent). */
    record Lecture(long taille, String sha256, byte[] contenu) {}

    /** Dépôt pris par cette instance : version obtenue, document et objet WORM visés. */
    private record Prise(long version, UUID documentId, String cleObjet) {}

    @Autowired
    public VerificationDepotService(
        StockageObjet stockage,
        ExtracteurTexte extracteur,
        DepotFragmenteRepository depots,
        DepotDocumentRepository documents,
        PlatformTransactionManager transactions,
        DepotFragmenteProprietes proprietes
    ) {
        this(stockage, extracteur, depots, documents, transactions, proprietes, Clock.systemUTC());
    }

    VerificationDepotService(
        StockageObjet stockage,
        ExtracteurTexte extracteur,
        DepotFragmenteRepository depots,
        DepotDocumentRepository documents,
        PlatformTransactionManager transactions,
        DepotFragmenteProprietes proprietes,
        Clock horloge
    ) {
        this.stockage = stockage;
        this.extracteur = extracteur;
        this.depots = depots;
        this.documents = documents;
        this.transactions = new TransactionTemplate(transactions);
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    /** Vérifie les dépôts en attente, l'un après l'autre ; rend le nombre de dépôts traités. */
    @Scheduled(
        fixedDelayString = "${gimac.document.depot.intervalle-verification:PT5S}",
        initialDelayString = "${gimac.document.depot.intervalle-verification:PT5S}"
    )
    public int verifierEnAttente() {
        int n = 0;
        for (UUID id : depots.aVerifier(EtatDepotFragmente.EN_VERIFICATION, horloge.instant(), PageRequest.of(0, LOT))) {
            try {
                verifier(id);
                n++;
            } catch (RuntimeException e) {
                LOG.error("Vérification du dépôt fragmenté {} interrompue : {}", id, e.toString());
            }
        }
        return n;
    }

    /** Vérifie un dépôt EN_VERIFICATION dont l'instant de reprise est atteint ; sans effet sinon. */
    public void verifier(UUID id) {
        Instant maintenant = horloge.instant();
        DepotFragmente d = depots.findById(id).orElse(null);
        if (d == null || d.getEtat() != EtatDepotFragmente.EN_VERIFICATION || (d.getRepriseLe() != null && d.getRepriseLe().isAfter(maintenant))) {
            return;
        }
        long version = d.getVersion();
        Flux flux;
        try {
            if (d.getEnvoiWorm() != null) {
                // Tentative précédente interrompue : son téléversement WORM n'a créé aucun objet, il est abandonné.
                stockage.abandonnerEcriture(d.getCleObjet(), d.getEnvoiWorm());
            }
            Optional<Flux> lu = stockage.lireDepot(d.getCleDepot());
            if (lu.isEmpty()) {
                LOG.error("Dépôt fragmenté {} : objet assemblé introuvable dans le compartiment des dépôts", id);
                terminer(id, version, v -> v.etat(EtatDepotFragmente.REJETE).motif("DOC_CONTENT_MISSING"));
                return;
            }
            flux = lu.orElseThrow();
        } catch (DocumentException e) {
            reporter(id, version, e);
            return;
        }
        try (InputStream entree = flux.contenu()) {
            verifier(d, version, flux, entree, maintenant);
        } catch (IOException e) {
            // Fermeture du flux : sans conséquence sur le résultat déjà inscrit.
            LOG.debug("Fermeture du flux du dépôt {} : {}", id, e.getMessage());
        }
    }

    private void verifier(DepotFragmente d, long version, Flux flux, InputStream entree, Instant maintenant) {
        UUID id = d.getId();
        if (flux.taille() != d.getTaille()) {
            rejeter(d, version, null, "DOC_SIZE_MISMATCH", flux.taille());
            return;
        }
        PushbackInputStream tete = new PushbackInputStream(entree, TETE);
        String typeMime;
        try {
            typeMime = typeMime(d.getTypeMime(), tete);
        } catch (IOException e) {
            reporter(id, version, e);
            return;
        }

        UUID documentId = d.getDocumentId() != null ? d.getDocumentId() : UUID.randomUUID();
        String cleObjet = d.getFinalite().toLowerCase(Locale.ROOT) + "/" + DATE_CLE.format(maintenant) + "/" + documentId;
        EcritureWorm ecriture;
        Prise prise;
        try {
            ecriture = stockage.ouvrirEcriture(cleObjet, typeMime);
        } catch (DocumentException e) {
            reporter(id, version, e);
            return;
        }
        prise = prendre(id, version, documentId, cleObjet, ecriture.envoi(), maintenant);
        if (prise == null) {
            LOG.info("Dépôt fragmenté {} pris par une autre instance", id);
            abandonner(ecriture, id);
            return;
        }

        Lecture lecture;
        try {
            lecture = lire(tete, d.getTaille(), d.getTaillePartie(), ecriture);
        } catch (DocumentException | IOException e) {
            abandonner(ecriture, id);
            reporter(id, prise.version(), e);
            return;
        }
        if (lecture.taille() != d.getTaille()) {
            rejeter(d, prise.version(), ecriture, "DOC_SIZE_MISMATCH", lecture.taille());
            return;
        }
        String motif = controlerNumerosCarte(id, lecture, typeMime);
        if (motif != null) {
            rejeter(d, prise.version(), ecriture, motif, lecture.taille());
            return;
        }

        Optional<DepotDocument> existant = documents.findFirstBySha256AndTailleOrderByDeposeLeAsc(lecture.sha256(), lecture.taille());
        ObjetDepose objet;
        try {
            if (existant.isPresent()) {
                abandonner(ecriture, id);
                objet = new ObjetDepose(existant.orElseThrow().getCleObjet(), existant.orElseThrow().getVersionObjet());
            } else {
                objet = ecriture.terminer();
            }
        } catch (DocumentException e) {
            abandonner(ecriture, id);
            reporter(id, prise.version(), e);
            return;
        }
        inscrire(d, prise, objet, lecture, typeMime, existant.isPresent());
    }

    /**
     * Lecture en une passe : tranches de {@code taillePartie} octets écrites dans le dépôt WORM,
     * empreinte calculée au fil de la lecture. La lecture s'arrête dès que la taille annoncée est
     * dépassée (la tranche en trop n'est pas écrite).
     */
    Lecture lire(InputStream entree, long tailleAnnoncee, long taillePartie, EcritureWorm ecriture) throws IOException {
        MessageDigest sha = sha256();
        boolean controle = tailleAnnoncee <= proprietes.tailleMax().toBytes();
        ByteArrayOutputStream contenu = controle ? new ByteArrayOutputStream((int) tailleAnnoncee) : null;
        byte[] tranche = new byte[(int) taillePartie];
        long total = 0;
        while (true) {
            int n = entree.readNBytes(tranche, 0, tranche.length);
            if (n == 0) {
                break;
            }
            total += n;
            if (total > tailleAnnoncee) {
                break;
            }
            sha.update(tranche, 0, n);
            if (contenu != null) {
                contenu.write(tranche, 0, n);
            }
            ecriture.ecrire(tranche, n);
            if (n < tranche.length) {
                break;
            }
        }
        return new Lecture(total, HexFormat.of().formatHex(sha.digest()), contenu == null ? null : contenu.toByteArray());
    }

    /** BNF-16 : motif de rejet, ou null si le contenu est accepté (ou trop volumineux pour être lu). */
    private String controlerNumerosCarte(UUID id, Lecture lecture, String typeMime) {
        if (lecture.contenu() == null) {
            LOG.info(
                "Dépôt fragmenté {} : contrôle des numéros de carte non appliqué ({} octets, au-delà de {})",
                id,
                lecture.taille(),
                proprietes.tailleMax()
            );
            return null;
        }
        int numeros;
        try {
            numeros = extracteur.extraire(lecture.contenu(), typeMime).map(ControleNumerosCarte::compter).orElse(0);
        } catch (DocumentException e) {
            return e.code();
        }
        if (numeros > 0) {
            LOG.warn("Dépôt fragmenté {} refusé : {} numéro(s) de carte détecté(s)", id, numeros);
            return "DOC_PAN_DETECTED";
        }
        return null;
    }

    /** Type MIME conservé : un contenu qui commence par la signature PDF est un PDF, quel que soit le type déclaré. */
    private static String typeMime(String declare, PushbackInputStream tete) throws IOException {
        byte[] debut = tete.readNBytes(TETE);
        tete.unread(debut);
        return ValidationDepot.typeMime(declare, debut);
    }

    /** Prise du dépôt (verrouillage optimiste) : réservé pendant le bail, écriture WORM inscrite pour une reprise. */
    private Prise prendre(UUID id, long version, UUID documentId, String cleObjet, String envoiWorm, Instant maintenant) {
        try {
            return transactions.execute(s -> {
                DepotFragmente v = depots.findById(id).orElse(null);
                if (v == null || v.getEtat() != EtatDepotFragmente.EN_VERIFICATION || !Objects.equals(v.getVersion(), version)) {
                    return null;
                }
                v.documentId(documentId).cleObjet(cleObjet).envoiWorm(envoiWorm).repriseLe(maintenant.plus(proprietes.bailVerification()));
                return new Prise(depots.saveAndFlush(v).getVersion(), documentId, cleObjet);
            });
        } catch (OptimisticLockingFailureException e) {
            return null;
        }
    }

    /** Document inscrit et dépôt DISPONIBLE dans une même transaction, puis objet assemblé supprimé. */
    private void inscrire(DepotFragmente d, Prise prise, ObjetDepose objet, Lecture lecture, String typeMime, boolean dedoublonne) {
        Instant maintenant = horloge.instant();
        boolean inscrit = terminer(d.getId(), prise.version(), v -> {
            documents.saveAndFlush(
                new DepotDocument()
                    .id(prise.documentId())
                    .finalite(v.getFinalite())
                    .reference(v.getReference())
                    .nomFichier(v.getNomFichier())
                    .taille(lecture.taille())
                    .typeMime(typeMime)
                    .sha256(lecture.sha256())
                    .cleObjet(objet.cle())
                    .versionObjet(objet.version())
                    .deposant(v.getDeposant())
                    .institutionId(v.getInstitutionId())
                    .deposeLe(maintenant)
            );
            v.etat(EtatDepotFragmente.DISPONIBLE).typeMime(typeMime).sha256(lecture.sha256()).dedoublonne(dedoublonne);
        });
        if (!inscrit) {
            if (!dedoublonne) {
                LOG.error("Objet orphelin dans le dépôt WORM : dépôt {} repris ailleurs, clé {}", d.getId(), objet.cle());
            }
            return;
        }
        supprimerDepot(d);
        LOG.info(
            "Dépôt fragmenté {} vérifié : document {}, finalité {}, {} octets, {}{}",
            d.getId(),
            prise.documentId(),
            d.getFinalite(),
            lecture.taille(),
            typeMime,
            dedoublonne ? ", objet existant réutilisé" : ""
        );
    }

    /** Rejet : écriture WORM abandonnée (aucun objet créé), dépôt REJETE avec son motif, objet assemblé supprimé. */
    private void rejeter(DepotFragmente d, long version, EcritureWorm ecriture, String motif, long lue) {
        if (ecriture != null) {
            abandonner(ecriture, d.getId());
        }
        LOG.warn("Dépôt fragmenté {} rejeté : {} ({} octets annoncés, {} lus)", d.getId(), motif, d.getTaille(), lue);
        if (terminer(d.getId(), version, v -> v.etat(EtatDepotFragmente.REJETE).motif(motif))) {
            supprimerDepot(d);
        }
    }

    /**
     * Passage à un état final si le dépôt est toujours celui pris (même version) ; l'écriture WORM
     * en cours et la réservation sont effacées. Faux si une autre instance l'a modifié entre-temps.
     */
    private boolean terminer(UUID id, long version, Consumer<DepotFragmente> modification) {
        try {
            Boolean fait = transactions.execute(s -> {
                DepotFragmente v = depots.findById(id).orElse(null);
                if (v == null || v.getEtat() != EtatDepotFragmente.EN_VERIFICATION || !Objects.equals(v.getVersion(), version)) {
                    return false;
                }
                modification.accept(v);
                v.envoiWorm(null).repriseLe(null).actifLe(horloge.instant());
                depots.saveAndFlush(v);
                return true;
            });
            return Boolean.TRUE.equals(fait);
        } catch (OptimisticLockingFailureException e) {
            return false;
        }
    }

    /** Dépôt objet indisponible : le dépôt reste EN_VERIFICATION, repris après le délai de nouvelle tentative. */
    private void reporter(UUID id, long version, Exception cause) {
        Instant reprise = horloge.instant().plus(proprietes.delaiNouvelleTentative());
        LOG.warn("Vérification du dépôt fragmenté {} reportée au {} : {}", id, reprise, cause instanceof DocumentException e ? e.code() : cause.toString());
        try {
            transactions.executeWithoutResult(s -> {
                DepotFragmente v = depots.findById(id).orElse(null);
                if (v != null && v.getEtat() == EtatDepotFragmente.EN_VERIFICATION && Objects.equals(v.getVersion(), version)) {
                    depots.saveAndFlush(v.repriseLe(reprise));
                }
            });
        } catch (OptimisticLockingFailureException e) {
            LOG.debug("Dépôt fragmenté {} modifié entre-temps", id);
        }
    }

    private void abandonner(EcritureWorm ecriture, UUID id) {
        try {
            ecriture.abandonner();
        } catch (DocumentException e) {
            LOG.warn("Écriture WORM du dépôt {} non abandonnée ({}) : aucun objet créé", id, e.code());
        }
    }

    /** L'objet assemblé est aussi purgé par la règle de cycle de vie du compartiment : un échec n'est que journalisé. */
    private void supprimerDepot(DepotFragmente d) {
        try {
            stockage.supprimerDepot(d.getCleDepot());
        } catch (DocumentException e) {
            LOG.warn("Objet assemblé du dépôt {} non supprimé ({}) : purgé par le cycle de vie du compartiment", d.getId(), e.code());
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
