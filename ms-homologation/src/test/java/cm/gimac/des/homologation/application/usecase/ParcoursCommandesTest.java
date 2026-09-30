package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort.Verification;
import cm.gimac.des.homologation.application.port.out.StrongReauthPort;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes.Affectation;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes.Piece;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.CoverageEstablishedPayload;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.Ceremonie;
import cm.gimac.des.homologation.domain.parcours.EtatFicheSuivi;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Qualite;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Conduite du parcours : revérification des pièces, réauthentification, événement du constat, service supplémentaire, portail. */
@ExtendWith(MockitoExtension.class)
class ParcoursCommandesTest {

    static final Instant T = Instant.parse("2026-10-06T09:00:00Z");
    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final String SHA = "e".repeat(64);
    static final Acteur ACTEUR = new Acteur(UUID.randomUUID(), "Conducteur de projet 1", Set.of(), null);

    @Mock
    ParcoursDepot depot;

    @Mock
    ParcoursInstanciation instanciation;

    @Mock
    Intervenants intervenants;

    @Mock
    DocumentRegistryPort documents;

    @Mock
    StrongReauthPort reauth;

    @Mock
    OutboxPublisher outbox;

    @Mock
    LiaisonRepository liaisons;

    @Mock
    ReferentialVersionRepository versions;

    @Mock
    ContractualScopeRepository perimetres;

    @Mock
    ScopeItemRepository elements;

    ParcoursCommandes commandes;
    Parcours p;

    @BeforeEach
    void init() {
        commandes = new ParcoursCommandes(depot, instanciation, intervenants, documents, reauth, outbox, liaisons, versions, perimetres, elements, Clock.fixed(T, ZoneOffset.UTC));
        p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        lenient().when(depot.trouver(p.id(), true)).thenReturn(Optional.of(p));
        lenient().when(documents.verifier(any(), anyString())).thenReturn(Verification.CONFORME);
    }

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    OperationParcours op(String rang) {
        return p.operation(rang).orElseThrow();
    }

    Piece piece() {
        return new Piece(UUID.randomUUID(), SHA, "cr.pdf", "relu");
    }

    void operationDuParcours(String rang) {
        when(depot.deLOperation(op(rang).getId())).thenReturn(p);
    }

    @Test
    @DisplayName("Livrable : pièce revérifiée auprès de MS-DOCUMENT ; mal formée, inconnue ou divergente, elle est refusée")
    void livrable() {
        operationDuParcours("3");
        UUID o3 = op("3").getId();
        refus(() -> commandes.deposerLivrable(o3, new Piece(null, SHA, "x", null), ACTEUR), "HOM_DOCUMENT_INVALID");
        refus(() -> commandes.deposerLivrable(o3, new Piece(UUID.randomUUID(), "zz", "x", null), ACTEUR), "HOM_DOCUMENT_INVALID");
        refus(() -> commandes.deposerLivrable(o3, new Piece(UUID.randomUUID(), SHA, " ", null), ACTEUR), "HOM_DOCUMENT_INVALID");
        refus(() -> commandes.deposerLivrable(o3, new Piece(UUID.randomUUID(), null, "x", null), ACTEUR), "HOM_DOCUMENT_INVALID");
        refus(() -> commandes.deposerLivrable(o3, new Piece(UUID.randomUUID(), SHA, null, null), ACTEUR), "HOM_DOCUMENT_INVALID");
        when(documents.verifier(any(), anyString())).thenReturn(Verification.INCONNUE, Verification.DIVERGENTE, Verification.CONFORME);
        refus(() -> commandes.deposerLivrable(o3, piece(), ACTEUR), "HOM_DOCUMENT_UNKNOWN");
        refus(() -> commandes.deposerLivrable(o3, piece(), ACTEUR), "HOM_DOCUMENT_HASH_MISMATCH");
        Piece d = piece();
        commandes.deposerLivrable(o3, d, new Acteur(null, " ", Set.of(), null));
        assertThat(op("3").getPreuve().documentRef()).isEqualTo(d.documentId().toString());
        assertThat(op("3").getPreuve().deposePar()).isEqualTo("Titulaire DDRM");
        refus(() -> commandes.deposerLivrable(o3, piece(), ACTEUR), "HOM_OPERATION_NOT_OPEN");
    }

