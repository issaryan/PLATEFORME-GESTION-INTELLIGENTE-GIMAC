package cm.gimac.des.document.application.usecase;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Réglages de l'aperçu des documents bureautiques (préfixe {@code gimac.apercu}, D-83,
 * docs/lot1/apercu-bureautique.md §4).
 *
 * @param url adresse du moteur de conversion (Gotenberg) ; {@code http://localhost:3000} par défaut,
 *     {@code http://conversion-documents:3000} dans le Compose
 * @param delai délai de réponse attendu du moteur ; 65 s par défaut, au-delà des 60 s que le moteur
 *     s'accorde (--api-timeout), pour que son propre refus arrive le premier
 * @param delaiConnexion délai d'établissement de la connexion au moteur ; 5 s par défaut
 * @param tailleMax taille maximale d'un fichier source ; 20 Mo par défaut
 * @param cacheDuree durée de conservation d'un PDF converti dans le cache ; 24 h par défaut
 * @param cacheTailleMax taille totale des PDF gardés en cache ; 500 Mo par défaut
 * @param limiteParMinute conversions admises par minute et par compte ; 30 par défaut
 */
@ConfigurationProperties(prefix = "gimac.apercu")
public record ApercuProprietes(
    String url,
    Duration delai,
    Duration delaiConnexion,
    DataSize tailleMax,
    Duration cacheDuree,
    DataSize cacheTailleMax,
    Integer limiteParMinute
) {
    public ApercuProprietes {
        url = url == null || url.isBlank() ? "http://localhost:3000" : url;
        delai = delai == null ? Duration.ofSeconds(65) : delai;
        delaiConnexion = delaiConnexion == null ? Duration.ofSeconds(5) : delaiConnexion;
        tailleMax = tailleMax == null ? DataSize.ofMegabytes(20) : tailleMax;
        cacheDuree = cacheDuree == null ? Duration.ofHours(24) : cacheDuree;
        cacheTailleMax = cacheTailleMax == null ? DataSize.ofMegabytes(500) : cacheTailleMax;
        limiteParMinute = limiteParMinute == null ? 30 : limiteParMinute;
    }

    /** Valeurs par défaut. */
    public static ApercuProprietes parDefaut() {
        return new ApercuProprietes(null, null, null, null, null, null, null);
    }
}
