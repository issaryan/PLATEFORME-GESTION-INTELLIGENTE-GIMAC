package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Liaison_;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectures des liaisons pour les écrans DES-08 et DES-09 : liste filtrée (dont « mes projets » du
 * conducteur, D-58), fiche avec intervenants et actions permises, conducteurs désignables.
 */
@Service
@Transactional(readOnly = true)
public class LiaisonQueries {

    public static final String PERMISSION_DESIGNATION = "HOM_PROJECT_DESIGNATE";

    /**
     * Filtres de la liste : ceux de l'étape 3 et ceux du modèle hybride (D-57), lus sur l'état du parcours :
     * statut du CR, étapes courantes, liaisons bloquées, conducteur à désigner.
     */
    public record Filtre(
        EtatLiaison etat,
        TypeProjet typeProjet,
        UUID institutionId,
        String recherche,
        UUID conducteurId,
        StatutParcours statut,
        List<EtapeCode> etapes,
        boolean bloquees,
        boolean sansConducteur
    ) {
        public Filtre(EtatLiaison etat, TypeProjet typeProjet, UUID institutionId, String recherche, UUID conducteurId) {
            this(etat, typeProjet, institutionId, recherche, conducteurId, null, List.of(), false, false);
        }
    }

    public record Intervenant(String role, String id, String nom) {}

    /** Fiche d'une liaison ; {@code parcours} résume son parcours (étapes 3 à 10), absent pour une liaison non instanciée. */
    public record Fiche(
        Liaison liaison,
        String chefDeProjetNom,
        String banquePartenaireNom,
        List<String> perimetre,
        List<Intervenant> intervenants,
        List<String> actionsPermises,
        Vues.LiaisonResume parcours
    ) {
        public Fiche(Liaison liaison, String chefDeProjetNom, String banquePartenaireNom, List<String> perimetre, List<Intervenant> intervenants, List<String> actionsPermises) {
            this(liaison, chefDeProjetNom, banquePartenaireNom, perimetre, intervenants, actionsPermises, null);
        }
    }

    private final LiaisonRepository liaisons;
    private final ScopeItemRepository elements;
    private final CollaboratorDirectoryPort collaborateurs;
    private final ParticipantDirectoryPort participants;
    private final ParcoursLectures parcours;

    public LiaisonQueries(LiaisonRepository liaisons, ScopeItemRepository elements, CollaboratorDirectoryPort collaborateurs, ParticipantDirectoryPort participants, ParcoursLectures parcours) {
        this.liaisons = liaisons;
        this.elements = elements;
        this.collaborateurs = collaborateurs;
        this.participants = participants;
        this.parcours = parcours;
    }

