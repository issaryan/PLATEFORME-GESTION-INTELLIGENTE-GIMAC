package cm.gimac.des.gateway.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.security.session.SessionGimac;
import cm.gimac.des.gateway.service.appareils.ServiceAppareils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/** « fermerAutresSessions » (docs/lot1/mon-compte.md §2) : la passerelle ferme les autres sessions sur le signal de MS-IAM. */
class FermetureAutresSessionsGatewayFilterFactoryTest {

    @SuppressWarnings("unchecked")
    static ObjectProvider<ServiceAppareils> fournisseur(ServiceAppareils s) {
        ObjectProvider<ServiceAppareils> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(s);
        return p;
    }

    /** Exécute le filtre ; le « service relayé » pose le statut et l'en-tête, puis la réponse est validée. */
    static MockServerWebExchange executer(GatewayFilter f, MockServerHttpRequest requete, HttpStatus statut, String entete, String sujet) {
        MockServerWebExchange e = MockServerWebExchange.from(requete);
        if (sujet != null) {
            WebSession s = e.getSession().block();
            s.getAttributes().put(SessionGimac.SUJET, sujet);
        }
        f
            .filter(e, ex -> {
                ex.getResponse().setStatusCode(statut);
                if (entete != null) {
                    ex.getResponse().getHeaders().add(FermetureAutresSessionsGatewayFilterFactory.EN_TETE, entete);
                }
                return ex.getResponse().setComplete();
            })
            .block();
        return e;
    }

    @Test
    @DisplayName("Mot de passe changé avec l'option : autres sessions fermées, en-tête retiré de la réponse")
    void fermeture() {
        ServiceAppareils service = mock(ServiceAppareils.class);
        when(service.fermerAutres(eq("u1"), any())).thenReturn(Mono.just(2));
        GatewayFilter f = new FermetureAutresSessionsGatewayFilterFactory(fournisseur(service)).apply(new Object());
        MockServerWebExchange e = executer(f, MockServerHttpRequest.post("/bff/des/iam/moi/mot-de-passe").build(), HttpStatus.NO_CONTENT, "1", "u1");
        verify(service).fermerAutres(eq("u1"), any());
        assertThat(e.getResponse().getHeaders().getFirst(FermetureAutresSessionsGatewayFilterFactory.EN_TETE)).isNull();
    }

    @Test
    @DisplayName("Sans signal, sur un autre chemin, en échec, sans compte ou sans service : rien n'est fermé, l'en-tête n'est jamais transmis")
    void sansFermeture() {
        ServiceAppareils service = mock(ServiceAppareils.class);
        when(service.fermerAutres(anyString(), any())).thenReturn(Mono.error(new IllegalStateException("panne")));
        GatewayFilter f = new FermetureAutresSessionsGatewayFilterFactory(fournisseur(service)).apply(new Object());
        executer(f, MockServerHttpRequest.post("/bff/des/iam/moi/mot-de-passe").build(), HttpStatus.NO_CONTENT, null, "u1");
        MockServerWebExchange autreChemin = executer(f, MockServerHttpRequest.put("/bff/des/iam/moi").build(), HttpStatus.OK, "1", "u1");
        assertThat(autreChemin.getResponse().getHeaders().getFirst(FermetureAutresSessionsGatewayFilterFactory.EN_TETE)).isNull();
        executer(f, MockServerHttpRequest.get("/bff/des/iam/moi/mot-de-passe").build(), HttpStatus.OK, "1", "u1");
        executer(f, MockServerHttpRequest.post("/bff/des/iam/moi/mot-de-passe").build(), HttpStatus.BAD_REQUEST, "1", "u1");
        executer(f, MockServerHttpRequest.post("/bff/des/iam/moi/mot-de-passe").build(), HttpStatus.NO_CONTENT, "0", "u1");
        executer(f, MockServerHttpRequest.post("/bff/des/iam/moi/mot-de-passe").build(), HttpStatus.NO_CONTENT, "1", null);
        verify(service, never()).fermerAutres(anyString(), any());
        // Panne de la fermeture : la réponse part quand même.
        MockServerWebExchange panne = executer(f, MockServerHttpRequest.post("/bff/portail/iam/moi/mot-de-passe").build(), HttpStatus.NO_CONTENT, "1", "u1");
        verify(service).fermerAutres(eq("u1"), any());
        assertThat(panne.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        GatewayFilter sansService = new FermetureAutresSessionsGatewayFilterFactory(fournisseur(null)).apply(new Object());
        executer(sansService, MockServerHttpRequest.post("/bff/console/iam/moi/mot-de-passe").build(), HttpStatus.NO_CONTENT, "1", "u1");
    }
}
