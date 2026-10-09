import { afterEach, describe, expect, it } from 'vitest'
import { servicesSimules } from '.'
import { notificationsDemo } from '../notifications'

// D-98 (docs/lot1/lancement-processus.md §8.8) : la proposition d'une transition de statut (MS-PARTICIPANT simulé) devient
// une tâche à prendre par les porteurs de PAR_TRANSITION_APPROVE (MS-PILOTAGE simulé), notifiée avec le lien de l'onglet
// des transitions, close quand la transition est prononcée ou refusée.

const chef = { id: 'U-020', nom: 'Bertrand Nguema Obiang', profils: ['ROLE_CHEF_DDRM'] }
const charge = { id: 'sim-charge', nom: 'Fatimé Abakar Hissein', profils: ['ROLE_CHARGE_DDRM'] }

afterEach(() => ['gm.demoContinue', 'gm.demo.parcours', 'gm.demo.participant', 'gm.demo.pilotage'].forEach((k) => localStorage.removeItem(k)))

async function banqueDeLibreville(s: ReturnType<typeof servicesSimules>) {
  const p = await s.participant.listerInstitutions({ recherche: 'Libreville', page: 1, taille: 10 })
  return p.contenu[0].id
}

describe('D-98 : tâche de l’approbateur d’une transition', () => {
  it('proposition : tâche à prendre de l’approbateur, notification vers les transitions, close au prononcé', async () => {
    // Le chargé propose (DDR-01 : le proposant ne prononce pas), le chef reçoit la tâche sur le même registre.
    localStorage.setItem('gm.demoContinue', '1')
    const parCharge = servicesSimules({ latenceMs: 0, utilisateur: charge })
    const id = await banqueDeLibreville(parCharge)
    const t = await parCharge.participant.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    const s = servicesSimules({ latenceMs: 0, utilisateur: chef })
    const n = notificationsDemo(s, { moi: chef.id })

    const aPrendre = (await s.pilotage.taches({ vue: 'unite' })).contenu.find((x) => x.origine.service === 'participant')!
    expect(aPrendre).toMatchObject({
      titre: 'Prononcer l’adhésion : Banque de Libreville', etat: 'A_PRENDRE', aPrendre: true, permissionsRequises: ['PAR_TRANSITION_APPROVE'],
      origine: { type: 'DECISION', reference: `${id}/TRANSITION/${t.id}`, service: 'participant', lien: `/des/institutions/${id}?onglet=transitions` },
    })
    expect(aPrendre.createur.nomAffiche).toBe('Fatimé Abakar Hissein')
    const notif = (await n.lister({ etat: 'toutes' })).contenu.find((x) => x.titre.startsWith('Prononcer'))!
    expect(notif).toMatchObject({ type: 'TACHE', lien: `/des/institutions/${id}?onglet=transitions`, sousTitre: 'À prendre' })

    // Prise (facultative) puis prononcé : la tâche est terminée par le service.
    await s.pilotage.prendre(aPrendre.id)
    await s.participant.approuverTransition(id, t.id, 'assertion')
    expect((await s.pilotage.tache(aPrendre.id)).etat).toBe('TERMINEE')
  })

  it('démonstration continue : le chargé propose, le chef retrouve sa tâche et sa notification ; refus : tâche close ; le chargé n’en a pas', async () => {
    localStorage.setItem('gm.demoContinue', '1')
    const parCharge = servicesSimules({ latenceMs: 0, utilisateur: charge })
    const id = await banqueDeLibreville(parCharge)
    const t = await parCharge.participant.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    expect((await parCharge.pilotage.taches({ vue: 'unite' })).contenu.some((x) => x.origine.service === 'participant')).toBe(false)
    expect(parCharge.decisions.aPrononcer()).toEqual([])

    const parChef = servicesSimules({ latenceMs: 0, utilisateur: chef })
    expect(parChef.decisions.aPrononcer()).toHaveLength(1)
    const n = notificationsDemo(parChef, { moi: chef.id })
    expect((await n.lister({ etat: 'toutes' })).contenu.some((x) => x.lien === `/des/institutions/${id}?onglet=transitions`)).toBe(true)
    const tache = (await parChef.pilotage.taches({ vue: 'unite' })).contenu.find((x) => x.origine.reference === `${id}/TRANSITION/${t.id}`)!
    expect(tache.etat).toBe('A_PRENDRE')
    await parChef.participant.rejeterTransition(id, t.id, 'Pièces à compléter')
    expect((await parChef.pilotage.taches({ vue: 'unite' })).contenu.some((x) => x.id === tache.id)).toBe(false)
  })
})
