package cm.gimac.des.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * Unique point d'emission des jetons internes de la passerelle.
 *
 * <p>
 * <b>Pourquoi cette classe existe.</b> La passerelle emet des jetons internes
 * par <i>deux</i> chemins : le motif Phantom Token
 * ({@link KeycloakIntrospectionClient}, pour un utilisateur authentifie chez
 * Keycloak) et l'authentification par cle API
 * ({@code ApiKeyAuthenticationFilter}, pour un appel machine). Tant que chacun
 * construisait son en-tete et signait de son cote, rien n'empechait l'un de
 * rester en HS256 pendant que l'autre passait en RS256 — et le trafic du chemin
 * oublie aurait ete rejete en bloc par les services aval. Les deux chemins
 * passent desormais ici.
 *
 * <p>
 * <b>Ce que garantit cette classe.</b> Tout jeton emis porte
 * {@code alg = RS256}, un {@code kid} et l'issuer {@code gimac-gateway}. Le
 * {@code kid} n'est pas decoratif : sans lui, un service aval dont le JWKS
 * contient deux cles (rotation en cours) ne sait pas laquelle essayer. La cle
 * privee ne quitte jamais la passerelle ; les services ne recoivent que la
 * partie publique, via {@code /api/internal/jwks}.
 */
@ConditionalOnProperty(name = "gimac.securite.jeton-fantome.actif", havingValue = "true")
@Component
public class EmetteurJwtInterne {

    /** Issuer que les services aval exigent (JwtValidators.createDefaultWithIssuer). */
    public static final String ISSUER = "gimac-gateway";

    /**
     * Duree de vie du jeton interne. Valeur <b>preexistante</b> a la bascule
     * RS256 : les deux emetteurs codaient deja 5 minutes en dur. Elle est
     * reprise telle quelle et centralisee ici — aucune mesure ni arbitrage
     * humain n'a ete trouve dans le depot pour la justifier.
     */
    private static final Duration DUREE_DE_VIE = Duration.ofMinutes(5);

    private final RSAKey cle;
    private final RSASSASigner signataire;

    /**
     * @param internalRsaKey paire persistante fournie par {@link RsaKeyConfig},
     *                       dont le {@code kid} est l'empreinte RFC 7638 — donc
     *                       stable entre instances et entre redemarrages
     * @throws JOSEException si la cle ne porte pas de partie privee
     */
    /** Certificat mTLS de la passerelle, absent tant que {@code gimac.securite.mtls.actif} est faux. */
    private final ObjectProvider<CertificatMtlsPasserelle> certificatMtls;

    public EmetteurJwtInterne(RSAKey internalRsaKey, ObjectProvider<CertificatMtlsPasserelle> certificatMtls)
        throws JOSEException {
        this.cle = internalRsaKey;
        this.certificatMtls = certificatMtls;
        // Le signataire est derive une fois : le reconstruire a chaque jeton
        // couterait une reconstruction de cle privee par requete, sans benefice.
        this.signataire = new RSASSASigner(internalRsaKey);
    }

    /**
     * Complete le jeu de claims fourni (issuer, dates) puis signe en RS256.
     *
     * @param claims claims propres a l'appelant — sujet, roles, tenant. L'issuer
     *               et les dates sont poses ici pour que les deux chemins
     *               d'emission ne puissent pas diverger dessus.
     * @return le jeton serialise, pret a etre pose en {@code Authorization}
     * @throws JOSEException si la signature echoue
     */
    public String emettre(JWTClaimsSet.Builder claims) throws JOSEException {
        long maintenant = System.currentTimeMillis();
        JWTClaimsSet.Builder jeuPartiel = claims
            .issuer(ISSUER)
            .issueTime(new Date(maintenant))
            .expirationTime(new Date(maintenant + DUREE_DE_VIE.toMillis()));
        // Liaison au certificat (RFC 8705 §3.1, GW-05) : le jeton n'est recevable
        // que présenté sur une connexion mTLS authentifiée par ce certificat.
        CertificatMtlsPasserelle certificat = certificatMtls.getIfAvailable();
        if (certificat != null) {
            jeuPartiel.claim("cnf", Map.of("x5t#S256", certificat.empreinte()));
        }
        JWTClaimsSet jeu = jeuPartiel.build();

        JWSHeader entete = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(cle.getKeyID())
            .type(JOSEObjectType.JWT)
            .build();

        SignedJWT jeton = new SignedJWT(entete, jeu);
        jeton.sign(signataire);
        return jeton.serialize();
    }

    /** Identifiant de la cle courante, tel qu'annonce dans l'en-tete des jetons emis. */
    public String kid() {
        return cle.getKeyID();
    }
}
