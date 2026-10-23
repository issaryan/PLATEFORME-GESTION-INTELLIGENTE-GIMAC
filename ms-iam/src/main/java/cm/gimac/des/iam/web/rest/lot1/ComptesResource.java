package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Administration des comptes par le super administrateur et le RSSI (D-62), exposée sous
 * /bff/console/iam/comptes et /bff/des/iam/comptes (D-69). Aucune inscription libre : tout compte est créé ici.
 */
@RestController
@RequestMapping("/api/v1/comptes")
public class ComptesResource {

    static final String REAUTH = "X-Reauth-Assertion";

    public record CompteVue(
        String id,
        String identifiant,
        String prenom,
        String nom,
        String nomAffiche,
        String email,
        boolean emailVerifie,
        boolean actif,
        Instant creeLe,
        String telephone,
        String fonction,
        String langue,
        String institutionId,
        boolean profilComplet,
        boolean premierAccesEnAttente,
        List<String> profils,
        List<String> actionsRequises,
        boolean secondFacteur,
        String avatarVersion,
        String unite,
        String responsableId
    ) {
        static CompteVue de(Compte c) {
            return new CompteVue(
                c.id(), c.identifiant(), c.prenom(), c.nom(), c.nomAffiche(), c.email(), c.emailVerifie(), c.actif(), c.creeLe(), c.telephone(),
                c.fonction(), c.langue(), c.institutionId(), c.profilComplet(), c.premierAccesEnAttente(), c.profils().stream().sorted().toList(),
                c.actionsRequises().stream().map(Enum::name).toList(), c.secondFacteur(), null, c.unite(), c.responsableId()
            );
        }

        /** Même vue avec la version de la photo de profil (docs/lot1/mon-compte.md §1), nulle sans photo. */
        CompteVue avecAvatar(String version) {
            return new CompteVue(
                id, identifiant, prenom, nom, nomAffiche, email, emailVerifie, actif, creeLe, telephone, fonction, langue, institutionId, profilComplet,
                premierAccesEnAttente, profils, actionsRequises, secondFacteur, version, unite, responsableId
            );
        }
    }

    public record PageVue<T>(List<T> contenu, long total) {}

    public record CreationRequest(
        @NotBlank @Size(max = 50) String identifiant,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String telephone,
        @Size(max = 200) String fonction,
        @Size(max = 36) String institutionId,
        @NotEmpty Set<String> profils,
        boolean envoyerInvitation,
        boolean motDePasseProvisoire
    ) {}

    public record CreationVue(CompteVue compte, boolean invitationEnvoyee, String motDePasseProvisoire) {}

    public record FicheRequest(
        @Size(max = 100) String prenom,
        @Size(max = 100) String nom,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String telephone,
        @Size(max = 200) String fonction,
        @Size(max = 5) String langue,
        @Size(max = 36) String institutionId
    ) {}

    /**
     * Rattachement organisationnel (docs/lot1/mon-activite.md §1.3) ; null retire la valeur. Motif facultatif, porté au
     * journal COMPTE_RATTACHEMENT_MODIFIE (D-102 : gestes de l'organigramme).
     */
    public record RattachementRequest(@Size(max = 32) String unite, @Size(max = 36) String responsableId, @Size(max = 1000) String motif) {}

    public record ProfilsRequest(@NotNull Set<String> profils, @NotBlank @Size(max = 1000) String motif) {}

    public record MotifRequest(@NotBlank @Size(max = 1000) String motif) {}

    public record ActionsRequest(@NotEmpty Set<ActionRequise> actions, boolean parCourriel, @NotBlank @Size(max = 1000) String motif) {}

    public record MotDePasseVue(String motDePasseProvisoire) {}

    public record JournalVue(String id, Instant horodatage, String acteurId, String acteurNom, String action, String cibleId, String cibleLibelle, String motif, String details, String correlationId) {
        static JournalVue de(JournalAdministration j) {
            return new JournalVue(j.getId().toString(), j.getHorodatage(), j.getActeurId(), j.getActeurNom(), j.getAction(), j.getCibleId(), j.getCibleLibelle(), j.getMotif(), j.getDetails(), j.getCorrelationId());
        }
    }

    private final AdministrationComptesService service;
    private final JournalAdministrationRepository journal;
    private final ReauthAdministration reauth;
    private final AvatarService avatars;
    private final OrganisationService organisation;

    public ComptesResource(
        AdministrationComptesService service,
        JournalAdministrationRepository journal,
        ReauthAdministration reauth,
        AvatarService avatars,
        OrganisationService organisation
    ) {
        this.service = service;
        this.journal = journal;
        this.reauth = reauth;
        this.avatars = avatars;
        this.organisation = organisation;
    }

    // ------------------------------------------------------------------ lectures

