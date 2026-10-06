package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.GererEtapesReferentielUseCase;
import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.usecase.ReferentialLifecycleService;
import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.parcours.CalendrierDag;
import cm.gimac.des.homologation.domain.parcours.CouleurEtape;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.NatureLivrable;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.VisibiliteReferentiel;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.ArcRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.CatalogueVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.CouleurVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.EtapeCanoniqueVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.EtapeRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.EtapeVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.OrdreEtapesRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.Entree;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.GrapheVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.OperationVue;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.Pilote;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.PiloteRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.PositionRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.VersionVue;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md §4), exposé par la passerelle sous
 * /bff/des/homologation/referentiels : création et retrait d'opérations, positions, arcs, graphe étendu (chemin
 * critique, échéances au plus tôt, anomalies par nœud), catalogue et pilote du brouillon ; étapes configurables
 * et lecture filtrée des personnes concernées (D-89, docs/lot1/referentiel-droits-etapes.md). Toute écriture sur
 * une version qui n'est pas un brouillon est refusée par 409 HOM_REFERENTIAL_FROZEN.
 */
@RestController
@RequestMapping("/api/v1/referentiels")
public class ModelisateurResource {

    static final List<String> TYPES_PIECES = List.of(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/csv",
        "application/zip"
    );

    private final ManageReferentialUseCase useCase;
    private final GererEtapesReferentielUseCase etapesUseCase;
    private final ReferentialVersionRepository versions;
    private final ReferentialGraphValidator validator;
    private final ReferentialCompletenessChecker completude;
    private final CollaboratorDirectoryPort annuaire;
    private final List<String> profilsPilote;

    public ModelisateurResource(
        ManageReferentialUseCase useCase,
        GererEtapesReferentielUseCase etapesUseCase,
        ReferentialVersionRepository versions,
        ReferentialGraphValidator validator,
        ReferentialCompletenessChecker completude,
        CollaboratorDirectoryPort annuaire,
        @Value("${gimac.homologation.referentiel.profils-pilote:" + ReferentialLifecycleService.PROFILS_EDITION + "}") List<String> profilsPilote
    ) {
        this.useCase = useCase;
        this.etapesUseCase = etapesUseCase;
        this.versions = versions;
        this.validator = validator;
        this.completude = completude;
        this.annuaire = annuaire;
        this.profilsPilote = List.copyOf(profilsPilote);
    }

    /** Création d'une opération au code fourni dans le corps. */
    @PostMapping("/{id}/operations")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional
    public ResponseEntity<OperationVue> creerOperation(@PathVariable UUID id, @Valid @RequestBody ReferenceOperationData data) {
        ReferenceOperation op = useCase.creerOperation(id, data.code(), data);
        return ResponseEntity.created(URI.create("/api/v1/referentiels/" + id + "/operations/" + op.getCode())).body(OperationVue.de(op));
    }

    /** Retrait d'une opération ; son code disparaît des prédécesseurs des autres. */
    @DeleteMapping("/{id}/operations/{code}")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerOperation(@PathVariable UUID id, @PathVariable String code) {
        useCase.supprimerOperation(id, code);
    }

    /** Positions des opérations dans le canevas ({@code [{code, x, y}]}). */
    @PutMapping("/{id}/positions")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void placer(@PathVariable UUID id, @Valid @RequestBody List<@Valid PositionRequest> positions) {
        Map<String, Position> m = new LinkedHashMap<>();
        positions.forEach(p -> m.put(p.code(), new Position(p.x(), p.y())));
        useCase.placer(id, m);
    }

    /** Arc {@code de} vers {@code vers} : ajouté ({@code present}) ou retiré ; rend l'opération {@code vers}. */
    @PutMapping("/{id}/arcs")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional
    public OperationVue arc(@PathVariable UUID id, @Valid @RequestBody ArcRequest r) {
        return OperationVue.de(useCase.arc(id, r.de(), r.vers(), r.present()));
    }

    /** Pilote du brouillon : compte portant HOM_REFERENTIEL_EDIT ; sa tâche suit (MS-PILOTAGE). */
    @PutMapping("/{id}/pilote")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional
    public VersionVue pilote(@PathVariable UUID id, @Valid @RequestBody PiloteRequest r, Authentication auth) {
        ReferentialVersion v = useCase.designerPilote(id, r.compteId(), ActeurCourant.de(auth).id());
        return VersionVue.de(v, versions.findFirstByTypeProjetAndEtat(v.getTypeProjet(), EtatVersionReferentiel.PUBLIEE).map(ReferentialVersion::getId).orElse(null));
    }

