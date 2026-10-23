package cm.gimac.des.iam.infrastructure.idp;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import cm.gimac.des.iam.domain.service.Totp;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Annuaire de substitution en mémoire (profils dev et test, PI §0.8) : mêmes refus que l'adaptateur
 * Keycloak (identifiant pris, compte introuvable, politique des mots de passe et historique, code TOTP
 * vérifié). Jamais sélectionné en production. Il démarre avec un super administrateur et un RSSI de
 * démonstration pour que la console soit utilisable.
 */
public class AnnuaireEnMemoire implements Annuaire {

    private final Map<String, Compte> comptes = new ConcurrentHashMap<>();
    private final Map<String, List<SessionCompte>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> profils = new TreeMap<>();
    /** Libellé (attribut gimac_libelle) et description de chaque profil (D-86). */
    private final Map<String, String> libelles = new TreeMap<>();
    private final Map<String, String> descriptions = new TreeMap<>();
    /** Attribut gimac_externe posé sur un profil (D-94) ; absent : règle de repli *_PORTAL_*. */
    private final Map<String, Boolean> externes = new TreeMap<>();
    /** Mots de passe du substitut (le plus récent en tête), jamais exposés. */
    private final Map<String, Deque<String>> motsDePasse = new ConcurrentHashMap<>();
    private final Map<String, String> totp = new ConcurrentHashMap<>();
    private volatile PolitiqueSecurite politique = new PolitiqueSecurite(12, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
    private volatile ConfigurationMessagerie messagerie = new ConfigurationMessagerie("messagerie", 1025, "plateforme@gimac.test", "Plateforme Moabi du GIMAC", false, false, false, null, null, false);

    /** Profils du catalogue avec leurs permissions ; privilégiés par leurs permissions (IAM_*, PLATFORM_*, D-94). */
    public AnnuaireEnMemoire(Map<String, Set<String>> catalogue) {
        catalogue.forEach((k, v) -> profils.put(k, new TreeSet<>(v)));
        profils.keySet().forEach(k -> {
            if (LIBELLES_PAR_PROFIL.containsKey(k)) {
                libelles.put(k, LIBELLES_PAR_PROFIL.get(k));
            }
        });
        Instant t = Instant.parse("2026-09-01T08:00:00Z");
        ajouter(new Compte("demo-super-admin", "super.admin", "Super", "Administrateur", "super.admin@gimac.test", true, true, t, "+237600000001", "Administrateur de la plateforme", "fr", null, true, Set.of("ROLE_SUPER_ADMIN"), List.of(), true));
        ajouter(new Compte("demo-rssi", "rssi", "Responsable", "Sécurité", "rssi@gimac.test", true, true, t, "+237600000002", "RSSI", "fr", null, true, Set.of("ROLE_RSSI"), List.of(), true));
    }

    private void ajouter(Compte c) {
        comptes.put(c.id(), c);
        sessions.put(c.id(), new ArrayList<>());
    }

    private Compte exiger(String id) {
        Compte c = comptes.get(id);
        if (c == null) {
            throw IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id);
        }
        return c;
    }

    private void remplacer(Compte c) {
        comptes.put(c.id(), c);
    }

    /** Libellé du code TOTP enregistré (tests), vide sinon. */
    public Optional<String> libelleTotp(String id) {
        return Optional.ofNullable(totp.get(id));
    }

    @Override
    public List<Compte> lister() {
        return comptes.values().stream().sorted((a, b) -> a.identifiant().compareTo(b.identifiant())).toList();
    }

    @Override
    public Optional<Compte> lire(String id) {
        return Optional.ofNullable(comptes.get(id));
    }

    @Override
    public Optional<Compte> parIdentifiant(String identifiant) {
        return comptes.values().stream().filter(c -> c.identifiant().equals(identifiant)).findFirst();
    }

    @Override
    public Optional<Compte> parEmail(String email) {
        List<Compte> l = comptes.values().stream().filter(c -> email.equalsIgnoreCase(c.email())).toList();
        return l.size() == 1 ? Optional.of(l.get(0)) : Optional.empty();
    }

    @Override
    public boolean emailPris(String email, String saufId) {
        return comptes.values().stream().anyMatch(c -> email.equalsIgnoreCase(c.email()) && !c.id().equals(saufId));
    }

