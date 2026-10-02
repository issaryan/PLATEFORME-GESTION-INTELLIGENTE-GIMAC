package cm.gimac.des.gateway.web.rest.live;

import cm.gimac.des.gateway.domain.live.Abonne;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.domain.live.RegistrePresence;
import cm.gimac.des.gateway.domain.live.VueEvenement;
import cm.gimac.des.gateway.security.RevendicationsSession;
import cm.gimac.des.gateway.service.live.CanalDirectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

/**
 * Canal poussé des couches de présentation (F0 §A3, contrat de messagerie §5) :
 * {@code GET /bff/{des|portail|console}/live?sujets=a,b&depuis=<id>}, flux SSE ouvert par
 * {@code EventSource} avec le cookie de session. La chaîne de sécurité exige une session à deux
 * facteurs (401 sans session, 403 sans second facteur) ; ici : couche connue (404 sinon),
 * institution exigée sur le portail (403 sinon). Chaque événement :
 * {@code id: <id>} et {@code data: {"id","type","subject","topic","acteur"}} ; signaux éphémères
 * (présence, saisie, D-72) sans {@code id:}. Présence des comptes internes :
 * {@code GET /bff/{des|console}/presence}.
 */
@RestController
public class CanalDirectResource {

    static final String DERNIER_ID = "Last-Event-ID";

    private final CanalDirectService canal;

    public CanalDirectResource(CanalDirectService canal) {
        this.canal = canal;
    }

    @GetMapping(path = "/bff/{couche}/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<VueEvenement>> live(
        @PathVariable("couche") String nomCouche,
        @RequestParam(name = "sujets", required = false) String sujets,
        @RequestParam(name = "depuis", required = false) String depuis,
        @RequestHeader(name = DERNIER_ID, required = false) String dernierId,
        Authentication auth,
        ServerHttpResponse reponse
    ) {
        Couche couche = Couche.depuis(nomCouche).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String compte = RevendicationsSession.claim(auth, RevendicationsSession.SUJET);
        String institution = RevendicationsSession.claim(auth, RevendicationsSession.INSTITUTION);
        if (compte.isBlank() || (couche == Couche.PORTAIL && institution.isBlank())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // Pas de mise en tampon par un mandataire inverse (nginx) : chaque événement part aussitôt.
        reponse.getHeaders().set("X-Accel-Buffering", "no");
        String dernier = dernierId != null && !dernierId.isBlank() ? dernierId : depuis;
        return canal.ouvrir(Abonne.de(couche, compte, institution, sujets), dernier);
    }

    /**
     * Présence des comptes internes (D-72) : comptes en ligne et dernière présence des autres, état
     * initial que le canal poussé tient ensuite à jour. Couches internes seulement (404 sur le portail).
     */
    @GetMapping(path = "/bff/{couche}/presence", produces = MediaType.APPLICATION_JSON_VALUE)
    public RegistrePresence.Etat presence(@PathVariable("couche") String nomCouche, Authentication auth) {
        Couche couche = Couche.depuis(nomCouche)
            .filter(Couche::interne)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (RevendicationsSession.claim(auth, RevendicationsSession.SUJET).isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return canal.presence();
    }
}
