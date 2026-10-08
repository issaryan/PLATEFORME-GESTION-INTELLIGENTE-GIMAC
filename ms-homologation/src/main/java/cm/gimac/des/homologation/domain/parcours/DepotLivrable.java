package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Dépôt d'un livrable attendu de l'opération (D-93 §3) : code du livrable du référentiel, pièce de MS-DOCUMENT
 * (référence, nom, empreinte, type), déposant ; {@code supervision} : déposé par un porteur de HOM_STEP_MANAGE
 * qui n'est ni R, ni déposant, ni conducteur (journalisé comme tel) ; {@code repris} : dépôt antérieur repris par la
 * migration de D-93. Jamais de contenu de document.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepotLivrable(
    String livrable,
    String documentRef,
    String nom,
    String empreinte,
    String typeMime,
    LocalDate deposeLe,
    String deposePar,
    String deposeParId,
    Boolean supervision,
    Boolean repris
) {}
