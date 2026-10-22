package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pièce déjà conservée par son empreinte (D-120, contrat de MS-HOMOLOGATION). */
class RechercheParEmpreinteServiceTest {

    final DepotDocumentRepository documents = mock(DepotDocumentRepository.class);
    final RechercheParEmpreinteService service = new RechercheParEmpreinteService(documents);

    @Test
    @DisplayName("Empreinte connue : identifiant, nom, type et empreinte ; inconnue : 404 ; mal formée : 422")
    void parEmpreinte() {
        String sha = "b".repeat(64);
        UUID id = UUID.randomUUID();
        when(documents.findFirstBySha256OrderByDeposeLeAsc(sha)).thenReturn(Optional.of(new DepotDocument().id(id).nomFichier("p.pdf").typeMime("application/pdf").sha256(sha)));
        assertThat(service.parEmpreinte(sha)).isEqualTo(new RechercheParEmpreinteService.Piece(id, "p.pdf", "application/pdf", sha));
        when(documents.findFirstBySha256OrderByDeposeLeAsc("c".repeat(64))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.parEmpreinte("c".repeat(64))).hasFieldOrPropertyWithValue("status", 404);
        for (String mauvaise : new String[] { null, "B".repeat(64), "b".repeat(63), "z".repeat(64) }) {
            assertThatThrownBy(() -> service.parEmpreinte(mauvaise)).hasFieldOrPropertyWithValue("status", 422).hasFieldOrPropertyWithValue("code", "DOC_SHA256_INVALIDE");
        }
    }
}
