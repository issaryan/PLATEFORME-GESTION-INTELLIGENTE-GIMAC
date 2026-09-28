package cm.gimac.des.collaboration.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

/**
 * Construit le decodeur des jetons internes emis par la passerelle.
 *
 * <p>
 * <b>Le defaut corrige.</b> Les jetons internes etaient signes en HS256 avec un
 * secret symetrique partage par tous les services. Chaque detenteur du secret
 * pouvait donc <i>forger</i> un jeton portant n'importe quel role,
 * {@code ROLE_SUPER_ADMIN} compris — l'hypothese du motif Phantom Token, « la
 * passerelle est l'unique emetteur », n'etait pas tenue. La passerelle signe
 * desormais en RS256 ; ce service ne detient que la cle <i>publique</i>,
 * recuperee sur {@code /api/internal/jwks}.
 *
 * <p>
 * <b>Pourquoi un client HTTP dedie.</b> {@code NimbusJwtDecoder.withJwkSetUri}
 * utilise par defaut un {@code RestTemplate} dont le magasin de confiance est
 * celui de la JVM. La passerelle presente un certificat signe par la CA interne
 * du deploiement, absente de ce magasin : en https interne, la recuperation du
 * JWKS echoue. Constat etabli depuis le reseau {@code gimac-des_default} —
 * {@code curl https://api-gateway:8443/api/internal/jwks} echoue au handshake,
 * la meme commande avec {@code --cacert ca.crt} renvoie le jeu de cles. On
 * construit donc un client dont le magasin de confiance contient cette CA.
 *
 * <p>
 * L'autre option envisagee — importer la CA dans le {@code cacerts} de l'image
 * — a ete ecartee : elle etend la confiance a <i>tout</i> le trafic sortant de
 * la JVM, alors qu'on ne veut l'accorder qu'a la passerelle, et elle impose de
 * reconstruire les huit images a chaque renouvellement de CA. Ici la CA est lue
 * au demarrage depuis le volume deja monte.
 *
 * <p>
 * <b>Cache et rotation.</b> Le decodeur Nimbus met le JWKS en cache. Un jeton
 * portant un {@code kid} inconnu declenche une nouvelle recuperation (limitee
 * en frequence), ce qui fait qu'une rotation de cle est prise en compte sans
 * redemarrage. La procedure complete est en
 * {@code api-gateway/docs/ROTATION_CLE_SIGNATURE.md}.
 */
final class DecodeurJwtInterne {

    private static final Logger log = LoggerFactory.getLogger(DecodeurJwtInterne.class);

    /** Issuer pose par la passerelle sur tout jeton interne. */
    private static final String ISSUER_PASSERELLE = "gimac-gateway";

    /** Longueur minimale du secret de repli, en caracteres (256 bits pour HmacSHA256). */
    private static final int LONGUEUR_MINIMALE_SECRET = 32;

    private DecodeurJwtInterne() {
    }

    /**
     * @param environnement environnement Spring : le profil {@code prod} interdit
     *                      le repli HS256, quelle que soit la configuration.
     * @param jwksUri       URL du JWKS de la passerelle. Vide = repli HS256.
     * @param cheminCa      PEM de la CA interne, pour joindre la passerelle en
     *                      https. Vide = magasin de confiance de la JVM.
     * @param secretHs256   secret symetrique de repli, pour les tests unitaires
     *                      et les environnements sans passerelle.
     * @param repliDemande  autorisation explicite du repli. Elle ne peut que
     *                      <b>restreindre</b> : en profil {@code prod} le repli
     *                      reste interdit meme si elle vaut vrai.
     * @param connexionMs   delai d'etablissement de la connexion au JWKS.
     * @param lectureMs     delai de lecture de la reponse du JWKS.
     */
    static JwtDecoder construire(
        Environment environnement,
        String jwksUri,
        String cheminCa,
        String secretHs256,
        boolean repliDemande,
        int connexionMs,
        int lectureMs
    ) {
        return construire(environnement, jwksUri, cheminCa, secretHs256, repliDemande, connexionMs, lectureMs, false);
    }

