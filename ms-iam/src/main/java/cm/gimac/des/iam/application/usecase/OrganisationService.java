package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.CatalogueUnites;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.service.ReglesOrganisation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1) : catalogue des unités
 * organisationnelles, rattachement d'un compte (unité, responsable n+1), lecture du rattachement et
 * de l'équipe d'un compte pour « Mon activité » (MS-PILOTAGE). Chaque modification est journalisée.
 */
@Service
@Transactional
public class OrganisationService {

    /** Objet du journal de configuration pour le catalogue des unités. */
    static final String OBJET_UNITE = "UNITE_ORGANISATIONNELLE";
    static final String ACTION_RATTACHEMENT = "COMPTE_RATTACHEMENT_MODIFIE";

    /** Responsable d'un compte : identité et fonction, sans coordonnées. */
    public record Responsable(String id, String nomAffiche, String fonction) {}

    /**
     * Rattachement d'un compte (§1.3) : unité et son chemin, responsable, chaîne des responsables, équipe
     * directe, unités dirigées (D-81 §2 : unités dont il est le responsable, sous-unités comprises).
     */
    public record Rattachement(
        String id,
        String nomAffiche,
        Unite unite,
        List<Unite> chemin,
        Responsable responsable,
        List<String> chaine,
        int equipeDirecte,
        List<String> unitesDirigees
    ) {}

    /**
     * Périmètre d'un compte pour une portée (D-81 §2) : comptes couverts (niveau 1 pour ses comptes
     * directs, 2 pour les leurs, etc. ; 0 pour un membre d'une unité dirigée hors de sa chaîne) et unités
     * couvertes (unités dirigées pour la portée UNITES, aucune sinon).
     */
    public record Perimetre(Portee portee, List<MembreEquipe> comptes, List<String> unites) {}

    /** Membre de l'équipe d'un compte : niveau 1 pour ses comptes directs, 2 pour les leurs, etc. */
    public record MembreEquipe(String id, String nomAffiche, String fonction, String unite, String responsableId, int niveau, boolean actif) {}

    /**
     * Membre d'une unité pour le moteur de processus (D-86, D-87) : compte interne actif, ses profils
     * (codes), sans donnée de contact ni de sécurité.
     */
    public record MembreUnite(String id, String nomAffiche, String fonction, String unite, String responsableId, boolean actif, Set<String> profils) {}

    private final CatalogueUnites catalogue;
    private final Annuaire annuaire;
    private final JournalActes journal;

    public OrganisationService(CatalogueUnites catalogue, Annuaire annuaire, JournalActes journal) {
        this.catalogue = catalogue;
        this.annuaire = annuaire;
        this.journal = journal;
    }

    // ------------------------------------------------------------------ unités

    @Transactional(readOnly = true)
    public List<Unite> unites(boolean activesSeulement) {
        return catalogue.toutes().stream().filter(u -> !activesSeulement || u.active()).toList();
    }

    /**
     * Une unité par son code (D-87, docs/lot1/processus-unite.md §5.6), active ou non, avec son responsable :
     * le moteur de processus y lit le chef d'une unité, même sans membre. Unité inconnue : 404 ORG_UNITE_INTROUVABLE.
     */
    @Transactional(readOnly = true)
    public Unite unite(String code) {
        Unite u = parCode().get(code);
        if (u == null) {
            throw IamException.introuvable("ORG_UNITE_INTROUVABLE", code);
        }
        return u;
    }

    public Unite creerUnite(Acteur acteur, Unite demande) {
        Unite u = ReglesOrganisation.normaliser(demande);
        ReglesOrganisation.verifierResponsableUnite(u.responsableId(), annuaire::lire);
        Map<String, Unite> parCode = parCode();
        if (parCode.containsKey(u.code())) {
            throw IamException.conflit("ORG_UNITE_EXISTANTE", "Une unité porte déjà ce code", Map.of("code", u.code()));
        }
        ReglesOrganisation.verifierPlace(u, parCode);
        Unite enregistree = catalogue.enregistrer(u);
        journal.configuration(acteur, OBJET_UNITE, null, champs("CREATION", enregistree));
        return enregistree;
    }

    /** Modifie le libellé, le type, le parent, l'état et le responsable d'une unité ; son code ne change pas. */
    public Unite modifierUnite(Acteur acteur, String code, Unite demande) {
        return modifierUnite(acteur, code, demande, false);
    }

