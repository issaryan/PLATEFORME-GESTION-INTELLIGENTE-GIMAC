package cm.gimac.des.document.application.usecase;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Réglages du dépôt (préfixe {@code gimac.document.depot}), communs au dépôt direct et aux dépôts
 * fragmentés (D-74, docs/lot1/depots-fragmentes.md §3).
 *
 * @param tailleMax taille maximale d'un dépôt direct, et seuil du contrôle des numéros de carte
 *     d'un dépôt fragmenté (au-delà, le texte n'est pas extrait) ; 20 Mo par défaut
 * @param tailleMaxFragmente taille maximale d'un dépôt fragmenté ; 20 Go par défaut
 * @param taillePartie taille d'une partie (la dernière porte le reste) ; 16 Mio par défaut, 5 Mio au
 *     moins (minimum S3)
 * @param inactivite délai après lequel un dépôt ouvert sans activité est abandonné ; 24 h par défaut
 * @param bailVerification durée pendant laquelle un dépôt pris par la vérification n'est pas repris
 *     par une autre instance (reprise après un arrêt au-delà) ; 1 h par défaut
 * @param delaiNouvelleTentative délai avant une nouvelle vérification quand le dépôt objet est
 *     indisponible ; 1 min par défaut
 */
@ConfigurationProperties(prefix = "gimac.document.depot")
public record DepotFragmenteProprietes(
    DataSize tailleMax,
    DataSize tailleMaxFragmente,
    DataSize taillePartie,
    Duration inactivite,
    Duration bailVerification,
    Duration delaiNouvelleTentative
) {
    /** Minimum S3 d'une partie autre que la dernière. */
    static final long TAILLE_PARTIE_MIN = 5L * 1024 * 1024;
    /** Maximum S3 du nombre de parties d'un téléversement. */
    static final long NOMBRE_PARTIES_MAX = 10_000;

    public DepotFragmenteProprietes {
        tailleMax = tailleMax == null ? DataSize.ofMegabytes(20) : tailleMax;
        tailleMaxFragmente = tailleMaxFragmente == null ? DataSize.ofGigabytes(20) : tailleMaxFragmente;
        taillePartie = taillePartie == null ? DataSize.ofMegabytes(16) : taillePartie;
        inactivite = inactivite == null ? Duration.ofHours(24) : inactivite;
        bailVerification = bailVerification == null ? Duration.ofHours(1) : bailVerification;
        delaiNouvelleTentative = delaiNouvelleTentative == null ? Duration.ofMinutes(1) : delaiNouvelleTentative;
    }

    /** Valeurs par défaut. */
    public static DepotFragmenteProprietes parDefaut() {
        return new DepotFragmenteProprietes(null, null, null, null, null, null);
    }

    /**
     * Refuse une configuration que S3 n'accepterait pas : partie de moins de 5 Mio, ou plus de
     * 10 000 parties pour un fichier de la taille maximale.
     */
    public DepotFragmenteProprietes verifier() {
        if (taillePartie.toBytes() < TAILLE_PARTIE_MIN) {
            throw new IllegalStateException("gimac.document.depot.taille-partie doit valoir au moins 5 Mio (minimum S3)");
        }
        if (taillePartie.toBytes() > Integer.MAX_VALUE - 8) {
            throw new IllegalStateException("gimac.document.depot.taille-partie est trop grande");
        }
        if (nombreParties(tailleMaxFragmente.toBytes()) > NOMBRE_PARTIES_MAX) {
            throw new IllegalStateException("gimac.document.depot.taille-max-fragmente exige plus de 10 000 parties (maximum S3)");
        }
        return this;
    }

    /** Nombre de parties d'un fichier de {@code taille} octets. */
    public long nombreParties(long taille) {
        long t = taillePartie.toBytes();
        return (taille + t - 1) / t;
    }
}
