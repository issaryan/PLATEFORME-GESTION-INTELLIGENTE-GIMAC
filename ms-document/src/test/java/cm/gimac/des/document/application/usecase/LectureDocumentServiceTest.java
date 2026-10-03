package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.port.in.LireDocumentUseCase.Contenu;
import cm.gimac.des.document.application.port.in.LireDocumentUseCase.Metadonnees;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture d'un document par un service interne, ports simulés : contenu en flux vérifié au fil de la lecture, métadonnées. */
class LectureDocumentServiceTest {

    static final UUID ID = UUID.randomUUID();
    static final byte[] OCTETS = "Compte rendu".getBytes(StandardCharsets.UTF_8);
    static final String CLE = "messagerie/2026/09/30/" + ID;
    static final Instant DEPOSE_LE = Instant.parse("2026-09-30T10:00:00Z");

    DepotDocumentRepository repository;
    StockageObjet stockage;
    LectureDocumentService service;

    @BeforeEach
    void init() {
        repository = mock(DepotDocumentRepository.class);
        stockage = mock(StockageObjet.class);
        service = new LectureDocumentService(repository, stockage);
        when(repository.findById(ID)).thenReturn(
            Optional.of(
                new DepotDocument()
                    .id(ID)
                    .finalite("MESSAGERIE")
                    .reference("conversation/c1")
                    .nomFichier("compte-rendu.txt")
                    .typeMime("text/plain")
                    .taille(OCTETS.length)
                    .sha256(EmpreinteSha256.de(OCTETS))
                    .cleObjet(CLE)
                    .deposant("agent")
                    .institutionId("inst-1")
                    .deposeLe(DEPOSE_LE)
            )
        );
    }

    static Optional<Flux> flux(byte[] octets) {
        return Optional.of(new Flux(new ByteArrayInputStream(octets), octets.length));
    }

    /** Lecture par petits morceaux, comme un convertisseur HTTP ; rend les octets reçus avant une éventuelle erreur. */
    static byte[] lireTout(InputStream in, ByteArrayOutputStream recus) throws IOException {
        byte[] tampon = new byte[5];
        int n;
        while ((n = in.read(tampon)) >= 0) {
            recus.write(tampon, 0, n);
        }
        return recus.toByteArray();
    }

    @Test
    @DisplayName("Lecture : contenu relu en flux, empreinte vérifiée, métadonnées inscrites restituées")
    void lecture() throws IOException {
        when(stockage.lireFlux(CLE)).thenReturn(flux(OCTETS));

        Contenu c = service.lire(ID, "collaboration-service");

        assertThat(c.id()).isEqualTo(ID);
        assertThat(c.nomFichier()).isEqualTo("compte-rendu.txt");
        assertThat(c.typeMime()).isEqualTo("text/plain");
        assertThat(c.sha256()).isEqualTo(EmpreinteSha256.de(OCTETS));
        assertThat(c.taille()).isEqualTo(OCTETS.length);
        try (InputStream in = c.flux()) {
            assertThat(lireTout(in, new ByteArrayOutputStream())).isEqualTo(OCTETS);
        }
    }

    @Test
    @DisplayName("Document inconnu : 404 DOC_NOT_FOUND, le dépôt objet n'est pas sollicité")
    void inconnu() {
        UUID inconnu = UUID.randomUUID();
        when(repository.findById(inconnu)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lire(inconnu, "collaboration-service")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(404);
            assertThat(e.code()).isEqualTo("DOC_NOT_FOUND");
        });
        assertThatThrownBy(() -> service.metadonnees(inconnu)).hasFieldOrPropertyWithValue("code", "DOC_NOT_FOUND");
        verifyNoInteractions(stockage);
    }

    @Test
    @DisplayName("Objet absent du dépôt WORM : 500 DOC_CONTENT_MISSING")
    void objetAbsent() {
        when(stockage.lireFlux(CLE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lire(ID, "collaboration-service")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(500);
            assertThat(e.code()).isEqualTo("DOC_CONTENT_MISSING");
            assertThat(e.details()).containsEntry("id", ID.toString());
        });
    }

    @Test
    @DisplayName("Objet d'une autre taille que la taille inscrite : 500 DOC_INTEGRITY_MISMATCH avant tout envoi, flux fermé")
    void tailleDifferente() {
        boolean[] ferme = { false };
        InputStream in = new ByteArrayInputStream(OCTETS) {
            @Override
            public void close() {
                ferme[0] = true;
            }
        };
        when(stockage.lireFlux(CLE)).thenReturn(Optional.of(new Flux(in, OCTETS.length + 1L)));
        assertThatThrownBy(() -> service.lire(ID, "collaboration-service")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(500);
            assertThat(e.code()).isEqualTo("DOC_INTEGRITY_MISMATCH");
        });
        assertThat(ferme[0]).isTrue();
    }

    @Test
    @DisplayName("Contenu altéré (empreinte différente) : la lecture échoue avant les derniers octets, le contenu n'est jamais restitué en entier")
    void contenuAltere() {
        byte[] altere = "Compte rendX".getBytes(StandardCharsets.UTF_8);
        when(stockage.lireFlux(CLE)).thenReturn(flux(altere));
        Contenu c = service.lire(ID, "collaboration-service");

        ByteArrayOutputStream recus = new ByteArrayOutputStream();
        assertThatThrownBy(() -> lireTout(c.flux(), recus)).isInstanceOf(IOException.class).hasMessageContaining("empreinte");
        assertThat(recus.size()).isLessThan(OCTETS.length);
    }

    @Test
    @DisplayName("Contenu plus court que la taille inscrite : la lecture échoue")
    void contenuTronque() {
        when(stockage.lireFlux(CLE)).thenReturn(Optional.of(new Flux(new ByteArrayInputStream("Compte".getBytes(StandardCharsets.UTF_8)), OCTETS.length)));
        Contenu c = service.lire(ID, "collaboration-service");
        assertThatThrownBy(() -> lireTout(c.flux(), new ByteArrayOutputStream())).isInstanceOf(IOException.class).hasMessageContaining("plus court");
    }

    @Test
    @DisplayName("Métadonnées : nom, taille, type, empreinte, finalité, référence, déposant, institution, date ; le dépôt objet n'est pas sollicité")
    void metadonnees() {
        Metadonnees m = service.metadonnees(ID);
        assertThat(m).isEqualTo(
            new Metadonnees(
                ID,
                "compte-rendu.txt",
                OCTETS.length,
                "text/plain",
                EmpreinteSha256.de(OCTETS),
                "MESSAGERIE",
                "conversation/c1",
                "agent",
                "inst-1",
                DEPOSE_LE
            )
        );
        verifyNoInteractions(stockage);
    }
}
