package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * homologation.liaison-scope-changed.v1 (sujet homologation.liaison, D-133) : périmètre du projet de certification
 * modifié par son gestionnaire, un super administrateur ou un chargé de l'implémentation des projets de la SNA.
 * {@code services} : services inclus après la modification (TRANSVERSE compris) ; {@code versionPerimetre} croît
 * strictement pour une même liaison, la version 1 étant le périmètre de homologation.liaison-opened.v1 : un lecteur
 * ignore toute version inférieure ou égale à celle qu'il connaît. Lu par SBX-CAMPAGNE, qui en dérive les services de
 * ses campagnes, et par MS-AUDIT.
 */
public record LiaisonScopeChangedPayload(
    UUID liaisonId,
    UUID institutionId,
    String codeParticipant,
    String typeProjet,
    List<String> services,
    List<String> servicesAjoutes,
    List<String> servicesRetires,
    int versionPerimetre,
    UUID auteurId,
    String permissionUtilisee,
    String motif,
    Instant modifieLe
) {
    public static final String TYPE = "homologation.liaison-scope-changed.v1";
}
