package cm.gimac.des.document.infrastructure.stockage;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accès au dépôt objet par le protocole S3 (D-50) : point d'accès de la passerelle Ceph RGW,
 * région (valeur conventionnelle pour Ceph), clés d'accès fournies par l'environnement
 * (GIMAC_S3_CLE_ACCES, GIMAC_S3_CLE_SECRETE, puis OpenBao à partir d'INT), compartiment WORM des
 * documents et compartiment des dépôts fragmentés en cours (D-74, sans verrouillage d'objets, avec
 * règles de cycle de vie : objets expirés après 2 jours, téléversements incomplets abandonnés
 * après 1 jour).
 *
 * @param exigerVerrouillage vrai pour refuser le démarrage si le compartiment WORM n'a pas le
 *     verrouillage d'objets (INT, QUAL, PROD) ; la rétention par défaut du compartiment
 *     (COMPLIANCE 10 ans hors DEV) s'applique alors à chaque objet déposé.
 */
@ConfigurationProperties(prefix = "gimac.stockage.s3")
public record StockageS3Proprietes(
    URI pointAcces,
    String region,
    String cleAcces,
    String cleSecrete,
    String compartiment,
    String compartimentDepots,
    boolean exigerVerrouillage
) {
    public StockageS3Proprietes {
        region = region == null || region.isBlank() ? "us-east-1" : region;
        compartiment = compartiment == null || compartiment.isBlank() ? "gimac-documents" : compartiment;
        compartimentDepots = compartimentDepots == null || compartimentDepots.isBlank() ? "gimac-depots" : compartimentDepots;
    }
}
