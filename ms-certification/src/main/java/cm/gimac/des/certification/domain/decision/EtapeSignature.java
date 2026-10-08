package cm.gimac.des.certification.domain.decision;

import java.util.regex.Pattern;

/**
 * Étape d'un circuit de signature (D-94, en remplacement de l'ancienne énumération des qualités) : code de la
 * signature, libellé, fonction signataire (code du catalogue des fonctions de MS-HOMOLOGATION, D-91), ordre (même
 * ordre : indifférent) et côté de la signature ({@code portail} : donnée au portail par l'institution de la liaison).
 */
public record EtapeSignature(String code, String libelle, String fonction, int ordre, boolean portail) {
    /** Code de signature ou de fonction : majuscule, puis 1 à 39 majuscules, chiffres ou soulignés (D-91). */
    public static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");

    public EtapeSignature {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Code de signature invalide : " + code);
        }
        if (fonction == null || !CODE.matcher(fonction).matches()) {
            throw new IllegalArgumentException("Fonction signataire invalide pour " + code + " : " + fonction);
        }
        if (ordre < 1 || ordre > 99) {
            throw new IllegalArgumentException("Ordre de la signature " + code + " hors de 1..99 : " + ordre);
        }
        libelle = libelle == null || libelle.isBlank() ? code : libelle;
    }
}
