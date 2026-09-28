package cm.gimac.des.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;

/** Empreinte RFC 8705 et chemins exemptés de jeton (GW-05, GW-19). */
class LiaisonCertificatPasserelleTest {

    private static X509Certificate certificat(String nom) throws Exception {
        try (InputStream flux = LiaisonCertificatPasserelleTest.class.getResourceAsStream("/certificats/" + nom)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(flux);
        }
    }

    @Test
    void empreinteConformeALaRfc8705() throws Exception {
        // Valeur calculée indépendamment : openssl x509 -outform DER | openssl dgst -sha256 | base64url
        assertThat(EmpreinteCertificat.x5tS256(certificat("client-test.pem"))).isEqualTo("Ym2FQ0bsC87TUU0Uwo1OxGXQZPYMrzeVpeZ84zdt7wE");
    }

    @Test
    void cheminsTechniquesAncres() {
        assertThat(PhantomTokenAuthenticationFilter.CHEMINS_TECHNIQUES.matcher("/management/health").matches()).isTrue();
        assertThat(PhantomTokenAuthenticationFilter.CHEMINS_TECHNIQUES.matcher("/services/iam/v3/api-docs").matches()).isTrue();
        assertThat(PhantomTokenAuthenticationFilter.CHEMINS_TECHNIQUES.matcher("/api/docs-legaux").matches()).isFalse();
        assertThat(PhantomTokenAuthenticationFilter.CHEMINS_TECHNIQUES.matcher("/api/risk/management/health").matches()).isFalse();
        assertThat(PhantomTokenAuthenticationFilter.CHEMINS_TECHNIQUES.matcher("/management/env").matches()).isFalse();
    }
}
