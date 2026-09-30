package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase;
import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.Acteur;
import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.OpenLiaisonCommand;
import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liaisons au cadrage (étape 3, HOM-04 adapté par D-57 et D-58), exposées sous
 * /bff/des/homologation/liaisons : liste, fiche, ouverture sur un adhérent, désignation du
 * conducteur de projet par un chef de département.
 */
@RestController
@RequestMapping("/api/v1/liaisons")
public class LiaisonsCadrageResource {

    public record Couverture(Double taux, int numerateur, int denominateur, boolean calculable, String motifNonCalculable) {
        /** Aucune opération instanciée au cadrage : taux non calculable (HOM-17). */
        static final Couverture AUCUNE = new Couverture(null, 0, 0, false, "Aucune opération instanciée");

        static Couverture de(cm.gimac.des.homologation.domain.parcours.Couverture c) {
            return new Couverture(c.taux() == null ? null : c.taux().doubleValue(), c.numerateur(), c.denominateur(), c.calculable(), c.motifNonCalculable());
        }
    }

    public record LiaisonVue(
        UUID id,
        TypeProjet typeProjet,
        UUID institutionId,
        String institutionNom,
        EtatLiaison etat,
        UUID chefDeProjetId,
        String chefDeProjetNom,
        UUID banquePartenaireId,
        String banquePartenaireNom,
        String dateOuverture,
        String motifEtat,
        UUID versionId,
        Integer versionNumero,
        String phaseCourante,
        Couverture couvertureCertification,
        Couverture couvertureMiseEnService,
        int operationsEnRetard,
        List<EtatLiaison> etatsParcourus,
        List<String> perimetre,
        List<LiaisonQueries.Intervenant> intervenants,
        List<String> transitionsPermises,
        List<String> actionsPermises,
        String reference,
        StatutParcours statut,
        EtapeCode etapeCourante,
        Boolean bloquee,
        LocalDate prochaineEcheance,
        LocalDate dateCibleProduction,
        UUID liaisonOrigineId
    ) {
        static LiaisonVue de(LiaisonQueries.Fiche f) {
            Liaison l = f.liaison();
            Vues.LiaisonResume r = f.parcours();
            return new LiaisonVue(
                l.getId(), l.getTypeProjet(), l.getInstitutionId(), l.getInstitutionNom(), l.getEtat(), l.getChefDeProjetId(), f.chefDeProjetNom(),
                l.getBanquePartenaireId(), f.banquePartenaireNom(), l.getDateOuverture().toString(), l.getMotifEtat(), l.getVersion().getId(),
                l.getVersion().getNumero(), l.getEtat() == EtatLiaison.OUVERTE ? "CADRAGE" : l.getEtat().name(),
                r == null ? Couverture.AUCUNE : Couverture.de(r.couvertureCertification()),
                r == null ? Couverture.AUCUNE : Couverture.de(r.couvertureMiseEnService()),
                r == null ? 0 : (int) r.tachesEnRetard(),
                List.of(l.getEtat()), f.perimetre(), f.intervenants(), List.of(), f.actionsPermises(),
                r == null ? null : r.reference(), r == null ? null : r.statut(), r == null ? null : r.etapeCourante(), r == null ? null : r.bloquee(),
                r == null ? null : r.prochaineEcheance(), r == null ? null : r.dateCibleProduction(), l.getLiaisonOrigineId()
            );
        }

        /** Portail : même forme, sans identifiant interne du conducteur ni de la banque partenaire. */
        static LiaisonVue dePortail(LiaisonQueries.Fiche f) {
            LiaisonVue v = de(f);
            return new LiaisonVue(
                v.id(), v.typeProjet(), v.institutionId(), v.institutionNom(), v.etat(), null, v.chefDeProjetNom(), null, null, v.dateOuverture(), v.motifEtat(),
                v.versionId(), v.versionNumero(), v.phaseCourante(), v.couvertureCertification(), v.couvertureMiseEnService(), v.operationsEnRetard(), v.etatsParcourus(),
                v.perimetre(), v.intervenants(), List.of(), List.of(), v.reference(), v.statut(), v.etapeCourante(), v.bloquee(), v.prochaineEcheance(), v.dateCibleProduction(), v.liaisonOrigineId()
            );
        }
    }

