package cm.gimac.des.pilotage.domain.service;

import cm.gimac.des.pilotage.domain.processus.Anomalie;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.Livrable;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contrôles structurels de publication d'un processus (§1), indépendants de l'organisation : au moins une
 * étape, codes d'étapes uniques, prédécesseurs connus, au moins une étape sans prédécesseur, graphe sans cycle
 * (Kahn) ; intervenant complet (profil pour PROFIL, personne pour PERSONNE, remplacement pour PERSONNE et
 * AU_LANCEMENT) ; validateur distinct de l'intervenant nommé ; livrables à codes uniques dans le processus,
 * signatures et champs à codes uniques dans l'étape ; signature portant au moins un livrable, de son étape ou
 * d'une étape antérieure ; champ CHOIX avec ses choix. Les contrôles liés à l'organisation (unité, profil
 * existant, personne active, couverture des profils) sont faits par le cas d'utilisation.
 */
public final class ControleProcessus {

    /** Rôle d'un intervenant dans une étape, rendu dans {@code element} d'une anomalie. */
    public static final String INTERVENANT = "intervenant";
    public static final String VALIDATEUR = "validateur";

    private ControleProcessus() {}

    /** Intervenant d'une étape avec son rôle : intervenant, validateur ou signature.CODE. */
    public record Role(String etape, String element, Intervenant intervenant) {}

    public static List<Anomalie> structure(List<Etape> etapes) {
        List<Anomalie> a = new ArrayList<>();
        if (etapes == null || etapes.isEmpty()) {
            a.add(Anomalie.bloquante("AUCUNE_ETAPE", null, null, "Le processus n'a aucune étape"));
            return a;
        }
        Map<String, Long> occurrences = etapes.stream().collect(Collectors.groupingBy(Etape::code, Collectors.counting()));
        occurrences.forEach((c, n) -> {
            if (n > 1) {
                a.add(Anomalie.bloquante("CODE_DUPLIQUE", c, null, "Code d'étape en double"));
            }
        });
        Set<String> codes = occurrences.keySet();
        for (Etape e : etapes) {
            for (String p : e.predecesseurs()) {
                if (!codes.contains(p)) {
                    a.add(Anomalie.bloquante("PREDECESSEUR_INCONNU", e.code(), p, "Prédécesseur inconnu : " + p));
                }
            }
        }
        if (GrapheProcessus.initiales(etapes).isEmpty()) {
            a.add(Anomalie.bloquante("AUCUNE_ETAPE_INITIALE", null, null, "Aucune étape sans prédécesseur"));
        }
        GrapheProcessus.Ordre ordre = GrapheProcessus.kahn(etapes);
        if (!ordre.enCycle().isEmpty()) {
            a.add(Anomalie.bloquante("CYCLE", ordre.enCycle().get(0), String.join(",", ordre.enCycle()), "Le graphe des étapes contient un cycle : " + String.join(", ", ordre.enCycle())));
        }
        for (Role r : roles(etapes)) {
            intervenant(r, a);
        }
        Set<String> livrables = new HashSet<>();
        for (Etape e : etapes) {
            if (e.validateur() != null && e.intervenant() != null && e.validateur().mode() == ModeIntervenant.PERSONNE && e.intervenant().mode() == ModeIntervenant.PERSONNE &&
                e.validateur().personneId() != null && e.validateur().personneId().equals(e.intervenant().personneId())) {
                a.add(Anomalie.bloquante("VALIDATEUR_IDENTIQUE", e.code(), VALIDATEUR, "Le validateur doit être distinct de l'intervenant nommé"));
            }
            for (Livrable l : e.livrables()) {
                if (!livrables.add(l.code())) {
                    a.add(Anomalie.bloquante("LIVRABLE_CODE_DUPLIQUE", e.code(), l.code(), "Code de livrable en double dans le processus"));
                }
            }
            doublons(e.signatures(), SignatureRequise::code).forEach(c -> a.add(Anomalie.bloquante("SIGNATURE_CODE_DUPLIQUE", e.code(), c, "Code de signature en double")));
            doublons(e.champs(), Champ::code).forEach(c -> a.add(Anomalie.bloquante("CHAMP_CODE_DUPLIQUE", e.code(), c, "Code de champ en double")));
            for (Champ c : e.champs()) {
                if (c.type() == TypeChamp.CHOIX && c.choix().isEmpty()) {
                    a.add(Anomalie.bloquante("CHAMP_CHOIX_ABSENT", e.code(), c.code(), "Un champ CHOIX énumère ses choix"));
                }
            }
            signatures(etapes, e, a);
        }
        return a;
    }

    /** Intervenants, validateurs et signataires de toutes les étapes, avec leur rôle. */
    public static List<Role> roles(List<Etape> etapes) {
        List<Role> roles = new ArrayList<>();
        for (Etape e : etapes) {
            roles.add(new Role(e.code(), INTERVENANT, e.intervenant()));
            if (e.validateur() != null) {
                roles.add(new Role(e.code(), VALIDATEUR, e.validateur()));
            }
            e.signatures().forEach(s -> roles.add(new Role(e.code(), "signature." + s.code(), s.signataire())));
        }
        return roles;
    }

    private static void intervenant(Role r, List<Anomalie> a) {
        Intervenant i = r.intervenant();
        if (i == null || i.mode() == null) {
            a.add(Anomalie.bloquante("INTERVENANT_ABSENT", r.etape(), r.element(), "Intervenant non désigné"));
            return;
        }
        if (i.mode() == ModeIntervenant.PROFIL && vide(i.profil())) {
            a.add(Anomalie.bloquante("INTERVENANT_INCOMPLET", r.etape(), r.element(), "Profil exigé pour un intervenant PROFIL"));
        }
        if (i.mode() == ModeIntervenant.PERSONNE && vide(i.personneId())) {
            a.add(Anomalie.bloquante("INTERVENANT_INCOMPLET", r.etape(), r.element(), "Personne exigée pour un intervenant PERSONNE"));
        }
        if (i.mode().exigeRemplacement() && vide(i.remplacement())) {
            a.add(Anomalie.bloquante("REMPLACEMENT_ABSENT", r.etape(), r.element(), "Profil de remplacement exigé pour " + i.mode()));
        }
    }

    private static void signatures(List<Etape> etapes, Etape e, List<Anomalie> a) {
        Set<String> admis = new HashSet<>();
        e.livrables().forEach(l -> admis.add(l.code()));
        Set<String> anterieures = GrapheProcessus.anterieures(etapes, e.code());
        etapes.stream().filter(x -> anterieures.contains(x.code())).forEach(x -> x.livrables().forEach(l -> admis.add(l.code())));
        for (SignatureRequise s : e.signatures()) {
            if (s.porte().isEmpty()) {
                a.add(Anomalie.bloquante("SIGNATURE_SANS_LIVRABLE", e.code(), s.code(), "Une signature porte sur au moins un livrable"));
            }
            for (String l : s.porte()) {
                if (!admis.contains(l)) {
                    a.add(Anomalie.bloquante("SIGNATURE_PORTEE_INVALIDE", e.code(), s.code(), "Le livrable " + l + " n'est ni de l'étape ni d'une étape antérieure"));
                }
            }
        }
    }

    private static <T> List<String> doublons(List<T> l, Function<T, String> code) {
        Set<String> vus = new HashSet<>();
        return l.stream().map(code).filter(c -> !vus.add(c)).distinct().toList();
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
