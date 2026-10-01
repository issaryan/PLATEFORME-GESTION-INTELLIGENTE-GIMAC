package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.NoeudReferentiel;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.CreateDraftRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.GrapheVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.OperationVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.VersionVue;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Référentiel d'opérations (HOM-01 A5, HOM-02 A3), exposé par la passerelle sous
 * /bff/des/homologation/referentiels.
 */
@RestController
@RequestMapping("/api/v1/referentiels")
public class ReferentialResource {

    private final ManageReferentialUseCase useCase;
    private final ReferentialVersionRepository versions;
    private final ReferentialGraphValidator validator;

    public ReferentialResource(ManageReferentialUseCase useCase, ReferentialVersionRepository versions, ReferentialGraphValidator validator) {
        this.useCase = useCase;
        this.versions = versions;
        this.validator = validator;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    public ResponseEntity<VersionVue> creer(@Valid @RequestBody CreateDraftRequest req) {
        ReferentialVersion v = useCase.creerBrouillon(req.typeProjet(), req.libelle(), req.versionSourceId());
        return ResponseEntity.created(URI.create("/api/v1/referentiels/" + v.getId())).body(VersionVue.de(v));
    }

    @PutMapping("/{id}/operations/{code}")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    public OperationVue enregistrer(@PathVariable UUID id, @PathVariable String code, @Valid @RequestBody ReferenceOperationData data) {
        return OperationVue.de(useCase.enregistrerOperation(id, code, data));
    }

    @PostMapping("/{id}/publication")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_PUBLISH')")
    public VersionVue publier(@PathVariable UUID id) {
        return VersionVue.de(useCase.publier(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public List<VersionVue> lister(@RequestParam(name = "type", required = false) TypeProjet type) {
        // Sans type : toutes les versions, que l'écran DES-12 regroupe par type de projet.
        List<ReferentialVersion> trouvees = type == null
            ? versions.findAllByOrderByDateCreationDesc()
            : versions.findByTypeProjetOrderByDateCreationDesc(type);
        return trouvees.stream().map(VersionVue::de).toList();
    }

    @GetMapping("/{id}/operations")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public List<OperationVue> operations(@PathVariable UUID id) {
        return charger(id).getOperations().stream().map(OperationVue::de).toList();
    }

    /** HOM-02 : contrôle à blanc, sans effet, y compris sur une version publiée. */
    @PostMapping("/{id}/controle")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional(readOnly = true)
    public ReferentialGraphValidator.Rapport controler(@PathVariable UUID id) {
        return validator.controler(charger(id).noeuds());
    }

    /** HOM-02 : graphe par couches (couche nulle si le graphe est cyclique). */
    @GetMapping("/{id}/graphe")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public GrapheVue graphe(@PathVariable UUID id) {
        List<NoeudReferentiel> noeuds = charger(id).noeuds();
        var rapport = validator.controler(noeuds);
        return new GrapheVue(
            noeuds.stream().map(n -> new GrapheVue.Noeud(n.code(), n.libelle(), n.segment(), rapport.couches().get(n.code()))).toList(),
            noeuds.stream().flatMap(n -> n.predecesseurs().stream().map(p -> new GrapheVue.Arc(p, n.code()))).toList(),
            rapport.anomalies()
        );
    }

    private ReferentialVersion charger(UUID id) {
        return versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
    }
}
