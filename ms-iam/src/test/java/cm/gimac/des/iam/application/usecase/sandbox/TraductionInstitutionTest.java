package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Traduction de institution_id en code participant (D-134) : cache borné, jamais de valeur devinée. */
class TraductionInstitutionTest {

    static final UUID BANQUE = UUID.fromString("6f1c2a3e-4444-4a6b-9c1d-000000000042");

    final Memoires.Horloge horloge = new Memoires.Horloge(Instant.parse("2026-11-05T08:00:00Z"));
    final AtomicInteger lectures = new AtomicInteger();
    boolean injoignable;

    final RegistreInstitutions registre = new RegistreInstitutions() {
        @Override
        public Optional<String> codeParticipant(UUID id) {
            lectures.incrementAndGet();
            if (injoignable) {
                throw new RegistreIndisponible("injoignable", null);
            }
            return BANQUE.equals(id) ? Optional.of("99002") : id.getMostSignificantBits() == 1 ? Optional.of("X 1") : id.getMostSignificantBits() == 2 ? Optional.of(BANQUE.toString()) : Optional.empty();
        }

        @Override
        public Optional<UUID> identifiant(String code) {
            if (injoignable) {
                throw new RegistreIndisponible("injoignable", null);
            }
            return "99002".equals(code) ? Optional.of(BANQUE) : Optional.empty();
        }
    };

    final TraductionInstitution traduction = new TraductionInstitution(registre, horloge, Duration.ofMinutes(5));

    @Test
    @DisplayName("UUID traduit et gardé en cache le temps prévu ; code rendu tel quel ; valeur illisible ou inconnue : vide")
    void traduction() {
        assertThat(traduction.code(BANQUE.toString())).contains("99002");
        assertThat(traduction.code(" " + BANQUE + " ")).contains("99002");
        assertThat(lectures).hasValue(1);
        horloge.avancer(Duration.ofMinutes(6));
        assertThat(traduction.code(BANQUE.toString())).contains("99002");
        assertThat(lectures).hasValue(2);
        assertThat(traduction.code("99001")).contains("99001");
        assertThat(traduction.code("INST-0042")).contains("INST-0042");
        assertThat(traduction.code("code invalide !")).isEmpty();
        assertThat(traduction.code(null)).isEmpty();
        assertThat(traduction.code(" ")).isEmpty();
        assertThat(traduction.code(UUID.randomUUID().toString())).isEmpty();
        // Correspondance sans code participant valide : jamais retenue.
        assertThat(traduction.code(new UUID(1, 1).toString())).isEmpty();
        // Un identifiant rendu à la place d'un code n'est jamais pris pour un code.
        assertThat(traduction.code(new UUID(2, 1).toString())).isEmpty();
        injoignable = true;
        assertThatThrownBy(() -> traduction.code(UUID.randomUUID().toString())).isInstanceOf(RegistreInstitutions.RegistreIndisponible.class);
        // En cache : la correspondance déjà lue reste servie, aucun état n'y est gardé.
        assertThat(traduction.code(BANQUE.toString())).contains("99002");
    }

    @Test
    @DisplayName("Même institution, valeurs possibles, valeur à enregistrer et comptes de l'institution, sous les deux formes")
    void comparaisonsEtRecherches() {
        assertThat(traduction.memeInstitution("99002", BANQUE.toString())).isTrue();
        assertThat(traduction.memeInstitution(BANQUE.toString(), "99002")).isTrue();
        assertThat(traduction.memeInstitution("99001", "99001")).isTrue();
        assertThat(traduction.memeInstitution("99001", BANQUE.toString())).isFalse();
        assertThat(traduction.memeInstitution("INST-0042", "INST-0043")).isFalse();
        assertThat(traduction.memeInstitution("a b", "a  b")).isFalse();
        assertThat(traduction.memeInstitution(null, "99001")).isFalse();
        assertThat(traduction.memeInstitution("99001", " ")).isFalse();
        assertThat(traduction.memeInstitution("99001", null)).isFalse();
        assertThat(traduction.memeInstitution(" ", "99001")).isFalse();
        assertThat(traduction.memeInstitution("code invalide !", "99001")).isFalse();
        assertThat(traduction.valeurs("99002")).containsExactly("99002", BANQUE.toString());
        assertThat(traduction.valeurs(BANQUE.toString())).containsExactly(BANQUE.toString(), "99002");
        assertThat(traduction.valeurs("INST-0042")).containsExactly("INST-0042");
        assertThat(traduction.aEnregistrer("99002")).isEqualTo(BANQUE.toString());
        assertThat(traduction.aEnregistrer("99001")).isEqualTo("99001");
        assertThat(traduction.aEnregistrer(BANQUE.toString())).isEqualTo(BANQUE.toString());
        assertThat(traduction.aEnregistrer(null)).isNull();
        assertThat(traduction.aEnregistrer("code invalide !")).isEqualTo("code invalide !");

        AnnuaireEnMemoire annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(Set.of("ROLE_SBX_TESTEUR")));
        String ancien = annuaire.creer(new NouveauCompte("ancien", "A", "B", "a@t.test", null, null, "99002", Set.of("ROLE_SBX_TESTEUR")), List.of());
        String nouveau = annuaire.creer(new NouveauCompte("nouveau", "C", "D", "c@t.test", null, null, BANQUE.toString(), Set.of("ROLE_SBX_TESTEUR")), List.of());
        annuaire.creer(new NouveauCompte("autre", "E", "F", "e@t.test", null, null, "99001", Set.of("ROLE_SBX_TESTEUR")), List.of());
        assertThat(traduction.comptes(annuaire, "99002")).extracting(Compte::id).containsExactlyInAnyOrder(ancien, nouveau);
        assertThat(traduction.comptes(annuaire, BANQUE.toString())).extracting(Compte::id).containsExactlyInAnyOrder(ancien, nouveau);

        injoignable = true;
        assertThat(traduction.valeurs("99002")).containsExactly("99002");
        assertThat(traduction.aEnregistrer("99002")).isEqualTo("99002");
        assertThat(new TraductionInstitution(registre, horloge, Duration.ofMinutes(5)).memeInstitution("99002", BANQUE.toString())).isFalse();
        assertThat(TraductionInstitution.sansRegistre(horloge).code("99001")).contains("99001");
    }

    @Test
    @DisplayName("Cache borné : au-delà de la taille maximale, il est vidé avant d'accueillir une nouvelle correspondance")
    void cacheBorne() {
        AtomicInteger appels = new AtomicInteger();
        RegistreInstitutions tous = new RegistreInstitutions() {
            @Override
            public Optional<String> codeParticipant(UUID id) {
                appels.incrementAndGet();
                return Optional.of("99003");
            }

            @Override
            public Optional<UUID> identifiant(String code) {
                return Optional.empty();
            }
        };
        TraductionInstitution t = new TraductionInstitution(tous, horloge, Duration.ofMinutes(5));
        UUID premier = new UUID(7, 0);
        t.code(premier.toString());
        for (int k = 1; k <= TraductionInstitution.TAILLE_MAX; k++) {
            t.code(new UUID(7, k).toString());
        }
        int avant = appels.get();
        t.code(premier.toString());
        assertThat(appels.get()).isEqualTo(avant + 1);
    }
}