    @GetMapping
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public PageVue<CompteVue> lister(
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "profil", required = false) String profil,
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "25") int taille
    ) {
        var p = service.lister(new AdministrationComptesService.Filtre(q, profil, etat), page, taille);
        Map<String, String> versions = avatars.versions(p.contenu().stream().map(Compte::id).toList());
        return new PageVue<>(p.contenu().stream().map(c -> CompteVue.de(c).avecAvatar(versions.get(c.id()))).toList(), p.total());
    }

    @GetMapping("/indicateurs")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public AdministrationComptesService.Indicateurs indicateurs() {
        return service.indicateurs();
    }

    @GetMapping("/journal")
    @PreAuthorize("hasAuthority('IAM_JOURNAL_READ')")
    public PageVue<JournalVue> journal(
        @RequestParam(name = "compte", required = false) String compte,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "25") int taille
    ) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, 100));
        Page<JournalAdministration> p = compte == null ? journal.findAllByOrderByHorodatageDesc(pr) : journal.findByCibleIdOrderByHorodatageDesc(compte, pr);
        return new PageVue<>(p.getContent().stream().map(JournalVue::de).toList(), p.getTotalElements());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public CompteVue lire(@PathVariable String id) {
        return CompteVue.de(service.lire(id)).avecAvatar(avatars.version(id).orElse(null));
    }

    @GetMapping("/{id}/sessions")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public List<SessionCompte> sessions(@PathVariable String id) {
        return service.sessions(id);
    }

    // ------------------------------------------------------------------ écritures

    @PostMapping
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_CREATE')")
    public ResponseEntity<CreationVue> creer(@Valid @RequestBody CreationRequest r, Authentication auth) {
        var c = service.creer(
            ActeurCourant.de(auth),
            new NouveauCompte(r.identifiant().trim().toLowerCase(java.util.Locale.ROOT), r.prenom().trim(), r.nom().trim(), r.email().trim(), r.telephone(), r.fonction(), r.institutionId(), r.profils()),
            r.envoyerInvitation(),
            r.motDePasseProvisoire()
        );
        return ResponseEntity.created(URI.create("/api/v1/comptes/" + c.compte().id())).body(new CreationVue(CompteVue.de(c.compte()), c.invitationEnvoyee(), c.motDePasseProvisoire()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_EDIT')")
    public CompteVue modifier(@PathVariable String id, @Valid @RequestBody FicheRequest r, Authentication auth) {
        return CompteVue.de(service.modifier(ActeurCourant.de(auth), id, new FicheCompte(r.prenom(), r.nom(), r.email(), r.telephone(), r.fonction(), r.langue(), r.institutionId())));
    }

    /** Rattachement organisationnel d'un compte (D-76, docs/lot1/mon-activite.md §1.2) ; réponse : la fiche du compte. */
    @PutMapping("/{id}/rattachement")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_EDIT')")
    public CompteVue rattacher(@PathVariable String id, @Valid @RequestBody RattachementRequest r, Authentication auth) {
        return CompteVue.de(organisation.rattacher(ActeurCourant.de(auth), id, r.unite(), r.responsableId(), r.motif(), null)).avecAvatar(avatars.version(id).orElse(null));
    }

    @PutMapping("/{id}/profils")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_ROLES')")
    public CompteVue profils(@PathVariable String id, @Valid @RequestBody ProfilsRequest r, @RequestHeader(name = REAUTH, required = false) String assertion, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return CompteVue.de(service.definirProfils(a, id, r.profils(), r.motif()));
    }

    @PostMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_STATUS')")
    public CompteVue activer(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return CompteVue.de(service.definirActif(ActeurCourant.de(auth), id, true, r.motif()));
    }

    @PostMapping("/{id}/desactivation")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_STATUS')")
    public CompteVue desactiver(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return CompteVue.de(service.definirActif(ActeurCourant.de(auth), id, false, r.motif()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_DELETE')")
    public ResponseEntity<Void> supprimer(@PathVariable String id, @RequestParam("motif") String motif, @RequestHeader(name = REAUTH, required = false) String assertion, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        service.supprimer(a, id, motif);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/actions-requises")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public CompteVue exigerActions(@PathVariable String id, @Valid @RequestBody ActionsRequest r, Authentication auth) {
        return CompteVue.de(service.exigerActions(ActeurCourant.de(auth), id, r.actions(), r.parCourriel(), r.motif()));
    }

    @PostMapping("/{id}/second-facteur/reinitialisation")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public CompteVue reinitialiserSecondFacteur(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return CompteVue.de(service.reinitialiserSecondFacteur(ActeurCourant.de(auth), id, r.motif()));
    }

    @PostMapping("/{id}/mot-de-passe-provisoire")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public MotDePasseVue motDePasseProvisoire(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return new MotDePasseVue(service.motDePasseProvisoire(ActeurCourant.de(auth), id, r.motif()));
    }

    /** US-EXI-23 : verrouillage du compte (protection contre la force brute), pour la console. */
    @GetMapping("/{id}/verrouillage")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public cm.gimac.des.iam.domain.compte.Verrouillage verrouillage(@PathVariable String id) {
        return service.verrouillage(id);
    }

    /** US-EXI-23 : déblocage par l'administrateur du GIMAC, motivé. */
    @PostMapping("/{id}/deblocage")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public cm.gimac.des.iam.domain.compte.Verrouillage debloquer(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return service.debloquer(ActeurCourant.de(auth), id, r.motif());
    }

    @PostMapping("/{id}/sessions/fermeture")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public ResponseEntity<Void> fermerSessions(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        service.fermerSessions(ActeurCourant.de(auth), id, r.motif());
        return ResponseEntity.noContent().build();
    }
}