    /**
     * Graphe étendu : nœuds complets, positions, couches, échéances au plus tôt, chemin critique, anomalies, étapes
     * de la version. Lecteur concerné (D-89) : ses opérations, leurs voisines directes en fantômes, les arcs qui
     * touchent une opération visible ; anomalies, attributs manquants et chemin critique réduits à ses opérations.
     */
    @GetMapping("/{id}/graphe")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public GrapheVue graphe(@PathVariable UUID id, Authentication auth) {
        ReferentialVersion v = versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
        VisibiliteReferentiel.Vue vue = VisibiliteReferentiel.pour(ActeurCourant.de(auth).permissions(), v.getOperations());
        List<ReferenceOperation> ops = v
            .getOperations()
            .stream()
            .sorted(Comparator.comparingInt(ReferenceOperation::ordreEffectif).thenComparing(ReferenceOperation::getCode))
            .toList();
        ReferentialGraphValidator.Rapport rapport = validator.controler(v.noeuds());
        Map<String, CalendrierDag.Tache> g = new LinkedHashMap<>();
        Map<String, List<String>> preds = new LinkedHashMap<>();
        ops.forEach(o -> {
            g.put(o.getCode(), new CalendrierDag.Tache(o.predecesseursListe(), o.delaiEffectif()));
            preds.put(o.getCode(), o.predecesseursListe());
        });
        CalendrierDag.Resultat cal = CalendrierDag.calculer(g);
        Map<String, List<String>> succ = CalendrierDag.successeurs(preds);
        List<String> chemin = cal.cheminCritique();
        Set<String> critiques = new HashSet<>(chemin);
        Set<String> arcsCritiques = new HashSet<>();
        for (int i = 1; i < chemin.size(); i++) {
            arcsCritiques.add(chemin.get(i - 1) + ">" + chemin.get(i));
        }
        List<GrapheVue.Noeud> noeuds = ops
            .stream()
            .filter(o -> vue.retenue(o.getCode()))
            .map(o ->
                vue.fantome(o.getCode())
                    ? new GrapheVue.Noeud(OperationVue.fantome(o), null, null, null, false, List.of())
                    : new GrapheVue.Noeud(
                    OperationVue.de(o, succ.getOrDefault(o.getCode(), List.of())),
                    rapport.couches().get(o.getCode()),
                    cal.debuts().get(o.getCode()),
                    cal.fins().get(o.getCode()),
                    critiques.contains(o.getCode()),
                    rapport.anomalies().stream().filter(a -> a.codes().contains(o.getCode())).map(a -> a.type().name()).distinct().toList()
                )
            )
            .toList();
        List<GrapheVue.Arc> arcs = ops
            .stream()
            .flatMap(o ->
                o
                    .predecesseursListe()
                    .stream()
                    .filter(p -> vue.visible(p) ? vue.retenue(o.getCode()) : vue.fantome(p) && vue.visible(o.getCode()))
                    .map(p -> new GrapheVue.Arc(p, o.getCode(), arcsCritiques.contains(p + ">" + o.getCode())))
            )
            .toList();
        return new GrapheVue(
            v.getId(),
            v.getTypeProjet(),
            v.getNumero(),
            v.getEtat(),
            v.getPiloteId(),
            noeuds,
            arcs,
            rapport.anomalies().stream().filter(a -> vue.complete() || (!a.codes().isEmpty() && a.codes().stream().allMatch(vue::visible))).toList(),
            completude.check(v.getOperations()).stream().filter(m -> vue.visible(m.code())).toList(),
            new GrapheVue.CheminCritique(vue.complete() ? chemin : chemin.stream().filter(vue::visible).toList(), cal.dureeTotale()),
            v.actionsPermises(),
            EtapeVue.de(v),
            vue.filtre()
        );
    }

