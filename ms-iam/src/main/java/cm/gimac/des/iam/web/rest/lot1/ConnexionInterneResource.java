package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.ConnexionService;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Connexion sur la plateforme, routes internes de la passerelle (D-65, connexion.md §3.1), jeton de
 * service du client gateway-service portant INTERNAL_IAM_READ ; jamais relayées aux navigateurs.
 */
@RestController
@RequestMapping("/api/v1/interne")
public class ConnexionInterneResource {

    public record EtatConnexionVue(boolean actif, boolean secondFacteur, List<String> actionsRequises) {
        static EtatConnexionVue de(EtatConnexion e) {
            return new EtatConnexionVue(e.actif(), e.secondFacteur(), e.actionsRequises().stream().map(Enum::name).toList());
        }
    }

    public record MotDePasseOublieRequest(@NotBlank @Size(max = 255) String identifiant, @Size(max = 20) String couche) {}

    /** Jeton et mot de passe : jamais dans une trace. */
    public record NouveauMotDePasseRequest(@NotBlank @Size(max = 100) String jeton, @NotNull @Size(max = 256) String motDePasse) {
        @Override
        public String toString() {
            return "NouveauMotDePasseRequest[jeton=****, motDePasse=****]";
        }
    }

    private final ConnexionService service;

    public ConnexionInterneResource(ConnexionService service) {
        this.service = service;
    }

    @GetMapping("/comptes/{id}/etat-connexion")
    public EtatConnexionVue etat(@PathVariable String id) {
        return EtatConnexionVue.de(service.etat(id));
    }

    /** 202 toujours : l'existence d'un compte n'est jamais révélée. */
    @PostMapping("/connexion/mot-de-passe-oublie")
    public ResponseEntity<Void> motDePasseOublie(@Valid @RequestBody MotDePasseOublieRequest r) {
        service.motDePasseOublie(r.identifiant(), r.couche());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/connexion/nouveau-mot-de-passe")
    public ResponseEntity<Void> nouveauMotDePasse(@Valid @RequestBody NouveauMotDePasseRequest r) {
        service.nouveauMotDePasse(r.jeton(), r.motDePasse());
        return ResponseEntity.noContent().build();
    }
}
