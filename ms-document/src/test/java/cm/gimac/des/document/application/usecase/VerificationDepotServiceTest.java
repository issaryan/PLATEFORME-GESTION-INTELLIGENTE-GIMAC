package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Demandeur;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Etat;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Ouverture;
import cm.gimac.des.document.application.port.out.ExtracteurTexte;
import cm.gimac.des.document.application.port.out.StockageObjet.EcritureWorm;
import cm.gimac.des.document.application.port.out.StockageObjet.Flux;
import cm.gimac.des.document.application.port.out.StockageObjet.ObjetDepose;
import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DepotFragmente;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.infrastructure.stockage.StockageObjetMemoire;
import cm.gimac.des.document.infrastructure.texte.ExtracteurTextePdfBox;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * Vérification serveur des dépôts fragmentés (D-74) : lecture en une passe vers le dépôt WORM,
 * empreinte, taille, numéros de carte jusqu'au seuil, déduplication, rejets, reprise après une
 * panne ou un arrêt, plusieurs instances. Substitut du dépôt objet (règles multipart de S3) et
 * base simulée ; parties de 8 octets, contrôle des numéros de carte jusqu'à 64 octets.
 */
class VerificationDepotServiceTest {

    static final Instant DEBUT = Instant.parse("2026-10-02T08:00:00Z");
    static final String AGENT = "7c1e0000-0000-0000-0000-000000000001";
    static final byte[] FICHIER = "Compte rendu du comite du 2 octobre 2026".getBytes(StandardCharsets.US_ASCII);

    static final DepotFragmenteProprietes PROPRIETES = new DepotFragmenteProprietes(
        DataSize.ofBytes(64),
        DataSize.ofBytes(10_000),
        DataSize.ofBytes(8),
        Duration.ofHours(24),
        Duration.ofHours(1),
        Duration.ofMinutes(1)
    );

    BaseEnMemoire base;
    BaseEnMemoire.Horloge horloge;
    StockageObjetMemoire stockage;
    ExtracteurTexte extracteur;
    DepotFragmenteService depots;
    VerificationDepotService verification;

    @BeforeEach
    void init() {
        base = new BaseEnMemoire();
        horloge = new BaseEnMemoire.Horloge(DEBUT);
        stockage = spy(new StockageObjetMemoire(1));
        extracteur = spy(new ExtracteurTextePdfBox());
        depots = new DepotFragmenteService(stockage, base.depotRepository, base.transactions, PROPRIETES, horloge);
        verification = new VerificationDepotService(
            stockage,
            extracteur,
            base.depotRepository,
            base.documentRepository,
            base.transactions,
            PROPRIETES,
            horloge
        );
    }

    /** Dépôt complet puis terminé, prêt pour la vérification. */
    UUID deposer(String deposant, byte[] contenu, String type) {
        Etat e = depots.ouvrir(
            new Ouverture("cr.txt", type, (long) contenu.length, "MESSAGERIE", "conversation/c1"),
            new Demandeur(deposant, null, Set.of("COL_MESSAGE_SEND"))
        );
        for (int n = 1; n <= e.nombreParties(); n++) {
            byte[] partie = Arrays.copyOfRange(contenu, (n - 1) * 8, Math.min(n * 8, contenu.length));
            depots.recevoirPartie(e.id(), n, new ByteArrayInputStream(partie), partie.length, deposant);
        }
        depots.terminer(e.id(), deposant);
        return e.id();
    }

    UUID deposer(byte[] contenu) {
        return deposer(AGENT, contenu, "text/plain");
    }

    DepotFragmente depot(UUID id) {
        return base.depot(id);
    }

    /** Écritures WORM ouvertes passées par {@code transformation} (pannes et arrêts simulés). */
    void ecrituresWorm(UnaryOperator<EcritureWorm> transformation) {
        doAnswer(i -> transformation.apply((EcritureWorm) i.callRealMethod()))
            .when(stockage)
            .ouvrirEcriture(anyString(), anyString());
    }

    /** Écriture WORM déléguée, dont une opération peut être remplacée. */
    static class EcritureDeleguee implements EcritureWorm {

        final EcritureWorm cible;

        EcritureDeleguee(EcritureWorm cible) {
            this.cible = cible;
        }

        @Override
        public String envoi() {
            return cible.envoi();
        }

