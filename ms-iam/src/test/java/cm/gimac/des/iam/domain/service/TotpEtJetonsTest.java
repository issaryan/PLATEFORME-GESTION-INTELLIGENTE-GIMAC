package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Couche;
import cm.gimac.des.iam.domain.compte.Courriel;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import cm.gimac.des.iam.domain.compte.FinaliteJeton;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Services de domaine de la connexion sur la plateforme (D-65) : TOTP (vecteurs de la RFC 6238),
 * base 32 (RFC 4648), jetons des liens, violations de la politique, modèle du compte.
 */
class TotpEtJetonsTest {

    /** Secrets de l'annexe B de la RFC 6238 pour chaque algorithme. */
    static final byte[] SHA1 = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    static final byte[] SHA256 = "12345678901234567890123456789012".getBytes(StandardCharsets.US_ASCII);
    static final byte[] SHA512 = "1234567890123456789012345678901234567890123456789012345678901234".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest(name = "t={0} {2}")
    @CsvSource({ "59,94287082,HmacSHA1", "1111111109,07081804,HmacSHA1", "1234567890,89005924,HmacSHA1", "59,46119246,HmacSHA256", "1111111111,67062674,HmacSHA256", "59,90693936,HmacSHA512", "20000000000,47863826,HmacSHA512" })
    @DisplayName("RFC 6238, annexe B : codes à 8 chiffres, pas de 30 s")
    void vecteursRfc6238(long t, String attendu, String algorithme) {
        byte[] secret = switch (algorithme) {
            case "HmacSHA256" -> SHA256;
            case "HmacSHA512" -> SHA512;
            default -> SHA1;
        };
        assertThat(Totp.code(Totp.base32(secret), Instant.ofEpochSecond(t), 8, 30, algorithme)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("Base 32 : RFC 4648, casse, espaces et remplissage ignorés, caractères étrangers refusés")
    void base32() {
        assertThat(Totp.base32("foobar".getBytes(StandardCharsets.US_ASCII))).isEqualTo("MZXW6YTBOI");
        assertThat(Totp.base32("f".getBytes(StandardCharsets.US_ASCII))).isEqualTo("MY");
        assertThat(new String(Totp.depuisBase32("mzxw 6ytb oi======"), StandardCharsets.US_ASCII)).isEqualTo("foobar");
        assertThatThrownBy(() -> Totp.depuisBase32("MZ1!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Totp.depuisBase32("====")).isInstanceOf(IllegalArgumentException.class);
        String s = Totp.nouveauSecret(new SecureRandom());
        assertThat(s).matches("[A-Z2-7]{32}");
        assertThat(Totp.depuisBase32(s)).hasSize(Totp.OCTETS_SECRET);
        assertThatThrownBy(() -> Totp.code(SHA1, 1, 6, "HmacInconnu")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Vérification : tolérance d'un pas de part et d'autre, pas au-delà ; codes mal formés refusés")
    void verification() {
        String secret = Totp.base32(SHA1);
        Instant t = Instant.ofEpochSecond(1_700_000_000L);
        String code = Totp.code(secret, t, 6, 30, "HmacSHA1");
        assertThat(Totp.verifier(secret, code, t, 6, 30, "HmacSHA1", 1)).isTrue();
        assertThat(Totp.verifier(secret, code, t.plusSeconds(30), 6, 30, "HmacSHA1", 1)).isTrue();
        assertThat(Totp.verifier(secret, code, t.minusSeconds(30), 6, 30, "HmacSHA1", 1)).isTrue();
        assertThat(Totp.verifier(secret, code, t.plusSeconds(90), 6, 30, "HmacSHA1", 1)).isFalse();
        assertThat(Totp.verifier(secret, code, t.plusSeconds(30), 6, 30, "HmacSHA1", 0)).isFalse();
        assertThat(Totp.verifier(secret, null, t, 6, 30, "HmacSHA1", 1)).isFalse();
        assertThat(Totp.verifier(secret, "12345", t, 6, 30, "HmacSHA1", 1)).isFalse();
        assertThat(Totp.verifier(secret, "abcdef", t, 6, 30, "HmacSHA1", 1)).isFalse();
        assertThat(Totp.verifier("!!", code, t, 6, 30, "HmacSHA1", 1)).isFalse();
    }

    @Test
    @DisplayName("URI du code QR : émetteur et identifiant encodés, algorithme sans préfixe Hmac")
    void uri() {
        assertThat(Totp.uri("awa mballa", "ABC", 8, 60, "HmacSHA256")).isEqualTo(
            "otpauth://totp/GIMAC%20DES:awa%20mballa?secret=ABC&issuer=GIMAC%20DES&digits=8&period=60&algorithm=SHA256"
        );
    }

    @Test
    @DisplayName("Jetons de lien : 32 octets en base64url (43 caractères), empreinte SHA-256, forme contrôlée")
    void jetons() {
        String j = JetonsLien.nouveau(new SecureRandom());
        assertThat(j).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(JetonsLien.bienForme(j)).isTrue();
        assertThat(JetonsLien.bienForme(null)).isFalse();
        assertThat(JetonsLien.bienForme(j + "=")).isFalse();
        assertThat(JetonsLien.empreinte("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(JetonsLien.nouveau(new SecureRandom())).isNotEqualTo(j);
        assertThatThrownBy(() -> JetonsLien.condensat("INCONNU")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Violations de la politique : longueur (plancher 12), classes de caractères, identifiant")
    void violations() {
        PolitiqueSecurite p = new PolitiqueSecurite(14, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
        assertThat(PolitiqueMotDePasse.violations(p, "Conforme-2026-OK", "agent")).isEmpty();
        assertThat(PolitiqueMotDePasse.violations(p, null, "agent")).containsExactly("LONGUEUR", "MAJUSCULE", "MINUSCULE", "CHIFFRE", "SPECIAL");
        assertThat(PolitiqueMotDePasse.violations(p, "tout-en-minuscules-agent", "AGENT")).containsExactly("MAJUSCULE", "CHIFFRE", "IDENTIFIANT");
        PolitiqueSecurite souple = new PolitiqueSecurite(8, 0, 0, 0, 0, 0, 0, false, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
        assertThat(PolitiqueMotDePasse.violations(souple, "agentagentagent", "agent")).isEmpty();
        assertThat(PolitiqueMotDePasse.violations(souple, "courtcourt", " ")).containsExactly("LONGUEUR");
    }

    @Test
    @DisplayName("Modèle : actions sous leurs anciens noms, couche du compte, étapes restantes, finalités, courriel sans corps en trace")
    void modele() {
        assertThat(ActionRequise.lire("UPDATE_PASSWORD")).contains(ActionRequise.MOT_DE_PASSE);
        assertThat(ActionRequise.lire(" configure_totp ")).contains(ActionRequise.SECOND_FACTEUR);
        assertThat(ActionRequise.lire("UPDATE_PROFILE")).contains(ActionRequise.FICHE);
        assertThat(ActionRequise.lire("CHARTE")).contains(ActionRequise.CHARTE);
        assertThat(ActionRequise.lire("VERIFY_EMAIL")).contains(ActionRequise.VERIFIER_COURRIEL);
        assertThat(ActionRequise.lire("TERMS_AND_CONDITIONS")).isEmpty();
        assertThat(ActionRequise.lire(null)).isEmpty();

        assertThat(Couche.lire("portail")).isEqualTo(Couche.PORTAIL);
        assertThat(Couche.lire(null)).isEqualTo(Couche.DES);
        assertThat(Couche.lire("inconnue")).isEqualTo(Couche.DES);
        Compte interne = new Compte("i", "agent", null, null, "a@gimac.test", true, true, null, null, null, "fr", null, true, Set.of(), List.of(), true);
        Compte correspondant = new Compte("c", "corr", null, null, "c@banque.test", true, true, null, null, null, "fr", "INST-1", true, Set.of(), List.of(), true);
        Compte vide = new Compte("v", "vide", null, null, null, false, true, null, null, null, "fr", " ", true, Set.of(), List.of(), true);
        assertThat(Couche.pour(interne, Couche.CONSOLE)).isEqualTo(Couche.CONSOLE);
        assertThat(Couche.pour(interne, Couche.PORTAIL)).isEqualTo(Couche.DES);
        assertThat(Couche.pour(correspondant, Couche.DES)).isEqualTo(Couche.PORTAIL);
        assertThat(Couche.pour(vide, Couche.DES)).isEqualTo(Couche.DES);

        assertThat(interne.etapesRestantes()).isEmpty();
        assertThat(interne.premierAccesEnAttente()).isFalse();
        EtatConnexion e = EtatConnexion.de(interne);
        assertThat(e.actif()).isTrue();
        assertThat(new EtatConnexion(false, false, null).actionsRequises()).isEmpty();

        assertThat(FinaliteJeton.INVITATION.validite(Duration.ofHours(24), Duration.ofMinutes(30))).isEqualTo(Duration.ofHours(24));
        assertThat(FinaliteJeton.REINITIALISATION.validite(Duration.ofHours(24), Duration.ofMinutes(30))).isEqualTo(Duration.ofMinutes(30));
        assertThat(new Courriel("a@gimac.test", "Objet", "jeton=secret", "<p>jeton=secret</p>").toString()).isEqualTo("Courriel[objet=Objet]");
    }
}
