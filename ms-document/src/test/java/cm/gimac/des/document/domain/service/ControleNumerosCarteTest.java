package cm.gimac.des.document.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** BNF-16, RG-52 : détection des numéros de carte au dépôt et masquage 6+4. */
class ControleNumerosCarteTest {

    @ParameterizedTest(name = "« {0} » : {1}")
    @CsvSource(
        delimiter = '|',
        value = {
            "Carte 4111111111111111 du client | 1",
            "4222222222222 | 1",
            "Numéro sur 19 chiffres : 4520030000000000007. | 1",
            "Groupée : 4111 1111 1111 1111 fin | 1",
            "Groupée : 5500-0000-0000-0004 fin | 1",
            "Deux cartes 4111111111111111 et 6759649826438453 | 2",
            "Luhn faux 4111111111111112 | 0",
            "Douze chiffres 123456789012 | 0",
            "Vingt chiffres 00000000000000000000 | 0",
            "Déjà masquée 411111******1111 | 0",
            "Séparateurs mélangés 4111 1111-1111 1111 | 0",
            "RCCM RC/DLA/2024/B/1234 du 12/03/2024 | 0",
        }
    )
    @DisplayName("Compte les numéros de carte plausibles (13 à 19 chiffres vérifiant Luhn)")
    void compter(String texte, int attendu) {
        assertThat(ControleNumerosCarte.compter(texte)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("Texte absent ou vide : aucun numéro")
    void texteVide() {
        assertThat(ControleNumerosCarte.compter(null)).isZero();
        assertThat(ControleNumerosCarte.compter("")).isZero();
    }

    @Test
    @DisplayName("Masquage 6+4 des seuls numéros vérifiant Luhn, de 13 à 19 chiffres")
    void masquer() {
        assertThat(ControleNumerosCarte.masquer("4111111111111111")).isEqualTo("411111******1111");
        assertThat(ControleNumerosCarte.masquer("4222222222222")).isEqualTo("422222***2222");
        assertThat(ControleNumerosCarte.masquer("4111111111111112")).isEqualTo("4111111111111112");
        assertThat(ControleNumerosCarte.masquer("123456789012")).isEqualTo("123456789012");
        assertThat(ControleNumerosCarte.masquer("00000000000000000000")).isEqualTo("00000000000000000000");
        assertThat(ControleNumerosCarte.masquer(null)).isNull();
    }

    @Test
    @DisplayName("Formule de Luhn : faux pour un caractère autre qu'un chiffre")
    void luhn() {
        assertThat(ControleNumerosCarte.luhnValide("4111111111111111")).isTrue();
        assertThat(ControleNumerosCarte.luhnValide("4111111111111112")).isFalse();
        assertThat(ControleNumerosCarte.luhnValide("41111111111111a1")).isFalse();
        assertThat(ControleNumerosCarte.luhnValide("4111 1111")).isFalse();
    }
}