        @Override
        public void ecrire(byte[] tranche, int longueur) {
            cible.ecrire(tranche, longueur);
        }

        @Override
        public ObjetDepose terminer() {
            return cible.terminer();
        }

        @Override
        public void abandonner() {
            cible.abandonner();
        }
    }

    // ------------------------------------------------------------------ vérification

    @Test
    @DisplayName("Vérification : DISPONIBLE, document inscrit, objet WORM identique sous finalite/aaaa/mm/jj/documentId, dépôt supprimé")
    void verification() {
        UUID id = deposer(FICHIER);
        horloge.avancer(Duration.ofSeconds(5));
        assertThat(verification.verifierEnAttente()).isEqualTo(1);

        Etat e = depots.etat(id, AGENT);
        String sha = EmpreinteSha256.de(FICHIER);
        assertThat(e.etat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        assertThat(e.sha256()).isEqualTo(sha);
        assertThat(e.documentId()).isNotNull();
        assertThat(e.dedoublonne()).isFalse();
        assertThat(e.motif()).isNull();

        assertThat(base.documents).hasSize(1);
        DepotDocument d = base.documents.get(0);
        assertThat(d.getId()).isEqualTo(e.documentId());
        assertThat(d.getFinalite()).isEqualTo("MESSAGERIE");
        assertThat(d.getReference()).isEqualTo("conversation/c1");
        assertThat(d.getNomFichier()).isEqualTo("cr.txt");
        assertThat(d.getTaille()).isEqualTo(FICHIER.length);
        assertThat(d.getTypeMime()).isEqualTo("text/plain");
        assertThat(d.getSha256()).isEqualTo(sha);
        assertThat(d.getDeposant()).isEqualTo(AGENT);
        assertThat(d.getServiceDeposant()).isNull();
        assertThat(d.getDeposeLe()).isEqualTo(DEBUT.plus(Duration.ofSeconds(5)));
        assertThat(d.getCleObjet()).isEqualTo("messagerie/2026/10/02/" + e.documentId());
        assertThat(d.getVersionObjet()).isEqualTo("v1");
        assertThat(stockage.lire(d.getCleObjet())).hasValueSatisfying(o -> assertThat(o).isEqualTo(FICHIER));

        assertThat(stockage.depotExiste(id.toString())).isFalse();
        assertThat(stockage.envoisEnCours()).isZero();
        assertThat(depot(id).getEnvoiWorm()).isNull();
        assertThat(depot(id).getRepriseLe()).isNull();
        verify(extracteur).extraire(any(), eq("text/plain"));
        // Plus rien à vérifier.
        assertThat(verification.verifierEnAttente()).isZero();
    }

    @Test
    @DisplayName("BNF-16 : numéro de carte dans un fichier d'au plus 20 Mo, REJETE DOC_PAN_DETECTED, aucun objet WORM, dépôt supprimé")
    void numeroDeCarte() {
        UUID id = deposer("Carte 4111 1111 1111 1111 a rembourser".getBytes(StandardCharsets.US_ASCII));
        verification.verifier(id);

        Etat e = depots.etat(id, AGENT);
        assertThat(e.etat()).isEqualTo(EtatDepotFragmente.REJETE);
        assertThat(e.motif()).isEqualTo("DOC_PAN_DETECTED");
        assertThat(e.documentId()).isNull();
        assertThat(e.sha256()).isNull();
        assertThat(base.documents).isEmpty();
        assertThat(stockage.objetsWorm()).isZero();
        assertThat(stockage.envoisEnCours()).isZero();
        assertThat(stockage.depotExiste(id.toString())).isFalse();
    }

    @Test
    @DisplayName("Au-delà du seuil (20 Mo en production) : contrôle des numéros de carte non appliqué, texte non extrait")
    void auDelaDuSeuil() {
        byte[] gros = ("Carte 4111 1111 1111 1111 " + "x".repeat(80)).getBytes(StandardCharsets.US_ASCII);
        UUID id = deposer(gros);
        verification.verifier(id);

        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        verifyNoInteractions(extracteur);
        assertThat(stockage.lire(base.documents.get(0).getCleObjet())).hasValueSatisfying(o -> assertThat(o).isEqualTo(gros));
    }

    @Test
    @DisplayName("Signature PDF : type retenu application/pdf quel que soit le type déclaré ; PDF illisible, REJETE DOC_PDF_UNREADABLE")
    void signaturePdf() {
        byte[] pdf = "%PDF-1.7 contenu sans structure".getBytes(StandardCharsets.US_ASCII);
        UUID illisible = deposer(AGENT, pdf, "application/octet-stream");
        verification.verifier(illisible);
        assertThat(depot(illisible).getEtat()).isEqualTo(EtatDepotFragmente.REJETE);
        assertThat(depot(illisible).getMotif()).isEqualTo("DOC_PDF_UNREADABLE");
        verify(extracteur).extraire(any(), eq("application/pdf"));

        doReturn(Optional.of("Rapport")).when(extracteur).extraire(any(), eq("application/pdf"));
        byte[] autre = "%PDF-1.4 autre contenu".getBytes(StandardCharsets.US_ASCII);
        UUID lisible = deposer(AGENT, autre, "application/octet-stream");
        verification.verifier(lisible);
        assertThat(depot(lisible).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        assertThat(depot(lisible).getTypeMime()).isEqualTo("application/pdf");
        assertThat(depots.etat(lisible, AGENT).typeMime()).isEqualTo("application/pdf");
        assertThat(base.documents.get(0).getTypeMime()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Déduplication : même contenu déposé par un autre compte, nouveau document vers le même objet WORM, aucune écriture retenue")
    void deduplication() {
        UUID premier = deposer(FICHIER);
        verification.verifier(premier);
        horloge.avancer(Duration.ofMinutes(3));
        UUID second = deposer("autre-agent", FICHIER, "text/plain");
        verification.verifier(second);

        Etat e = depots.etat(second, "autre-agent");
        assertThat(e.etat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        assertThat(e.dedoublonne()).isTrue();
        assertThat(depots.etat(premier, AGENT).dedoublonne()).isFalse();
        assertThat(base.documents).hasSize(2);
        DepotDocument a = base.documents.get(0);
        DepotDocument b = base.documents.get(1);
        assertThat(b.getId()).isEqualTo(e.documentId()).isNotEqualTo(a.getId());
        assertThat(b.getDeposant()).isEqualTo("autre-agent");
        assertThat(b.getCleObjet()).isEqualTo(a.getCleObjet());
        assertThat(b.getVersionObjet()).isEqualTo(a.getVersionObjet());
        assertThat(b.getSha256()).isEqualTo(a.getSha256());
        assertThat(stockage.objetsWorm()).isEqualTo(1);
        assertThat(stockage.versions(a.getCleObjet())).isEqualTo(1);
        assertThat(stockage.envoisEnCours()).isZero();
    }

    @Test
    @DisplayName("Taille de l'objet assemblé différente de la taille annoncée : REJETE DOC_SIZE_MISMATCH, rien n'est écrit")
    void tailleAnnonceeDifferente() {
        UUID id = deposer(FICHIER);
        doReturn(Optional.of(new Flux(new ByteArrayInputStream(FICHIER), FICHIER.length - 1L))).when(stockage).lireDepot(id.toString());
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.REJETE);
        assertThat(depot(id).getMotif()).isEqualTo("DOC_SIZE_MISMATCH");
        verify(stockage, never()).ouvrirEcriture(anyString(), anyString());
    }

    @Test
    @DisplayName("Contenu lu plus court ou plus long que la taille annoncée : REJETE DOC_SIZE_MISMATCH, écriture WORM abandonnée")
    void contenuLuDifferent() {
        UUID court = deposer(FICHIER);
        doReturn(Optional.of(new Flux(new ByteArrayInputStream(Arrays.copyOf(FICHIER, 20)), FICHIER.length)))
            .when(stockage)
            .lireDepot(court.toString());
        verification.verifier(court);
        assertThat(depot(court).getMotif()).isEqualTo("DOC_SIZE_MISMATCH");

        UUID long_ = deposer("autre", FICHIER, "text/plain");
        byte[] plus = Arrays.copyOf(FICHIER, FICHIER.length + 10);
        doReturn(Optional.of(new Flux(new ByteArrayInputStream(plus), FICHIER.length))).when(stockage).lireDepot(long_.toString());
        verification.verifier(long_);
        assertThat(depot(long_).getEtat()).isEqualTo(EtatDepotFragmente.REJETE);
        assertThat(depot(long_).getMotif()).isEqualTo("DOC_SIZE_MISMATCH");

        assertThat(stockage.objetsWorm()).isZero();
        assertThat(stockage.envoisEnCours()).isZero();
        assertThat(base.documents).isEmpty();
    }

    @Test
    @DisplayName("Objet assemblé introuvable : REJETE DOC_CONTENT_MISSING")
    void objetAssembleIntrouvable() {
        UUID id = deposer(FICHIER);
        stockage.supprimerDepot(id.toString());
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.REJETE);
        assertThat(depot(id).getMotif()).isEqualTo("DOC_CONTENT_MISSING");
    }

    // ------------------------------------------------------------------ pannes, reprises, instances

    @Test
    @DisplayName("Dépôt objet indisponible : reste EN_VERIFICATION, repris après le délai de nouvelle tentative")
    void depotObjetIndisponible() {
        UUID id = deposer(FICHIER);
        stockage.simulerPanne(true);
        verification.verifierEnAttente();

        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(depot(id).getRepriseLe()).isEqualTo(DEBUT.plus(Duration.ofMinutes(1)));
        stockage.simulerPanne(false);
        assertThat(verification.verifierEnAttente()).isZero();
        horloge.avancer(Duration.ofMinutes(1));
        assertThat(verification.verifierEnAttente()).isEqualTo(1);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
    }

    @Test
    @DisplayName("Panne pendant l'écriture WORM : reste EN_VERIFICATION ; à la reprise, l'écriture laissée en cours est abandonnée")
    void panneEnCoursDEcriture() {
        UUID id = deposer(FICHIER);
        AtomicBoolean premiere = new AtomicBoolean(true);
        ecrituresWorm(e -> {
            if (premiere.getAndSet(false)) {
                stockage.simulerPanne(true);
            }
            return e;
        });
        verification.verifier(id);

        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(depot(id).getEnvoiWorm()).isNotNull();
        assertThat(depot(id).getRepriseLe()).isEqualTo(DEBUT.plus(Duration.ofMinutes(1)));
        assertThat(stockage.envoisEnCours()).isEqualTo(1);

        stockage.simulerPanne(false);
        horloge.avancer(Duration.ofMinutes(1));
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        assertThat(stockage.envoisEnCours()).isZero();
        assertThat(stockage.objetsWorm()).isEqualTo(1);
    }

    @Test
    @DisplayName("Arrêt pendant la vérification : dépôt réservé pendant le bail, puis repris ; document identique au ticket attribué")
    void repriseApresArret() {
        UUID id = deposer(FICHIER);
        AtomicBoolean arret = new AtomicBoolean(true);
        ecrituresWorm(e ->
            new EcritureDeleguee(e) {
                @Override
                public void ecrire(byte[] tranche, int longueur) {
                    if (arret.getAndSet(false)) {
                        throw new IllegalStateException("arrêt du service");
                    }
                    super.ecrire(tranche, longueur);
                }
            }
        );
        assertThat(verification.verifierEnAttente()).isZero();
        UUID ticket = depot(id).getDocumentId();
        assertThat(ticket).isNotNull();
        assertThat(depot(id).getRepriseLe()).isEqualTo(DEBUT.plus(Duration.ofHours(1)));

        verification.verifier(id);
        assertThat(verification.verifierEnAttente()).isZero();
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);

        horloge.avancer(Duration.ofHours(1));
        assertThat(verification.verifierEnAttente()).isEqualTo(1);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        assertThat(depot(id).getDocumentId()).isEqualTo(ticket);
        assertThat(base.documents.get(0).getId()).isEqualTo(ticket);
        assertThat(stockage.envoisEnCours()).isZero();
    }

    @Test
    @DisplayName("Dépôt pris par une autre instance : écriture abandonnée, rien n'est inscrit")
    void autreInstance() {
        UUID id = deposer(FICHIER);
        ecrituresWorm(e -> {
            base.modifierAilleurs(id);
            return e;
        });
        verification.verifier(id);

        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(depot(id).getEnvoiWorm()).isNull();
        assertThat(base.documents).isEmpty();
        assertThat(stockage.envoisEnCours()).isZero();
    }

    @Test
    @DisplayName("Dépôt modifié par une autre instance pendant la lecture : objet WORM orphelin journalisé, rien n'est inscrit")
    void conflitALInscription() {
        UUID id = deposer(FICHIER);
        ecrituresWorm(e ->
            new EcritureDeleguee(e) {
                @Override
                public ObjetDepose terminer() {
                    ObjetDepose o = super.terminer();
                    base.modifierAilleurs(id);
                    return o;
                }
            }
        );
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(base.documents).isEmpty();
        assertThat(stockage.objetsWorm()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fin de l'écriture WORM impossible : reste EN_VERIFICATION, écriture abandonnée, nouvelle tentative plus tard")
    void finDEcritureImpossible() {
        UUID id = deposer(FICHIER);
        AtomicBoolean premiere = new AtomicBoolean(true);
        ecrituresWorm(e ->
            new EcritureDeleguee(e) {
                @Override
                public ObjetDepose terminer() {
                    if (premiere.getAndSet(false)) {
                        throw DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "panne");
                    }
                    return super.terminer();
                }
            }
        );
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(stockage.envoisEnCours()).isZero();

        horloge.avancer(Duration.ofMinutes(1));
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
    }

    @Test
    @DisplayName("Abandon et suppression impossibles après la vérification : seulement journalisés (cycle de vie du compartiment)")
    void nettoyageImpossible() {
        UUID id = deposer(FICHIER);
        doThrow(DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "panne")).when(stockage).supprimerDepot(anyString());
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);

        UUID rejete = deposer("autre", "Carte 4111111111111111".getBytes(StandardCharsets.US_ASCII), "text/plain");
        ecrituresWorm(e ->
            new EcritureDeleguee(e) {
                @Override
                public void abandonner() {
                    throw DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "panne");
                }
            }
        );
        verification.verifier(rejete);
        assertThat(depot(rejete).getEtat()).isEqualTo(EtatDepotFragmente.REJETE);
    }