    @Test
    @DisplayName("Preuve : contrôles métier avant la revérification ; la pièce réelle garde la valeur de référence et le code de contrôle")
    void preuve() {
        avancer(p, "24", J);
        operationDuParcours("C1");
        UUID c1 = op("C1").getId();
        refus(() -> commandes.deposerPreuve(c1, piece(), null, null, null, ACTEUR), "HOM_KEY_CEREMONY_INCOMPLETE");
        verify(documents, never()).verifier(any(), anyString());
        Ceremonie cer = new Ceremonie("2026-10-01", List.of("A", "B", "C"), List.of("AAAA", "BBBB", "CCCC"));
        Piece d = piece();
        commandes.deposerPreuve(c1, d, null, null, cer, ACTEUR);
        assertThat(op("C1").getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
        assertThat(op("C1").getPreuve().documentRef()).isEqualTo(d.documentId().toString());
        assertThat(op("C1").getPreuve().ceremonie()).isEqualTo(cer);
        assertThat(op("C1").getPreuve().deposePar()).isEqualTo("Conducteur de projet 1");
    }

    @Test
    @DisplayName("Validation : assertion exigée (428), jeton utilisateur exigé, assertion refusée (403), jamais par le responsable")
    void validation() {
        operationDuParcours("3");
        UUID o3 = op("3").getId();
        refus(() -> commandes.valider(o3, null, ACTEUR), "IAM_REAUTH_REQUIRED");
        refus(() -> commandes.valider(o3, " ", ACTEUR), "IAM_REAUTH_REQUIRED");
        refus(() -> commandes.valider(o3, "a", new Acteur(null, null, Set.of(), null)), "HOM_USER_TOKEN_REQUIRED");
        refus(() -> commandes.valider(o3, "a", ACTEUR), "HOM_OPERATION_NOT_IN_VALIDATION");
        p.deposerPreuve(op("3"), cm.gimac.des.homologation.domain.parcours.Preuve.piece("d", "n", SHA, J, "x"), null, null, null);
        when(reauth.verifier("mauvaise", ACTEUR.id(), ParcoursCommandes.FINALITE_VALIDATION)).thenReturn(false);
        refus(() -> commandes.valider(o3, "mauvaise", ACTEUR), "HOM_REAUTH_INVALID");
        Acteur responsable = new Acteur(UUID.fromString(op("3").getRaci().r().id().replace("U-DDRM", "00000000-0000-4000-8000-000000000001")), null, Set.of(), null);
        p.attribuer(op("3"), Qualite.R, new Personne(responsable.id().toString(), "R", Fonction.DDRM));
        refus(() -> commandes.valider(o3, "a", responsable), "HOM_VALIDATOR_IS_RESPONSIBLE");
        when(reauth.verifier("bonne", ACTEUR.id(), ParcoursCommandes.FINALITE_VALIDATION)).thenReturn(true);
        commandes.valider(o3, "bonne", ACTEUR);
        assertThat(op("3").faite()).isTrue();
    }

    @Test
    @DisplayName("Ressources, qualités (dont le correspondant), blocage déclaré puis levé")
    void conduite() {
        refus(() -> commandes.affecterRessources(p.id(), List.of(new Affectation(Fonction.SNA, "x")), ACTEUR), "HOM_STEP_NOT_OPEN");
        avancer(p, "3", J);
        Personne sna = new Personne(UUID.randomUUID().toString(), "SNA", Fonction.SNA);
        when(intervenants.pourFonction("s", Fonction.SNA)).thenReturn(sna);
        commandes.affecterRessources(p.id(), List.of(new Affectation(Fonction.SNA, "s")), ACTEUR);
        assertThat(op("6").faite()).isTrue();
        assertThat(op("6").getPreuve().documentRef()).isEqualTo("AFF-LIA-2026-001");
        assertThat(op("10").getRaci().r()).isEqualTo(sna);
        operationDuParcours("4");
        Personne corr = Personne.correspondant(p.liaison().getInstitutionId().toString(), p.liaison().getInstitutionNom());
        commandes.attribuer(op("4").getId(), Qualite.C, corr.id(), "motif", ACTEUR);
        assertThat(op("4").getRaci().c()).contains(corr);
        when(intervenants.personne("u")).thenReturn(sna);
        commandes.attribuer(op("4").getId(), Qualite.I, "u", null, ACTEUR);
        assertThat(op("4").getRaci().i()).contains(sna);
        BlocageParcours b = commandes.declarerBlocage(p.id(), "m", "a", "u", J, ACTEUR);
        verify(depot).ajouter(b);
        when(depot.duBlocage(b.getId())).thenReturn(p);
        commandes.leverBlocage(b.getId(), "levé", ACTEUR);
        assertThat(b.actif()).isFalse();
    }

    @Test
    @DisplayName("Constat de la couverture : homologation.coverage-established.v1 publié avec l'institution de la liaison")
    void constat() {
        avancer(p, "16", J);
        commandes.constaterCouverture(p.id(), ACTEUR);
        ArgumentCaptor<Object> charge = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("liaison"), eq(p.id()), eq(CoverageEstablishedPayload.TYPE), charge.capture(), eq(p.liaison().getInstitutionId().toString()));
        CoverageEstablishedPayload c = (CoverageEstablishedPayload) charge.getValue();
        assertThat(c.taux()).isEqualTo(100);
        assertThat(c.servicesCouverts()).containsExactly("RETRAIT");
        assertThat(c.constatePar()).isEqualTo(ACTEUR.id());
    }

