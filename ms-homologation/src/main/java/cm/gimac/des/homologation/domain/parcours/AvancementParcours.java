package cm.gimac.des.homologation.domain.parcours;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Avancement global d'un projet (D-110) : part pondérée des étapes du CR franchies, de la prise de contact à la mise en
 * production. Chaque étape vaut son poids multiplié par la part de ses opérations faites (les opérations sans objet ne
 * comptent pas) ; la prise de contact et l'adhésion, tenues par MS-PARTICIPANT, sont acquises à l'ouverture de la
 * liaison. Les essais valent au moins le taux de réussite mesuré sur la sandbox ({@link #ESSAIS_GLOBAL}, repris ou
 * rapproché du simulateur), faute de quoi la moyenne des taux par service. Le suivi d'exploitation ne compte pas : une
 * liaison en production est à 100 %.
 */
public final class AvancementParcours {

    /** Code de la mesure globale des essais (table coverage, nature CERTIFICATION) : cas réussis sur cas attribués. */
    public static final String ESSAIS_GLOBAL = "ESSAIS_GLOBAL";

    /** Poids des étapes, sur 100 : les essais portent l'essentiel de l'effort d'un projet de certification. */
    public static final Map<EtapeCode, Integer> POIDS;

    static {
        Map<EtapeCode, Integer> p = new EnumMap<>(EtapeCode.class);
        p.put(EtapeCode.PRISE_CONTACT, 3);
        p.put(EtapeCode.ADHESION, 2);
        p.put(EtapeCode.CADRAGE, 5);
        p.put(EtapeCode.LANCEMENT, 5);
        p.put(EtapeCode.ATELIERS, 10);
        p.put(EtapeCode.INTEGRATION, 10);
        p.put(EtapeCode.TESTS, 35);
        p.put(EtapeCode.CERTIFICATION, 10);
        p.put(EtapeCode.APPROBATION_SECURITE, 10);
        p.put(EtapeCode.MISE_EN_PRODUCTION, 10);
        POIDS = Collections.unmodifiableMap(p);
    }

    private AvancementParcours() {}

    /** Avancement en pourcentage entier, porté par une couverture calculable (numérateur : points acquis sur 100). */
    public static Couverture de(StatutParcours statut, List<OperationParcours> operations, Map<String, Integer> mesures) {
        if (statut == StatutParcours.EN_PRODUCTION) {
            return new Couverture(100, 100, 100, true, null);
        }
        if (operations.isEmpty()) {
            return new Couverture(null, 0, 0, false, "aucune opération instanciée");
        }
        double acquis = 0;
        double total = 0;
        for (Map.Entry<EtapeCode, Integer> e : POIDS.entrySet()) {
            EtapeCode etape = e.getKey();
            List<OperationParcours> ops = operations.stream().filter(o -> o.getEtape() == etape && o.getEtat() != EtatOperationParcours.SANS_OBJET).toList();
            Double part;
            if (etape == EtapeCode.PRISE_CONTACT || etape == EtapeCode.ADHESION) {
                part = 1.0;
            } else if (ops.isEmpty()) {
                part = etape == EtapeCode.TESTS ? essais(mesures) : null;
            } else {
                part = ops.stream().filter(OperationParcours::faite).count() / (double) ops.size();
                if (etape == EtapeCode.TESTS && part < 1.0) {
                    Double mesure = essais(mesures);
                    part = mesure == null ? part : Math.max(part, mesure);
                }
            }
            if (part != null) {
                acquis += e.getValue() * part;
                total += e.getValue();
            }
        }
        int taux = (int) Math.round(acquis * 100 / total);
        return new Couverture(taux, taux, 100, true, null);
    }

    /** Taux de réussite des essais : mesure globale, sinon moyenne des taux par service ; null sans mesure. */
    static Double essais(Map<String, Integer> mesures) {
        if (mesures == null || mesures.isEmpty()) {
            return null;
        }
        Integer global = mesures.get(ESSAIS_GLOBAL);
        if (global != null) {
            return Math.min(100, Math.max(0, global)) / 100.0;
        }
        return mesures.values().stream().mapToInt(v -> Math.min(100, Math.max(0, v))).average().orElse(0) / 100.0;
    }
}
