package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.StrongReauthPort;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.event.CoverageEstablishedPayload;
import cm.gimac.des.homologation.domain.event.OperationSignedPayload;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import cm.gimac.des.homologation.domain.parcours.Ceremonie;
import cm.gimac.des.homologation.domain.parcours.Couverture;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Qualite;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.parcours.SignatureRecueillie;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.domain.service.VisibiliteReferentiel;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conduite du parcours, étapes 4 à 10 (docs/lot1/parcours-backend.md §1) : affectation des ressources, livrables,
 * preuves, validation par réauthentification forte, qualités, blocages datés, constat de la couverture,
 * service supplémentaire, fiche de suivi et son constat (gestes lus sur les opérations, D-77) ; gestes du participant
 * sur le portail, cloisonnés à l'institution du jeton. Chaque écriture verrouille la liaison.
 */
@Service
@Transactional
public class ParcoursCommandes {

    private static final Logger LOG = LoggerFactory.getLogger(ParcoursCommandes.class);
    // Finalité du défi demandé par l'interface (SignPurpose du socle : OPERATION_VALIDATE), vérifiée par MS-IAM.
    static final String FINALITE_VALIDATION = "OPERATION_VALIDATE";
    // Finalité du défi FIDO2 d'une signature d'opération (D-91), vérifiée par MS-IAM.
    static final String FINALITE_SIGNATURE = "SIGNATURE_OPERATION";
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern CODE_SERVICE = Pattern.compile("^[A-Z0-9_]{2,40}$");

    /** Pièce déposée dans MS-DOCUMENT par l'interface, rattachée par référence et empreinte. */
    public record Piece(UUID documentId, String empreinte, String nom, String commentaire) {}

    /** Affectation d'une ressource à une fonction (geste AFFECTER_RESSOURCES). */
    public record Affectation(Fonction fonction, String personneId) {}

    private final ParcoursDepot depot;
    private final ParcoursInstanciation instanciation;
    private final Intervenants intervenants;
    private final DocumentRegistryPort documents;
    private final StrongReauthPort reauth;
    private final OutboxPublisher outbox;
    private final LiaisonRepository liaisons;
    private final ReferentialVersionRepository versions;
    private final ContractualScopeRepository perimetres;
    private final ScopeItemRepository elements;
    private final Clock clock;
    private cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort participants;

    /** Registre des institutions (institution liée d'une fonction externe, D-93 §1). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setParticipants(cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort participants) {
        this.participants = participants;
    }

    public ParcoursCommandes(
        ParcoursDepot depot,
        ParcoursInstanciation instanciation,
        Intervenants intervenants,
        DocumentRegistryPort documents,
        StrongReauthPort reauth,
        OutboxPublisher outbox,
        LiaisonRepository liaisons,
        ReferentialVersionRepository versions,
        ContractualScopeRepository perimetres,
        ScopeItemRepository elements,
        Clock clock
    ) {
        this.depot = depot;
        this.instanciation = instanciation;
        this.intervenants = intervenants;
        this.documents = documents;
        this.reauth = reauth;
        this.outbox = outbox;
        this.liaisons = liaisons;
        this.versions = versions;
        this.perimetres = perimetres;
        this.elements = elements;
        this.clock = clock;
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(clock);
    }

    /** Parcours verrouillé d'une liaison ; une liaison ouverte avant cette tranche est instanciée à la volée. */
    Parcours charger(UUID liaisonId) {
        return depot.trouver(liaisonId, true).orElseGet(() -> instanciation.instancierExistante(depot.liaison(liaisonId)));
    }

    /** Portail : la liaison doit appartenir à l'institution du jeton, sinon elle est introuvable (RG-59). */
    static Parcours sienne(Parcours p, Acteur a) {
        if (a.institutionId() == null || !p.liaison().getInstitutionId().toString().equalsIgnoreCase(a.institutionId())) {
            throw HomologationException.notFound("HOM_LIAISON_NOT_FOUND", "Ce parcours est introuvable.", p.id());
        }
        return p;
    }

