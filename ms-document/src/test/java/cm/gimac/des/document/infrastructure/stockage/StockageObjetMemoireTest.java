package cm.gimac.des.document.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Substitut en mémoire du dépôt objet : compartiment versionné, rien n'est écrasé. */
class StockageObjetMemoireTest {

    final StockageObjetMemoire stockage = new StockageObjetMemoire();

    @Test
    @DisplayName("Dépôt puis lecture : contenu restitué à l'identique, copie défensive")
    void depotLecture() {
        byte[] contenu = { 1, 2, 3 };
        ObjetDepose o = stockage.deposer("adhesion/2026/09/29/x", contenu, "application/pdf", "a".repeat(64));
        contenu[0] = 9;

        assertThat(o.cle()).isEqualTo("adhesion/2026/09/29/x");
        assertThat(o.version()).isEqualTo("v1");
        byte[] lu = stockage.lire("adhesion/2026/09/29/x").orElseThrow();
        assertThat(lu).containsExactly(1, 2, 3);
        lu[1] = 9;
        assertThat(stockage.lire("adhesion/2026/09/29/x").orElseThrow()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Même clé : nouvelle version, la précédente est conservée")
    void versions() {
        stockage.deposer("k", new byte[] { 1 }, "text/plain", "a".repeat(64));
        ObjetDepose v2 = stockage.deposer("k", new byte[] { 2 }, "text/plain", "b".repeat(64));

        assertThat(v2.version()).isEqualTo("v2");
        assertThat(stockage.versions("k")).isEqualTo(2);
        assertThat(stockage.lire("k").orElseThrow()).containsExactly(2);
    }

    @Test
    @DisplayName("Clé inconnue : vide")
    void inconnue() {
        assertThat(stockage.lire("absente")).isEmpty();
        assertThat(stockage.versions("absente")).isZero();
    }
}
