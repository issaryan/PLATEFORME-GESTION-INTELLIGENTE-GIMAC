package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.EquipeUseCase;
import cm.gimac.des.pilotage.web.rest.activite.Vues.SyntheseVue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Synthèse « Ma journée » du tableau de bord (docs/lot1/mon-activite.md §2.6), PIL_ACTIVITY_OWN. */
@RestController
@RequestMapping("/api/v1/moi")
public class SyntheseResource {

    private final EquipeUseCase equipe;

    public SyntheseResource(EquipeUseCase equipe) {
        this.equipe = equipe;
    }

    @GetMapping("/synthese")
    @PreAuthorize(Jetons.ACTIVITE)
    public SyntheseVue synthese(@RequestParam(name = "jour", required = false) String jour, Authentication auth) {
        return SyntheseVue.de(equipe.synthese(appelant(auth), jour));
    }
}
