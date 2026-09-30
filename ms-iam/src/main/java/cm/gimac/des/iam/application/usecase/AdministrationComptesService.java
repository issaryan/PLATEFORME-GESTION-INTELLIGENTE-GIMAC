package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration des comptes par le super administrateur et le RSSI (D-62) : création (aucune
 * inscription libre), fiche, profils, activation, suppression, actions de sécurité (changement de
 * mot de passe, second facteur), sessions. Chaque acte est journalisé et publié vers l'audit.
 */
@Service
@Transactional
public class AdministrationComptesService {

    /** Filtre de la liste : recherche libre, profil, état. */
    public record Filtre(String recherche, String profil, String etat) {}

    public record PageComptes(List<Compte> contenu, long total) {}

    /** Résultat d'une création : le mot de passe provisoire n'est rendu qu'une fois, s'il a été demandé. */
    public record Creation(Compte compte, boolean invitationEnvoyee, String motDePasseProvisoire) {}

    public record Indicateurs(
        long total,
        long actifs,
        long inactifs,
        long premierAccesEnAttente,
        long sansSecondFacteur,
        long privilegies,
        long creesDepuis30Jours,
        long actesDepuis24Heures,
        Map<String, Long> parProfil
    ) {}

    private final Annuaire annuaire;
    private final JournalActes journal;
    private final JournalAdministrationRepository entrees;
    private final IamProprietes proprietes;
    private final Clock clock;

