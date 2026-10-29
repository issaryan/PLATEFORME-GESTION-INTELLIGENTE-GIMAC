package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.InstanceLue;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.SignatureInstance;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PDF d'une instance : données figées au lancement, champs des étapes et signatures, sans assertion FIDO2. */
@Service
public class PdfInstanceService {

    public record PdfInstance(byte[] contenu, String nomFichier) {}

    private final InstancesProcessusUseCase instances;
    private final RenduPdfPort rendu;

    public PdfInstanceService(InstancesProcessusUseCase instances, RenduPdfPort rendu) {
        this.instances = instances;
        this.rendu = rendu;
    }

    @Transactional(readOnly = true)
    public PdfInstance telecharger(Appelant appelant, UUID id) {
        InstanceLue lue = instances.instance(appelant, id);
        InstanceProcessus i = lue.instance();
        String reference = java.util.Optional.ofNullable(i.getReference()).orElseGet(() -> i.getId().toString());
        StringBuilder h = new StringBuilder("<html><head><meta charset=\"UTF-8\"/><style>")
            .append("@page{size:A4;margin:16mm}body{font-family:sans-serif;font-size:10pt;color:#17231f}h1{font-size:19pt;color:#12352b}h2{margin-top:20px;padding:7px;background:#12352b;color:white;font-size:12pt}table{width:100%;border-collapse:collapse;margin-bottom:12px}th,td{border:1px solid #9aa9a2;padding:6px;text-align:left;vertical-align:top}th{width:34%;background:#edf3f0}.valeur{white-space:pre-wrap}.empreinte{font-family:monospace;font-size:7pt;word-break:break-all}</style></head><body>")
            .append("<h1>Moabi — ").append(e(i.getLibelle())).append("</h1><table>");
        ligne(h, "Référence", reference);
        ligne(h, "Objet", i.getIntitule());
        ligne(h, "Demandeur", nom(i.getLanceurId(), lue.noms()));
        ligne(h, "Date de la demande", i.getLanceeLe());
        ligne(h, "État", i.getEtat());
        h.append("</table><h2>Données du demandeur</h2><table>");
        java.util.Optional.ofNullable(lue.processus()).ifPresent(p -> {
            for (ChampFormulaire c : p.getFormulaire().champs()) {
                ligne(h, c.libelle(), i.getFormulaire().get(c.code()));
            }
        });
        h.append("</table>");
        for (EtapeInstance etape : lue.etapes()) {
            Etape def = java.util.Optional.ofNullable(lue.processus()).flatMap(p -> p.etape(etape.getCode())).orElse(null);
            h.append("<h2>").append(e(java.util.Optional.ofNullable(def).map(Etape::libelle).orElse(etape.getCode()))).append("</h2>");
            if (def != null && !def.champs().isEmpty()) {
                h.append("<table>");
                for (Champ c : def.champs()) {
                    ligne(h, c.libelle(), etape.getValeurs().get(c.code()));
                }
                h.append("</table>");
            }
            for (SignatureInstance s : etape.getSignatures()) {
                h.append("<table><tr><th>Signature — ").append(e(s.libelle())).append("</th><td>")
                    .append("État : ").append(e(s.etat()));
                java.util.Optional.ofNullable(s.signePar()).ifPresent(signataire -> h.append("<br/>Signataire : ").append(e(nom(signataire, lue.noms()))).append("<br/>Date : ").append(e(s.signeLe()))
                    .append("<br/>Empreinte : <span class=\"empreinte\">").append(e(s.empreinte())).append("</span>"));
                h.append("</td></tr></table>");
            }
        }
        h.append("</body></html>");
        return new PdfInstance(rendu.rendre(h.toString(), "Moabi · " + reference + " · Document interne"), reference + ".pdf");
    }

    private static String nom(String id, Map<String, String> noms) {
        return noms.getOrDefault(id, id);
    }

    private static void ligne(StringBuilder h, String libelle, Object valeur) {
        h.append("<tr><th>").append(e(libelle)).append("</th><td class=\"valeur\">").append(e(valeur)).append("</td></tr>");
    }

    private static String e(Object valeur) {
        if (valeur == null) {
            return "—";
        }
        return valeur.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
