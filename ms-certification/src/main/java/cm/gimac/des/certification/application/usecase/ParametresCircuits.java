package cm.gimac.des.certification.application.usecase;

import cm.gimac.des.certification.domain.decision.EtapeSignature;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Circuits de signature par défaut (D-94, {@code gimac.certification.circuits}) : données de configuration, jamais
 * de qualité codée. {@code procesVerbal} : signatures du procès-verbal (le côté {@code portail} est celui du
 * participant, que le référentiel D-91 ne peut porter) ; {@code visas} : signature de chaque visa optionnel, par nature.
 */
@ConfigurationProperties(prefix = "gimac.certification.circuits")
public record ParametresCircuits(List<Etape> procesVerbal, Map<String, Etape> visas, String finalite) {
    /** Finalité de la réauthentification FIDO2 des signatures (D-91 §5.4). */
    public static final String FINALITE_DEFAUT = "SIGNATURE_OPERATION";

    public ParametresCircuits {
        procesVerbal = procesVerbal == null ? List.of() : List.copyOf(procesVerbal);
        visas = visas == null ? Map.of() : Map.copyOf(visas);
        finalite = finalite == null || finalite.isBlank() ? FINALITE_DEFAUT : finalite.trim();
    }

    /** Étape saisie en configuration : ordre absent = 1, côté absent = interne. */
    public record Etape(String code, String libelle, String fonction, Integer ordre, Boolean portail) {
        public EtapeSignature vers() {
            return new EtapeSignature(code, libelle, fonction, ordre == null ? 1 : ordre, Boolean.TRUE.equals(portail));
        }
    }
}
