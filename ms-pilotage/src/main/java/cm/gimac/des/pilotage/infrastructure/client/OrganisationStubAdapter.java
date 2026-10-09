package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution de l'organisation (développement, tests) : une petite hiérarchie du GIMAC
 * (DG, chef du DES, chef de service et ses agents, chef de la DDRM), un compte sans responsable, un
 * compte désactivé, un correspondant d'institution et un super administrateur sans unité (D-92) ; les unités
 * du jeu initial et leurs responsables (D-81 §2) ; la matrice des pouvoirs par défaut (D-81 §3) : portées des responsables pour les chefs
 * (DG, chef du DES, cheffe de service, chef de la DDRM), AUCUNE pour les autres ; PROCESSUS_DEFINIR et
 * PROCESSUS_LANCER à UNITES pour les responsables (D-86 §4). Chaque compte porte ses profils (ROLE_…), qui
 * servent aux processus d'unité (D-87). Jeu de démonstration du pôle réseau (chef, deux ingénieurs réseau
 * portant ROLE_POLE_RESEAU) et service des ressources humaines (cheffe, gestionnaire, D-98), chargés avec
 * gimac.demonstration.processus=true (profil dev) ou par {@link #chargerDemonstrationReseau()} et
 * {@link #chargerDemonstrationRh()}. Les tests peuvent enregistrer d'autres comptes et unités, leurs
 * profils, et changer les pouvoirs d'un compte.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.organisation", havingValue = "stub", matchIfMissing = true)
public class OrganisationStubAdapter implements OrganisationPort {

    public static final String DG = "00000000-0000-4000-8000-0000000d0001";
    public static final String CHEF_DES = "00000000-0000-4000-8000-0000000d0002";
    public static final String CHEF_SERVICE = "00000000-0000-4000-8000-0000000d0003";
    public static final String AGENT = "00000000-0000-4000-8000-0000000d0004";
    public static final String AGENT_BIS = "00000000-0000-4000-8000-0000000d0005";
    public static final String CHEF_DDRM = "00000000-0000-4000-8000-0000000d0006";
    public static final String SANS_RESPONSABLE = "00000000-0000-4000-8000-0000000d0007";
    public static final String INACTIF = "00000000-0000-4000-8000-0000000d0008";
    public static final String CORRESPONDANT = "00000000-0000-4000-8000-0000000d0009";
    /**
     * Super administrateur (D-92) : compte interne sans unité ni responsable, sans aucun pouvoir de matrice ; ses
     * droits sur les processus de toutes les unités viennent seulement de la permission REF_METIER_ADMIN du jeton.
     */
    public static final String SUPER_ADMIN = "00000000-0000-4000-8000-0000000d0010";
    /** Jeu de démonstration du pôle réseau (processus d'unité, D-87). */
    public static final String CHEF_RESEAU = "00000000-0000-4000-8000-0000000d0011";
    public static final String INGENIEUR_RESEAU = "00000000-0000-4000-8000-0000000d0012";
    public static final String TECHNICIEN_RESEAU = "00000000-0000-4000-8000-0000000d0013";
    public static final String PROFIL_RESEAU = "ROLE_POLE_RESEAU";
    /** Service des ressources humaines de démonstration (demande de congé, D-98). */
    public static final String CHEF_RH = "00000000-0000-4000-8000-0000000d0014";
    public static final String GESTIONNAIRE_RH = "00000000-0000-4000-8000-0000000d0015";
    public static final String UNITE_RH = "RH";

    /** Profils connus de l'organisation simulée (profils internes du domaine gimac, domaine-gimac.json). */
    static final Set<String> PROFILS_CONNUS = Set.of(
        "ROLE_SUPER_ADMIN",
        "ROLE_ADMIN_SYSTEME",
        "ROLE_DG",
        "ROLE_CHEF_DES",
        "ROLE_CHEF_DDRM",
        "ROLE_CHARGE_DDRM",
        "ROLE_RESPONSABLE_SERVICE",
        "ROLE_COLLABORATEUR",
        "ROLE_SNA",
        "ROLE_INGENIEUR_HOMOLOGATION",
        "ROLE_PRODUCTION_MONETIQUE",
        "ROLE_POLE_RESEAU",
        "ROLE_POLE_SYSTEME",
        "ROLE_RSSI",
        "ROLE_CONFORMITE",
        "ROLE_CONDUCTEUR_PROJET"
    );

    /** Compte de l'organisation simulée. */
    public record Compte(String id, String nomAffiche, String fonction, String unite, String responsableId, boolean actif, boolean interne) {}

    /** Unité de l'organisation simulée : parent et responsable d'unité (D-81 §2). */
    public record Unite(String code, String parent, String responsableId) {}

    /** Portées par défaut des profils de responsables (D-81 §3, D-86 §4). */
    public static final Map<Pouvoir, Portee> MATRICE_RESPONSABLE = Collections.unmodifiableMap(
        new EnumMap<>(
            Map.of(
                Pouvoir.PROCESSUS_DEFINIR,
                Portee.UNITES,
                Pouvoir.PROCESSUS_LANCER,
                Portee.UNITES,
                Pouvoir.TACHE_CONFIER,
                Portee.UNITES,
                Pouvoir.TACHE_SUIVRE,
                Portee.UNITES,
                Pouvoir.ACTIVITE_LIRE,
                Portee.EQUIPE,
                Pouvoir.RAPPORT_VISER,
                Portee.EQUIPE,
                Pouvoir.RATTACHEMENT_MODIFIER,
                Portee.DIRECTS,
                Pouvoir.COMPTE_DEMANDER,
                Portee.UNITES,
                Pouvoir.PROFIL_DEMANDER,
                Portee.EQUIPE,
                Pouvoir.STATUT_DEMANDER,
                Portee.EQUIPE
            )
        )
    );

    static final List<String> PROFILS_DEMANDABLES = List.of("ROLE_COLLABORATEUR", "ROLE_RESPONSABLE_SERVICE");

    /** Profondeur de la chaîne des responsables (§1.3) et taille de l'équipe rendue. */
    static final int CHAINE_MAX = 10;
    static final int EQUIPE_MAX = 500;

    private final Map<String, Compte> comptes = new ConcurrentHashMap<>();
    private final Map<String, Unite> unites = new ConcurrentHashMap<>();
    private final Map<String, Map<Pouvoir, Portee>> matrices = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> profils = new ConcurrentHashMap<>();
    /** Composition des profils en permissions (D-94) : vide au départ, définie par les tests. */
    private final Map<String, Set<String>> compositions = new ConcurrentHashMap<>();
    private final boolean demonstration;

    public OrganisationStubAdapter() {
        this(false);
    }

    @Autowired
    public OrganisationStubAdapter(@Value("${gimac.demonstration.processus:false}") boolean demonstration) {
        this.demonstration = demonstration;
        reinitialiser();
    }

    /** Jeu initial ; les comptes, unités et pouvoirs changés par un test sont rétablis. */
    public final void reinitialiser() {
        comptes.clear();
        unites.clear();
        matrices.clear();
        profils.clear();
        compositions.clear();
        enregistrerUnite(new Unite("DG", null, DG));
        enregistrerUnite(new Unite("DES", "DG", CHEF_DES));
        enregistrerUnite(new Unite("SNA", "DES", CHEF_SERVICE));
        enregistrerUnite(new Unite("PROD_MONETIQUE", "DES", null));
        enregistrerUnite(new Unite("PROD_INFO", "DES", null));
        enregistrerUnite(new Unite("POLE_RESEAU", "PROD_INFO", null));
        enregistrerUnite(new Unite("POLE_SYSTEME", "PROD_INFO", null));
        enregistrerUnite(new Unite("DDRM", "DG", CHEF_DDRM));
        enregistrerUnite(new Unite("SSI", "DG", null));
        enregistrerUnite(new Unite("CONFORMITE", "DG", null));
        for (String chef : List.of(DG, CHEF_DES, CHEF_SERVICE, CHEF_DDRM)) {
            definirPouvoirs(chef, MATRICE_RESPONSABLE);
        }
        enregistrer(new Compte(DG, "Directeur Général", "Directeur Général", "DG", null, true, true));
        enregistrer(new Compte(CHEF_DES, "Paul Mbarga Atangana", "Chef du DES", "DES", DG, true, true));
        enregistrer(new Compte(CHEF_SERVICE, "Aminata Ngono", "Cheffe du service Normes et Agréments", "SNA", CHEF_DES, true, true));
        enregistrer(new Compte(AGENT, "Serge Ndzana", "Ingénieur d'homologation", "SNA", CHEF_SERVICE, true, true));
        enregistrer(new Compte(AGENT_BIS, "Clarisse Bilong", "Chargée d'études", "SNA", CHEF_SERVICE, true, true));
        enregistrer(new Compte(CHEF_DDRM, "Hervé Tchoua", "Chef de la DDRM", "DDRM", DG, true, true));
        enregistrer(new Compte(SANS_RESPONSABLE, "Jean Essomba", "Chargé de mission", null, null, true, true));
        enregistrer(new Compte(INACTIF, "Compte Désactivé", "Ancien agent", "SNA", CHEF_SERVICE, false, true));
        enregistrer(new Compte(CORRESPONDANT, "Jeanne Owona", "Correspondante", null, null, true, false));
        enregistrer(new Compte(SUPER_ADMIN, "Administrateur de la plateforme", "Super administrateur", null, null, true, true));
        attribuerProfils(DG, "ROLE_DG");
        attribuerProfils(CHEF_DES, "ROLE_CHEF_DES");
        attribuerProfils(CHEF_SERVICE, "ROLE_RESPONSABLE_SERVICE");
        attribuerProfils(AGENT, "ROLE_INGENIEUR_HOMOLOGATION");
        attribuerProfils(AGENT_BIS, "ROLE_SNA");
        attribuerProfils(CHEF_DDRM, "ROLE_CHEF_DDRM");
        attribuerProfils(SANS_RESPONSABLE, "ROLE_COLLABORATEUR");
        attribuerProfils(INACTIF, "ROLE_SNA");
        attribuerProfils(SUPER_ADMIN, "ROLE_SUPER_ADMIN");
        if (demonstration) {
            chargerDemonstrationReseau();
            chargerDemonstrationRh();
        }
    }

    /**
     * Service des ressources humaines de démonstration (D-98) : une cheffe de service (responsable de l'unité RH, n+1 :
     * le directeur général) et un gestionnaire, pour la demande de congé lancée de tout le GIMAC.
     */
    public final void chargerDemonstrationRh() {
        enregistrerUnite(new Unite(UNITE_RH, "DG", CHEF_RH));
        enregistrer(new Compte(CHEF_RH, "Solange Ateba Mvondo", "Cheffe du service des ressources humaines", UNITE_RH, DG, true, true));
        enregistrer(new Compte(GESTIONNAIRE_RH, "Thierry Bekolo Ngoa", "Gestionnaire des ressources humaines", UNITE_RH, CHEF_RH, true, true));
        attribuerProfils(CHEF_RH, "ROLE_RESPONSABLE_SERVICE");
        attribuerProfils(GESTIONNAIRE_RH, "ROLE_COLLABORATEUR");
        definirPouvoirs(CHEF_RH, MATRICE_RESPONSABLE);
    }

    /**
     * Pôle réseau de démonstration (D-87) : un chef de pôle (responsable de l'unité POLE_RESEAU, n+1 : chef du
     * DES) et deux agents portant ROLE_POLE_RESEAU, pour le processus « Ouverture d'un flux réseau ».
     */
    public final void chargerDemonstrationReseau() {
        enregistrerUnite(new Unite("POLE_RESEAU", "PROD_INFO", CHEF_RESEAU));
        enregistrer(new Compte(CHEF_RESEAU, "Brice Onana", "Chef du pôle réseau", "POLE_RESEAU", CHEF_DES, true, true));
        enregistrer(new Compte(INGENIEUR_RESEAU, "Ingrid Mballa", "Ingénieure réseau", "POLE_RESEAU", CHEF_RESEAU, true, true));
        enregistrer(new Compte(TECHNICIEN_RESEAU, "Yves Ateba", "Technicien réseau", "POLE_RESEAU", CHEF_RESEAU, true, true));
        attribuerProfils(CHEF_RESEAU, "ROLE_RESPONSABLE_SERVICE");
        attribuerProfils(INGENIEUR_RESEAU, PROFIL_RESEAU);
        attribuerProfils(TECHNICIEN_RESEAU, PROFIL_RESEAU);
        definirPouvoirs(CHEF_RESEAU, MATRICE_RESPONSABLE);
    }

    /** Profils du compte (remplace les précédents). */
    public void attribuerProfils(String compteId, String... codes) {
        profils.put(compteId, Set.of(codes));
    }

    /** Permissions qui composent le profil (remplace les précédentes), comme une composition de Keycloak (D-94). */
    public void composerProfil(String profil, String... permissions) {
        compositions.put(profil, Set.of(permissions));
    }

    /** Comptes internes actifs dont un profil compose la permission, comme GET /api/v1/interne/collaborateurs?permission=. */
    @Override
    public List<MembreProfil> porteursPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return List.of();
        }
        return comptes
            .values()
            .stream()
            .filter(c -> c.interne() && c.actif() && profilsDe(c.id()).stream().anyMatch(p -> compositions.getOrDefault(p, Set.of()).contains(permission)))
            .sorted(Comparator.comparing(Compte::id))
            .map(c -> new MembreProfil(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.actif(), profilsDe(c.id())))
            .toList();
    }

    @Override
    public Set<String> profilsDe(String compteId) {
        return compteId == null ? Set.of() : profils.getOrDefault(compteId, Set.of());
    }

    @Override
    public Optional<UniteMembres> membresUnite(String code, String profil) {
        Unite u = code == null ? null : unites.get(code);
        if (u == null) {
            return Optional.empty();
        }
        String p = profil == null || profil.isBlank() ? null : profil;
        boolean connu = p == null || PROFILS_CONNUS.contains(p) || profils.values().stream().anyMatch(s -> s.contains(p));
        List<MembreProfil> membres = comptes
            .values()
            .stream()
            .filter(c -> c.interne() && c.unite() != null && chemin(c.unite()).contains(code))
            .filter(c -> p == null || profilsDe(c.id()).contains(p))
            .sorted(Comparator.comparing(Compte::nomAffiche))
            .map(c -> new MembreProfil(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.actif(), profilsDe(c.id())))
            .limit(EQUIPE_MAX)
            .toList();
        return Optional.of(new UniteMembres(u.code(), u.code(), u.responsableId(), true, connu, membres));
    }

    @Override
    public List<MembreProfil> porteursProfil(String profil) {
        if (profil == null || profil.isBlank()) {
            return List.of();
        }
        return comptes
            .values()
            .stream()
            .filter(c -> c.interne() && c.actif() && profilsDe(c.id()).contains(profil))
            .sorted(Comparator.comparing(Compte::id))
            .map(c -> new MembreProfil(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.actif(), profilsDe(c.id())))
            .limit(100)
            .toList();
    }

    public void enregistrer(Compte c) {
        comptes.put(c.id(), c);
    }

    public void enregistrerUnite(Unite u) {
        unites.put(u.code(), u);
    }

    /** Portées du compte ; une action absente vaut AUCUNE. */
    public void definirPouvoirs(String compteId, Map<Pouvoir, Portee> actions) {
        EnumMap<Pouvoir, Portee> m = new EnumMap<>(Pouvoir.class);
        m.putAll(actions);
        matrices.put(compteId, m);
    }

    @Override
    public Pouvoirs pouvoirs(String compteId) {
        Map<Pouvoir, Portee> actions = matrices.getOrDefault(compteId, Map.of());
        if (actions.isEmpty()) {
            return new Pouvoirs(Map.of(), List.of(), List.of(), List.of());
        }
        Portee large = actions.values().stream().max(Comparator.naturalOrder()).orElse(Portee.AUCUNE);
        return new Pouvoirs(actions, PROFILS_DEMANDABLES, perimetre(compteId, large).comptes(), unitesDirigees(compteId));
    }

    @Override
    public Perimetre perimetre(String compteId, Portee portee) {
        if (portee == null || portee == Portee.AUCUNE) {
            return Perimetre.vide();
        }
        Map<String, Membre> couverts = new LinkedHashMap<>();
        switch (portee) {
            case DIRECTS -> equipe(compteId).stream().filter(m -> m.niveau() == 1).forEach(m -> couverts.put(m.id(), m));
            case EQUIPE -> equipe(compteId).forEach(m -> couverts.put(m.id(), m));
            default -> {
                equipe(compteId).forEach(m -> couverts.put(m.id(), m));
                Set<String> dirigees = Set.copyOf(unitesDirigees(compteId));
                comptes
                    .values()
                    .stream()
                    .filter(c -> c.interne() && c.unite() != null && dirigees.contains(c.unite()) && !c.id().equals(compteId))
                    .sorted(Comparator.comparing(Compte::nomAffiche))
                    .forEach(c -> couverts.putIfAbsent(c.id(), new Membre(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), 0, c.actif())));
            }
        }
        List<String> codes = portee == Portee.UNITES || portee == Portee.TOUTES ? unitesDirigees(compteId) : List.of();
        return new Perimetre(portee, couverts.values().stream().limit(EQUIPE_MAX).toList(), codes);
    }

    /** Unités dont le compte est responsable, et leurs sous-unités. */
    List<String> unitesDirigees(String compteId) {
        Set<String> r = new LinkedHashSet<>();
        Deque<String> file = new ArrayDeque<>();
        unites.values().stream().filter(u -> compteId.equals(u.responsableId())).map(Unite::code).sorted().forEach(file::add);
        while (!file.isEmpty()) {
            String code = file.poll();
            if (r.add(code)) {
                unites.values().stream().filter(u -> code.equals(u.parent())).map(Unite::code).sorted().forEach(file::add);
            }
        }
        return List.copyOf(r);
    }

    /** Codes des unités, de la plus haute à {@code code}. */
    private List<String> chemin(String code) {
        List<String> chemin = new ArrayList<>();
        for (Unite u = code == null ? null : unites.get(code); u != null && !chemin.contains(u.code()) && chemin.size() < CHAINE_MAX; u = u.parent() == null ? null : unites.get(u.parent())) {
            chemin.add(0, u.code());
        }
        if (chemin.isEmpty() && code != null) {
            chemin.add(code);
        }
        return chemin;
    }

    @Override
    public Optional<Rattachement> rattachement(String compteId) {
        Compte c = comptes.get(compteId);
        if (c == null || !c.interne()) {
            return Optional.empty();
        }
        Compte resp = c.responsableId() == null ? null : comptes.get(c.responsableId());
        List<String> chaine = new ArrayList<>();
        for (Compte r = resp; r != null && chaine.size() < CHAINE_MAX && !chaine.contains(r.id()); r = r.responsableId() == null ? null : comptes.get(r.responsableId())) {
            chaine.add(r.id());
        }
        Personne responsable = resp == null ? null : personne(resp);
        return Optional.of(new Rattachement(c.id(), c.nomAffiche(), c.unite(), responsable, chaine, chemin(c.unite())));
    }

    @Override
    public List<Membre> equipe(String compteId) {
        List<Membre> equipe = new ArrayList<>();
        Deque<Map.Entry<String, Integer>> file = new ArrayDeque<>();
        file.add(Map.entry(compteId, 0));
        while (!file.isEmpty() && equipe.size() < EQUIPE_MAX) {
            Map.Entry<String, Integer> e = file.poll();
            int niveau = e.getValue() + 1;
            if (niveau > CHAINE_MAX) {
                continue;
            }
            comptes
                .values()
                .stream()
                .filter(c -> e.getKey().equals(c.responsableId()) && !c.id().equals(compteId))
                .sorted(Comparator.comparing(Compte::nomAffiche))
                .forEach(c -> {
                    if (equipe.stream().noneMatch(m -> m.id().equals(c.id())) && equipe.size() < EQUIPE_MAX) {
                        equipe.add(new Membre(c.id(), c.nomAffiche(), c.fonction(), c.unite(), c.responsableId(), niveau, c.actif()));
                        file.add(Map.entry(c.id(), niveau));
                    }
                });
        }
        return List.copyOf(equipe);
    }

    @Override
    public Optional<Personne> collaborateur(String compteId) {
        return Optional.ofNullable(compteId == null ? null : comptes.get(compteId)).map(OrganisationStubAdapter::personne);
    }

    private static Personne personne(Compte c) {
        return new Personne(c.id(), c.nomAffiche(), c.fonction(), c.actif(), c.interne());
    }
}
