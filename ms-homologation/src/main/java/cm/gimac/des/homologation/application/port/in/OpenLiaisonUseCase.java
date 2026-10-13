package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.UUID;

/** Cadrage (étape 3, D-57, D-58) : ouverture d'une liaison sur un adhérent et désignation du conducteur de projet. */
public interface OpenLiaisonUseCase {
    /** Acteur authentifié : sujet du jeton ; peutDesigner s'il détient HOM_PROJECT_DESIGNATE (chef de département). */
    record Acteur(UUID id, boolean peutDesigner) {}

    /**
     * Ouverture d'une liaison. Une institution peut conduire plusieurs liaisons actives, y compris du même type
     * (D-104) : {@code intituleProjet} les distingue, {@code referenceOrigine} (facultative, unique) rattache un
     * projet repris à sa référence dans l'outil d'origine.
     */
    record OpenLiaisonCommand(UUID institutionId, TypeProjet typeProjet, UUID chefDeProjetId, UUID banquePartenaireId, String intituleProjet, String referenceOrigine) {
        public OpenLiaisonCommand(UUID institutionId, TypeProjet typeProjet, UUID chefDeProjetId, UUID banquePartenaireId) {
            this(institutionId, typeProjet, chefDeProjetId, banquePartenaireId, null, null);
        }
    }

    Liaison ouvrir(OpenLiaisonCommand commande, Acteur acteur);

    Liaison designerChefDeProjet(UUID liaisonId, UUID chefDeProjetId, String motif, Acteur acteur);
}
