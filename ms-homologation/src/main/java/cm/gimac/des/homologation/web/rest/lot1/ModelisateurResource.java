package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.parcours.CalendrierDag;
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
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.ArcRequest;
import cm.gimac.des.homologation.web.rest.lot1.ReferentialViews.CatalogueVue;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md §4), exposé par la passerelle sous
 * /bff/des/homologation/referentiels : création et retrait d'opérations, positions, arcs, graphe étendu (chemin
 * critique, échéances au plus tôt, anomalies par nœud), catalogue et pilote du brouillon. Toute écriture sur une
 * version qui n'est pas un brouillon est refusée par 409 HOM_REFERENTIAL_FROZEN.
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
    private final ReferentialVersionRepository versions;
    private final ReferentialGraphValidator validator;
    private final ReferentialCompletenessChecker completude;
    private final CollaboratorDirectoryPort annuaire;
    private final List<String> profilsPilote;

    public ModelisateurResource(
        ManageReferentialUseCase useCase,
        ReferentialVersionRepository versions,
        ReferentialGraphValidator validator,
        ReferentialCompletenessChecker completude,
        CollaboratorDirectoryPort annuaire,
        @Value("${gimac.homologation.referentiel.profils-pilote:ROLE_INGENIEUR_HOMOLOGATION}") List<String> profilsPilote
    ) {
        this.useCase = useCase;
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

    /** Graphe étendu : nœuds complets, positions, couches, échéances au plus tôt, chemin critique, anomalies. */
    @GetMapping("/{id}/graphe")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    @Transactional(readOnly = true)
    public GrapheVue graphe(@PathVariable UUID id) {
        ReferentialVersion v = versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
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
            .map(o ->
                new GrapheVue.Noeud(
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
            .flatMap(o -> o.predecesseursListe().stream().map(p -> new GrapheVue.Arc(p, o.getCode(), arcsCritiques.contains(p + ">" + o.getCode()))))
            .toList();
        return new GrapheVue(
            v.getId(),
            v.getTypeProjet(),
            v.getNumero(),
            v.getEtat(),
            v.getPiloteId(),
            noeuds,
            arcs,
            rapport.anomalies(),
            completude.check(v.getOperations()),
            new GrapheVue.CheminCritique(chemin, cal.dureeTotale()),
            v.actionsPermises()
        );
    }

    /** Catalogue du modélisateur : listes fermées et pilotes possibles (annuaire injoignable : liste vide). */
    @GetMapping("/catalogue")
    @PreAuthorize("hasAuthority('HOM_REFERENTIEL_READ')")
    public CatalogueVue catalogue() {
        return new CatalogueVue(
            Arrays.stream(Fonction.values()).map(f -> new Entree(f.name(), f.libelle(), f.profils(), f.externe(), null, null)).toList(),
            Arrays.stream(Geste.values()).map(x -> new Entree(x.name(), x.libelle(), null, null, x.dedie(), null)).toList(),
            Arrays.stream(Jalon.values()).map(x -> new Entree(x.name(), x.libelle(), null, null, null, x.ordinal() + 1)).toList(),
            Declencheurs.CONNUS.entrySet().stream().map(e -> Entree.de(e.getKey(), e.getValue())).toList(),
            Arrays.stream(EtapeCode.values()).map(e -> new Entree(e.name(), e.libelle(), null, null, null, e.rang())).toList(),
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
            pilotes()
        );
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
