package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.event.ProjectLeadDesignatedPayload;
import cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ouverture d'une liaison au cadrage (HOM-04 adapté par D-57 et D-58) : sur un adhérent ou une
 * institution déjà en homologation, liée à la version publiée en vigueur du référentiel, avec son
 * périmètre contractualisé dérivé des services souscrits (HOM-05, service TRANSVERSE toujours
 * inclus) et le conducteur de projet désigné par un chef de département.
 * L'événement homologation.liaison-opened.v1 fait passer l'adhérent à EN_HOMOLOGATION dans MS-PARTICIPANT.
 */
@Service
@Transactional
public class OpenLiaisonService implements OpenLiaisonUseCase {

    static final String TRANSVERSE = "TRANSVERSE";
    static final String AGREGAT = "liaison";

    private final ParticipantDirectoryPort participants;
    private final CollaboratorDirectoryPort collaborateurs;
    private final LiaisonOpeningPolicy politique;
    private final LiaisonRepository liaisons;
    private final ReferentialVersionRepository versions;
    private final ContractualScopeRepository perimetres;
    private final ScopeItemRepository elements;
    private final OutboxPublisher outbox;
    private final Clock clock;
    private final ParcoursInstanciation parcours;
    private final ParcoursCommandes conduite;

    public OpenLiaisonService(
        ParticipantDirectoryPort participants,
        CollaboratorDirectoryPort collaborateurs,
        LiaisonOpeningPolicy politique,
        LiaisonRepository liaisons,
        ReferentialVersionRepository versions,
        ContractualScopeRepository perimetres,
        ScopeItemRepository elements,
        OutboxPublisher outbox,
        Clock clock,
        ParcoursInstanciation parcours,
        ParcoursCommandes conduite
    ) {
        this.participants = participants;
        this.collaborateurs = collaborateurs;
        this.politique = politique;
        this.liaisons = liaisons;
        this.versions = versions;
        this.perimetres = perimetres;
        this.elements = elements;
        this.outbox = outbox;
        this.clock = clock;
        this.parcours = parcours;
        this.conduite = conduite;
    }

    @Override
    public Liaison ouvrir(OpenLiaisonCommand cmd, Acteur acteur) {
        if (!acteur.peutDesigner()) {
            // Le conducteur est désigné par un chef de département (rang 6, D-58).
            throw HomologationException.forbidden("HOM_PROJECT_DESIGNATION_FORBIDDEN", "La désignation du conducteur de projet revient à un chef de département");
        }
        InstitutionView institution = participants.getInstitution(cmd.institutionId());
        InstitutionView porteur = cmd.banquePartenaireId() == null ? null : participants.getInstitution(cmd.banquePartenaireId());
        politique.verifier(institution, porteur, collaborateurs.findCollaborator(cmd.chefDeProjetId()).orElse(null));
        if (liaisons.existsByInstitutionIdAndTypeProjetAndEtatNotAndLiaisonOrigineIdIsNull(cmd.institutionId(), cmd.typeProjet(), EtatLiaison.CLOSE)) {
            throw doublon(cmd);
        }
        ReferentialVersion enVigueur = versions
            .findFirstByTypeProjetAndEtat(cmd.typeProjet(), EtatVersionReferentiel.PUBLIEE)
            .orElseThrow(() ->
                HomologationException.conflict("HOM_REFERENTIAL_NONE_PUBLISHED", "Aucune version publiée du référentiel pour ce type de projet", Map.of("typeProjet", cmd.typeProjet().name()))
            );
        Instant maintenant = clock.instant();
        Liaison l = new Liaison()
            .typeProjet(cmd.typeProjet())
            .institutionId(institution.id())
            .institutionNom(institution.nom())
            .etat(EtatLiaison.OUVERTE)
            .chefDeProjetId(cmd.chefDeProjetId())
            .banquePartenaireId(cmd.banquePartenaireId())
            .dateOuverture(maintenant)
            .cycleAptitude(0)
            .version(enVigueur);
        try {
            l = liaisons.saveAndFlush(l);
        } catch (DataIntegrityViolationException e) {
            // Ouverture concurrente : l'index partiel uk_liaison_active tranche.
            throw doublon(cmd);
        }
        List<String> perimetre = deriverPerimetre(l, institution, maintenant);
        // Étapes 3 à 10 : référentiel de l'annexe A instancié pour la liaison (D-59).
        parcours.instancier(l, perimetre, estFintech(institution), null, false);
        outbox.publish(
            AGREGAT,
            l.getId(),
            LiaisonOpenedPayload.TYPE,
            new LiaisonOpenedPayload(l.getId(), l.getInstitutionId(), l.getTypeProjet().name(), enVigueur.getId(), enVigueur.getNumero(), l.getChefDeProjetId(), l.getBanquePartenaireId(), perimetre, acteur.id())
        );
        return l;
    }

