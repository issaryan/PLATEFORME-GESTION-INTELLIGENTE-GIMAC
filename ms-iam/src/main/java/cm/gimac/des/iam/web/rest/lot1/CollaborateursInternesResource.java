package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.domain.compte.Compte;
import java.util.List;
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

    public record Collaborateur(String id, String nomAffiche, Set<String> profils, String institutionId, boolean actif, String fonction) {
        static Collaborateur de(Compte c) {
            return new Collaborateur(c.id(), c.nomAffiche(), c.profils(), c.institutionId(), c.actif(), c.fonction());
        }
    }

    private final AdministrationComptesService comptes;

    public CollaborateursInternesResource(AdministrationComptesService comptes) {
        this.comptes = comptes;
    }

    @GetMapping("/{id}")
    public Collaborateur lire(@PathVariable String id) {
        return Collaborateur.de(comptes.lire(id));
    }

    public record PageCollaborateurs(List<Collaborateur> contenu, long total, int page, int taille) {}

    /** Collaborateurs actifs d'un profil (100 au plus), triés par identifiant. */
    @GetMapping(params = "profil")
    public List<Collaborateur> parProfil(@RequestParam("profil") String profil) {
        return comptes.lister(new AdministrationComptesService.Filtre(null, profil, null), 0, 100).contenu().stream().filter(Compte::actif).map(Collaborateur::de).toList();
    }

    /** Comptes internes actifs (sans institution), filtrés par nom, courriel ou fonction, paginés (D-64). */
    @GetMapping
    public PageCollaborateurs annuaire(
        @RequestParam(name = "recherche", required = false) String recherche,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        var p = comptes.annuaireInterne(recherche, page, taille);
        return new PageCollaborateurs(p.contenu().stream().map(Collaborateur::de).toList(), p.total(), p.page(), p.taille());
    }
}