    /**
     * @param liaisonCertificatExigee vrai dès que le mTLS est actif
     *        ({@code gimac.securite.mtls.actif}) : le jeton doit alors porter
     *        {@code cnf.x5t#S256} égal à l'empreinte du certificat client (RFC 8705).
     */
    static JwtDecoder construire(
        Environment environnement,
        String jwksUri,
        String cheminCa,
        String secretHs256,
        boolean repliDemande,
        int connexionMs,
        int lectureMs,
        boolean liaisonCertificatExigee
    ) {
        OAuth2TokenValidator<Jwt> validateurIssuer = liaisonCertificatExigee
            ? new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER_PASSERELLE),
                new ValidateurLiaisonCertificat()
            )
            : JwtValidators.createDefaultWithIssuer(ISSUER_PASSERELLE);

        // Le garde-fou tient au PROFIL, pas a une propriete : une variable
        // d'environnement oubliee dans un manifeste reinstallerait sinon la
        // faille en silence. En prod, le repli symetrique est impossible.
        boolean enProduction = environnement.matchesProfiles("prod");
        boolean repliHs256Autorise = repliDemande && !enProduction;

        if (jwksUri != null && !jwksUri.isBlank()) {
            NimbusJwtDecoder decodeur = NimbusJwtDecoder
                .withJwkSetUri(jwksUri)
                .restOperations(clientHttp(jwksUri, cheminCa, connexionMs, lectureMs))
                .build();
            decodeur.setJwtValidator(validateurIssuer);
            log.info("Jetons internes verifies en RS256 via le JWKS de la passerelle ({}).", jwksUri);
            return decodeur;
        }

        if (!repliHs256Autorise) {
            throw new IllegalStateException(
                "Securite : application.phantom-token.jwks-uri est obligatoire " +
                (enProduction
                    ? "en profil prod. "
                    : "des lors que application.phantom-token.allow-hs256-fallback vaut faux. ") +
                "Un secret symetrique partage permet a tout service le detenant de forger un jeton " +
                "interne portant n'importe quel role, ROLE_SUPER_ADMIN compris — c'est precisement " +
                "ce que la signature RS256 de la passerelle supprime. Renseigner l'URL de son JWKS " +
                "(APPLICATION_PHANTOM_TOKEN_JWKS_URI)."
            );
        }

        if (secretHs256 == null || secretHs256.isBlank()) {
            throw new IllegalStateException(
                "Securite : application.phantom-token.internal-jwt-secret ou jwks-uri est obligatoire."
            );
        }
        if (secretHs256.length() < LONGUEUR_MINIMALE_SECRET) {
            throw new IllegalStateException(
                "Securite : le secret JWT interne doit faire au moins 32 caracteres (256 bits). " +
                "Secret actuel : " + secretHs256.length() + " caractere(s)."
            );
        }
        log.warn(
            "REPLI HS256 — les jetons internes sont verifies avec un secret symetrique partage. " +
            "Tout service detenant ce secret peut forger un jeton portant n'importe quel role. " +
            "Ce mode est reserve aux tests et aux environnements sans passerelle ; il est interdit " +
            "en production (allow-hs256-fallback: false)."
        );
        SecretKeySpec cle = new SecretKeySpec(secretHs256.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decodeur = NimbusJwtDecoder.withSecretKey(cle).build();
        decodeur.setJwtValidator(validateurIssuer);
        return decodeur;
    }

    /**
     * Client HTTP de recuperation du JWKS. En https, son magasin de confiance
     * est construit a partir du PEM de la CA interne ; en http (poste de
     * developpement), aucun contexte TLS n'est necessaire.
     */
    private static RestTemplate clientHttp(String jwksUri, String cheminCa, int connexionMs, int lectureMs) {
        java.net.http.HttpClient.Builder constructeur = java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(connexionMs));

        if (jwksUri.startsWith("https://")) {
            if (cheminCa == null || cheminCa.isBlank()) {
                log.warn(
                    "JWKS en https sans CA interne declaree (application.phantom-token.jwks-ca-certificate) : " +
                    "le magasin de confiance de la JVM sera utilise. Si la passerelle presente un certificat " +
                    "signe par la CA interne du deploiement, la recuperation du JWKS echouera au handshake."
                );
            } else {
                constructeur.sslContext(contexteTls(Path.of(cheminCa)));
            }
        }

        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(constructeur.build());
        fabrique.setReadTimeout(Duration.ofMillis(lectureMs));
        RestTemplate client = new RestTemplate();
        client.setRequestFactory(fabrique);
        return client;
    }

    /**
     * Magasin de confiance ne contenant que la CA interne.
     *
     * <p>
     * Un PEM illisible fait echouer le demarrage. Se rabattre sur le magasin de
     * la JVM produirait un service qui demarre normalement puis rejette tous les
     * jetons a la premiere requete, avec une erreur TLS sans rapport apparent
     * avec l'authentification.
     */
    private static SSLContext contexteTls(Path pemCa) {
        try {
            CertificateFactory fabrique = CertificateFactory.getInstance("X.509");
            KeyStore magasin = KeyStore.getInstance(KeyStore.getDefaultType());
            magasin.load(null, null);
            try (InputStream flux = Files.newInputStream(pemCa)) {
                List<? extends Certificate> certificats = List.copyOf(fabrique.generateCertificates(flux));
                if (certificats.isEmpty()) {
                    throw new IllegalStateException("Aucun certificat X.509 dans " + pemCa);
                }
                for (int i = 0; i < certificats.size(); i++) {
                    magasin.setCertificateEntry("ca-interne-" + i, certificats.get(i));
                }
            }
            TrustManagerFactory gestionnaires = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            gestionnaires.init(magasin);
            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(null, gestionnaires.getTrustManagers(), null);
            return contexte;
        } catch (Exception e) {
            throw new IllegalStateException(
                "CA interne illisible (" + pemCa + ") : impossible de joindre le JWKS de la passerelle " +
                "en https. Verifier le montage du volume des certificats.",
                e
            );
        }
    }
}
