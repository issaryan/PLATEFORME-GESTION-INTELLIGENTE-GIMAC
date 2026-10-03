package cm.gimac.des.document.infrastructure.stockage;

import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DocumentException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut du dépôt objet (profils dev et test, PI §0.8) : objets en mémoire, perdus à l'arrêt.
 * Il imite un compartiment WORM versionné (un second dépôt sous la même clé ajoute une version et
 * n'écrase jamais la précédente) et le compartiment des dépôts en cours, non versionné. Les
 * téléversements multipart suivent les règles de S3 : partie remplacée si elle est renvoyée,
 * assemblage limité aux parties citées (numéros croissants, ETag exacts), parties d'au moins
 * 5 Mio sauf la dernière, téléversement inconnu après assemblage ou abandon. Une panne peut être
 * simulée (tests). Jamais sélectionné en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.stockage", havingValue = "stub")
public class StockageObjetMemoire implements StockageObjet {

    /** Taille minimale d'une partie autre que la dernière (S3). */
    static final long TAILLE_PARTIE_MIN_S3 = 5L * 1024 * 1024;

    /** Téléversement multipart en cours : clé, parties reçues (numéro, contenu) et leurs ETag. */
    private record Envoi(String cle, TreeMap<Integer, byte[]> parties, Map<Integer, String> etags) {
        Envoi(String cle) {
            this(cle, new TreeMap<>(), new TreeMap<>());
        }
    }

    private final Map<String, List<byte[]>> objets = new ConcurrentHashMap<>();
    private final Map<String, byte[]> depots = new ConcurrentHashMap<>();
    private final Map<String, Envoi> envoisDepot = new ConcurrentHashMap<>();
    private final Map<String, Envoi> envoisWorm = new ConcurrentHashMap<>();
    private final long taillePartieMin;
    private volatile boolean enPanne;

    public StockageObjetMemoire() {
        this(TAILLE_PARTIE_MIN_S3);
    }

    /** Substitut dont la taille minimale des parties est réduite (tests sur de petits fichiers). */
    public StockageObjetMemoire(long taillePartieMin) {
        this.taillePartieMin = taillePartieMin;
    }

    // ------------------------------------------------------------------ dépôt WORM

    @Override
    public ObjetDepose deposer(String cle, byte[] contenu, String typeMime, String sha256) {
        disponible();
        return ajouterVersion(cle, contenu.clone());
    }

    @Override
    public Optional<byte[]> lire(String cle) {
        disponible();
        return derniereVersion(cle);
    }

    @Override
    public Optional<Flux> lireFlux(String cle) {
        disponible();
        return derniereVersion(cle).map(o -> new Flux(new ByteArrayInputStream(o), o.length));
    }

    @Override
    public EcritureWorm ouvrirEcriture(String cle, String typeMime) {
        disponible();
        String id = UUID.randomUUID().toString();
        envoisWorm.put(id, new Envoi(cle));
        return new EcritureMemoire(cle, id);
    }

    @Override
    public void abandonnerEcriture(String cle, String envoi) {
        disponible();
        envoisWorm.remove(envoi);
    }

    private final class EcritureMemoire implements EcritureWorm {

        private final String cle;
        private final String id;

        EcritureMemoire(String cle, String id) {
            this.cle = cle;
            this.id = id;
        }

        @Override
        public String envoi() {
            return id;
        }

        @Override
        public void ecrire(byte[] tranche, int longueur) {
            disponible();
            Envoi e = enCours(envoisWorm, cle, id);
            synchronized (e) {
                int numero = e.parties().isEmpty() ? 1 : e.parties().lastKey() + 1;
                byte[] partie = Arrays.copyOf(tranche, longueur);
                e.parties().put(numero, partie);
                e.etags().put(numero, etag(partie));
            }
        }

        @Override
        public ObjetDepose terminer() {
            disponible();
            Envoi e = enCours(envoisWorm, cle, id);
            synchronized (e) {
                byte[] contenu = assembler(e, e.etags().entrySet().stream().map(p -> new Partie(p.getKey(), p.getValue())).toList());
                envoisWorm.remove(id);
                return ajouterVersion(cle, contenu);
            }
        }

        @Override
        public void abandonner() {
            abandonnerEcriture(cle, id);
        }
    }

    // ------------------------------------------------------------------ dépôts en cours

    @Override
    public String ouvrirDepot(String cle, String typeMime) {
        disponible();
        String id = UUID.randomUUID().toString();
        envoisDepot.put(id, new Envoi(cle));
        return id;
    }

    @Override
    public String deposerPartie(String cle, String envoi, int numero, byte[] octets) {
        disponible();
        if (numero < 1 || numero > 10_000) {
            throw new IllegalArgumentException("Numéro de partie hors de 1 à 10000 : " + numero);
        }
        Envoi e = enCours(envoisDepot, cle, envoi);
        synchronized (e) {
            byte[] partie = octets.clone();
            String etag = etag(partie);
            e.parties().put(numero, partie);
            e.etags().put(numero, etag);
            return etag;
        }
    }

    @Override
    public void assemblerDepot(String cle, String envoi, List<Partie> parties) {
        disponible();
        Envoi e = enCours(envoisDepot, cle, envoi);
        synchronized (e) {
            depots.put(cle, assembler(e, parties));
            envoisDepot.remove(envoi);
        }
    }

    @Override
    public void abandonnerDepot(String cle, String envoi) {
        disponible();
        envoisDepot.remove(envoi);
    }

    @Override
    public Optional<Flux> lireDepot(String cle) {
        disponible();
        return Optional.ofNullable(depots.get(cle)).map(o -> new Flux(new ByteArrayInputStream(o), o.length));
    }

    @Override
    public void supprimerDepot(String cle) {
        disponible();
        depots.remove(cle);
    }

    // ------------------------------------------------------------------ règles S3

    /** Téléversement en cours ; sinon le dépôt est fermé (NoSuchUpload en S3). */
    private static Envoi enCours(Map<String, Envoi> envois, String cle, String id) {
        Envoi e = id == null ? null : envois.get(id);
        if (e == null || !e.cle().equals(cle)) {
            throw StockageObjetS3.envoiInconnu();
        }
        return e;
    }

    /** CompleteMultipartUpload : parties citées en ordre croissant, ETag exacts, taille minimale sauf la dernière. */
    private byte[] assembler(Envoi e, List<Partie> parties) {
        if (parties.isEmpty()) {
            throw new IllegalArgumentException("Aucune partie à assembler (MalformedXML)");
        }
        ByteArrayOutputStream contenu = new ByteArrayOutputStream();
        int precedent = 0;
        for (int i = 0; i < parties.size(); i++) {
            Partie p = parties.get(i);
            if (p.numero() <= precedent) {
                throw new IllegalArgumentException("Parties non croissantes (InvalidPartOrder)");
            }
            precedent = p.numero();
            byte[] octets = e.parties().get(p.numero());
            if (octets == null || !e.etags().get(p.numero()).equals(p.etag())) {
                throw new IllegalArgumentException("Partie " + p.numero() + " inconnue ou ETag différent (InvalidPart)");
            }
            if (i < parties.size() - 1 && octets.length < taillePartieMin) {
                throw new IllegalArgumentException("Partie " + p.numero() + " trop petite (EntityTooSmall)");
            }
            contenu.writeBytes(octets);
        }
        return contenu.toByteArray();
    }

    private ObjetDepose ajouterVersion(String cle, byte[] contenu) {
        List<byte[]> versions = objets.computeIfAbsent(cle, c -> new ArrayList<>());
        synchronized (versions) {
            versions.add(contenu);
            return new ObjetDepose(cle, "v" + versions.size());
        }
    }

    private Optional<byte[]> derniereVersion(String cle) {
        List<byte[]> versions = objets.get(cle);
        if (versions == null) {
            return Optional.empty();
        }
        synchronized (versions) {
            return Optional.of(versions.get(versions.size() - 1).clone());
        }
    }

    static String etag(byte[] octets) {
        try {
            return "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(octets)) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 indisponible", e);
        }
    }

    private void disponible() {
        if (enPanne) {
            throw DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "Dépôt objet indisponible");
        }
    }

    // ------------------------------------------------------------------ observation (tests)

    /** Simule une panne du dépôt objet : toute opération répond DOC_STORAGE_UNAVAILABLE. */
    public void simulerPanne(boolean panne) {
        this.enPanne = panne;
    }

    /** Nombre de versions conservées sous une clé du dépôt WORM (tests). */
    public int versions(String cle) {
        List<byte[]> versions = objets.get(cle);
        return versions == null ? 0 : versions.size();
    }

    /** Nombre d'objets du dépôt WORM, toutes clés confondues (tests). */
    public int objetsWorm() {
        return objets.size();
    }

    /** Vrai si un objet assemblé existe sous cette clé du compartiment des dépôts en cours (tests). */
    public boolean depotExiste(String cle) {
        return depots.containsKey(cle);
    }

    /** Téléversements multipart en cours, dans les deux compartiments (tests). */
    public int envoisEnCours() {
        return envoisDepot.size() + envoisWorm.size();
    }
}
