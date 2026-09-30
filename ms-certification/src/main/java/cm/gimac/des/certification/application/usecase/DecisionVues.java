package cm.gimac.des.certification.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Représentations servies à l'interface, champ pour champ celles de
 * frontend/packages/lot1/src/modele/parcours.ts (ProcesVerbalCertification, ApprobationSecurite, Signature,
 * Attestation, Visa, Preuve) et commun.ts (CoverageValue). Les dates « …Le » sont des jours (fuseau de
 * Douala), l'horodatage du cachet un instant ISO 8601.
 */
public final class DecisionVues {

    /** Fuseau des dates affichées (siège du GIMAC). */
    public static final ZoneId FUSEAU = ZoneId.of("Africa/Douala");

    private DecisionVues() {}

    public static LocalDate jour(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, FUSEAU);
    }

    public record CouvertureVue(Integer taux, int numerateur, int denominateur, boolean calculable, String motifNonCalculable) {
        public static CouvertureVue de(Integer taux, int numerateur, int denominateur) {
            boolean calculable = denominateur > 0;
            return new CouvertureVue(calculable ? taux : null, numerateur, denominateur, calculable, calculable ? null : "aucun service inclus");
        }
    }

    public record SignatureVue(String signataire, String fonction, LocalDate signeLe, Instant horodatage) {}

    public record ProcesVerbalVue(
        UUID liaisonId,
        String etat,
        CouvertureVue couverture,
        List<String> servicesCouverts,
        String etabliPar,
        LocalDate etabliLe,
        String empreinte,
        SignatureVue signatureGimac,
        SignatureVue signatureParticipant,
        List<String> transitionsPermises,
        List<String> actionsPermises
    ) {}

    public record PreuveVue(String documentRef, String nom, String empreinte, LocalDate deposeLe, String deposePar) {}

    public record FicheVue(String reference, String etatApprobation, LocalDate lueLe, boolean aRelire) {}

    public record AttestationVue(
        String type,
        boolean requise,
        PreuveVue preuve,
        LocalDate verifieeLe,
        String verifieePar,
        LocalDate archiveeLe,
        String motifRejet
    ) {}

    public record VisaVue(String nature, boolean actif, String etat, SignatureVue signature, String motif) {}

    public record ApprobationVue(
        UUID liaisonId,
        String etat,
        PreuveVue dossierChangement,
        FicheVue ficheChangement,
        LocalDate documentParametrageRecu,
        List<AttestationVue> attestations,
        boolean fintech,
        List<VisaVue> visas,
        LocalDate approuveeLe,
        List<String> transitionsPermises,
        List<String> actionsPermises
    ) {}
}
