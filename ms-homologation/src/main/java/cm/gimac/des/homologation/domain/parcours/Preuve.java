package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Preuve d'une opération : pièce déposée dans MS-DOCUMENT (référence et empreinte), valeur de
 * référence relisible, code de contrôle d'une clé ou cérémonie de clés. Jamais de valeur secrète. Une preuve reprise de
 * l'existant (D-107) porte {@code reprise} à vrai et, à défaut de pièce et d'empreinte, son commentaire de reprise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Preuve(
    String documentRef,
    String nom,
    String empreinte,
    LocalDate deposeLe,
    String deposePar,
    ValeurReference valeurReference,
    String codeControle,
    Ceremonie ceremonie,
    Boolean reprise,
    String commentaire
) {
    public Preuve(
        String documentRef,
        String nom,
        String empreinte,
        LocalDate deposeLe,
        String deposePar,
        ValeurReference valeurReference,
        String codeControle,
        Ceremonie ceremonie
    ) {
        this(documentRef, nom, empreinte, deposeLe, deposePar, valeurReference, codeControle, ceremonie, null, null);
    }

    /** Preuve reprise de l'existant (D-107) : référence et empreinte si la pièce est déposée, sinon le seul commentaire. */
    public static Preuve reprise(String documentRef, String empreinte, LocalDate le, String par, ValeurReference valeur, String commentaire) {
        return new Preuve(documentRef, null, empreinte, le, par, valeur, null, null, Boolean.TRUE, commentaire);
    }

    /** Preuve reprise de l'existant (D-107), et non déposée sur la plateforme. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean estReprise() {
        return Boolean.TRUE.equals(reprise);
    }

    public static Preuve piece(String documentRef, String nom, String empreinte, LocalDate le, String par) {
        return new Preuve(documentRef, nom, empreinte, le, par, null, null, null);
    }

    public Preuve avecValeurReference(ValeurReference v) {
        return new Preuve(documentRef, nom, empreinte, deposeLe, deposePar, v, codeControle, ceremonie, reprise, commentaire);
    }

    public Preuve avecCle(String code, Ceremonie c) {
        return new Preuve(documentRef, nom, empreinte, deposeLe, deposePar, valeurReference, code, c, reprise, commentaire);
    }

    /** Projection du portail : le déposant du GIMAC n'apparaît que par sa fonction. */
    public Preuve deposeeParFonction(String fonction) {
        return new Preuve(documentRef, nom, empreinte, deposeLe, fonction, valeurReference, codeControle, ceremonie, reprise, commentaire);
    }
}
