package cm.gimac.des.homologation.domain.cadrage;

import java.util.List;
import java.util.UUID;

/**
 * Institution telle que publiée par MS-PARTICIPANT : CANDIDAT, ADHERENT, EN_HOMOLOGATION, ACTIF… (D-57).
 * {@code codeParticipant} : code du participant au registre, servi par l'interface interne de MS-PARTICIPANT et porté
 * par les événements de liaison lus par la Sandbox GIMAC (US-EXI-16, D-122) ; absent d'une vue antérieure.
 */
public record InstitutionView(UUID id, String nom, String statut, String categorie, List<ServiceSouscrit> servicesSouscrits, String codeParticipant) {
    public InstitutionView {
        servicesSouscrits = servicesSouscrits == null ? List.of() : List.copyOf(servicesSouscrits);
    }

    /** Vue sans code participant (jeux d'essai antérieurs à US-EXI-16). */
    public InstitutionView(UUID id, String nom, String statut, String categorie, List<ServiceSouscrit> servicesSouscrits) {
        this(id, nom, statut, categorie, servicesSouscrits, null);
    }

    /** Service souscrit par l'institution (source du périmètre contractualisé, HOM-05). */
    public record ServiceSouscrit(String code, String depuis) {}
}
