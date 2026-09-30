package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.ReferentielOperation;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.repository.ReferentielOperationRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Instanciation du référentiel de l'annexe A pour une liaison, à son ouverture (étape 3) : rangs 3 et
 * suivants, branche du type de projet, qualités initiales par fonction, périmètre suivi service par service,
 * référence lisible LIA-AAAA-NNN. Les liaisons ouvertes avant cette tranche sont instanciées à la première lecture.
 */
@Component
public class ParcoursInstanciation {

    private final ReferentielOperationRepository referentiel;
    private final ScopeItemRepository elements;
    private final ParcoursDepot depot;
    private final Intervenants intervenants;
    private final Clock clock;

    public ParcoursInstanciation(ReferentielOperationRepository referentiel, ScopeItemRepository elements, ParcoursDepot depot, Intervenants intervenants, Clock clock) {
        this.referentiel = referentiel;
        this.elements = elements;
        this.depot = depot;
        this.intervenants = intervenants;
        this.clock = clock;
    }

    /**
     * Instancie le parcours d'une liaison nouvellement ouverte. {@code services} : codes du périmètre
     * contractualisé (le service TRANSVERSE, sans objet pour la certification, est écarté).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Parcours instancier(Liaison l, List<String> services, boolean fintech, UUID origine, boolean conducteurADesigner) {
        LocalDate aujourdhui = LocalDate.ofInstant(l.getDateOuverture() == null ? clock.instant() : l.getDateOuverture(), ZoneOffset.UTC);
        String reference = "LIA-" + aujourdhui.getYear() + "-" + "%03d".formatted(depot.prochainNumero());
        ParcoursLiaison etat = new ParcoursLiaison(l.getId(), reference, aujourdhui).fintech(fintech).liaisonOrigineId(origine);
        etat.setConducteurADesigner(conducteurADesigner);
        List<ServiceParcours> svc = services
            .stream()
            .filter(c -> c != null && !c.isBlank() && !CatalogueServices.TRANSVERSE.equals(c))
            .distinct()
            .map(c -> new ServiceParcours(l.getId(), c, CatalogueServices.libelle(c), true))
            .toList();
        Map<Fonction, Personne> cache = new EnumMap<>(Fonction.class);
        Personne conducteur = new Personne(l.getChefDeProjetId().toString(), nomOuDefaut(l.getChefDeProjetId()), Fonction.CONDUCTEUR);
        Parcours.Designation d = f ->
            cache.computeIfAbsent(f, x ->
                switch (x) {
                    case CONDUCTEUR -> conducteur;
                    case PARTICIPANT -> Personne.correspondant(l.getInstitutionId().toString(), l.getInstitutionNom());
                    case PERSONNALISATEUR -> new Personne("EXT-PERSONNALISATEUR", "Personnalisateur", Fonction.PERSONNALISATEUR);
                    case BANQUE_PARTENAIRE -> new Personne("EXT-BANQUE", "Banque partenaire", Fonction.BANQUE_PARTENAIRE);
                    case CHEFS_DEPARTEMENT -> intervenants.titulaire(Fonction.CHEF_DES);
                    default -> intervenants.titulaire(x);
                }
            );
        Parcours p = Parcours.instancier(l, etat, referentiel.findByVersionCrOrderByOrdre(ReferentielOperation.VERSION_CR), svc, d);
        depot.creer(p);
        return p;
    }

    /** Liaison ouverte avant cette tranche : instanciée depuis son périmètre contractualisé. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Parcours instancierExistante(Liaison l) {
        List<String> services = l.getContractualScope() == null
            ? List.of()
            : elements.findByScope_IdOrderByServiceCode(l.getContractualScope().getId()).stream().filter(s -> s.getEtat() != EtatScopeItem.HORS_PERIMETRE).map(ScopeItem::getServiceCode).toList();
        return instancier(l, services, false, l.getLiaisonOrigineId(), false);
    }

    private String nomOuDefaut(UUID id) {
        String nom = intervenants.nom(id);
        return nom == null ? "Conducteur de projet" : nom;
    }
}