    /**
     * Catalogue du modélisateur : listes fermées et pilotes possibles (annuaire injoignable : liste vide). Avec
     * {@code version} (D-89), {@code etapes} liste les étapes de cette version (rang : leur ordre) et
     * {@code etapesVersion} leur détail ; sans elle, les onze étapes canoniques (rang : celui du CR).
     */
    @GetMapping("/catalogue")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public CatalogueVue catalogue(@RequestParam(name = "version", required = false) UUID versionId) {
        ReferentialVersion version = versionId == null
            ? null
            : versions.findById(versionId).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", versionId));
        return new CatalogueVue(
            Arrays.stream(Fonction.values()).map(f -> new Entree(f.name(), f.libelle(), f.profils(), f.externe(), null, null)).toList(),
            Arrays.stream(Geste.values()).map(x -> new Entree(x.name(), x.libelle(), null, null, x.dedie(), null)).toList(),
            Arrays.stream(Jalon.values()).map(x -> new Entree(x.name(), x.libelle(), null, null, null, x.ordinal() + 1)).toList(),
            Declencheurs.CONNUS.entrySet().stream().map(e -> Entree.de(e.getKey(), e.getValue())).toList(),
            version == null
                ? Arrays.stream(EtapeCode.values()).map(e -> new Entree(e.name(), e.libelle(), null, null, null, e.rang())).toList()
                : version.etapes().stream().map(e -> new Entree(e.getCode(), e.getLibelle(), null, null, null, e.getOrdre())).toList(),
            List.of(
                Entree.de(SegmentParcours.AMONT.name(), "Tronc commun amont"),
                Entree.de(SegmentParcours.BRANCHE.name(), "Branche du type de projet"),
                Entree.de(SegmentParcours.AVAL.name(), "Tronc commun aval")
            ),
            Arrays.stream(PlateformeExecution.values()).map(x -> Entree.de(x.name(), x.libelle())).toList(),
            List.of(Entree.de(GrainPreuve.PHASE.name(), "Pièce de phase"), Entree.de(GrainPreuve.OPERATION.name(), "Preuve par opération")),
            List.of(Entree.de(NatureLivrable.LIVRABLE.name(), "Livrable"), Entree.de(NatureLivrable.PREUVE.name(), "Preuve")),
            Arrays.stream(Particularite.values()).map(x -> Entree.de(x.name(), x.libelle())).toList(),
            TYPES_PIECES,
            pilotes(),
            Arrays.stream(EtapeCode.values()).map(e -> new EtapeCanoniqueVue(e.name(), e.libelle(), e.rang(), e.protegee())).toList(),
            Arrays.stream(CouleurEtape.values()).map(c -> new CouleurVue(c.name(), c.libelle(), c.jeton())).toList(),
            version == null ? null : EtapeVue.de(version)
        );
    }

    // ------------------------------------------------------------------ étapes configurables (D-89, §2)

    /** Étapes de la version, dans leur ordre. */
    @GetMapping("/{id}/etapes")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public List<EtapeVue> etapes(@PathVariable UUID id) {
        return EtapeVue.de(versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id)));
    }

    /** Ajout d'une étape au brouillon (code exigé ; à la fin par défaut). */
    @PostMapping("/{id}/etapes")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional
    public ResponseEntity<EtapeVue> ajouterEtape(@PathVariable UUID id, @Valid @RequestBody EtapeRequest r) {
        EtapeReferentiel e = etapesUseCase.ajouter(id, r.code(), r.libelle(), r.couleur(), r.canonique(), r.ordre());
        return ResponseEntity.created(URI.create("/api/v1/referentiels/" + id + "/etapes/" + e.getCode())).body(EtapeVue.de(e, e.getVersion()));
    }

    /** Nouvel ordre des étapes ({@code {codes: [...]}}) ; rend les étapes dans cet ordre. */
    @PutMapping("/{id}/etapes/ordre")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional
    public List<EtapeVue> ordonnerEtapes(@PathVariable UUID id, @Valid @RequestBody OrdreEtapesRequest r) {
        return EtapeVue.de(etapesUseCase.ordonner(id, r.codes()));
    }

    /** Renommage, couleur et étape canonique ({@code code} et {@code ordre} du corps ignorés). */
    @PutMapping("/{id}/etapes/{code}")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @Transactional
    public EtapeVue modifierEtape(@PathVariable UUID id, @PathVariable String code, @Valid @RequestBody EtapeRequest r) {
        EtapeReferentiel e = etapesUseCase.modifier(id, code, r.libelle(), r.couleur(), r.canonique());
        return EtapeVue.de(e, e.getVersion());
    }

    /** Retrait d'une étape vide et non protégée. */
    @DeleteMapping("/{id}/etapes/{code}")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerEtape(@PathVariable UUID id, @PathVariable String code) {
        etapesUseCase.supprimer(id, code);
    }

    private List<Pilote> pilotes() {
        try {
            return profilsPilote
                .stream()
                .flatMap(p -> annuaire.parProfil(p).stream())
                .filter(CollaboratorView::actif)
                .map(c -> new Pilote(c.id().toString(), c.nomAffiche()))
                .distinct()
                .sorted(Comparator.comparing(Pilote::nom, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        } catch (HomologationException e) {
            return List.of();
        }
    }
}
