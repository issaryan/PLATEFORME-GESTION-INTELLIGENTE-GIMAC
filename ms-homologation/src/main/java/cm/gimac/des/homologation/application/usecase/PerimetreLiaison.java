package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.PerimetreModification;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.event.LiaisonScopeChangedPayload;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.PerimetreModificationRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import cm.gimac.des.homologation.repository.ServiceParcoursRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Périmètre du projet de certification (D-133). Le périmètre est dérivé des services souscrits à l'ouverture
 * (HOM-05), puis modifiable par trois acteurs seulement : le gestionnaire du projet, c'est-à-dire l'agent du GIMAC
 * qui a créé la liaison (HOM_PERIMETRE_EDIT et sujet du jeton égal au gestionnaire), un super administrateur ou un
 * chargé de l'implémentation des projets de la SNA (HOM_PERIMETRE_EDIT_TOUS). Aucun compte d'institution, ni son
 * administrateur ni son intégrateur, ne le modifie.
 *
 * <p>Un service ajouté doit être souscrit (RG-72) ; un service retiré passe hors périmètre sans qu'aucune opération
 * ni preuve ne soit supprimée (critère 3 de HOM-05) ; TRANSVERSE reste toujours inclus. Chaque modification est
 * journalisée (version, auteur, permission utilisée, avant, après, motif, horodatage) et publie
 * homologation.liaison-scope-changed.v1 dans la même transaction.
 */
@Service
@Transactional
public class PerimetreLiaison {

    public static final String EDIT = "HOM_PERIMETRE_EDIT";
    public static final String EDIT_TOUS = "HOM_PERIMETRE_EDIT_TOUS";
    static final String AGREGAT = "liaison";
    static final int MOTIF_MAX = 500;
    private static final Set<EtatLiaison> FERMEES = EnumSet.of(EtatLiaison.CLOSE, EtatLiaison.CERTIFIEE);

    /** Service du périmètre : code, libellé, état et inclusion. */
    public record Element(String code, String libelle, EtatScopeItem etat, boolean inclus) {}

    /**
     * Périmètre servi à l'écran du DES : version, gestionnaire, services, services proposables (souscrits, TRANSVERSE
     * compris ; vide si le registre est injoignable) et droit de modification de l'acteur.
     */
    public record Vue(UUID liaisonId, int version, UUID gestionnaireId, String gestionnaireNom, List<Element> services, List<String> proposables, boolean modifiable) {}

