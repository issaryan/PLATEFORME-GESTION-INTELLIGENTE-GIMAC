package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
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
    private final IamProprietes proprietes;
    private final ParametreChiffreRepository parametres;
    private final ChiffrementSecrets chiffrement;
    private final Clock clock;

    public ConfigurationAuthentificationService(
        Annuaire annuaire,
        JournalActes journal,
        IamProprietes proprietes,
        ParametreChiffreRepository parametres,
        ChiffrementSecrets chiffrement,
        Clock clock
    ) {
        this.annuaire = annuaire;
        this.journal = journal;
        this.proprietes = proprietes;
        this.parametres = parametres;
        this.chiffrement = chiffrement;
        this.clock = clock;
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

    @Transactional(readOnly = true)
    public List<Profil> profils() {
        Set<String> attribuables = proprietes.profils().attribuables();
        return annuaire.profils().stream().filter(p -> attribuables.contains(p.code())).toList();
    }

    @Transactional(readOnly = true)
    public List<String> permissions() {
        return annuaire.permissions();
    }

    /** Recompose un profil ; modifier un profil privilégié exige le droit d'octroi privilégié. */
    public Profil definirPermissions(Acteur acteur, String profil, Set<String> permissions, String motif) {
        ReglesAdministration.exigerMotif(motif);
        if (!proprietes.profils().attribuables().contains(profil)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", profil);
        }
        if (proprietes.profils().privilegies().contains(profil) && !acteur.peut(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "La modification d'un profil privilégié est réservée au super administrateur");
        }
        Set<String> connues = new TreeSet<>(annuaire.permissions());
        Set<String> inconnues = new TreeSet<>(permissions);
        inconnues.removeAll(connues);
        if (!inconnues.isEmpty()) {
            throw IamException.invalide("IAM_PERMISSION_UNKNOWN", "Permission inconnue", Map.of("permissions", List.copyOf(inconnues)));
        }
        annuaire.definirPermissionsProfil(profil, permissions);
        journal.configuration(acteur, "PROFIL_" + profil, motif, List.copyOf(new TreeSet<>(permissions)));
        return profils().stream().filter(p -> p.code().equals(profil)).findFirst().orElseThrow();
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
