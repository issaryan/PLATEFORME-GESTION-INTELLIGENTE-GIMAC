package cm.gimac.des.homologation.domain.cadrage;

import java.util.List;
import java.util.UUID;

/** Institution telle que publiée par MS-PARTICIPANT : CANDIDAT, ADHERENT, EN_HOMOLOGATION, ACTIF… (D-57). */
public record InstitutionView(UUID id, String nom, String statut, String categorie, List<ServiceSouscrit> servicesSouscrits) {
    public InstitutionView {
        servicesSouscrits = servicesSouscrits == null ? List.of() : List.copyOf(servicesSouscrits);
    }

    /** Service souscrit par l'institution (source du périmètre contractualisé, HOM-05). */
    public record ServiceSouscrit(String code, String depuis) {}
}
