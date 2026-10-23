import { describe, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { referenceConversation } from '@gimac/socle'
import { servicesSimules } from './simule'
import { lecteurApercu, notificationsDemo, notificationsPortailDemo, type DemoNotifications } from './notifications'

// Notifications de la plateforme (D-80) côté lot 1 : aperçu lu auprès de la messagerie, MS-NOTIFICATION simulé
// branché sur la messagerie simulée (réception d'un message d'un autre membre).

const moi = { id: 'U-001', nom: 'Paul Mbarga Atangana' }

describe('notifications de démonstration', () => {
  it('non-lus de démonstration regroupés par conversation, tâche et rapport ; aperçu du dernier message', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi })
    const n = notificationsDemo(s)
    const page = await n.lister({ etat: 'toutes' })
    // QUO-05 : l'accord du supérieur de la demande de congé de démonstration (n+1 du demandeur) est notifié au chef du DES.
    expect(page.contenu.map((x) => x.type).sort()).toEqual(['HOMOLOGATION', 'MESSAGE', 'MESSAGE', 'PROCESSUS', 'RAPPORT', 'TACHE'])
    expect(page.contenu.find((x) => x.type === 'PROCESSUS')).toMatchObject({ titre: 'Étape à réaliser : Accord du supérieur hiérarchique', lien: expect.stringMatching(/\?etape=VALIDATION$/) })
    const groupe = page.contenu.find((x) => x.sousTitre === 'Essais de LIA-2026-030')!
    expect(groupe).toMatchObject({ titre: 'Rachel Biyong Mengue', nombre: 2, lien: expect.stringMatching(/^\/des\/messagerie\?conversation=CONV-/) })
    expect(await n.compteur()).toEqual({ nonLues: 4 })
    // Homologation (§11) : lien profond vers l'opération dans le dossier de la liaison.
    expect(page.contenu.find((x) => x.type === 'HOMOLOGATION')!.lien).toBe('/des/parcours/LIA-2026-036?onglet=operations&operation=LIA-2026-036%3A11')
    const apercu = lecteurApercu(s)
    expect(await apercu(groupe)).toBe('Le blocage est déclaré sur la liaison ; échéance de levée au 27 septembre.')
    expect(await apercu(page.contenu.find((x) => x.type === 'TACHE')!)).toBeNull()
    // Jamais pour l'acteur lui-même : la tâche de démonstration est confiée par un autre (son responsable), et mène à elle (DDR-20).
    const tache = page.contenu.find((x) => x.type === 'TACHE')!
    expect(tache.titre).toBe('Tâche confiée par Jean-Baptiste Ella Mintsa')
    expect(tache.acteur!.id).toBe('u-dg')
    const id = tache.lien.match(/^\/des\/taches\?tache=(.+)$/)![1]
    expect((await s.pilotage.taches({ vue: 'moi' })).contenu.map((x) => x.id)).toContain(decodeURIComponent(id))
  })

  it('DDR-20 : notifications de démonstration propres au profil (chargée de la DDRM : ni rapport reçu, ni action d’une autre fonction)', async () => {
    const charge = { id: 'sim-charge', nom: 'Fatimé Abakar Hissein', profils: ['ROLE_CHARGE_DDRM'] }
    const s = servicesSimules({ latenceMs: 0, utilisateur: charge })
    const page = await notificationsDemo(s, { moi: charge.id, profils: charge.profils }).lister({ etat: 'toutes' })
    expect(page.contenu.some((x) => x.type === 'RAPPORT')).toBe(false)
    expect(page.contenu.some((x) => x.titre === 'Action attendue : Atelier comptabilisation')).toBe(false)
    const tache = page.contenu.find((x) => x.type === 'TACHE')!
    expect(tache.titre).toBe('Tâche confiée par Bertrand Nguema Obiang')
    const id = decodeURIComponent(tache.lien.split('tache=')[1])
    expect((await s.pilotage.taches({ vue: 'moi' })).contenu.find((x) => x.id === id)?.createur.id).toBe('U-020')
  })

  it('DDR-08 : la chargée est avisée du refus et du prononcé de sa proposition, des opérations ouvertes et renvoyées', async () => {
    localStorage.setItem('gm.demoContinue', '1')
    try {
      const charge = { id: 'sim-charge', nom: 'Fatimé Abakar Hissein', profils: ['ROLE_CHARGE_DDRM'] }
      const chef = { id: 'U-020', nom: 'Bertrand Nguema Obiang', profils: ['ROLE_CHEF_DDRM'] }
      const conducteur = { id: 'U-201', nom: 'Aïcha Mahamat Nour', profils: ['ROLE_CONDUCTEUR_PROJET'] }
      const parCharge = servicesSimules({ latenceMs: 0, utilisateur: charge })
      const id = (await parCharge.participant.listerInstitutions({ recherche: 'Libreville', page: 1, taille: 5 })).contenu[0].id
      const t1 = await parCharge.participant.proposerTransition(id, 'ADHERENT', 'Dossier complet')
      const parChef = servicesSimules({ latenceMs: 0, utilisateur: chef })
      await parChef.participant.rejeterTransition(id, t1.id, 'Statuts à jour exigés')
      const t2 = await servicesSimules({ latenceMs: 0, utilisateur: charge }).participant.proposerTransition(id, 'ADHERENT', 'Statuts mis à jour')
      const chef2 = servicesSimules({ latenceMs: 0, utilisateur: chef })
      await chef2.participant.approuverTransition(id, t2.id, 'a')
      const l = await chef2.parcours.ouvrirLiaison({ institutionId: id, typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-COND-1', banquePartenaireId: null } as never)
      // La chargée dépose la preuve du rang 3, le conducteur la renvoie avec motif.
      const s3 = servicesSimules({ latenceMs: 0, utilisateur: charge })
      await s3.parcours.deposerPreuve(`${l.id}:3`, new File(['a'], 'perimetre.pdf'), {})
      await servicesSimules({ latenceMs: 0, utilisateur: conducteur }).parcours.renvoyer(`${l.id}:3`, 'Signature manquante')

      const s = servicesSimules({ latenceMs: 0, utilisateur: charge })
      const titres = (await notificationsDemo(s, { moi: charge.id, profils: charge.profils }).lister({ etat: 'toutes' })).contenu.map((x) => `${x.type} ${x.titre} | ${x.sousTitre ?? ''}`)
      expect(titres).toEqual(expect.arrayContaining([
        'DEMANDE Proposition de statut refusée : Banque de Libreville | Motif : Statuts à jour exigés',
        'DEMANDE Adhésion prononcée : Banque de Libreville | ',
        expect.stringMatching(new RegExp(`^HOMOLOGATION Opération renvoyée : .* \\| ${l.id} \\(Banque de Libreville\\)\\. Motif : Signature manquante$`)),
        expect.stringMatching(/^HOMOLOGATION Action attendue : /),
      ]))
      // Jamais l'approbateur pour sa propre décision.
      const duChef = (await notificationsDemo(servicesSimules({ latenceMs: 0, utilisateur: chef }), { moi: chef.id, profils: chef.profils }).lister({ etat: 'toutes' })).contenu
      expect(duChef.some((x) => x.type === 'DEMANDE')).toBe(false)
    } finally {
      Object.keys(localStorage).filter((k) => k.startsWith('gm.demo')).forEach((k) => localStorage.removeItem(k))
    }
  })

  it('un message reçu crée puis regroupe la notification de la conversation et rafraîchit la messagerie', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi })
    const qc = new QueryClient()
    const invalider = vi.spyOn(qc, 'invalidateQueries')
    const n = notificationsDemo(s, { qc, moi: moi.id })
    await n.marquerLues()
    const demo = (window as unknown as { gmDemoNotifications: DemoNotifications }).gmDemoNotifications
    const id = demo.recevoirMessage({ contenu: 'Premier' })
    demo.recevoirMessage({ conversationId: id, contenu: 'Second' })
    const [notif] = (await n.lister({ etat: 'non-lues' })).contenu
    expect(notif).toMatchObject({ type: 'MESSAGE', titre: 'Aïcha Mahamat Nour', sousTitre: null, nombre: 2, reference: referenceConversation(id) })
    expect(invalider).toHaveBeenCalledWith({ queryKey: ['collaboration'] })
    expect(await lecteurApercu(s)(notif)).toBe('Second')
    expect((await s.messagerie.nonLus()).total).toBeGreaterThanOrEqual(2)
  })
})

