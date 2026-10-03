package cm.gimac.des.document.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.domain.DocumentException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Adaptateur S3 réel (Ceph RGW, D-50), client S3 simulé. */
class StockageObjetS3Test {

    static final StockageS3Proprietes PROPRIETES = new StockageS3Proprietes(
        URI.create("http://localhost:7480"),
        null,
        "cle",
        "secret",
        "gimac-documents",
        "gimac-depots",
        false
    );

    S3Client client;
    StockageObjetS3 stockage;

    @BeforeEach
    void init() {
        client = mock(S3Client.class);
        stockage = new StockageObjetS3(client, PROPRIETES);
    }

    @Test
    @DisplayName("Dépôt : compartiment, clé, type, taille et empreinte en métadonnée ; version restituée")
    void depot() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().versionId("v-42").build());

        ObjetDepose o = stockage.deposer("adhesion/2026/09/29/id", new byte[] { 1, 2, 3 }, "application/pdf", "a".repeat(64));

        assertThat(o).isEqualTo(new ObjetDepose("adhesion/2026/09/29/id", "v-42"));
        ArgumentCaptor<PutObjectRequest> requete = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> corps = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).putObject(requete.capture(), corps.capture());
        assertThat(requete.getValue().bucket()).isEqualTo("gimac-documents");
        assertThat(requete.getValue().key()).isEqualTo("adhesion/2026/09/29/id");
        assertThat(requete.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(requete.getValue().contentLength()).isEqualTo(3L);
        assertThat(requete.getValue().metadata()).containsEntry("sha256", "a".repeat(64));
        assertThat(requete.getValue().contentMD5()).isEqualTo("Uonfc331cyb83SJZevsfrA==");
        assertThat(corps.getValue().optionalContentLength()).contains(3L);
    }

    @Test
    @DisplayName("Dépôt : passerelle injoignable ou refus S3, 503 DOC_STORAGE_UNAVAILABLE")
    void depotIndisponible() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(SdkClientException.create("connexion refusée"));
        assertThatThrownBy(() -> stockage.deposer("k", new byte[] { 1 }, "text/plain", "a".repeat(64))).isInstanceOfSatisfying(
            DocumentException.class,
            e -> {
                assertThat(e.status()).isEqualTo(503);
                assertThat(e.code()).isEqualTo("DOC_STORAGE_UNAVAILABLE");
            }
        );
    }

    @Test
    @DisplayName("Lecture : contenu de l'objet ; clé inconnue, vide ; panne, 503")
    void lecture() {
        GetObjectRequest attendue = GetObjectRequest.builder().bucket("gimac-documents").key("k").build();
        when(client.getObjectAsBytes(attendue)).thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] { 7 }));
        assertThat(stockage.lire("k")).hasValueSatisfying(b -> assertThat(b).containsExactly(7));

        when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("absent").build());
        assertThat(stockage.lire("absente")).isEmpty();

        S3Client enPanne = mock(S3Client.class);
        when(enPanne.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(500).message("panne").build());
        assertThatThrownBy(() -> new StockageObjetS3(enPanne, PROPRIETES).lire("k")).hasFieldOrPropertyWithValue("code", "DOC_STORAGE_UNAVAILABLE");
    }

    static StockageS3Proprietes exigeant() {
        return new StockageS3Proprietes(URI.create("http://localhost:7480"), "us-east-1", "cle", "secret", "gimac-documents", "gimac-depots", true);
    }

    @Test
    @DisplayName("Verrouillage exigé et actif : démarrage accepté")
    void verrouillageActif() {
        when(client.getObjectLockConfiguration(any(GetObjectLockConfigurationRequest.class))).thenReturn(
            GetObjectLockConfigurationResponse.builder()
                .objectLockConfiguration(ObjectLockConfiguration.builder().objectLockEnabled(ObjectLockEnabled.ENABLED).build())
                .build()
        );
        assertThat(new StockageObjetS3(client, exigeant())).isNotNull();
    }

    @Test
    @DisplayName("Verrouillage exigé mais absent ou illisible : démarrage refusé")
    void verrouillageAbsent() {
        when(client.getObjectLockConfiguration(any(GetObjectLockConfigurationRequest.class))).thenReturn(
            GetObjectLockConfigurationResponse.builder().build()
        );
        assertThatThrownBy(() -> new StockageObjetS3(client, exigeant())).isInstanceOf(IllegalStateException.class).hasMessageContaining("WORM");

        when(client.getObjectLockConfiguration(any(GetObjectLockConfigurationRequest.class))).thenReturn(
            GetObjectLockConfigurationResponse.builder().objectLockConfiguration(ObjectLockConfiguration.builder().build()).build()
        );
        assertThatThrownBy(() -> new StockageObjetS3(client, exigeant())).isInstanceOf(IllegalStateException.class);

        S3Client sansConfiguration = mock(S3Client.class);
        when(sansConfiguration.getObjectLockConfiguration(any(GetObjectLockConfigurationRequest.class))).thenThrow(
            S3Exception.builder().statusCode(404).message("ObjectLockConfigurationNotFoundError").build()
        );
        assertThatThrownBy(() -> new StockageObjetS3(sansConfiguration, exigeant()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("gimac-documents");
    }

    @Test
    @DisplayName("Verrouillage non exigé (DEV) : aucun appel au démarrage")
    void verrouillageNonExige() {
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("Client S3 : construit pour Ceph RGW ; propriétés obligatoires contrôlées")
    void client() {
        try (S3Client c = StockageObjetS3.client(PROPRIETES)) {
            assertThat(c.serviceClientConfiguration().region().id()).isEqualTo("us-east-1");
            assertThat(c.serviceClientConfiguration().endpointOverride()).contains(URI.create("http://localhost:7480"));
        }
        assertThatThrownBy(() -> StockageObjetS3.client(new StockageS3Proprietes(null, null, "a", "b", null, null, false))).hasMessageContaining("point-acces");
        assertThatThrownBy(() -> StockageObjetS3.client(new StockageS3Proprietes(URI.create("http://x"), null, null, "b", null, null, false))).hasMessageContaining(
            "cle-acces"
        );
        assertThatThrownBy(() -> StockageObjetS3.client(new StockageS3Proprietes(URI.create("http://x"), null, "a", null, null, null, false))).hasMessageContaining(
            "cle-secrete"
        );
    }

    @Test
    @DisplayName("Propriétés : région et compartiment par défaut")
    void proprietesParDefaut() {
        StockageS3Proprietes p = new StockageS3Proprietes(URI.create("http://x"), " ", "a", "b", "", null, false);
        assertThat(p.region()).isEqualTo("us-east-1");
        assertThat(p.compartiment()).isEqualTo("gimac-documents");
        assertThat(p.compartimentDepots()).isEqualTo("gimac-depots");
        assertThat(new StockageS3Proprietes(URI.create("http://x"), "eu-west-3", "a", "b", "autre", "autres-depots", true).compartiment()).isEqualTo("autre");
        assertThat(new StockageS3Proprietes(URI.create("http://x"), null, "a", "b", null, " ", true).compartimentDepots()).isEqualTo("gimac-depots");
        assertThat(new StockageS3Proprietes(URI.create("http://x"), null, "a", "b", null, "autres-depots", true).compartimentDepots()).isEqualTo(
            "autres-depots"
        );
    }
}
