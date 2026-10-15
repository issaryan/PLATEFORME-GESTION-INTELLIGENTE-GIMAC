package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.repository.BlocageParcoursRepository;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.OperationParcoursRepository;
import cm.gimac.des.homologation.repository.ParcoursLiaisonRepository;
import cm.gimac.des.homologation.repository.ServiceParcoursRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Chargement et enregistrement de l'agrégat {@link Parcours} : liaison (verrouillée pour une écriture),
 * état du parcours, opérations, services, blocages et taux repris du simulateur (table coverage, HOM-12).
 */
@Component
public class ParcoursDepot {

    private final LiaisonRepository liaisons;
    private final ParcoursLiaisonRepository parcours;
    private final OperationParcoursRepository operations;
    private final ServiceParcoursRepository services;
    private final BlocageParcoursRepository blocages;
    private final CoverageRepository couvertures;
    private final EntityManager em;
    private SuiviTachesParcours suivi;

    public ParcoursDepot(
        LiaisonRepository liaisons,
        ParcoursLiaisonRepository parcours,
        OperationParcoursRepository operations,
        ServiceParcoursRepository services,
        BlocageParcoursRepository blocages,
        CoverageRepository couvertures,
        EntityManager em
    ) {
        this.liaisons = liaisons;
        this.parcours = parcours;
        this.operations = operations;
        this.services = services;
        this.blocages = blocages;
        this.couvertures = couvertures;
        this.em = em;
    }

    /** Suivi des tâches de MS-PILOTAGE (D-77, §5) : chaque parcours chargé pour une écriture est observé. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSuivi(SuiviTachesParcours suivi) {
        this.suivi = suivi;
    }

    /** Parcours d'une liaison ; {@code verrou} sérialise les écritures concurrentes sur la liaison. */
    public Optional<Parcours> trouver(UUID liaisonId, boolean verrou) {
        Optional<Liaison> l = verrou ? liaisons.findByIdForUpdate(liaisonId) : liaisons.findById(liaisonId);
        if (l.isEmpty()) {
            return Optional.empty();
        }
        Optional<Parcours> p = parcours.findById(liaisonId).map(e -> assembler(l.orElseThrow(), e));
        if (suivi != null) {
            p.ifPresent(suivi::observer);
        }
        return p;
    }

    public Liaison liaison(UUID liaisonId) {
        return liaisons.findById(liaisonId).orElseThrow(() -> introuvable(liaisonId));
    }

    public static HomologationException introuvable(Object id) {
        return HomologationException.notFound("HOM_LIAISON_NOT_FOUND", "Liaison introuvable.", id);
    }

    /**
     * Parcours de l'opération désignée, verrouillé : la liaison est lue sans charger l'opération, pour que
     * celle-ci le soit sous le verrou, dans son dernier état validé.
     */
    public Parcours deLOperation(UUID operationId) {
        UUID liaison = operations.liaisonDe(operationId).orElseThrow(() -> HomologationException.notFound("HOM_OPERATION_NOT_FOUND", operationId));
        return trouver(liaison, true).orElseThrow(() -> introuvable(liaison));
    }

    /** Parcours de l'opération désignée, en lecture seule (D-109 : pièces et chronologie), sans verrou. */
    public Parcours deLOperationLecture(UUID operationId) {
        UUID liaison = operations.liaisonDe(operationId).orElseThrow(() -> HomologationException.notFound("HOM_OPERATION_NOT_FOUND", operationId));
        return trouver(liaison, false).orElseThrow(() -> introuvable(liaison));
    }

    /** Parcours du blocage désigné, verrouillé (même précaution). */
    public Parcours duBlocage(UUID blocageId) {
        UUID liaison = blocages.liaisonDe(blocageId).orElseThrow(() -> HomologationException.notFound("HOM_BLOCKAGE_NOT_FOUND", "Blocage introuvable.", blocageId));
        return trouver(liaison, true).orElseThrow(() -> introuvable(liaison));
    }