describe('notifications de démonstration du portail', () => {
  it('CER-12 : une action attendue par tâche de l’institution (dont le procès-verbal à signer), lien du geste au portail ; jalon lu', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const n = notificationsPortailDemo(s, { intervalleMs: 0 })
    const taches = (await s.portailParcours.mesTaches()).taches
    await new Promise((r) => setTimeout(r, 50))
    const page = await n.lister({ etat: 'toutes', taille: 50 })
    const actions = page.contenu.filter((x) => x.reference.type === 'OPERATION')
    expect(actions.map((x) => x.lien).sort()).toEqual(taches.map((t) => t.lien).sort())
    expect(actions.every((x) => x.lien.startsWith('/portail/liaisons/') && x.titre.startsWith('Action attendue : '))).toBe(true)
    // Le procès-verbal établi de LIA-2026-027 attend la signature de l'institution : notification vers la page du PV.
    expect(actions.map((x) => x.lien)).toContain('/portail/liaisons/LIA-2026-027/proces-verbal')
    expect(page.contenu.find((x) => x.reference.type === 'LIAISON')).toMatchObject({ lien: '/portail/liaisons/LIA-2025-088', lueLe: expect.any(String) })
    expect(await n.compteur()).toEqual({ nonLues: actions.length })
  })
  it('CER-13 et US-EXI-09 : au portail, seules les préférences du parcours de ses liaisons et de la sandbox ; un type interne est refusé', async () => {
    const n = notificationsPortailDemo()
    expect(Object.keys((await n.preferences()).types)).toEqual(['HOMOLOGATION', 'SANDBOX'])
    await expect(n.enregistrerPreferences({ types: { MESSAGE: false } })).rejects.toEqual(expect.objectContaining({ code: 'NOT_REQUETE_INVALIDE' }))
    expect((await n.enregistrerPreferences({ types: { HOMOLOGATION: false } })).types).toEqual({ HOMOLOGATION: false, SANDBOX: true })
  })
})