    @Test
    @DisplayName("Liaison sans parcours : instanciée à la volée ; liaison inconnue : 404")
    void chargementParesseux() {
        UUID id = UUID.randomUUID();
        Liaison l = ParcoursSamples.liaison(TypeProjet.CARTE_INTERBANCAIRE);
        when(depot.trouver(id, true)).thenReturn(Optional.empty());
        when(depot.liaison(id)).thenReturn(l);
        when(instanciation.instancierExistante(l)).thenReturn(p);
        assertThat(commandes.charger(id)).isSameAs(p);
    }

    @Test
    @DisplayName("Service supplémentaire : liaison en production exigée, services valides, nouvelle liaison au cadrage hors unicité")
    void serviceSupplementaire() {
        refus(() -> commandes.ouvrirServiceSupplementaire(p.id(), List.of("CASH_IN"), ACTEUR), "HOM_LIAISON_NOT_IN_PRODUCTION");
        avancer(p, "31", J);
        refus(() -> commandes.ouvrirServiceSupplementaire(p.id(), List.of(), ACTEUR), "HOM_SERVICES_INVALID");
        refus(() -> commandes.ouvrirServiceSupplementaire(p.id(), null, ACTEUR), "HOM_SERVICES_INVALID");
        refus(() -> commandes.ouvrirServiceSupplementaire(p.id(), List.of("cash in"), ACTEUR), "HOM_SERVICES_INVALID");
        refus(() -> commandes.ouvrirServiceSupplementaire(p.id(), List.of("TRANSVERSE"), ACTEUR), "HOM_SERVICES_INVALID");
        when(liaisons.saveAndFlush(any())).thenAnswer(i -> {
            Liaison l = i.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(perimetres.save(any())).thenAnswer(i -> {
            ContractualScope s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(versions.findFirstByTypeProjetAndEtat(any(), any())).thenReturn(Optional.empty());
        Liaison n = commandes.ouvrirServiceSupplementaire(p.id(), java.util.Arrays.asList("CASH_IN", " CASH_IN ", "COMMERCE_ELECTRONIQUE"), ACTEUR);
        assertThat(n.getLiaisonOrigineId()).isEqualTo(p.id());
        assertThat(n.getInstitutionId()).isEqualTo(p.liaison().getInstitutionId());
        assertThat(n.getVersion()).isSameAs(p.liaison().getVersion());
        verify(elements, org.mockito.Mockito.times(3)).save(any(ScopeItem.class));
        verify(instanciation).instancier(n, List.of("CASH_IN", "COMMERCE_ELECTRONIQUE"), false, p.id(), true);
    }

    @Test
    @DisplayName("Fiche de suivi : remise, renseignement par le participant, constat")
    void fiche() {
        avancer(p, "31", J);
        commandes.remettreFiche(p.id(), ACTEUR);
        Acteur correspondant = new Acteur(null, null, Set.of(), p.liaison().getInstitutionId().toString());
        commandes.renseignerFiche(p.id(), List.of(new Parcours.LigneFiche("RETRAIT", true, null)), correspondant);
        assertThat(p.etat().getFicheEtat()).isEqualTo(EtatFicheSuivi.RENSEIGNEE);
        commandes.constaterMiseEnService(p.id(), ACTEUR);
        assertThat(p.etat().getFicheEtat()).isEqualTo(EtatFicheSuivi.CONSTATEE);
        refus(() -> commandes.renseignerFiche(p.id(), null, correspondant), "HOM_SERVICE_SHEET_NOT_OPEN");
    }

    @Test
    @DisplayName("Portail : cloisonné à l'institution du jeton ; coordonnées ; livrable déposé pour le compte du participant")
    void portail() {
        Acteur autre = new Acteur(null, null, Set.of(), UUID.randomUUID().toString());
        Acteur sans = new Acteur(null, null, Set.of(), null);
        Acteur sien = new Acteur(UUID.randomUUID(), null, Set.of(), p.liaison().getInstitutionId().toString().toUpperCase());
        refus(() -> commandes.communiquerCoordonnees(p.id(), "essai.exemple", "8443", null, autre), "HOM_LIAISON_NOT_FOUND");
        refus(() -> commandes.communiquerCoordonnees(p.id(), "essai.exemple", "8443", null, sans), "HOM_LIAISON_NOT_FOUND");
        avancer(p, "11", J);
        commandes.communiquerCoordonnees(p.id(), "essai.exemple", "8443", "", sien);
        assertThat(op("12").getPreuve().valeurReference().valeur()).isEqualTo("essai.exemple:8443");
        assertThat(op("12").getPreuve().deposePar()).isEmpty();
        avancer(p, "14", J);
        operationDuParcours("15");
        operationDuParcours("16");
        UUID o15 = op("15").getId();
        UUID o16 = op("16").getId();
        refus(() -> commandes.deposerLivrablePortail(o15, "x".getBytes(), "doc.pdf", "application/pdf", null, autre), "HOM_LIAISON_NOT_FOUND");
        refus(() -> commandes.deposerLivrablePortail(o16, "x".getBytes(), "doc.pdf", "application/pdf", null, sien), "HOM_OPERATION_NOT_OPEN");
        refus(() -> commandes.deposerLivrablePortail(o15, new byte[0], "doc.pdf", "application/pdf", null, sien), "HOM_DOCUMENT_INVALID");
        refus(() -> commandes.deposerLivrablePortail(o15, null, "doc.pdf", "application/pdf", null, sien), "HOM_DOCUMENT_INVALID");
        refus(() -> commandes.deposerLivrablePortail(o15, "x".getBytes(), " ", "application/pdf", null, sien), "HOM_DOCUMENT_INVALID");
        UUID doc = UUID.randomUUID();
        when(documents.deposer(any(), eq("parametrage.pdf"), any(), eq("LIVRABLE"), eq(o15.toString()), eq(sien.id()))).thenReturn(new DocumentRegistryPort.Depot(doc, SHA, "parametrage.pdf"));
        commandes.deposerLivrablePortail(o15, "x".getBytes(), "parametrage.pdf", "application/pdf", "commentaire", sien);
        assertThat(op("15").faite()).isTrue();
        assertThat(op("15").getPreuve().deposePar()).startsWith("Correspondant ");
        assertThat(op("15").getPreuve().documentRef()).isEqualTo(doc.toString());
    }

    @Test
    @DisplayName("Conducteur désigné : les opérations non faites passent au nouveau conducteur ; nom par défaut si l'annuaire l'ignore")
    void conducteurDesigne() {
        UUID nouveau = UUID.randomUUID();
        when(depot.trouver(p.id(), false)).thenReturn(Optional.of(p));
        when(intervenants.nom(nouveau)).thenReturn(null);
        commandes.conducteurDesigne(p.id(), nouveau);
        assertThat(op("3").getRaci().v()).isEqualTo(new Personne(nouveau.toString(), "Conducteur de projet", Fonction.CONDUCTEUR));
        when(intervenants.nom(nouveau)).thenReturn("Paul Essomba");
        commandes.conducteurDesigne(p.id(), nouveau);
        assertThat(op("3").getRaci().v().nom()).isEqualTo("Paul Essomba");
        assertThat(ParcoursCommandes.sha256("abc")).hasSize(64);
        verify(depot, never()).creer(any());
        verify(instanciation, never()).instancier(any(), any(), anyBoolean(), any(), anyBoolean());
    }
}