    public AdministrationComptesService(Annuaire annuaire, JournalActes journal, JournalAdministrationRepository entrees, IamProprietes proprietes, Clock clock) {
        this.annuaire = annuaire;
        this.journal = journal;
        this.entrees = entrees;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ lectures

    @Transactional(readOnly = true)
    public PageComptes lister(Filtre f, int page, int taille) {
        String r = f.recherche() == null ? "" : f.recherche().trim().toLowerCase(Locale.ROOT);
        List<Compte> l = annuaire.lister().stream()
            .filter(c -> r.isEmpty() || contient(c.identifiant(), r) || contient(c.nomAffiche(), r) || contient(c.email(), r))
            .filter(c -> f.profil() == null || f.profil().isBlank() || c.profils().contains(f.profil()))
            .filter(c -> etat(c, f.etat()))
            .sorted(Comparator.comparing(Compte::identifiant))
            .toList();
        int t = Math.clamp(taille, 1, 100);
        int debut = Math.min(Math.max(page, 0) * t, l.size());
        return new PageComptes(l.subList(debut, Math.min(debut + t, l.size())), l.size());
    }

    /** Page de l'annuaire interne, avec son numéro et sa taille effectifs. */
    public record PageAnnuaire(List<Compte> contenu, long total, int page, int taille) {}

    /**
     * Annuaire des comptes internes actifs (D-64) : sans institution, filtrés par nom, courriel ou
     * fonction, triés par nom affiché. Destinataires de la messagerie de MS-COLLABORATION.
     */
    @Transactional(readOnly = true)
    public PageAnnuaire annuaireInterne(String recherche, int page, int taille) {
        String r = recherche == null ? "" : recherche.trim().toLowerCase(Locale.ROOT);
        List<Compte> l = annuaire.lister().stream()
            .filter(c -> c.actif() && (c.institutionId() == null || c.institutionId().isBlank()))
            .filter(c -> r.isEmpty() || contient(c.nomAffiche(), r) || contient(c.email(), r) || contient(c.fonction(), r))
            .sorted(Comparator.comparing((Compte c) -> c.nomAffiche().toLowerCase(Locale.ROOT)).thenComparing(Compte::id))
            .toList();
        int t = Math.clamp(taille, 1, 100);
        int p = Math.max(page, 0);
        int debut = (int) Math.min((long) p * t, l.size());
        return new PageAnnuaire(l.subList(debut, Math.min(debut + t, l.size())), l.size(), p, t);
    }

    @Transactional(readOnly = true)
    public Compte lire(String id) {
        return annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id));
    }

    @Transactional(readOnly = true)
    public List<SessionCompte> sessions(String id) {
        lire(id);
        return annuaire.sessions(id);
    }

    @Transactional(readOnly = true)
    public Indicateurs indicateurs() {
        List<Compte> l = annuaire.lister();
        Instant il30Jours = clock.instant().minus(Duration.ofDays(30));
        Map<String, Long> parProfil = new java.util.TreeMap<>();
        l.forEach(c -> c.profils().forEach(p -> parProfil.merge(p, 1L, Long::sum)));
        Set<String> privilegies = proprietes.profils().privilegies();
        return new Indicateurs(
            l.size(),
            l.stream().filter(Compte::actif).count(),
            l.stream().filter(c -> !c.actif()).count(),
            l.stream().filter(c -> c.actif() && c.premierAccesEnAttente()).count(),
            l.stream().filter(c -> c.actif() && !c.secondFacteur()).count(),
            l.stream().filter(c -> c.profils().stream().anyMatch(privilegies::contains)).count(),
            l.stream().filter(c -> c.creeLe() != null && c.creeLe().isAfter(il30Jours)).count(),
            entrees.countByHorodatageAfter(clock.instant().minus(Duration.ofHours(24))),
            parProfil
        );
    }

    // ------------------------------------------------------------------ écritures

    public Creation creer(Acteur acteur, NouveauCompte n, boolean envoyerInvitation, boolean motDePasseProvisoire) {
        ReglesAdministration.verifierIdentifiant(n.identifiant());
        ReglesAdministration.verifierTelephone(n.telephone());
        if (n.email() == null || n.email().isBlank()) {
            throw IamException.invalide("IAM_EMAIL_REQUIRED", "Courriel obligatoire", Map.of());
        }
        if (!envoyerInvitation && !motDePasseProvisoire) {
            throw IamException.invalide("IAM_ONBOARDING_REQUIRED", "Choisir l'invitation par courriel ou un mot de passe provisoire", Map.of());
        }
        ReglesAdministration.verifierProfils(acteur, Set.of(), n.profils(), proprietes.profils().attribuables(), proprietes.profils().privilegies());
        if (annuaire.parIdentifiant(n.identifiant()).isPresent() || annuaire.emailPris(n.email(), null)) {
            throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
        }
        List<ActionRequise> actions = proprietes.premiereConnexion().actions();
        String id = annuaire.creer(n, actions);
        String mdp = motDePasseProvisoire ? annuaire.definirMotDePasseProvisoire(id) : null;
        if (envoyerInvitation) {
            annuaire.envoyerInvitation(id, actions, proprietes.premiereConnexion().validiteInvitation());
        }
        journal.compte(acteur, "CREATION", id, n.identifiant(), null, List.copyOf(new TreeSet<>(n.profils())));
        return new Creation(lire(id), envoyerInvitation, mdp);
    }

    public Compte modifier(Acteur acteur, String id, FicheCompte f) {
        Compte c = lire(id);
        ReglesAdministration.verifierTelephone(f.telephone());
        if (f.email() != null && !f.email().equalsIgnoreCase(c.email()) && annuaire.emailPris(f.email(), id)) {
            throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
        }
        annuaire.modifierFiche(id, f);
        journal.compte(acteur, "FICHE", id, c.identifiant(), null, List.of());
        return lire(id);
    }

    public Compte definirProfils(Acteur acteur, String id, Set<String> profils, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        ReglesAdministration.verifierProfils(acteur, c.profils(), profils, proprietes.profils().attribuables(), proprietes.profils().privilegies());
        if (c.profils().contains(ReglesAdministration.SUPER_ADMIN) && !profils.contains(ReglesAdministration.SUPER_ADMIN)) {
            ReglesAdministration.verifierDernierSuperAdmin(c.actif(), superAdminsActifs());
        }
        annuaire.definirProfils(id, profils);
        journal.compte(acteur, "PROFILS", id, c.identifiant(), motif, List.copyOf(new TreeSet<>(profils)));
        return lire(id);
    }

    public Compte definirActif(Acteur acteur, String id, boolean actif, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        if (!actif && c.profils().contains(ReglesAdministration.SUPER_ADMIN)) {
            ReglesAdministration.verifierDernierSuperAdmin(c.actif(), superAdminsActifs());
        }
        annuaire.definirActif(id, actif);
        journal.compte(acteur, actif ? "ACTIVATION" : "DESACTIVATION", id, c.identifiant(), motif, List.of());
        return lire(id);
    }

    public void supprimer(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        if (c.profils().contains(ReglesAdministration.SUPER_ADMIN)) {
            ReglesAdministration.verifierDernierSuperAdmin(c.actif(), superAdminsActifs());
        }
        annuaire.supprimer(id);
        journal.compte(acteur, "SUPPRESSION", id, c.identifiant(), motif, List.of());
    }

    /** Demande à l'utilisateur, à sa prochaine connexion, de changer son mot de passe ou de configurer son second facteur. */
    public Compte exigerActions(Acteur acteur, String id, Set<ActionRequise> actions, boolean parCourriel, String motif) {
        ReglesAdministration.exigerMotif(motif);
        if (actions == null || actions.isEmpty()) {
            throw IamException.invalide("IAM_ACTIONS_REQUIRED", "Au moins une action est requise", Map.of());
        }
        Compte c = lire(id);
        annuaire.exigerActions(id, actions);
        if (parCourriel) {
            annuaire.envoyerInvitation(id, List.copyOf(EnumSet.copyOf(actions)), proprietes.premiereConnexion().validiteInvitation());
        }
        journal.compte(acteur, "ACTIONS_REQUISES", id, c.identifiant(), motif, actions.stream().map(Enum::name).sorted().toList());
        return lire(id);
    }

    public Compte reinitialiserSecondFacteur(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        annuaire.reinitialiserSecondFacteur(id);
        annuaire.fermerSessions(id);
        journal.compte(acteur, "REINITIALISATION_2FA", id, c.identifiant(), motif, List.of());
        return lire(id);
    }

    public String motDePasseProvisoire(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        String mdp = annuaire.definirMotDePasseProvisoire(id);
        annuaire.fermerSessions(id);
        journal.compte(acteur, "MOT_DE_PASSE_PROVISOIRE", id, c.identifiant(), motif, List.of());
        return mdp;
    }

    public void fermerSessions(Acteur acteur, String id, String motif) {
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        annuaire.fermerSessions(id);
        journal.compte(acteur, "FERMETURE_SESSIONS", id, c.identifiant(), motif, List.of());
    }

    // ------------------------------------------------------------------ outils

    private long superAdminsActifs() {
        return annuaire.lister().stream().filter(c -> c.actif() && c.profils().contains(ReglesAdministration.SUPER_ADMIN)).count();
    }

    private static boolean contient(String v, String r) {
        return v != null && v.toLowerCase(Locale.ROOT).contains(r);
    }

    private static boolean etat(Compte c, String etat) {
        if (etat == null || etat.isBlank()) return true;
        return switch (etat) {
            case "ACTIF" -> c.actif();
            case "INACTIF" -> !c.actif();
            case "PREMIER_ACCES" -> c.actif() && c.premierAccesEnAttente();
            case "SANS_2FA" -> c.actif() && !c.secondFacteur();
            default -> throw IamException.invalide("IAM_FILTER_INVALID", "État de filtre inconnu", Map.of("etat", etat));
        };
    }
}
