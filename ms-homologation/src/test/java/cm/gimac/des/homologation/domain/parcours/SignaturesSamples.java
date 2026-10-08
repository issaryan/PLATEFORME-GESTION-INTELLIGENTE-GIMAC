package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Jeu d'essai des signatures d'une opération (D-91) : type générique, opération OUV (livrable L1, juriste consulté)
 * puis SIG (livrable L1) qui exige VISA_JURIDIQUE (juriste, ordre 1, porte L1 et OUV/L1) puis APPROBATION
 * (conducteur, ordre 2, porte L1).
 */
public final class SignaturesSamples {

    public static final TypeProjet TYPE = TypeProjet.valueOf("AGREGATEUR");
    public static final Fonction JURISTE = Fonction.valueOf("JURISTE");
    public static final UUID CONDUCTEUR = UUID.fromString("00000000-0000-4000-8000-0000000c0001");
    public static final LocalDate J = LocalDate.parse("2026-10-06");

    private SignaturesSamples() {}

    /** Le juriste rejoint le catalogue (fonction interne, profil ROLE_JURISTE). */
    public static void brancherCatalogue() {
        List<CatalogueFonctions.Definition> l = new java.util.ArrayList<>(CatalogueFonctions.REPRISES);
        l.add(new CatalogueFonctions.Definition("JURISTE", "Juriste", List.of("ROLE_JURISTE"), false, true));
        CatalogueFonctions.brancher(() -> l);
    }

    public static ReferenceOperationData donnees(String etape, Fonction r, Fonction v, List<Fonction> consultes, List<String> preds, List<SignatureOperation> signatures) {
        return new ReferenceOperationData(
            "Opération",
            null,
            Segment.TRONC_COMMUN_AMONT,
            null,
            null,
            null,
            null,
            null,
            preds,
            null,
            null,
            null,
            null,
            etape,
            1,
            r,
            v,
            consultes,
            List.of(),
            List.of(),
            2,
            GrainPreuve.OPERATION,
            null,
            false,
            false,
            false,
            false,
            null,
            List.of(new Livrable("L1", "Compte rendu", NatureLivrable.LIVRABLE, r, List.of("application/pdf"), true)),
            Geste.DEPOSER_LIVRABLE,
            null,
            null,
            null,
            null,
            signatures
        );
    }

    public static List<SignatureOperation> signatures() {
        return List.of(
            new SignatureOperation("APPROBATION", "Approbation du conducteur", Fonction.CONDUCTEUR, 2, List.of("L1")),
            new SignatureOperation("VISA_JURIDIQUE", "Visa juridique", JURISTE, 1, List.of("L1", "OUV/L1"))
        );
    }

    public static ReferentialVersion version(List<SignatureOperation> signatures) {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(TYPE, "Agrégateur", Instant.parse("2026-10-01T00:00:00Z"));
        v.setId(UUID.randomUUID());
        v.upsertOperation("OUV", donnees("LANCEMENT", Fonction.CONDUCTEUR, Fonction.CHEF_DES, List.of(JURISTE), List.of(), null));
        v.upsertOperation("SIG", donnees("LANCEMENT", Fonction.CONDUCTEUR, Fonction.CHEF_DES, List.of(), List.of("OUV"), signatures));
        v.setNumero(1);
        return v;
    }

    /** Désignation : le conducteur est un compte, les autres fonctions restent à affecter. */
    public static Parcours.Designation designation() {
        return f -> Fonction.CONDUCTEUR.equals(f) ? new Personne(CONDUCTEUR.toString(), "Conducteur de projet 1", Fonction.CONDUCTEUR) : Personne.aAffecter(f);
    }

    public static Parcours parcours() {
        ReferentialVersion v = version(signatures());
        Liaison l = new Liaison()
            .typeProjet(TYPE)
            .institutionId(UUID.randomUUID())
            .institutionNom("Agrégateur Alpha")
            .etat(EtatLiaison.OUVERTE)
            .chefDeProjetId(CONDUCTEUR)
            .dateOuverture(Instant.parse("2026-10-01T08:00:00Z"))
            .cycleAptitude(0)
            .version(v);
        l.setId(UUID.randomUUID());
        ParcoursLiaison e = new ParcoursLiaison(l.getId(), "LIA-2026-091", J);
        return Parcours.instancier(l, e, v.getOperations(), List.of(), designation());
    }

    public static Preuve piece(String doc) {
        return Preuve.piece(doc, doc + ".pdf", "a".repeat(64), J, "Conducteur");
    }
}