    /**
     * Modifie une unité ; avec {@code conserverResponsable}, son responsable actuel est gardé (requête qui
     * ne le mentionne pas). Un responsable nouveau doit être un compte interne actif (D-81 §2).
     */
    public Unite modifierUnite(Acteur acteur, String code, Unite demande, boolean conserverResponsable) {
        Map<String, Unite> parCode = parCode();
        Unite actuelle = parCode.get(code);
        if (actuelle == null) {
            throw IamException.introuvable("ORG_UNITE_INTROUVABLE", code);
        }
        String responsable = conserverResponsable ? actuelle.responsableId() : demande.responsableId();
        Unite u = ReglesOrganisation.normaliser(new Unite(code, demande.libelle(), demande.type(), demande.parent(), demande.active(), responsable));
        if (u.responsableId() != null && !u.responsableId().equals(actuelle.responsableId())) {
            ReglesOrganisation.verifierResponsableUnite(u.responsableId(), annuaire::lire);
        }
        ReglesOrganisation.verifierPlace(u, parCode);
        Unite enregistree = catalogue.enregistrer(u);
        journal.configuration(acteur, OBJET_UNITE, null, champs(u.active() ? "MODIFICATION" : "DESACTIVATION", enregistree));
        return enregistree;
    }

    private static List<String> champs(String acte, Unite u) {
        return List.of(acte, "code=" + u.code(), "type=" + u.type(), "parent=" + u.parent(), "active=" + u.active(), "responsable=" + u.responsableId());
    }

    private Map<String, Unite> parCode() {
        Map<String, Unite> m = new LinkedHashMap<>();
        catalogue.toutes().forEach(u -> m.put(u.code(), u));
        return m;
    }

    // ------------------------------------------------------------------ rattachement

    /**
     * Rattache un compte à une unité et à un responsable (§1.2) ; une valeur nulle retire le
     * rattachement correspondant. Journal COMPTE_RATTACHEMENT_MODIFIE (avant, après).
     */
    public Compte rattacher(Acteur acteur, String id, String unite, String responsableId) {
        return rattacher(acteur, id, unite, responsableId, null, null);
    }

    /**
     * Rattachement avec un motif facultatif et l'origine de l'acte (D-81 §3 : {@code "pouvoir=RATTACHEMENT_MODIFIER"}
     * quand un chef agit sur un subordonné), ajoutée aux détails du journal ; mêmes règles et même événement.
     */
    public Compte rattacher(Acteur acteur, String id, String unite, String responsableId, String motif, String origine) {
        Compte c = annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
        String u = vide(unite);
        String r = vide(responsableId);
        ReglesOrganisation.verifierRattachement(c, u, r, parCode(), annuaire::lire);
        annuaire.definirRattachement(id, u, r);
        List<String> details = new ArrayList<>(List.of("avant: " + etat(c.unite(), c.responsableId()), "après: " + etat(u, r)));
        if (origine != null) {
            details.add(origine);
        }
        journal.compte(acteur, ACTION_RATTACHEMENT, id, c.identifiant(), vide(motif), details);
        return annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
    }

    private static String etat(String unite, String responsableId) {
        return "unite=" + unite + " responsable=" + responsableId;
    }

    static String vide(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    // ------------------------------------------------------------------ lectures

    /** Rattachement d'un compte : unité, chemin, responsable, chaîne (10 au plus), nombre de comptes directs. */
    @Transactional(readOnly = true)
    public Rattachement rattachement(String id) {
        Compte c = annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
        Map<String, Unite> parCode = parCode();
        Responsable responsable = c.responsableId() == null
            ? null
            : annuaire.lire(c.responsableId()).map(x -> new Responsable(x.id(), x.nomAffiche(), x.fonction())).orElse(null);
        return new Rattachement(
            c.id(),
            c.nomAffiche(),
            c.unite() == null ? null : parCode.get(c.unite()),
            ReglesOrganisation.chemin(c.unite(), parCode),
            responsable,
            ReglesOrganisation.chaine(c, annuaire::lire),
            annuaire.parResponsable(c.id()).size(),
            ReglesOrganisation.unitesDirigees(c.id(), parCode.values())
        );
    }

    /** Unités dirigées par un compte, sous-unités comprises (D-81 §2). */
    @Transactional(readOnly = true)
    public List<String> unitesDirigees(String id) {
        return ReglesOrganisation.unitesDirigees(id, catalogue.toutes());
    }

    /**
     * Vérifie un rattachement proposé pour un compte interne à créer (D-81 §4) : unité connue et active,
     * responsable interne actif ; mêmes refus que {@link #rattacher}.
     */
    @Transactional(readOnly = true)
    public void verifierRattachementNouveau(String unite, String responsableId) {
        Compte nouveau = new Compte("nouveau-compte", "nouveau", null, null, null, false, true, null, null, null, null, null, false, null, null, false);
        ReglesOrganisation.verifierRattachement(nouveau, vide(unite), vide(responsableId), parCode(), annuaire::lire);
    }

    /**
     * Périmètre d'un compte pour une portée (D-81 §1, §2) : DIRECTS, ses comptes directs ; EQUIPE, son
     * équipe (chaîne des responsables) ; UNITES, son équipe et les membres de ses unités dirigées et de
     * leurs sous-unités (niveau 0 hors de la chaîne) ; AUCUNE, rien. Jamais le compte lui-même ; 500 comptes
     * au plus.
     */
    @Transactional(readOnly = true)
    public Perimetre perimetre(String id, Portee portee) {
        annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
        Portee p = portee == null ? Portee.AUCUNE : portee;
        return switch (p) {
            case AUCUNE -> new Perimetre(p, List.of(), List.of());
            case DIRECTS -> new Perimetre(
                p,
                annuaire.parResponsable(id).stream()
                    .filter(c -> !c.id().equals(id))
                    .map(c -> membre(c, 1))
                    .sorted(Comparator.comparing((MembreEquipe m) -> String.valueOf(m.nomAffiche()).toLowerCase(Locale.ROOT)).thenComparing(MembreEquipe::id))
                    .limit(ReglesOrganisation.EQUIPE_MAX)
                    .toList(),
                List.of()
            );
            case EQUIPE -> new Perimetre(p, equipe(id), List.of());
            case UNITES -> perimetreUnites(id);
        };
    }

    private Perimetre perimetreUnites(String id) {
        List<String> unites = unitesDirigees(id);
        List<MembreEquipe> comptes = new ArrayList<>(equipe(id));
        Set<String> vus = new HashSet<>(Set.of(id));
        comptes.forEach(m -> vus.add(m.id()));
        List<MembreEquipe> horsChaine = new ArrayList<>();
        for (String unite : unites) {
            for (Compte c : annuaire.parUnite(unite)) {
                if (vus.add(c.id())) {
                    horsChaine.add(membre(c, 0));
                }
            }
        }
        horsChaine.sort(Comparator.comparing((MembreEquipe m) -> String.valueOf(m.nomAffiche()).toLowerCase(Locale.ROOT)).thenComparing(MembreEquipe::id));
        comptes.addAll(horsChaine);
        return new Perimetre(Portee.UNITES, comptes.size() > ReglesOrganisation.EQUIPE_MAX ? List.copyOf(comptes.subList(0, ReglesOrganisation.EQUIPE_MAX)) : List.copyOf(comptes), unites);
    }

    private static MembreEquipe membre(Compte c, int niveau) {
        return new MembreEquipe(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), niveau, c.actif());
    }