    private static String deposant(Acteur a, OperationParcours o) {
        if (a.nom() != null && !a.nom().isBlank()) {
            return a.nom();
        }
        Personne r = o == null ? null : o.getRaci().r();
        return r == null ? "" : r.nom();
    }

    /** Pièce revérifiée auprès de MS-DOCUMENT : inconnue ou d'empreinte divergente, elle est refusée. */
    private Preuve piece(Piece d, Acteur a, OperationParcours o) {
        if (d.documentId() == null || d.empreinte() == null || !SHA256.matcher(d.empreinte()).matches() || d.nom() == null || d.nom().isBlank()) {
            throw HomologationException.unprocessable("HOM_DOCUMENT_INVALID", "La pièce doit porter sa référence, son nom et son empreinte SHA-256.");
        }
        switch (documents.verifier(d.documentId(), d.empreinte())) {
            case INCONNUE -> throw HomologationException.unprocessable("HOM_DOCUMENT_UNKNOWN", "La pièce est inconnue du registre documentaire.");
            case DIVERGENTE -> throw HomologationException.unprocessable("HOM_DOCUMENT_HASH_MISMATCH", "L'empreinte de la pièce ne correspond pas à celle du registre documentaire.");
            default -> {
                // conforme
            }
        }
        return Preuve.piece(d.documentId().toString(), d.nom().trim(), d.empreinte().toLowerCase(java.util.Locale.ROOT), aujourdhui(), deposant(a, o));
    }

    /** Preuve produite par le service lui-même (constat, affectation, fiche) : référence et empreinte de son contenu. */
    private Preuve preuveInterne(String prefixe, Parcours p, String nom, String contenu, Acteur a) {
        return Preuve.piece(prefixe + "-" + p.etat().getReference(), nom, sha256(contenu), aujourdhui(), deposant(a, null));
    }

