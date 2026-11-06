package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Codes de processus générés par le système : créations simultanées sous le même code envoyé, aucune collision. */
class ProcessusCodeIT extends AbstractProcessusIT {

    @Test
    void creationsConcurrentesCodesDistincts() throws Exception {
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            List<Callable<String>> creations = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                creations.add(() -> lire(mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), definition("MEME_CODE"))).andExpect(status().isCreated()).andReturn()).get("code").asText());
            }
            List<String> codes = new ArrayList<>();
            for (Future<String> f : pool.invokeAll(creations)) {
                codes.add(f.get());
            }
            assertThat(new HashSet<>(codes)).hasSize(n);
            assertThat(codes).allMatch(c -> c.matches("POLE_RESEAU-PRC-\\d+")).doesNotContain("MEME_CODE");
        } finally {
            pool.shutdownNow();
        }
    }
}
