package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.PerimetreLiaison;
import cm.gimac.des.homologation.domain.PerimetreModification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Périmètre du projet de certification (D-133), servi sous /bff/des/homologation/liaisons/{id}/perimetre : lecture
 * et historique pour tout lecteur des liaisons ; modification par le gestionnaire du projet (HOM_PERIMETRE_EDIT), un
 * super administrateur ou un chargé de l'implémentation des projets de la SNA (HOM_PERIMETRE_EDIT_TOUS). La règle du
 * gestionnaire est tenue par le cas d'utilisation, au delà de la permission.
 */
@RestController
@RequestMapping("/api/v1/liaisons/{id}/perimetre")
public class PerimetreLiaisonResource {

    static final int TAILLE_MAX = 100;

    public record ModificationRequest(@NotNull @Size(min = 1, max = 60) List<@NotNull @Size(min = 1, max = 40) String> services, @Size(max = 500) String motif) {}

    /** Ligne de l'historique : services séparés en listes. */
    public record ModificationVue(int version, UUID auteurId, String auteurNom, String permissionUtilisee, List<String> avant, List<String> apres, String motif, String modifieLe) {
        static ModificationVue de(PerimetreModification m) {
            return new ModificationVue(m.getVersionPerimetre(), m.getAuteurId(), m.getAuteurNom(), m.getPermissionUtilisee(), codes(m.getAvant()), codes(m.getApres()), m.getMotif(), m.getModifieLe().toString());
        }

        private static List<String> codes(String s) {
            return s == null || s.isBlank() ? List.of() : Arrays.asList(s.split(","));
        }
    }

    private final PerimetreLiaison perimetre;

    public PerimetreLiaisonResource(PerimetreLiaison perimetre) {
        this.perimetre = perimetre;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public PerimetreLiaison.Vue lire(@PathVariable UUID id, Authentication auth) {
        return perimetre.lire(id, ActeurCourant.de(auth));
    }

    @GetMapping("/historique")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public LiaisonsCadrageResource.PageVue<ModificationVue> historique(
        @PathVariable UUID id,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "25") int taille
    ) {
        var p = perimetre.historique(id, PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), TAILLE_MAX)));
        return new LiaisonsCadrageResource.PageVue<>(p.getContent().stream().map(ModificationVue::de).toList(), p.getTotalElements());
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('HOM_PERIMETRE_EDIT', 'HOM_PERIMETRE_EDIT_TOUS')")
    public PerimetreLiaison.Vue modifier(@PathVariable UUID id, @Valid @RequestBody ModificationRequest r, Authentication auth) {
        return perimetre.modifier(id, r.services(), r.motif(), ActeurCourant.de(auth));
    }
}
