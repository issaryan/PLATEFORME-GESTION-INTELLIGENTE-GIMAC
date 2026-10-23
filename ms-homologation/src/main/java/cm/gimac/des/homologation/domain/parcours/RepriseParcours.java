package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.HomologationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reprise de l'avancement d'un projet existant sur son parcours (D-107, docs/exploitation/reprise-des-donnees.md §4).
 * L'état de chaque opération du référentiel publié est posé tel que la source le donne, sans passer par les gestes
 * (signatures, livrables, verrous) qui ont eu lieu hors de la plateforme ; une opération faite reçoit une preuve
 * marquée « reprise ». Le moteur recalcule ensuite tout le reste : opérations suivantes ouvertes, statut du CR,
 * étape courante, automate technique, blocage. Service de domaine pur, sans Spring.
 */
public final class RepriseParcours {

    /** États repris d'une opération. */
    public enum EtatRepris {
        FAITE,
        EN_COURS,
        A_FAIRE,
        SANS_OBJET,
        BLOQUEE,
    }

    /**
     * Avancement d'une liaison reprise : à qualifier (aucun état connu, le moteur ouvre les premières opérations),
     * détaillé opération par opération, ou en production (opérations jusqu'au jalon EN_PRODUCTION faites).
     */
    public enum Avancement {
        A_QUALIFIER,
        DETAILLE,
        EN_PRODUCTION,
    }

    /**
     * État repris d'une opération : date de début (blocage), date de fin, commentaire, preuve facultative (pièce déjà
     * déposée dans MS-DOCUMENT par sa référence et son empreinte, ou valeur de référence relisible) et échéance de
     * levée d'un blocage.
     */
    public record OperationReprise(
        String rang,
        EtatRepris etat,
        LocalDate debut,
        LocalDate faiteLe,
        String commentaire,
        ValeurReference valeurReference,
        String documentRef,
        String empreinte,
        LocalDate echeanceLevee
    ) {}

    /** Bilan : opérations dont l'état a changé, avertissements de cohérence. */
    public record Bilan(int modifiees, List<String> avertissements) {}

    /** Action d'un blocage repris : à préciser par le conducteur, l'échéance par défaut étant le jour de la reprise. */
    public static final String PROCHAINE_ACTION = "À préciser après la reprise de l'existant";

    private static final Set<EtatOperationParcours> TRAITEES_SUR_LA_PLATEFORME = EnumSet.of(
        EtatOperationParcours.EN_VALIDATION,
        EtatOperationParcours.EN_SIGNATURE
    );

    private RepriseParcours() {}

    /**
     * Applique l'avancement repris. {@code dateProduction} date les opérations faites par l'avancement EN_PRODUCTION ;
     * {@code dateCible} devient l'échéance de l'opération du jalon EN_PRODUCTION si elle n'est pas faite. Une opération
     * faite ou en cours de validation ou de signature sur la plateforme (preuve non reprise) n'est jamais réécrite.
     */
    public static Bilan appliquer(
        Parcours p,
        Avancement avancement,
        List<OperationReprise> operations,
        LocalDate dateProduction,
        LocalDate dateCible,
        String origine,
        String acteur,
        UUID acteurId,
        LocalDate aujourdhui
    ) {
        List<String> avertissements = new ArrayList<>();
        Map<String, OperationReprise> cibles = new LinkedHashMap<>();
        if (avancement == Avancement.EN_PRODUCTION) {
            OperationParcours jalon = p
                .parJalon(Jalon.EN_PRODUCTION)
                .orElseThrow(() -> HomologationException.unprocessable("HOM_REPRISE_JALON_ABSENT", "Le référentiel de cette liaison ne porte pas de jalon EN_PRODUCTION."));
            LocalDate le = dateProduction == null ? aujourdhui : dateProduction;
            String commentaire = "Repris à l'état final";
            p.operations(o -> o.getOrdre() <= jalon.getOrdre()).forEach(o ->
                cibles.put(o.getRang(), new OperationReprise(o.getRang(), EtatRepris.FAITE, null, le, commentaire, null, null, null, null))
            );
        }
        List<String> inconnues = new ArrayList<>();
        for (OperationReprise o : operations) {
            if (p.operation(o.rang()).isEmpty()) {
                inconnues.add(o.rang());
            } else {
                cibles.put(o.rang(), o);
            }
        }
        if (!inconnues.isEmpty()) {
            throw HomologationException.unprocessable(
                "HOM_REPRISE_OPERATION_INCONNUE",
                "Opérations absentes du référentiel de la liaison : " + String.join(", ", inconnues)
            );
        }
        int modifiees = 0;
        List<OperationReprise> bloquees = new ArrayList<>();
        for (OperationReprise cible : cibles.values()) {
            OperationParcours o = p.operation(cible.rang()).orElseThrow();
            if (cible.etat() == EtatRepris.BLOQUEE) {
                bloquees.add(cible);
            }
            if (identique(o, cible)) {
                continue;
            }
            exigerReprenable(o);
            poser(p, o, cible, origine, acteur, aujourdhui);
            modifiees++;
        }
        p.ouvrirSuivantes(aujourdhui);
        if (!bloquees.isEmpty() && !p.bloquee()) {
            OperationReprise b = bloquees.get(0);
            String motif = bloquees.stream().map(x -> x.rang() + (x.commentaire() == null ? "" : " : " + x.commentaire())).reduce((x, y) -> x + " ; " + y).orElse("");
            Personne responsable = p.conducteur() != null ? p.conducteur() : Personne.aAffecter(Fonction.CONDUCTEUR);
            p.declarerBlocage(
                tronquer("Blocage repris de l'existant, " + motif, 500),
                PROCHAINE_ACTION,
                responsable,
                b.echeanceLevee() == null ? aujourdhui : b.echeanceLevee(),
                acteurId,
                b.debut() == null ? aujourdhui : b.debut()
            );
            modifiees++;
        }
        if (dateCible != null) {
            p.parJalon(Jalon.EN_PRODUCTION).filter(o -> !o.faite()).ifPresent(o -> o.setEcheance(dateCible));
        }
        for (OperationParcours o : p.operations()) {
            List<String> avant = o
                .getPredecesseurs()
                .stream()
                .filter(r -> p.operation(r).map(x -> !x.faite() && x.getEtat() != EtatOperationParcours.SANS_OBJET).orElse(false))
                .toList();
            if (cibles.containsKey(o.getRang()) && o.faite() && !avant.isEmpty()) {
                avertissements.add(o.getRang() + " faite alors que " + String.join(", ", avant) + " ne l'est pas");
            }
        }
        long sansPiece = p.operations().stream().filter(o -> o.faite() && o.getPreuve() != null && o.getPreuve().estRepriseSansPiece()).count();
        if (sansPiece > 0) {
            avertissements.add(sansPiece + " opération(s) faite(s) reprise(s) sans pièce numérisée");
        }
        p.rafraichir();
        return new Bilan(modifiees, List.copyOf(avertissements));
    }

