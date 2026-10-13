package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.HomologationException;
import java.util.Map;
import java.util.Set;

/**
 * Règles d'ouverture d'une liaison au cadrage (étape 3, rang 3 du CR ; D-57, D-58). Service de
 * domaine pur.
 * <ul>
 *   <li>L'institution est un adhérent (première liaison, qui la fait passer à EN_HOMOLOGATION), déjà en
 *       homologation (nouvelle liaison sans repasser par l'adhésion, rang 35) ou déjà active (nouveau projet d'une
 *       institution en production, D-107 : son statut ne change pas, elle ne repasse pas en homologation).</li>
 *   <li>Le porteur éventuel est une banque ; le portage n'est jamais un type de projet.</li>
 *   <li>Le conducteur de projet est un collaborateur actif du GIMAC portant la permission dédiée
 *       HOM_PROJECT_CONDUCT (D-94 §3, quel que soit le profil qui la lui donne) ; il est désigné par un chef de
 *       département.</li>
 * </ul>
 */
public final class LiaisonOpeningPolicy {

    /** Permission qui rend un collaborateur désignable comme conducteur de projet (D-94 §3). */
    public static final String PERMISSION_CONDUITE = "HOM_PROJECT_CONDUCT";
    private static final Set<String> STATUTS_ELIGIBLES = Set.of("ADHERENT", "EN_HOMOLOGATION", "ACTIF");

    public void verifier(InstitutionView institution, InstitutionView porteur, CollaboratorView conducteur, boolean porteConduite) {
        if (!STATUTS_ELIGIBLES.contains(institution.statut())) {
            throw HomologationException.conflict(
                "HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE",
                "Une liaison ne s'ouvre que sur un adhérent (étape 2 prononcée), une institution en homologation ou une institution active",
                Map.of("institutionId", institution.id().toString(), "statut", institution.statut())
            );
        }
        if (porteur != null) {
            if (porteur.id().equals(institution.id())) {
                throw HomologationException.conflict("HOM_LIAISON_CARRIER_INVALID", "Une institution ne se porte pas elle-même", Map.of());
            }
            if (!"BANQUE".equals(porteur.categorie())) {
                throw HomologationException.conflict(
                    "HOM_LIAISON_CARRIER_NOT_BANK",
                    "Le porteur désigné n'est pas une banque",
                    Map.of("banquePartenaireId", porteur.id().toString())
                );
            }
        }
        verifierConducteur(conducteur, porteConduite);
    }

    /** {@code porteConduite} : le compte porte HOM_PROJECT_CONDUCT (annuaire par permission). */
    public void verifierConducteur(CollaboratorView c, boolean porteConduite) {
        if (c == null || !c.actif() || (c.institutionId() != null && !c.institutionId().isBlank()) || !porteConduite) {
            throw HomologationException.conflict(
                "HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED",
                "Le conducteur de projet doit être un collaborateur actif du GIMAC portant la permission HOM_PROJECT_CONDUCT",
                Map.of()
            );
        }
    }
}