    @Override
    public String creer(NouveauCompte n, List<ActionRequise> actions) {
        if (parIdentifiant(n.identifiant()).isPresent()) {
            throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
        }
        String id = UUID.randomUUID().toString();
        ajouter(new Compte(id, n.identifiant(), n.prenom(), n.nom(), n.email(), false, true, Instant.now(), n.telephone(), n.fonction(), "fr", n.institutionId(), false, n.profils(), actions, false));
        return id;
    }

    @Override
    public void modifierFiche(String id, FicheCompte f) {
        Compte c = exiger(id);
        remplacer(new Compte(c.id(), c.identifiant(), val(f.prenom(), c.prenom()), val(f.nom(), c.nom()), val(f.email(), c.email()),
            f.email() == null || f.email().equalsIgnoreCase(c.email()) ? c.emailVerifie() : false, c.actif(), c.creeLe(), val(f.telephone(), c.telephone()),
            val(f.fonction(), c.fonction()), val(f.langue(), c.langue()), val(f.institutionId(), c.institutionId()), c.profilComplet(), c.profils(), c.actionsRequises(), c.secondFacteur(),
            c.unite(), c.responsableId()));
    }

    @Override
    public void marquerProfilComplet(String id, boolean complet) {
        Compte c = exiger(id);
        remplacer(avec(c, c.actif(), complet, c.profils(), c.actionsRequises(), c.secondFacteur()));
    }

    @Override
    public void definirProfils(String id, Set<String> p) {
        Compte c = exiger(id);
        remplacer(avec(c, c.actif(), c.profilComplet(), p, c.actionsRequises(), c.secondFacteur()));
    }

    @Override
    public void definirActif(String id, boolean actif) {
        Compte c = exiger(id);
        remplacer(avec(c, actif, c.profilComplet(), c.profils(), c.actionsRequises(), c.secondFacteur()));
        if (!actif) {
            sessions.get(id).clear();
        }
    }

    @Override
    public void supprimer(String id) {
        exiger(id);
        comptes.remove(id);
        sessions.remove(id);
        motsDePasse.remove(id);
        totp.remove(id);
    }

    @Override
    public void exigerActions(String id, Set<ActionRequise> actions) {
        Compte c = exiger(id);
        Set<ActionRequise> toutes = EnumSet.noneOf(ActionRequise.class);
        toutes.addAll(c.actionsRequises());
        toutes.addAll(actions);
        remplacer(avec(c, c.actif(), c.profilComplet(), c.profils(), List.copyOf(toutes), c.secondFacteur()));
    }

    @Override
    public void retirerActions(String id, Set<ActionRequise> actions) {
        Compte c = exiger(id);
        List<ActionRequise> restantes = c.actionsRequises().stream().filter(a -> !actions.contains(a)).toList();
        remplacer(avec(c, c.actif(), c.profilComplet(), c.profils(), restantes, c.secondFacteur()));
    }

    @Override
    public String definirMotDePasseProvisoire(String id) {
        exiger(id);
        String mdp = "Provisoire-" + UUID.randomUUID().toString().substring(0, 12) + "!A1";
        memoriser(id, mdp);
        return mdp;
    }

    @Override
    public void definirMotDePasse(String id, String motDePasse) {
        Compte c = exiger(id);
        List<String> violations = PolitiqueMotDePasse.violations(politique, motDePasse, c.identifiant());
        if (!violations.isEmpty()) {
            throw IamException.invalide("IAM_PASSWORD_POLICY", "Mot de passe non conforme à la politique de sécurité", Map.of("violations", violations));
        }
        Deque<String> historique = motsDePasse.getOrDefault(id, new ArrayDeque<>());
        if (historique.stream().limit(Math.max(politique.historique(), 1)).anyMatch(motDePasse::equals)) {
            throw IamException.invalide("IAM_PASSWORD_REUSED", "Le nouveau mot de passe doit différer des précédents", Map.of("violations", List.of("HISTORIQUE")));
        }
        memoriser(id, motDePasse);
    }

    private void memoriser(String id, String motDePasse) {
        Deque<String> h = motsDePasse.computeIfAbsent(id, k -> new ArrayDeque<>());
        h.addFirst(motDePasse);
        while (h.size() > 25) {
            h.removeLast();
        }
    }

