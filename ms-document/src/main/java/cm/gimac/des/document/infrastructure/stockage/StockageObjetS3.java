package cm.gimac.des.document.infrastructure.stockage;

import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DocumentException;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DefaultRetention;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * Adaptateur réel du dépôt objet (D-50) : Ceph RGW par le seul protocole S3 (SDK AWS).
 *
 * Compartiment WORM : chaque dépôt est un objet distinct sous une clé neuve ; le compartiment a le
 * verrouillage d'objets et une rétention par défaut (COMPLIANCE 10 ans hors DEV, D-11), appliquée
 * par le dépôt à chaque version : l'objet ne peut plus être ni écrasé ni supprimé avant
 * l'échéance. L'empreinte SHA-256 est posée en métadonnée (x-amz-meta-sha256) pour un dépôt entier ;
 * un objet écrit en flux (dépôt fragmenté, D-74) n'en porte pas, son empreinte n'étant connue
 * qu'à la fin de l'écriture : elle est inscrite en base, comme pour tout document. Chaque partie
 * écrite porte son Content-MD5, exigé par le verrouillage d'objets, et la rétention par défaut du
 * compartiment (lue au démarrage) est posée explicitement à l'ouverture du téléversement.
 *
 * Compartiment des dépôts en cours (D-74) : téléversements multipart des parties envoyées par le
 * client, puis objet assemblé, lu en flux par la vérification et supprimé ensuite. Un téléversement
 * inconnu (abandonné, déjà assemblé ou expiré) répond 409 DOC_UPLOAD_CLOSED.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.stockage", havingValue = "real")
public class StockageObjetS3 implements StockageObjet {

    private static final Logger LOG = LoggerFactory.getLogger(StockageObjetS3.class);
    static final String METADONNEE_EMPREINTE = "sha256";
    private static final String TYPE_OCTETS = "application/octet-stream";

    private final S3Client client;
    private final String compartiment;
    private final String compartimentDepots;
    /** Rétention par défaut du compartiment WORM, lue au démarrage quand le verrouillage est exigé. */
    private DefaultRetention retention;

    public StockageObjetS3(S3Client client, StockageS3Proprietes proprietes) {
        this.client = client;
        this.compartiment = proprietes.compartiment();
        this.compartimentDepots = proprietes.compartimentDepots();
        if (proprietes.exigerVerrouillage()) {
            verifierVerrouillage();
        }
    }

