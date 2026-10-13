package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase;
import cm.gimac.des.participant.application.usecase.InstitutionRegistryQueries;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.web.rest.lot1.InstitutionRequests.CandidatureRequest;
import cm.gimac.des.participant.web.rest.lot1.InstitutionRequests.ConditionsRequest;
import cm.gimac.des.participant.web.rest.lot1.InstitutionRequests.DepotRequest;
import cm.gimac.des.participant.web.rest.lot1.InstitutionRequests.MotifRequest;
import cm.gimac.des.participant.web.rest.lot1.InstitutionRequests.TransitionRequest;
import cm.gimac.des.participant.web.rest.lot1.InstitutionViews.DossierVue;
import cm.gimac.des.participant.web.rest.lot1.InstitutionViews.InstitutionVue;
import cm.gimac.des.participant.web.rest.lot1.InstitutionViews.PageVue;
import cm.gimac.des.participant.web.rest.lot1.InstitutionViews.PieceVue;
import cm.gimac.des.participant.web.rest.lot1.InstitutionViews.PrerequisVue;
import cm.gimac.des.participant.web.rest.lot1.InstitutionViews.TransitionVue;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registre des institutions, étapes 1 et 2 du parcours (prise de contact, adhésion : D-57) et
 * cycle de vie en maker-checker (PAR-01 à PAR-04). Exposé sous /bff/des/participant/institutions.
 */
@RestController
@RequestMapping("/api/v1/institutions")
public class RegistreInstitutionsResource {

    static final String REAUTH_HEADER = "X-Reauth-Assertion";

    private final ManageInstitutionUseCase useCase;
    private final InstitutionRegistryQueries queries;

    public RegistreInstitutionsResource(ManageInstitutionUseCase useCase, InstitutionRegistryQueries queries) {
        this.useCase = useCase;
        this.queries = queries;
    }

    // ------------------------------------------------------------------ lectures

    @GetMapping
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_READ')")
    @Transactional(readOnly = true)
    public PageVue<InstitutionVue> lister(
        @RequestParam(name = "statut", required = false) Set<InstitutionStatus> statuts,
        @RequestParam(name = "q", required = false) String recherche,
        @RequestParam(name = "mesDossiers", defaultValue = "false") boolean mesDossiers,
        @RequestParam(name = "groupe", required = false) String groupe,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille,
        Authentication auth
    ) {
        UUID charge = mesDossiers ? ActeurCourant.de(auth).id() : null;
        Page<Institution> p = queries.lister(new InstitutionRegistryQueries.Filtre(statuts, recherche, charge, groupe), page, taille);
        return new PageVue<>(p.getContent().stream().map(InstitutionVue::de).toList(), p.getTotalElements());
    }

    /** Groupes ou maisons mères déclarés (D-104) : filtre de la liste, suggestions de la fiche. */
    @GetMapping("/groupes")
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_READ')")
    public List<String> groupes() {
        return queries.groupes();
    }

    @GetMapping("/indicateurs")
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_READ')")
    public InstitutionRegistryQueries.Indicateurs indicateurs() {
        return queries.indicateurs();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_READ')")
    @Transactional(readOnly = true)
    public DossierVue dossier(@PathVariable UUID id) {
        return DossierVue.de(queries.dossier(id));
    }

    @GetMapping("/{id}/prerequis")
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_READ')")
    public List<PrerequisVue> prerequis(@PathVariable UUID id, @RequestParam("etat") InstitutionStatus etat) {
        return useCase.prerequis(id, etat).stream().map(PrerequisVue::de).toList();
    }

    // ------------------------------------------------------------------ étape 1 : prise de contact

    @PostMapping
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_CREATE')")
    public ResponseEntity<InstitutionVue> enregistrer(@Valid @RequestBody CandidatureRequest req, Authentication auth) {
        Institution i = useCase.enregistrerCandidat(req.commande(), ActeurCourant.de(auth));
        return ResponseEntity.created(URI.create("/api/v1/institutions/" + i.getId())).body(InstitutionVue.de(i));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_EDIT')")
    public InstitutionVue modifier(@PathVariable UUID id, @Valid @RequestBody CandidatureRequest req, Authentication auth) {
        return InstitutionVue.de(useCase.modifierFiche(id, req.commande(), ActeurCourant.de(auth)));
    }

    // ------------------------------------------------------------------ étape 2 : adhésion

    @PutMapping("/{id}/conditions-adhesion")
    @PreAuthorize("hasAuthority('PAR_INSTITUTION_EDIT')")
    public InstitutionVue conditions(@PathVariable UUID id, @Valid @RequestBody ConditionsRequest req, Authentication auth) {
        return InstitutionVue.de(useCase.definirConditionsAdhesion(id, req.commande(), ActeurCourant.de(auth)));
    }

    @PostMapping("/{id}/pieces/{code}/depot")
    @PreAuthorize("hasAuthority('PAR_ADHESION_INSTRUCT')")
    public PieceVue deposer(@PathVariable UUID id, @PathVariable String code, @Valid @RequestBody DepotRequest req, Authentication auth) {
        return PieceVue.de(useCase.deposerPiece(id, code, req.commande(), ActeurCourant.de(auth)));
    }

    @PostMapping("/{id}/pieces/{code}/validation")
    @PreAuthorize("hasAuthority('PAR_ADHESION_INSTRUCT')")
    public PieceVue valider(@PathVariable UUID id, @PathVariable String code, Authentication auth) {
        return PieceVue.de(useCase.validerPiece(id, code, ActeurCourant.de(auth)));
    }

    @PostMapping("/{id}/pieces/{code}/rejet")
    @PreAuthorize("hasAuthority('PAR_ADHESION_INSTRUCT')")
    public PieceVue rejeter(@PathVariable UUID id, @PathVariable String code, @Valid @RequestBody MotifRequest req, Authentication auth) {
        return PieceVue.de(useCase.rejeterPiece(id, code, req.motif(), ActeurCourant.de(auth)));
    }

    // ------------------------------------------------------------------ transitions (6.25)

    @PostMapping("/{id}/transitions")
    @PreAuthorize("hasAuthority('PAR_TRANSITION_REQUEST')")
    public ResponseEntity<TransitionVue> proposer(@PathVariable UUID id, @Valid @RequestBody TransitionRequest req, Authentication auth) {
        var t = useCase.proposerTransition(id, req.vers(), req.motif(), req.pieceDecisionId(), ActeurCourant.de(auth));
        return ResponseEntity.created(URI.create("/api/v1/institutions/" + id + "/transitions/" + t.getId())).body(TransitionVue.de(t));
    }

    @PostMapping("/{id}/transitions/{transitionId}/approbation")
    @PreAuthorize("hasAuthority('PAR_TRANSITION_APPROVE')")
    public TransitionVue approuver(
        @PathVariable UUID id,
        @PathVariable UUID transitionId,
        @RequestHeader(name = REAUTH_HEADER, required = false) String assertion,
        Authentication auth
    ) {
        return TransitionVue.de(useCase.approuverTransition(id, transitionId, ActeurCourant.de(auth), assertion));
    }

    @PostMapping("/{id}/transitions/{transitionId}/rejet")
    @PreAuthorize("hasAuthority('PAR_TRANSITION_APPROVE')")
    public TransitionVue rejeterTransition(
        @PathVariable UUID id,
        @PathVariable UUID transitionId,
        @Valid @RequestBody MotifRequest req,
        Authentication auth
    ) {
        return TransitionVue.de(useCase.rejeterTransition(id, transitionId, req.motif(), ActeurCourant.de(auth)));
    }
}
