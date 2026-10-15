package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Dépôt d'un livrable attendu de l'opération (D-93 §3) : code du livrable du référentiel, pièce de MS-DOCUMENT
 * (référence, nom, empreinte, type), déposant ; {@code supervision} : déposé par un porteur de HOM_STEP_MANAGE
 * qui n'est ni R, ni déposant, ni conducteur (journalisé comme tel) ; {@code repris} : dépôt antérieur repris par une
 * migration (D-93, D-109). Jamais de contenu de document. D-109 : {@code service} : service du périmètre visé par un
 * livrable au grain du service ; {@code sansObjet} et {@code motif} : livrable déclaré sans objet, motivé, sans pièce ;
 * {@code origine} : pièce reprise d'un geste dédié ou d'un événement d'un autre service (type d'événement ou geste).
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
    Boolean repris,
    String service,
    Boolean sansObjet,
    String motif,
    String origine
) {
    public DepotLivrable(
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
    ) {
        this(livrable, documentRef, nom, empreinte, typeMime, deposeLe, deposePar, deposeParId, supervision, repris, null, null, null, null);
    }

    /** Livrable déclaré sans objet, motivé (D-109) : aucune pièce. */
    public static DepotLivrable sansObjet(String livrable, String service, String motif, LocalDate le, String par, String parId) {
        return new DepotLivrable(livrable, null, null, null, null, le, par, parId, null, null, service, Boolean.TRUE, motif, null);
    }

    /** Dépôt d'un livrable d'un service du périmètre (D-109). */
    public DepotLivrable pourService(String code) {
        return new DepotLivrable(livrable, documentRef, nom, empreinte, typeMime, deposeLe, deposePar, deposeParId, supervision, repris, code, sansObjet, motif, origine);
    }

    /** Pièce reprise d'un geste dédié ou d'un événement (D-109). */
    public DepotLivrable avecOrigine(String o) {
        return new DepotLivrable(livrable, documentRef, nom, empreinte, typeMime, deposeLe, deposePar, deposeParId, supervision, repris, service, sansObjet, motif, o);
    }

    @JsonIgnore
    public boolean estSansObjet() {
        return Boolean.TRUE.equals(sansObjet);
    }

    @JsonIgnore
    public boolean estRepris() {
        return Boolean.TRUE.equals(repris);
    }

    /** Même livrable et même service (un nouveau dépôt remplace le précédent). */
    public boolean memeCible(DepotLivrable d) {
        return livrable.equals(d.livrable()) && java.util.Objects.equals(service, d.service());
    }
}
