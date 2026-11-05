package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.participant.application.port.out.RegistreLecture;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.Fiche;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.PageServices;
import cm.gimac.des.participant.domain.ParticipantException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture du registre par la Sandbox GIMAC (US-EXI-21), registre en mémoire. */
class RegistreSandboxQueriesTest {

    static final UUID ID = UUID.randomUUID();
    static final RegistreLecture.Correspondant CORRESPONDANT = new RegistreLecture.Correspondant(UUID.randomUUID(), UUID.randomUUID(), "Awa Ndiaye", "MONETIQUE");

    final List<String> codesLus = new ArrayList<>();
    final VersionsStatutMemoire versions = new VersionsStatutMemoire();

    final RegistreLecture registre = new RegistreLecture() {
        @Override
        public Optional<Identite> parCode(String code) {
            codesLus.add(code);
            return "99001".equals(code) ? Optional.of(new Identite(ID, "99001", "Banque fictive 99001", "EN_HOMOLOGATION")) : Optional.empty();
        }

        @Override
        public Optional<Identite> parId(UUID id) {
            return ID.equals(id) ? Optional.of(new Identite(ID, "99001", "Banque fictive 99001", "EN_HOMOLOGATION")) : Optional.empty();
        }

        @Override
        public List<Correspondant> correspondantsActifs(UUID institutionId) {
            return ID.equals(institutionId) ? List.of(CORRESPONDANT) : List.of();
        }

        @Override
        public List<String> servicesSouscrits(UUID institutionId) {
            return ID.equals(institutionId) ? List.of("CARTE", "MOBILE") : List.of();
        }

        @Override
        public List<Service> services(int page, int taille) {
            List<Service> tous = List.of(new Service("CARTE", 3), new Service("MOBILE", 1), new Service("VIREMENT", 2));
            return tous.subList(Math.min(page * taille, tous.size()), Math.min((page + 1) * taille, tous.size()));
        }

        @Override
        public long nombreServices() {
            return 3;
        }
    };

    final RegistreSandboxQueries queries = new RegistreSandboxQueries(registre, versions);

    @Test
    @DisplayName("Critère 1 : fiche de 99001 en homologation, code, raison sociale, statut, version de statut et correspondants")
    void lectureDeLaFiche() {
        versions.versions.put(ID, 4L);
        Fiche f = queries.fiche(" 99001 ");
        assertThat(f).isEqualTo(new Fiche("99001", "Banque fictive 99001", "EN_HOMOLOGATION", 4, List.of(CORRESPONDANT), List.of("CARTE", "MOBILE")));
        assertThat(codesLus).containsExactly("99001");
    }

    @Test
    @DisplayName("D-134 : correspondance entre l'identifiant au registre et le code participant, dans les deux sens ; 404 sinon")
    void correspondance() {
        assertThat(queries.correspondance(ID)).isEqualTo(new RegistreSandboxQueries.Correspondance(ID, "99001"));
        assertThat(queries.correspondance(" 99001 ")).isEqualTo(new RegistreSandboxQueries.Correspondance(ID, "99001"));
        assertThatThrownBy(() -> queries.correspondance(UUID.randomUUID())).isInstanceOf(ParticipantException.class);
        assertThatThrownBy(() -> queries.correspondance("99999")).isInstanceOf(ParticipantException.class);
        assertThatThrownBy(() -> queries.correspondance((String) null)).isInstanceOf(ParticipantException.class);
    }

    @Test
    @DisplayName("Code absent du registre (ou nul) : 404 PAR_INSTITUTION_NOT_FOUND, aucune donnée inventée")
    void codeAbsent() {
        assertThatThrownBy(() -> queries.fiche("99999"))
            .isInstanceOfSatisfying(ParticipantException.class, e -> assertThat(e.code()).isEqualTo("PAR_INSTITUTION_NOT_FOUND"));
        assertThatThrownBy(() -> queries.fiche(null)).isInstanceOf(ParticipantException.class);
        assertThat(codesLus).containsExactly("99999", "");
    }

    @Test
    @DisplayName("Critère 2 : nomenclature des services souscrits, triée, paginée, code retenu celui du registre")
    void nomenclatureDesServices() {
        PageServices p = queries.nomenclature(0, 2);
        assertThat(p.contenu()).extracting(RegistreLecture.Service::code).containsExactly("CARTE", "MOBILE");
        assertThat(p.total()).isEqualTo(3);
        assertThat(queries.nomenclature(1, 2).contenu()).extracting(RegistreLecture.Service::code).containsExactly("VIREMENT");
        assertThat(queries.nomenclature(0, RegistreSandboxQueries.TAILLE_MAX).taille()).isEqualTo(100);
    }

    @Test
    @DisplayName("Pagination invalide : 422 PAR_PAGINATION_INVALIDE")
    void paginationInvalide() {
        for (int[] pt : new int[][] { { -1, 10 }, { 0, 0 }, { 0, 101 } }) {
            assertThatThrownBy(() -> queries.nomenclature(pt[0], pt[1]))
                .isInstanceOfSatisfying(ParticipantException.class, e -> assertThat(e.code()).isEqualTo("PAR_PAGINATION_INVALIDE"));
        }
    }
}
