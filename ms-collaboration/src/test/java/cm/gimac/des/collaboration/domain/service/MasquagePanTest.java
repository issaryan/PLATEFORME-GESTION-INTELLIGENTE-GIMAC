package cm.gimac.des.collaboration.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RG-52 : masquage des numéros de carte avant l'enregistrement d'un message. */
class MasquagePanTest {

    final MasquagePan masquage = new MasquagePan();

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = {
            "Carte 4111111111111111 bloquée|Carte 411111******1111 bloquée|1",
            "Carte 4111 1111 1111 1111.|Carte 411111******1111.|1",
            "PAN 4111-1111-1111-1111 à vérifier|PAN 411111******1111 à vérifier|1",
            "13 chiffres 4222222222222 fin|13 chiffres 422222***2222 fin|1",
            "19 chiffres 4917610000000000003|19 chiffres 491761*********0003|1",
            "Deux : 4111111111111111 et 5500000000000004|Deux : 411111******1111 et 550000******0004|2",
        }
    )
    @DisplayName("Suite de 13 à 19 chiffres valide au contrôle de Luhn : 6 premiers, astérisques, 4 derniers")
    void masque(String entree, String attendu, int nombre) {
        MasquagePan.Resultat r = masquage.masquer(entree);
        assertThat(r.texte()).isEqualTo(attendu);
        assertThat(r.masques()).isEqualTo(nombre);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = {
            "Référence 4111111111111112 (Luhn invalide)",
            "Téléphone 237699001122",
            "Compte 12345678901234567890123 trop long",
            "Montant 1 250 000 FCFA",
            "LIA-2026-027 et TCK-000318",
        }
    )
    @DisplayName("Rien n'est masqué hors d'un numéro de carte")
    void laisse(String entree) {
        MasquagePan.Resultat r = masquage.masquer(entree);
        assertThat(r.texte()).isEqualTo(entree);
        assertThat(r.masques()).isZero();
    }

    @Test
    @DisplayName("Texte vide ou absent, caractères spéciaux de remplacement conservés")
    void bords() {
        assertThat(masquage.masquer(null).texte()).isNull();
        assertThat(masquage.masquer("").masques()).isZero();
        assertThat(masquage.masquer("Coût $1 pour 4111111111111111 \\ fin").texte()).isEqualTo("Coût $1 pour 411111******1111 \\ fin");
        assertThat(MasquagePan.luhn("79927398713")).isTrue();
        assertThat(MasquagePan.luhn("79927398710")).isFalse();
    }
}
