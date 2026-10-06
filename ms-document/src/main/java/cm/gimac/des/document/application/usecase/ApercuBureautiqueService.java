package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.in.ApercuBureautiqueUseCase;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.domain.service.SignatureBureautique;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md) : contrôles de taille, d'extension
 * et de signature, cache des PDF convertis par empreinte SHA-256 de la source (et extension, qui décide du
 * filtre d'import du moteur), limite de débit par compte, puis conversion par le port {@link ConvertisseurApercu}.
 *
 * <p>Seules les conversions effectives comptent dans la limite (30 par minute et par compte, fenêtre
 * glissante) : un aperçu servi par le cache ne sollicite pas le moteur. Le journal porte le compte, la
 * taille, l'extension, la durée et le résultat ; jamais le nom du fichier ni son contenu.
 */
@Service
public class ApercuBureautiqueService implements ApercuBureautiqueUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ApercuBureautiqueService.class);
    private static final Duration FENETRE = Duration.ofMinutes(1);

    private final ConvertisseurApercu convertisseur;
    private final long tailleMax;
    private final int limiteParMinute;
    private final Clock horloge;
    private final Cache<String, byte[]> cache;
    /** Instants (ms) des conversions de la dernière minute, par compte. */
    private final Cache<String, Deque<Long>> conversionsRecentes;

    @Autowired
    public ApercuBureautiqueService(ConvertisseurApercu convertisseur, ApercuProprietes proprietes) {
        this(convertisseur, proprietes, Clock.systemUTC());
    }

    ApercuBureautiqueService(ConvertisseurApercu convertisseur, ApercuProprietes proprietes, Clock horloge) {
        this.convertisseur = convertisseur;
        this.tailleMax = proprietes.tailleMax().toBytes();
        this.limiteParMinute = proprietes.limiteParMinute();
        this.horloge = horloge;
        Ticker ticker = () -> horloge.millis() * 1_000_000L;
        // Maintenance (expiration, éviction) sur le fil de l'appelant : bornes tenues dès l'écriture.
        this.cache = Caffeine.newBuilder()
            .executor(Runnable::run)
            .ticker(ticker)
            .expireAfterWrite(proprietes.cacheDuree())
            .maximumWeight(proprietes.cacheTailleMax().toBytes())
            .<String, byte[]>weigher((cle, pdf) -> pdf.length)
            .build();
        this.conversionsRecentes = Caffeine.newBuilder().ticker(ticker).expireAfterAccess(FENETRE).maximumSize(100_000).build();
    }

    @Override
    public long tailleMax() {
        return tailleMax;
    }

    @Override
    public byte[] apercu(Demande demande, String compte) {
        long debut = horloge.millis();
        byte[] contenu = demande.contenu() == null ? new byte[0] : demande.contenu();
        String extension = SignatureBureautique.extension(demande.nomFichier()).orElse("?");
        try {
            byte[] pdf = convertir(extension, contenu, compte);
            journaliser(compte, contenu.length, extension, debut, "SUCCES");
            return pdf;
        } catch (DocumentException e) {
            journaliser(compte, contenu.length, extension, debut, e.code());
            throw e;
        }
    }

    private byte[] convertir(String extension, byte[] contenu, String compte) {
        if (contenu.length > tailleMax) {
            throw DocumentException.tropVolumineux("DOC_APERCU_TROP_VOLUMINEUX", contenu.length, tailleMax);
        }
        if (!SignatureBureautique.FORMATS.containsKey(extension)) {
            throw DocumentException.nonPrisEnCharge("DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE", "Format non pris en charge par l'aperçu");
        }
        if (!SignatureBureautique.concorde(extension, contenu)) {
            throw DocumentException.nonPrisEnCharge(
                "DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE",
                "Le contenu du fichier ne correspond pas à son extension"
            );
        }
        String cle = EmpreinteSha256.de(contenu) + "." + extension;
        byte[] enCache = cache.getIfPresent(cle);
        if (enCache != null) {
            return enCache;
        }
        reserverConversion(compte);
        byte[] pdf;
        try {
            pdf = convertisseur.convertirEnPdf(extension, contenu);
        } catch (ConversionRefusee e) {
            throw DocumentException.refuse(
                "DOC_APERCU_ILLISIBLE",
                "Le fichier n'a pas pu être converti : il est peut-être corrompu ou protégé par un mot de passe",
                Map.of()
            );
        } catch (MoteurIndisponible e) {
            throw DocumentException.indisponible("DOC_APERCU_INDISPONIBLE", "Le service d'aperçu est momentanément indisponible");
        }
        cache.put(cle, pdf);
        return pdf;
    }

    /** Fenêtre glissante d'une minute : au-delà de la limite, 429 DOC_APERCU_LIMITE. */
    private void reserverConversion(String compte) {
        Deque<Long> instants = conversionsRecentes.get(compte, c -> new ArrayDeque<>());
        long maintenant = horloge.millis();
        synchronized (instants) {
            while (!instants.isEmpty() && instants.peekFirst() <= maintenant - FENETRE.toMillis()) {
                instants.pollFirst();
            }
            if (instants.size() >= limiteParMinute) {
                long attente = Math.max(1, (instants.peekFirst() + FENETRE.toMillis() - maintenant + 999) / 1000);
                throw DocumentException.limiteAtteinte(
                    "DOC_APERCU_LIMITE",
                    "Trop d'aperçus demandés : réessayez dans un instant",
                    Map.of("limiteParMinute", limiteParMinute, "reessayerDans", attente)
                );
            }
            instants.addLast(maintenant);
        }
    }

    private void journaliser(String compte, int taille, String extension, long debut, String resultat) {
        LOG.info(
            "Aperçu bureautique : compte={}, taille={}, extension={}, duree={} ms, resultat={}",
            compte,
            taille,
            extension,
            horloge.millis() - debut,
            resultat
        );
    }
}
