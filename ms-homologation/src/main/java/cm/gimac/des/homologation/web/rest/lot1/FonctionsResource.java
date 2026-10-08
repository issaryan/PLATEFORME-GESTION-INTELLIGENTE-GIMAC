package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.FonctionsService;
import cm.gimac.des.homologation.domain.DefinitionFonction;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogue des fonctions (D-91, docs/lot1/types-projet.md §2), exposé sous
 * /bff/des/homologation/referentiels/fonctions : liste ({@code actif=}), lecture, création, modification motivée.
 * Lecture : HOM_REFERENTIEL_READ ; écriture : HOM_REFERENTIEL_EDIT (ou REF_METIER_ADMIN, D-92). Aucune suppression.
 */
@RestController
@RequestMapping("/api/v1/referentiels/fonctions")
public class FonctionsResource {

    /** Fonction du catalogue ; {@code reprise} : une des seize fonctions de l'ancienne énumération. */
    public record FonctionVue(
        String code,
        String libelle,
        List<String> profils,
        boolean externe,
        boolean actif,
        List<String> permissions,
        boolean reprise,
        Instant creeLe,
        UUID creePar,
        Instant modifieLe,
        UUID modifiePar
    ) {
        static FonctionVue de(DefinitionFonction f) {
            return new FonctionVue(
                f.getCode(),
                f.getLibelle(),
                f.getProfils(),
                f.isExterne(),
                f.isActif(),
                f.getPermissions(),
                Fonction.valueOf(f.getCode()).reprise(),
                f.getCreeLe(),
                f.getCreePar(),
                f.getModifieLe(),
                f.getModifiePar()
            );
        }
    }

    public record CreationRequest(
        @Size(max = 40) String code,
        @Size(max = 120) String libelle,
        @Size(max = 20) List<String> profils,
        Boolean externe,
        @Size(max = 20) List<String> permissions
    ) {}

    public record ModificationRequest(
        @Size(max = 120) String libelle,
        @Size(max = 20) List<String> profils,
        Boolean externe,
        Boolean actif,
        @Size(max = 500) String motif,
        @Size(max = 20) List<String> permissions
    ) {}

    private final FonctionsService service;

    public FonctionsResource(FonctionsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    public List<FonctionVue> lister(@RequestParam(name = "actif", required = false) Boolean actif) {
        return service.lister(actif).stream().map(FonctionVue::de).toList();
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    public FonctionVue lire(@PathVariable String code) {
        return FonctionVue.de(service.lire(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @Transactional
    public ResponseEntity<FonctionVue> creer(@Valid @RequestBody CreationRequest r, Authentication auth) {
        DefinitionFonction f = service.creer(new FonctionsService.Creation(r.code(), r.libelle(), r.profils(), r.externe(), r.permissions()), ActeurCourant.de(auth).id());
        return ResponseEntity.created(URI.create("/api/v1/referentiels/fonctions/" + f.getCode())).body(FonctionVue.de(f));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @Transactional
    public FonctionVue modifier(@PathVariable String code, @Valid @RequestBody ModificationRequest r, Authentication auth) {
        return FonctionVue.de(service.modifier(code, new FonctionsService.Modification(r.libelle(), r.profils(), r.externe(), r.actif(), r.motif(), r.permissions()), ActeurCourant.de(auth).id()));
    }
}
