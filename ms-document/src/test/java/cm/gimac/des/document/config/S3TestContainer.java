package cm.gimac.des.document.config;

import cm.gimac.des.document.infrastructure.stockage.StockageObjetS3;
import cm.gimac.des.document.infrastructure.stockage.StockageS3Proprietes;
import java.net.URI;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Passerelle S3 des tests d'intégration de l'adaptateur réel : SeaweedFS, qui parle le même
 * protocole S3 que Ceph RGW (D-50) et dont l'image est légère. L'image officielle de Ceph
 * (quay.io/ceph/ceph) reste celle de la plateforme de développement (infra/docker-compose) ; ces
 * tests éprouvent le protocole S3 de l'adaptateur, pas le mode conformité de Ceph, vérifié à part
 * (D-50, tester-socle.sh). Conteneur unique, démarré au premier usage.
 */
public final class S3TestContainer {

    public static final String CLE_ACCES = "gimac-test";
    public static final String CLE_SECRETE = "gimac-test-secret";

    /** Identité S3 unique, aux droits complets, pour les requêtes signées du SDK. */
    private static final String IDENTITES =
        "{\"identities\":[{\"name\":\"gimac\",\"credentials\":[{\"accessKey\":\"" +
        CLE_ACCES +
        "\",\"secretKey\":\"" +
        CLE_SECRETE +
        "\"}],\"actions\":[\"Admin\",\"Read\",\"Write\",\"List\",\"Tagging\"]}]}";

    private static GenericContainer<?> conteneur;

    private S3TestContainer() {}

    @SuppressWarnings("resource")
    public static synchronized URI pointAcces() {
        if (conteneur == null) {
            conteneur = new GenericContainer<>("chrislusf/seaweedfs:3.97")
                .withCopyToContainer(Transferable.of(IDENTITES), "/etc/seaweedfs/s3.json")
                .withCommand("server", "-s3", "-s3.config=/etc/seaweedfs/s3.json", "-dir=/data", "-ip.bind=0.0.0.0")
                .withExposedPorts(8333)
                .waitingFor(Wait.forHttp("/").forPort(8333).forStatusCodeMatching(c -> c < 500).withStartupTimeout(Duration.ofMinutes(3)));
            conteneur.start();
        }
        return URI.create("http://" + conteneur.getHost() + ":" + conteneur.getMappedPort(8333));
    }

    public static StockageS3Proprietes proprietes(String compartiment, boolean exigerVerrouillage) {
        return new StockageS3Proprietes(pointAcces(), "us-east-1", CLE_ACCES, CLE_SECRETE, compartiment, exigerVerrouillage);
    }

    /** Client S3 construit comme en production (adressage par chemin, sommes de contrôle à la demande). */
    public static S3Client client(String compartiment) {
        return StockageObjetS3.client(proprietes(compartiment, false));
    }
}