    /** Échecs de connexion par compte et instant du dernier (protection contre la force brute, US-EXI-23). */
    private final Map<String, Integer> echecs = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Instant> derniersEchecs = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean verifierMotDePasse(String identifiant, String motDePasse) {
        Optional<Compte> c = parIdentifiant(identifiant).filter(Compte::actif);
        if (c.isEmpty()) {
            return false;
        }
        String id = c.orElseThrow().id();
        if (verrouillage(id).verrouille()) {
            return false;
        }
        boolean juste = motDePasse != null && motDePasse.equals(motsDePasse.getOrDefault(id, new ArrayDeque<>()).peekFirst());
        if (juste) {
            echecs.remove(id);
        } else {
            echecs.merge(id, 1, Integer::sum);
            derniersEchecs.put(id, Instant.now());
        }
        return juste;
    }

    @Override
    public cm.gimac.des.iam.domain.compte.Verrouillage verrouillage(String id) {
        exiger(id);
        int n = echecs.getOrDefault(id, 0);
        return new cm.gimac.des.iam.domain.compte.Verrouillage(politique.forceBruteActive() && n >= politique.forceBruteEchecsMax(), n, derniersEchecs.get(id));
    }

    @Override
    public void debloquer(String id) {
        exiger(id);
        echecs.remove(id);
    }

    @Override
    public void reinitialiserSecondFacteur(String id) {
        Compte c = exiger(id);
        totp.remove(id);
        remplacer(avec(c, c.actif(), c.profilComplet(), c.profils(), c.actionsRequises(), false));
    }

    @Override
    public void enregistrerTotp(String id, String secretBase32, String code, String libelle) {
        Compte c = exiger(id);
        if (!Totp.verifier(secretBase32, code, Instant.now(), politique.otpChiffres(), politique.otpPeriodeSecondes(), politique.otpAlgorithme(), 1)) {
            throw IamException.invalide("IAM_TOTP_CODE_INVALID", "Code de vérification incorrect", Map.of());
        }
        totp.put(id, libelle == null || libelle.isBlank() ? "Application d'authentification" : libelle);
        remplacer(avec(c, c.actif(), c.profilComplet(), c.profils(), c.actionsRequises(), true));
    }

    @Override
    public void marquerEmailVerifie(String id) {
        Compte c = exiger(id);
        remplacer(new Compte(c.id(), c.identifiant(), c.prenom(), c.nom(), c.email(), true, c.actif(), c.creeLe(), c.telephone(), c.fonction(), c.langue(),
            c.institutionId(), c.profilComplet(), c.profils(), c.actionsRequises(), c.secondFacteur(), c.unite(), c.responsableId()));
    }

    @Override
    public void definirRattachement(String id, String unite, String responsableId) {
        Compte c = exiger(id);
        remplacer(c.avecRattachement(vide(unite), vide(responsableId)));
    }

    @Override
    public List<Compte> parResponsable(String responsableId) {
        return comptes.values().stream().filter(c -> responsableId.equals(c.responsableId())).sorted((a, b) -> a.identifiant().compareTo(b.identifiant())).toList();
    }

    @Override
    public List<SessionCompte> sessions(String id) {
        exiger(id);
        return List.copyOf(sessions.get(id));
    }

    /** Ouvre une session fictive (tests et démonstration). */
    public String ouvrirSession(String id, String ip) {
        String s = UUID.randomUUID().toString();
        sessions.get(id).add(new SessionCompte(s, ip, Instant.now(), Instant.now(), List.of("web_app")));
        return s;
    }

    @Override
    public void fermerSessions(String id) {
        exiger(id);
        sessions.get(id).clear();
    }

    @Override
    public void fermerSession(String id, String sessionId) {
        exiger(id);
        if (!sessions.get(id).removeIf(s -> s.id().equals(sessionId))) {
            throw IamException.introuvable("IAM_SESSION_NOT_FOUND", sessionId);
        }
    }

    @Override
    public PolitiqueSecurite politique() {
        return politique;
    }

    @Override
    public void appliquerPolitique(PolitiqueSecurite p) {
        politique = p;
    }

    @Override
    public ConfigurationMessagerie messagerie() {
        return messagerie;
    }

    @Override
    public void appliquerMessagerie(ConfigurationMessagerie m) {
        boolean defini = m.motDePasse() != null && !m.motDePasse().isEmpty() || messagerie.motDePasseDefini();
        messagerie = new ConfigurationMessagerie(m.hote(), m.port(), m.expediteur(), m.nomExpediteur(), m.starttls(), m.ssl(), m.authentification(), m.utilisateur(), null, defini);
    }

    @Override
    public void testerMessagerie(String destinataire) {
        if (messagerie.hote() == null || messagerie.hote().isBlank()) {
            throw IamException.indisponible("IAM_SMTP_FAILED", "Le serveur de messagerie a refusé l'envoi d'essai");
        }
    }

