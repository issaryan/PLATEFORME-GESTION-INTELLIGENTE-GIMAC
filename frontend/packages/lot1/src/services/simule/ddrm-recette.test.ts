import { afterEach, describe, expect, it } from 'vitest'
import { aujourdhui } from '../../modele/jours'
import { servicesSimules } from './index'

// Recette du lot DDR (docs/recette/ddrm.md) : anomalies reproduites sur les services simulés, qui suivent les règles de
// MS-PARTICIPANT, MS-HOMOLOGATION et MS-PILOTAGE (RACI des dépôts, affectation, démonstration continue, dates).
const charge = { id: 'sim-charge', nom: 'Fatimé Abakar Hissein', profils: ['ROLE_CHARGE_DDRM'] }
const chef = { id: 'U-020', nom: 'Bertrand Nguema Obiang', profils: ['ROLE_CHEF_DDRM'] }
const chefDes = { id: 'U-001', nom: 'Paul Mbarga Atangana', profils: ['ROLE_CHEF_DES'] }
const sna = { id: 'u-sna', nom: 'Rachel Biyong Mengue', profils: ['ROLE_SNA'] }
const conducteur = { id: 'U-201', nom: 'Aïcha Mahamat Nour', profils: ['ROLE_CONDUCTEUR_PROJET'] }
type U = typeof charge
const svc = (u: U) => servicesSimules({ latenceMs: 0, utilisateur: u })
const refus = (code: string) => expect.objectContaining({ code })
const fichier = (nom: string, contenu = 'contenu') => new File([contenu], nom, { type: 'application/pdf' })

afterEach(() => Object.keys(localStorage).filter((k) => k.startsWith('gm.demo')).forEach((k) => localStorage.removeItem(k)))

