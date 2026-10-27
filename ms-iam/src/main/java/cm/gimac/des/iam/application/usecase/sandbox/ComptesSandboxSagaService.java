package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.ReglesSandbox;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Étapes OUVERTURE et MATRICE de SBX-ESPACE, sur l'administration ordinaire des comptes (D-115). */
@Service
public class ComptesSandboxSagaService {

    public record CompteSaga(
        String id,
        String identifiant,
        String nomAffiche,
        String email,
        String institutionId,
        boolean actif,
        Instant creeLe,
        List<String> profils,
        List<String> permissionsSbx,
        boolean rejouee
    ) {}

    static final String OUVERTURE = "SBX_OUVERTURE_COMPTE";
    static final String MATRICE = "SBX_MATRICE_COMPTE";

    private final AdministrationComptesService comptes;
    private final CatalogueProfils catalogue;
    private final EtapesIdempotentes etapes;

    public ComptesSandboxSagaService(AdministrationComptesService comptes, CatalogueProfils catalogue, EtapesIdempotentes etapes) {
        this.comptes = comptes;
        this.catalogue = catalogue;
        this.etapes = etapes;
    }

    /** Ouvre un compte d'institution avec invitation ; la clé rejouée retrouve le compte sans doublon. */
    @Transactional
    public CompteSaga ouvrir(Acteur acteur, NouveauCompte nouveau, String cle) {
        String identifiant = nouveau.identifiant().trim().toLowerCase(java.util.Locale.ROOT);
        verifierProfils(nouveau.profils());
        NouveauCompte n = new NouveauCompte(
            identifiant,
            nouveau.prenom(),
            nouveau.nom(),
            nouveau.email(),
            nouveau.telephone(),
            nouveau.fonction(),
            nouveau.institutionId(),
            nouveau.profils()
        );
        boolean rejouee = reserve(cle, OUVERTURE, identifiant, acteur);
        Compte c = rejouee
            ? comptes.lister(new AdministrationComptesService.Filtre(identifiant, null, null), 0, 100).contenu().stream()
                .filter(x -> x.identifiant().equals(identifiant))
                .findFirst()
                .orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", identifiant))
            : comptes.creer(service(acteur), n, true, false).compte();
        return vue(c, rejouee);
    }

    /** Remplace les seuls profils porteurs de permissions SBX ; les autres accès du compte sont conservés. */
    @Transactional
    public CompteSaga matrice(Acteur acteur, String compteId, Set<String> profilsSbx, String motif, String cle) {
        verifierProfils(profilsSbx);
        Compte avant = comptes.lire(compteId);
        if (avant.interne()) {
            throw IamException.nonTraitable("IAM_SBX_COMPTE_HORS_INSTITUTION", "Seul un compte d'institution est administré par cette saga", Map.of("id", compteId));
        }
        boolean rejouee = reserve(cle, MATRICE, compteId, acteur);
        Compte c = rejouee ? avant : comptes.definirProfils(service(acteur), compteId, profilsFusionnes(avant, profilsSbx), motif);
        return vue(c, rejouee);
    }

    @Transactional(readOnly = true)
    public CompteSaga lire(String compteId) {
        return vue(comptes.lire(compteId), false);
    }

    private Set<String> profilsFusionnes(Compte avant, Set<String> profilsSbx) {
        Map<String, Set<String>> compositions = catalogue.compositions();
        Set<String> fusion = new TreeSet<>(profilsSbx);
        avant.profils().stream()
            .filter(p -> compositions.getOrDefault(p, Set.of()).stream().noneMatch(ReglesSandbox::sandbox))
            .forEach(fusion::add);
        return fusion;
    }

    private void verifierProfils(Set<String> profils) {
        if (profils == null || profils.isEmpty()) {
            throw IamException.invalide("IAM_SBX_PROFIL_REQUIS", "Au moins un profil de la sandbox est requis", Map.of());
        }
        Set<String> externes = catalogue.externes();
        Map<String, Set<String>> compositions = catalogue.compositions();
        Set<String> invalides = new TreeSet<>();
        profils.forEach(p -> {
            Set<String> permissions = compositions.getOrDefault(p, Set.of());
            if (!externes.contains(p) || permissions.stream().noneMatch(ReglesSandbox::sandbox)) {
                invalides.add(p);
            }
        });
        if (!invalides.isEmpty()) {
            throw IamException.nonTraitable("IAM_SBX_PROFIL_INVALIDE", "Profils externes de la sandbox attendus", Map.of("profils", List.copyOf(invalides)));
        }
    }

    private CompteSaga vue(Compte c, boolean rejouee) {
        Set<String> permissions = new TreeSet<>();
        Map<String, Set<String>> compositions = catalogue.compositions();
        c.profils().forEach(p -> compositions.getOrDefault(p, Set.of()).stream().filter(ReglesSandbox::sandbox).forEach(permissions::add));
        return new CompteSaga(
            c.id(), c.identifiant(), c.nomAffiche(), c.email(), c.institutionId(), c.actif(), c.creeLe(),
            c.profils().stream().sorted().toList(), List.copyOf(permissions), rejouee
        );
    }

    private boolean reserve(String cle, String operation, String resultat, Acteur acteur) {
        return cle != null && !cle.isBlank() && etape(cle, operation, resultat, acteur);
    }

    private boolean etape(String cle, String operation, String resultat, Acteur acteur) {
        return etapes.reserver(cle.strip(), operation, resultat, acteur.id()).isPresent();
    }

    private static Acteur service(Acteur a) {
        return new Acteur(a.id(), a.nom(), Set.of(ReglesAdministration.OCTROI_PRIVILEGIE));
    }
}
