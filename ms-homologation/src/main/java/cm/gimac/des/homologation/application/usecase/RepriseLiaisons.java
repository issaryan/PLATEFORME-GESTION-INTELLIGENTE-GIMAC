package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.application.usecase.RepriseLiaisonsValidation.LiaisonControlee;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.DefinitionTypeProjet;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.parcours.AvancementParcours;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.repository.DefinitionTypeProjetRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reprise des projets de certification existants (D-107, docs/exploitation/reprise-des-donnees.md §4). Une transaction
 * par liaison ; en simulation, chaque liaison est jouée jusqu'à l'écriture en base puis annulée. Une liaison reprise
 * s'ouvre comme une liaison saisie (version publiée du référentiel de son type, périmètre contractualisé, parcours
 * instancié, événement homologation.liaison-opened.v1), puis reçoit l'avancement de sa source ; le moteur recalcule
 * statut, étape courante et automate technique, et les événements du parcours partent par l'outbox à la validation.
 * Une liaison déjà reprise (même référence d'origine) est mise à jour sans réécrire ce qui a été fait sur la plateforme.
 */
@Service
public class RepriseLiaisons implements RepriseLiaisonsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RepriseLiaisons.class);
    static final String AGREGAT = "liaison";

    private final LiaisonRepository liaisons;
    private final ParticipantDirectoryPort participants;
    private final CollaboratorDirectoryPort collaborateurs;
    private final LiaisonOpeningPolicy politique;
    private final ReferentialVersionRepository versions;
    private final DefinitionTypeProjetRepository typesProjet;
    private final ContractualScopeRepository perimetres;
    private final ScopeItemRepository elements;
    private final ParcoursInstanciation instanciation;
    private final ParcoursDepot depot;
    private final Intervenants intervenants;
    private final OutboxPublisher outbox;
    private final EntityManager em;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private cm.gimac.des.homologation.application.usecase.sandbox.PublicationEtatsLiaison publication;
    private PorteursPermission porteurs;
    private CoverageRepository couvertures;

    public RepriseLiaisons(
        LiaisonRepository liaisons,
        ParticipantDirectoryPort participants,
        CollaboratorDirectoryPort collaborateurs,
        LiaisonOpeningPolicy politique,
        ReferentialVersionRepository versions,
        DefinitionTypeProjetRepository typesProjet,
        ContractualScopeRepository perimetres,
        ScopeItemRepository elements,
        ParcoursInstanciation instanciation,
        ParcoursDepot depot,
        Intervenants intervenants,
        OutboxPublisher outbox,
        EntityManager em,
        PlatformTransactionManager transactions,
        Clock clock
    ) {
        this.liaisons = liaisons;
        this.participants = participants;
        this.collaborateurs = collaborateurs;
        this.politique = politique;
        this.versions = versions;
        this.typesProjet = typesProjet;
        this.perimetres = perimetres;
        this.elements = elements;
        this.instanciation = instanciation;
        this.depot = depot;
        this.intervenants = intervenants;
        this.outbox = outbox;
        this.em = em;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /** Mesures des essais (table coverage, D-110) ; absent : les essais repris ne sont pas enregistrés. */
    @Autowired(required = false)
    public void setCouvertures(CoverageRepository couvertures) {
        this.couvertures = couvertures;
    }

    /** Publication des états de la liaison pour la Sandbox GIMAC (US-EXI-16). */
    @Autowired(required = false)
    public void setPublication(cm.gimac.des.homologation.application.usecase.sandbox.PublicationEtatsLiaison publication) {
        this.publication = publication;
    }

    /** Lecteur des porteurs d'une permission (HOM_PROJECT_CONDUCT du conducteur, D-94 §3) ; absent : admis. */
    @Autowired(required = false)
    public void setPorteurs(PorteursPermission porteurs) {
        this.porteurs = porteurs;
    }

    @Override
    public Rapport importer(List<LiaisonReprise> lignes, boolean simulation, Acteur acteur) {
        if (lignes == null || lignes.isEmpty()) {
            throw HomologationException.unprocessable("HOM_REPRISE_VIDE", "Aucune liaison à reprendre");
        }
        if (lignes.size() > LIGNES_MAX) {
            throw HomologationException.unprocessableDetails("HOM_REPRISE_TROP_DE_LIGNES", "Lot trop volumineux", Map.of("max", LIGNES_MAX, "recues", lignes.size()));
        }
        if (acteur == null || acteur.id() == null) {
            throw HomologationException.forbidden("HOM_USER_TOKEN_REQUIRED", "Jeton identifiant le compte de reprise exigé");
        }
        Set<String> vues = new HashSet<>();
        List<RepriseLiaisonsUseCase.Ligne> rapport = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            LiaisonReprise l = lignes.get(i);
            String ref = l == null ? null : RepriseLiaisonsValidation.texte(l.referenceOrigine());
            if (l == null) {
                rapport.add(refus(i, null, "HOM_REPRISE_LIGNE_INVALIDE", "Ligne vide", Map.of()));
            } else if (ref != null && !vues.add(ref)) {
                rapport.add(refus(i, ref, "HOM_REPRISE_DOUBLON_FICHIER", "Référence d'origine déjà présente plus haut dans le fichier", Map.of("referenceOrigine", ref)));
            } else {
                rapport.add(executer(i, l, simulation, acteur));
            }
        }
        Map<Resultat, Long> totaux = new EnumMap<>(Resultat.class);
        for (Resultat r : Resultat.values()) {
            totaux.put(r, rapport.stream().filter(x -> x.resultat() == r).count());
        }
        LOG.info("Reprise des liaisons ({}) par {} : {}", simulation ? "simulation" : "réelle", acteur.id(), totaux);
        return new Rapport(simulation, lignes.size(), totaux, rapport);
    }

    private RepriseLiaisonsUseCase.Ligne executer(int rang, LiaisonReprise l, boolean simulation, Acteur acteur) {
        try {
            return transaction.execute(statut -> {
                RepriseLiaisonsUseCase.Ligne r = traiter(rang, l, acteur);
                em.flush();
                if (simulation) {
                    statut.setRollbackOnly();
                }
                return r;
            });
        } catch (HomologationException e) {
            return refus(rang, l.referenceOrigine(), e.code(), e.getMessage(), e.details());
        } catch (DataAccessException | PersistenceException | jakarta.validation.ConstraintViolationException e) {
            LOG.warn("Reprise de la liaison {} refusée par la base : {}", l.referenceOrigine(), e.getClass().getSimpleName());
            return refus(rang, l.referenceOrigine(), "HOM_REPRISE_ECRITURE_REFUSEE", "Écriture refusée par la base de données", Map.of("cause", e.getClass().getSimpleName()));
        }
    }

    private RepriseLiaisonsUseCase.Ligne traiter(int rang, LiaisonReprise source, Acteur acteur) {
        LiaisonControlee l = RepriseLiaisonsValidation.valider(source, aujourdhui());
        InstitutionView institution = participants
            .parCodeMembre(l.codeMembre())
            .orElseThrow(() -> HomologationException.conflict("HOM_LIAISON_INSTITUTION_UNKNOWN", "Institution inconnue du registre (à reprendre d'abord)", Map.of("codeMembre", l.codeMembre())));
        politique.verifierReprise(institution);
        if (l.conducteurId() != null) {
            politique.verifierConducteur(collaborateurs.findCollaborator(l.conducteurId()).orElse(null), porteConduite(l.conducteurId()));
        }
        Liaison existante = liaisons.findByReferenceOrigine(l.referenceOrigine()).orElse(null);
        return existante == null ? creer(rang, l, institution, acteur) : mettreAJour(rang, l, existante, institution, acteur);
    }

    private RepriseLiaisonsUseCase.Ligne creer(int rang, LiaisonControlee l, InstitutionView institution, Acteur acteur) {
        DefinitionTypeProjet definition = typesProjet
            .findById(l.typeProjet().name())
            .orElseThrow(() -> HomologationException.notFound("HOM_TYPE_PROJET_INTROUVABLE", "Type de projet inconnu.", l.typeProjet().name()));
        ReferentialVersion version = versions
            .findFirstByTypeProjetAndEtat(l.typeProjet(), EtatVersionReferentiel.PUBLIEE)
            .orElseThrow(() -> HomologationException.conflict("HOM_REFERENTIAL_NONE_PUBLISHED", "Aucune version publiée du référentiel pour ce type de projet", Map.of("typeProjet", l.typeProjet().name())));
        InstitutionView banque = banque(l, institution);
        Instant maintenant = clock.instant();
        UUID conducteur = l.conducteurId() != null ? l.conducteurId() : acteur.id();
        Liaison liaison = liaisons.saveAndFlush(
            new Liaison()
                .typeProjet(l.typeProjet())
                .institutionId(institution.id())
                .institutionNom(institution.nom())
                .etat(EtatLiaison.OUVERTE)
                .chefDeProjetId(conducteur)
                .banquePartenaireId(banque == null ? null : banque.id())
                .dateOuverture(l.dateOuverture() == null ? maintenant : l.dateOuverture().atStartOfDay(ZoneOffset.UTC).toInstant())
                .cycleAptitude(0)
                .version(version)
                .definitionTypeProjet(definition)
                .intituleProjet(l.intitule())
                .referenceOrigine(l.referenceOrigine())
        );
        liaison.setOrigineEtat(l.origineEtat());
        liaison.setRepriseLe(maintenant);
        List<String> perimetre = perimetre(liaison, l, institution, maintenant);
        Parcours p = instanciation.instancier(liaison, perimetre, OpenLiaisonService.estFintech(institution), null, false);
        if (l.conducteurId() == null) {
            // Conducteur inconnu de la source : la liaison attend sa désignation par un chef de département.
            p.retirerCompte(acteur.id().toString());
        }
        l.ressources().forEach((f, compte) -> p.affecterFonction(f, intervenants.pourFonction(compte.toString(), f)));
        appliquerServices(p, l, new ArrayList<>());
        mesurerEssais(liaison, l.essais(), maintenant);
        RepriseParcours.Bilan bilan = avancer(p, l, acteur);
        LiaisonOpenedPayload ouverte = new LiaisonOpenedPayload(liaison.getId(), liaison.getInstitutionId(), liaison.getTypeProjet().name(), version.getId(), version.getNumero(), liaison.getChefDeProjetId(), liaison.getBanquePartenaireId(), perimetre, acteur.id(), liaison.modeleTypeProjet().name());
        if (publication != null) {
            // US-EXI-16 : code membre de la ligne reprise et version d'état 1, puis homologation.liaison-status-changed.v1.
            publication.ouverture(liaison, institution.codeParticipant() != null ? institution.codeParticipant() : l.codeMembre(), ouverte);
        } else {
            outbox.publish(AGREGAT, liaison.getId(), LiaisonOpenedPayload.TYPE, ouverte);
        }
        List<String> avertissements = new ArrayList<>(bilan.avertissements());
        if (l.conducteurId() == null) {
            avertissements.add("Conducteur à désigner par un chef de département");
        }
        return ligne(rang, Resultat.CREEE, p, bilan.modifiees(), avertissements);
    }

    private RepriseLiaisonsUseCase.Ligne mettreAJour(int rang, LiaisonControlee l, Liaison existante, InstitutionView institution, Acteur acteur) {
        if (!existante.getInstitutionId().equals(institution.id()) || !existante.getTypeProjet().equals(l.typeProjet())) {
            throw HomologationException.conflict(
                "HOM_REPRISE_LIAISON_DIVERGENTE",
                "Cette référence d'origine est portée par une liaison d'une autre institution ou d'un autre type de projet",
                Map.of("referenceOrigine", l.referenceOrigine(), "liaisonId", existante.getId().toString())
            );
        }
        Parcours p = depot.trouver(existante.getId(), true).orElseThrow(() -> ParcoursDepot.introuvable(existante.getId()));
        Liaison liaison = p.liaison();
        List<String> changements = new ArrayList<>();
        changer(liaison.getIntituleProjet(), l.intitule(), liaison::setIntituleProjet, "intitule", changements);
        changer(liaison.getOrigineEtat(), l.origineEtat(), liaison::setOrigineEtat, "origineEtat", changements);
        if (l.conducteurId() != null && (!l.conducteurId().equals(liaison.getChefDeProjetId()) || p.etat().isConducteurADesigner())) {
            liaison.setChefDeProjetId(l.conducteurId());
            p.designerConducteur(new Personne(l.conducteurId().toString(), nomOuDefaut(l.conducteurId()), Fonction.CONDUCTEUR));
            changements.add("conducteur");
        }
        l.ressources().forEach((f, compte) -> {
            Personne actuelle = p.etat().getRessources().get(f);
            if (actuelle == null || !compte.toString().equals(actuelle.id())) {
                p.affecterFonction(f, intervenants.pourFonction(compte.toString(), f));
                changements.add("ressources");
            }
        });
        if (mesurerEssais(liaison, l.essais(), clock.instant())) {
            changements.add("essais");
        }
        List<String> avertissements = new ArrayList<>();
        appliquerServices(p, l, avertissements);
        int avant = blocages(p).size();
        RepriseParcours.Bilan bilan = avancer(p, l, acteur);
        avertissements.addAll(bilan.avertissements());
        if (changements.isEmpty() && bilan.modifiees() == 0 && blocages(p).size() == avant) {
            return ligne(rang, Resultat.INCHANGEE, p, 0, avertissements);
        }
        liaison.setRepriseLe(clock.instant());
        return ligne(rang, Resultat.MISE_A_JOUR, p, bilan.modifiees(), avertissements);
    }

    /**
     * Essais mesurés sur la sandbox (D-110) : mesure globale de la liaison (nature CERTIFICATION, code
     * {@link AvancementParcours#ESSAIS_GLOBAL}), créée ou mise à jour ; rend vrai si elle a changé.
     */
    boolean mesurerEssais(Liaison liaison, RepriseLiaisonsUseCase.EssaisRepris essais, Instant maintenant) {
        if (essais == null || couvertures == null) {
            return false;
        }
        Coverage c = couvertures
            .findFirstByLiaison_IdAndNatureAndServiceCode(liaison.getId(), NatureCouverture.CERTIFICATION, AvancementParcours.ESSAIS_GLOBAL)
            .orElseGet(() -> new Coverage().serviceCode(AvancementParcours.ESSAIS_GLOBAL).nature(NatureCouverture.CERTIFICATION).liaison(liaison));
        BigDecimal taux = BigDecimal.valueOf(Math.round(essais.reussis() * 100.0 / essais.attribues()));
        if (essais.reussis().equals(c.getNumerateur()) && essais.attribues().equals(c.getDenominateur()) && Boolean.TRUE.equals(c.getCalculable())) {
            return false;
        }
        Instant le = essais.mesureLe() == null ? maintenant : essais.mesureLe().atStartOfDay(ZoneOffset.UTC).toInstant();
        couvertures.save(c.numerateur(essais.reussis()).denominateur(essais.attribues()).taux(taux).calculable(true).dateCalcul(le));
        return true;
    }

    /** Avancement repris, puis enregistrement des blocages déclarés par la reprise. */
    private RepriseParcours.Bilan avancer(Parcours p, LiaisonControlee l, Acteur acteur) {
        Set<BlocageParcours> avant = blocages(p);
        RepriseParcours.Bilan bilan = RepriseParcours.appliquer(
            p,
            l.avancement(),
            l.operations(),
            l.dateMiseEnProduction(),
            l.dateCibleProduction(),
            l.origineEtat(),
            acteur.nom() == null ? acteur.id().toString() : acteur.nom(),
            acteur.id(),
            aujourdhui()
        );
        p.blocages().stream().filter(b -> !avant.contains(b)).forEach(depot::ajouter);
        return bilan;
    }

    private static Set<BlocageParcours> blocages(Parcours p) {
        Set<BlocageParcours> s = Collections.newSetFromMap(new IdentityHashMap<>());
        s.addAll(p.blocages());
        return s;
    }

    /**
     * Services repris : date de certification, ouverture et commentaire d'un service du périmètre ; un service hors
     * périmètre d'une liaison déjà reprise est signalé, jamais ajouté (le périmètre se modifie sur la plateforme).
     */
    private static void appliquerServices(Parcours p, LiaisonControlee l, List<String> avertissements) {
        for (RepriseLiaisonsUseCase.ServiceRepris s : l.services()) {
            ServiceParcours sp = p.services().stream().filter(x -> x.getServiceCode().equals(s.code())).findFirst().orElse(null);
            if (sp == null) {
                avertissements.add("Service " + s.code() + " hors du périmètre de la liaison : non ajouté par la reprise");
                continue;
            }
            if (s.certifieLe() != null) {
                sp.setCertifieLe(s.certifieLe());
            }
            if (s.ouvert() != null) {
                sp.setOuvert(s.ouvert());
            }
            if (s.commentaire() != null) {
                sp.setCommentaire(s.commentaire());
            }
        }
    }

    /** Périmètre contractualisé : services repris, sinon services souscrits ou catalogue du type (comme à l'ouverture). */
    private List<String> perimetre(Liaison liaison, LiaisonControlee l, InstitutionView institution, Instant maintenant) {
        List<String> codes = l.services().isEmpty()
            ? OpenLiaisonService.perimetre(liaison.modeleTypeProjet(), institution)
            : l.services().stream().map(RepriseLiaisonsUseCase.ServiceRepris::code).toList();
        if (!codes.contains(CatalogueServices.TRANSVERSE)) {
            List<String> avec = new ArrayList<>();
            avec.add(CatalogueServices.TRANSVERSE);
            avec.addAll(codes);
            codes = avec;
        }
        ContractualScope scope = perimetres.save(new ContractualScope().dateDerivation(maintenant).sourceEventId("reprise:" + liaison.getId()).liaison(liaison));
        for (String code : codes) {
            elements.save(new ScopeItem().serviceCode(code).serviceLibelle(CatalogueServices.libelle(code)).etat(EtatScopeItem.A_ENGAGER).scope(scope));
        }
        liaison.setContractualScope(scope);
        return codes;
    }

    private InstitutionView banque(LiaisonControlee l, InstitutionView institution) {
        if (l.banquePartenaire() == null) {
            return null;
        }
        InstitutionView b = participants
            .parCodeMembre(l.banquePartenaire())
            .orElseThrow(() -> HomologationException.conflict("HOM_LIAISON_INSTITUTION_UNKNOWN", "Banque partenaire inconnue du registre", Map.of("codeMembre", l.banquePartenaire())));
        if (b.id().equals(institution.id())) {
            throw HomologationException.conflict("HOM_LIAISON_CARRIER_INVALID", "Une institution ne se porte pas elle-même", Map.of());
        }
        if (!"BANQUE".equals(b.categorie())) {
            throw HomologationException.conflict("HOM_LIAISON_CARRIER_NOT_BANK", "Le porteur désigné n'est pas une banque", Map.of("banquePartenaire", l.banquePartenaire()));
        }
        return b;
    }

    private boolean porteConduite(UUID compte) {
        return porteurs == null || porteurs.porte(compte, LiaisonOpeningPolicy.PERMISSION_CONDUITE);
    }

    private String nomOuDefaut(UUID id) {
        String nom = intervenants.nom(id);
        return nom == null ? "Conducteur de projet" : nom;
    }

    private static <T> void changer(T actuel, T nouveau, Consumer<T> appliquer, String champ, List<String> changements) {
        if (nouveau != null && !Objects.equals(nouveau, actuel)) {
            appliquer.accept(nouveau);
            changements.add(champ);
        }
    }

    private static RepriseLiaisonsUseCase.Ligne ligne(int rang, Resultat r, Parcours p, int modifiees, List<String> avertissements) {
        Liaison l = p.liaison();
        return new RepriseLiaisonsUseCase.Ligne(
            rang,
            l.getReferenceOrigine(),
            r,
            l.getId(),
            p.etat().getReference(),
            p.etat().getStatut().name(),
            p.etat().getEtapeCourante().name(),
            l.getEtat().name(),
            modifiees,
            List.copyOf(avertissements),
            null,
            null,
            Map.of()
        );
    }

    private static RepriseLiaisonsUseCase.Ligne refus(int rang, String reference, String code, String motif, Map<String, Object> details) {
        return new RepriseLiaisonsUseCase.Ligne(rang, reference, Resultat.REFUSEE, null, null, null, null, null, 0, List.of(), code, motif, details == null ? Map.of() : details);
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(clock.withZone(java.time.ZoneId.of("Africa/Douala")));
    }
}