    /** Tous les parcours (tableaux de bord, pilotage) : quatre lectures groupées. */
    public List<Parcours> tous() {
        List<ParcoursLiaison> etats = parcours.findAll();
        return assemblerTous(etats);
    }

    /** Parcours des liaisons d'une institution (portail, cloisonné par le jeton). */
    public List<Parcours> deLInstitution(String institutionId) {
        UUID id;
        try {
            id = UUID.fromString(institutionId);
        } catch (IllegalArgumentException | NullPointerException e) {
            return List.of();
        }
        List<UUID> ids = liaisons.findByInstitutionId(id).stream().map(Liaison::getId).toList();
        return assemblerTous(parcours.findAllById(ids));
    }

    private List<Parcours> assemblerTous(List<ParcoursLiaison> etats) {
        if (etats.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = etats.stream().map(ParcoursLiaison::getLiaisonId).toList();
        Map<UUID, Liaison> ls = liaisons.findAllById(ids).stream().collect(Collectors.toMap(Liaison::getId, Function.identity()));
        Map<UUID, List<OperationParcours>> ops = operations.findByLiaisonIdIn(ids).stream().collect(Collectors.groupingBy(OperationParcours::getLiaisonId));
        Map<UUID, List<ServiceParcours>> svc = services.findByLiaisonIdIn(ids).stream().collect(Collectors.groupingBy(ServiceParcours::getLiaisonId));
        Map<UUID, List<BlocageParcours>> blq = blocages.findByLiaisonIdIn(ids).stream().collect(Collectors.groupingBy(BlocageParcours::getLiaisonId));
        Map<UUID, Map<String, Integer>> mesures = mesures(ids);
        return etats
            .stream()
            .filter(e -> ls.containsKey(e.getLiaisonId()))
            .map(e ->
                new Parcours(ls.get(e.getLiaisonId()), e, ops.getOrDefault(e.getLiaisonId(), List.of()), svc.getOrDefault(e.getLiaisonId(), List.of()), blq.getOrDefault(e.getLiaisonId(), List.of())).avecMesures(
                    mesures.getOrDefault(e.getLiaisonId(), Map.of())
                )
            )
            .toList();
    }

    private Parcours assembler(Liaison l, ParcoursLiaison e) {
        UUID id = l.getId();
        return new Parcours(l, e, operations.findByLiaisonIdOrderByOrdre(id), services.findByLiaisonIdOrderByServiceCode(id), blocages.findByLiaisonId(id)).avecMesures(
            mesures(List.of(id)).getOrDefault(id, Map.of())
        );
    }

    /** Taux de certification repris du simulateur, par liaison et par service (table coverage, HOM-12). */
    private Map<UUID, Map<String, Integer>> mesures(Collection<UUID> ids) {
        Map<UUID, Map<String, Integer>> m = new HashMap<>();
        for (Coverage c : couvertures.findByLiaison_IdInAndNature(ids, NatureCouverture.CERTIFICATION)) {
            if (Boolean.TRUE.equals(c.getCalculable()) && c.getTaux() != null) {
                m.computeIfAbsent(c.getLiaison().getId(), k -> new HashMap<>()).put(c.getServiceCode(), c.getTaux().intValue());
            }
        }
        return m;
    }

    /** Premier enregistrement d'un parcours instancié. */
    public void creer(Parcours p) {
        em.persist(p.etat());
        p.operations().forEach(em::persist);
        p.services().forEach(em::persist);
        if (suivi != null) {
            suivi.observerNouveau(p);
        }
    }

    /** Opération ajoutée par une migration vers une nouvelle version du référentiel. */
    public void ajouter(OperationParcours o) {
        em.persist(o);
    }

    /** Opération retirée par une migration, jamais commencée. */
    public void supprimer(OperationParcours o) {
        em.remove(o);
    }

    /** Enregistre un blocage nouvellement déclaré ; le reste de l'agrégat est suivi par le contexte de persistance. */
    public void ajouter(BlocageParcours b) {
        em.persist(b);
    }

    public long prochainNumero() {
        return parcours.prochainNumero();
    }

    public boolean existe(UUID liaisonId) {
        return parcours.existsById(liaisonId);
    }
}