    public Page<Fiche> lister(Filtre f, int page, int taille, Set<String> permissions) {
        Page<Liaison> p = liaisons.findAll(specification(f), PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, 100), Sort.by(Sort.Direction.DESC, Liaison_.DATE_OUVERTURE)));
        Map<UUID, String> noms = new HashMap<>();
        p.getContent().forEach(l -> noms.computeIfAbsent(l.getChefDeProjetId(), this::nomCollaborateur));
        Map<UUID, Vues.LiaisonResume> resumes = parcours.resumes(p.getContent(), noms);
        return p.map(l -> avecParcours(fiche(l, noms, permissions), resumes.get(l.getId())));
    }

    public Fiche fiche(UUID id, Set<String> permissions) {
        Liaison l = liaisons.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_LIAISON_NOT_FOUND", id));
        Map<UUID, String> noms = new HashMap<>();
        Fiche f = fiche(l, noms, permissions);
        return avecParcours(f, parcours.resumes(List.of(l), noms).get(l.getId()));
    }

    /** Portail : fiche sans nom interne (le conducteur n'apparaît que par sa fonction) ni action du DES. */
    public Fiche fichePortail(Liaison l, Vues.LiaisonResume resume) {
        List<String> perimetre = l.getContractualScope() == null
            ? List.of()
            : elements.findByScope_IdOrderByServiceCode(l.getContractualScope().getId()).stream().map(ScopeItem::getServiceCode).toList();
        return new Fiche(l, "Conducteur de projet GIMAC", null, perimetre, List.of(new Intervenant("INSTITUTION", l.getInstitutionId().toString(), l.getInstitutionNom())), List.of(), resume);
    }

    private static Fiche avecParcours(Fiche f, Vues.LiaisonResume r) {
        return r == null ? f : new Fiche(f.liaison(), f.chefDeProjetNom(), f.banquePartenaireNom(), f.perimetre(), f.intervenants(), f.actionsPermises(), r);
    }

    /** Collaborateurs désignables comme conducteur de projet (rôle CONDUCTEUR_PROJET). */
    public List<CollaboratorView> conducteurs() {
        return collaborateurs.parProfil(LiaisonOpeningPolicy.PROFIL_CONDUCTEUR);
    }

    private Fiche fiche(Liaison l, Map<UUID, String> noms, Set<String> permissions) {
        String conducteur = noms.computeIfAbsent(l.getChefDeProjetId(), this::nomCollaborateur);
        String banque = l.getBanquePartenaireId() == null ? null : noms.computeIfAbsent(l.getBanquePartenaireId(), this::nomInstitution);
        List<String> perimetre = l.getContractualScope() == null
            ? List.of()
            : elements.findByScope_IdOrderByServiceCode(l.getContractualScope().getId()).stream().map(ScopeItem::getServiceCode).toList();
        List<Intervenant> intervenants = new ArrayList<>();
        intervenants.add(new Intervenant("INSTITUTION", l.getInstitutionId().toString(), l.getInstitutionNom()));
        if (l.getBanquePartenaireId() != null) {
            intervenants.add(new Intervenant("BANQUE_PARTENAIRE", l.getBanquePartenaireId().toString(), banque));
        }
        intervenants.add(new Intervenant("CHEF_DE_PROJET", l.getChefDeProjetId().toString(), conducteur));
        List<String> actions = new ArrayList<>();
        if (l.getEtat() != EtatLiaison.CLOSE && permissions.contains(PERMISSION_DESIGNATION)) {
            actions.add("DESIGNER_CHEF_DE_PROJET");
        }
        return new Fiche(l, conducteur, banque, perimetre, intervenants, actions);
    }

    /** Nom affiché ; null si l'annuaire est momentanément injoignable (la fiche reste lisible). */
    private String nomCollaborateur(UUID id) {
        try {
            return collaborateurs.findCollaborator(id).map(CollaboratorView::nomAffiche).orElse(null);
        } catch (HomologationException e) {
            return null;
        }
    }

    private String nomInstitution(UUID id) {
        try {
            return participants.getInstitution(id).nom();
        } catch (HomologationException e) {
            return null;
        }
    }

    static Specification<Liaison> specification(Filtre f) {
        return (racine, requete, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.etat() != null) p.add(cb.equal(racine.get(Liaison_.ETAT), f.etat()));
            if (f.typeProjet() != null) p.add(cb.equal(racine.get(Liaison_.TYPE_PROJET), f.typeProjet()));
            if (f.institutionId() != null) p.add(cb.equal(racine.get(Liaison_.INSTITUTION_ID), f.institutionId()));
            if (f.conducteurId() != null) p.add(cb.equal(racine.get(Liaison_.CHEF_DE_PROJET_ID), f.conducteurId()));
            if (f.recherche() != null && !f.recherche().isBlank()) {
                p.add(cb.like(cb.lower(racine.get(Liaison_.INSTITUTION_NOM)), "%" + f.recherche().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (f.statut() != null || (f.etapes() != null && !f.etapes().isEmpty()) || f.bloquees() || f.sansConducteur()) {
                Subquery<UUID> sq = requete.subquery(UUID.class);
                Root<ParcoursLiaison> pl = sq.from(ParcoursLiaison.class);
                List<Predicate> q = new ArrayList<>();
                if (f.statut() != null) q.add(cb.equal(pl.get("statut"), f.statut()));
                if (f.etapes() != null && !f.etapes().isEmpty()) q.add(pl.get("etapeCourante").in(f.etapes()));
                if (f.bloquees()) q.add(cb.isTrue(pl.get("bloquee")));
                if (f.sansConducteur()) q.add(cb.isTrue(pl.get("conducteurADesigner")));
                sq.select(pl.get("liaisonId")).where(q.toArray(Predicate[]::new));
                p.add(racine.get(Liaison_.ID).in(sq));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
