package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.IntrospectionSandboxService;
import cm.gimac.des.iam.domain.sandbox.Introspection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Introspection des jetons pour la sandbox (D-119, docs/lot1/sandbox-existant-adapte.md §6) : jeton de service portant
 * INTERNAL_IAM_SBX_INTROSPECT (passerelle, services de la sandbox). Réponse au format de RFC 7662 : {@code active}
 * seul en cas de refus, sinon la portée effective projetée dans le jeton interne réduit.
 */
@RestController
@RequestMapping("/api/v1/interne/sandbox")
@PreAuthorize("hasAuthority('INTERNAL_IAM_SBX_INTROSPECT')")
public class SandboxIntrospectionResource {

    /**
     * Jeton présenté (jamais tracé), et s'il y a lieu institution demandée et composant appelé (accès de prestataire,
     * US-EXI-24).
     */
    public record IntrospectionRequest(@NotBlank @Size(max = 16384) String jeton, @Size(max = 64) String institution, @Size(max = 64) String composant) {
        @Override
        public String toString() {
            return "IntrospectionRequest[jeton=****, institution=" + institution + ", composant=" + composant + "]";
        }
    }

    private final IntrospectionSandboxService service;

    public SandboxIntrospectionResource(IntrospectionSandboxService service) {
        this.service = service;
    }

    @PostMapping("/introspection")
    public Map<String, Object> introspecter(@Valid @RequestBody IntrospectionRequest r) {
        return vue(service.introspecter(r.jeton(), r.institution(), r.composant()));
    }

    static Map<String, Object> vue(Introspection i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active", i.active());
        if (!i.active()) {
            return m;
        }
        m.put("sub", i.sujet());
        m.put("institution_id", i.institutionId());
        m.put("permissions", new TreeSet<>(i.permissions()));
        m.put("type", i.type());
        m.put("delegation_id", i.delegationId());
        m.put("composants", new TreeSet<>(i.composants()));
        m.put("exp", i.expiration() == null ? null : i.expiration().getEpochSecond());
        return m;
    }
}
