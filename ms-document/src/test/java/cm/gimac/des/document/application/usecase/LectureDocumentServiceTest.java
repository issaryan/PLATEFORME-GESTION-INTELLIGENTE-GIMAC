package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.port.in.LireDocumentUseCase.Contenu;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture d'un document par un service interne, ports simulés. */
class LectureDocumentServiceTest {

    static final UUID ID = UUID.randomUUID();
    static final byte[] OCTETS = "Compte rendu".getBytes(StandardCharsets.UTF_8);
    static final String CLE = "messagerie/2026/09/30/" + ID;

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
                    .nomFichier("compte-rendu.txt")
                    .typeMime("text/plain")
                    .taille(OCTETS.length)
                    .sha256(EmpreinteSha256.de(OCTETS))
                    .cleObjet(CLE)
            )
        );
    }

    @Test
    @DisplayName("Lecture : contenu relu du dépôt WORM, empreinte vérifiée, métadonnées inscrites restituées")
    void lecture() {
        when(stockage.lire(CLE)).thenReturn(Optional.of(OCTETS));

        Contenu c = service.lire(ID, "collaboration-service");

        assertThat(c.id()).isEqualTo(ID);
        assertThat(c.nomFichier()).isEqualTo("compte-rendu.txt");
        assertThat(c.typeMime()).isEqualTo("text/plain");
        assertThat(c.sha256()).isEqualTo(EmpreinteSha256.de(OCTETS));
        assertThat(c.octets()).isEqualTo(OCTETS);
        assertThat(c.taille()).isEqualTo(OCTETS.length);
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
        verifyNoInteractions(stockage);
    }

    @Test
    @DisplayName("Objet absent du dépôt WORM : 500 DOC_CONTENT_MISSING")
    void objetAbsent() {
        when(stockage.lire(CLE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lire(ID, "collaboration-service")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(500);
            assertThat(e.code()).isEqualTo("DOC_CONTENT_MISSING");
            assertThat(e.details()).containsEntry("id", ID.toString());
        });
    }

    @Test
    @DisplayName("Contenu altéré (empreinte différente) : 500 DOC_INTEGRITY_MISMATCH, rien n'est restitué")
    void contenuAltere() {
        when(stockage.lire(CLE)).thenReturn(Optional.of("Compte rendu modifié".getBytes(StandardCharsets.UTF_8)));
        assertThatThrownBy(() -> service.lire(ID, "collaboration-service")).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(500);
            assertThat(e.code()).isEqualTo("DOC_INTEGRITY_MISMATCH");
        });
    }
}
