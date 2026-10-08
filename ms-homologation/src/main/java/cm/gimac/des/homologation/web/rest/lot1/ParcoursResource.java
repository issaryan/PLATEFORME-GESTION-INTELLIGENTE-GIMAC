package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes.Affectation;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes.Piece;
import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.Ceremonie;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Qualite;
import cm.gimac.des.homologation.domain.parcours.Vues;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.domain.HomologationException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
 * Parcours de la liaison, étapes 4 à 10 (docs/lot1/parcours-backend.md §1, docs/lot1/correspondance.md),
 * exposé sous /bff/des/homologation : dossier numérique, opérations, ressources (rang 6), livrables,
 * preuves, validation (FIDO2), qualités, blocages, constat de la couverture (rang 17), service
 * supplémentaire (rang 35), fiche de suivi et constat de mise en service (rangs 32, 33).
 */
@RestController
@RequestMapping("/api/v1")
public class ParcoursResource {

    static final String REAUTH_HEADER = "X-Reauth-Assertion";

    public record AffectationRequest(@NotNull Fonction fonction, @NotBlank String personneId) {}

    public record RessourcesRequest(@NotNull List<@Valid AffectationRequest> affectations) {}

    /** {@code livrable} (D-93 §3) : code du livrable attendu ; absent : l'unique livrable de l'opération. */
    public record LivrableRequest(
        @NotNull UUID documentId,
        @NotBlank String empreinte,
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 2000) String commentaire,
        @Size(max = 40) String livrable
    ) {}

    /** Preuve : toute propriété hors contrat est refusée, pour qu'aucune valeur de clé ni de secret ne soit jamais reçue (D-59). */
    public record PreuveRequest(
        @NotNull UUID documentId,
        @NotBlank String empreinte,
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 200) String valeurReference,
        @Size(max = 16) String codeControle,
        Ceremonie ceremonie,
        @Size(max = 40) String livrable,
        @JsonAnySetter Map<String, Object> autres
    ) {}

    /** Renvoi par le validateur (D-93 §2) : motif exigé. */
    public record RenvoiRequest(@NotBlank @Size(max = 500) String motif) {}

    /**
     * Affectation d'une fonction sur la liaison (D-93 §1) : {@code personneId} pour une fonction interne,
     * {@code institutionId} pour une fonction externe (banque partenaire, personnalisateur…).
     */
    public record AffectationFonctionRequest(String personneId, UUID institutionId, @Size(max = 500) String motif) {}

    public record IntervenantRequest(@NotNull Qualite qualite, @NotBlank String personneId, @Size(max = 500) String motif) {}

    public record BlocageRequest(@NotBlank @Size(max = 500) String motif, @NotBlank @Size(max = 500) String prochaineAction, @NotBlank String responsableId, @NotNull LocalDate echeanceLevee) {}

    public record LeveeRequest(@Size(max = 500) String commentaire) {}

    public record ServicesRequest(@NotNull List<String> services) {}

    private final ParcoursLectures lectures;
    private final ParcoursCommandes commandes;
    private final LiaisonQueries liaisons;
    private final ParcoursProjection projection;

    public ParcoursResource(ParcoursLectures lectures, ParcoursCommandes commandes, LiaisonQueries liaisons, ParcoursProjection projection) {
        this.lectures = lectures;
        this.commandes = commandes;
        this.liaisons = liaisons;
        this.projection = projection;
    }

    @GetMapping("/liaisons/{id}/dossier")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public Vues.Dossier dossier(@PathVariable UUID id, Authentication auth) {
        return lectures.dossier(id, ActeurCourant.de(auth));
    }

    @GetMapping("/liaisons/{id}/parcours/operations")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public List<Vues.Operation> operations(
        @PathVariable UUID id,
        @RequestParam(name = "etape", required = false) EtapeCode etape,
        @RequestParam(name = "fonction", required = false) Fonction fonction,
        Authentication auth
    ) {
        return lectures.operations(id, etape, fonction, ActeurCourant.de(auth));
    }

    @PutMapping("/liaisons/{id}/ressources")
    @PreAuthorize("hasAuthority('HOM_STEP_MANAGE')")
    public ResponseEntity<Void> ressources(@PathVariable UUID id, @Valid @RequestBody RessourcesRequest r, Authentication auth) {
        commandes.affecterRessources(id, r.affectations().stream().map(a -> new Affectation(a.fonction(), a.personneId())).toList(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/parcours/operations/{id}/livrable")
    @PreAuthorize("hasAnyAuthority('HOM_STEP_MANAGE', 'HOM_EVIDENCE_SUBMIT')")
    public ResponseEntity<Void> livrable(@PathVariable UUID id, @Valid @RequestBody LivrableRequest r, Authentication auth) {
        commandes.deposerLivrable(id, r.livrable(), new Piece(r.documentId(), r.empreinte(), r.nom(), r.commentaire()), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    /**
     * D-98 : comme le livrable, la preuve est ouverte à HOM_STEP_MANAGE ou HOM_EVIDENCE_SUBMIT ; le droit de dépôt (R
     * désigné, porteur de la fonction à affecter, déposant, conducteur, supervision) reste décidé par le parcours. Sans
     * cela, le chargé de la DDRM, responsable des preuves du cadrage, ne pouvait pas les déposer.
     */
    @PostMapping("/parcours/operations/{id}/preuves")
    @PreAuthorize("hasAnyAuthority('HOM_STEP_MANAGE', 'HOM_EVIDENCE_SUBMIT')")
    public ResponseEntity<Void> preuve(@PathVariable UUID id, @Valid @RequestBody PreuveRequest r, Authentication auth) {
        if (r.autres() != null && !r.autres().isEmpty()) {
            throw HomologationException.unprocessable(
                "HOM_EVIDENCE_FIELD_REFUSED",
                "Une preuve ne porte que sa pièce, sa valeur de référence, un code de contrôle ou la cérémonie : aucune valeur de clé ni de secret."
            );
        }
        commandes.deposerPreuve(id, r.livrable(), new Piece(r.documentId(), r.empreinte(), r.nom(), null), r.valeurReference(), r.codeControle(), r.ceremonie(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/parcours/operations/{id}/validation")
    @PreAuthorize("hasAuthority('HOM_OPERATION_VALIDATE')")
    public ResponseEntity<Void> validation(@PathVariable UUID id, @RequestHeader(name = REAUTH_HEADER, required = false) String assertion, Authentication auth) {
        commandes.valider(id, assertion, ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    /** Renvoi au responsable avec motif (D-93 §2) : réservé au validateur désigné, l'opération revient à faire. */
    @PostMapping("/parcours/operations/{id}/renvoi")
    @PreAuthorize("hasAuthority('HOM_OPERATION_VALIDATE')")
    public ResponseEntity<Void> renvoi(@PathVariable UUID id, @Valid @RequestBody RenvoiRequest r, Authentication auth) {
        commandes.renvoyer(id, r.motif(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    /** Affectation d'une fonction à une personne ou à une institution liée, à tout moment du parcours (D-93 §1). */
    @PutMapping("/liaisons/{id}/fonctions/{fonction}")
    @PreAuthorize("hasAnyAuthority('HOM_STEP_MANAGE', 'HOM_OPERATION_ASSIGN')")
    public ResponseEntity<Void> affecterFonction(@PathVariable UUID id, @PathVariable Fonction fonction, @Valid @RequestBody AffectationFonctionRequest r, Authentication auth) {
        commandes.affecterFonction(id, fonction, r.personneId(), r.institutionId(), r.motif(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    /**
     * Signature d'une opération en attente de signatures (D-91, docs/lot1/types-projet.md §3) : opération désignée par
     * son code (rang) dans la liaison, signature par son code ; assertion FIDO2 de finalité SIGNATURE_OPERATION dans
     * l'en-tête X-Reauth-Assertion. Le signataire attendu et l'ordre sont vérifiés par le service.
     */
    @PostMapping("/liaisons/{id}/operations/{code}/signatures/{signature}")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public ResponseEntity<Void> signer(
        @PathVariable UUID id,
        @PathVariable String code,
        @PathVariable String signature,
        @RequestHeader(name = REAUTH_HEADER, required = false) String assertion,
        Authentication auth
    ) {
        commandes.signer(id, code, signature, assertion, ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/parcours/operations/{id}/intervenants")
    @PreAuthorize("hasAnyAuthority('HOM_OPERATION_ASSIGN', 'HOM_STEP_MANAGE')")
    public ResponseEntity<Void> intervenants(@PathVariable UUID id, @Valid @RequestBody IntervenantRequest r, Authentication auth) {
        commandes.attribuer(id, r.qualite(), r.personneId(), r.motif(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/liaisons/{id}/blocages")
    @PreAuthorize("hasAuthority('HOM_STEP_MANAGE')")
    public ResponseEntity<Vues.Blocage> declarerBlocage(@PathVariable UUID id, @Valid @RequestBody BlocageRequest r, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        BlocageParcours b = commandes.declarerBlocage(id, r.motif(), r.prochaineAction(), r.responsableId(), r.echeanceLevee(), a);
        return ResponseEntity.created(URI.create("/api/v1/blocages/" + b.getId())).body(projection.blocage(b, a));
    }

    @PostMapping("/blocages/{id}/levee")
    @PreAuthorize("hasAuthority('HOM_STEP_MANAGE')")
    public ResponseEntity<Void> leverBlocage(@PathVariable UUID id, @Valid @RequestBody(required = false) LeveeRequest r, Authentication auth) {
        commandes.leverBlocage(id, r == null ? null : r.commentaire(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/liaisons/{id}/couverture/constat")
    @PreAuthorize("hasAuthority('CER_DOSSIER_VERIFY')")
    public ResponseEntity<Void> constaterCouverture(@PathVariable UUID id, Authentication auth) {
        commandes.constaterCouverture(id, ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/liaisons/{id}/services-supplementaires")
    @PreAuthorize("hasAnyAuthority('HOM_LIAISON_OPEN', 'HOM_STEP_MANAGE')")
    public ResponseEntity<LiaisonsCadrageResource.LiaisonVue> serviceSupplementaire(@PathVariable UUID id, @Valid @RequestBody ServicesRequest r, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        Liaison n = commandes.ouvrirServiceSupplementaire(id, r.services(), a);
        return ResponseEntity.created(URI.create("/api/v1/liaisons/" + n.getId())).body(LiaisonsCadrageResource.LiaisonVue.de(liaisons.fiche(n.getId(), a.permissions())));
    }

    @GetMapping("/liaisons/{id}/fiche-suivi")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public Vues.FicheSuivi fiche(@PathVariable UUID id, Authentication auth) {
        return lectures.fiche(id, ActeurCourant.de(auth));
    }

    @PostMapping("/liaisons/{id}/fiche-suivi/remise")
    // Remise par le conducteur (conduite de l'étape 10) ou par Normes et Agréments, comme l'interface (PERMISSION_GESTE).
    @PreAuthorize("hasAnyAuthority('HOM_STEP_MANAGE', 'CER_DOSSIER_VERIFY')")
    public ResponseEntity<Void> remettreFiche(@PathVariable UUID id, Authentication auth) {
        commandes.remettreFiche(id, ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/liaisons/{id}/fiche-suivi/constat")
    @PreAuthorize("hasAuthority('CER_DOSSIER_VERIFY')")
    public ResponseEntity<Void> constaterMiseEnService(@PathVariable UUID id, Authentication auth) {
        commandes.constaterMiseEnService(id, ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }
}
