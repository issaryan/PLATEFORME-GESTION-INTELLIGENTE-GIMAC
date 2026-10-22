package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.usecase.DepotPieceSandboxService;
import cm.gimac.des.document.application.usecase.DepotPieceSandboxService.Demande;
import cm.gimac.des.document.application.usecase.DepotPieceSandboxService.Resultat;
import cm.gimac.des.document.application.usecase.RechercheParEmpreinteService;
import cm.gimac.des.document.application.usecase.TypesPiecesSandboxService;
import cm.gimac.des.document.application.usecase.TypesPiecesSandboxService.PageQuarantaines;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Pièces de la Sandbox GIMAC (US-EXI-08, D-120) :
 * <ul>
 *   <li>{@code POST /api/v1/interne/pieces-sandbox} (jeton de service, INTERNAL_DOCUMENT_DEPOSIT) : dépôt par
 *   SBX-CAMPAGNE, SBX-TRACE ou SBX-ISO20022 ; 201 pièce scellée, 200 pièce déjà conservée, 202 quarantaine ;</li>
 *   <li>{@code GET /api/v1/sandbox/types-pieces} (SBX_TRA_CONSULTATION) et {@code PUT …/{code}/retention}
 *   (SBX_TRA_ADMINISTRATION) : catalogue des types et politique de rétention, depuis la console ;</li>
 *   <li>{@code GET /api/v1/interne/documents/empreintes/{sha256}} (jeton de service, INTERNAL_DOCUMENT_READ) : pièce
 *   déjà conservée de cette empreinte (MS-HOMOLOGATION) ; 422 empreinte mal formée, 404 aucune ;</li>
 *   <li>{@code GET /api/v1/sandbox/quarantaines} (SBX_TRA_CONSULTATION) : supervision des pièces en quarantaine.</li>
 * </ul>
 */
@RestController
public class PiecesSandboxResource {

    /** Type de pièce vu par la console. */
    public record TypeVue(
        String code,
        Map<String, String> libelles,
        String retention,
        boolean probante,
        List<String> servicesAutorises,
        String reglePan,
        long tailleMax,
        boolean actif,
        int version
    ) {
        static TypeVue de(TypePieceSandbox t) {
            return new TypeVue(t.code(), t.libelles(), t.retention().toString(), t.probante(), t.servicesAutorises().stream().sorted().toList(), t.reglePan().name(), t.tailleMax(), t.actif(), t.version());
        }
    }

    /** Modification de la rétention : période ISO 8601, motif, version lue. */
    public record Retention(String retention, String motif, int version) {}

    public record PageVue(List<PieceSandbox> contenu, long total, int page, int taille) {}

    private final DepotPieceSandboxService depot;
    private final TypesPiecesSandboxService types;
    private final RechercheParEmpreinteService empreintes;

    public PiecesSandboxResource(DepotPieceSandboxService depot, TypesPiecesSandboxService types, RechercheParEmpreinteService empreintes) {
        this.depot = depot;
        this.types = types;
        this.empreintes = empreintes;
    }

    @GetMapping("/api/v1/interne/documents/empreintes/{sha256}")
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_READ')")
    public RechercheParEmpreinteService.Piece parEmpreinte(@PathVariable String sha256) {
        return empreintes.parEmpreinte(sha256);
    }

    @PostMapping(value = "/api/v1/interne/pieces-sandbox", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_DEPOSIT')")
    public ResponseEntity<Resultat> deposer(
        @RequestPart(name = "fichier", required = false) MultipartFile fichier,
        @RequestParam(name = "type", required = false) String type,
        @RequestParam(name = "empreinte", required = false) String empreinte,
        @RequestParam(name = "codeParticipant", required = false) String codeParticipant,
        @RequestParam(name = "reference", required = false) String reference,
        Authentication auth
    ) {
        String service = DocumentInterneResource.service(auth);
        Resultat r = depot.deposer(new Demande(fichier == null ? null : DocumentDepotResource.lire(fichier), type, empreinte, codeParticipant, reference), service);
        HttpStatus statut = r.etat() == PieceSandbox.Etat.QUARANTAINE ? HttpStatus.ACCEPTED : (r.dejaConservee() ? HttpStatus.OK : HttpStatus.CREATED);
        return ResponseEntity.status(statut).body(r);
    }

    @GetMapping("/api/v1/sandbox/types-pieces")
    @PreAuthorize("hasAuthority('SBX_TRA_CONSULTATION')")
    public List<TypeVue> types() {
        return types.types().stream().map(TypeVue::de).toList();
    }

    @PutMapping("/api/v1/sandbox/types-pieces/{code}/retention")
    @PreAuthorize("hasAuthority('SBX_TRA_ADMINISTRATION')")
    public TypeVue modifierRetention(@PathVariable String code, @RequestBody Retention corps, Authentication auth) {
        return TypeVue.de(types.modifierRetention(code, corps.retention(), corps.motif(), corps.version(), auth.getName()));
    }

    @GetMapping("/api/v1/sandbox/quarantaines")
    @PreAuthorize("hasAuthority('SBX_TRA_CONSULTATION')")
    public PageVue quarantaines(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        PageQuarantaines p = types.quarantaines(page, taille);
        return new PageVue(p.contenu(), p.total(), p.page(), p.taille());
    }
}
