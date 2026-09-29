package cm.gimac.des.document.config;

import cm.gimac.des.document.infrastructure.stockage.StockageObjetS3;
import cm.gimac.des.document.infrastructure.stockage.StockageS3Proprietes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Dépôt objet WORM (D-50) : le client S3 n'est créé qu'avec l'adaptateur réel
 * ({@code gimac.adapters.stockage=real}, profil prod) ; les profils dev et test utilisent le
 * substitut en mémoire.
 */
@Configuration
@EnableConfigurationProperties(StockageS3Proprietes.class)
public class StockageObjetConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "gimac.adapters.stockage", havingValue = "real")
    public S3Client s3Client(StockageS3Proprietes proprietes) {
        return StockageObjetS3.client(proprietes);
    }
}
