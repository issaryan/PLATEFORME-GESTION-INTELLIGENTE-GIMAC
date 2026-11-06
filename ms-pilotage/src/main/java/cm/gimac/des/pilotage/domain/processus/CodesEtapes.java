package cm.gimac.des.pilotage.domain.processus;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Codes d'étape générés par le système (D-136) : dans un modèle saisi, la clé envoyée pour une étape nouvelle n'est
 * que provisoire et devient {CODE_PROCESSUS}-E{N} ; les prédécesseurs suivent la réécriture, si bien que le graphe
 * reste le même. Un code déjà connu de la famille (brouillon courant, versions antérieures) est gardé : les modèles
 * existants et les modèles provisionnés par le système ne changent pas. Le préfixe est tronqué pour tenir dans les
 * 40 caractères d'un code d'étape ; le numéro, jamais.
 */
public final class CodesEtapes {

    static final int LONGUEUR_MAX = 40;

    private CodesEtapes() {}

    public static List<Etape> reecrire(String codeProcessus, List<Etape> etapes, Set<String> connus) {
        Pattern genere = Pattern.compile("^" + Pattern.quote(codeProcessus) + "-E(\\d+)$");
        long n = 0;
        Set<String> tous = new HashSet<>(connus);
        etapes.forEach(e -> tous.add(e.code()));
        for (String c : tous) {
            Matcher m = genere.matcher(c);
            if (m.matches()) {
                n = Math.max(n, Long.parseLong(m.group(1)));
            }
        }
        Map<String, String> nouveaux = new HashMap<>();
        for (int i = 0; i < etapes.size(); i++) {
            String cle = etapes.get(i).code();
            if (connus.contains(cle)) {
                continue;
            }
            if (nouveaux.containsKey(cle)) {
                throw PilotageException.champ("etapes[" + i + "].code", "Clé d'étape en double : " + cle);
            }
            nouveaux.put(cle, code(codeProcessus, ++n));
        }
        List<Etape> l = new ArrayList<>();
        for (Etape e : etapes) {
            List<String> preds = e.predecesseurs().stream().map(p -> nouveaux.getOrDefault(p, p)).toList();
            l.add(new Etape(nouveaux.getOrDefault(e.code(), e.code()), e.libelle(), e.action(), e.intervenant(), e.validateur(), preds, e.delaiJours(), e.livrables(), e.signatures(), e.champs(), e.position(), e.delaiMaxJours()));
        }
        return List.copyOf(l);
    }

    static String code(String codeProcessus, long numero) {
        String suffixe = "-E" + numero;
        String prefixe = codeProcessus.length() + suffixe.length() > LONGUEUR_MAX ? codeProcessus.substring(0, LONGUEUR_MAX - suffixe.length()) : codeProcessus;
        return prefixe + suffixe;
    }
}
