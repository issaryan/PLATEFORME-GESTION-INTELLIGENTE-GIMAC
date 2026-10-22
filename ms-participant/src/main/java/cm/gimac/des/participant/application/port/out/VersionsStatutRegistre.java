package cm.gimac.des.participant.application.port.out;

import java.util.UUID;

/**
 * Version de statut d'une institution au registre (US-EXI-20, D-120) : entier strictement croissant par institution,
 * attribué dans la transaction du changement de statut et publié dans participant.status-changed.v1
 * (versionStatutRegistre). Les consommateurs (SBX-ESPACE, SBX-RACCORDEMENT) écartent un événement de version
 * inférieure ou égale à celle qu'ils ont retenue.
 */
public interface VersionsStatutRegistre {
    /** Version suivante (1 au premier changement de statut) ; à appeler dans la transaction du changement. */
    long suivante(UUID institutionId);

    /** Version courante, 0 si aucun changement de statut n'a encore été publié. */
    long courante(UUID institutionId);
}
