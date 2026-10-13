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
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.VisibiliteReferentiel;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.ControleVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.CreateDraftRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.EvenementVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.MigrationRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.MigrationVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.OperationVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.VersionVue;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
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
 * /bff/des/homologation/referentiels. Le graphe étendu, le catalogue et les gestes du modélisateur (D-77) sont
 * servis par {@link ModelisateurResource}.
 */
@RestController
@RequestMapping("/api/v1/referentiels")
public class ReferentialResource {

    private final ManageReferentialUseCase useCase;
    private final ReferentialVersionRepository versions;
    private final ReferentialGraphValidator validator;
    private cm.gimac.des.homologation.application.usecase.AttestationsTypeProjet attestations;

    /** Attestations exigées par le modèle du type (contrôle à blanc des gestes de certification, §10.5). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAttestations(cm.gimac.des.homologation.application.usecase.AttestationsTypeProjet attestations) {
        this.attestations = attestations;
    }
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
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    public ResponseEntity<VersionVue> creer(@Valid @RequestBody CreateDraftRequest req, Authentication auth) {
        ReferentialVersion v = useCase.creerBrouillon(req.typeProjet(), req.libelle(), req.versionSourceId(), ActeurCourant.de(auth).id(), req.piloteId());
        return ResponseEntity.created(URI.create("/api/v1/referentiels/" + v.getId())).body(VersionVue.de(v, active(v.getTypeProjet()).orElse(null)));
    }

    /** Suppression d'un brouillon (D-92) ; une version publiée ou retirée se retire, elle ne se supprime pas (409). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        useCase.supprimerBrouillon(id);
    }

    @PutMapping("/{id}/operations/{code}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @Transactional
    public OperationVue enregistrer(@PathVariable UUID id, @PathVariable String code, @Valid @RequestBody ReferenceOperationData data) {
        return OperationVue.de(useCase.enregistrerOperation(id, code, data));
    }

    @PostMapping("/{id}/publication")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_PUBLISH', 'REF_METIER_ADMIN')")
    public VersionVue publier(@PathVariable UUID id) {
        return VersionVue.de(useCase.publier(id));
    }

    /** Versions d'un type, ou de tous les types (arbre de DES-12), les plus récentes d'abord. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public List<VersionVue> lister(@RequestParam(name = "type", required = false) TypeProjet type, Authentication auth) {
        Map<TypeProjet, java.util.Optional<UUID>> actives = new java.util.HashMap<>();
        var permissions = ActeurCourant.de(auth).permissions();
        List<ReferentialVersion> liste = type == null
            ? versions.findAllByOrderByDateCreationDesc().stream().sorted(Comparator.comparing(ReferentialVersion::getTypeProjet)).toList()
            : versions.findByTypeProjetOrderByDateCreationDesc(type);
        return liste
            .stream()
            .map(v -> VersionVue.de(v, actives.computeIfAbsent(v.getTypeProjet(), this::active).orElse(null), VisibiliteReferentiel.pour(permissions, v.getOperations())))
            .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public VersionVue lire(@PathVariable UUID id, Authentication auth) {
        ReferentialVersion v = charger(id);
        return VersionVue.de(v, active(v.getTypeProjet()).orElse(null), VisibiliteReferentiel.pour(ActeurCourant.de(auth).permissions(), v.getOperations()));
    }

    /** Chronologie d'une version : création, publication, puis événements publiés (outbox), du plus récent au plus ancien. */
    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_MIGRATE', 'REF_METIER_ADMIN')")
    public List<ReferentialMigrationService.Impact> simuler(@PathVariable UUID id) {
        return migrations.simuler(id);
    }

    /** HOM-03 : rattachement des liaisons retenues à cette version. */
    @PostMapping("/{id}/migrations")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_MIGRATE', 'REF_METIER_ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MigrationVue migrer(@PathVariable UUID id, @Valid @RequestBody MigrationRequest req, Authentication auth) {
        ReferentialMigration m = migrations.migrer(id, req.liaisonsRetenues(), ActeurCourant.de(auth).id());
        return new MigrationVue(m.getId(), m.getVersionSourceId(), m.getVersionCibleId(), m.getEtat().name(), JsonTexte.listeDeTextes(m.getLiaisonsRetenues()));
    }

