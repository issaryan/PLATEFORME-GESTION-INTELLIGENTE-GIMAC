import { describe, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { referenceConversation } from '@gimac/socle'
import { servicesSimules } from './simule'
import { lecteurApercu, notificationsDemo, type DemoNotifications } from './notifications'

// Notifications de la plateforme (D-80) côté lot 1 : aperçu lu auprès de la messagerie, MS-NOTIFICATION simulé
// branché sur la messagerie simulée (réception d'un message d'un autre membre).

const moi = { id: 'U-001', nom: 'Paul Mbarga Atangana' }

describe('notifications de démonstration', () => {
  it('non-lus de démonstration regroupés par conversation, tâche et rapport ; aperçu du dernier message', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi })
    const n = notificationsDemo(s)
    const page = await n.lister({ etat: 'toutes' })
    expect(page.contenu.map((x) => x.type).sort()).toEqual(['MESSAGE', 'MESSAGE', 'RAPPORT', 'TACHE'])
    const groupe = page.contenu.find((x) => x.sousTitre === 'Essais de LIA-2026-030')!
    expect(groupe).toMatchObject({ titre: 'Rachel Biyong Mengue', nombre: 2, lien: expect.stringMatching(/^\/des\/messagerie\?conversation=CONV-/) })
    expect(await n.compteur()).toEqual({ nonLues: 3 })
    const apercu = lecteurApercu(s)
    expect(await apercu(groupe)).toBe('Le blocage est déclaré sur la liaison ; échéance de levée au 27 septembre.')
    expect(await apercu(page.contenu.find((x) => x.type === 'TACHE')!)).toBeNull()
    // Jamais pour l'acteur lui-même : la tâche de démonstration est confiée par un autre.
    expect(page.contenu.find((x) => x.type === 'TACHE')!.titre).toBe('Tâche confiée par Paul Mbarga Atangana')
    expect((await notificationsDemo(s, { moi: 'U-001' }).lister({})).contenu.find((x) => x.type === 'TACHE')!.acteur!.id).toBe('U-020')
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
