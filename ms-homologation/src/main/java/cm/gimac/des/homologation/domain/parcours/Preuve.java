package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Preuve d'une opération : pièce déposée dans MS-DOCUMENT (référence et empreinte), valeur de
 * référence relisible, code de contrôle d'une clé ou cérémonie de clés. Jamais de valeur secrète.
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
    Ceremonie ceremonie
) {
    public static Preuve piece(String documentRef, String nom, String empreinte, LocalDate le, String par) {
        return new Preuve(documentRef, nom, empreinte, le, par, null, null, null);
    }

    public Preuve avecValeurReference(ValeurReference v) {
        return new Preuve(documentRef, nom, empreinte, deposeLe, deposePar, v, codeControle, ceremonie);
    }

    public Preuve avecCle(String code, Ceremonie c) {
        return new Preuve(documentRef, nom, empreinte, deposeLe, deposePar, valeurReference, code, c);
    }

    /** Projection du portail : le déposant du GIMAC n'apparaît que par sa fonction. */
    public Preuve deposeeParFonction(String fonction) {
        return new Preuve(documentRef, nom, empreinte, deposeLe, fonction, valeurReference, codeControle, ceremonie);
    }
}
