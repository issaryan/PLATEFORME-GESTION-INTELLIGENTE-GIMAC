package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Demandeur;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Etat;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Ouverture;
import cm.gimac.des.document.domain.DepotFragmente;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import cm.gimac.des.document.infrastructure.stockage.StockageObjetMemoire;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

/**
 * Dépôts fragmentés côté client (D-74) : ouverture, parties, fin, abandon et expiration, avec le
 * substitut du dépôt objet (règles multipart de S3, parties de 8 octets) et une base simulée.
 */
class DepotFragmenteServiceTest {

    static final Instant DEBUT = Instant.parse("2026-10-02T08:00:00Z");
    static final String AGENT = "7c1e0000-0000-0000-0000-000000000001";
    static final Demandeur DEMANDEUR = new Demandeur(AGENT, "inst-1", Set.of("DOC_DEPOSIT"));
    /** 26 octets : trois parties de 8 octets et une dernière de 2. */
    static final byte[] FICHIER = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.US_ASCII);

    static final DepotFragmenteProprietes PROPRIETES = new DepotFragmenteProprietes(
        DataSize.ofBytes(64),
        DataSize.ofBytes(1000),
        DataSize.ofBytes(8),
        Duration.ofHours(24),
        Duration.ofHours(1),
        Duration.ofMinutes(1)
    );

    BaseEnMemoire base;
    BaseEnMemoire.Horloge horloge;
    StockageObjetMemoire stockage;
    DepotFragmenteService service;

    @BeforeEach
    void init() {
        base = new BaseEnMemoire();
        horloge = new BaseEnMemoire.Horloge(DEBUT);
        stockage = spy(new StockageObjetMemoire(1));
        service = new DepotFragmenteService(stockage, base.depotRepository, base.transactions, PROPRIETES, horloge);
    }

    static Ouverture ouverture(String finalite, Long taille) {
        return new Ouverture("C:\\videos\\atelier.mp4", "Video/MP4; codecs=avc1", taille, finalite, "conversation/c1");
    }

    Etat ouvrir() {
        return service.ouvrir(ouverture("adhesion", (long) FICHIER.length), DEMANDEUR);
    }

    static InputStream corps(byte[] octets) {
        return new ByteArrayInputStream(octets);
    }

    void envoyer(UUID id, int numero) {
        byte[] partie = Arrays.copyOfRange(FICHIER, (numero - 1) * 8, Math.min(numero * 8, FICHIER.length));
        service.recevoirPartie(id, numero, corps(partie), partie.length, AGENT);
    }

    static void erreur(ThrowingCallable appel, int statut, String code) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(statut);
            assertThat(e.code()).isEqualTo(code);
        });
    }

    // ------------------------------------------------------------------ ouverture

    @Test
    @DisplayName("Ouverture : dépôt OUVERT, parties de 8 octets, téléversement ouvert, nom et type assainis, expiration à 24 h")
    void ouverture() {
        Etat e = ouvrir();

        assertThat(e.etat()).isEqualTo(EtatDepotFragmente.OUVERT);
        assertThat(e.nomFichier()).isEqualTo("atelier.mp4");
        assertThat(e.typeMime()).isEqualTo("video/mp4");
        assertThat(e.taille()).isEqualTo(26);
        assertThat(e.taillePartie()).isEqualTo(8);
        assertThat(e.nombreParties()).isEqualTo(4);
        assertThat(e.partiesRecues()).isEmpty();
        assertThat(e.documentId()).isNull();
        assertThat(e.sha256()).isNull();
        assertThat(e.motif()).isNull();
        assertThat(e.dedoublonne()).isFalse();
        assertThat(e.expireLe()).isEqualTo(DEBUT.plus(Duration.ofHours(24)));

        DepotFragmente d = base.depot(e.id());
        assertThat(d.getFinalite()).isEqualTo("ADHESION");
        assertThat(d.getReference()).isEqualTo("conversation/c1");
        assertThat(d.getDeposant()).isEqualTo(AGENT);
        assertThat(d.getInstitutionId()).isEqualTo("inst-1");
        assertThat(d.getCleDepot()).isEqualTo(e.id().toString());
        assertThat(d.getOuvertLe()).isEqualTo(DEBUT);
        assertThat(d.getVersion()).isZero();
        assertThat(d.toString()).contains("OUVERT", "ADHESION", "0/4").doesNotContain("conversation/c1");
        assertThat(d).isEqualTo(d).isNotEqualTo(new DepotFragmente().id(UUID.randomUUID())).isNotEqualTo(null);
        assertThat(d.hashCode()).isEqualTo(new DepotFragmente().hashCode());
        assertThat(stockage.envoisEnCours()).isEqualTo(1);
    }

    @Test
    @DisplayName("Taille de partie : la dernière porte le reste ; fichier d'une seule partie")
    void tailleDesParties() {
        Etat e = service.ouvrir(ouverture("ADHESION", 16L), DEMANDEUR);
        assertThat(e.nombreParties()).isEqualTo(2);
        assertThat(base.depot(e.id()).tailleAttendue(2)).isEqualTo(8);
        Etat un = service.ouvrir(ouverture("ADHESION", 1L), DEMANDEUR);
        assertThat(un.nombreParties()).isEqualTo(1);
        assertThat(base.depot(un.id()).tailleAttendue(1)).isEqualTo(1);
    }

    @Test
    @DisplayName("Permission selon la finalité : MESSAGERIE exige COL_MESSAGE_SEND, les autres une permission du dépôt direct ; 403 sinon")
    void permissionSelonLaFinalite() {
        Ouverture messagerie = ouverture("MESSAGERIE", 10L);
        erreur(() -> service.ouvrir(messagerie, new Demandeur(AGENT, null, Set.of("DOC_DEPOSIT"))), 403, "DOC_PURPOSE_FORBIDDEN");
        assertThat(service.ouvrir(messagerie, new Demandeur(AGENT, null, Set.of("COL_MESSAGE_SEND"))).etat()).isEqualTo(EtatDepotFragmente.OUVERT);

        Ouverture adhesion = ouverture("ADHESION", 10L);
        erreur(() -> service.ouvrir(adhesion, new Demandeur(AGENT, null, Set.of("COL_MESSAGE_SEND"))), 403, "DOC_PURPOSE_FORBIDDEN");
        for (String p : DepotFragmenteService.PERMISSIONS_DEPOT) {
            assertThat(service.ouvrir(adhesion, new Demandeur(AGENT, null, Set.of(p))).etat()).isEqualTo(EtatDepotFragmente.OUVERT);
        }
        assertThat(stockage.envoisEnCours()).isEqualTo(1 + DepotFragmenteService.PERMISSIONS_DEPOT.size());
    }

    @Test
    @DisplayName("Permission selon la finalité : RAPPORT_ACTIVITE exige PIL_ACTIVITY_OWN (D-76), ni COL_MESSAGE_SEND ni une permission du dépôt direct ; 403 sinon")
    void permissionRapportActivite() {
        Ouverture rapport = ouverture("RAPPORT_ACTIVITE", 10L);
        erreur(() -> service.ouvrir(rapport, new Demandeur(AGENT, null, Set.of("DOC_DEPOSIT"))), 403, "DOC_PURPOSE_FORBIDDEN");
        erreur(() -> service.ouvrir(rapport, new Demandeur(AGENT, null, Set.of("COL_MESSAGE_SEND"))), 403, "DOC_PURPOSE_FORBIDDEN");
        assertThat(service.ouvrir(rapport, new Demandeur(AGENT, null, Set.of("PIL_ACTIVITY_OWN"))).etat()).isEqualTo(EtatDepotFragmente.OUVERT);

        Ouverture adhesion = ouverture("ADHESION", 10L);
        erreur(() -> service.ouvrir(adhesion, new Demandeur(AGENT, null, Set.of("PIL_ACTIVITY_OWN"))), 403, "DOC_PURPOSE_FORBIDDEN");
        Ouverture messagerie = ouverture("MESSAGERIE", 10L);
        erreur(() -> service.ouvrir(messagerie, new Demandeur(AGENT, null, Set.of("PIL_ACTIVITY_OWN"))), 403, "DOC_PURPOSE_FORBIDDEN");
        assertThat(stockage.envoisEnCours()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-87 : PROCESSUS_UNITE exige PIL_ACTIVITY_OWN, ni COL_MESSAGE_SEND ni une permission du dépôt direct ; référence = identifiant de l'instance")
    void permissionProcessusUnite() {
        String instance = UUID.randomUUID().toString();
        Ouverture livrable = new Ouverture("note.pdf", "application/pdf", 10L, "processus_unite", instance);
        erreur(() -> service.ouvrir(livrable, new Demandeur(AGENT, null, Set.of("DOC_DEPOSIT"))), 403, "DOC_PURPOSE_FORBIDDEN");
        erreur(() -> service.ouvrir(livrable, new Demandeur(AGENT, null, Set.of("COL_MESSAGE_SEND"))), 403, "DOC_PURPOSE_FORBIDDEN");
        erreur(() -> service.ouvrir(livrable, new Demandeur(AGENT, null, Set.of("HOM_STEP_MANAGE", "PAR_ADHESION_INSTRUCT"))), 403, "DOC_PURPOSE_FORBIDDEN");
        var ouvert = service.ouvrir(livrable, new Demandeur(AGENT, null, Set.of("PIL_ACTIVITY_OWN")));
        assertThat(ouvert.etat()).isEqualTo(EtatDepotFragmente.OUVERT);
        assertThat(base.depot(ouvert.id()).getFinalite()).isEqualTo("PROCESSUS_UNITE");
        assertThat(base.depot(ouvert.id()).getReference()).isEqualTo(instance);
        assertThat(stockage.envoisEnCours()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-116 : RAPPORT_MONITORING exige MON_SUPERVISION_READ, ni PIL_ACTIVITY_OWN ni une permission du dépôt direct")
    void permissionRapportMonitoring() {
        Ouverture rapport = new Ouverture("rapport-nuit.pdf", "application/pdf", 10L, "rapport_monitoring", "2026-10-20");
        erreur(() -> service.ouvrir(rapport, new Demandeur(AGENT, null, Set.of("DOC_DEPOSIT"))), 403, "DOC_PURPOSE_FORBIDDEN");
        erreur(() -> service.ouvrir(rapport, new Demandeur(AGENT, null, Set.of("PIL_ACTIVITY_OWN"))), 403, "DOC_PURPOSE_FORBIDDEN");
        var ouvert = service.ouvrir(rapport, new Demandeur(AGENT, null, Set.of("MON_SUPERVISION_READ")));
        assertThat(ouvert.etat()).isEqualTo(EtatDepotFragmente.OUVERT);
        assertThat(base.depot(ouvert.id()).getFinalite()).isEqualTo("RAPPORT_MONITORING");
    }

    @Test
    @DisplayName("Admission : taille absente, négative ou nulle (400), au-delà du maximum (413), finalité et référence (400) ; rien n'est ouvert")
    void admission() {
        erreur(() -> service.ouvrir(ouverture("ADHESION", null), DEMANDEUR), 400, "DOC_SIZE_INVALID");
        erreur(() -> service.ouvrir(ouverture("ADHESION", -1L), DEMANDEUR), 400, "DOC_SIZE_INVALID");
        erreur(() -> service.ouvrir(ouverture("ADHESION", 0L), DEMANDEUR), 400, "DOC_FILE_EMPTY");
        assertThatThrownBy(() -> service.ouvrir(ouverture("ADHESION", 1001L), DEMANDEUR)).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(413);
            assertThat(e.code()).isEqualTo("DOC_FILE_TOO_LARGE");
            assertThat(e.details()).containsEntry("tailleMax", 1000L);
        });
        erreur(() -> service.ouvrir(ouverture("a", 10L), DEMANDEUR), 400, "DOC_PURPOSE_INVALID");
        erreur(() -> service.ouvrir(new Ouverture("f", "text/plain", 10L, "ADHESION", " "), DEMANDEUR), 400, "DOC_REFERENCE_INVALID");
        assertThat(service.ouvrir(ouverture("ADHESION", 1000L), DEMANDEUR).nombreParties()).isEqualTo(125);
        assertThat(stockage.envoisEnCours()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ parties

    @Test
    @DisplayName("Parties : reçues et listées dans l'ordre, activité mise à jour ; renvoi d'une partie (reprise) accepté")
    void parties() {
        UUID id = ouvrir().id();
        horloge.avancer(Duration.ofMinutes(5));
        envoyer(id, 3);
        envoyer(id, 1);
        envoyer(id, 1);

        Etat e = service.etat(id, AGENT);
        assertThat(e.partiesRecues()).containsExactly(1, 3);
        assertThat(e.expireLe()).isEqualTo(DEBUT.plus(Duration.ofMinutes(5)).plus(Duration.ofHours(24)));
        assertThat(base.depot(id).partiesManquantes()).containsExactly(2, 4);
    }

    @Test
    @DisplayName("Partie renvoyée : la dernière version remplace la précédente dans l'objet assemblé")
    void partieRemplacee() {
        UUID id = service.ouvrir(ouverture("ADHESION", 16L), DEMANDEUR).id();
        service.recevoirPartie(id, 1, corps("AAAAAAAA".getBytes(StandardCharsets.US_ASCII)), 8, AGENT);
        service.recevoirPartie(id, 1, corps("BBBBBBBB".getBytes(StandardCharsets.US_ASCII)), -1, AGENT);
        service.recevoirPartie(id, 2, corps("CCCCCCCC".getBytes(StandardCharsets.US_ASCII)), 8, AGENT);
        service.terminer(id, AGENT);
        assertThat(new String(lireDepot(id.toString()), StandardCharsets.US_ASCII)).isEqualTo("BBBBBBBBCCCCCCCC");
    }

    byte[] lireDepot(String cle) {
        try (InputStream in = stockage.lireDepot(cle).orElseThrow().contenu()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Partie de taille inexacte (plus courte, plus longue ou longueur annoncée différente) : 400 DOC_PART_SIZE, rien n'est envoyé")
    void tailleDePartie() {
        UUID id = ouvrir().id();
        assertThatThrownBy(() -> service.recevoirPartie(id, 1, corps(new byte[7]), -1, AGENT)).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(400);
            assertThat(e.code()).isEqualTo("DOC_PART_SIZE");
            assertThat(e.details()).containsEntry("numero", 1).containsEntry("attendue", 8L).containsEntry("recue", 7L);
        });
        erreur(() -> service.recevoirPartie(id, 1, corps(new byte[9]), -1, AGENT), 400, "DOC_PART_SIZE");
        erreur(() -> service.recevoirPartie(id, 4, corps(new byte[8]), 8, AGENT), 400, "DOC_PART_SIZE");
        erreur(() -> service.recevoirPartie(id, 4, corps(new byte[2]), 3, AGENT), 400, "DOC_PART_SIZE");
        verify(stockage, never()).deposerPartie(anyString(), anyString(), anyInt(), any());
        assertThat(service.etat(id, AGENT).partiesRecues()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 5 })
    @DisplayName("Numéro de partie hors du dépôt : 400 DOC_PART_NUMBER_INVALID")
    void numeroHorsDepot(int numero) {
        UUID id = ouvrir().id();
        erreur(() -> service.recevoirPartie(id, numero, corps(new byte[8]), 8, AGENT), 400, "DOC_PART_NUMBER_INVALID");
    }

    @Test
    @DisplayName("Corps illisible : 400 DOC_FILE_UNREADABLE")
    void corpsIllisible() {
        UUID id = ouvrir().id();
        InputStream illisible = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connexion coupée");
            }
        };
        erreur(() -> service.recevoirPartie(id, 1, illisible, 8, AGENT), 400, "DOC_FILE_UNREADABLE");
    }

    @Test
    @DisplayName("Dépôt d'un autre compte ou inconnu : 404 DOC_UPLOAD_NOT_FOUND pour chaque action, sans révéler son existence")
    void autreCompte() {
        UUID id = ouvrir().id();
        String autre = "autre-agent";
        erreur(() -> service.recevoirPartie(id, 1, corps(new byte[8]), 8, autre), 404, "DOC_UPLOAD_NOT_FOUND");
        erreur(() -> service.etat(id, autre), 404, "DOC_UPLOAD_NOT_FOUND");
        erreur(() -> service.terminer(id, autre), 404, "DOC_UPLOAD_NOT_FOUND");
        erreur(() -> service.abandonner(id, autre), 404, "DOC_UPLOAD_NOT_FOUND");
        UUID inconnu = UUID.randomUUID();
        assertThatThrownBy(() -> service.etat(inconnu, AGENT)).hasFieldOrPropertyWithValue("details", Map.of("id", inconnu.toString()));
        assertThat(base.depot(id).getEtat()).isEqualTo(EtatDepotFragmente.OUVERT);
    }

    @Test
    @DisplayName("Partie arrivée pendant la fin du dépôt : 409 DOC_UPLOAD_CLOSED, la partie n'est pas inscrite")
    void partiePendantLaFin() {
        UUID id = ouvrir().id();
        doAnswer(i -> {
            Object etag = i.callRealMethod();
            base.depot(id).etat(EtatDepotFragmente.EN_VERIFICATION);
            return etag;
        })
            .when(stockage)
            .deposerPartie(anyString(), anyString(), anyInt(), any());
        erreur(() -> envoyer(id, 1), 409, "DOC_UPLOAD_CLOSED");
        assertThat(base.depot(id).getParties()).isEmpty();
    }

    // ------------------------------------------------------------------ fin

    @Test
    @DisplayName("Fin incomplète : 409 DOC_UPLOAD_INCOMPLETE avec details.manquantes ; rien n'est assemblé")
    void finIncomplete() {
        UUID id = ouvrir().id();
        envoyer(id, 1);
        envoyer(id, 3);
        assertThatThrownBy(() -> service.terminer(id, AGENT)).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.code()).isEqualTo("DOC_UPLOAD_INCOMPLETE");
            assertThat(e.details()).containsEntry("manquantes", List.of(2, 4)).containsEntry("nombreParties", 4);
        });
        assertThat(base.depot(id).getEtat()).isEqualTo(EtatDepotFragmente.OUVERT);
        assertThat(stockage.depotExiste(id.toString())).isFalse();
    }

    @Test
    @DisplayName("Fin : parties assemblées dans l'ordre, EN_VERIFICATION ; fin renvoyée sans nouvel assemblage ; plus aucune partie acceptée")
    void fin() {
        UUID id = ouvrir().id();
        for (int n = 4; n >= 1; n--) {
            envoyer(id, n);
        }
        horloge.avancer(Duration.ofMinutes(1));
        Etat e = service.terminer(id, AGENT);

        assertThat(e.etat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(e.expireLe()).isNull();
        assertThat(lireDepot(id.toString())).isEqualTo(FICHIER);
        assertThat(stockage.envoisEnCours()).isZero();
        assertThat(base.depot(id).getActifLe()).isEqualTo(DEBUT.plus(Duration.ofMinutes(1)));

        assertThat(service.terminer(id, AGENT).etat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        verify(stockage).assemblerDepot(anyString(), anyString(), any());
        erreur(() -> envoyer(id, 1), 409, "DOC_UPLOAD_CLOSED");
        erreur(() -> service.abandonner(id, AGENT), 409, "DOC_UPLOAD_CLOSED");
    }

    @Test
    @DisplayName("Dépôt objet indisponible à l'assemblage : 503, le dépôt reste OUVERT")
    void finIndisponible() {
        UUID id = ouvrir().id();
        for (int n = 1; n <= 4; n++) {
            envoyer(id, n);
        }
        stockage.simulerPanne(true);
        erreur(() -> service.terminer(id, AGENT), 503, "DOC_STORAGE_UNAVAILABLE");
        assertThat(base.depot(id).getEtat()).isEqualTo(EtatDepotFragmente.OUVERT);
        stockage.simulerPanne(false);
        assertThat(service.terminer(id, AGENT).etat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
    }

    // ------------------------------------------------------------------ abandon et expiration

    @Test
    @DisplayName("Abandon : ABANDONNE, téléversement abandonné ; renvoyé, sans effet ; plus aucune partie ni fin")
    void abandon() {
        UUID id = ouvrir().id();
        envoyer(id, 1);
        service.abandonner(id, AGENT);

        assertThat(service.etat(id, AGENT).etat()).isEqualTo(EtatDepotFragmente.ABANDONNE);
        assertThat(stockage.envoisEnCours()).isZero();
        service.abandonner(id, AGENT);
        verify(stockage).abandonnerDepot(anyString(), anyString());
        erreur(() -> envoyer(id, 2), 409, "DOC_UPLOAD_CLOSED");
        erreur(() -> service.terminer(id, AGENT), 409, "DOC_UPLOAD_CLOSED");
    }

    @Test
    @DisplayName("Abandon : échec de l'abandon du téléversement seulement journalisé (cycle de vie du compartiment)")
    void abandonDepotObjetIndisponible() {
        UUID id = ouvrir().id();
        doThrow(DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "panne")).when(stockage).abandonnerDepot(anyString(), anyString());
        service.abandonner(id, AGENT);
        assertThat(base.depot(id).getEtat()).isEqualTo(EtatDepotFragmente.ABANDONNE);
    }

    @Test
    @DisplayName("Expiration : dépôt OUVERT sans activité depuis 24 h abandonné avec son téléversement ; les autres restent")
    void expiration() {
        UUID ancien = ouvrir().id();
        horloge.avancer(Duration.ofHours(12));
        UUID recent = ouvrir().id();
        UUID termine = service.ouvrir(ouverture("ADHESION", 8L), DEMANDEUR).id();
        service.recevoirPartie(termine, 1, corps(new byte[8]), 8, AGENT);
        service.terminer(termine, AGENT);

        horloge.avancer(Duration.ofHours(12));
        assertThat(service.abandonnerInactifs()).isZero();
        horloge.avancer(Duration.ofSeconds(1));
        assertThat(service.abandonnerInactifs()).isEqualTo(1);

        assertThat(base.depot(ancien).getEtat()).isEqualTo(EtatDepotFragmente.ABANDONNE);
        assertThat(base.depot(recent).getEtat()).isEqualTo(EtatDepotFragmente.OUVERT);
        assertThat(base.depot(termine).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(stockage.envoisEnCours()).isEqualTo(1);

        horloge.avancer(Duration.ofHours(13));
        assertThat(service.abandonnerInactifs()).isEqualTo(1);
        assertThat(base.depot(recent).getEtat()).isEqualTo(EtatDepotFragmente.ABANDONNE);
        assertThat(base.depot(termine).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(stockage.envoisEnCours()).isZero();
    }

    @Test
    @DisplayName("Expiration : dépôt redevenu actif entre la recherche et le verrou, conservé")
    void expirationConcurrente() {
        UUID id = ouvrir().id();
        horloge.avancer(Duration.ofHours(25));
        doAnswer(i -> {
            base.depot(id).actifLe(horloge.instant());
            return Optional.of(base.depot(id));
        })
            .when(base.depotRepository)
            .verrouiller(id);
        assertThat(service.abandonnerInactifs()).isZero();
        assertThat(base.depot(id).getEtat()).isEqualTo(EtatDepotFragmente.OUVERT);
    }

    @Test
    @DisplayName("Configuration : parties de moins de 5 Mio ou plus de 10 000 parties refusées ; valeurs par défaut")
    void configuration() {
        assertThatThrownBy(PROPRIETES::verifier).isInstanceOf(IllegalStateException.class).hasMessageContaining("5 Mio");
        DepotFragmenteProprietes trop = new DepotFragmenteProprietes(null, DataSize.ofGigabytes(200), null, null, null, null);
        assertThatThrownBy(trop::verifier).isInstanceOf(IllegalStateException.class).hasMessageContaining("10 000");
        DepotFragmenteProprietes enorme = new DepotFragmenteProprietes(null, null, DataSize.ofGigabytes(3), null, null, null);
        assertThatThrownBy(enorme::verifier).isInstanceOf(IllegalStateException.class);

        DepotFragmenteProprietes p = DepotFragmenteProprietes.parDefaut().verifier();
        assertThat(p.tailleMax()).isEqualTo(DataSize.ofMegabytes(20));
        assertThat(p.tailleMaxFragmente()).isEqualTo(DataSize.ofGigabytes(20));
        assertThat(p.taillePartie().toBytes()).isEqualTo(16L * 1024 * 1024);
        assertThat(p.inactivite()).isEqualTo(Duration.ofHours(24));
        assertThat(p.bailVerification()).isEqualTo(Duration.ofHours(1));
        assertThat(p.delaiNouvelleTentative()).isEqualTo(Duration.ofMinutes(1));
        assertThat(p.nombreParties(4L * 1024 * 1024 * 1024)).isEqualTo(256);
        assertThat(
            new DepotFragmenteService(stockage, base.depotRepository, base.transactions, DepotFragmenteProprietes.parDefaut())
        ).isNotNull();
    }
}
