package cm.gimac.des.iam.domain.sandbox;

import java.time.Instant;

/**
 * Indicateur de suspension posé sur le périmètre d'une institution (D-119, US-EXI-06, algorithme 6.2 du CC de la
 * sandbox) : superposé à l'état des comptes et des délégations, jamais recopié sur eux.
 *
 * @param code code participant de l'institution (claim institution_id)
 * @param depuis instant où l'indicateur a été posé
 * @param acteurId compte ou service qui l'a posé (saga SUSPENSION de SBX-ESPACE)
 * @param motif motif transmis, nul s'il n'y en a pas
 */
public record PerimetreSuspendu(String code, Instant depuis, String acteurId, String motif) {}
