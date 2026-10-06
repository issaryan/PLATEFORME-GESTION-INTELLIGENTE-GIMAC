package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Signature exigée par une étape (§1) : le signataire signe l'empreinte des livrables portés ({@code porte},
 * livrables de l'étape ou d'étapes antérieures) ; les signatures se font par {@code ordre} croissant.
 */
public record SignatureRequise(String code, String libelle, Intervenant signataire, List<String> porte, int ordre) {
    public SignatureRequise {
        porte = porte == null ? List.of() : List.copyOf(porte);
    }
}