    public record PageVue<T>(List<T> contenu, long total) {}

    public record OuvertureRequest(@NotNull UUID institutionId, @NotNull TypeProjet typeProjet, @NotNull UUID chefDeProjetId, UUID banquePartenaireId) {}

    public record DesignationRequest(@NotNull UUID chefDeProjetId, @Size(max = 500) String motif) {}

    public record ConducteurVue(UUID id, String nom) {}

    private final OpenLiaisonUseCase useCase;
    private final LiaisonQueries queries;

    public LiaisonsCadrageResource(OpenLiaisonUseCase useCase, LiaisonQueries queries) {
        this.useCase = useCase;
        this.queries = queries;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('HOM_LIAISON_LIST', 'HOM_LIAISON_READ')")
    public PageVue<LiaisonVue> lister(
        @RequestParam(name = "etat", required = false) EtatLiaison etat,
        @RequestParam(name = "typeProjet", required = false) TypeProjet typeProjet,
        @RequestParam(name = "institutionId", required = false) UUID institutionId,
        @RequestParam(name = "recherche", required = false) String recherche,
        @RequestParam(name = "mesProjets", defaultValue = "false") boolean mesProjets,
        @RequestParam(name = "statut", required = false) StatutParcours statut,
        @RequestParam(name = "etape", required = false) List<EtapeCode> etapes,
        @RequestParam(name = "bloquees", defaultValue = "false") boolean bloquees,
        @RequestParam(name = "sansConducteur", defaultValue = "false") boolean sansConducteur,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "25") int taille,
        Authentication auth
    ) {
        UUID conducteur = mesProjets ? sujet(auth) : null;
        var p = queries.lister(new LiaisonQueries.Filtre(etat, typeProjet, institutionId, recherche, conducteur, statut, etapes == null ? List.of() : etapes, bloquees, sansConducteur), page, taille, permissions(auth));
        return new PageVue<>(p.getContent().stream().map(LiaisonVue::de).toList(), p.getTotalElements());
    }

    @GetMapping("/conducteurs")
    @PreAuthorize("hasAnyAuthority('HOM_PROJECT_DESIGNATE', 'HOM_LIAISON_OPEN')")
    public List<ConducteurVue> conducteurs() {
        return queries.conducteurs().stream().map(c -> new ConducteurVue(c.id(), c.nomAffiche())).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public LiaisonVue lire(@PathVariable UUID id, Authentication auth) {
        return LiaisonVue.de(queries.fiche(id, permissions(auth)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HOM_LIAISON_OPEN')")
    public ResponseEntity<LiaisonVue> ouvrir(@Valid @RequestBody OuvertureRequest r, Authentication auth) {
        Liaison l = useCase.ouvrir(new OpenLiaisonCommand(r.institutionId(), r.typeProjet(), r.chefDeProjetId(), r.banquePartenaireId()), acteur(auth));
        return ResponseEntity.created(URI.create("/api/v1/liaisons/" + l.getId())).body(LiaisonVue.de(queries.fiche(l.getId(), permissions(auth))));
    }

    @PutMapping("/{id}/chef-de-projet")
    @PreAuthorize("hasAuthority('HOM_PROJECT_DESIGNATE')")
    public LiaisonVue designer(@PathVariable UUID id, @Valid @RequestBody DesignationRequest r, Authentication auth) {
        useCase.designerChefDeProjet(id, r.chefDeProjetId(), r.motif(), acteur(auth));
        return LiaisonVue.de(queries.fiche(id, permissions(auth)));
    }

    private static Set<String> permissions(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private static Acteur acteur(Authentication auth) {
        return new Acteur(sujet(auth), permissions(auth).contains(LiaisonQueries.PERMISSION_DESIGNATION));
    }

    private static UUID sujet(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt && jwt.getToken().getSubject() != null) {
            try {
                return UUID.fromString(jwt.getToken().getSubject());
            } catch (IllegalArgumentException e) {
                // sujet non conforme : refus ci-dessous
            }
        }
        throw HomologationException.forbidden("HOM_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
    }
}
