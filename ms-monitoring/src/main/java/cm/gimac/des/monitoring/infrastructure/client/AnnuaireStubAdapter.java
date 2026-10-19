package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut de l'annuaire (développement, tests ; jamais en production) : équipe fictive du DES (jeu D-30 :
 * USR-0007 chef du DES, USR-0008 chef de quart, USR-0042 opérateur), à identifiants stables. Les tests peuvent
 * remplacer l'équipe ({@link #definir}).
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.annuaire", havingValue = "stub", matchIfMissing = true)
public class AnnuaireStubAdapter implements AnnuairePort {

    /** Compte fictif : identifiant, nom et permissions MON_*. */
    public record Compte(String id, String nom, Set<String> permissions) {}

    public static final List<Compte> EQUIPE_DEMO = List.of(
        new Compte("00000000-0000-4000-8000-000000000007", "USR-0007 Chef du DES", Set.of("MON_ROSTER_MANAGE", "MON_ROSTER_PUBLISH", "MON_SHIFT_LEAD", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000008", "USR-0008 Chef de quart", Set.of("MON_SHIFT_OPERATOR", "MON_SHIFT_LEAD", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000009", "USR-0009 Cheffe de quart", Set.of("MON_SHIFT_OPERATOR", "MON_SHIFT_LEAD", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000010", "USR-0010 Chef de quart", Set.of("MON_SHIFT_OPERATOR", "MON_SHIFT_LEAD", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000042", "USR-0042 Opérateur", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000043", "USR-0043 Opératrice", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000044", "USR-0044 Opérateur", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000045", "USR-0045 Opératrice", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000046", "USR-0046 Opérateur", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000047", "USR-0047 Opératrice", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000048", "USR-0048 Opérateur", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000049", "USR-0049 Opératrice", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000050", "USR-0050 Opérateur", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2")),
        new Compte("00000000-0000-4000-8000-000000000051", "USR-0051 Opératrice", Set.of("MON_SHIFT_OPERATOR", "MON_ONCALL_L2"))
    );

    private volatile List<Compte> equipe = EQUIPE_DEMO;
    private final Map<String, String> nomsSupplementaires = new HashMap<>();

    /** Remplace l'équipe fictive (tests). */
    public void definir(List<Compte> nouvelle) {
        this.equipe = List.copyOf(nouvelle);
    }

    public void reinitialiser() {
        this.equipe = EQUIPE_DEMO;
        nomsSupplementaires.clear();
    }

    @Override
    public List<Personne> porteurs(String permission) {
        List<Personne> l = new ArrayList<>();
        for (Compte c : equipe) {
            if (c.permissions().contains(permission)) {
                l.add(new Personne(c.id(), c.nom()));
            }
        }
        return l;
    }

    @Override
    public Map<String, String> noms(Collection<String> ids) {
        Map<String, String> tous = new LinkedHashMap<>();
        equipe.forEach(c -> tous.put(c.id(), c.nom()));
        Map<String, String> r = new HashMap<>();
        for (String id : ids) {
            if (id != null && tous.containsKey(id)) {
                r.put(id, tous.get(id));
            }
        }
        return r;
    }
}
