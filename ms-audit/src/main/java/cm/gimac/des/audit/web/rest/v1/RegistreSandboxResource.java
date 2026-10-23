package cm.gimac.des.audit.web.rest.v1;

import cm.gimac.des.audit.application.usecase.RegistreSandboxQueries;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registre d'audit filtré sur la source sandbox (US-EXI-10), relayé depuis la console par la passerelle
 * ({@code /bff/console/audit/sandbox/**}) : recherche par institution, export CSV, rebut des enveloppes non conformes.
 * Lecture seule, SBX_TRA_CONSULTATION (traces de la sandbox) ; second facteur exigé par la passerelle (OPA).
 */
@RestController
@RequestMapping("/api/v1/registre/sandbox")
@PreAuthorize("hasAuthority('SBX_TRA_CONSULTATION')")
public class RegistreSandboxResource {

    private final RegistreSandboxQueries registre;

    public RegistreSandboxResource(RegistreSandboxQueries registre) {
        this.registre = registre;
    }

    @GetMapping
    public RegistreSandboxQueries.Page<RegistreSandboxQueries.Entree> rechercher(
        @RequestParam(name = "institution", required = false) String institution,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        return registre.rechercher(institution, page, taille);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exporter(@RequestParam(name = "institution", required = false) String institution) {
        String nom = "registre-sandbox" + (RegistreSandboxQueries.institutionAffichee(institution)) + ".csv";
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
            .body(registre.exporter(institution).getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/rebut")
    public RegistreSandboxQueries.PageRebut rebut(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "taille", defaultValue = "20") int taille) {
        return registre.rebut(page, taille);
    }
}
