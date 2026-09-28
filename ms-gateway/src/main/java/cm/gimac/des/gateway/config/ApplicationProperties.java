package cm.gimac.des.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties specific to Api Gateway.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 *
 * <p>
 * GIMAC : Cette classe expose, de maniere fortement typee, les parametres
 * metier de la Gateway aux beans Spring (filtres Phantom Token, Rate Limiter
 * Token Bucket, routage interne mTLS). Elle est validee au demarrage par
 * Bean Validation : toute valeur incoherente fait echouer l'application
 * avant d'accepter le moindre trafic, conformement a la philosophie fail-fast
 * exigee par le contexte bancaire.
 */
@Validated
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    // jhipster-needle-application-properties-property

    /** Parametres du pattern Phantom Token (User Story INF-13). */
    @Valid
    @NotNull
    private PhantomToken phantomToken = new PhantomToken(5, 10_000);

    /** Parametres du Rate Limiter Token Bucket distribue (User Story INF-14). */
    @Valid
    @NotNull
    private RateLimit rateLimit = new RateLimit(1_000, 1_000, "rate_limit:");

    /** Parametres du routage interne mTLS (User Story INF-15). */
    @Valid
    @NotNull
    private InternalRouting internalRouting = new InternalRouting(".gimac.internal");

    /** Parametres de la cle de signature des JWT internes (RS256). */
    @Valid
    @NotNull
    private SigningKey signingKey = new SigningKey("", "", List.of(), 2048, true);

    // jhipster-needle-application-properties-property-getter

    public PhantomToken getPhantomToken() {
        return phantomToken;
    }

    public void setPhantomToken(PhantomToken phantomToken) {
        this.phantomToken = phantomToken;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public SigningKey getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(SigningKey signingKey) {
        this.signingKey = signingKey;
    }

    public InternalRouting getInternalRouting() {
        return internalRouting;
    }

    public void setInternalRouting(InternalRouting internalRouting) {
        this.internalRouting = internalRouting;
    }

    // jhipster-needle-application-properties-property-class

    // =========================================================================
    // Sous-classes typesafe (records Java 21)
    // =========================================================================

    /**
     * Configuration du cache d'introspection Keycloak (Phantom Token, INF-13).
     *
     * @param cacheTtlMinutes   Duree de vie maximale (en minutes) d'une entree dans
     *                          le cache Caffeine. Doit etre legerement inferieure
     *                          a la duree de vie d'un access token Keycloak pour
     *                          eviter de servir un jeton expire depuis le cache.
     *                          Plage [1, 60].
     * @param cacheMaxSize      Capacite maximale du cache (nombre d'entrees).
     *                          Au-dela, l'eviction LRU est appliquee. Une valeur
     *                          de 10 000 convient typiquement a une instance
     *                          gerant 100 agences avec sessions concurrentes.
     *
     * <p>
     * <b>Le secret HMAC a disparu de cette section, volontairement.</b> Les
     * jetons internes sont signes en RS256 avec la paire de
     * {@code application.signing-key} : la passerelle n'a plus aucun usage d'une
     * cle symetrique, et ne doit surtout pas en detenir une. Un secret partage
     * avec les services aval leur donnerait le pouvoir de forger un jeton
     * portant n'importe quel role, ce que le motif Phantom Token exclut par
     * construction. La classe etant annotee
     * {@code ignoreUnknownFields = false}, laisser trainer un
     * {@code application.phantom-token.internal-jwt-secret} dans un fichier de
     * configuration fait desormais echouer le demarrage — c'est l'effet
     * recherche : l'oubli est bruyant.
     */
    public record PhantomToken(
            @Min(value = 1, message = "Le TTL du cache Phantom Token doit etre >= 1 minute") int cacheTtlMinutes,

            @Min(value = 100, message = "La taille max du cache Phantom Token doit etre >= 100") int cacheMaxSize) {
    }

    /**
     * Configuration du Rate Limiter Token Bucket distribue (INF-14).
     *
     * <p>
     * Ces valeurs s'appliquent UNIQUEMENT lorsqu'aucune PolitiqueQuota
     * specifique au tenant n'est trouvee en base DB_Gateway. En production,
     * chaque tenant dispose de sa propre configuration personnalisee.
     *
     * @param defaultReplenishRate Taux de rechargement du seau (jetons par seconde)
     *                             applique par defaut. Plage [1, 1_000_000].
     * @param defaultBurstCapacity Capacite maximale instantanee du seau.
     *                             Permet d'absorber les rafales legitimes.
     *                             Plage [1, 1_000_000].
     * @param keyPrefix            Prefixe des cles Redis pour eviter les collisions
     *                             entre environnements. Exemple :
     *                             "rate_limit:dev:".
     */
    public record RateLimit(
            @Min(value = 1, message = "Le taux de rechargement par defaut doit etre >= 1") int defaultReplenishRate,

            @Min(value = 1, message = "La capacite de rafale par defaut doit etre >= 1") int defaultBurstCapacity,

            @NotBlank(message = "Le prefixe des cles Redis est obligatoire") String keyPrefix) {
    }

    /**
     * Configuration du routage interne vers les microservices (INF-15 mTLS).
     *
     * @param internalDomain Suffixe DNS des microservices internes. Utilise pour
     *                       la validation du CN du certificat mTLS et pour la
     *                       construction des URIs internes. Ex:
     *                       ".gimac.internal".
     */
    /**
     * Configuration de la cle RSA signant les JWT internes (RS256).
     *
     * <p>
     * <b>Pourquoi cette section existe.</b> La paire etait auparavant generee a
     * chaque demarrage, avec un {@code kid} tire au sort. Deux consequences :
     * un redemarrage invalidait tous les jetons en circulation, et deux
     * instances de passerelle derriere le meme repartiteur signaient avec des
     * cles differentes — un jeton emis par l'une etait rejete par l'autre. La
     * paire doit donc venir d'une source externe persistante.
     *
     * <p>
     * <b>Ordre de resolution</b>, applique par {@code RsaKeyConfig} :
     * {@code privateKeyPem} (servi par le coffre-fort), puis
     * {@code privateKeyPath} (volume monte), puis generation ephemere si
     * {@code allowEphemeral} l'autorise.
     *
     * @param privateKeyPem          PEM PKCS#8 en clair, injecte par le coffre-fort.
     *                               Ne JAMAIS ecrire cette valeur dans un fichier
     *                               versionne : elle n'a sa place que dans Vault ou
     *                               dans une variable d'environnement du runtime.
     * @param privateKeyPath         chemin du PEM PKCS#8 sur un volume monte.
     *                               Ignore si {@code privateKeyPem} est renseigne.
     * @param previousPublicKeyPaths cles publiques SPKI des paires precedentes,
     *                               encore publiees au JWKS le temps que les jetons
     *                               qu'elles ont signes expirent. C'est ce qui rend
     *                               une rotation possible sans coupure.
     * @param minimumKeySize         taille minimale acceptee, en bits. 2048 est le
     *                               plancher RSA de la NIST SP 800-131A Rev. 2 pour
     *                               la signature au-dela de 2030.
     * @param allowEphemeral         autorise la generation d'une paire jetable
     *                               lorsqu'aucune source persistante n'est fournie.
     *                               Vrai en developpement, faux en production : le
     *                               demarrage echoue alors plutot que de basculer
     *                               silencieusement sur une cle jetable.
     */
    public record SigningKey(
            String privateKeyPem,

            String privateKeyPath,

            @NotNull(message = "La liste des cles publiques precedentes ne peut pas etre nulle") List<
                    String> previousPublicKeyPaths,

            @Min(value = 2048, message = "La cle de signature doit faire au moins 2048 bits") int minimumKeySize,

            boolean allowEphemeral) {
    }

    public record InternalRouting(
            @NotBlank(message = "Le domaine interne est obligatoire pour le routage mTLS") String internalDomain) {
    }
}