    @Test
    @DisplayName("Flux illisible pendant la lecture : reste EN_VERIFICATION, nouvelle tentative plus tard")
    void fluxIllisible() {
        UUID id = deposer(FICHIER);
        InputStream coupe = new InputStream() {
            int lus;

            @Override
            public int read() throws IOException {
                if (lus++ > 10) {
                    throw new IOException("connexion coupée");
                }
                return 'a';
            }
        };
        doReturn(Optional.of(new Flux(coupe, FICHIER.length))).when(stockage).lireDepot(id.toString());
        verification.verifier(id);
        assertThat(depot(id).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
        assertThat(depot(id).getRepriseLe()).isEqualTo(DEBUT.plus(Duration.ofMinutes(1)));
        assertThat(stockage.envoisEnCours()).isZero();
    }

    @Test
    @DisplayName("Dépôt inconnu, encore ouvert ou déjà vérifié : sans effet")
    void sansEffet() {
        verification.verifier(UUID.randomUUID());
        Etat ouvert = depots.ouvrir(new Ouverture("a.txt", "text/plain", 8L, "MESSAGERIE", "r"), new Demandeur(AGENT, null, Set.of("COL_MESSAGE_SEND")));
        verification.verifier(ouvert.id());
        assertThat(depot(ouvert.id()).getEtat()).isEqualTo(EtatDepotFragmente.OUVERT);
        verify(stockage, never()).lireDepot(anyString());
    }

    @Test
    @DisplayName("Erreur inattendue sur un dépôt : journalisée, les suivants sont vérifiés")
    void erreurInattendue() {
        UUID premier = deposer(FICHIER);
        horloge.avancer(Duration.ofSeconds(1));
        UUID second = deposer("autre", "Ordre du jour".getBytes(StandardCharsets.US_ASCII), "text/plain");
        doThrow(new IllegalStateException("inattendue")).when(stockage).lireDepot(premier.toString());
        assertThat(verification.verifierEnAttente()).isEqualTo(1);
        assertThat(depot(second).getEtat()).isEqualTo(EtatDepotFragmente.DISPONIBLE);
        assertThat(depot(premier).getEtat()).isEqualTo(EtatDepotFragmente.EN_VERIFICATION);
    }

    @Test
    @DisplayName("Constructeur Spring : horloge système")
    void constructeur() {
        assertThat(
            new VerificationDepotService(stockage, extracteur, base.depotRepository, base.documentRepository, base.transactions, PROPRIETES)
        ).isNotNull();
    }
}