    /** L'opération est déjà dans l'état repris (une opération bloquée l'est par le blocage de la liaison). */
    static boolean identique(OperationParcours o, OperationReprise cible) {
        return switch (cible.etat()) {
            case FAITE -> o.getEtat() == EtatOperationParcours.FAITE;
            case SANS_OBJET -> o.getEtat() == EtatOperationParcours.SANS_OBJET;
            case EN_COURS -> o.getEtat() == EtatOperationParcours.EN_COURS;
            case A_FAIRE -> o.getEtat() == EtatOperationParcours.A_FAIRE;
            case BLOQUEE -> o.getEtat() == EtatOperationParcours.BLOQUEE;
        };
    }

    private static void exigerReprenable(OperationParcours o) {
        boolean faiteSurLaPlateforme = o.getEtat() == EtatOperationParcours.FAITE && (o.getPreuve() == null || !o.getPreuve().estReprise());
        if (faiteSurLaPlateforme || TRAITEES_SUR_LA_PLATEFORME.contains(o.getEtat())) {
            throw HomologationException.conflict(
                "HOM_REPRISE_OPERATION_TRAITEE",
                "Cette opération a été traitée sur la plateforme : la reprise ne la réécrit pas",
                Map.of("rang", o.getRang(), "etat", o.getEtat().name())
            );
        }
    }

    private static void poser(Parcours p, OperationParcours o, OperationReprise cible, String origine, String acteur, LocalDate aujourdhui) {
        switch (cible.etat()) {
            case FAITE -> {
                LocalDate le = cible.faiteLe() == null ? aujourdhui : cible.faiteLe();
                o.setEtat(EtatOperationParcours.FAITE);
                o.setFaiteLe(le);
                String piece = texte(cible.documentRef());
                // Sans pièce numérisée : marquée comme telle (D-107), seule exception au contrôle des preuves exigées (D-109).
                String commentaire = piece == null ? sansPiece(cible, origine) : commentaire(cible);
                o.setPreuve(Preuve.reprise(piece, texte(cible.empreinte()), le, acteur, cible.valeurReference(), commentaire));
                if (o.getParticularite() == Particularite.ENTREE_ESSAIS) {
                    p.servicesInclus().stream().filter(s -> s.getEntreeEssai() == null).forEach(s -> s.setEntreeEssai(le));
                }
                if (o.getParticularite() == Particularite.FIN_ESSAIS) {
                    p.certifierServices(le);
                }
            }
            case SANS_OBJET -> {
                o.setEtat(EtatOperationParcours.SANS_OBJET);
                o.setFaiteLe(null);
                o.setPreuve(Preuve.reprise(null, null, aujourdhui, acteur, null, commentaire(cible)));
            }
            case EN_COURS, A_FAIRE, BLOQUEE -> {
                // Bloquée : rouverte ici, puis bloquée avec les autres opérations ouvertes par le blocage de la liaison.
                o.setEtat(cible.etat() == EtatRepris.EN_COURS ? EtatOperationParcours.EN_COURS : EtatOperationParcours.A_FAIRE);
                o.setFaiteLe(null);
                o.setPreuve(cible.commentaire() == null && cible.valeurReference() == null ? null : Preuve.reprise(null, null, aujourdhui, acteur, cible.valeurReference(), commentaire(cible)));
            }
        }
    }

    private static String sansPiece(OperationReprise o, String origine) {
        String c = texte(o.commentaire());
        return tronquer(Preuve.SANS_PIECE + (origine == null ? "" : " (" + origine + ")") + (c == null ? "" : " : " + c), 500);
    }

    private static String commentaire(OperationReprise o) {
        String c = texte(o.commentaire());
        return tronquer("Reprise de l'existant" + (c == null ? "" : " : " + c), 500);
    }

    private static String texte(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String tronquer(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
