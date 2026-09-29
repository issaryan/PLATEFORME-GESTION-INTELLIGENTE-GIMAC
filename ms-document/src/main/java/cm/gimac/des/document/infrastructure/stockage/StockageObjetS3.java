package cm.gimac.des.document.infrastructure.stockage;

import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DocumentException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * Adaptateur réel du dépôt objet WORM (D-50) : Ceph RGW par le seul protocole S3 (SDK AWS).
 * Chaque dépôt est un objet distinct sous une clé neuve ; le compartiment a le verrouillage
 * d'objets et une rétention par défaut (COMPLIANCE 10 ans hors DEV, D-11), appliquée par le dépôt
 * à chaque version : l'objet ne peut plus être ni écrasé ni supprimé avant l'échéance.
 * L'empreinte SHA-256 est posée en métadonnée (x-amz-meta-sha256) pour le contrôle d'intégrité.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.stockage", havingValue = "real")
public class StockageObjetS3 implements StockageObjet {

    private static final Logger LOG = LoggerFactory.getLogger(StockageObjetS3.class);
    static final String METADONNEE_EMPREINTE = "sha256";

    private final S3Client client;
    private final String compartiment;

    public StockageObjetS3(S3Client client, StockageS3Proprietes proprietes) {
        this.client = client;
        this.compartiment = proprietes.compartiment();
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
        LOG.info("Compartiment WORM {} : verrouillage d'objets actif", compartiment);
    }

    @Override
    public ObjetDepose deposer(String cle, byte[] contenu, String typeMime, String sha256) {
        try {
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
        } catch (SdkException e) {
            LOG.error("Dépôt objet indisponible (compartiment {}) : {}", compartiment, e.getMessage());
            throw DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "Dépôt objet indisponible");
        }
    }

    @Override
    public Optional<byte[]> lire(String cle) {
        try {
            return Optional.of(client.getObjectAsBytes(GetObjectRequest.builder().bucket(compartiment).key(cle).build()).asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            LOG.error("Dépôt objet indisponible (compartiment {}) : {}", compartiment, e.getMessage());
            throw DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "Dépôt objet indisponible");
        }
    }
}
