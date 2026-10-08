package cm.gimac.des.audit.domain.registre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Arbre de Merkle du registre (CC §6.1.3.3, RFC 9162). */
class ArbreMerkleTest {

    static String sha256(byte[]... morceaux) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        for (byte[] m : morceaux) {
            d.update(m);
        }
        return HexFormat.of().formatHex(d.digest());
    }

    @Test
    @DisplayName("Séparation de domaine : feuille SHA256(0x00 || canon), nœud SHA256(0x01 || g || d) ; arbre vide : SHA256 de rien")
    void empreintes() throws Exception {
        String f = ArbreMerkle.feuille("{\"a\":1}");
        assertThat(f).isEqualTo(sha256(new byte[] { 0 }, "{\"a\":1}".getBytes(StandardCharsets.UTF_8)));
        String g = ArbreMerkle.feuille("g");
        assertThat(ArbreMerkle.noeud(g, f)).isEqualTo(sha256(new byte[] { 1 }, HexFormat.of().parseHex(g), HexFormat.of().parseHex(f)));
        assertThat(ArbreMerkle.racine(List.of())).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(ArbreMerkle.racine(List.of(f))).isEqualTo(f);
    }

    @Test
    @DisplayName("Inscription incrémentale : les sous-arbres complets inscrits redonnent la racine MTH pour chaque taille de 1 à 33")
    void incremental() {
        Map<String, String> noeuds = new HashMap<>();
        ArbreMerkle.Lecteur lecteur = (n, p) -> noeuds.get(n + "/" + p);
        List<String> feuilles = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            String f = ArbreMerkle.feuille("{\"i\":" + i + "}");
            feuilles.add(f);
            List<ArbreMerkle.Noeud> nouveaux = ArbreMerkle.noeudsAInscrire(i, f, lecteur);
            // Nombre de nœuds inscrits : 1 + nombre de bits à 1 de poids faible de l'index.
            assertThat(nouveaux).hasSize(1 + Long.numberOfTrailingZeros(~(long) i));
            nouveaux.forEach(n -> noeuds.put(n.niveau() + "/" + n.position(), n.empreinte()));
            assertThat(ArbreMerkle.racine(i + 1L, lecteur)).isEqualTo(ArbreMerkle.racine(feuilles));
        }
        // Racine à 3 feuilles : MTH = H(H(f0, f1), f2).
        assertThat(ArbreMerkle.racine(feuilles.subList(0, 3))).isEqualTo(ArbreMerkle.noeud(ArbreMerkle.noeud(feuilles.get(0), feuilles.get(1)), feuilles.get(2)));
    }

    @Test
    @DisplayName("Frère gauche manquant : registre incohérent signalé")
    void incoherent() {
        assertThatThrownBy(() -> ArbreMerkle.noeudsAInscrire(1, ArbreMerkle.feuille("x"), (n, p) -> null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ArbreMerkle.racine(2, (n, p) -> null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Natures : types de signature reconnus, domaine et action ; autres types ignorés")
    void natures() {
        assertThat(NatureSignature.deType("pilotage.process-step-signed.v1")).contains(NatureSignature.PROCESSUS);
        assertThat(NatureSignature.PROCESSUS.action()).isEqualTo("SIGNATURE_PROCESSUS");
        assertThat(NatureSignature.OPERATION.domaine()).isEqualTo("homologation");
        assertThat(NatureSignature.deType("homologation.operation-signed.v1")).contains(NatureSignature.OPERATION);
        assertThat(NatureSignature.deType("pilotage.process-step-activated.v1")).isEmpty();
        assertThat(NatureSignature.deType(null)).isEmpty();
    }
}
