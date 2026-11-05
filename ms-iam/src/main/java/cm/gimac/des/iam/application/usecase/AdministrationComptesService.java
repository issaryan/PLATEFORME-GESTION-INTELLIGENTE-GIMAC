package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Couche;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.FinaliteJeton;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration des comptes par le super administrateur et le RSSI (D-62) : création (aucune
 * inscription libre), fiche, profils, activation, suppression, actions de sécurité (changement de
 * mot de passe, second facteur), sessions. Chaque acte est journalisé et publié vers l'audit.
 *
 * <p>D-65 : les actions exigées sont celles de la plateforme (attribut gimac_actions_requises), jamais
 * celles de Keycloak ; invitations et rappels partent en courriels de la plateforme, avec un lien vers
 * l'application de la couche du compte.
 *
 * <p>D-94 : aucune règle sur un nom de profil. Le « dernier super administrateur » est le dernier compte actif
 * portant IAM_ROLE_PRIVILEGED_GRANT par la composition de ses profils ; le rattachement à une institution suit
 * l'attribut externe des profils ; la couche des liens suit les permissions de la console.
 */
@Service
@Transactional
public class AdministrationComptesService {

    private static final Logger LOG = LoggerFactory.getLogger(AdministrationComptesService.class);

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
        Map<String, Long> parProfil,
        List<ParJour> creationsParJour,
        List<ParJour> actesParJour
    ) {}

    /** Nombre d'événements d'un jour (fuseau de la plateforme). */
    public record ParJour(LocalDate jour, long nombre) {}

    /** Profondeur des séries quotidiennes des tableaux de bord. */
    public static final int JOURS_SERIES = 30;
    /** Fuseau des jours des séries : celui de la CEMAC (UTC+1, sans heure d'été). */
    public static final ZoneId FUSEAU = ZoneId.of("Africa/Douala");

    private final Annuaire annuaire;
    private final JournalActes journal;
    private final JournalAdministrationRepository entrees;
    private final IamProprietes proprietes;
    private final LiensCourriel liens;
    private final Clock clock;
    private final CatalogueProfils catalogue;
    private final cm.gimac.des.iam.application.usecase.sandbox.TraductionInstitution traduction;

    @Autowired
    public AdministrationComptesService(
        Annuaire annuaire,
        JournalActes journal,
        JournalAdministrationRepository entrees,
        IamProprietes proprietes,
        LiensCourriel liens,
        Clock clock,
        CatalogueProfils catalogue,
        cm.gimac.des.iam.application.usecase.sandbox.TraductionInstitution traduction
    ) {
        this.traduction = traduction;
        this.annuaire = annuaire;
        this.journal = journal;
        this.entrees = entrees;
        this.proprietes = proprietes;
        this.liens = liens;
        this.clock = clock;
        this.catalogue = catalogue;
    }

    /** Service sur un catalogue donné, sans registre des participants (tests). */
    public AdministrationComptesService(
        Annuaire annuaire,
        JournalActes journal,
        JournalAdministrationRepository entrees,
        IamProprietes proprietes,
        LiensCourriel liens,
        Clock clock,
        CatalogueProfils catalogue
    ) {
        this(annuaire, journal, entrees, proprietes, liens, clock, catalogue, cm.gimac.des.iam.application.usecase.sandbox.TraductionInstitution.sansRegistre(clock));
    }

    /** Service limité aux profils de la configuration (sans profil créé). */
    public AdministrationComptesService(
        Annuaire annuaire,
        JournalActes journal,
        JournalAdministrationRepository entrees,
        IamProprietes proprietes,
        LiensCourriel liens,
        Clock clock
    ) {
        this(annuaire, journal, entrees, proprietes, liens, clock, CatalogueProfils.configuration(proprietes, annuaire));
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

    /**
     * Comptes internes actifs dont un profil porte la permission (D-94 §3) : intervenants proposés par
     * MS-HOMOLOGATION. Composition lue dans le fournisseur d'identité (profils créés et recompositions compris).
     */
    @Transactional(readOnly = true)
    public List<Compte> porteursPermission(String permission) {
        return PorteursPermission.lire(annuaire, permission);
    }

    /**
     * Comptes actifs rattachés à une institution (D-93 §11) : destinataires des notifications d'une institution chez
     * MS-NOTIFICATION. Profils complétés par la lecture des profils par compte ; triés par nom affiché ; identifiant
     * nul ou blanc : aucun compte.
     */
    @Transactional(readOnly = true)
    public List<Compte> comptesInstitution(String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            return List.of();
        }
        // D-134 : institution désignée par son identifiant au registre ou par son code participant (MS-NOTIFICATION).
        List<Compte> l = traduction.comptes(annuaire, institutionId.strip()).stream().filter(Compte::actif).toList();
        if (l.isEmpty()) {
            return l;
        }
        Map<String, Set<String>> parCompte = annuaire.profilsParCompte();
        return l
            .stream()
            .map(c -> c.avecProfils(parCompte.getOrDefault(c.id(), c.profils())))
            .sorted(Comparator.comparing((Compte c) -> c.nomAffiche().toLowerCase(Locale.ROOT)).thenComparing(Compte::id))
            .toList();
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
        Set<String> privilegies = catalogue.privilegies();
        return new Indicateurs(
            l.size(),
            l.stream().filter(Compte::actif).count(),
            l.stream().filter(c -> !c.actif()).count(),
            l.stream().filter(c -> c.actif() && c.premierAccesEnAttente()).count(),
            l.stream().filter(c -> c.actif() && !c.secondFacteur()).count(),
            l.stream().filter(c -> c.profils().stream().anyMatch(privilegies::contains)).count(),
            l.stream().filter(c -> c.creeLe() != null && c.creeLe().isAfter(il30Jours)).count(),
            entrees.countByHorodatageAfter(clock.instant().minus(Duration.ofHours(24))),
            parProfil,
            parJour(l.stream().map(Compte::creeLe).filter(java.util.Objects::nonNull).toList()),
            parJour(entrees.horodatagesDepuis(debutSeries()))
        );
    }

    /** Premier instant de la fenêtre des séries : minuit du jour J-29, fuseau de la plateforme. */
    private Instant debutSeries() {
        return LocalDate.now(clock.withZone(FUSEAU)).minusDays(JOURS_SERIES - 1L).atStartOfDay(FUSEAU).toInstant();
    }

    /** Un point par jour sur les 30 derniers jours (aujourd'hui compris), zéro les jours sans événement. */
    List<ParJour> parJour(List<Instant> instants) {
        LocalDate fin = LocalDate.now(clock.withZone(FUSEAU));
        LocalDate debut = fin.minusDays(JOURS_SERIES - 1L);
        Map<LocalDate, Long> compte = new java.util.HashMap<>();
        instants.forEach(i -> {
            LocalDate j = LocalDate.ofInstant(i, FUSEAU);
            if (!j.isBefore(debut) && !j.isAfter(fin)) {
                compte.merge(j, 1L, Long::sum);
            }
        });
        return debut.datesUntil(fin.plusDays(1)).map(j -> new ParJour(j, compte.getOrDefault(j, 0L))).toList();
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
        ReglesAdministration.verifierProfils(acteur, Set.of(), n.profils(), catalogue.attribuables(), catalogue.privilegies());
        ReglesAdministration.verifierRattachement(n.profils(), n.institutionId(), catalogue.externes());
        if (annuaire.parIdentifiant(n.identifiant()).isPresent() || annuaire.emailPris(n.email(), null)) {
            throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
        }
        Set<ActionRequise> actions = new TreeSet<>(proprietes.premiereConnexion().actions());
        if (motDePasseProvisoire) {
            actions.add(ActionRequise.MOT_DE_PASSE);
        }
        String id = annuaire.creer(n, List.copyOf(actions));
        String mdp = motDePasseProvisoire ? annuaire.definirMotDePasseProvisoire(id) : null;
        journal.compte(acteur, "CREATION", id, n.identifiant(), null, List.copyOf(new TreeSet<>(n.profils())));
        boolean invitation = envoyerInvitation && inviter(acteur, lire(id));
        return new Creation(lire(id), invitation, mdp);
    }

    /**
     * Invitation par courriel de la plateforme (lien vers le nouveau mot de passe, 24 h). Un échec de
     * la messagerie n'annule pas la création : il est tracé et rendu (invitation non envoyée).
     */
    private boolean inviter(Acteur acteur, Compte c) {
        try {
            liens.envoyer(c, FinaliteJeton.INVITATION, couche(c));
            journal.compte(acteur, "INVITATION", c.id(), c.identifiant(), null, List.of(couche(c).name()));
            return true;
        } catch (IamException e) {
            LOG.warn("Invitation non envoyée au compte {} ({})", c.id(), e.code());
            journal.compte(acteur, "INVITATION_ECHEC", c.id(), c.identifiant(), null, List.of(e.code()));
            return false;
        }
    }

    /**
     * Couche où le compte se connecte (D-94) : portail pour un compte d'institution, console si ses profils
     * composent une permission de la console, département sinon.
     */
    private Couche couche(Compte c) {
        return catalogue.couche(c);
    }

    public Compte modifier(Acteur acteur, String id, FicheCompte f) {
        Compte c = lire(id);
        ReglesAdministration.verifierTelephone(f.telephone());
        // Institution inchangée quand elle est absente de la fiche (null) ; vide, elle est retirée.
        ReglesAdministration.verifierRattachement(c.profils(), f.institutionId() != null ? f.institutionId() : c.institutionId(), catalogue.externes());
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
        ReglesAdministration.verifierProfils(acteur, c.profils(), profils, catalogue.attribuables(), catalogue.privilegies());
        ReglesAdministration.verifierRattachement(profils, c.institutionId(), catalogue.externes());
        Map<String, Set<String>> compositions = catalogue.compositions();
        if (catalogue.porteOctroiPrivilegie(c.profils(), compositions) && !catalogue.porteOctroiPrivilegie(profils, compositions)) {
            ReglesAdministration.verifierDernierPorteurOctroi(c.actif(), catalogue.porteursActifsOctroiPrivilegie(compositions));
        }
        annuaire.definirProfils(id, profils);
        journal.compte(acteur, "PROFILS", id, c.identifiant(), motif, List.copyOf(new TreeSet<>(profils)));
        return lire(id);
    }

    public Compte definirActif(Acteur acteur, String id, boolean actif, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        Map<String, Set<String>> compositions = actif ? Map.of() : catalogue.compositions();
        if (!actif && catalogue.porteOctroiPrivilegie(c.profils(), compositions)) {
            ReglesAdministration.verifierDernierPorteurOctroi(c.actif(), catalogue.porteursActifsOctroiPrivilegie(compositions));
        }
        annuaire.definirActif(id, actif);
        journal.compte(acteur, actif ? "ACTIVATION" : "DESACTIVATION", id, c.identifiant(), motif, List.of());
        return lire(id);
    }

    public void supprimer(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        Map<String, Set<String>> compositions = catalogue.compositions();
        if (catalogue.porteOctroiPrivilegie(c.profils(), compositions)) {
            ReglesAdministration.verifierDernierPorteurOctroi(c.actif(), catalogue.porteursActifsOctroiPrivilegie(compositions));
        }
        annuaire.supprimer(id);
        journal.compte(acteur, "SUPPRESSION", id, c.identifiant(), motif, List.of());
    }

    /**
     * Exige des actions à la prochaine connexion (attribut gimac_actions_requises). Par courriel : un
     * lien vers le nouveau mot de passe (24 h) si le mot de passe ou la vérification du courriel est
     * exigé, un rappel avec le lien de connexion sinon.
     */
    public Compte exigerActions(Acteur acteur, String id, Set<ActionRequise> actions, boolean parCourriel, String motif) {
        ReglesAdministration.exigerMotif(motif);
        if (actions == null || actions.isEmpty()) {
            throw IamException.invalide("IAM_ACTIONS_REQUIRED", "Au moins une action est requise", Map.of());
        }
        Compte c = lire(id);
        annuaire.exigerActions(id, actions);
        journal.compte(acteur, "ACTIONS_REQUISES", id, c.identifiant(), motif, actions.stream().map(Enum::name).sorted().toList());
        if (parCourriel) {
            if (actions.contains(ActionRequise.MOT_DE_PASSE) || actions.contains(ActionRequise.VERIFIER_COURRIEL)) {
                liens.envoyer(c, FinaliteJeton.INVITATION, couche(c));
            } else {
                liens.rappeler(c, couche(c));
            }
        }
        return lire(id);
    }

    public Compte reinitialiserSecondFacteur(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        annuaire.reinitialiserSecondFacteur(id);
        annuaire.exigerActions(id, Set.of(ActionRequise.SECOND_FACTEUR));
        annuaire.fermerSessions(id);
        journal.compte(acteur, "REINITIALISATION_2FA", id, c.identifiant(), motif, List.of());
        return lire(id);
    }

    public String motDePasseProvisoire(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        String mdp = annuaire.definirMotDePasseProvisoire(id);
        annuaire.exigerActions(id, Set.of(ActionRequise.MOT_DE_PASSE));
        annuaire.fermerSessions(id);
        journal.compte(acteur, "MOT_DE_PASSE_PROVISOIRE", id, c.identifiant(), motif, List.of());
        return mdp;
    }

    /** Verrouillage du compte par la protection contre la force brute (US-EXI-23). */
    public cm.gimac.des.iam.domain.compte.Verrouillage verrouillage(String id) {
        return annuaire.verrouillage(id);
    }

    /**
     * Déblocage par l'administrateur du GIMAC (US-EXI-23) : verrouillage levé, compteur d'échecs remis à zéro, journal
     * COMPTE_DEBLOQUE (iam.account-administered.v1). Jamais sur soi-même ; compte non verrouillé : sans effet ni trace.
     */
    public cm.gimac.des.iam.domain.compte.Verrouillage debloquer(Acteur acteur, String id, String motif) {
        ReglesAdministration.verifierPasSoiMeme(acteur, id);
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        cm.gimac.des.iam.domain.compte.Verrouillage v = annuaire.verrouillage(id);
        if (!v.verrouille() && v.echecs() == 0) {
            return v;
        }
        annuaire.debloquer(id);
        journal.compte(acteur, "COMPTE_DEBLOQUE", id, c.identifiant(), motif, List.of(String.valueOf(v.echecs())));
        return annuaire.verrouillage(id);
    }

    public void fermerSessions(Acteur acteur, String id, String motif) {
        ReglesAdministration.exigerMotif(motif);
        Compte c = lire(id);
        annuaire.fermerSessions(id);
        journal.compte(acteur, "FERMETURE_SESSIONS", id, c.identifiant(), motif, List.of());
    }

    // ------------------------------------------------------------------ outils

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
