package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.domain.compte.Compte;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Annuaire des collaborateurs pour les autres services (IAM-04), jeton de service INTERNAL_IAM_READ :
 * MS-HOMOLOGATION y vérifie l'habilitation du conducteur de projet désigné (D-58) et y liste les
 * détenteurs du rôle CONDUCTEUR_PROJET ; MS-COLLABORATION y cherche les destinataires de la
 * messagerie (D-64), comptes internes actifs. Aucune donnée de contact ni de sécurité n'y figure.
 */
@RestController
@RequestMapping("/api/v1/interne/collaborateurs")
public class CollaborateursInternesResource {

    /** « avatarVersion » : version de la photo de profil, nulle sans photo (docs/lot1/mon-compte.md §1). */
    public record Collaborateur(String id, String nomAffiche, Set<String> profils, String institutionId, boolean actif, String fonction, String avatarVersion) {
        static Collaborateur de(Compte c, String avatarVersion) {
            return new Collaborateur(c.id(), c.nomAffiche(), c.profils(), c.institutionId(), c.actif(), c.fonction(), avatarVersion);
        }
    }

    private final AdministrationComptesService comptes;
    private final AvatarService avatars;

    public CollaborateursInternesResource(AdministrationComptesService comptes, AvatarService avatars) {
        this.comptes = comptes;
        this.avatars = avatars;
    }

    @GetMapping("/{id}")
    public Collaborateur lire(@PathVariable String id) {
        return Collaborateur.de(comptes.lire(id), avatars.version(id).orElse(null));
    }

    private List<Collaborateur> vues(List<Compte> liste) {
        Map<String, String> versions = avatars.versions(liste.stream().map(Compte::id).toList());
        return liste.stream().map(c -> Collaborateur.de(c, versions.get(c.id()))).toList();
    }

    /** Nombre maximal d'identifiants par demande de versions de photos. */
    static final int VERSIONS_MAX = 200;

    /**
     * Versions des photos de profil d'un lot de comptes (docs/lot1/mon-compte.md §1) : membres et
     * auteurs des conversations de MS-COLLABORATION, sans interroger le fournisseur d'identité. Les
     * comptes sans photo sont absents de la réponse ; 200 identifiants au plus (les suivants sont ignorés).
     */
    @GetMapping(path = "/avatars", params = "ids")
    public Map<String, String> versionsAvatars(@RequestParam("ids") List<String> ids) {
        return avatars.versions(ids.stream().filter(i -> i != null && !i.isBlank()).distinct().limit(VERSIONS_MAX).toList());
    }

    public record PageCollaborateurs(List<Collaborateur> contenu, long total, int page, int taille) {}

    /** Collaborateurs actifs d'un profil (100 au plus), triés par identifiant. */
    @GetMapping(params = "profil")
    public List<Collaborateur> parProfil(@RequestParam("profil") String profil) {
        return vues(comptes.lister(new AdministrationComptesService.Filtre(null, profil, null), 0, 100).contenu().stream().filter(Compte::actif).toList());
    }

    /** Comptes internes actifs (sans institution), filtrés par nom, courriel ou fonction, paginés (D-64). */
    @GetMapping
    public PageCollaborateurs annuaire(
        @RequestParam(name = "recherche", required = false) String recherche,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        var p = comptes.annuaireInterne(recherche, page, taille);
        return new PageCollaborateurs(vues(p.contenu()), p.total(), p.page(), p.taille());
    }
}
