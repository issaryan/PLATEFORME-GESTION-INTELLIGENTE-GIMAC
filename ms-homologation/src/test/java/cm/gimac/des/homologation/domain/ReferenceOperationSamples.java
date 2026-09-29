package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import java.util.List;
import java.util.Map;

/** Saisies d'opérations de test (HOM-01, HOM-02). */
public final class ReferenceOperationSamples {

    private ReferenceOperationSamples() {}

    /** Opération complète du segment donné, sans prédécesseur. */
    public static Saisie complete(Segment segment) {
        return new Saisie(
            new ReferenceOperationData(
                "Signature de la convention",
                "Adhésion",
                segment,
                Partie.PARTICIPANT,
                "TRANSVERSE",
                NatureCouverture.AUCUNE,
                Map.of("ETABLISSEMENT_PAIEMENT", "CONVENTION_SIGNEE"),
                null,
                List.of(),
                "DIRECTION",
                null
            )
        );
    }

    /** Saisie modifiable par petites touches, pour les cas d'incomplétude. */
    public record Saisie(ReferenceOperationData data) {
        public Saisie sansPartie() {
            return new Saisie(copie(d -> new ReferenceOperationData(d.libelle(), d.phase(), d.segment(), null, d.serviceCode(), d.natureCouverture(), d.preuveAttendue(), d.idScenario(), d.predecesseurs(), d.fonctionResponsable(), d.grille())));
        }

        public Saisie sansPreuveAttendue() {
            return new Saisie(copie(d -> new ReferenceOperationData(d.libelle(), d.phase(), d.segment(), d.partie(), d.serviceCode(), d.natureCouverture(), null, d.idScenario(), d.predecesseurs(), d.fonctionResponsable(), d.grille())));
        }

        public Saisie avecPredecesseurs(String... codes) {
            return new Saisie(copie(d -> new ReferenceOperationData(d.libelle(), d.phase(), d.segment(), d.partie(), d.serviceCode(), d.natureCouverture(), d.preuveAttendue(), d.idScenario(), List.of(codes), d.fonctionResponsable(), d.grille())));
        }

        private ReferenceOperationData copie(java.util.function.UnaryOperator<ReferenceOperationData> f) {
            return f.apply(data);
        }
    }
}
