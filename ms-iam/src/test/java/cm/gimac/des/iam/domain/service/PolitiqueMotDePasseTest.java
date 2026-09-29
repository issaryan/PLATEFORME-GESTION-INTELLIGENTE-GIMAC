package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Politique de sécurité configurée depuis la console (D-62) : syntaxe du fournisseur et planchers. */
class PolitiqueMotDePasseTest {

    static PolitiqueSecurite p(int longueur, boolean forceBrute) {
        return new PolitiqueSecurite(longueur, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA1", 30, 10, forceBrute, 5, 15);
    }

    @Test
    @DisplayName("Conversion vers la syntaxe du fournisseur puis relecture")
    void allerRetour() {
        String c = PolitiqueMotDePasse.versChaine(p(14, true));
        assertThat(c).isEqualTo("length(14) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and passwordHistory(5) and forceExpiredPasswordChange(90) and notUsername(undefined)");
        Map<String, Integer> r = PolitiqueMotDePasse.lire(c);
        assertThat(r).containsEntry("length", 14).containsEntry("passwordHistory", 5).containsKey("notUsername");
        assertThat(PolitiqueMotDePasse.lire(null)).isEmpty();
        assertThat(PolitiqueMotDePasse.lire("notUsername()")).containsEntry("notUsername", 1);
        PolitiqueSecurite sansRegles = new PolitiqueSecurite(12, 0, 0, 0, 0, 0, 0, false, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
        assertThat(PolitiqueMotDePasse.versChaine(sansRegles)).isEqualTo("length(12)");
    }

    @Test
    @DisplayName("Une politique conforme passe ; plus faible que les planchers, elle est refusée champ par champ")
    void verification() {
        assertThatCode(() -> PolitiqueMotDePasse.verifier(p(12, true))).doesNotThrowAnyException();
        PolitiqueSecurite faible = new PolitiqueSecurite(8, 5, 5, 5, 5, 30, 400, false, 7, 10, "MD5", 1, 48, false, 50, 0);
        assertThatThrownBy(() -> PolitiqueMotDePasse.verifier(faible)).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_POLICY_INVALID");
            @SuppressWarnings("unchecked")
            Map<String, Object> champs = (Map<String, Object>) e.details().get("champs");
            assertThat(champs).containsKeys(
                "longueurMinimale", "classes", "historique", "expirationJours", "otpChiffres", "otpPeriodeSecondes", "otpAlgorithme",
                "sessionInactiviteMinutes", "sessionDureeMaxHeures", "forceBruteActive", "forceBruteEchecsMax", "forceBruteAttenteMinutes"
            );
        });
        PolitiqueSecurite trop = new PolitiqueSecurite(200, 0, 0, 0, 0, -1, -1, true, 8, 60, "HmacSHA512", 120, 24, true, 3, 1441);
        assertThatThrownBy(() -> PolitiqueMotDePasse.verifier(trop)).isInstanceOf(IamException.class);
    }
}
