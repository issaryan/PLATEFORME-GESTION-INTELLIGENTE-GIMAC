package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.DemandesService;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.demande.ContenuDemande;
import cm.gimac.des.iam.domain.demande.Demande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demandes des chefs à l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §4), exposées sous
 * /bff/{des,console}/iam/demandes. La matrice des pouvoirs et l'état de la demande sont contrôlés par le
 * service ; l'acceptation exige en plus la réauthentification FIDO2 (finalité ADMIN_SENSITIVE).
 */
@RestController
@RequestMapping("/api/v1/demandes")
public class DemandesResource {

    /** Nouvelle demande : type, compte visé (absent pour une création), contenu selon le type, motif de 5 à 500 caractères. */
    public record DemandeRequest(@NotNull TypeDemande type, @Size(max = 36) String cibleId, @NotNull ContenuDemande contenu, @NotNull @Size(max = 500) String motif) {}

    /** Motif d'une décision : obligatoire pour un refus, facultatif pour une acceptation. */
    public record DecisionRequest(@Size(max = 500) String motif) {}

    private final DemandesService service;
    private final ReauthAdministration reauth;

    public DemandesResource(DemandesService service, ReauthAdministration reauth) {
        this.service = service;
        this.reauth = reauth;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public ResponseEntity<Demande> soumettre(@Valid @RequestBody DemandeRequest r, Authentication auth) {
        Demande d = service.soumettre(ActeurCourant.de(auth), r.type(), r.cibleId(), r.contenu(), r.motif());
        return ResponseEntity.created(URI.create("/api/v1/demandes/" + d.id())).body(d);
    }

    /** Vue « miennes » (PIL_ACTIVITY_OWN) ou « a-traiter » (IAM_ACCOUNT_READ) ; filtre par état. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PIL_ACTIVITY_OWN', 'IAM_ACCOUNT_READ')")
    public DemandesService.PageDemandes lister(
        @RequestParam(name = "vue", required = false) String vue,
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "taille", required = false) Integer taille,
        Authentication auth
    ) {
        return service.lister(ActeurCourant.de(auth), vue, etat, page, taille);
    }

    /** Une demande, pour son demandeur ou l'administration (404 sinon). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PIL_ACTIVITY_OWN', 'IAM_ACCOUNT_READ')")
    public Demande lire(@PathVariable UUID id, Authentication auth) {
        return service.lire(ActeurCourant.de(auth), id);
    }

    @PostMapping("/{id}/annulation")
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public Demande annuler(@PathVariable UUID id, Authentication auth) {
        return service.annuler(ActeurCourant.de(auth), id);
    }

    /** Acceptation : permission de l'acte (vérifiée par le service selon le type) et réauthentification FIDO2. */
    @PostMapping("/{id}/acceptation")
    @PreAuthorize("hasAnyAuthority('IAM_ACCOUNT_CREATE', 'IAM_ACCOUNT_ROLES', 'IAM_ACCOUNT_STATUS')")
    public Demande accepter(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) DecisionRequest r,
        @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return service.accepter(a, id, r == null ? null : r.motif());
    }

    @PostMapping("/{id}/refus")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public Demande refuser(@PathVariable UUID id, @Valid @RequestBody DecisionRequest r, Authentication auth) {
        return service.refuser(ActeurCourant.de(auth), id, r.motif());
    }
}
