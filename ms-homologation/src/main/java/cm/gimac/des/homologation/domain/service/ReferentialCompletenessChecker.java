package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Complétude d'un référentiel avant publication (HOM-01 A3) : restitue, par code, les attributs
 * absents. L'identifiant de scénario n'est exigé que pour une opération de nature CERTIFICATION,
 * car c'est lui qui rapproche les résultats du simulateur (HOM-15).
 */
public final class ReferentialCompletenessChecker {

    public record MissingAttributes(String code, List<String> attributs) {}

    public List<MissingAttributes> check(Collection<ReferenceOperation> operations) {
        List<MissingAttributes> manquants = new ArrayList<>();
        for (ReferenceOperation op : operations) {
            List<String> absents = new ArrayList<>();
            if (vide(op.getLibelle())) absents.add("libelle");
            if (vide(op.getPhase())) absents.add("phase");
            if (op.getSegment() == null) absents.add("segment");
            if (op.getPartie() == null) absents.add("partie");
            if (vide(op.getServiceCode())) absents.add("serviceCode");
            if (op.getNatureCouverture() == null) absents.add("natureCouverture");
            if (jsonVide(op.getPreuveAttendue())) absents.add("preuveAttendue");
            if (vide(op.getFonctionResponsable())) absents.add("fonctionResponsable");
            if (op.getNatureCouverture() == NatureCouverture.CERTIFICATION && vide(op.getIdScenario())) absents.add("idScenario");
            if (!absents.isEmpty()) {
                manquants.add(new MissingAttributes(op.getCode(), List.copyOf(absents)));
            }
        }
        manquants.sort(Comparator.comparing(MissingAttributes::code));
        return manquants;
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    /** Preuve attendue stockée en JSON (jsonb) : absente, vide, objet ou tableau vide. */
    private static boolean jsonVide(String json) {
        if (vide(json)) {
            return true;
        }
        String compact = json.replaceAll("\\s", "");
        return compact.equals("{}") || compact.equals("[]") || compact.equals("null");
    }
}
