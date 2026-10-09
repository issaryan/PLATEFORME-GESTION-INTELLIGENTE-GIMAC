package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.CatalogueUnites;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.service.ReglesOrganisation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Affectation explicite de personnes à une unité de l'organigramme (D-102, docs/lot1/organigramme.md §2) : une
 * direction, un département, un service, un pôle ou une cellule reçoit de 1 à 50 comptes internes actifs en un
 * seul geste motivé ; le n+1 de chacun est celui demandé ou, à défaut, le chef de l'unité (ou de l'unité parente la
 * plus proche, voir {@link ReglesOrganisation#responsableParDefaut}). Tout est contrôlé avant la première
 * modification : une seule personne refusée et rien n'est fait (422 ORG_AFFECTATION_REFUSEE, détail par compte).
 * Chaque personne affectée passe par {@link OrganisationService#rattacher} : journal COMPTE_RATTACHEMENT_MODIFIE avec
 * le motif et événement iam.account-administered.v1, une fois par personne.
 *
 * <p>Administration (IAM_ACCOUNT_EDIT) : toute unité active, toute personne interne active. Chef (D-81, sans
 * IAM_ACCOUNT_EDIT) : mêmes contrôles que le rattachement d'un subordonné (action RATTACHEMENT_MODIFIER), unité cible
 * parmi les unités qu'il dirige, sinon 403 ORG_HORS_PERIMETRE.
 */
@Service
@Transactional
public class AffectationsService {

    /** Nombre de personnes affectées en un geste. */
    public static final int AFFECTATION_MAX = 50;
    static final int MOTIF_MIN = 3;
    static final int MOTIF_MAX = 500;
    static final String PERMISSION_ADMINISTRATION = "IAM_ACCOUNT_EDIT";

    /** Rattachement d'une personne : unité et n+1. */
    public record Etat(String unite, String responsableId) {}

    /** Effet du geste sur une personne : rattachement avant et après. */
    public record Resultat(String id, String nomAffiche, Etat avant, Etat apres) {}

    private final CatalogueUnites catalogue;
    private final Annuaire annuaire;
    private final OrganisationService organisation;
    private final PouvoirsService pouvoirs;

    public AffectationsService(CatalogueUnites catalogue, Annuaire annuaire, OrganisationService organisation, PouvoirsService pouvoirs) {
        this.catalogue = catalogue;
        this.annuaire = annuaire;
        this.organisation = organisation;
        this.pouvoirs = pouvoirs;
    }

    /**
     * Affecte des personnes à l'unité {@code code}.
     *
     * @param responsableFourni vrai quand la requête nomme le n+1 (une valeur nulle retire alors le n+1) ; faux : n+1
     *     résolu pour chaque personne. Une personne qui est elle-même le n+1 demandé reçoit le n+1 résolu.
     */
    public List<Resultat> affecter(Acteur acteur, String code, List<String> comptes, boolean responsableFourni, String responsableId, String motif) {
        String m = motif(motif);
        List<String> ids = identifiants(comptes);
        Map<String, Unite> parCode = parCode();
        Unite unite = parCode.get(code);
        if (unite == null) {
            throw IamException.introuvable("ORG_UNITE_INTROUVABLE", code);
        }
        if (!unite.active()) {
            throw IamException.nonTraitable("ORG_UNITE_INCONNUE", "Unité inconnue ou inactive", Map.of("unite", code));
        }
        String demande = OrganisationService.vide(responsableId);

        // 1. Personnes : connues, internes, actives.
        List<Map<String, String>> refus = new ArrayList<>();
        Map<String, Compte> lus = new LinkedHashMap<>();
        for (String id : ids) {
            Compte c = annuaire.lire(id).orElse(null);
            if (c == null) {
                refus.add(refus(id, "IAM_ACCOUNT_NOT_FOUND", "Compte inconnu"));
            } else if (!c.interne()) {
                refus.add(refus(id, "ORG_COMPTE_EXTERNE", "Un compte d'institution n'a ni unité ni responsable"));
            } else if (!c.actif()) {
                refus.add(refus(id, "ORG_COMPTE_INACTIF", "Le compte est désactivé"));
            } else {
                lus.put(id, c);
            }
        }
        refuserSi(code, refus);

        // 2. n+1 visé de chacun.
        Map<String, String> responsables = new LinkedHashMap<>();
        lus.forEach((id, c) ->
            responsables.put(id, responsableFourni && !id.equals(demande) ? demande : ReglesOrganisation.responsableParDefaut(id, code, parCode))
        );

        // 3. Chef sans droit d'administration : périmètre de l'action RATTACHEMENT_MODIFIER (403 ORG_HORS_PERIMETRE).
        String origine = "affectation=" + code;
        if (!acteur.peut(PERMISSION_ADMINISTRATION)) {
            origine += " " + pouvoirs.verifierAffectationParChef(acteur, code, responsables);
        }

        // 4. Règles du rattachement pour chacun, avant toute modification.
        for (Compte c : lus.values()) {
            try {
                ReglesOrganisation.verifierRattachement(c, code, responsables.get(c.id()), parCode, annuaire::lire);
            } catch (IamException e) {
                refus.add(refus(c.id(), e.code(), e.getMessage()));
            }
        }
        refuserSi(code, refus);

        // 5. Affectation, une personne après l'autre (journal et événement par personne).
        List<Resultat> resultats = new ArrayList<>();
        for (Compte c : lus.values()) {
            Compte apres = organisation.rattacher(acteur, c.id(), code, responsables.get(c.id()), m, origine);
            resultats.add(new Resultat(c.id(), c.nomAffiche(), new Etat(c.unite(), c.responsableId()), new Etat(apres.unite(), apres.responsableId())));
        }
        return resultats;
    }

    /**
     * Retire une personne de l'unité {@code code} (D-102 §2) : son unité devient nulle ; son n+1 est retiré s'il était
     * celui que l'unité lui donnait par défaut (chef de l'unité ou de la plus proche unité parente), gardé sinon. La
     * personne doit être rattachée à cette unité (409 ORG_AFFECTATION_ABSENTE) et ne pas en être le chef (409
     * ORG_CHEF_UNITE : désigner d'abord un autre chef). Motif obligatoire, journal et événement comme l'affectation.
     */
    public Resultat retirer(Acteur acteur, String code, String id, String motif) {
        String m = motif(motif);
        Map<String, Unite> parCode = parCode();
        Unite unite = parCode.get(code);
        if (unite == null) {
            throw IamException.introuvable("ORG_UNITE_INTROUVABLE", code);
        }
        Compte c = annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
        if (!code.equals(c.unite())) {
            throw IamException.conflit("ORG_AFFECTATION_ABSENTE", "La personne n'est pas rattachée à cette unité", Map.of("id", id, "unite", code));
        }
        if (id.equals(unite.responsableId())) {
            throw IamException.conflit("ORG_CHEF_UNITE", "La personne est le chef de cette unité : désigner d'abord un autre chef", Map.of("id", id, "unite", code));
        }
        String parDefaut = ReglesOrganisation.responsableParDefaut(id, code, parCode);
        String responsable = c.responsableId() != null && c.responsableId().equals(parDefaut) ? null : c.responsableId();
        Compte apres = organisation.rattacher(acteur, id, null, responsable, m, "retrait=" + code);
        return new Resultat(id, c.nomAffiche(), new Etat(c.unite(), c.responsableId()), new Etat(apres.unite(), apres.responsableId()));
    }

    // ------------------------------------------------------------------ contrôles

    static String motif(String motif) {
        String m = OrganisationService.vide(motif);
        if (m == null || m.length() < MOTIF_MIN || m.length() > MOTIF_MAX) {
            throw IamException.invalide("IAM_REASON_REQUIRED", "Motif obligatoire, de " + MOTIF_MIN + " à " + MOTIF_MAX + " caractères", Map.of("champ", "motif"));
        }
        return m;
    }

    static List<String> identifiants(List<String> comptes) {
        if (comptes == null || comptes.isEmpty() || comptes.size() > AFFECTATION_MAX) {
            throw IamException.invalide("ORG_AFFECTATION_INVALIDE", "De 1 à " + AFFECTATION_MAX + " personnes par affectation", Map.of("champ", "comptes"));
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String id : comptes) {
            String v = OrganisationService.vide(id);
            if (v == null || v.length() > 36) {
                throw IamException.invalide("ORG_AFFECTATION_INVALIDE", "Identifiant de compte invalide", Map.of("champ", "comptes"));
            }
            ids.add(v);
        }
        return List.copyOf(ids);
    }

    private static Map<String, String> refus(String id, String code, String message) {
        return Map.of("id", id, "code", code, "message", message == null ? code : message);
    }

    private static void refuserSi(String unite, List<Map<String, String>> refus) {
        if (!refus.isEmpty()) {
            throw IamException.nonTraitable(
                "ORG_AFFECTATION_REFUSEE",
                "Affectation refusée : aucune personne n'a été affectée",
                Map.of("unite", unite, "refus", List.copyOf(refus))
            );
        }
    }

    private Map<String, Unite> parCode() {
        Map<String, Unite> m = new LinkedHashMap<>();
        catalogue.toutes().forEach(u -> m.put(u.code(), u));
        return m;
    }
}
