package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
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
 * Instanciation du parcours d'une liaison à son ouverture (étape 3) depuis la version publiée du référentiel à
 * laquelle elle est rattachée (moteur unique, D-77) : opérations de la version, qualités initiales par fonction,
 * périmètre suivi service par service, référence lisible LIA-AAAA-NNN. Les liaisons ouvertes avant cette tranche
 * sont instanciées à la première lecture.
 */
@Component
public class ParcoursInstanciation {

    private final ReferentialVersionRepository versions;
    private final ScopeItemRepository elements;
    private final ParcoursDepot depot;
    private final Intervenants intervenants;
    private final Clock clock;

    public ParcoursInstanciation(ReferentialVersionRepository versions, ScopeItemRepository elements, ParcoursDepot depot, Intervenants intervenants, Clock clock) {
        this.versions = versions;
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
        ReferentialVersion version = version(l);
        etat.rattacher(version.getId(), version.getNumero());
        Parcours p = Parcours.instancier(l, etat, version.getOperations(), svc, designation(l, Map.of()));
        depot.creer(p);
        return p;
    }

    /** Version de la liaison ; à défaut (liaison antérieure au rattachement), la version publiée de son type. */
    ReferentialVersion version(Liaison l) {
        if (l.getVersion() != null && !l.getVersion().getOperations().isEmpty()) {
            return l.getVersion();
        }
        ReferentialVersion v = versions
            .findFirstByTypeProjetAndEtat(l.getTypeProjet(), EtatVersionReferentiel.PUBLIEE)
            .orElseThrow(() ->
                HomologationException.conflict("HOM_REFERENTIAL_NONE_PUBLISHED", "Aucune version publiée du référentiel pour ce type de projet", Map.of("typeProjet", l.getTypeProjet().name()))
            );
        l.setVersion(v);
        return v;
    }

    /**
     * Personne qui tient chaque fonction : ressources déjà affectées, conducteur de la liaison, correspondant du
     * participant, tiers désignés par leur fonction, sinon le premier titulaire de l'annuaire.
     */
    public Parcours.Designation designation(Liaison l, Map<Fonction, Personne> affectees) {
        Map<Fonction, Personne> cache = new EnumMap<>(Fonction.class);
        cache.putAll(affectees);
        Personne conducteur = new Personne(l.getChefDeProjetId().toString(), nomOuDefaut(l.getChefDeProjetId()), Fonction.CONDUCTEUR);
        return f ->
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
