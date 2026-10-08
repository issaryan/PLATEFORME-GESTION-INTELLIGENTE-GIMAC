package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.TypesProjetService;
import cm.gimac.des.homologation.domain.DefinitionTypeProjet;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
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
 * Types de projet (D-91, docs/lot1/types-projet.md §1), exposés par la passerelle sous
 * /bff/des/homologation/types-projet : liste, lecture, création avec le brouillon v1 de son référentiel (vierge ou
 * copié), modification motivée. Lecture : HOM_REFERENTIEL_READ ; écriture : HOM_REFERENTIEL_EDIT (ou
 * REF_METIER_ADMIN, D-92). Aucune suppression.
 */
@RestController
@RequestMapping("/api/v1/types-projet")
public class TypesProjetResource {

    /** Version publiée en vigueur du référentiel du type. */
    public record ReferentielActif(UUID id, Integer version) {}

    /**
     * Type de projet : {@code fige} (code et modèle figés, une version a été publiée), {@code proposable} (actif et
     * référentiel publié : proposé au cadrage d'une liaison), {@code brouillonId} (brouillon le plus récent),
     * {@code nombreLiaisons}.
     */
    public record TypeProjetVue(
        String code,
        String libelle,
        String description,
        ModeleTypeProjet modele,
        String libelleModele,
        boolean actif,
        ReferentielActif referentielActif,
        UUID brouillonId,
        boolean fige,
        boolean proposable,
        long nombreLiaisons,
        Instant creeLe,
        UUID creePar,
        Instant modifieLe,
        UUID modifiePar
    ) {}

    public record Depart(TypesProjetService.Mode mode, @Size(max = 40) String typeSource) {}

    public record CreationRequest(
        @Size(max = 40) String code,
        @Size(max = 120) String libelle,
        @Size(max = 1000) String description,
        ModeleTypeProjet modele,
        @Valid Depart depart
    ) {}

    public record ModificationRequest(
        @Size(max = 120) String libelle,
        @Size(max = 1000) String description,
        ModeleTypeProjet modele,
        Boolean actif,
        @Size(max = 500) String motif
    ) {}

    private final TypesProjetService service;
    private final ReferentialVersionRepository versions;
    private final LiaisonRepository liaisons;

    public TypesProjetResource(TypesProjetService service, ReferentialVersionRepository versions, LiaisonRepository liaisons) {
        this.service = service;
        this.versions = versions;
        this.liaisons = liaisons;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public List<TypeProjetVue> lister(@RequestParam(name = "actif", required = false) Boolean actif) {
        return service.lister(actif).stream().map(this::vue).toList();
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_READ', 'REF_METIER_ADMIN')")
    @Transactional(readOnly = true)
    public TypeProjetVue lire(@PathVariable String code) {
        return vue(service.lire(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @Transactional
    public ResponseEntity<TypeProjetVue> creer(@Valid @RequestBody CreationRequest r, Authentication auth) {
        Depart d = r.depart() == null ? new Depart(null, null) : r.depart();
        TypesProjetService.Cree c = service.creer(
            new TypesProjetService.Creation(r.code(), r.libelle(), r.description(), r.modele(), d.mode(), d.typeSource()),
            ActeurCourant.de(auth).id()
        );
        return ResponseEntity.created(URI.create("/api/v1/types-projet/" + c.type().getCode())).body(vue(c.type()));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('HOM_REFERENTIEL_EDIT', 'REF_METIER_ADMIN')")
    @Transactional
    public TypeProjetVue modifier(@PathVariable String code, @Valid @RequestBody ModificationRequest r, Authentication auth) {
        return vue(
            service.modifier(code, new TypesProjetService.Modification(r.libelle(), r.description(), r.modele(), r.actif(), r.motif()), ActeurCourant.de(auth).id())
        );
    }

    private TypeProjetVue vue(DefinitionTypeProjet t) {
        ReferentielActif actif = versions
            .findFirstByTypeProjetAndEtat(t.type(), EtatVersionReferentiel.PUBLIEE)
            .map(v -> new ReferentielActif(v.getId(), v.getNumero()))
            .orElse(null);
        UUID brouillon = versions
            .findByTypeProjetOrderByDateCreationDesc(t.type())
            .stream()
            .filter(v -> v.getEtat() == EtatVersionReferentiel.BROUILLON)
            .map(ReferentialVersion::getId)
            .findFirst()
            .orElse(null);
        return new TypeProjetVue(
            t.getCode(),
            t.getLibelle(),
            t.getDescription(),
            t.getModele(),
            t.getModele().libelle(),
            t.isActif(),
            actif,
            brouillon,
            service.fige(t),
            t.isActif() && actif != null,
            liaisons.countByTypeProjet(t.type()),
            t.getCreeLe(),
            t.getCreePar(),
            t.getModifieLe(),
            t.getModifiePar()
        );
    }
}
