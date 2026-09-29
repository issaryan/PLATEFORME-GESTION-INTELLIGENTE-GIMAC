package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Versions de référentiel de test (HOM-01). */
public final class ReferentialVersionSamples {

    private static final Instant T0 = Instant.parse("2026-10-05T08:00:00Z");

    private ReferentialVersionSamples() {}

    /** Brouillon complet et acyclique : amont, branche, aval. */
    public static ReferentialVersion brouillonComplet(TypeProjet type) {
        String p = prefixe(type);
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(type, "Référentiel " + type, T0);
        v.setId(UUID.randomUUID());
        v.upsertOperation(p + "-AMONT-01", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).data());
        v.upsertOperation(p + "-BR-01", ReferenceOperationSamples.complete(Segment.BRANCHE).avecPredecesseurs(p + "-AMONT-01").data());
        v.upsertOperation(p + "-AVAL-01", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AVAL).avecPredecesseurs(p + "-BR-01").data());
        return v;
    }

    public static ReferentialVersion publiee(TypeProjet type, int numero) {
        ReferentialVersion v = brouillonComplet(type);
        v.publier(numero, T0.plusSeconds(60), List.of());
        return v;
    }

    public static String prefixe(TypeProjet type) {
        return switch (type) {
            case CARTE_INTERBANCAIRE -> "CI";
            case PAIEMENT_MOBILE -> "PM";
            case DELEGATAIRE -> "DL";
        };
    }
}
