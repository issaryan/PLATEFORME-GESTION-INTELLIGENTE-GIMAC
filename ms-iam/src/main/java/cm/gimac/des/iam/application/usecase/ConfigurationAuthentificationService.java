package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.PresentationsProfils;
import cm.gimac.des.iam.application.port.out.ProfilsCrees;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.ProfilCree;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import cm.gimac.des.iam.domain.service.ReglesSandbox;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffre;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffreRepository;
import cm.gimac.des.iam.infrastructure.securite.ChiffrementSecrets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration de l'authentification depuis la console (D-62), sans passer par la console du
 * fournisseur d'identité : politique de sécurité (mots de passe, second facteur, sessions, force
 * brute), serveur de messagerie des invitations, permissions de chaque profil.
 */
@Service
@Transactional
public class ConfigurationAuthentificationService {

    private static final Pattern COURRIEL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Annuaire annuaire;
    private final JournalActes journal;
    private final ParametreChiffreRepository parametres;
    private final ChiffrementSecrets chiffrement;
    private final Clock clock;
    private final CatalogueProfils catalogue;
    private final ProfilsCrees crees;
    private final PresentationsProfils presentations;

    @Autowired
    public ConfigurationAuthentificationService(
        Annuaire annuaire,
        JournalActes journal,
        ParametreChiffreRepository parametres,
        ChiffrementSecrets chiffrement,
        Clock clock,
        CatalogueProfils catalogue,
        ProfilsCrees crees,
        PresentationsProfils presentations
    ) {
        this.annuaire = annuaire;
        this.journal = journal;
        this.parametres = parametres;
        this.chiffrement = chiffrement;
        this.clock = clock;
        this.catalogue = catalogue;
        this.crees = crees;
        this.presentations = presentations;
    }

    /** Service sans trace des modifications de profils (tests). */
    public ConfigurationAuthentificationService(
        Annuaire annuaire,
        JournalActes journal,
        ParametreChiffreRepository parametres,
        ChiffrementSecrets chiffrement,
        Clock clock,
        CatalogueProfils catalogue,
        ProfilsCrees crees
    ) {
        this(annuaire, journal, parametres, chiffrement, clock, catalogue, crees, null);
    }

    /** Service limité aux profils de la configuration (sans profil créé). */
    public ConfigurationAuthentificationService(
        Annuaire annuaire,
        JournalActes journal,
        IamProprietes proprietes,
        ParametreChiffreRepository parametres,
        ChiffrementSecrets chiffrement,
        Clock clock
    ) {
        this(annuaire, journal, parametres, chiffrement, clock, CatalogueProfils.configuration(proprietes, annuaire), null, null);
    }

    @Transactional(readOnly = true)
    public PolitiqueSecurite politique() {
        return annuaire.politique();
    }

    public PolitiqueSecurite appliquerPolitique(Acteur acteur, PolitiqueSecurite p, String motif) {
        ReglesAdministration.exigerMotif(motif);
        PolitiqueMotDePasse.verifier(p);
        List<String> champs = differences(annuaire.politique(), p);
        annuaire.appliquerPolitique(p);
        journal.configuration(acteur, "POLITIQUE_SECURITE", motif, champs);
        return annuaire.politique();
    }

    @Transactional(readOnly = true)
    public ConfigurationMessagerie messagerie() {
        return annuaire.messagerie().sansSecret();
    }

    public ConfigurationMessagerie appliquerMessagerie(Acteur acteur, ConfigurationMessagerie m, String motif) {
        ReglesAdministration.exigerMotif(motif);
        if (m.hote() == null || m.hote().isBlank() || m.port() < 1 || m.port() > 65535 || m.expediteur() == null || !COURRIEL.matcher(m.expediteur()).matches()) {
            throw IamException.invalide("IAM_SMTP_INVALID", "Hôte, port et adresse d'expédition obligatoires", Map.of());
        }
        if (m.authentification() && (m.utilisateur() == null || m.utilisateur().isBlank())) {
            throw IamException.invalide("IAM_SMTP_INVALID", "Utilisateur obligatoire avec l'authentification", Map.of());
        }
        annuaire.appliquerMessagerie(m);
        // D-65 : MS-IAM envoie lui-même ses courriels ; le fournisseur d'identité ne restitue jamais le mot de
        // passe du serveur, MS-IAM en garde donc une copie chiffrée (écriture seule, jamais relue par l'API).
        if (!m.authentification()) {
            parametres.deleteById(ParametreChiffre.SMTP_MOT_DE_PASSE);
        } else if (m.motDePasse() != null && !m.motDePasse().isEmpty()) {
            parametres.save(new ParametreChiffre(ParametreChiffre.SMTP_MOT_DE_PASSE, chiffrement.chiffrer(m.motDePasse()), clock.instant()));
        }
        journal.configuration(acteur, "MESSAGERIE", motif, List.of("hote", "port", "expediteur"));
        return messagerie();
    }

    @Transactional(readOnly = true)
    public void testerMessagerie(String destinataire) {
        if (destinataire == null || !COURRIEL.matcher(destinataire).matches()) {
            throw IamException.invalide("IAM_SMTP_INVALID", "Adresse de destination invalide", Map.of());
        }
        annuaire.testerMessagerie(destinataire);
    }

