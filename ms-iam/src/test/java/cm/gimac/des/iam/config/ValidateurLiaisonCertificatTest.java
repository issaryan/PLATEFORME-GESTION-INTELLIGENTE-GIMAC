package cm.gimac.des.iam.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Jeton lié au certificat client (RFC 8705, GW-05). */
class ValidateurLiaisonCertificatTest {

    private final ValidateurLiaisonCertificat validateur = new ValidateurLiaisonCertificat();

    private static X509Certificate certificat(String nom) throws Exception {
        try (InputStream flux = ValidateurLiaisonCertificatTest.class.getResourceAsStream("/certificats/" + nom)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(flux);
        }
    }

    private static Jwt jeton(Map<String, Object> cnf) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256").subject("USR-0042").issuedAt(Instant.now());
        if (cnf != null) {
            b.claim("cnf", cnf);
        }
        return b.build();
    }

    private static void connexionAvec(X509Certificate certificat) {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        if (certificat != null) {
            requete.setAttribute(ValidateurLiaisonCertificat.ATTRIBUT_CERTIFICATS, new X509Certificate[] { certificat });
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));
    }

    @AfterEach
    void nettoyer() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void accepteLeJetonPresenteParLeCertificatAuquelIlEstLie() throws Exception {
        X509Certificate cert = certificat("client-test.pem");
        connexionAvec(cert);
        assertThat(validateur.validate(jeton(Map.of("x5t#S256", ValidateurLiaisonCertificat.empreinte(cert)))).hasErrors()).isFalse();
    }

    @Test
    void refuseLeJetonRejoueDepuisUnAutreCertificat() throws Exception {
        connexionAvec(certificat("autre-test.pem"));
        String empreinteLegitime = ValidateurLiaisonCertificat.empreinte(certificat("client-test.pem"));
        assertThat(validateur.validate(jeton(Map.of("x5t#S256", empreinteLegitime))).hasErrors()).isTrue();
    }

    @Test
    void refuseLeJetonSansConfirmationOuSansCertificat() throws Exception {
        connexionAvec(certificat("client-test.pem"));
        assertThat(validateur.validate(jeton(null)).hasErrors()).isTrue();
        connexionAvec(null);
        assertThat(validateur.validate(jeton(Map.of("x5t#S256", "x"))).hasErrors()).isTrue();
    }
}
