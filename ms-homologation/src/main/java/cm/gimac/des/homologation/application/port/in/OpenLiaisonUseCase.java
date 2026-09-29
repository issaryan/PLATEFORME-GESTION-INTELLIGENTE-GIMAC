package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.UUID;

/** Cadrage (étape 3, D-57, D-58) : ouverture d'une liaison sur un adhérent et désignation du conducteur de projet. */
public interface OpenLiaisonUseCase {
    /** Acteur authentifié : sujet du jeton ; peutDesigner s'il détient HOM_PROJECT_DESIGNATE (chef de département). */
    record Acteur(UUID id, boolean peutDesigner) {}

    record OpenLiaisonCommand(UUID institutionId, TypeProjet typeProjet, UUID chefDeProjetId, UUID banquePartenaireId) {}

    Liaison ouvrir(OpenLiaisonCommand commande, Acteur acteur);

    Liaison designerChefDeProjet(UUID liaisonId, UUID chefDeProjetId, String motif, Acteur acteur);
}
