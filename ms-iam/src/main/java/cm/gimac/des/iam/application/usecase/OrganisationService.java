package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.CatalogueUnites;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.organisation.Unite;
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

    /** Rattachement d'un compte (§1.3) : unité et son chemin, responsable, chaîne des responsables, équipe directe. */
    public record Rattachement(String id, String nomAffiche, Unite unite, List<Unite> chemin, Responsable responsable, List<String> chaine, int equipeDirecte) {}

    /** Membre de l'équipe d'un compte : niveau 1 pour ses comptes directs, 2 pour les leurs, etc. */
    public record MembreEquipe(String id, String nomAffiche, String fonction, String unite, String responsableId, int niveau, boolean actif) {}

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

    public Unite creerUnite(Acteur acteur, Unite demande) {
        Unite u = ReglesOrganisation.normaliser(demande);
        Map<String, Unite> parCode = parCode();
        if (parCode.containsKey(u.code())) {
            throw IamException.conflit("ORG_UNITE_EXISTANTE", "Une unité porte déjà ce code", Map.of("code", u.code()));
        }
        ReglesOrganisation.verifierPlace(u, parCode);
        Unite enregistree = catalogue.enregistrer(u);
        journal.configuration(acteur, OBJET_UNITE, null, champs("CREATION", enregistree));
        return enregistree;
    }

    /** Modifie le libellé, le type, le parent ou l'état d'une unité ; son code ne change pas. */
    public Unite modifierUnite(Acteur acteur, String code, Unite demande) {
        Map<String, Unite> parCode = parCode();
        if (!parCode.containsKey(code)) {
            throw IamException.introuvable("ORG_UNITE_INTROUVABLE", code);
        }
        Unite u = ReglesOrganisation.normaliser(new Unite(code, demande.libelle(), demande.type(), demande.parent(), demande.active()));
        ReglesOrganisation.verifierPlace(u, parCode);
        Unite enregistree = catalogue.enregistrer(u);
        journal.configuration(acteur, OBJET_UNITE, null, champs(u.active() ? "MODIFICATION" : "DESACTIVATION", enregistree));
        return enregistree;
    }

    private static List<String> champs(String acte, Unite u) {
        return List.of(acte, "code=" + u.code(), "type=" + u.type(), "parent=" + u.parent(), "active=" + u.active());
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
        Compte c = annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
        String u = vide(unite);
        String r = vide(responsableId);
        ReglesOrganisation.verifierRattachement(c, u, r, parCode(), annuaire::lire);
        annuaire.definirRattachement(id, u, r);
        journal.compte(acteur, ACTION_RATTACHEMENT, id, c.identifiant(), null, List.of("avant: " + etat(c.unite(), c.responsableId()), "après: " + etat(u, r)));
        return annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
    }

    private static String etat(String unite, String responsableId) {
        return "unite=" + unite + " responsable=" + responsableId;
    }

    private static String vide(String v) {
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
            annuaire.parResponsable(c.id()).size()
        );
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
