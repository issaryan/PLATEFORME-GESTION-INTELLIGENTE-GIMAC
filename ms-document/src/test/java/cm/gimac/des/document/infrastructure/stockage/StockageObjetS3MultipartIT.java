package cm.gimac.des.document.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.out.StockageObjet.EcritureWorm;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.application.port.out.StockageObjet.Partie;
import cm.gimac.des.document.domain.DocumentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;

/**
 * Adaptateur S3 réel face à une passerelle S3 désignée par {@code GIMAC_S3_TEST_ENDPOINT} (Ceph RGW,
 * MinIO, moto...) : téléversements multipart des dépôts fragmentés (D-74) dans un compartiment sans
 * verrouillage avec règles de cycle de vie, et écriture en flux dans un compartiment à verrouillage
 * d'objets (Content-MD5 par partie, rétention par défaut appliquée à l'objet assemblé). Ignoré sans
 * cette variable ; clés lues dans {@code GIMAC_S3_TEST_CLE_ACCES} et {@code GIMAC_S3_TEST_CLE_SECRETE}
 * (« test » par défaut).
 */
@EnabledIfEnvironmentVariable(named = "GIMAC_S3_TEST_ENDPOINT", matches = ".+")
class StockageObjetS3MultipartIT {

    static final int MIO_5 = 5 * 1024 * 1024;
    static final String SUFFIXE = UUID.randomUUID().toString().substring(0, 8);
    static final String WORM = "gimac-documents-mp-" + SUFFIXE;
    static final String DEPOTS = "gimac-depots-mp-" + SUFFIXE;

    static S3Client client;
    static StockageObjetS3 stockage;

    @BeforeAll
    static void preparer() {
        String cle = System.getenv().getOrDefault("GIMAC_S3_TEST_CLE_ACCES", "test");
        String secret = System.getenv().getOrDefault("GIMAC_S3_TEST_CLE_SECRETE", "test");
        StockageS3Proprietes p = new StockageS3Proprietes(URI.create(System.getenv("GIMAC_S3_TEST_ENDPOINT")), null, cle, secret, WORM, DEPOTS, true);
        client = StockageObjetS3.client(p);
        client.createBucket(b -> b.bucket(WORM).objectLockEnabledForBucket(true));
        client.putObjectLockConfiguration(b ->
            b
                .bucket(WORM)
                .objectLockConfiguration(c ->
                    c.objectLockEnabled(ObjectLockEnabled.ENABLED).rule(r -> r.defaultRetention(d -> d.mode(ObjectLockRetentionMode.GOVERNANCE).days(1)))
                )
        );
        client.createBucket(b -> b.bucket(DEPOTS));
        // Mêmes règles que infra/docker-compose/stockage-objet/init-compartiments.sh.
        client.putBucketLifecycleConfiguration(b ->
            b
                .bucket(DEPOTS)
                .lifecycleConfiguration(l ->
                    l.rules(
                        r -> r.id("objets-expires").status(ExpirationStatus.ENABLED).filter(f -> f.prefix("")).expiration(e -> e.days(2)),
                        r -> r.id("envois-incomplets").status(ExpirationStatus.ENABLED).filter(f -> f.prefix("")).abortIncompleteMultipartUpload(a -> a.daysAfterInitiation(1))
                    )
                )
        );
        stockage = new StockageObjetS3(client, p);
    }

    @AfterAll
    static void fermer() {
        client.close();
    }

    static byte[] octets(int taille, long graine) {
        byte[] b = new byte[taille];
        new Random(graine).nextBytes(b);
        return b;
    }