    private final LiaisonRepository liaisons;
    private final ContractualScopeRepository perimetres;
    private final ScopeItemRepository elements;
    private final ServiceParcoursRepository servicesParcours;
    private final PerimetreModificationRepository journal;
    private final ParticipantDirectoryPort participants;
    private final Intervenants intervenants;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public PerimetreLiaison(
        LiaisonRepository liaisons,
        ContractualScopeRepository perimetres,
        ScopeItemRepository elements,
        ServiceParcoursRepository servicesParcours,
        PerimetreModificationRepository journal,
        ParticipantDirectoryPort participants,
        Intervenants intervenants,
        OutboxPublisher outbox,
        Clock clock
    ) {
        this.liaisons = liaisons;
        this.perimetres = perimetres;
        this.elements = elements;
        this.servicesParcours = servicesParcours;
        this.journal = journal;
        this.participants = participants;
        this.intervenants = intervenants;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Vue lire(UUID liaisonId, Acteur acteur) {
        Liaison l = liaisons.findById(liaisonId).orElseThrow(() -> ParcoursDepot.introuvable(liaisonId));
        return vue(l, acteur);
    }

    @Transactional(readOnly = true)
    public Page<PerimetreModification> historique(UUID liaisonId, Pageable page) {
        if (!liaisons.existsById(liaisonId)) {
            throw ParcoursDepot.introuvable(liaisonId);
        }
        return journal.findByLiaisonIdOrderByVersionPerimetreDesc(liaisonId, page);
    }

    /** Modification du périmètre ; un périmètre inchangé ne crée ni version ni événement. */
    public Vue modifier(UUID liaisonId, List<String> services, String motif, Acteur acteur) {
        Liaison l = liaisons.findByIdForUpdate(liaisonId).orElseThrow(() -> ParcoursDepot.introuvable(liaisonId));
        String permission = autorisation(l, acteur);
        String m = motif == null ? "" : motif.trim();
        if (m.isEmpty() || m.length() > MOTIF_MAX) {
            throw HomologationException.unprocessable("HOM_PERIMETRE_MOTIF_REQUIS", "Le motif de la modification est obligatoire (500 caractères au plus).");
        }
        if (FERMEES.contains(l.getEtat())) {
            throw HomologationException.conflict("HOM_PERIMETRE_LIAISON_CLOSE", "Le périmètre d'un projet clos ou certifié n'est plus modifiable.", Map.of("etat", l.getEtat().name()));
        }
        Set<String> apres = new TreeSet<>();
        apres.add(CatalogueServices.TRANSVERSE);
        (services == null ? List.<String>of() : services).stream().filter(s -> s != null && !s.isBlank()).map(String::trim).forEach(apres::add);
        if (apres.size() == 1) {
            throw HomologationException.unprocessable("HOM_PERIMETRE_VIDE", "Le périmètre porte au moins un service à certifier.");
        }
        ContractualScope scope = scope(l);
        List<ScopeItem> items = elements.findByScope_IdOrderByServiceCode(scope.getId());
        Set<String> avant = inclus(items);
        if (avant.equals(apres)) {
            return vue(l, acteur);
        }
        InstitutionView institution = participants.getInstitution(l.getInstitutionId());
        List<String> ajoutes = apres.stream().filter(s -> !avant.contains(s)).toList();
        List<String> proposables = OpenLiaisonService.perimetre(l.modeleTypeProjet(), institution);
        List<String> nonSouscrits = ajoutes.stream().filter(s -> !proposables.contains(s)).toList();
        if (!nonSouscrits.isEmpty()) {
            throw HomologationException.unprocessableDetails(
                "HOM_PERIMETRE_SERVICE_NON_SOUSCRIT",
                "Un service non souscrit ne peut entrer au périmètre (RG-72).",
                Map.of("services", nonSouscrits)
            );
        }
        List<String> retires = avant.stream().filter(s -> !apres.contains(s)).toList();
        appliquer(l, scope, items, apres);
        Instant t = clock.instant();
        int version = version(l.getId()) + 1;
        journal.save(new PerimetreModification(l.getId(), version, acteur.id(), acteur.nom(), permission, String.join(",", avant), String.join(",", apres), m, t));
        outbox.publish(
            AGREGAT,
            l.getId(),
            LiaisonScopeChangedPayload.TYPE,
            new LiaisonScopeChangedPayload(l.getId(), l.getInstitutionId(), institution.codeParticipant(), l.getTypeProjet().name(), List.copyOf(apres), ajoutes, retires, version, acteur.id(), permission, m, t),
            l.getInstitutionId().toString()
        );
        return vue(l, acteur);
    }

    /**
     * Permission qui autorise l'acteur à modifier le périmètre : HOM_PERIMETRE_EDIT_TOUS sur tout projet,
     * HOM_PERIMETRE_EDIT sur le seul projet dont il est le gestionnaire. Un compte d'institution est toujours refusé.
     */
    static String autorisation(Liaison l, Acteur a) {
        String p = permission(l, a);
        if (p == null) {
            throw HomologationException.forbidden("HOM_PERIMETRE_GESTIONNAIRE_SEUL", "Seuls le gestionnaire du projet, un super administrateur ou un chargé de l'implémentation des projets de la SNA modifient le périmètre.");
        }
        return p;
    }

    private static String permission(Liaison l, Acteur a) {
        if (a == null || a.id() == null || (a.institutionId() != null && !a.institutionId().isBlank())) {
            return null;
        }
        if (a.peut(EDIT_TOUS)) {
            return EDIT_TOUS;
        }
        return a.peut(EDIT) && a.id().equals(l.getGestionnaireId()) ? EDIT : null;
    }

    private ContractualScope scope(Liaison l) {
        if (l.getContractualScope() != null) {
            return l.getContractualScope();
        }
        ContractualScope p = perimetres.save(new ContractualScope().dateDerivation(clock.instant()).sourceEventId("perimetre:" + l.getId()).liaison(l));
        l.setContractualScope(p);
        return p;
    }

    private void appliquer(Liaison l, ContractualScope scope, List<ScopeItem> items, Set<String> apres) {
        Set<String> connus = new LinkedHashSet<>();
        for (ScopeItem i : items) {
            connus.add(i.getServiceCode());
            boolean garder = apres.contains(i.getServiceCode());
            if (garder && i.getEtat() == EtatScopeItem.HORS_PERIMETRE) {
                elements.save(i.etat(EtatScopeItem.A_ENGAGER));
            } else if (!garder && i.getEtat() != EtatScopeItem.HORS_PERIMETRE) {
                elements.save(i.etat(EtatScopeItem.HORS_PERIMETRE));
            }
        }
        apres.stream().filter(c -> !connus.contains(c)).forEach(c ->
            elements.save(new ScopeItem().serviceCode(c).serviceLibelle(CatalogueServices.libelle(c)).etat(EtatScopeItem.A_ENGAGER).scope(scope))
        );
        // Parcours instancié : l'inclusion de ses services suit le périmètre (couvertures, livrables au grain du service).
        List<ServiceParcours> suivis = servicesParcours.findByLiaisonIdOrderByServiceCode(l.getId());
        if (suivis.isEmpty()) {
            return;
        }
        Set<String> suivisCodes = new LinkedHashSet<>();
        for (ServiceParcours s : suivis) {
            suivisCodes.add(s.getServiceCode());
            if (s.isInclus() != apres.contains(s.getServiceCode())) {
                s.setInclus(apres.contains(s.getServiceCode()));
                servicesParcours.save(s);
            }
        }
        apres.stream().filter(c -> !suivisCodes.contains(c)).forEach(c -> servicesParcours.save(new ServiceParcours(l.getId(), c, CatalogueServices.libelle(c), true)));
    }

    private Vue vue(Liaison l, Acteur acteur) {
        List<ScopeItem> items = l.getContractualScope() == null ? List.of() : elements.findByScope_IdOrderByServiceCode(l.getContractualScope().getId());
        List<Element> services = items.stream().map(i -> new Element(i.getServiceCode(), CatalogueServices.libelle(i.getServiceCode()), i.getEtat(), i.getEtat() != EtatScopeItem.HORS_PERIMETRE)).toList();
        boolean modifiable = permission(l, acteur) != null && !FERMEES.contains(l.getEtat());
        List<String> proposables = modifiable ? proposables(l) : List.of();
        String nom = l.getGestionnaireId() == null ? null : intervenants.nom(l.getGestionnaireId());
        return new Vue(l.getId(), version(l.getId()), l.getGestionnaireId(), nom, services, proposables, modifiable);
    }

    /** Services proposables : souscrits et TRANSVERSE ; registre injoignable : aucun (l'ajout sera contrôlé). */
    private List<String> proposables(Liaison l) {
        try {
            return new ArrayList<>(OpenLiaisonService.perimetre(l.modeleTypeProjet(), participants.getInstitution(l.getInstitutionId())));
        } catch (HomologationException e) {
            return List.of();
        }
    }

    private int version(UUID liaisonId) {
        return journal.findFirstByLiaisonIdOrderByVersionPerimetreDesc(liaisonId).map(PerimetreModification::getVersionPerimetre).orElse(1);
    }

    private static Set<String> inclus(List<ScopeItem> items) {
        Set<String> s = new TreeSet<>();
        items.stream().filter(i -> i.getEtat() != EtatScopeItem.HORS_PERIMETRE).map(ScopeItem::getServiceCode).forEach(s::add);
        return s;
    }
}
