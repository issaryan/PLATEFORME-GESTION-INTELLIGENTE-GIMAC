package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.application.usecase.ReferentialMigrationService;
import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ReferentialMigration;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxEvent;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxEventRepository;
import cm.gimac.des.homologation.domain.service.NoeudReferentiel;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.ControleVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.CreateDraftRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.EvenementVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.MigrationRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.MigrationVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.GrapheVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.OperationVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.VersionVue;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    private final ReferentialCompletenessChecker completude;
    private final ReferentialMigrationService migrations;
    private final OutboxEventRepository evenements;

    public ReferentialResource(
        ManageReferentialUseCase useCase,
        ReferentialVersionRepository versions,
        ReferentialGraphValidator validator,
        ReferentialCompletenessChecker completude,
        ReferentialMigrationService migrations,
        OutboxEventRepository evenements
    ) {
        this.useCase = useCase;
        this.versions = versions;
        this.validator = validator;
        this.completude = completude;
        this.migrations = migrations;
        this.evenements = evenements;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    public ResponseEntity<VersionVue> creer(@Valid @RequestBody CreateDraftRequest req) {
        ReferentialVersion v = useCase.creerBrouillon(req.typeProjet(), req.libelle(), req.versionSourceId());
        return ResponseEntity.created(URI.create("/api/v1/referentiels/" + v.getId())).body(vue(v, actives()));
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

    /** Versions d'un type, ou de tous les types (arbre de DES-12), les plus récentes d'abord. */
    @GetMapping
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public List<VersionVue> lister(@RequestParam(name = "type", required = false) TypeProjet type) {
        Map<TypeProjet, UUID> actives = actives();
        List<TypeProjet> types = type == null ? Arrays.asList(TypeProjet.values()) : List.of(type);
        return types.stream().flatMap(t -> versions.findByTypeProjetOrderByDateCreationDesc(t).stream()).map(v -> vue(v, actives)).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public VersionVue lire(@PathVariable UUID id) {
        return vue(charger(id), actives());
    }

    /** Chronologie d'une version : création, publication, puis événements publiés (outbox), du plus récent au plus ancien. */
    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public List<EvenementVue> historique(@PathVariable UUID id) {
        ReferentialVersion v = charger(id);
        List<EvenementVue> h = new ArrayList<>();
        h.add(new EvenementVue("Version créée" + (v.getVersionSourceId() == null ? "" : " par dérivation"), v.getDateCreation(), null, null));
        for (OutboxEvent e : evenements.findByAggregateIdOrderByCreatedAtAsc(id.toString())) {
            h.add(new EvenementVue(libelle(e.getEventType(), v), e.getCreatedAt(), null, e.getCorrelationId()));
        }
        if (v.getDatePublication() != null && h.stream().noneMatch(x -> x.event().startsWith("Version publiée"))) {
            h.add(new EvenementVue("Version publiée sous le numéro " + v.getNumero(), v.getDatePublication(), null, null));
        }
        if (v.getEtat() == EtatVersionReferentiel.RETIREE) {
            h.add(new EvenementVue("Version retirée (remplacée par une version publiée)", null, null, null));
        }
        h.sort(Comparator.comparing(EvenementVue::at, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        return h;
    }

    /** HOM-03 : simulation de la migration des liaisons vers cette version (sans effet). */
    @PostMapping("/{id}/migrations/simulation")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_MIGRATE')")
    public List<ReferentialMigrationService.Impact> simuler(@PathVariable UUID id) {
        return migrations.simuler(id);
    }

    /** HOM-03 : rattachement des liaisons retenues à cette version. */
    @PostMapping("/{id}/migrations")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_MIGRATE')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MigrationVue migrer(@PathVariable UUID id, @Valid @RequestBody MigrationRequest req, Authentication auth) {
        ReferentialMigration m = migrations.migrer(id, req.liaisonsRetenues(), ActeurCourant.de(auth).id());
        return new MigrationVue(m.getId(), m.getVersionSourceId(), m.getVersionCibleId(), m.getEtat().name(), JsonTexte.listeDeTextes(m.getLiaisonsRetenues()));
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
    public ControleVue controler(@PathVariable UUID id) {
        ReferentialVersion v = charger(id);
        return ControleVue.de(validator.controler(v.noeuds()), completude.check(v.getOperations()));
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

    private Map<TypeProjet, UUID> actives() {
        Map<TypeProjet, UUID> m = new EnumMap<>(TypeProjet.class);
        for (TypeProjet t : TypeProjet.values()) {
            versions.findFirstByTypeProjetAndEtat(t, EtatVersionReferentiel.PUBLIEE).ifPresent(v -> m.put(t, v.getId()));
        }
        return m;
    }

    private static VersionVue vue(ReferentialVersion v, Map<TypeProjet, UUID> actives) {
        return VersionVue.de(v, actives.get(v.getTypeProjet()));
    }

    private static String libelle(String type, ReferentialVersion v) {
        return switch (type) {
            case "homologation.catalog-published.v1" -> "Version publiée sous le numéro " + v.getNumero();
            case ReferentialMigrationService.EVENEMENT -> "Liaisons migrées vers cette version";
            default -> type;
        };
    }

    private ReferentialVersion charger(UUID id) {
        return versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
    }
}
