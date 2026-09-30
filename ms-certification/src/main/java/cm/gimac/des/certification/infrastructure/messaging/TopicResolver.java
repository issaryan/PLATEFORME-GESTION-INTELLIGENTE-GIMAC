package cm.gimac.des.certification.infrastructure.messaging;

import cm.gimac.des.certification.domain.event.DecisionEvenements;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Sujet d'agrégat d'un type d'événement (PI §0.4 et §0.5 ; docs/lot1/parcours-backend.md §3). */
@Component
public class TopicResolver {

    /** Sujets nommés par le contrat des verrous, que la règle générale ne déduit pas du type. */
    private static final Map<String, String> SUJETS = Map.ofEntries(
        Map.entry(DecisionEvenements.PV_ETABLI, DecisionEvenements.SUJET_PROCES_VERBAL),
        Map.entry(DecisionEvenements.PV_SIGNE, DecisionEvenements.SUJET_PROCES_VERBAL),
        Map.entry(DecisionEvenements.PV_SIGNATURE_ENREGISTREE, DecisionEvenements.SUJET_PROCES_VERBAL),
        Map.entry(DecisionEvenements.DOSSIER_CHANGEMENT_CONSTITUE, DecisionEvenements.SUJET_APPROBATION),
        Map.entry(DecisionEvenements.FICHE_CHANGEMENT_REFERENCEE, DecisionEvenements.SUJET_APPROBATION),
        Map.entry(DecisionEvenements.ATTESTATION_DEPOSEE, DecisionEvenements.SUJET_APPROBATION),
        Map.entry(DecisionEvenements.ATTESTATION_VERIFIEE, DecisionEvenements.SUJET_APPROBATION),
        Map.entry(DecisionEvenements.ATTESTATIONS_ARCHIVEES, DecisionEvenements.SUJET_APPROBATION),
        Map.entry(DecisionEvenements.VISA_SIGNE, DecisionEvenements.SUJET_APPROBATION),
        Map.entry(DecisionEvenements.SECURITE_APPROUVEE, DecisionEvenements.SUJET_APPROBATION)
    );

    /**
     * Sujet nommé par le contrat s'il existe ; sinon règle générale : homologation.operation-closed.v1 vers
     * homologation.operation, certification.quitus-signed.v1 vers certification.quitus.
     */
    public String topicFor(String eventType) {
        String sujet = SUJETS.get(eventType);
        if (sujet != null) {
            return sujet;
        }
        String[] parts = eventType.split("\\.");
        if (parts.length < 3 || parts[1].indexOf('-') < 0) {
            throw new IllegalArgumentException("Type d'événement mal formé : " + eventType);
        }
        String aggregate = parts[1].substring(0, parts[1].indexOf('-'));
        return parts[0] + "." + aggregate;
    }
}