    @Override
    public Liaison designerChefDeProjet(UUID liaisonId, UUID chefDeProjetId, String motif, Acteur acteur) {
        if (!acteur.peutDesigner()) {
            throw HomologationException.forbidden("HOM_PROJECT_DESIGNATION_FORBIDDEN", "La désignation du conducteur de projet revient à un chef de département");
        }
        politique.verifierConducteur(collaborateurs.findCollaborator(chefDeProjetId).orElse(null));
        Liaison l = liaisons.findByIdForUpdate(liaisonId).orElseThrow(() -> HomologationException.notFound("HOM_LIAISON_NOT_FOUND", liaisonId));
        if (l.getEtat() == EtatLiaison.CLOSE) {
            throw HomologationException.conflict("HOM_LIAISON_CLOSED", "La liaison est close", Map.of("etat", l.getEtat().name()));
        }
        UUID ancien = l.getChefDeProjetId();
        // Le conducteur tient désormais sa fonction sur les opérations non faites (étapes 4 à 10).
        conduite.conducteurDesigne(liaisonId, chefDeProjetId);
        if (ancien.equals(chefDeProjetId)) {
            return l;
        }
        l.setChefDeProjetId(chefDeProjetId);
        outbox.publish(
            AGREGAT,
            l.getId(),
            ProjectLeadDesignatedPayload.TYPE,
            new ProjectLeadDesignatedPayload(l.getId(), l.getInstitutionId(), ancien, chefDeProjetId, acteur.id(), motif)
        );
        return l;
    }

    /** Participant fintech : conformité COBAC exigée (rang 22, D-56). */
    static boolean estFintech(InstitutionView i) {
        return i.categorie() != null && i.categorie().toUpperCase(java.util.Locale.ROOT).contains("FINTECH");
    }

    /**
     * Périmètre contractualisé (HOM-05) : services souscrits actifs, plus le service TRANSVERSE. Sans service souscrit
     * enregistré au registre, le périmètre est le catalogue du type de projet, arrêté ensuite au rang 3 (règle du
     * simulateur de l'interface) : une liaison n'est jamais ouverte sans service à certifier.
     */
    static List<String> perimetre(cm.gimac.des.homologation.domain.enumeration.TypeProjet type, InstitutionView institution) {
        List<String> codes = new ArrayList<>();
        codes.add(TRANSVERSE);
        institution.servicesSouscrits().stream().map(InstitutionView.ServiceSouscrit::code).filter(c -> c != null && !c.isBlank() && !TRANSVERSE.equals(c)).distinct().forEach(codes::add);
        if (codes.size() == 1) {
            codes.addAll(cm.gimac.des.homologation.domain.parcours.CatalogueServices.parType(type));
        }
        return codes;
    }

    private List<String> deriverPerimetre(Liaison l, InstitutionView institution, Instant maintenant) {
        ContractualScope p = perimetres.save(new ContractualScope().dateDerivation(maintenant).sourceEventId("ouverture:" + l.getId()).liaison(l));
        List<String> codes = perimetre(l.getTypeProjet(), institution);
        for (String code : codes) {
            elements.save(new ScopeItem().serviceCode(code).serviceLibelle(code).etat(EtatScopeItem.A_ENGAGER).scope(p));
        }
        l.setContractualScope(p);
        return codes;
    }

    private static HomologationException doublon(OpenLiaisonCommand cmd) {
        return HomologationException.conflict(
            "HOM_LIAISON_DUPLICATE",
            "Une liaison active existe déjà pour cette institution et ce type de projet",
            Map.of("institutionId", cmd.institutionId().toString(), "typeProjet", cmd.typeProjet().name())
        );
    }
}