    static String sha256(String contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    // ------------------------------------------------------------------ DES

    /** Affectation des ressources (geste AFFECTER_RESSOURCES). */
    public void affecterRessources(UUID liaisonId, List<Affectation> affectations, Acteur a) {
        Parcours p = charger(liaisonId);
        if (p.operations(o -> o.porte(Geste.AFFECTER_RESSOURCES) && o.getEtat().ouverte()).isEmpty()) {
            p.affecterRessources(List.of(), null, aujourdhui());
        }
        List<Personne> personnes = new ArrayList<>();
        for (Affectation x : affectations) {
            personnes.add(intervenants.pourFonction(x.personneId(), x.fonction()));
        }
        String contenu = personnes.stream().map(x -> x.fonction() + "=" + x.id()).reduce("", (u, v) -> u + ";" + v);
        p.affecterRessources(personnes, preuveInterne("AFF", p, "designation-ressources", contenu, a), aujourdhui());
    }

    public void deposerLivrable(UUID operationId, Piece d, Acteur a) {
        deposerLivrable(operationId, null, d, a);
    }

    /**
     * Dépôt d'un livrable attendu (D-93 §2 et §3) : livrable désigné par son code (sans code : l'unique livrable),
     * déposant contrôlé (R désigné, déposant du livrable, conducteur, sinon supervision HOM_STEP_MANAGE journalisée),
     * pièce revérifiée et type contrôlé auprès de MS-DOCUMENT ; l'opération se clôt quand aucun livrable obligatoire
     * ne manque.
     */
    public void deposerLivrable(UUID operationId, String livrable, Piece d, Acteur a) {
        Parcours p = depot.deLOperation(operationId);
        OperationParcours o = p.operation(operationId);
        if (!o.getEtat().ouverte()) {
            p.deposerLivrable(o, null, aujourdhui());
        }
        Livrable l = Parcours.livrableVise(o, livrable);
        Parcours.DroitDepot droit = p.exigerDroitDepot(o, l, a, VisibiliteReferentiel.fonctions(a.permissions()));
        Preuve piece = piece(d, a, o);
        String type = typeControle(l, d.documentId());
        p.deposerLivrable(o, l, piece, type, a.id() == null ? null : a.id().toString(), droit == Parcours.DroitDepot.SUPERVISION, aujourdhui());
        journaliser(droit, o, p, a, l);
    }

    /** Type de la pièce, lu chez MS-DOCUMENT quand le livrable restreint les types admis (422 HOM_LIVRABLE_TYPE_REFUSE). */
    private String typeControle(Livrable l, UUID documentId) {
        if (l == null) {
            return null;
        }
        String type = documents.typeMime(documentId);
        Parcours.verifierType(l, type);
        return type;
    }

    private static void journaliser(Parcours.DroitDepot droit, OperationParcours o, Parcours p, Acteur a, Livrable l) {
        if (droit == Parcours.DroitDepot.SUPERVISION) {
            LOG.warn(
                "Supervision (HOM_STEP_MANAGE) : dépôt hors RACI sur l'opération {} de la liaison {} par {}, livrable {}",
                o.getRang(),
                p.id(),
                a.id(),
                l == null ? "-" : l.code()
            );
        }
    }

    public void deposerPreuve(UUID operationId, Piece d, String valeurReference, String codeControle, Ceremonie ceremonie, Acteur a) {
        deposerPreuve(operationId, null, d, valeurReference, codeControle, ceremonie, a);
    }

    /** Preuve d'un livrable attendu (D-93 §3), même contrôle du déposant et du type que le livrable. */
    public void deposerPreuve(UUID operationId, String livrable, Piece d, String valeurReference, String codeControle, Ceremonie ceremonie, Acteur a) {
        Parcours p = depot.deLOperation(operationId);
        OperationParcours o = p.operation(operationId);
        Livrable l = o.getEtat().ouverte() ? Parcours.livrableVise(o, livrable) : null;
        Parcours.DroitDepot droit = o.getEtat().ouverte() ? p.exigerDroitDepot(o, l, a, VisibiliteReferentiel.fonctions(a.permissions())) : null;
        // Contrôles métier avant la revérification de la pièce : une preuve irrecevable n'appelle pas MS-DOCUMENT.
        Preuve provisoire = Preuve.piece("", "", "", aujourdhui(), "");
        p.deposerPreuve(o, l, provisoire, valeurReference, codeControle, ceremonie);
        Preuve controlee = o.getPreuve();
        Preuve reelle = piece(d, a, o);
        String type = typeControle(l, d.documentId());
        o.setPreuve(new Preuve(reelle.documentRef(), reelle.nom(), reelle.empreinte(), reelle.deposeLe(), reelle.deposePar(), controlee.valeurReference(), controlee.codeControle(), controlee.ceremonie()));
        p.inscrirePreuve(o, l, type, a.id() == null ? null : a.id().toString(), droit == Parcours.DroitDepot.SUPERVISION);
        journaliser(droit, o, p, a, l);
    }

    /** Validation (V) après réauthentification forte : le V désigné (ou un porteur de sa fonction s'il est à affecter), jamais R. */
    public void valider(UUID operationId, String assertion, Acteur a) {
        if (assertion == null || assertion.isBlank()) {
            throw HomologationException.reauthRequired();
        }
        if (a.id() == null) {
            throw HomologationException.forbidden("HOM_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
        }
        Parcours p = depot.deLOperation(operationId);
        OperationParcours o = p.operation(operationId);
        if (o.getEtat() == cm.gimac.des.homologation.domain.parcours.EtatOperationParcours.EN_VALIDATION) {
            p.exigerValidateur(o, a, VisibiliteReferentiel.fonctions(a.permissions()));
            if (!a.est(o.getRaci().r()) && !reauth.verifier(assertion, a.id(), FINALITE_VALIDATION)) {
                throw HomologationException.forbidden("HOM_REAUTH_INVALID", "Réauthentification forte invalide");
            }
        }
        p.valider(o, a, aujourdhui());
    }

    /** Renvoi par le validateur avec motif (D-93 §2) : l'opération revient à faire, la tâche de R est rouverte. */
    public void renvoyer(UUID operationId, String motif, Acteur a) {
        if (a.id() == null) {
            throw HomologationException.forbidden("HOM_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
        }
        Parcours p = depot.deLOperation(operationId);
        OperationParcours o = p.operation(operationId);
        if (o.getEtat() == cm.gimac.des.homologation.domain.parcours.EtatOperationParcours.EN_VALIDATION) {
            p.exigerValidateur(o, a, VisibiliteReferentiel.fonctions(a.permissions()));
        }
        p.renvoyer(o, a, motif, aujourdhui());
        LOG.info("Opération {} de la liaison {} renvoyée par {}", o.getRang(), p.id(), a.id());
    }

    /**
     * Affectation d'une fonction sur la liaison (D-93 §1), à tout moment : une personne de l'annuaire qui détient un
     * profil de la fonction (fonction interne) ou une institution du registre (fonction externe : banque partenaire,
     * personnalisateur…). Elle remplace la fonction à affecter et l'ancien titulaire sur les opérations non faites.
     */
    public void affecterFonction(UUID liaisonId, Fonction f, String personneId, UUID institutionId, String motif, Acteur a) {
        if (Fonction.PARTICIPANT.equals(f) || Fonction.CONDUCTEUR.equals(f) || !f.connue()) {
            throw HomologationException.unprocessable(
                "HOM_FONCTION_NON_AFFECTABLE",
                "Cette fonction ne s'affecte pas ainsi : le participant tient sa liaison, le conducteur est désigné par un chef de département."
            );
        }
        Parcours p = charger(liaisonId);
        Personne personne;
        if (f.externe()) {
            if (institutionId == null || participants == null) {
                throw HomologationException.unprocessable("HOM_INSTITUTION_REQUISE", "Une fonction externe est tenue par une institution du registre.");
            }
            personne = Personne.institution(f, institutionId.toString(), participants.getInstitution(institutionId).nom());
        } else {
            personne = intervenants.pourFonction(personneId, f);
        }
        p.affecterFonction(f, personne);
        LOG.info("Fonction {} affectée sur la liaison {} par {} : {} ({})", f, p.id(), a.id(), personne.id(), motif);
    }

    /**
     * Signature d'une opération en attente de signatures (D-91, docs/lot1/types-projet.md §3) : le signataire est la
     * personne qui tient la fonction attendue sur l'opération ou la liaison, à défaut un porteur de cette fonction ;
     * l'ordre est contrôlé avant la réauthentification forte (finalité SIGNATURE_OPERATION, vérifiée et consommée par
     * MS-IAM) ; l'événement homologation.operation-signed.v1 est scellé par le registre d'audit.
     */
    public SignatureRecueillie signer(UUID liaisonId, String rang, String code, String assertion, Acteur a) {
        if (assertion == null || assertion.isBlank()) {
            throw HomologationException.reauthRequired();
        }
        if (a.id() == null) {
            throw HomologationException.forbidden("HOM_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
        }
        Parcours p = charger(liaisonId);
        OperationParcours o = p.operation(rang).orElseThrow(() -> HomologationException.notFound("HOM_OPERATION_NOT_FOUND", "Opération introuvable dans ce parcours.", rang));
        SignatureOperation s = Parcours.signature(o, code);
        p.verifierSignable(o, code);
        Personne signataire = signataire(p, o, s, a);
        if (!reauth.verifier(assertion, a.id(), FINALITE_SIGNATURE)) {
            throw HomologationException.forbidden("HOM_REAUTH_INVALID", "Réauthentification forte invalide");
        }
        SignatureRecueillie r = p.signer(o, code, signataire, clock.instant(), aujourdhui());
        outbox.publish(
            OpenLiaisonService.AGREGAT,
            p.id(),
            OperationSignedPayload.TYPE,
            new OperationSignedPayload(
                p.id(),
                p.etat().getReference(),
                p.liaison().getInstitutionId(),
                o.getId(),
                o.getRang(),
                o.getLibelle(),
                s.code(),
                s.libelle(),
                s.ordreEffectif(),
                s.signataire().name(),
                r.signataireId(),
                r.signataireNom(),
                r.empreinte(),
                r.documents(),
                o.signaturesAttendues().stream().map(SignatureOperation::code).toList(),
                o.faite(),
                ParcoursProjection.lienOperation(p, o),
                r.signeLe()
            ),
            p.liaison().getInstitutionId().toString()
        );
        LOG.info("Signature {} de l'opération {} de la liaison {} donnée par {}", code, o.getRang(), p.id(), a.id());
        return r;
    }

    /** Signataire admis : la personne désignée pour la fonction, sinon un porteur de la fonction (403 HOM_SIGNATURE_NON_SIGNATAIRE). */
    private static Personne signataire(Parcours p, OperationParcours o, SignatureOperation s, Acteur a) {
        String nom = a.nom() == null || a.nom().isBlank() ? s.signataire().libelle() : a.nom();
        java.util.Optional<Personne> designe = p.titulaire(o, s.signataire());
        boolean admis = designe.isPresent()
            ? a.est(designe.orElseThrow())
            : cm.gimac.des.homologation.domain.service.VisibiliteReferentiel.fonctions(a.permissions()).contains(s.signataire());
        if (!admis) {
            throw HomologationException.forbidden(
                "HOM_SIGNATURE_NON_SIGNATAIRE",
                "Cette signature revient à " + designe.map(Personne::nom).orElse("un porteur de la fonction " + s.signataire().libelle()) + "."
            );
        }
        return new Personne(a.id().toString(), nom, s.signataire());
    }

    /** Attribution d'une qualité (R, C, V, I, E) à une personne de l'annuaire ou au correspondant du participant. */
    public void attribuer(UUID operationId, Qualite q, String personneId, String motif, Acteur a) {
        Parcours p = depot.deLOperation(operationId);
        OperationParcours o = p.operation(operationId);
        Personne correspondant = Personne.correspondant(p.liaison().getInstitutionId().toString(), p.liaison().getInstitutionNom());
        Personne personne = correspondant.id().equals(personneId) ? correspondant : intervenants.personne(personneId);
        p.attribuer(o, q, personne);
        LOG.info("Qualité {} attribuée sur l'opération {} de la liaison {} par {} : {}", q, o.getRang(), p.id(), a.id(), motif);
    }

    public BlocageParcours declarerBlocage(UUID liaisonId, String motif, String prochaineAction, String responsableId, LocalDate echeanceLevee, Acteur a) {
        Parcours p = charger(liaisonId);
        Personne responsable = intervenants.personne(responsableId);
        BlocageParcours b = p.declarerBlocage(motif, prochaineAction, responsable, echeanceLevee, a.id(), aujourdhui());
        depot.ajouter(b);
        return b;
    }

    public void leverBlocage(UUID blocageId, String commentaire, Acteur a) {
        Parcours p = depot.duBlocage(blocageId);
        p.leverBlocage(p.blocage(blocageId).orElseThrow(), commentaire, aujourdhui());
    }

    /** Constat de la couverture (geste CONSTATER_COUVERTURE) : publie homologation.coverage-established.v1 pour MS-CERTIFICATION. */
    public Couverture constaterCouverture(UUID liaisonId, Acteur a) {
        Parcours p = charger(liaisonId);
        Couverture c = p.constaterCouverture(preuveInterne("COUV", p, "constat-couverture", p.id() + ":" + p.servicesCouverts(), a), aujourdhui());
        outbox.publish(
            OpenLiaisonService.AGREGAT,
            p.id(),
            CoverageEstablishedPayload.TYPE,
            new CoverageEstablishedPayload(p.id(), p.liaison().getInstitutionId(), c.taux(), c.numerateur(), c.denominateur(), p.servicesCouverts(), aujourdhui(), a.id()),
            p.liaison().getInstitutionId().toString()
        );
        return c;
    }

    /**
     * Service supplémentaire : nouvelle liaison au cadrage sur la même institution, sans repasser
     * par l'adhésion, rattachée à la liaison principale ; le conducteur est à désigner.
     */
    public Liaison ouvrirServiceSupplementaire(UUID liaisonId, List<String> services, Acteur a) {
        Parcours origine = charger(liaisonId);
        if (origine.etat().getStatut() != StatutParcours.EN_PRODUCTION) {
            throw HomologationException.conflict("HOM_LIAISON_NOT_IN_PRODUCTION", "Un service supplémentaire s'ouvre sur une liaison en production.", Map.of("statut", origine.etat().getStatut().name()));
        }
        List<String> codes = services == null ? List.of() : services.stream().map(s -> s == null ? "" : s.trim()).distinct().toList();
        if (codes.isEmpty() || codes.stream().anyMatch(c -> !CODE_SERVICE.matcher(c).matches() || CatalogueServices.TRANSVERSE.equals(c))) {
            throw HomologationException.unprocessable("HOM_SERVICES_INVALID", "Au moins un service, désigné par son code, est exigé.");
        }
        Liaison o = origine.liaison();
        Liaison n = new Liaison()
            .typeProjet(o.getTypeProjet())
            .institutionId(o.getInstitutionId())
            .institutionNom(o.getInstitutionNom())
            .etat(EtatLiaison.OUVERTE)
            .chefDeProjetId(o.getChefDeProjetId())
            .banquePartenaireId(o.getBanquePartenaireId())
            .dateOuverture(clock.instant())
            .cycleAptitude(0)
            .version(versions.findFirstByTypeProjetAndEtat(o.getTypeProjet(), EtatVersionReferentiel.PUBLIEE).orElse(o.getVersion()))
            .liaisonOrigineId(o.getId())
            // Même projet, intitulé repris (D-104) ; la référence d'origine reste celle de la liaison principale.
            .intituleProjet(o.getIntituleProjet())
            .definitionTypeProjet(o.getDefinitionTypeProjet());
        n = liaisons.saveAndFlush(n);
        ContractualScope scope = perimetres.save(new ContractualScope().dateDerivation(clock.instant()).sourceEventId("service-supplementaire:" + o.getId()).liaison(n));
        List<String> perimetre = new ArrayList<>();
        perimetre.add(CatalogueServices.TRANSVERSE);
        perimetre.addAll(codes);
        for (String code : perimetre) {
            elements.save(new ScopeItem().serviceCode(code).serviceLibelle(CatalogueServices.libelle(code)).etat(EtatScopeItem.A_ENGAGER).scope(scope));
        }
        n.setContractualScope(scope);
        instanciation.instancier(n, codes, origine.etat().isFintech(), o.getId(), true);
        LOG.info("Service supplémentaire {} ouvert sur la liaison {} par {} : liaison {}", codes, o.getId(), a.id(), n.getId());
        return n;
    }

    public void remettreFiche(UUID liaisonId, Acteur a) {
        charger(liaisonId).remettreFiche();
    }

    /** Constat de mise en service (fiche de suivi, responsable interne), sur la fiche renseignée par le participant. */
    public void constaterMiseEnService(UUID liaisonId, Acteur a) {
        Parcours p = charger(liaisonId);
        p.constaterMiseEnService(preuveInterne("CST", p, "constat-mise-en-service", p.id() + ":" + p.couvertureMiseEnService(), a), aujourdhui());
    }

    /** Conducteur désigné ou redésigné (étape 3) : il tient sa fonction sur les opérations non faites. */
    public void conducteurDesigne(UUID liaisonId, UUID conducteurId) {
        depot.trouver(liaisonId, false).ifPresent(p -> p.designerConducteur(new Personne(conducteurId.toString(), nomConducteur(conducteurId), Fonction.CONDUCTEUR)));
    }

    private String nomConducteur(UUID id) {
        String nom = intervenants.nom(id);
        return nom == null ? "Conducteur de projet" : nom;
    }

    // ------------------------------------------------------------------ portail

    /** Coordonnées techniques d'essai (geste COORDONNEES_TECHNIQUES), communiquées par le participant. */
    public void communiquerCoordonnees(UUID liaisonId, String adresse, String port, String pointsTerminaison, Acteur a) {
        Parcours p = sienne(charger(liaisonId), a);
        Preuve piece = preuveInterne("COORD", p, "fiche-coordonnees-techniques", adresse + ":" + port, a);
        p.communiquerCoordonnees(adresse, port, pointsTerminaison, piece, aujourdhui());
    }

    /** Livrable du participant (multipart) : déposé dans MS-DOCUMENT pour son compte, puis rattaché. */
    public void deposerLivrablePortail(UUID operationId, byte[] contenu, String nomFichier, String typeMime, String commentaire, Acteur a) {
        deposerLivrablePortail(operationId, null, contenu, nomFichier, typeMime, commentaire, a);
    }

    /** Livrable du participant désigné par son code (D-93 §3), type contrôlé avant le dépôt dans MS-DOCUMENT. */
    public void deposerLivrablePortail(UUID operationId, String livrable, byte[] contenu, String nomFichier, String typeMime, String commentaire, Acteur a) {
        Parcours p = sienne(depot.deLOperation(operationId), a);
        OperationParcours o = p.operation(operationId);
        if (!ParcoursProjection.livrableParticipant(o) || !o.getEtat().ouverte()) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Cette opération n'attend pas de livrable du participant.", Map.of("etat", o.getEtat().name(), "rang", o.getRang()));
        }
        if (contenu == null || contenu.length == 0 || nomFichier == null || nomFichier.isBlank()) {
            throw HomologationException.unprocessable("HOM_DOCUMENT_INVALID", "Le livrable doit porter un fichier non vide.");
        }
        Livrable l = Parcours.livrableVise(o, livrable);
        Parcours.verifierType(l, typeMime);
        DocumentRegistryPort.Depot d = documents.deposer(contenu, nomFichier, typeMime, "LIVRABLE", operationId.toString(), a.id());
        String par = a.nom() == null || a.nom().isBlank() ? Personne.correspondant(p.liaison().getInstitutionId().toString(), p.liaison().getInstitutionNom()).nom() : a.nom();
        p.deposerLivrable(o, l, Preuve.piece(d.id().toString(), d.nomFichier(), d.sha256(), aujourdhui(), par), typeMime, a.id() == null ? null : a.id().toString(), false, aujourdhui());
        LOG.info("Livrable du participant déposé sur l'opération {} de la liaison {} ({})", o.getRang(), p.id(), commentaire == null ? "" : commentaire.length() + " caractères de commentaire");
    }

    /** Fiche de suivi renseignée par le participant (geste FICHE_SUIVI, responsable externe). */
    public void renseignerFiche(UUID liaisonId, List<Parcours.LigneFiche> lignes, Acteur a) {
        Parcours p = sienne(charger(liaisonId), a);
        p.renseignerFiche(lignes == null ? List.of() : lignes, preuveInterne("FSS", p, "fiche-suivi-services", p.id() + ":" + lignes, a), aujourdhui());
    }
}
