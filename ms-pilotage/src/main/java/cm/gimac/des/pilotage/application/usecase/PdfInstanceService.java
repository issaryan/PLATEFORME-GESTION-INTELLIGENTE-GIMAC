package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.InstanceLue;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort.Document;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort.Fait;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.EtatSignature;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.SignatureInstance;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF d'une instance, au gabarit commun des PDF de la plateforme (D-139) : cartouche (référence, état, demandeur, date
 * de la demande), données figées au lancement, champs de chaque étape avec son état, signatures (signataire, date,
 * empreinte), sans assertion FIDO2. Libellés du document en fr, en ou es ; dates à l'heure de Douala.
 */
@Service
public class PdfInstanceService {

    public record PdfInstance(byte[] contenu, String nomFichier) {}

    private final InstancesProcessusUseCase instances;
    private final RenduPdfPort rendu;
    private final Clock horloge;

    public PdfInstanceService(InstancesProcessusUseCase instances, RenduPdfPort rendu, Clock horloge) {
        this.instances = instances;
        this.rendu = rendu;
        this.horloge = horloge;
    }

    /** PDF de l'instance ; langue fr, en ou es (fr par défaut). */
    @Transactional(readOnly = true)
    public PdfInstance telecharger(Appelant appelant, UUID id, String langue) {
        InstanceLue lue = instances.instance(appelant, id);
        InstanceProcessus i = lue.instance();
        String l = TextesPdfInstance.langue(langue);
        Map<String, String> t = TextesPdfInstance.de(l);
        String reference = Optional.ofNullable(i.getReference()).orElseGet(() -> i.getId().toString());
        List<Fait> faits = List.of(
            new Fait(t.get("reference"), reference),
            new Fait(t.get("etat"), libelle(t, i.getEtat())),
            new Fait(t.get("demandeur"), nom(i.getLanceurId(), lue.noms())),
            new Fait(t.get("date"), date(t, i.getLanceeLe()))
        );
        StringBuilder h = new StringBuilder(4_000);
        h.append("<h2>").append(e(t.get("donnees"))).append("</h2>");
        List<ChampFormulaire> champs = Optional.ofNullable(lue.processus()).map(p -> p.getFormulaire().champs()).orElse(List.of());
        if (champs.isEmpty()) {
            h.append("<p class=\"muet\">").append(e(t.get("aucune"))).append("</p>");
        } else {
            h.append("<table class=\"fiche\"><tbody>");
            champs.forEach(c -> ligne(h, t, c.libelle(), i.getFormulaire().get(c.code()), c.type() == TypeChampFormulaire.LISTE));
            h.append("</tbody></table>");
        }
        for (EtapeInstance etape : lue.etapes()) {
            etape(h, t, etape, Optional.ofNullable(lue.processus()).flatMap(p -> p.etape(etape.getCode())).orElse(null), lue.noms());
        }
        Document d = new Document(h.toString(), i.getLibelle(), i.getIntitule(), reference, "INTERNE", l, Instant.now(horloge), faits);
        return new PdfInstance(rendu.rendre(d), reference + ".pdf");
    }

    private static void etape(StringBuilder h, Map<String, String> t, EtapeInstance etape, Etape def, Map<String, String> noms) {
        h.append("<h2>").append(e(def == null ? etape.getCode() : def.libelle()));
        if (etape.getEtat() != null) {
            String badge = switch (etape.getEtat()) {
                case TERMINEE -> "ok";
                case ANNULEE -> "ko";
                case EN_ATTENTE -> "attente";
                default -> "";
            };
            h.append(" <span class=\"badge ").append(badge).append("\">").append(e(libelle(t, etape.getEtat()))).append("</span>");
        }
        h.append("</h2>");
        if (def != null && !def.champs().isEmpty()) {
            h.append("<table class=\"fiche\"><tbody>");
            for (Champ c : def.champs()) {
                ligne(h, t, c.libelle(), etape.getValeurs().get(c.code()), c.type() == TypeChamp.CHOIX);
            }
            h.append("</tbody></table>");
        }
        if (etape.getSignatures().isEmpty()) {
            return;
        }
        h.append("<table><thead><tr><th style=\"width:38%\">").append(e(t.get("signature"))).append("</th><th style=\"width:14%\">").append(e(t.get("etat")));
        h.append("</th><th>").append(e(t.get("signataire"))).append("</th><th style=\"width:18%\">").append(e(t.get("signeeLe"))).append("</th></tr></thead><tbody>");
        for (SignatureInstance s : etape.getSignatures()) {
            // Une signature attendue n'affiche aucun nom.
            boolean signee = s.etat() == EtatSignature.SIGNEE && s.signePar() != null;
            h.append("<tr><td>").append(e(s.libelle()));
            if (signee && s.empreinte() != null) {
                h.append("<div class=\"empreinte\">").append(e(t.get("empreinte"))).append(" : ").append(e(s.empreinte())).append("</div>");
            }
            h.append("</td><td><span class=\"badge ").append(signee ? "ok" : "attente").append("\">").append(e(libelle(t, s.etat()))).append("</span></td>");
            h.append("<td>").append(signee ? e(nom(s.signePar(), noms)) : "<span class=\"muet\">—</span>").append("</td>");
            h.append("<td>").append(signee ? e(date(t, s.signeLe())) : "").append("</td></tr>");
        }
        h.append("</tbody></table>");
    }

    private static String libelle(Map<String, String> t, Enum<?> etat) {
        return etat == null ? "—" : t.getOrDefault(etat.name(), etat.name());
    }

    static String date(Map<String, String> t, Object valeur) {
        return ValeursPdfInstance.date(t, valeur);
    }

    private static String nom(String id, Map<String, String> noms) {
        return noms.getOrDefault(id, id);
    }

    private static void ligne(StringBuilder h, Map<String, String> t, String libelle, Object valeur, boolean choix) {
        h.append("<tr><th>").append(e(libelle)).append("</th><td class=\"valeur\">");
        String v = ValeursPdfInstance.lisible(t, t.get("langue"), valeur, choix);
        h.append(v.isBlank() ? "<span class=\"muet\">—</span>" : e(v)).append("</td></tr>");
    }

    private static String e(Object valeur) {
        if (valeur == null) {
            return "—";
        }
        return valeur.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
