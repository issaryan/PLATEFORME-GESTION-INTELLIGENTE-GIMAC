package cm.gimac.des.homologation.domain.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Charge de homologation.operation-activated.v1 et homologation.operation-validation-requested.v1 (sujet
 * homologation.operation, clé = liaison, D-93 §1), consommée par MS-NOTIFICATION (C et I internes, comptes de
 * l'institution pour une opération externe) et journalisée par l'audit.
 * <ul>
 * <li>{@code motif} (activée seulement) : OUVERTURE (prédécesseurs faits), RENVOI (renvoyée par le validateur),
 * REPRISE (rouverte après avoir été dépassée) ;</li>
 * <li>{@code responsable}, {@code validateur} : R et V ; {@code destinataires} : consultés (C) et informés (I)
 * internes ;</li>
 * <li>{@code externe} : R ou E tenu par une institution ; {@code institutionId} : l'institution concernée (celle de R,
 * sinon la première de E), {@code institutionsConcernees} : toutes ; null pour une opération interne ;</li>
 * <li>{@code etape} : code de l'étape de la version ; {@code vue} : écran spécialisé ; {@code lien} : chemin de
 * l'interface du DES ;</li>
 * <li>{@code declencheur} : événement externe qui clôt l'opération (par exemple certification.pv-signed.v1), absent
 * sinon ; {@code signatures} : signatures configurées de l'opération (D-91), vide sinon. Lus par MS-CERTIFICATION
 * (gestes contrôlés, circuit de signature, docs/lot1/effet-reel-referentiels.md §10).</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperationParcoursPayload(
    UUID liaisonId,
    String reference,
    UUID participantId,
    String institutionNom,
    String typeProjet,
    String etatLiaison,
    String statut,
    UUID operationId,
    String code,
    String libelle,
    String etape,
    String vue,
    String geste,
    String jalon,
    LocalDate echeance,
    String motif,
    DestinataireOperation responsable,
    DestinataireOperation validateur,
    List<DestinataireOperation> destinataires,
    boolean externe,
    String institutionId,
    List<String> institutionsConcernees,
    String motifRenvoi,
    String lien,
    Instant le,
    String declencheur,
    List<SignatureConfiguree> signatures
) {
    public static final String TYPE_ACTIVEE = "homologation.operation-activated.v1";
    public static final String TYPE_VALIDATION = "homologation.operation-validation-requested.v1";

    /** Signature configurée de l'opération (D-91) : code, libellé, fonction du signataire, ordre effectif. */
    public record SignatureConfiguree(String code, String libelle, String signataire, int ordre) {}
}