    /**
     * Membres d'une unité (D-86, docs/lot1/processus-unite.md) : comptes internes actifs rattachés à l'unité
     * (et, avec {@code sousUnites}, à ses sous-unités), portant le profil demandé s'il l'est ; triés par nom,
     * 500 au plus. Unité inconnue : 404 ORG_UNITE_INTROUVABLE.
     */
    @Transactional(readOnly = true)
    public List<MembreUnite> membresUnite(String code, String profil, boolean sousUnites) {
        Map<String, Unite> parCode = parCode();
        if (!parCode.containsKey(code)) {
            throw IamException.introuvable("ORG_UNITE_INTROUVABLE", code);
        }
        List<String> unites = sousUnites ? ReglesOrganisation.sousUnites(code, parCode.values()) : List.of(code);
        String p = vide(profil);
        Map<String, Set<String>> profils = annuaire.profilsParCompte();
        Map<String, MembreUnite> membres = new LinkedHashMap<>();
        for (String unite : unites) {
            for (Compte c : annuaire.parUnite(unite)) {
                Set<String> siens = profils.getOrDefault(c.id(), Set.of());
                if (c.actif() && c.interne() && (p == null || siens.contains(p))) {
                    membres.putIfAbsent(c.id(), new MembreUnite(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), c.actif(), new java.util.TreeSet<>(siens)));
                }
            }
        }
        return membres.values().stream()
            .sorted(Comparator.comparing((MembreUnite m) -> String.valueOf(m.nomAffiche()).toLowerCase(Locale.ROOT)).thenComparing(MembreUnite::id))
            .limit(ReglesOrganisation.EQUIPE_MAX)
            .toList();
    }

    /**
     * Équipe d'un compte : comptes dont il figure dans la chaîne des responsables, parcourus niveau par
     * niveau (recherche par attribut responsable_id), sans cycle, 500 au plus ; triés par niveau puis nom.
     */
    @Transactional(readOnly = true)
    public List<MembreEquipe> equipe(String id) {
        annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
        List<MembreEquipe> membres = new ArrayList<>();
        Set<String> vus = new HashSet<>(Set.of(id));
        List<String> niveau = List.of(id);
        for (int n = 1; !niveau.isEmpty() && membres.size() < ReglesOrganisation.EQUIPE_MAX; n++) {
            List<String> suivant = new ArrayList<>();
            for (String responsable : niveau) {
                for (Compte c : annuaire.parResponsable(responsable)) {
                    if (membres.size() < ReglesOrganisation.EQUIPE_MAX && vus.add(c.id())) {
                        membres.add(new MembreEquipe(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), n, c.actif()));
                        suivant.add(c.id());
                    }
                }
            }
            niveau = suivant;
        }
        return membres.stream()
            .sorted(Comparator.comparingInt(MembreEquipe::niveau).thenComparing((MembreEquipe m) -> String.valueOf(m.nomAffiche()).toLowerCase(Locale.ROOT)).thenComparing(MembreEquipe::id))
            .toList();
    }
}
