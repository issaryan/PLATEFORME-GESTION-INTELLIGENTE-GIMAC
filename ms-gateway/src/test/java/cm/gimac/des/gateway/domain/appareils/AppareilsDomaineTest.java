package cm.gimac.des.gateway.domain.appareils;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Appareils connectés (docs/lot1/mon-compte.md §3) : analyse locale du User-Agent, masquage des adresses, résultats. */
class AppareilsDomaineTest {

    @ParameterizedTest(name = "{1} / {2} / {3}")
    @CsvSource(
        delimiter = '|',
        value = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36|Chrome 128|Windows 10/11|ORDINATEUR",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.2739.42|Edge 128|Windows 10/11|ORDINATEUR",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0|Firefox 115|Windows 7|ORDINATEUR",
            "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko|Internet Explorer 11|Windows 8.1|ORDINATEUR",
            "Mozilla/5.0 (Windows NT 6.2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0 Safari/537.36 OPR/95.0|Opera 95|Windows 8|ORDINATEUR",
            "Mozilla/5.0 (Windows NT 5.1) Gecko/20100101 Firefox/52.0|Firefox 52|Windows|ORDINATEUR",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15|Safari 17|macOS|ORDINATEUR",
            "Mozilla/5.0 (Macintosh; Intel) Gecko Firefox/128.0|Firefox 128|macOS|ORDINATEUR",
            "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0|Firefox 128|Linux|ORDINATEUR",
            "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36|Chrome 120|ChromeOS|ORDINATEUR",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1|Safari 17|iOS 17|MOBILE",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/128.0.6613.98 Mobile/15E148 Safari/604.1|Chrome 128|iOS 17|MOBILE",
            "Mozilla/5.0 (iPad; CPU OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1|Safari 16|iPadOS 16|TABLETTE",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0.0.0 Mobile Safari/537.36|Samsung Internet 25|Android 14|MOBILE",
            "Mozilla/5.0 (Linux; Android 13; SM-X200) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36|Chrome 128|Android 13|TABLETTE",
            "Mozilla/5.0 (Android 14; Mobile; rv:131.0) Gecko/131.0 Firefox/131.0|Firefox 131|Android 14|MOBILE",
            "curl/8.5.0|Inconnu|Inconnu|INCONNU",
        }
    )
    @DisplayName("User-Agent : navigateur et version majeure, système, catégorie")
    void analyse(String ua, String navigateur, String systeme, String type) {
        Appareil a = AnalyseurUserAgent.analyser(ua);
        assertThat(a.navigateur()).isEqualTo(navigateur);
        assertThat(a.systeme()).isEqualTo(systeme);
        assertThat(a.type()).isEqualTo(TypeAppareil.valueOf(type));
    }

    @Test
    @DisplayName("User-Agent absent, vide ou démesuré : « Inconnu », libellés bornés")
    void limites() {
        assertThat(AnalyseurUserAgent.analyser(null)).isEqualTo(Appareil.inconnu());
        assertThat(AnalyseurUserAgent.analyser("  ")).isEqualTo(Appareil.inconnu());
        String long_ = "Mozilla/5.0 (Windows NT 10.0) Firefox/1" + "2".repeat(600);
        Appareil a = AnalyseurUserAgent.analyser(long_);
        assertThat(a.navigateur().length()).isLessThanOrEqualTo(60);
        assertThat(a.systeme()).isEqualTo("Windows 10/11");
        assertThat(AnalyseurUserAgent.analyser("Firefox/131.0 " + "x".repeat(600) + " Windows NT 10.0").systeme()).isEqualTo(Appareil.INCONNU);
        assertThat(new Appareil(null, " ", null)).isEqualTo(Appareil.inconnu());
    }

    @Test
    @DisplayName("Empreinte d'un appareil : familles sans versions (une mise à jour n'est pas un nouvel appareil)")
    void empreinte() {
        assertThat(new Appareil("Firefox 131", "Windows 10/11", TypeAppareil.ORDINATEUR).empreinte()).isEqualTo(new Appareil("Firefox 132", "Windows 10/11", TypeAppareil.ORDINATEUR).empreinte());
        assertThat(new Appareil("Safari 17", "iOS 17", TypeAppareil.MOBILE).empreinte()).isEqualTo("safari|ios").isEqualTo(new Appareil("Safari 18", "iOS 18.1", TypeAppareil.MOBILE).empreinte());
        assertThat(new Appareil("Firefox 131", "Windows 10/11", TypeAppareil.ORDINATEUR).empreinte()).isNotEqualTo(new Appareil("Chrome 131", "Windows 10/11", TypeAppareil.ORDINATEUR).empreinte());
        assertThat(new Appareil("Samsung Internet 25", "Android 14", TypeAppareil.MOBILE).empreinte()).isEqualTo("samsung internet|android");
    }

    @Test
    @DisplayName("Adresse masquée au dernier octet (IPv4) ou au dernier groupe (IPv6) ; valeur douteuse entièrement masquée")
    void masque() {
        assertThat(MasqueAdresseIp.masquer("41.202.10.20")).isEqualTo("41.202.10.*");
        assertThat(MasqueAdresseIp.masquer(" 10.0.0.1 ")).isEqualTo("10.0.0.*");
        assertThat(MasqueAdresseIp.masquer("2001:db8:0:0:0:0:0:1")).isEqualTo("2001:db8:0:0:0:0:0:*");
        assertThat(MasqueAdresseIp.masquer("::1")).isEqualTo("::*");
        assertThat(MasqueAdresseIp.masquer("inconnue")).isEqualTo("*");
        assertThat(MasqueAdresseIp.masquer("1.2.3")).isEqualTo("*");
        assertThat(MasqueAdresseIp.masquer(".1")).isEqualTo("*");
        assertThat(MasqueAdresseIp.masquer(null)).isEqualTo("*");
        assertThat(MasqueAdresseIp.masquer("")).isEqualTo("*");
    }

    @Test
    @DisplayName("Résultat d'un échec : mot de passe, code, blocage ; requête invalide et panne non inscrites")
    void resultats() {
        assertThat(ResultatTentative.pour(CauseEchec.IDENTIFIANTS_INVALIDES, false)).contains(ResultatTentative.ECHEC_MOT_DE_PASSE);
        assertThat(ResultatTentative.pour(CauseEchec.IDENTIFIANTS_INVALIDES, true)).contains(ResultatTentative.ECHEC_CODE);
        assertThat(ResultatTentative.pour(CauseEchec.TROP_DE_TENTATIVES, false)).contains(ResultatTentative.BLOQUEE);
        assertThat(ResultatTentative.pour(CauseEchec.COMPTE_INACTIF, false)).contains(ResultatTentative.BLOQUEE);
        assertThat(ResultatTentative.pour(CauseEchec.COUCHE_REFUSEE, true)).contains(ResultatTentative.BLOQUEE);
        assertThat(ResultatTentative.pour(CauseEchec.REQUETE_INVALIDE, false)).isEqualTo(Optional.empty());
        assertThat(ResultatTentative.pour(CauseEchec.SERVICE_INDISPONIBLE, false)).isEmpty();
    }
}
