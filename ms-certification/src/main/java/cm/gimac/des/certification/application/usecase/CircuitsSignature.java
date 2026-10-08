package cm.gimac.des.certification.application.usecase;

import cm.gimac.des.certification.domain.decision.EtapeSignature;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.operation.OperationLiaison;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Circuits de signature des objets de MS-CERTIFICATION (D-94) : plus aucune qualité signataire codée.
 * <ul>
 * <li>Procès-verbal : signatures configurées de l'opération qui porte la signature du procès-verbal (D-91 §5.4,
 * reçues avec ses événements), complétées par les étapes du portail de la configuration ; à défaut, le circuit par
 * défaut {@code gimac.certification.circuits.proces-verbal}. Il est figé sur le procès-verbal à la première
 * signature.</li>
 * <li>Visa : étape {@code gimac.certification.circuits.visas.<NATURE>}.</li>
 * </ul>
 * La configuration est contrôlée au démarrage : circuit non vide, codes uniques, une étape par visa.
 */
@Component
public class CircuitsSignature {

    private final List<EtapeSignature> procesVerbal;
    private final Map<NatureVisa, EtapeSignature> visas = new EnumMap<>(NatureVisa.class);
    private final String finalite;

    public CircuitsSignature(ParametresCircuits parametres) {
        this.procesVerbal = parametres.procesVerbal().stream().map(ParametresCircuits.Etape::vers).toList();
        if (procesVerbal.isEmpty()) {
            throw new IllegalStateException("gimac.certification.circuits.proces-verbal : au moins une signature est exigée");
        }
        verifierCodesUniques(procesVerbal);
        for (NatureVisa n : NatureVisa.values()) {
            ParametresCircuits.Etape e = parametres.visas().get(n.name());
            if (e == null) {
                throw new IllegalStateException("gimac.certification.circuits.visas." + n.name() + " : signature du visa non configurée");
            }
            visas.put(n, e.vers());
        }
        this.finalite = parametres.finalite();
    }

    /** Circuit du procès-verbal pour les opérations connues qui portent sa signature (vide : liaison sans projection). */
    public List<EtapeSignature> procesVerbal(Collection<OperationLiaison> operationsSignature) {
        List<EtapeSignature> configurees = operationsSignature
            .stream()
            .flatMap(o -> o.getSignatures().stream())
            .map(s -> new EtapeSignature(s.code(), s.libelle(), s.fonction(), s.ordre(), false))
            .toList();
        if (configurees.isEmpty()) {
            return procesVerbal;
        }
        List<EtapeSignature> circuit = new ArrayList<>(configurees);
        procesVerbal.stream().filter(EtapeSignature::portail).filter(p -> configurees.stream().noneMatch(c -> c.code().equals(p.code()))).forEach(circuit::add);
        verifierCodesUniques(circuit);
        return List.copyOf(circuit);
    }

    /** Circuit par défaut du procès-verbal (configuration). */
    public List<EtapeSignature> procesVerbalParDefaut() {
        return procesVerbal;
    }

    public EtapeSignature visa(NatureVisa nature) {
        return visas.get(nature);
    }

    /** Finalité de la réauthentification FIDO2 exigée par une signature (SIGNATURE_OPERATION par défaut). */
    public String finalite() {
        return finalite;
    }

    private static void verifierCodesUniques(List<EtapeSignature> circuit) {
        Set<String> vus = new HashSet<>();
        for (EtapeSignature e : circuit) {
            if (!vus.add(e.code())) {
                throw new IllegalStateException("Circuit de signature : code dupliqué " + e.code());
            }
        }
    }
}
