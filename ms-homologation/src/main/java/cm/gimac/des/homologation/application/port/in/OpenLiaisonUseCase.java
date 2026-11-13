package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.List;
import java.util.UUID;

/** Cadrage (étape 3, D-57, D-58) : ouverture d'une liaison sur un adhérent et désignation du conducteur de projet. */
public interface OpenLiaisonUseCase {
    /** Acteur authentifié : sujet du jeton ; peutDesigner s'il détient HOM_PROJECT_DESIGNATE (chef de département). */
    record Acteur(UUID id, boolean peutDesigner) {}

    /**
     * Ouverture d'une liaison. Une institution peut conduire plusieurs liaisons actives, y compris du même type
     * (D-104) : {@code intituleProjet} les distingue, {@code referenceOrigine} (facultative, unique) rattache un
     * projet repris à sa référence dans l'outil d'origine. {@code perimetre} (D-145) : services retenus par le
     * gestionnaire du projet à la création, parmi les services proposables ; nul pour un appelant qui ne le saisit pas
     * (reprise, automate), le périmètre étant alors dérivé des services souscrits (HOM-05).
     */
    record OpenLiaisonCommand(
        UUID institutionId,
        TypeProjet typeProjet,
        UUID chefDeProjetId,
        UUID banquePartenaireId,
        String intituleProjet,
        String referenceOrigine,
        List<String> perimetre
    ) {
        public OpenLiaisonCommand(UUID institutionId, TypeProjet typeProjet, UUID chefDeProjetId, UUID banquePartenaireId, String intituleProjet, String referenceOrigine) {
            this(institutionId, typeProjet, chefDeProjetId, banquePartenaireId, intituleProjet, referenceOrigine, null);
        }

        public OpenLiaisonCommand(UUID institutionId, TypeProjet typeProjet, UUID chefDeProjetId, UUID banquePartenaireId) {
            this(institutionId, typeProjet, chefDeProjetId, banquePartenaireId, null, null);
        }
    }

    /**
     * Service proposable au périmètre d'un projet à créer (D-145) : code, libellé et interface de la sandbox d'après
     * le préfixe du code (D-143 : {@code ISO.} pour le mobile ISO 20022, {@code CRT.} pour la carte ISO 8583, sinon
     * le mobile propriétaire ; nulle pour un service du catalogue du type de projet proposé faute de souscription).
     */
    record ServiceProposable(String code, String libelle, String interfaceSandbox) {}

    /**
     * Services proposables pour une institution et un type de projet : services souscrits, sinon catalogue du type
     * ({@code souscrits} faux), TRANSVERSE exclu puisqu'il est toujours inclus.
     */
    record PerimetreProposable(List<ServiceProposable> services, boolean souscrits) {}

    Liaison ouvrir(OpenLiaisonCommand commande, Acteur acteur);

    PerimetreProposable perimetreProposable(UUID institutionId, TypeProjet typeProjet);

    Liaison designerChefDeProjet(UUID liaisonId, UUID chefDeProjetId, String motif, Acteur acteur);
}
