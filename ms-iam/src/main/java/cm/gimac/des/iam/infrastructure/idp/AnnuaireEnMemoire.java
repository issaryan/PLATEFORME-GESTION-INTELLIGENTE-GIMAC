package cm.gimac.des.iam.infrastructure.idp;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
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
    private final Set<String> privilegies;
    /** Mots de passe du substitut (le plus récent en tête), jamais exposés. */
    private final Map<String, Deque<String>> motsDePasse = new ConcurrentHashMap<>();
    private final Map<String, String> totp = new ConcurrentHashMap<>();
    private volatile PolitiqueSecurite politique = new PolitiqueSecurite(12, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
    private volatile ConfigurationMessagerie messagerie = new ConfigurationMessagerie("messagerie", 1025, "plateforme@gimac.test", "Plateforme DES du GIMAC", false, false, false, null, null, false);

    public AnnuaireEnMemoire(Map<String, Set<String>> catalogue, Set<String> privilegies) {
        this.privilegies = Set.copyOf(privilegies);
        catalogue.forEach((k, v) -> profils.put(k, new TreeSet<>(v)));
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
            val(f.fonction(), c.fonction()), val(f.langue(), c.langue()), val(f.institutionId(), c.institutionId()), c.profilComplet(), c.profils(), c.actionsRequises(), c.secondFacteur()));
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

    @Override
    public boolean verifierMotDePasse(String identifiant, String motDePasse) {
        return parIdentifiant(identifiant).filter(Compte::actif).map(c -> motsDePasse.getOrDefault(c.id(), new ArrayDeque<>()).peekFirst()).map(motDePasse::equals).orElse(false);
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
            c.institutionId(), c.profilComplet(), c.profils(), c.actionsRequises(), c.secondFacteur()));
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
    public List<Profil> profils() {
        List<Profil> l = new ArrayList<>();
        profils.forEach((code, perms) ->
            l.add(new Profil(code, null, perms, privilegies.contains(code), comptes.values().stream().filter(c -> c.profils().contains(code)).count()))
        );
        return l;
    }

    @Override
    public List<String> permissions() {
        return profils.values().stream().flatMap(Set::stream).distinct().sorted().toList();
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
            c.institutionId(), complet, profils, actions, secondFacteur);
    }

    private static String val(String nouveau, String ancien) {
        return nouveau == null ? ancien : nouveau;
    }

    /** Catalogue minimal des profils, quand la configuration n'en fournit pas. */
    public static Map<String, Set<String>> catalogueParDefaut(Set<String> profilsAttribuables) {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        profilsAttribuables.forEach(p -> m.put(p, Set.of()));
        return m;
    }
}
