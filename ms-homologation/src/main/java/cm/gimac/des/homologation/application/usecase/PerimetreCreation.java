package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.PerimetreProposable;
import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase.ServiceProposable;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.PerimetreModification;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Périmètre défini à la création du projet (D-145, décision du responsable du 12/11/2026) : le gestionnaire du projet
 * au GIMAC retient, parmi les services proposables (services souscrits de l'institution, sinon catalogue du type de
 * projet), les services du projet ; l'administrateur de l'institution ne le décide pas. TRANSVERSE reste toujours
 * inclus, un service non proposable est refusé (RG-72), et la définition est journalisée comme version 1 du périmètre.
 */
final class PerimetreCreation {

    static final String PERMISSION = "HOM_LIAISON_OPEN";
    static final String MOTIF = "Périmètre défini par le gestionnaire à la création du projet (D-145)";
    static final String MOBILE = "MOBILE";
    static final String ISO20022 = "ISO20022";
    static final String CARTE = "CARTE";

    private PerimetreCreation() {}

    /**
     * Périmètre retenu : sans saisie, les services proposables (dérivation HOM-05) ; sinon TRANSVERSE et les services
     * saisis, dans l'ordre des proposables. Refus 422 HOM_PERIMETRE_VIDE sans service à certifier, 422
     * HOM_PERIMETRE_SERVICE_NON_SOUSCRIT pour un service qui n'est pas proposable.
     */
    static List<String> retenir(List<String> saisis, List<String> proposables) {
        if (saisis == null) {
            return proposables;
        }
        Set<String> choisis = new LinkedHashSet<>();
        saisis.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).filter(s -> !CatalogueServices.TRANSVERSE.equals(s)).forEach(choisis::add);
        if (choisis.isEmpty()) {
            throw HomologationException.unprocessable("HOM_PERIMETRE_VIDE", "Le périmètre porte au moins un service à certifier.");
        }
        List<String> inconnus = choisis.stream().filter(s -> !proposables.contains(s)).toList();
        if (!inconnus.isEmpty()) {
            throw HomologationException.unprocessableDetails(
                "HOM_PERIMETRE_SERVICE_NON_SOUSCRIT",
                "Un service non souscrit ne peut entrer au périmètre (RG-72).",
                Map.of("services", inconnus)
            );
        }
        List<String> retenus = new ArrayList<>();
        retenus.add(CatalogueServices.TRANSVERSE);
        proposables.stream().filter(choisis::contains).forEach(retenus::add);
        return retenus;
    }

    /** Ligne de journal de la version 1 : services proposables avant, services retenus après. */
    static PerimetreModification ligne(UUID liaison, UUID auteur, List<String> proposables, List<String> retenus, Instant t) {
        return new PerimetreModification(liaison, 1, auteur, null, PERMISSION, String.join(",", proposables), String.join(",", retenus), MOTIF, t);
    }

    /** Services proposables à l'écran de création, TRANSVERSE exclu, avec l'interface de la sandbox de chacun. */
    static PerimetreProposable proposable(List<String> proposables, InstitutionView institution) {
        boolean souscrits = institution.servicesSouscrits().stream().anyMatch(s -> s.code() != null && !s.code().isBlank() && !CatalogueServices.TRANSVERSE.equals(s.code()));
        List<ServiceProposable> services = proposables
            .stream()
            .filter(c -> !CatalogueServices.TRANSVERSE.equals(c))
            .map(c -> new ServiceProposable(c, CatalogueServices.libelle(c), interfaceSandbox(c, souscrits)))
            .toList();
        return new PerimetreProposable(services, souscrits);
    }

    /**
     * Interface de la sandbox d'après le préfixe du code de service (D-143) ; un service du catalogue du type de
     * projet, proposé faute de souscription, n'en porte pas.
     */
    static String interfaceSandbox(String code, boolean souscrit) {
        if (code.startsWith("ISO.")) {
            return ISO20022;
        }
        if (code.startsWith("CRT.")) {
            return CARTE;
        }
        return souscrit ? MOBILE : null;
    }
}