    /**
     * Client S3 pour Ceph RGW : adressage par chemin (pas de sous-domaine par compartiment) et
     * sommes de contrôle calculées seulement quand l'opération l'exige, faute de quoi les versions
     * récentes du SDK envoient des en-têtes CRC que toutes les passerelles n'acceptent pas.
     */
    public static S3Client client(StockageS3Proprietes p) {
        Objects.requireNonNull(p.pointAcces(), "gimac.stockage.s3.point-acces est obligatoire");
        Objects.requireNonNull(p.cleAcces(), "gimac.stockage.s3.cle-acces est obligatoire");
        Objects.requireNonNull(p.cleSecrete(), "gimac.stockage.s3.cle-secrete est obligatoire");
        return S3Client.builder()
            .endpointOverride(p.pointAcces())
            .region(Region.of(p.region()))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(p.cleAcces(), p.cleSecrete())))
            .forcePathStyle(true)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();
    }

    /** Refuse le démarrage si le compartiment n'a pas le verrouillage d'objets (RG-04, RG-16). */
    final void verifierVerrouillage() {
        GetObjectLockConfigurationResponse r;
        try {
            r = client.getObjectLockConfiguration(GetObjectLockConfigurationRequest.builder().bucket(compartiment).build());
        } catch (SdkException e) {
            throw new IllegalStateException("Verrouillage d'objets introuvable sur le compartiment " + compartiment, e);
        }
        if (r.objectLockConfiguration() == null || r.objectLockConfiguration().objectLockEnabled() != ObjectLockEnabled.ENABLED) {
            throw new IllegalStateException("Le compartiment " + compartiment + " n'a pas le verrouillage d'objets (WORM, D-50)");
        }
        retention = r.objectLockConfiguration().rule() == null ? null : r.objectLockConfiguration().rule().defaultRetention();
        LOG.info("Compartiment WORM {} : verrouillage d'objets actif, rétention par défaut {}", compartiment, retention);
    }

    /**
     * Rétention posée explicitement sur un objet WORM écrit en flux : celle du compartiment, pour
     * qu'elle ne dépende pas de son application par la passerelle à l'assemblage d'un téléversement
     * multipart (toutes les implémentations S3 ne l'appliquent pas).
     */
    static Instant echeance(DefaultRetention d, Instant maintenant) {
        ZonedDateTime t = maintenant.atZone(ZoneOffset.UTC);
        if (d.years() != null) {
            t = t.plusYears(d.years());
        }
        if (d.days() != null) {
            t = t.plusDays(d.days());
        }
        return t.toInstant();
    }

    // ------------------------------------------------------------------ dépôt WORM

    @Override
    public ObjetDepose deposer(String cle, byte[] contenu, String typeMime, String sha256) {
        return appeler(compartiment, () -> {
            PutObjectRequest requete = PutObjectRequest.builder()
                .bucket(compartiment)
                .key(cle)
                .contentType(typeMime)
                .contentLength((long) contenu.length)
                // Exigé par S3 pour écrire dans un compartiment à verrouillage d'objets ; vérifié par la passerelle.
                .contentMD5(Md5Utils.md5AsBase64(contenu))
                .metadata(Map.of(METADONNEE_EMPREINTE, sha256))
                .build();
            PutObjectResponse r = client.putObject(requete, RequestBody.fromBytes(contenu));
            return new ObjetDepose(cle, r.versionId());
        });
    }

    @Override
    public Optional<byte[]> lire(String cle) {
        return appeler(compartiment, () -> {
            try {
                return Optional.of(client.getObjectAsBytes(GetObjectRequest.builder().bucket(compartiment).key(cle).build()).asByteArray());
            } catch (NoSuchKeyException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public Optional<Flux> lireFlux(String cle) {
        return lireEnFlux(compartiment, cle);
    }

    @Override
    public EcritureWorm ouvrirEcriture(String cle, String typeMime) {
        String envoi = ouvrir(compartiment, cle, typeMime);
        return new EcritureS3(cle, envoi);
    }

    @Override
    public void abandonnerEcriture(String cle, String envoi) {
        abandonner(compartiment, cle, envoi);
    }

    /** Écriture en flux d'un objet WORM : une partie par tranche, numérotées dans l'ordre d'écriture. */
    private final class EcritureS3 implements EcritureWorm {

        private final String cle;
        private final String envoi;
        private final List<Partie> parties = new ArrayList<>();

        EcritureS3(String cle, String envoi) {
            this.cle = cle;
            this.envoi = envoi;
        }

        @Override
        public String envoi() {
            return envoi;
        }

        @Override
        public void ecrire(byte[] tranche, int longueur) {
            int numero = parties.size() + 1;
            parties.add(new Partie(numero, envoyerPartie(compartiment, cle, envoi, numero, tranche, longueur)));
        }

        @Override
        public ObjetDepose terminer() {
            return new ObjetDepose(cle, assembler(compartiment, cle, envoi, parties));
        }

        @Override
        public void abandonner() {
            StockageObjetS3.this.abandonner(compartiment, cle, envoi);
        }
    }

    // ------------------------------------------------------------------ dépôts en cours

    @Override
    public String ouvrirDepot(String cle, String typeMime) {
        return ouvrir(compartimentDepots, cle, typeMime);
    }

    @Override
    public String deposerPartie(String cle, String envoi, int numero, byte[] octets) {
        return envoyerPartie(compartimentDepots, cle, envoi, numero, octets, octets.length);
    }

    @Override
    public void assemblerDepot(String cle, String envoi, List<Partie> parties) {
        try {
            assembler(compartimentDepots, cle, envoi, parties);
        } catch (DocumentException e) {
            // Reprise d'une fin dont la réponse a été perdue : le téléversement est déjà assemblé.
            if ("DOC_UPLOAD_CLOSED".equals(e.code()) && existe(compartimentDepots, cle)) {
                LOG.info("Dépôt {} déjà assemblé", cle);
                return;
            }
            throw e;
        }
    }

    @Override
    public void abandonnerDepot(String cle, String envoi) {
        abandonner(compartimentDepots, cle, envoi);
    }

    @Override
    public Optional<Flux> lireDepot(String cle) {
        return lireEnFlux(compartimentDepots, cle);
    }

    @Override
    public void supprimerDepot(String cle) {
        appeler(compartimentDepots, () -> client.deleteObject(DeleteObjectRequest.builder().bucket(compartimentDepots).key(cle).build()));
    }

    // ------------------------------------------------------------------ opérations S3 communes

    private Optional<Flux> lireEnFlux(String bucket, String cle) {
        return appeler(bucket, () -> {
            try {
                ResponseInputStream<GetObjectResponse> flux = client.getObject(GetObjectRequest.builder().bucket(bucket).key(cle).build());
                return Optional.of(new Flux(flux, flux.response().contentLength()));
            } catch (NoSuchKeyException e) {
                return Optional.empty();
            }
        });
    }

    private boolean existe(String bucket, String cle) {
        return appeler(bucket, () -> {
            try {
                client.headObject(HeadObjectRequest.builder().bucket(bucket).key(cle).build());
                return true;
            } catch (NoSuchKeyException e) {
                return false;
            }
        });
    }

    private String ouvrir(String bucket, String cle, String typeMime) {
        CreateMultipartUploadRequest.Builder requete = CreateMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(cle)
            .contentType(typeMime == null ? TYPE_OCTETS : typeMime);
        if (bucket.equals(compartiment) && retention != null && retention.mode() != null) {
            requete.objectLockMode(ObjectLockMode.fromValue(retention.modeAsString())).objectLockRetainUntilDate(echeance(retention, Instant.now()));
        }
        return appeler(bucket, () -> client.createMultipartUpload(requete.build()).uploadId());
    }

    private String envoyerPartie(String bucket, String cle, String envoi, int numero, byte[] octets, int longueur) {
        String md5 = md5(octets, longueur);
        return appeler(bucket, () ->
            client
                .uploadPart(
                    UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(cle)
                        .uploadId(envoi)
                        .partNumber(numero)
                        .contentLength((long) longueur)
                        .contentMD5(md5)
                        .build(),
                    // Aucune copie des octets : le corps est relu depuis le tableau à chaque tentative du SDK.
                    RequestBody.fromContentProvider(() -> new ByteArrayInputStream(octets, 0, longueur), longueur, TYPE_OCTETS)
                )
                .eTag()
        );
    }

    /** CompleteMultipartUpload ; rend l'identifiant de version de l'objet créé. */
    private String assembler(String bucket, String cle, String envoi, List<Partie> parties) {
        List<CompletedPart> completes = parties.stream().map(p -> CompletedPart.builder().partNumber(p.numero()).eTag(p.etag()).build()).toList();
        return appeler(bucket, () ->
            client
                .completeMultipartUpload(
                    CompleteMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(cle)
                        .uploadId(envoi)
                        .multipartUpload(CompletedMultipartUpload.builder().parts(completes).build())
                        .build()
                )
                .versionId()
        );
    }

    private void abandonner(String bucket, String cle, String envoi) {
        try {
            appeler(bucket, () -> client.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket).key(cle).uploadId(envoi).build()));
        } catch (DocumentException e) {
            if (!"DOC_UPLOAD_CLOSED".equals(e.code())) {
                throw e;
            }
        }
    }

    /** Appel S3 : téléversement inconnu, 409 DOC_UPLOAD_CLOSED ; toute autre erreur, 503 DOC_STORAGE_UNAVAILABLE. */
    private <T> T appeler(String bucket, Supplier<T> appel) {
        try {
            return appel.get();
        } catch (SdkException e) {
            if (televersementInconnu(e)) {
                throw envoiInconnu();
            }
            LOG.error("Dépôt objet indisponible (compartiment {}) : {}", bucket, e.getMessage());
            throw DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "Dépôt objet indisponible");
        }
    }

    static boolean televersementInconnu(SdkException e) {
        return (
            e instanceof NoSuchUploadException ||
            (e instanceof S3Exception s3 && s3.awsErrorDetails() != null && "NoSuchUpload".equals(s3.awsErrorDetails().errorCode()))
        );
    }

    /** Téléversement multipart inconnu (abandonné, assemblé ou expiré) : le dépôt est fermé. */
    static DocumentException envoiInconnu() {
        return DocumentException.conflit("DOC_UPLOAD_CLOSED", "Le dépôt n'accepte plus de parties", Map.of());
    }

    static String md5(byte[] octets, int longueur) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(octets, 0, longueur);
            return Base64.getEncoder().encodeToString(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 indisponible", e);
        }
    }
}
