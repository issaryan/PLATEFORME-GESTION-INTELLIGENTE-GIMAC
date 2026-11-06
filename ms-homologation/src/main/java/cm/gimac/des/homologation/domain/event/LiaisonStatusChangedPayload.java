package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * homologation.liaison-status-changed.v1 (sujet homologation.liaison ; nom du point P14 retenu par D-122) : la liaison
 * est ouverte, gelée ou close pour la Sandbox GIMAC (US-EXI-16). Lu par SBX-CAMPAGNE (aucune campagne sur une
 * liaison qui n'est pas ouverte) et SBX-LECTURE (périmètre du compte de service). {@code versionEtat} croît
 * strictement d'une publication à l'autre pour une même liaison : un lecteur ignore toute version inférieure ou égale
 * à celle qu'il connaît. {@code etatLiaison} : état technique de la liaison (automate HOM-19), pour information.
 * {@code referenceOrigine} (D-133, ajouté par extension) : référence du projet dans l'outil d'origine, normalisée par la
 * reprise D-107 ; SBX-CAMPAGNE y rattache une campagne reprise de même code, jamais une liaison par défaut.
 */
public record LiaisonStatusChangedPayload(
    UUID liaisonId,
    UUID institutionId,
    String codeParticipant,
    String typeProjet,
    String statut,
    String statutPrecedent,
    String etatLiaison,
    long versionEtat,
    Instant survenuLe,
    String referenceOrigine
) {
    public static final String TYPE = "homologation.liaison-status-changed.v1";

    /** Charge antérieure à D-133, sans référence d'origine. */
    public LiaisonStatusChangedPayload(
        UUID liaisonId,
        UUID institutionId,
        String codeParticipant,
        String typeProjet,
        String statut,
        String statutPrecedent,
        String etatLiaison,
        long versionEtat,
        Instant survenuLe
    ) {
        this(liaisonId, institutionId, codeParticipant, typeProjet, statut, statutPrecedent, etatLiaison, versionEtat, survenuLe, null);
    }
}