    @Override
    public List<Compte> parUnite(String unite) {
        return comptes.values().stream().filter(c -> unite.equals(c.unite())).sorted((a, b) -> a.identifiant().compareTo(b.identifiant())).toList();
    }

    @Override
    public synchronized void creerProfil(String code, String libelle, String description, Set<String> permissions) {
        if (profils.containsKey(code)) {
            throw IamException.conflit("IAM_PROFIL_EXISTANT", "Un profil porte déjà ce code", Map.of("code", code));
        }
        profils.put(code, new TreeSet<>(permissions));
        presenter(code, libelle, description);
    }

    @Override
    public synchronized void supprimerProfil(String code) {
        profils.remove(code);
        externes.remove(code);
        libelles.remove(code);
        descriptions.remove(code);
    }

    @Override
    public synchronized void definirPresentationProfil(String code, String libelle, String description) {
        if (!profils.containsKey(code)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", code);
        }
        presenter(code, libelle, description);
    }

    private void presenter(String code, String libelle, String description) {
        if (libelle == null || libelle.isBlank()) {
            libelles.remove(code);
        } else {
            libelles.put(code, libelle);
        }
        if (description == null) {
            descriptions.remove(code);
        } else {
            descriptions.put(code, description);
        }
    }

    @Override
    public synchronized Map<String, PresentationProfil> presentationsProfils() {
        Map<String, PresentationProfil> m = new TreeMap<>();
        profils.keySet().forEach(code -> m.put(code, PresentationProfil.lue(code, libelles.get(code), descriptions.get(code))));
        return m;
    }

    @Override
    public synchronized Map<String, Set<String>> profilsParCompte() {
        Map<String, Set<String>> m = new TreeMap<>();
        comptes.values().forEach(c -> c.profils().stream().filter(profils::containsKey).forEach(p -> m.computeIfAbsent(c.id(), k -> new TreeSet<>()).add(p)));
        return m;
    }

    @Override
    public synchronized List<Profil> profils() {
        List<Profil> l = new ArrayList<>();
        profils.forEach((code, perms) ->
            l.add(
                new Profil(
                    code,
                    libelles.get(code),
                    descriptions.get(code),
                    perms,
                    ReglesProfils.privilegie(perms),
                    comptes.values().stream().filter(c -> c.profils().contains(code)).count(),
                    false,
                    null,
                    null,
                    ReglesProfils.externe(externes.get(code), perms)
                )
            )
        );
        return l;
    }

    @Override
    public synchronized Map<String, Boolean> attributsExternes() {
        return Map.copyOf(externes);
    }

    @Override
    public synchronized void definirExterne(String code, boolean externe) {
        if (!profils.containsKey(code)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", code);
        }
        externes.put(code, externe);
    }

    @Override
    public synchronized List<String> permissions() {
        Set<String> s = new TreeSet<>(PERMISSIONS_CONNUES);
        profils.values().forEach(s::addAll);
        return List.copyOf(s);
    }

    @Override
    public void definirPermissionsProfil(String profil, Set<String> permissions) {
        if (!profils.containsKey(profil)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", profil);
        }
        profils.put(profil, new TreeSet<>(permissions));
    }

    private static Compte avec(Compte c, boolean actif, boolean complet, Set<String> profils, List<ActionRequise> actions, boolean secondFacteur) {
        return new Compte(c.id(), c.identifiant(), c.prenom(), c.nom(), c.email(), c.emailVerifie(), actif, c.creeLe(), c.telephone(), c.fonction(), c.langue(),
            c.institutionId(), complet, profils, actions, secondFacteur, c.unite(), c.responsableId());
    }

