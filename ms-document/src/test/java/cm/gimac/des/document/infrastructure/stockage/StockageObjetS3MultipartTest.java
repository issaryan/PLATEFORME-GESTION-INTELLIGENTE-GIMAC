package cm.gimac.des.document.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.port.out.StockageObjet.EcritureWorm;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.application.port.out.StockageObjet.Partie;
import cm.gimac.des.document.domain.DocumentException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DefaultRetention;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.ObjectLockConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Adaptateur S3 réel, client simulé : écriture WORM en flux (multipart, Content-MD5 par partie),
 * lecture en flux, et compartiment des dépôts en cours (D-74).
 */
class StockageObjetS3MultipartTest {

    S3Client client;
    StockageObjetS3 stockage;

    @BeforeEach
    void init() {
        client = mock(S3Client.class);
        stockage = new StockageObjetS3(client, StockageObjetS3Test.PROPRIETES);
    }

    static NoSuchUploadException televersementInconnu() {
        return NoSuchUploadException.builder().message("NoSuchUpload").build();
    }

    static S3Exception erreurS3(int statut, String code) {
        return (S3Exception) S3Exception.builder().statusCode(statut).awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build()).build();
    }

    static void erreur(ThrowingCallable appel, int statut, String code) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(statut);
            assertThat(e.code()).isEqualTo(code);
        });
    }

    static byte[] corps(RequestBody b) throws IOException {
        try (InputStream in = b.contentStreamProvider().newStream()) {
            return in.readAllBytes();
        }
    }

    // ------------------------------------------------------------------ dépôt WORM

    @Test
    @DisplayName("Écriture WORM en flux : téléversement ouvert dans le compartiment WORM, une partie par tranche avec Content-MD5, assemblage dans l'ordre")
    void ecritureWorm() throws IOException {
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u-worm").build()
        );
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenReturn(
            UploadPartResponse.builder().eTag("\"e1\"").build(),
            UploadPartResponse.builder().eTag("\"e2\"").build()
        );
        when(client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(
            CompleteMultipartUploadResponse.builder().versionId("v-7").build()
        );

        EcritureWorm e = stockage.ouvrirEcriture("messagerie/2026/10/02/d", "video/mp4");
        assertThat(e.envoi()).isEqualTo("u-worm");
        byte[] tranche = "abcdefgh".getBytes(StandardCharsets.US_ASCII);
        e.ecrire(tranche, 8);
        e.ecrire(tranche, 3);
        ObjetDepose o = e.terminer();

        assertThat(o).isEqualTo(new ObjetDepose("messagerie/2026/10/02/d", "v-7"));
        ArgumentCaptor<CreateMultipartUploadRequest> ouverture = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client).createMultipartUpload(ouverture.capture());
        assertThat(ouverture.getValue().bucket()).isEqualTo("gimac-documents");
        assertThat(ouverture.getValue().key()).isEqualTo("messagerie/2026/10/02/d");
        assertThat(ouverture.getValue().contentType()).isEqualTo("video/mp4");

        ArgumentCaptor<UploadPartRequest> parties = ArgumentCaptor.forClass(UploadPartRequest.class);
        ArgumentCaptor<RequestBody> corps = ArgumentCaptor.forClass(RequestBody.class);
        verify(client, times(2)).uploadPart(parties.capture(), corps.capture());
        UploadPartRequest p1 = parties.getAllValues().get(0);
        UploadPartRequest p2 = parties.getAllValues().get(1);
        assertThat(p1.bucket()).isEqualTo("gimac-documents");
        assertThat(p1.uploadId()).isEqualTo("u-worm");
        assertThat(p1.partNumber()).isEqualTo(1);
        assertThat(p1.contentLength()).isEqualTo(8L);
        assertThat(p1.contentMD5()).isEqualTo(StockageObjetS3.md5(tranche, 8));
        assertThat(p2.partNumber()).isEqualTo(2);
        assertThat(p2.contentLength()).isEqualTo(3L);
        // MD5 de « abc » (RFC 1321).
        assertThat(p2.contentMD5()).isEqualTo("kAFQmDzST7DWlj99KOF/cg==");
        assertThat(corps(corps.getAllValues().get(1))).containsExactly('a', 'b', 'c');
        // Corps relisible (nouvelle tentative du SDK).
        assertThat(corps(corps.getAllValues().get(1))).hasSize(3);

        ArgumentCaptor<CompleteMultipartUploadRequest> fin = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(client).completeMultipartUpload(fin.capture());
        assertThat(fin.getValue().bucket()).isEqualTo("gimac-documents");
        assertThat(fin.getValue().uploadId()).isEqualTo("u-worm");
        assertThat(fin.getValue().multipartUpload().parts()).extracting(p -> p.partNumber() + ":" + p.eTag()).containsExactly("1:\"e1\"", "2:\"e2\"");
    }

    @Test
    @DisplayName("Écriture WORM abandonnée : AbortMultipartUpload ; téléversement déjà inconnu, sans effet ; panne, 503")
    void ecritureWormAbandonnee() {
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u-worm").build()
        );
        when(client.abortMultipartUpload(any(AbortMultipartUploadRequest.class))).thenReturn(
            AbortMultipartUploadResponse.builder().build()
        );
        stockage.ouvrirEcriture("k", null).abandonner();
        ArgumentCaptor<AbortMultipartUploadRequest> abandon = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(client).abortMultipartUpload(abandon.capture());
        assertThat(abandon.getValue().bucket()).isEqualTo("gimac-documents");
        assertThat(abandon.getValue().key()).isEqualTo("k");
        assertThat(abandon.getValue().uploadId()).isEqualTo("u-worm");

        when(client.abortMultipartUpload(any(AbortMultipartUploadRequest.class))).thenThrow(televersementInconnu());
        stockage.abandonnerEcriture("k", "u-ancien");

        when(client.abortMultipartUpload(any(AbortMultipartUploadRequest.class))).thenThrow(SdkClientException.create("connexion refusée"));
        erreur(() -> stockage.abandonnerEcriture("k", "u"), 503, "DOC_STORAGE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Écriture WORM : type par défaut application/octet-stream ; panne à l'ouverture ou pendant l'écriture, 503")
    void ecritureWormEnPanne() {
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u").build()
        );
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenThrow(erreurS3(500, "InternalError"));
        EcritureWorm e = stockage.ouvrirEcriture("k", null);
        erreur(() -> e.ecrire(new byte[] { 1 }, 1), 503, "DOC_STORAGE_UNAVAILABLE");
        ArgumentCaptor<CreateMultipartUploadRequest> ouverture = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client).createMultipartUpload(ouverture.capture());
        assertThat(ouverture.getValue().contentType()).isEqualTo("application/octet-stream");

        S3Client enPanne = mock(S3Client.class);
        when(enPanne.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenThrow(SdkClientException.create("injoignable"));
        erreur(() -> new StockageObjetS3(enPanne, StockageObjetS3Test.PROPRIETES).ouvrirEcriture("k", "text/plain"), 503, "DOC_STORAGE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Lecture WORM en flux : contenu et taille annoncée ; clé inconnue, vide ; panne, 503")
    void lectureEnFlux() throws IOException {
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(
            new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(3L).build(),
                AbortableInputStream.create(new ByteArrayInputStream(new byte[] { 7, 8, 9 }))
            )
        );
        Flux f = stockage.lireFlux("k").orElseThrow();
        assertThat(f.taille()).isEqualTo(3);
        try (InputStream in = f.contenu()) {
            assertThat(in.readAllBytes()).containsExactly(7, 8, 9);
        }
        ArgumentCaptor<GetObjectRequest> requete = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObject(requete.capture());
        assertThat(requete.getValue().bucket()).isEqualTo("gimac-documents");
        assertThat(requete.getValue().key()).isEqualTo("k");

        when(client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("absent").build());
        assertThat(stockage.lireFlux("absente")).isEmpty();

        S3Client enPanne = mock(S3Client.class);
        when(enPanne.getObject(any(GetObjectRequest.class))).thenThrow(erreurS3(503, "SlowDown"));
        erreur(() -> new StockageObjetS3(enPanne, StockageObjetS3Test.PROPRIETES).lireFlux("k"), 503, "DOC_STORAGE_UNAVAILABLE");
    }

    // ------------------------------------------------------------------ dépôts en cours

    @Test
    @DisplayName("Dépôt en cours : ouverture, parties (Content-MD5) puis assemblage dans le compartiment des dépôts, sans verrouillage")
    void depotEnCours() throws IOException {
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u-dep").build()
        );
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenReturn(UploadPartResponse.builder().eTag("\"e3\"").build());
        when(client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(CompleteMultipartUploadResponse.builder().build());

        assertThat(stockage.ouvrirDepot("id-1", "video/mp4")).isEqualTo("u-dep");
        byte[] octets = { 1, 2, 3, 4 };
        assertThat(stockage.deposerPartie("id-1", "u-dep", 3, octets)).isEqualTo("\"e3\"");
        stockage.assemblerDepot("id-1", "u-dep", List.of(new Partie(1, "\"e1\""), new Partie(2, "\"e2\"")));

        ArgumentCaptor<CreateMultipartUploadRequest> ouverture = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client).createMultipartUpload(ouverture.capture());
        assertThat(ouverture.getValue().bucket()).isEqualTo("gimac-depots");
        ArgumentCaptor<UploadPartRequest> partie = ArgumentCaptor.forClass(UploadPartRequest.class);
        ArgumentCaptor<RequestBody> corps = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).uploadPart(partie.capture(), corps.capture());
        assertThat(partie.getValue().bucket()).isEqualTo("gimac-depots");
        assertThat(partie.getValue().key()).isEqualTo("id-1");
        assertThat(partie.getValue().partNumber()).isEqualTo(3);
        assertThat(partie.getValue().contentLength()).isEqualTo(4L);
        assertThat(partie.getValue().contentMD5()).isEqualTo(StockageObjetS3.md5(octets, 4));
        assertThat(corps(corps.getValue())).containsExactly(1, 2, 3, 4);
        ArgumentCaptor<CompleteMultipartUploadRequest> fin = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(client).completeMultipartUpload(fin.capture());
        assertThat(fin.getValue().bucket()).isEqualTo("gimac-depots");
        assertThat(fin.getValue().multipartUpload().parts()).hasSize(2);
    }

    @Test
    @DisplayName("Partie d'un téléversement inconnu (abandonné ou expiré) : 409 DOC_UPLOAD_CLOSED, y compris signalé par le seul code d'erreur ; panne, 503")
    void partieTeleversementInconnu() {
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenThrow(televersementInconnu());
        erreur(() -> stockage.deposerPartie("id", "u", 1, new byte[] { 1 }), 409, "DOC_UPLOAD_CLOSED");

        S3Client autre = mock(S3Client.class);
        when(autre.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenThrow(erreurS3(404, "NoSuchUpload"));
        erreur(() -> new StockageObjetS3(autre, StockageObjetS3Test.PROPRIETES).deposerPartie("id", "u", 1, new byte[] { 1 }), 409, "DOC_UPLOAD_CLOSED");

        S3Client enPanne = mock(S3Client.class);
        when(enPanne.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenThrow(erreurS3(500, null));
        erreur(() -> new StockageObjetS3(enPanne, StockageObjetS3Test.PROPRIETES).deposerPartie("id", "u", 1, new byte[] { 1 }), 503, "DOC_STORAGE_UNAVAILABLE");

        S3Client sansDetails = mock(S3Client.class);
        when(sansDetails.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenThrow(
            (S3Exception) S3Exception.builder().statusCode(500).build()
        );
        erreur(() -> new StockageObjetS3(sansDetails, StockageObjetS3Test.PROPRIETES).deposerPartie("id", "u", 1, new byte[] { 1 }), 503, "DOC_STORAGE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Assemblage d'un téléversement inconnu : déjà assemblé (objet présent), accepté ; sinon 409 DOC_UPLOAD_CLOSED ; panne, 503")
    void assemblageRepris() {
        when(client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(televersementInconnu());
        when(client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().contentLength(26L).build());
        stockage.assemblerDepot("id", "u", List.of(new Partie(1, "\"e\"")));
        ArgumentCaptor<HeadObjectRequest> tete = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(client).headObject(tete.capture());
        assertThat(tete.getValue().bucket()).isEqualTo("gimac-depots");
        assertThat(tete.getValue().key()).isEqualTo("id");

        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("absent").build());
        erreur(() -> stockage.assemblerDepot("id", "u", List.of(new Partie(1, "\"e\""))), 409, "DOC_UPLOAD_CLOSED");

        S3Client enPanne = mock(S3Client.class);
        when(enPanne.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(SdkClientException.create("injoignable"));
        erreur(
            () -> new StockageObjetS3(enPanne, StockageObjetS3Test.PROPRIETES).assemblerDepot("id", "u", List.of(new Partie(1, "\"e\""))),
            503,
            "DOC_STORAGE_UNAVAILABLE"
        );
    }

    @Test
    @DisplayName("Dépôt en cours : abandon (inconnu sans effet), lecture en flux, suppression, dans le compartiment des dépôts")
    void abandonLectureSuppression() throws IOException {
        when(client.abortMultipartUpload(any(AbortMultipartUploadRequest.class))).thenThrow(televersementInconnu());
        stockage.abandonnerDepot("id", "u");
        ArgumentCaptor<AbortMultipartUploadRequest> abandon = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(client).abortMultipartUpload(abandon.capture());
        assertThat(abandon.getValue().bucket()).isEqualTo("gimac-depots");

        when(client.getObject(any(GetObjectRequest.class))).thenReturn(
            new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(2L).build(),
                AbortableInputStream.create(new ByteArrayInputStream(new byte[] { 5, 6 }))
            )
        );
        Flux f = stockage.lireDepot("id").orElseThrow();
        assertThat(f.taille()).isEqualTo(2);
        f.contenu().close();
        ArgumentCaptor<GetObjectRequest> lecture = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObject(lecture.capture());
        assertThat(lecture.getValue().bucket()).isEqualTo("gimac-depots");

        when(client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());
        stockage.supprimerDepot("id");
        ArgumentCaptor<DeleteObjectRequest> suppression = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(suppression.capture());
        assertThat(suppression.getValue().bucket()).isEqualTo("gimac-depots");
        assertThat(suppression.getValue().key()).isEqualTo("id");

        S3Client enPanne = mock(S3Client.class);
        when(enPanne.deleteObject(any(DeleteObjectRequest.class))).thenThrow(SdkClientException.create("injoignable"));
        erreur(() -> new StockageObjetS3(enPanne, StockageObjetS3Test.PROPRIETES).supprimerDepot("id"), 503, "DOC_STORAGE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Rétention par défaut du compartiment WORM (lue au démarrage) posée sur l'écriture en flux, jamais sur un dépôt en cours")
    void retentionExplicite() {
        when(client.getObjectLockConfiguration(any(GetObjectLockConfigurationRequest.class))).thenReturn(
            GetObjectLockConfigurationResponse.builder()
                .objectLockConfiguration(
                    ObjectLockConfiguration.builder()
                        .objectLockEnabled(ObjectLockEnabled.ENABLED)
                        .rule(r -> r.defaultRetention(d -> d.mode(ObjectLockRetentionMode.COMPLIANCE).years(10)))
                        .build()
                )
                .build()
        );
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u").build()
        );
        StockageObjetS3 verrouille = new StockageObjetS3(client, StockageObjetS3Test.exigeant());
        Instant avant = Instant.now();
        verrouille.ouvrirEcriture("k", "text/plain");
        verrouille.ouvrirDepot("d", "text/plain");

        ArgumentCaptor<CreateMultipartUploadRequest> ouvertures = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client, times(2)).createMultipartUpload(ouvertures.capture());
        CreateMultipartUploadRequest worm = ouvertures.getAllValues().get(0);
        assertThat(worm.objectLockMode()).isEqualTo(ObjectLockMode.COMPLIANCE);
        assertThat(worm.objectLockRetainUntilDate()).isBetween(
            avant.atZone(ZoneOffset.UTC).plusYears(10).toInstant(),
            Instant.now().atZone(ZoneOffset.UTC).plusYears(10).toInstant()
        );
        CreateMultipartUploadRequest depot = ouvertures.getAllValues().get(1);
        assertThat(depot.objectLockMode()).isNull();
        assertThat(depot.objectLockRetainUntilDate()).isNull();
    }

    @Test
    @DisplayName("Échéance de rétention : années et jours ajoutés au calendrier UTC ; sans règle par défaut, aucune rétention explicite")
    void echeance() {
        Instant t = Instant.parse("2026-10-02T08:00:00Z");
        assertThat(StockageObjetS3.echeance(DefaultRetention.builder().days(1).build(), t)).isEqualTo(Instant.parse("2026-10-03T08:00:00Z"));
        assertThat(StockageObjetS3.echeance(DefaultRetention.builder().years(10).build(), t)).isEqualTo(Instant.parse("2036-10-02T08:00:00Z"));

        when(client.getObjectLockConfiguration(any(GetObjectLockConfigurationRequest.class))).thenReturn(
            GetObjectLockConfigurationResponse.builder()
                .objectLockConfiguration(ObjectLockConfiguration.builder().objectLockEnabled(ObjectLockEnabled.ENABLED).build())
                .build()
        );
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u").build()
        );
        new StockageObjetS3(client, StockageObjetS3Test.exigeant()).ouvrirEcriture("k", "text/plain");
        ArgumentCaptor<CreateMultipartUploadRequest> ouverture = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client).createMultipartUpload(ouverture.capture());
        assertThat(ouverture.getValue().objectLockMode()).isNull();
    }

    @Test
    @DisplayName("Client S3 : compartiment des dépôts en cours lu dans les propriétés")
    void compartimentDesDepots() {
        StockageS3Proprietes p = new StockageS3Proprietes(URI.create("http://x"), null, "a", "b", "worm", "en-cours", false);
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("u").build()
        );
        new StockageObjetS3(client, p).ouvrirDepot("id", "text/plain");
        ArgumentCaptor<CreateMultipartUploadRequest> ouverture = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client).createMultipartUpload(ouverture.capture());
        assertThat(ouverture.getValue().bucket()).isEqualTo("en-cours");
    }
}