    /**
     * Opérations de la version. Lecteur concerné (D-89) : ses opérations et leurs voisines directes en fantômes
     * ({@code fantome: true}, code et libellé) ; l'en-tête {@value #EN_TETE_FILTRE} vaut CONCERNE ou COMPLET.
     */
    @GetMapping("/{id}/operations")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OperationVue>> operations(@PathVariable UUID id, Authentication auth) {
        ReferentialVersion v = charger(id);
        VisibiliteReferentiel.Vue vue = VisibiliteReferentiel.pour(ActeurCourant.de(auth).permissions(), v.getOperations());
        return ResponseEntity.ok().header(EN_TETE_FILTRE, vue.filtre().name()).body(operationsRetenues(v, vue));
    }

    /**
     * Export CSV (séparateur point-virgule, UTF-8) des opérations de la version, filtré comme la liste (D-89) ; la
     * dernière colonne marque les fantômes.
     */
    @GetMapping(value = "/{id}/export", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<String> exporter(@PathVariable UUID id, Authentication auth) {
        ReferentialVersion v = charger(id);
        VisibiliteReferentiel.Vue vue = VisibiliteReferentiel.pour(ActeurCourant.de(auth).permissions(), v.getOperations());
        StringBuilder csv = new StringBuilder("code;libelle;etape;segment;responsable;validateur;consultes;informes;externes;predecesseurs;delaiJours;fantome;signatures;delaiMaxJours\n");
        for (OperationVue o : operationsRetenues(v, vue)) {
            csv
                .append(String.join(
                    ";",
                    cellule(o.code()),
                    cellule(o.libelle()),
                    cellule(o.etape()),
                    cellule(o.segment()),
                    cellule(o.responsable()),
                    cellule(o.validateur()),
                    cellule(o.consultes()),
                    cellule(o.informes()),
                    cellule(o.externes()),
                    cellule(o.predecesseurs()),
                    cellule(o.delaiJours()),
                    String.valueOf(o.fantome()),
                    cellule(o.signatures() == null ? null : o.signatures().stream().map(x -> x.code() + ":" + x.signataire() + ":" + x.ordreEffectif() + ":" + String.join(",", x.porte())).toList()),
                    cellule(o.delaiMaxJours())
                ))
                .append('\n');
        }
        String nom = "referentiel-" + v.getTypeProjet().name().toLowerCase(java.util.Locale.ROOT) + "-" + (v.getNumero() == null ? "brouillon" : "v" + v.getNumero()) + ".csv";
        return ResponseEntity.ok()
            .header(EN_TETE_FILTRE, vue.filtre().name())
            .header("Content-Disposition", "attachment; filename=\"" + nom + "\"")
            .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
            .body(csv.toString());
    }

    /** En-tête de la lecture filtrée (D-89) : CONCERNE ou COMPLET. */
    public static final String EN_TETE_FILTRE = "X-Gimac-Filtre";

    private static List<OperationVue> operationsRetenues(ReferentialVersion v, VisibiliteReferentiel.Vue vue) {
        return v
            .getOperations()
            .stream()
            .filter(o -> vue.retenue(o.getCode()))
            .map(o -> vue.fantome(o.getCode()) ? OperationVue.fantome(o) : OperationVue.de(o))
            .toList();
    }

    private static String cellule(Object valeur) {
        if (valeur == null) {
            return "";
        }
        String t = valeur instanceof java.util.Collection<?> c ? String.join("|", c.stream().map(String::valueOf).toList()) : String.valueOf(valeur);
        // Formules neutralisées (injection CSV) et guillemets doublés.
        if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0) {
            t = "'" + t;
        }
        return t.contains(";") || t.contains("\"") || t.contains("\n") ? "\"" + t.replace("\"", "\"\"") + "\"" : t;
    }

    /** HOM-02 : contrôle à blanc, sans effet, y compris sur une version publiée. */
    @PostMapping("/{id}/controle")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public ControleVue controler(@PathVariable UUID id) {
        ReferentialVersion v = charger(id);
        return ControleVue.de(validator.controler(v.noeuds(), attestations == null || attestations.exigees(v.getTypeProjet())), completude.check(v.getOperations()));
    }

    /** Version publiée en vigueur du type. */
    private java.util.Optional<UUID> active(TypeProjet t) {
        return versions.findFirstByTypeProjetAndEtat(t, EtatVersionReferentiel.PUBLIEE).map(ReferentialVersion::getId);
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