    private static String vide(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String val(String nouveau, String ancien) {
        return nouveau == null ? ancien : nouveau;
    }

    /**
     * Catalogue des profils du substitut : profils attribuables de la configuration, avec les principales
     * permissions du realm (infra/keycloak/generer-realm-gimac.py) pour les profils connus, vides sinon.
     */
    public static Map<String, Set<String>> catalogueParDefaut(Set<String> profilsAttribuables) {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        profilsAttribuables.stream().sorted().forEach(p -> m.put(p, PERMISSIONS_PAR_PROFIL.getOrDefault(p, Set.of())));
        return m;
    }

    private static final Set<String> COMMUNES = Set.of("COL_MESSAGE_READ", "COL_MESSAGE_SEND", "NOT_INBOX_READ", "PIL_ACTIVITY_OWN");
    private static final Set<String> IAM = Set.of(
        "IAM_ACCOUNT_READ", "IAM_ACCOUNT_CREATE", "IAM_ACCOUNT_EDIT", "IAM_ACCOUNT_ROLES", "IAM_ACCOUNT_STATUS", "IAM_ACCOUNT_DELETE",
        "IAM_ACCOUNT_SECURITY", "IAM_JOURNAL_READ", "IAM_PROFILE_EDIT", "PLATFORM_SECURITY_POLICY_READ", "PLATFORM_SECURITY_POLICY_EDIT", "PLATFORM_MESSAGING_EDIT"
    );
    private static final Set<String> CHEF = Set.of("PIL_TEAM_READ", "PIL_TASK_ASSIGN");
    /**
     * « Administration des référentiels métier, tous domaines » (D-92, docs/lot1/referentiels-metier-admin.md) :
     * permission ordinaire, non privilégiée, attribuable à tout profil ; portée par défaut par les deux profils
     * qui la reçoivent dans le realm.
     */
    static final String REF_METIER_ADMIN = "REF_METIER_ADMIN";

    /** Permissions des fonctions opérationnelles du catalogue D-91 (D-97) : non privilégiées, rôles du realm livré. */
    static final Set<String> FONCTIONS_OPERATIONNELLES = Set.of(
        "HOM_FUNCTION_DDRM",
        "HOM_FUNCTION_PRODUCTION",
        "HOM_FUNCTION_NETWORK",
        "HOM_FUNCTION_SYSTEM",
        "HOM_FUNCTION_OPERATIONS"
    );

    private static Set<String> union(Set<?>... ensembles) {
        Set<String> s = new TreeSet<>();
        for (Set<?> e : ensembles) {
            e.forEach(x -> s.add((String) x));
        }
        return s;
    }

    /**
     * Permissions principales des profils livrés (extrait du realm du lot 1), dont la permission de chaque fonction
     * opérationnelle du catalogue D-91 (D-97 : HOM_FUNCTION_*, non privilégiées).
     */
    static final Map<String, Set<String>> PERMISSIONS_PAR_PROFIL = Map.ofEntries(
        Map.entry("ROLE_SUPER_ADMIN", union(COMMUNES, IAM, Set.of("IAM_ROLE_PRIVILEGED_GRANT", "PLATFORM_CONFIG_READ", "PLATFORM_CONFIG_EDIT", REF_METIER_ADMIN))),
        Map.entry("ROLE_RSSI", union(COMMUNES, IAM, CHEF)),
        Map.entry("ROLE_AGENT_SSI", union(COMMUNES, Set.of("HOM_DASHBOARD_READ"))),
        Map.entry("ROLE_CHEF_DES", union(COMMUNES, CHEF, Set.of("HOM_DASHBOARD_READ"))),
        Map.entry("ROLE_CHEF_DDRM", union(COMMUNES, CHEF, Set.of("PAR_INSTITUTION_READ"))),
        Map.entry("ROLE_DG", union(COMMUNES, CHEF, Set.of("CER_QUITUS_DG_SIGN"))),
        Map.entry("ROLE_CONFORMITE", union(COMMUNES, CHEF, Set.of("CER_QUITUS_CONFORMITE_SIGN"))),
        Map.entry("ROLE_RESPONSABLE_SERVICE", union(COMMUNES, CHEF)),
        Map.entry("ROLE_ADMIN_SYSTEME", union(COMMUNES, Set.of("PLATFORM_OPS_READ", "PLATFORM_OPS_EXECUTE"))),
        Map.entry("ROLE_COLLABORATEUR", COMMUNES),
        Map.entry("ROLE_INGENIEUR_HOMOLOGATION", union(COMMUNES, Set.of("HOM_DASHBOARD_READ", "HOM_OPERATION_VALIDATE", REF_METIER_ADMIN))),
        Map.entry("ROLE_SNA", union(COMMUNES, Set.of("HOM_DASHBOARD_READ"))),
        Map.entry("ROLE_PRODUCTION_MONETIQUE", union(COMMUNES, Set.of("HOM_DASHBOARD_READ", "HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_OPERATIONS"))),
        Map.entry("ROLE_POLE_RESEAU", union(COMMUNES, Set.of("HOM_DASHBOARD_READ", "HOM_FUNCTION_NETWORK"))),
        Map.entry("ROLE_POLE_SYSTEME", union(COMMUNES, Set.of("HOM_DASHBOARD_READ", "HOM_FUNCTION_SYSTEM"))),
        Map.entry("ROLE_CHARGE_DDRM", union(COMMUNES, Set.of("PAR_INSTITUTION_READ", "HOM_FUNCTION_DDRM"))),
        Map.entry("ROLE_CONDUCTEUR_PROJET", union(COMMUNES, Set.of("HOM_DASHBOARD_READ"))),
        Map.entry("ROLE_CORRESPONDANT", Set.of("HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT", "NOT_INBOX_READ")),
        // Rôles d'institution de la sandbox (D-115) : extrait des valeurs de départ du domaine livré.
        Map.entry("ROLE_SBX_ADMIN_INSTITUTION", Set.of("SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "SBX_RAC_CONSULTATION", "SBX_CER_CONSULTATION", "NOT_INBOX_READ")),
        Map.entry("ROLE_SBX_CHEF_PROJET", Set.of("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION", "SBX_MOB_SIMULATION", "SBX_MOB_CONSULTATION", "NOT_INBOX_READ")),
        Map.entry("ROLE_SBX_DEVELOPPEUR", Set.of("SBX_RAC_ADMINISTRATION", "SBX_RAC_CONSULTATION", "SBX_MOB_SIMULATION", "SBX_MOB_CONSULTATION", "NOT_INBOX_READ")),
        Map.entry("ROLE_SBX_TESTEUR", Set.of("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION", "SBX_MOB_EXECUTION", "SBX_MOB_CONSULTATION", "NOT_INBOX_READ")),
        Map.entry("ROLE_SBX_LECTEUR", Set.of("SBX_CER_CONSULTATION", "SBX_MOB_CONSULTATION", "NOT_INBOX_READ")),
        Map.entry("ROLE_SBX_PARTENAIRE", Set.of("SBX_RAC_ADMINISTRATION", "SBX_RAC_CONSULTATION", "SBX_CER_EXECUTION", "SBX_CER_CONSULTATION", "NOT_INBOX_READ"))
    );

    /** Libellés des profils livrés (infra/keycloak/generer-realm-gimac.py, attribut gimac_libelle). */
    static final Map<String, String> LIBELLES_PAR_PROFIL = Map.ofEntries(
        Map.entry("ROLE_SUPER_ADMIN", "Super administrateur"),
        Map.entry("ROLE_RSSI", "Sécurité des SI"),
        Map.entry("ROLE_AGENT_SSI", "Agent de la sécurité des SI"),
        Map.entry("ROLE_CHEF_DES", "Chef du DES"),
        Map.entry("ROLE_CHEF_DDRM", "Chef de la DDRM"),
        Map.entry("ROLE_DG", "Direction générale"),
        Map.entry("ROLE_CONFORMITE", "Conformité"),
        Map.entry("ROLE_RESPONSABLE_SERVICE", "Responsable de service"),
        Map.entry("ROLE_ADMIN_SYSTEME", "Administrateur système"),
        Map.entry("ROLE_COLLABORATEUR", "Collaborateur"),
        Map.entry("ROLE_INGENIEUR_HOMOLOGATION", "Ingénieur d'homologation"),
        Map.entry("ROLE_SNA", "Normes et Agréments"),
        Map.entry("ROLE_PRODUCTION_MONETIQUE", "Production monétique"),
        Map.entry("ROLE_POLE_RESEAU", "Pôle réseau"),
        Map.entry("ROLE_POLE_SYSTEME", "Pôle système"),
        Map.entry("ROLE_CHARGE_DDRM", "Chargé de la DDRM"),
        Map.entry("ROLE_CONDUCTEUR_PROJET", "Conducteur de projet"),
        Map.entry("ROLE_CORRESPONDANT", "Correspondant du participant")
    );

    /** Permissions connues du substitut, même sans profil qui les porte (dont les permissions internes). */
    /** Permissions de la Sandbox GIMAC (D-115) : SBX_<MODULE>_<FONCTION>, comme le domaine Keycloak livré. */
    static final Set<String> PERMISSIONS_SANDBOX = Set.copyOf(cm.gimac.des.iam.domain.service.ReglesSandbox.PERMISSIONS);

    static final Set<String> PERMISSIONS_CONNUES = Set.copyOf(
        union(Set.of("INTERNAL_IAM_READ", "PAR_INSTITUTION_READ", "HOM_DASHBOARD_READ", REF_METIER_ADMIN), FONCTIONS_OPERATIONNELLES, PERMISSIONS_SANDBOX)
    );
}
