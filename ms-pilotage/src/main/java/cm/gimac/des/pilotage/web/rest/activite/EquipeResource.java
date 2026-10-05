package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.EquipeUseCase;
import cm.gimac.des.pilotage.web.rest.activite.Vues.MembreVue;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vue d'équipe du jour (docs/lot1/mon-activite.md §2.5), PIL_TEAM_READ. */
@RestController
@RequestMapping("/api/v1/equipe")
public class EquipeResource {

    private final EquipeUseCase equipe;

    public EquipeResource(EquipeUseCase equipe) {
        this.equipe = equipe;
    }

    @GetMapping
    @PreAuthorize(Jetons.EQUIPE)
    public List<MembreVue> equipe(@RequestParam(name = "jour", required = false) String jour, Authentication auth) {
        return equipe.equipe(appelant(auth), jour).stream().map(MembreVue::de).toList();
    }
}