    static byte[] lire(Flux f) throws IOException {
        try (InputStream in = f.contenu()) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("Règles de cycle de vie du compartiment des dépôts : objets 2 jours, téléversements incomplets 1 jour")
    void cycleDeVie() {
        List<LifecycleRule> regles = client.getBucketLifecycleConfiguration(b -> b.bucket(DEPOTS)).rules();
        assertThat(regles).filteredOn(r -> r.expiration() != null).extracting(r -> r.expiration().days()).containsExactly(2);
        assertThat(regles)
            .filteredOn(r -> r.abortIncompleteMultipartUpload() != null)
            .extracting(r -> r.abortIncompleteMultipartUpload().daysAfterInitiation())
            .containsExactly(1);
    }

    @Test
    @DisplayName("Dépôt en cours : parties dans le désordre, partie renvoyée, assemblage, relecture en flux, fin renvoyée acceptée, suppression")
    void depotEnCours() throws IOException {
        byte[] fichier = octets(2 * MIO_5 + 1000, 1);
        String cle = UUID.randomUUID().toString();
        String envoi = stockage.ouvrirDepot(cle, "application/octet-stream");
        String e3 = stockage.deposerPartie(cle, envoi, 3, Arrays.copyOfRange(fichier, 2 * MIO_5, fichier.length));
        stockage.deposerPartie(cle, envoi, 1, new byte[MIO_5]);
        String e1 = stockage.deposerPartie(cle, envoi, 1, Arrays.copyOfRange(fichier, 0, MIO_5));
        String e2 = stockage.deposerPartie(cle, envoi, 2, Arrays.copyOfRange(fichier, MIO_5, 2 * MIO_5));
        List<Partie> parties = List.of(new Partie(1, e1), new Partie(2, e2), new Partie(3, e3));

        stockage.assemblerDepot(cle, envoi, parties);
        Flux f = stockage.lireDepot(cle).orElseThrow();
        assertThat(f.taille()).isEqualTo(fichier.length);
        assertThat(lire(f)).isEqualTo(fichier);

        // Fin renvoyée après une réponse perdue : téléversement déjà assemblé, accepté.
        stockage.assemblerDepot(cle, envoi, parties);
        stockage.supprimerDepot(cle);
        assertThat(stockage.lireDepot(cle)).isEmpty();
        stockage.supprimerDepot(cle);
    }

    @Test
    @DisplayName("Dépôt abandonné : téléversement inconnu ensuite, 409 DOC_UPLOAD_CLOSED ; abandon renvoyé sans effet")
    void depotAbandonne() {
        String cle = UUID.randomUUID().toString();
        String envoi = stockage.ouvrirDepot(cle, "text/plain");
        stockage.deposerPartie(cle, envoi, 1, new byte[] { 1, 2, 3 });
        stockage.abandonnerDepot(cle, envoi);
        stockage.abandonnerDepot(cle, envoi);
        assertThatThrownBy(() -> stockage.deposerPartie(cle, envoi, 2, new byte[] { 4 })).isInstanceOfSatisfying(DocumentException.class, e ->
            assertThat(e.code()).isEqualTo("DOC_UPLOAD_CLOSED")
        );
        assertThatThrownBy(() -> stockage.assemblerDepot(cle, envoi, List.of(new Partie(1, "\"x\"")))).isInstanceOfSatisfying(
            DocumentException.class,
            e -> assertThat(e.code()).isEqualTo("DOC_UPLOAD_CLOSED")
        );
    }

    @Test
    @DisplayName("Écriture WORM en flux : parties avec Content-MD5 sur un compartiment verrouillé, objet versionné sous rétention, suppression refusée")
    void ecritureWorm() throws IOException {
        byte[] fichier = octets(MIO_5 + 777, 2);
        String cle = "messagerie/2026/10/02/" + UUID.randomUUID();
        EcritureWorm e = stockage.ouvrirEcriture(cle, "video/mp4");
        byte[] tranche = new byte[MIO_5];
        System.arraycopy(fichier, 0, tranche, 0, MIO_5);
        e.ecrire(tranche, MIO_5);
        System.arraycopy(fichier, MIO_5, tranche, 0, 777);
        e.ecrire(tranche, 777);
        ObjetDepose o = e.terminer();

        assertThat(o.cle()).isEqualTo(cle);
        assertThat(o.version()).isNotBlank();
        Flux f = stockage.lireFlux(cle).orElseThrow();
        assertThat(f.taille()).isEqualTo(fichier.length);
        assertThat(lire(f)).isEqualTo(fichier);
        HeadObjectResponse tete = client.headObject(b -> b.bucket(WORM).key(cle));
        assertThat(tete.contentType()).isEqualTo("video/mp4");
        assertThat(tete.objectLockMode()).isEqualTo(ObjectLockMode.GOVERNANCE);
        assertThat(tete.objectLockRetainUntilDate()).isAfter(Instant.now());
        // Version sous rétention : sa suppression est refusée par la passerelle.
        assertThatThrownBy(() -> client.deleteObject(b -> b.bucket(WORM).key(cle).versionId(o.version()))).isInstanceOf(SdkException.class);
    }

    @Test
    @DisplayName("Écriture WORM abandonnée (doublon, rejet) : aucun objet créé ; abandon après un arrêt sans effet")
    void ecritureWormAbandonnee() {
        String cle = "messagerie/2026/10/02/" + UUID.randomUUID();
        EcritureWorm e = stockage.ouvrirEcriture(cle, "application/octet-stream");
        e.ecrire(octets(1000, 3), 1000);
        e.abandonner();
        assertThatThrownBy(() -> client.headObject(b -> b.bucket(WORM).key(cle))).isInstanceOf(NoSuchKeyException.class);
        assertThat(stockage.lireFlux(cle)).isEmpty();
        stockage.abandonnerEcriture(cle, e.envoi());
    }
}
