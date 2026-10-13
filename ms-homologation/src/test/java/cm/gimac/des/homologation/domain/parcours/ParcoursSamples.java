package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ConversionAnnexeA;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Jeux d'essai du parcours : référentiel de l'annexe A du CR en vigueur, v1.7
 * (config/liquibase/data/parcours_referentiel_cr_v17.csv), sa version fidèle à laquelle les liaisons sont
 * rattachées, liaisons et parcours instanciés depuis cette version.
 */
public final class ParcoursSamples {

    public static final LocalDate DEBUT = LocalDate.parse("2026-09-01");
    public static final UUID CONDUCTEUR = UUID.fromString("00000000-0000-4000-8000-0000000c0001");
    public static final UUID SNA = UUID.fromString("00000000-0000-4000-8000-0000000c0101");

    private ParcoursSamples() {}

    /** Référentiel de l'annexe A du CR en vigueur (v1.7). */
    public static List<ReferentielOperation> referentiel() {
        try (var in = ParcoursSamples.class.getResourceAsStream("/config/liquibase/data/parcours_referentiel_cr_v17.csv")) {
            String[] lignes = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n");
            List<ReferentielOperation> l = new ArrayList<>();
            for (int i = 1; i < lignes.length; i++) {
                if (lignes[i].isBlank()) {
                    continue;
                }
                String[] c = lignes[i].split(";", -1);
                ReferentielOperation o = new ReferentielOperation(
                    c[0],
                    Integer.parseInt(c[1]),
                    EtapeCode.valueOf(c[2]),
                    SegmentParcours.valueOf(c[3]),
                    Arrays.stream(c[4].split("\\|")).map(Fonction::valueOf).toList(),
                    Arrays.stream(c[5].split("\\|")).map(TypeProjet::valueOf).toList(),
                    c[13].isBlank() ? List.of() : Arrays.asList(c[13].split("\\|"))
                ).attributs(
                    c[6].isBlank() ? null : PlateformeExecution.valueOf(c[6]),
                    GrainPreuve.valueOf(c[7]),
                    Boolean.parseBoolean(c[8]),
                    Boolean.parseBoolean(c[9]),
                    Boolean.parseBoolean(c[10]),
                    Boolean.parseBoolean(c[11]),
                    Boolean.parseBoolean(c[12])
                );
                l.add(o);
            }
            return l;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Version du référentiel d'un type fidèle à l'annexe A du CR en vigueur (v1.7), numérotée 2 dans les jeux d'essai. */
    public static ReferentialVersion version1(TypeProjet type) {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(type, ConversionAnnexeA.EN_VIGUEUR.libelle(), Instant.parse("2026-09-01T00:00:00Z"));
        v.setId(UUID.randomUUID());
        ConversionAnnexeA.enVigueur(type).forEach(o -> v.upsertOperation(o.code(), o.data()));
        v.setNumero(2);
        return v;
    }

    public static Liaison liaison(TypeProjet type) {
        ReferentialVersion v = version1(type);
        Liaison l = new Liaison()
            .typeProjet(type)
            .institutionId(UUID.randomUUID())
            .institutionNom("Établissement de paiement A")
            .etat(EtatLiaison.OUVERTE)
            .chefDeProjetId(CONDUCTEUR)
            .dateOuverture(Instant.parse("2026-09-01T08:00:00Z"))
            .cycleAptitude(0)
            .version(v);
        l.setId(UUID.randomUUID());
        return l;
    }

    /** Désignation de démonstration : le conducteur, le correspondant et une personne par fonction interne. */
    public static Parcours.Designation designation(Liaison l) {
        return f ->
            switch (f.name()) {
                case "CONDUCTEUR" -> new Personne(CONDUCTEUR.toString(), "Conducteur de projet 1", Fonction.CONDUCTEUR);
                case "PARTICIPANT" -> Personne.correspondant(l.getInstitutionId().toString(), l.getInstitutionNom());
                // Plus de marqueur fictif (D-93 §1) : sans institution liée, le personnalisateur reste à affecter.
                case "PERSONNALISATEUR" -> Personne.aAffecter(Fonction.PERSONNALISATEUR);
                case "SNA" -> new Personne(SNA.toString(), "Ingénieur SNA", Fonction.SNA);
                case "EXPLOITATION" -> Personne.aAffecter(Fonction.EXPLOITATION);
                case "CHEFS_DEPARTEMENT" -> new Personne("U-CHEF_DES", "Titulaire CHEF_DES", Fonction.CHEF_DES);
                default -> new Personne("U-" + f.name(), "Titulaire " + f.name(), f);
            };
    }

    public static Parcours parcours(TypeProjet type, String... services) {
        Liaison l = liaison(type);
        ParcoursLiaison e = new ParcoursLiaison(l.getId(), "LIA-2026-001", DEBUT);
        List<ServiceParcours> svc = Arrays.stream(services).map(s -> new ServiceParcours(l.getId(), s, CatalogueServices.libelle(s), true)).toList();
        return Parcours.instancier(l, e, l.getVersion().getOperations(), svc, designation(l));
    }

    /** Clôt dans l'ordre toutes les opérations jusqu'au rang donné compris (par leur geste le plus simple). */
    public static void avancer(Parcours p, String jusquA, LocalDate le) {
        for (OperationParcours o : p.operations()) {
            if (!o.faite()) {
                p.terminer(o, Preuve.piece("DOC-" + o.getRang(), "preuve-" + o.getRang() + ".pdf", "a".repeat(64), le, "Test"), le);
            }
            if (o.getRang().equals(jusquA)) {
                return;
            }
        }
    }
}
