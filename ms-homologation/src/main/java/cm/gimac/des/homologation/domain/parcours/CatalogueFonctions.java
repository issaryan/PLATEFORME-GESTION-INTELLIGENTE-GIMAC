package cm.gimac.des.homologation.domain.parcours;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Catalogue des fonctions des rôles R, V, C, I, E et des déposants (D-91, docs/lot1/types-projet.md §2) : données
 * de MS-HOMOLOGATION (table {@code referentiel_fonction}), lues par le domaine sans Spring. Sans source branchée
 * (tests unitaires), les seize fonctions reprises de l'ancienne énumération. La source est relue au plus tard
 * après {@link #FRAICHEUR} (autres instances du service), et aussitôt après une écriture de cette instance.
 */
public final class CatalogueFonctions {

    /**
     * Fonction du catalogue : libellé, profils IAM qui la confèrent (le premier sert à l'annuaire), tiers, active, et
     * permissions qui la caractérisent (D-94 : candidats d'une fonction à affecter, lus par permission dans MS-IAM ;
     * vide : la fonction à affecter reste à la main du conducteur).
     */
    public record Definition(String code, String libelle, List<String> profils, boolean externe, boolean actif, List<String> permissions) {
        public Definition {
            profils = profils == null ? List.of() : List.copyOf(profils);
            permissions = permissions == null ? List.of() : List.copyOf(permissions);
        }

        public Definition(String code, String libelle, List<String> profils, boolean externe, boolean actif) {
            this(code, libelle, profils, externe, actif, List.of());
        }
    }

    public static final Duration FRAICHEUR = Duration.ofSeconds(30);

    /** Les seize fonctions de l'ancienne énumération, avec leurs profils, leur caractère externe et leurs libellés. */
    public static final List<Definition> REPRISES = List.of(
        new Definition("DDRM", "Développement du Réseau et du Marché", List.of("ROLE_CHARGE_DDRM"), false, true, List.of("HOM_FUNCTION_DDRM")),
        new Definition("CHEF_DDRM", "Chef du département DDRM", List.of("ROLE_CHEF_DDRM"), false, true, List.of("PAR_TRANSITION_APPROVE")),
        new Definition("CHEF_DES", "Chef du département DES", List.of("ROLE_CHEF_DES"), false, true, List.of("CER_QUITUS_TECHNIQUE_SIGN")),
        new Definition("CHEFS_DEPARTEMENT", "Chefs des deux départements", List.of("ROLE_CHEF_DES"), false, true, List.of("CER_QUITUS_TECHNIQUE_SIGN", "PAR_TRANSITION_APPROVE")),
        new Definition("CONDUCTEUR", "Conducteur de projet", List.of("ROLE_CONDUCTEUR_PROJET"), false, true, List.of("HOM_PROJECT_CONDUCT")),
        new Definition("SNA", "Normes et Agréments", List.of("ROLE_SNA", "ROLE_INGENIEUR_HOMOLOGATION"), false, true, List.of("CER_DOSSIER_VERIFY")),
        new Definition("PRODUCTION_MONETIQUE", "Production monétique", List.of("ROLE_PRODUCTION_MONETIQUE"), false, true, List.of("HOM_FUNCTION_PRODUCTION")),
        new Definition("POLE_RESEAU", "Pôle réseau", List.of("ROLE_POLE_RESEAU"), false, true, List.of("HOM_FUNCTION_NETWORK")),
        new Definition("POLE_SYSTEME", "Pôle système", List.of("ROLE_POLE_SYSTEME"), false, true, List.of("HOM_FUNCTION_SYSTEM")),
        new Definition("RSSI", "Sécurité des systèmes d'information", List.of("ROLE_RSSI"), false, true, List.of("CER_SECURITY_VERIFY")),
        new Definition("CONFORMITE", "Conformité", List.of("ROLE_CONFORMITE"), false, true, List.of("CER_QUITUS_CONFORMITE_SIGN")),
        new Definition("DG", "Direction générale", List.of("ROLE_DG"), false, true, List.of("CER_QUITUS_DG_SIGN")),
        // Exploitation et supervision (CR §3) : tenue par la production monétique, aucun profil propre (D-58) ; sa
        // permission (D-97) est donnée par défaut au profil de la production monétique.
        new Definition("EXPLOITATION", "Exploitation et Supervision", List.of("ROLE_PRODUCTION_MONETIQUE"), false, true, List.of("HOM_FUNCTION_OPERATIONS")),
        new Definition("PARTICIPANT", "Participant", List.of("ROLE_CORRESPONDANT"), true, true),
        new Definition("BANQUE_PARTENAIRE", "Banque partenaire", List.of(), true, true),
        new Definition("PERSONNALISATEUR", "Personnalisateur", List.of(), true, true)
    );

    private static volatile Supplier<List<Definition>> source;
    private static volatile Map<String, Definition> cache;
    private static volatile long chargeLe;
    private static volatile boolean perime = true;

    private CatalogueFonctions() {}

    /** Branche la source du catalogue (dépôt de MS-HOMOLOGATION) ; {@code null} revient aux fonctions reprises. */
    public static void brancher(Supplier<List<Definition>> s) {
        source = s;
        cache = null;
        invalider();
    }

    /** La prochaine lecture relit la source (écriture validée ou annulée, données effacées) ; la dernière lecture sert si elle échoue. */
    public static void invalider() {
        perime = true;
    }

    /** Écriture de cette instance : visible aussitôt, avant la relecture qui suit la transaction. */
    public static void retenir(Definition d) {
        Map<String, Definition> m = new LinkedHashMap<>(courant());
        m.put(d.code(), d);
        cache = ordonner(m.values());
        chargeLe = System.nanoTime();
        perime = false;
    }

    /** Toutes les fonctions, actives ou non : les reprises dans leur ordre, puis les autres par code. */
    public static List<Definition> toutes() {
        return List.copyOf(courant().values());
    }

    public static Optional<Definition> trouver(String code) {
        return Optional.ofNullable(courant().get(code));
    }

    /** Définition d'une fonction ; inconnue du catalogue : son code pour libellé, sans profil, inactive. */
    public static Definition definition(Fonction f) {
        return trouver(f.name()).orElseGet(() -> new Definition(f.name(), f.name(), List.of(), false, false));
    }

    /** La fonction figure au catalogue et elle est active. */
    public static boolean active(Fonction f) {
        return trouver(f.name()).map(Definition::actif).orElse(false);
    }

    private static Map<String, Definition> courant() {
        Map<String, Definition> c = cache;
        if (c != null && !perime && System.nanoTime() - chargeLe < FRAICHEUR.toNanos()) {
            return c;
        }
        Supplier<List<Definition>> s = source;
        List<Definition> lues = REPRISES;
        if (s != null) {
            try {
                lues = s.get();
            } catch (RuntimeException e) {
                // Source momentanément illisible : la dernière lecture, sinon les fonctions reprises.
                lues = c != null ? new ArrayList<>(c.values()) : REPRISES;
            }
        }
        Map<String, Definition> m = ordonner(lues);
        cache = m;
        chargeLe = System.nanoTime();
        perime = false;
        return m;
    }

    private static Map<String, Definition> ordonner(java.util.Collection<Definition> defs) {
        List<Definition> l = new ArrayList<>(defs);
        l.sort(Comparator.comparing((Definition d) -> Fonction.valueOf(d.code())));
        Map<String, Definition> m = new LinkedHashMap<>();
        l.forEach(d -> m.put(d.code(), d));
        return m;
    }
}
