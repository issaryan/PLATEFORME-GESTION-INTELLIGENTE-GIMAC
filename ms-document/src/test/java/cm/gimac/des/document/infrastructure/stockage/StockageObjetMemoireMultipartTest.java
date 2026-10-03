package cm.gimac.des.document.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.out.StockageObjet.EcritureWorm;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.application.port.out.StockageObjet.Partie;
import cm.gimac.des.document.domain.DocumentException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Substitut en mémoire, téléversements multipart (D-74) : il doit suivre les règles de S3 pour que
 * les tests des cas d'utilisation aient un sens (remplacement d'une partie, parties citées, ordre,
 * ETag, taille minimale, téléversement inconnu après assemblage ou abandon).
 */
class StockageObjetMemoireMultipartTest {

    /** Parties de 4 octets au moins, sauf la dernière. */
    final StockageObjetMemoire stockage = new StockageObjetMemoire(4);

    static byte[] lire(Flux f) {
        try (InputStream in = f.contenu()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void ferme(ThrowingCallable appel) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.code()).isEqualTo("DOC_UPLOAD_CLOSED");
        });
    }

    @Test
    @DisplayName("Dépôt en cours : parties dans le désordre, partie renvoyée remplacée, assemblage des parties citées dans l'ordre")
    void assemblage() {
        String envoi = stockage.ouvrirDepot("d1", "text/plain");
        String e2 = stockage.deposerPartie("d1", envoi, 2, new byte[] { 5, 6 });
        stockage.deposerPartie("d1", envoi, 1, new byte[] { 0, 0, 0, 0 });
        String e1 = stockage.deposerPartie("d1", envoi, 1, new byte[] { 1, 2, 3, 4 });
        String e3 = stockage.deposerPartie("d1", envoi, 3, new byte[] { 9 });
        assertThat(e1).startsWith("\"").endsWith("\"").isNotEqualTo(e2);
        assertThat(stockage.envoisEnCours()).isEqualTo(1);

        stockage.assemblerDepot("d1", envoi, List.of(new Partie(1, e1), new Partie(2, e2)));

        Flux f = stockage.lireDepot("d1").orElseThrow();
        assertThat(f.taille()).isEqualTo(6);
        assertThat(lire(f)).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(stockage.depotExiste("d1")).isTrue();
        assertThat(stockage.envoisEnCours()).isZero();
        // Téléversement inconnu après assemblage : aucune partie ni nouvel assemblage.
        ferme(() -> stockage.deposerPartie("d1", envoi, 3, new byte[] { 9 }));
        ferme(() -> stockage.assemblerDepot("d1", envoi, List.of(new Partie(3, e3))));

        stockage.supprimerDepot("d1");
        assertThat(stockage.lireDepot("d1")).isEmpty();
        stockage.supprimerDepot("d1");
    }

    @Test
    @DisplayName("Règles de l'assemblage : aucune partie, ordre non croissant, ETag différent ou partie absente, partie trop petite hors la dernière")
    void reglesAssemblage() {
        String envoi = stockage.ouvrirDepot("d2", "text/plain");
        String e1 = stockage.deposerPartie("d2", envoi, 1, new byte[] { 1, 2 });
        String e2 = stockage.deposerPartie("d2", envoi, 2, new byte[] { 3, 4, 5, 6 });

        assertThatThrownBy(() -> stockage.assemblerDepot("d2", envoi, List.of())).hasMessageContaining("MalformedXML");
        assertThatThrownBy(() -> stockage.assemblerDepot("d2", envoi, List.of(new Partie(2, e2), new Partie(1, e1)))).hasMessageContaining(
            "InvalidPartOrder"
        );
        assertThatThrownBy(() -> stockage.assemblerDepot("d2", envoi, List.of(new Partie(1, e2)))).hasMessageContaining("InvalidPart");
        assertThatThrownBy(() -> stockage.assemblerDepot("d2", envoi, List.of(new Partie(5, e2)))).hasMessageContaining("InvalidPart");
        assertThatThrownBy(() -> stockage.assemblerDepot("d2", envoi, List.of(new Partie(1, e1), new Partie(2, e2)))).hasMessageContaining(
            "EntityTooSmall"
        );
        assertThatThrownBy(() -> stockage.deposerPartie("d2", envoi, 0, new byte[] { 1 })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stockage.deposerPartie("d2", envoi, 10_001, new byte[] { 1 })).isInstanceOf(IllegalArgumentException.class);
        // Une seule partie, aussi petite soit-elle, est acceptée.
        stockage.assemblerDepot("d2", envoi, List.of(new Partie(2, e2)));
        assertThat(lire(stockage.lireDepot("d2").orElseThrow())).containsExactly(3, 4, 5, 6);
    }

    @Test
    @DisplayName("Abandon : téléversement inconnu ensuite ; abandon renvoyé sans effet ; autre clé refusée")
    void abandon() {
        String envoi = stockage.ouvrirDepot("d3", "text/plain");
        ferme(() -> stockage.deposerPartie("autre-cle", envoi, 1, new byte[] { 1 }));
        ferme(() -> stockage.deposerPartie("d3", null, 1, new byte[] { 1 }));
        stockage.abandonnerDepot("d3", envoi);
        stockage.abandonnerDepot("d3", envoi);
        ferme(() -> stockage.deposerPartie("d3", envoi, 1, new byte[] { 1 }));
        assertThat(stockage.envoisEnCours()).isZero();
        assertThat(stockage.lireDepot("d3")).isEmpty();
    }

    @Test
    @DisplayName("Écriture WORM en flux : parties numérotées dans l'ordre, objet versionné à la fin ; abandon, aucun objet")
    void ecritureWorm() {
        EcritureWorm e = stockage.ouvrirEcriture("w/1", "text/plain");
        byte[] tranche = { 1, 2, 3, 4, 5 };
        e.ecrire(tranche, 4);
        tranche[0] = 9;
        e.ecrire(tranche, 2);
        ObjetDepose o = e.terminer();
        assertThat(o).isEqualTo(new ObjetDepose("w/1", "v1"));
        assertThat(stockage.lire("w/1")).hasValueSatisfying(b -> assertThat(b).containsExactly(1, 2, 3, 4, 9, 2));
        Flux f = stockage.lireFlux("w/1").orElseThrow();
        assertThat(f.taille()).isEqualTo(6);
        assertThat(lire(f)).containsExactly(1, 2, 3, 4, 9, 2);
        ferme(() -> e.ecrire(tranche, 1));
        ferme(e::terminer);

        EcritureWorm abandonnee = stockage.ouvrirEcriture("w/2", "text/plain");
        abandonnee.ecrire(tranche, 5);
        abandonnee.abandonner();
        assertThat(stockage.lire("w/2")).isEmpty();
        assertThat(stockage.lireFlux("w/2")).isEmpty();
        assertThat(stockage.objetsWorm()).isEqualTo(1);

        EcritureWorm laissee = stockage.ouvrirEcriture("w/3", "text/plain");
        assertThat(stockage.envoisEnCours()).isEqualTo(1);
        stockage.abandonnerEcriture("w/3", laissee.envoi());
        stockage.abandonnerEcriture("w/3", laissee.envoi());
        assertThat(stockage.envoisEnCours()).isZero();

        EcritureWorm petite = stockage.ouvrirEcriture("w/4", "text/plain");
        petite.ecrire(tranche, 2);
        petite.ecrire(tranche, 2);
        assertThatThrownBy(petite::terminer).hasMessageContaining("EntityTooSmall");
    }

    @Test
    @DisplayName("Panne simulée : toute opération répond 503 DOC_STORAGE_UNAVAILABLE, jusqu'au rétablissement")
    void panne() {
        String envoi = stockage.ouvrirDepot("d4", "text/plain");
        stockage.simulerPanne(true);
        List<ThrowingCallable> operations = List.of(
            () -> stockage.ouvrirDepot("d5", "text/plain"),
            () -> stockage.deposerPartie("d4", envoi, 1, new byte[] { 1 }),
            () -> stockage.assemblerDepot("d4", envoi, List.of()),
            () -> stockage.abandonnerDepot("d4", envoi),
            () -> stockage.lireDepot("d4"),
            () -> stockage.supprimerDepot("d4"),
            () -> stockage.ouvrirEcriture("w", "text/plain"),
            () -> stockage.abandonnerEcriture("w", "u"),
            () -> stockage.lireFlux("w"),
            () -> stockage.lire("w"),
            () -> stockage.deposer("w", new byte[] { 1 }, "text/plain", "a".repeat(64))
        );
        for (ThrowingCallable o : operations) {
            assertThatThrownBy(o).hasFieldOrPropertyWithValue("code", "DOC_STORAGE_UNAVAILABLE");
        }
        stockage.simulerPanne(false);
        assertThat(stockage.deposerPartie("d4", envoi, 1, new byte[] { 1 })).isNotBlank();
        assertThat(new StockageObjetMemoire().versions("x")).isZero();
    }
}
