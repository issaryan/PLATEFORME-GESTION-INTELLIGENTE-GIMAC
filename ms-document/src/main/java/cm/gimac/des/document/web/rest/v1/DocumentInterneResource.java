package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.web.rest.v1.DocumentVues.EmpreinteVue;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interface interne de MS-DOCUMENT, réservée aux jetons de service (INTERNAL_DOCUMENT_READ) :
 * MS-PARTICIPANT y revérifie l'empreinte déclarée d'une pièce avant de la rattacher au dossier.
 * Non exposée par la passerelle.
 */
@RestController
@RequestMapping("/api/v1/interne/documents")
public class DocumentInterneResource {

    private final DeposerDocumentUseCase useCase;

    public DocumentInterneResource(DeposerDocumentUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{id}/empreinte")
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_READ')")
    public EmpreinteVue empreinte(@PathVariable UUID id) {
        return new EmpreinteVue(useCase.empreinte(id));
    }
}
