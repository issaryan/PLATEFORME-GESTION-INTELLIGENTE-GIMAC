package cm.gimac.des.homologation.domain.event;

import java.util.List;
import java.util.UUID;

/**
 * homologation.liaison-opened.v1 (sujet homologation.liaison) : liaison ouverte au cadrage. Lu par
 * MS-PARTICIPANT, qui fait passer l'adhérent à EN_HOMOLOGATION (D-57), et par MS-AUDIT. {@code modeleTypeProjet} :
 * modèle du type de projet (D-91), ajouté sans changer les autres champs. {@code codeParticipant} et
 * {@code versionEtat} (US-EXI-16, D-122) : code du participant au registre et version d'état de la liaison (1 à
 * l'ouverture), lus par SBX-CAMPAGNE et SBX-LECTURE ; ajoutés sans changer les autres champs.
 */
public record LiaisonOpenedPayload(
    UUID liaisonId,
    UUID institutionId,
    String typeProjet,
    UUID versionId,
    Integer versionNumero,
    UUID chefDeProjetId,
    UUID banquePartenaireId,
    List<String> perimetre,
    UUID ouvertPar,
    String modeleTypeProjet,
    String codeParticipant,
    Long versionEtat
) {
    public static final String TYPE = "homologation.liaison-opened.v1";

    /** Charge antérieure à US-EXI-16 : sans code participant ni version d'état. */
    public LiaisonOpenedPayload(
        UUID liaisonId,
        UUID institutionId,
        String typeProjet,
        UUID versionId,
        Integer versionNumero,
        UUID chefDeProjetId,
        UUID banquePartenaireId,
        List<String> perimetre,
        UUID ouvertPar,
        String modeleTypeProjet
    ) {
        this(liaisonId, institutionId, typeProjet, versionId, versionNumero, chefDeProjetId, banquePartenaireId, perimetre, ouvertPar, modeleTypeProjet, null, null);
    }

    /** Charge sans modèle (antérieure à D-91) : le modèle est celui du type repris de même nom, sinon générique. */
    public LiaisonOpenedPayload(
        UUID liaisonId,
        UUID institutionId,
        String typeProjet,
        UUID versionId,
        Integer versionNumero,
        UUID chefDeProjetId,
        UUID banquePartenaireId,
        List<String> perimetre,
        UUID ouvertPar
    ) {
        this(
            liaisonId,
            institutionId,
            typeProjet,
            versionId,
            versionNumero,
            chefDeProjetId,
            banquePartenaireId,
            perimetre,
            ouvertPar,
            cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet.parDefaut(
                typeProjet == null ? null : cm.gimac.des.homologation.domain.enumeration.TypeProjet.valueOf(typeProjet)
            ).name()
        );
    }

    /** Même charge, complétée du code participant et de la version d'état (US-EXI-16). */
    public LiaisonOpenedPayload avecEtat(String code, long version) {
        return new LiaisonOpenedPayload(liaisonId, institutionId, typeProjet, versionId, versionNumero, chefDeProjetId, banquePartenaireId, perimetre, ouvertPar, modeleTypeProjet, code, version);
    }
}
