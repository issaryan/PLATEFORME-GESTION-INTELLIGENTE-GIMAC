package cm.gimac.des.collaboration.infrastructure.client;

import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution de l'annuaire (développement, tests) : quelques acteurs internes du
 * GIMAC, un correspondant de participant et un compte désactivé, qui ne sont pas joignables.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.annuaire", havingValue = "stub", matchIfMissing = true)
public class CollaboratorStubAdapter implements CollaboratorDirectoryPort {

    public static final String CHEF_DES = "00000000-0000-4000-8000-00000000c001";
    public static final String CONDUCTEUR = "00000000-0000-4000-8000-00000000c002";
    public static final String INGENIEUR = "00000000-0000-4000-8000-00000000c003";
    public static final String RSSI = "00000000-0000-4000-8000-00000000c004";
    public static final String DDRM = "00000000-0000-4000-8000-00000000c005";
    public static final String CORRESPONDANT = "00000000-0000-4000-8000-00000000c006";
    public static final String INACTIF = "00000000-0000-4000-8000-00000000c007";

    private final Map<String, Collaborateur> collaborateurs = new ConcurrentHashMap<>();

    public CollaboratorStubAdapter() {
        enregistrer(new Collaborateur(CHEF_DES, "Paul Mbarga Atangana", "Chef du DES", Set.of("ROLE_CHEF_DES"), null, true));
        enregistrer(new Collaborateur(CONDUCTEUR, "Aminata Ngono", "Conductrice de projet", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true));
        enregistrer(new Collaborateur(INGENIEUR, "Serge Ndzana", "Ingénieur d'homologation", Set.of("ROLE_INGENIEUR_HOMOLOGATION"), null, true));
        enregistrer(new Collaborateur(RSSI, "Clarisse Bilong", "Responsable de la sécurité des SI", Set.of("ROLE_RSSI"), null, true));
        enregistrer(new Collaborateur(DDRM, "Hervé Tchoua", "Chef de la DDRM", Set.of("ROLE_DDRM"), null, true));
        enregistrer(new Collaborateur(CORRESPONDANT, "Jeanne Owona", "Correspondante", Set.of("ROLE_CORRESPONDANT"), "11111111-1111-4111-8111-111111111111", true));
        enregistrer(new Collaborateur(INACTIF, "Compte Désactivé", "Ancien agent", Set.of("ROLE_SNA"), null, false));
    }

    public void enregistrer(Collaborateur c) {
        collaborateurs.put(c.id(), c);
    }

    @Override
    public PageAnnuaire rechercher(String recherche, int page, int taille) {
        String r = recherche == null ? "" : recherche.strip().toLowerCase(Locale.ROOT);
        List<Collaborateur> l = collaborateurs
            .values()
            .stream()
            .filter(Collaborateur::joignable)
            .filter(c -> r.isEmpty() || contient(c.nom(), r) || contient(c.fonction(), r))
            .sorted(Comparator.comparing(Collaborateur::nom))
            .toList();
        int debut = Math.min(page * taille, l.size());
        return new PageAnnuaire(l.subList(debut, Math.min(debut + taille, l.size())), l.size(), page, taille);
    }

    @Override
    public Optional<Collaborateur> trouver(String id) {
        return Optional.ofNullable(collaborateurs.get(id));
    }

    @Override
    public Map<String, String> versionsAvatar(Collection<String> ids) {
        Map<String, String> v = new HashMap<>();
        if (ids != null) {
            ids.forEach(id -> {
                Collaborateur c = id == null ? null : collaborateurs.get(id);
                if (c != null && c.avatarVersion() != null) {
                    v.put(id, c.avatarVersion());
                }
            });
        }
        return v;
    }

    private static boolean contient(String valeur, String motif) {
        return valeur != null && valeur.toLowerCase(Locale.ROOT).contains(motif);
    }
}
