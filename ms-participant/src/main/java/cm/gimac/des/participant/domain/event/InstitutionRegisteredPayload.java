package cm.gimac.des.participant.domain.event;

import java.time.LocalDate;
import java.util.UUID;

/** participant.institution-registered.v1 : prise de contact enregistrée (PAR-01, CR-CERT rang 1). */
public record InstitutionRegisteredPayload(
    UUID institutionId,
    String nom,
    String sigle,
    String pays,
    String categorie,
    String statut,
    LocalDate datePriseDeContact,
    UUID chargeDdrmId
) {
    public static final String TYPE = "participant.institution-registered.v1";
}