    /**
     * Profils attribuables (configuration ∪ profils créés, D-81 §5), qualifiés par le catalogue de MS-IAM :
     * privilégié, créé ; libellé et description (D-86 §2), date et auteur de la dernière modification.
     */
    @Transactional(readOnly = true)
    public List<Profil> profils() {
        Set<String> attribuables = catalogue.attribuables();
        Set<String> privilegies = catalogue.privilegies();
        Set<String> livres = catalogue.livres();
        Map<String, PresentationProfil> traces = presentations == null ? Map.of() : presentations.toutes();
        return annuaire.profils().stream()
            .filter(p -> attribuables.contains(p.code()))
            .map(p -> p.qualifie(privilegies.contains(p.code()), !livres.contains(p.code())).modifie(traces.get(p.code())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> permissions() {
        return annuaire.permissions();
    }

    /** Permissions du domaine dont le code commence par le préfixe (D-115 : {@code ?prefixe=SBX}) ; absent : toutes. */
    @Transactional(readOnly = true)
    public List<String> permissions(String prefixe) {
        return ReglesSandbox.filtrer(annuaire.permissions(), prefixe);
    }

    /**
     * Recompose un profil ; modifier un profil privilégié exige le droit d'octroi privilégié. Profil créé
     * (D-81 §5) : mêmes règles qu'à sa création (aucune permission INTERNAL_*, permissions communes
     * gardées, privilégié s'il porte une permission IAM_* ou PLATFORM_*). D-94 : retirer IAM_ROLE_PRIVILEGED_GRANT
     * d'une composition qui laisserait la plateforme sans compte actif portant cette permission est refusé (409
     * IAM_LAST_SUPER_ADMIN), quel que soit le nom du profil.
     */
    public Profil definirPermissions(Acteur acteur, String profil, Set<String> permissions, String motif) {
        ReglesAdministration.exigerMotif(motif);
        if (!catalogue.attribuables().contains(profil)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", profil);
        }
        if (catalogue.privilegies().contains(profil) && !acteur.peut(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "La modification d'un profil privilégié est réservée au super administrateur");
        }
        if (crees != null && !catalogue.livres().contains(profil)) {
            ProfilCree cree = crees.lire(profil).orElseThrow(() -> IamException.introuvable("IAM_PROFILE_NOT_FOUND", profil));
            permissions = ReglesProfils.avecCommunes(permissions);
            ReglesProfils.verifierComposition(acteur, permissions, annuaire.permissions());
            crees.enregistrer(new ProfilCree(profil, cree.description(), ReglesProfils.privilegie(permissions), cree.creePar(), cree.creeLe()));
        }
        Set<String> connues = new TreeSet<>(annuaire.permissions());
        Set<String> inconnues = new TreeSet<>(permissions);
        inconnues.removeAll(connues);
        if (!inconnues.isEmpty()) {
            throw IamException.invalide("IAM_PERMISSION_UNKNOWN", "Permission inconnue", Map.of("permissions", List.copyOf(inconnues)));
        }
        // D-115, RG-05 : la validation des verdicts de la sandbox n'est jamais portée par un profil externe.
        ReglesSandbox.verifierValidationReservee(catalogue.externes().contains(profil), permissions);
        // D-94 : une composition privilégiée (IAM_* ou PLATFORM_*) l'est pour tout profil, livré ou créé.
        if (ReglesProfils.privilegie(permissions) && !acteur.peut(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "Un profil privilégié est réservé au super administrateur");
        }
        verifierOctroiConserve(profil, permissions);
        annuaire.definirPermissionsProfil(profil, permissions);
        if (presentations != null) {
            presentations.toucher(profil, clock.instant(), acteur.id(), acteur.nom());
        }
        journal.configuration(acteur, "PROFIL_" + profil, motif, List.copyOf(new TreeSet<>(permissions)));
        return profils().stream().filter(p -> p.code().equals(profil)).findFirst().orElseThrow();
    }

    /** La plateforme garde au moins un compte actif portant IAM_ROLE_PRIVILEGED_GRANT après la recomposition. */
    private void verifierOctroiConserve(String profil, Set<String> permissions) {
        Map<String, Set<String>> avant = catalogue.compositions();
        if (!avant.getOrDefault(profil, Set.of()).contains(ReglesAdministration.OCTROI_PRIVILEGIE) || permissions.contains(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            return;
        }
        Map<String, Set<String>> apres = new java.util.HashMap<>(avant);
        apres.put(profil, Set.copyOf(permissions));
        long porteurs = catalogue.porteursActifsOctroiPrivilegie(apres);
        if (porteurs == 0) {
            ReglesAdministration.verifierDernierPorteurOctroi(true, 0);
        }
    }

    static List<String> differences(PolitiqueSecurite a, PolitiqueSecurite b) {
        List<String> d = new ArrayList<>();
        var composants = PolitiqueSecurite.class.getRecordComponents();
        for (var c : composants) {
            try {
                Object va = c.getAccessor().invoke(a);
                Object vb = c.getAccessor().invoke(b);
                if (!java.util.Objects.equals(va, vb)) {
                    d.add(c.getName());
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return d;
    }
}