/** Liaison de carte interbancaire ouverte au cadrage par le chef de la DDRM sur un adhérent (démonstration continue). */
async function liaison() {
  localStorage.setItem('gm.demoContinue', '1')
  return svc(chef).parcours.ouvrirLiaison({ institutionId: 'INST-0105', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-COND-1', banquePartenaireId: null } as never)
}
const op = async (u: U, liaisonId: string, rang: string) => (await svc(u).parcours.operations(liaisonId)).find((o) => o.rang === rang)!

describe('DDR-05 : dépôt de preuve réservé au RACI, auteur réel tracé', () => {
  it('le chef de la DDRM (non responsable) ne se voit pas offrir le dépôt ; la chargée responsable oui, et le dépôt porte son nom', async () => {
    const l = await liaison()
    const duChef = await op(chef, l.id, '3')
    expect(duChef.raci.R?.fonction).toBe('DDRM')
    expect(duChef.actionsPermises).not.toContain('DEPOSER_PREUVE')
    expect(duChef.livrablesAttendus?.every((x) => !x.peutDeposer)).toBe(true)
    expect((await op(charge, l.id, '3')).actionsPermises).toContain('DEPOSER_PREUVE')
    // Sans responsabilité ni supervision (HOM_STEP_MANAGE) : 403, comme MS-HOMOLOGATION.
    await expect(svc(sna).parcours.deposerPreuve(`${l.id}:3`, fichier('perimetre.pdf'), {})).rejects.toEqual(refus('HOM_DEPOT_NON_AUTORISE'))
    await svc(charge).parcours.deposerPreuve(`${l.id}:3`, fichier('perimetre.pdf'), {})
    expect((await op(charge, l.id, '3')).preuve?.deposePar).toBe('Fatimé Abakar Hissein')
  })

  it('DDR-26 : deux fichiers de preuve différents ont deux empreintes différentes', async () => {
    const l = await liaison()
    await svc(charge).parcours.deposerPreuve(`${l.id}:3`, fichier('perimetre-v1.pdf', 'a'), {})
    const v1 = (await op(charge, l.id, '3')).preuve!.empreinte
    await svc(conducteur).parcours.renvoyer(`${l.id}:3`, 'Signature manquante')
    await svc(charge).parcours.deposerPreuve(`${l.id}:3`, fichier('perimetre-v2.pdf', 'bb'), {})
    expect((await op(charge, l.id, '3')).preuve!.empreinte).not.toBe(v1)
  })
})

describe('DDR-06 : affectation des ressources contrôlée', () => {
  it('personne inconnue ou d’une autre fonction refusée ; la personne affectée tient sa fonction sur les opérations non faites', async () => {
    const l = await liaison()
    const s = svc(chef)
    // Le rang 6 s'ouvre une fois le cadrage clos : il est ouvert ici directement pour isoler la règle.
    const d = await s.parcours.dossier(l.id)
    expect(d.versionReferentiel).toBe(3)
    await expect(s.parcours.affecterRessources(l.id, [])).rejects.toEqual(expect.objectContaining({ code: expect.stringMatching(/HOM_STEP_NOT_OPEN|HOM_RESOURCES_REQUIRED/) }))
    for (const rang of ['3', '4', '5']) {
      await svc(charge).parcours.deposerPreuve(`${l.id}:${rang}`, fichier(`preuve-${rang}.pdf`), {})
      await svc(conducteur).parcours.validerOperation(`${l.id}:${rang}`, 'assertion')
    }
    await expect(svc(chef).parcours.affecterRessources(l.id, [])).rejects.toEqual(refus('HOM_RESOURCES_REQUIRED'))
    await expect(svc(chef).parcours.affecterRessources(l.id, [{ fonction: 'SNA', personneId: 'U-RSSI' }])).rejects.toEqual(refus('HOM_PERSON_NOT_ELIGIBLE'))
    await expect(svc(chef).parcours.affecterRessources(l.id, [{ fonction: 'SNA', personneId: 'U-INCONNU' }])).rejects.toEqual(refus('HOM_PERSON_UNKNOWN'))
    await svc(chef).parcours.affecterRessources(l.id, [{ fonction: 'SNA', personneId: 'U-SNA-2' }])
    const ops = await svc(chef).parcours.operations(l.id)
    expect(ops.filter((o) => o.etat !== 'FAITE' && o.raci.R?.fonction === 'SNA').every((o) => o.raci.R?.id === 'U-SNA-2')).toBe(true)
    expect(ops.find((o) => o.rang === '6')!.preuve?.deposePar).toBe('Bertrand Nguema Obiang')
  })
})

describe('DDR-09 : tâches gardées entre acteurs en démonstration continue', () => {
  it('la tâche confiée par le chef à la chargée figure dans ses tâches après le changement d’acteur', async () => {
    localStorage.setItem('gm.demoContinue', '1')
    const t = await svc(chef).pilotage.creerTache({ titre: 'Relancer la Banque Atlantique', description: null, echeance: null, priorite: 'NORMALE', responsableId: 'sim-charge' })
    const siennes = (await svc(charge).pilotage.taches({ vue: 'moi' })).contenu
    expect(siennes.find((x) => x.id === t.id)?.titre).toBe('Relancer la Banque Atlantique')
    // Aucun identifiant réattribué au chargement suivant.
    const autre = await svc(chef).pilotage.creerTache({ titre: 'Autre', description: null, echeance: null, priorite: 'NORMALE' })
    expect(autre.id).not.toBe(t.id)
  })
})

describe('DDR-10 : seules les actions de ses fonctions sont annoncées', () => {
  it('la signature du procès-verbal (CHEF_DES) n’est pas annoncée au chef de la DDRM, mais l’est au chef du DES', async () => {
    const pv = (u: U) => svc(u).parcours.mesOperations().then((m) => m.aAgir.filter((x) => x.liaisonId === 'LIA-2026-027' && x.responsable?.fonction === 'CHEF_DES'))
    expect(await pv(chef)).toEqual([])
    expect((await pv(chefDes)).length).toBeGreaterThan(0)
  })
})

describe('DDR-15 : dates de la démonstration sur l’horloge du jour', () => {
  it('liaison ouverte aujourd’hui ; étapes 1 et 2 closes aux dates de l’institution ; aucune échéance passée dite « proche »', async () => {
    const l = await liaison()
    const d = await svc(chef).parcours.dossier(l.id)
    expect(d.dateDemarrage).toBe(aujourdhui())
    const adhesion = d.etapes.find((e) => e.code === 'ADHESION')!
    expect(adhesion.closeLe).toBe('2026-02-10')
    const ops = await svc(chef).parcours.operations('LIA-2026-030')
    expect(ops.filter((o) => o.sla === 'ECHEANCE_PROCHE' && o.echeance! < aujourdhui())).toEqual([])
  })
})
