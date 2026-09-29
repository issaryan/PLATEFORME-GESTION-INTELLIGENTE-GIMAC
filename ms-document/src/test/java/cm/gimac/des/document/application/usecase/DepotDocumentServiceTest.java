package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Fichier;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Resultat;
import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.unit.DataSize;

/** Cas d'utilisation du dépôt direct, ports simulés. */
class DepotDocumentServiceTest {

    static final Instant MAINTENANT = Instant.parse("2026-09-29T10:15:00Z");
    static final byte[] CONTENU = "Statuts de la Banque B".getBytes(StandardCharsets.UTF_8);
    static final Deposant AGENT = new Deposant("7c1e0000-0000-0000-0000-000000000001", null);

    StockageObjet stockage;
    ExtracteurTexte extracteur;
    DepotDocumentRepository repository;
    DepotDocumentService service;

    @BeforeEach
    void init() {
        stockage = mock(StockageObjet.class);
        extracteur = mock(ExtracteurTexte.class);
        repository = mock(DepotDocumentRepository.class);
        service = new DepotDocumentService(stockage, extracteur, repository, DataSize.ofMegabytes(20), Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        when(extracteur.extraire(any(), anyString())).thenReturn(Optional.of("Statuts de la Banque B"));
        when(stockage.deposer(anyString(), any(), anyString(), anyString())).thenAnswer(i -> new StockageObjet.ObjetDepose(i.getArgument(0), "v1"));
    }

    static Commande commande(byte[] contenu) {
        return new Commande(new Fichier("C:\\depot\\statuts.txt", "text/plain", contenu), "adhesion", "inst-1/STATUTS");
    }

    @Test
    @DisplayName("Dépôt : empreinte calculée par le serveur, objet WORM puis métadonnées")
    void depot() {
        Resultat r = service.deposer(commande(CONTENU), new Deposant("agent", "inst-1"));

        String sha = EmpreinteSha256.de(CONTENU);
        assertThat(r.sha256()).isEqualTo(sha);
        assertThat(r.nomFichier()).isEqualTo("statuts.txt");
        assertThat(r.taille()).isEqualTo(CONTENU.length);
        assertThat(r.typeMime()).isEqualTo("text/plain");

        String cle = "adhesion/2026/09/29/" + r.id();
        verify(stockage).deposer(eq(cle), eq(CONTENU), eq("text/plain"), eq(sha));
        ArgumentCaptor<DepotDocument> ligne = ArgumentCaptor.forClass(DepotDocument.class);
        verify(repository).saveAndFlush(ligne.capture());
        DepotDocument d = ligne.getValue();
        assertThat(d.getId()).isEqualTo(r.id());
        assertThat(d.getFinalite()).isEqualTo("ADHESION");
        assertThat(d.getReference()).isEqualTo("inst-1/STATUTS");
        assertThat(d.getNomFichier()).isEqualTo("statuts.txt");
        assertThat(d.getTaille()).isEqualTo(CONTENU.length);
        assertThat(d.getTypeMime()).isEqualTo("text/plain");
        assertThat(d.getSha256()).isEqualTo(sha);
        assertThat(d.getCleObjet()).isEqualTo(cle);
        assertThat(d.getVersionObjet()).isEqualTo("v1");
        assertThat(d.getDeposant()).isEqualTo("agent");
        assertThat(d.getInstitutionId()).isEqualTo("inst-1");
        assertThat(d.getDeposeLe()).isEqualTo(MAINTENANT);
        assertThat(d.toString()).contains("ADHESION").doesNotContain("inst-1/STATUTS");
        assertThat(d).isEqualTo(d).isNotEqualTo(new DepotDocument().id(UUID.randomUUID())).isNotEqualTo(null);
        assertThat(d.hashCode()).isEqualTo(new DepotDocument().hashCode());
    }

    @Test
    @DisplayName("BNF-16 : numéro de carte détecté, 422 DOC_PAN_DETECTED, rien n'est conservé")
    void numeroDeCarte() {
        when(extracteur.extraire(any(), anyString())).thenReturn(Optional.of("Carte 4111 1111 1111 1111"));

        assertThatThrownBy(() -> service.deposer(commande(CONTENU), AGENT))
            .isInstanceOfSatisfying(DocumentException.class, e -> {
                assertThat(e.code()).isEqualTo("DOC_PAN_DETECTED");
                assertThat(e.status()).isEqualTo(422);
                assertThat(e.details()).containsEntry("occurrences", 1);
                assertThat(e.getMessage()).doesNotContain("4111");
            });
        verifyNoInteractions(stockage, repository);
    }

    @Test
    @DisplayName("Type non contrôlable (image) : accepté sans extraction de texte")
    void typeNonControle() {
        when(extracteur.extraire(any(), anyString())).thenReturn(Optional.empty());
        Resultat r = service.deposer(new Commande(new Fichier("scan.png", "image/png", CONTENU), "ADHESION", "ref"), AGENT);
        assertThat(r.typeMime()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Fichier absent, vide ou trop volumineux : refusé avant tout dépôt")
    void admission() {
        assertThatThrownBy(() -> service.deposer(new Commande(null, "ADHESION", "ref"), AGENT)).hasFieldOrPropertyWithValue("code", "DOC_FILE_MISSING");
        assertThatThrownBy(() -> service.deposer(commande(new byte[0]), AGENT)).hasFieldOrPropertyWithValue("code", "DOC_FILE_EMPTY");
        byte[] gros = new byte[(int) DataSize.ofMegabytes(20).toBytes() + 1];
        assertThatThrownBy(() -> service.deposer(commande(gros), AGENT)).hasFieldOrPropertyWithValue("status", 413);
        assertThatThrownBy(() -> service.deposer(new Commande(new Fichier("a", "text/plain", CONTENU), "", "ref"), AGENT)).hasFieldOrPropertyWithValue(
            "code",
            "DOC_PURPOSE_INVALID"
        );
        assertThatThrownBy(() -> service.deposer(new Commande(new Fichier("a", "text/plain", CONTENU), "ADHESION", null), AGENT)).hasFieldOrPropertyWithValue(
            "code",
            "DOC_REFERENCE_INVALID"
        );
        verifyNoInteractions(stockage, repository);
    }

    @Test
    @DisplayName("Dépôt objet indisponible : 503, aucune métadonnée inscrite")
    void stockageIndisponible() {
        when(stockage.deposer(anyString(), any(), anyString(), anyString())).thenThrow(
            DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "Dépôt objet indisponible")
        );
        assertThatThrownBy(() -> service.deposer(commande(CONTENU), AGENT)).hasFieldOrPropertyWithValue("code", "DOC_STORAGE_UNAVAILABLE");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Échec d'écriture des métadonnées : l'erreur remonte (objet orphelin journalisé)")
    void metadonneesEnEchec() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("ux_depot_document_cle_objet"));
        assertThatThrownBy(() -> service.deposer(commande(CONTENU), AGENT)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Empreinte : lue en base ; document inconnu, 404 DOC_NOT_FOUND")
    void empreinte() {
        UUID connu = UUID.randomUUID();
        UUID inconnu = UUID.randomUUID();
        when(repository.findSha256ById(connu)).thenReturn(Optional.of("a".repeat(64)));
        when(repository.findSha256ById(inconnu)).thenReturn(Optional.empty());

        assertThat(service.empreinte(connu)).isEqualTo("a".repeat(64));
        assertThatThrownBy(() -> service.empreinte(inconnu)).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(404);
            assertThat(e.code()).isEqualTo("DOC_NOT_FOUND");
            assertThat(e.details()).containsEntry("id", inconnu.toString());
        });
    }

    @Test
    @DisplayName("Constructeur Spring : horloge du système")
    void constructeurSpring() {
        DepotDocumentService s = new DepotDocumentService(stockage, extracteur, repository, DataSize.ofMegabytes(1));
        assertThat(s.deposer(commande(CONTENU), AGENT).id()).isNotNull();
    }
}
