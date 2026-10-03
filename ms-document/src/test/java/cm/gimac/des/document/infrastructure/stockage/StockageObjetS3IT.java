package cm.gimac.des.document.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.config.S3TestContainer;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * Adaptateur S3 réel face à une vraie passerelle S3 (Testcontainers) : écriture, métadonnée
 * d'empreinte, relecture, clé absente, passerelle injoignable, contrôle du verrouillage d'objets.
 */
class StockageObjetS3IT {

    static final String COMPARTIMENT = "gimac-documents-it";
    static S3Client client;
    static StockageObjetS3 stockage;

    @BeforeAll
    static void demarrer() {
        client = S3TestContainer.client(COMPARTIMENT);
        client.createBucket(b -> b.bucket(COMPARTIMENT));
        stockage = new StockageObjetS3(client, S3TestContainer.proprietes(COMPARTIMENT, false));
    }

    @AfterAll
    static void arreter() {
        client.close();
    }

    @Test
    @DisplayName("Dépôt puis relecture : contenu identique, type et empreinte posés sur l'objet")
    void depotRelecture() {
        byte[] contenu = "Statuts de la Banque B".getBytes(StandardCharsets.UTF_8);
        String sha = EmpreinteSha256.de(contenu);
        String cle = "adhesion/2026/09/29/" + UUID.randomUUID();

        ObjetDepose o = stockage.deposer(cle, contenu, "text/plain", sha);

        assertThat(o.cle()).isEqualTo(cle);
        assertThat(stockage.lire(cle)).hasValueSatisfying(lu -> assertThat(lu).isEqualTo(contenu));
        HeadObjectResponse tete = client.headObject(b -> b.bucket(COMPARTIMENT).key(cle));
        assertThat(tete.contentType()).isEqualTo("text/plain");
        assertThat(tete.contentLength()).isEqualTo(contenu.length);
        assertThat(tete.metadata()).containsEntry("sha256", sha);
        assertThat(EmpreinteSha256.de(stockage.lire(cle).orElseThrow())).isEqualTo(sha);
    }

    @Test
    @DisplayName("Clé absente : vide")
    void cleAbsente() {
        assertThat(stockage.lire("adhesion/absente/" + UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Compartiment sans verrouillage d'objets : démarrage refusé si le verrouillage est exigé")
    void verrouillageExige() {
        assertThatThrownBy(() -> new StockageObjetS3(client, S3TestContainer.proprietes(COMPARTIMENT, true))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Compartiment créé avec verrouillage d'objets (WORM) : démarrage accepté, dépôt et relecture")
    void compartimentVerrouille() {
        String verrouille = "gimac-documents-worm-it";
        client.createBucket(b -> b.bucket(verrouille).objectLockEnabledForBucket(true));
        try (S3Client c = S3TestContainer.client(verrouille)) {
            StockageObjetS3 worm = new StockageObjetS3(c, S3TestContainer.proprietes(verrouille, true));
            byte[] contenu = { 4, 2 };
            String cle = "adhesion/worm/" + UUID.randomUUID();
            worm.deposer(cle, contenu, "application/octet-stream", EmpreinteSha256.de(contenu));
            assertThat(worm.lire(cle)).hasValueSatisfying(lu -> assertThat(lu).containsExactly(4, 2));
        }
    }

    @Test
    @DisplayName("Passerelle injoignable : 503 DOC_STORAGE_UNAVAILABLE")
    void passerelleInjoignable() {
        StockageS3Proprietes p = new StockageS3Proprietes(URI.create("http://127.0.0.1:1"), null, "a", "b", COMPARTIMENT, null, false);
        try (S3Client injoignable = StockageObjetS3.client(p)) {
            StockageObjetS3 s = new StockageObjetS3(injoignable, p);
            assertThatThrownBy(() -> s.deposer("k", new byte[] { 1 }, "text/plain", "a".repeat(64))).isInstanceOfSatisfying(DocumentException.class, e ->
                assertThat(e.code()).isEqualTo("DOC_STORAGE_UNAVAILABLE")
            );
            assertThatThrownBy(() -> s.lire("k")).isInstanceOf(DocumentException.class);
        }
    }
}
