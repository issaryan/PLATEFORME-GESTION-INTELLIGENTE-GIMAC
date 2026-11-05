package cm.gimac.des.participant.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * participant.subscription-changed.v1 (CC-SBX-GIMAC-02 §6, CP-SBX : nomenclature des services souscrits) : services
 * souscrits d'une institution après une évolution de ses abonnements actifs, sujet participant.subscription. Lu par
 * SBX-RACCORDEMENT (participantCode, puis relecture de la fiche), SBX-CAMPAGNE (codesSouscrits, versionSouscriptions)
 * et SBX-REFERENTIEL (codesSouscrits). versionSouscriptions : instant de l'évolution en millisecondes, croissant par
 * institution ; un lecteur écarte sans effet une version inférieure ou égale à celle qu'il a retenue. Contrat lisible
 * par machine : config/contrats/participant.subscription-changed.v1.schema.json.
 */
public record SubscriptionChangedPayload(UUID institutionId, String participantCode, List<String> codesSouscrits, long versionSouscriptions, Instant dateEffet) {
    public static final String TYPE = "participant.subscription-changed.v1";
}
