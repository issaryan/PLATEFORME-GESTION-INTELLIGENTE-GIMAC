import { afterEach, describe, expect, it, vi } from 'vitest'
import { referenceConversation, type NotificationPlateforme } from '@gimac/socle'
import { extraitCitation } from '../modele/messagerie'
import { messagerieHttp } from './http/messagerie'
import { apercuHttp } from './http/apercu'
import { servicesHttp } from './http'
import { servicesSimules } from './simule'
import { arriveeMention, lecteurApercu, notificationsDemo, type DemoNotifications } from './notifications'

// Réponse à un message et mentions (D-82) : corps envoyés, règles du service simulé, notifications MENTION et
// REPONSE ; aperçu bureautique (D-83) : route, en-têtes et refus.

afterEach(() => vi.restoreAllMocks())
const moi = { id: 'U-001', nom: 'Paul Mbarga Atangana' }
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string }) => `${e.status} ${e.code}`)

describe('extrait d’un message cité', () => {
  it('espaces réduits, 120 caractères au plus, coupé à une espace proche de la fin', () => {
    expect(extraitCitation('  Bonjour\n\n  à tous ')).toBe('Bonjour à tous')
    const long = `${'mot '.repeat(40)}fin`
    const e = extraitCitation(long)
    expect(e.endsWith('…')).toBe(true)
    expect(Array.from(e).length).toBeLessThanOrEqual(120)
    expect(e).toBe(`${'mot '.repeat(29).trim()}…`)
    // Sans espace dans les 30 derniers caractères : coupé à 119.
    expect(extraitCitation('x'.repeat(200))).toBe(`${'x'.repeat(119)}…`)
  })
})

describe('messagerie HTTP : reponseA et mentions dans la partie JSON (D-82)', () => {
  it('envoi et création ; absents sans citation ni mention', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    await messagerieHttp.envoyer('C1', { contenu: '@Aïcha Mahamat Nour d’accord', etiquettes: [], fichiers: [], reponseA: 'M-7', mentions: ['U-201'] })
    await messagerieHttp.demarrer({ destinataires: ['U-201'], titre: null, liaisonId: null, contenu: 'Suite', etiquettes: [], fichiers: [], reponseA: 'M-8', mentions: [] })
    const corps = async (i: number) => JSON.parse(await (((globalThis.fetch as unknown as { mock: { calls: [URL, RequestInit][] } }).mock.calls[i][1].body as FormData).get('message') as Blob).text())
    expect(await corps(0)).toEqual({ contenu: '@Aïcha Mahamat Nour d’accord', etiquettes: [], reponseA: 'M-7', mentions: ['U-201'] })
    expect(await corps(1)).toEqual({ contenu: 'Suite', etiquettes: [], reponseA: 'M-8', destinataires: ['U-201'], titre: null, liaisonId: null })
  })
})

describe('messagerie simulée : réponses et mentions (D-82)', () => {
  it('jeu de démonstration : citation, mention de l’utilisateur, message cité supprimé', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi }).messagerie
    const [groupe, , directe] = (await s.conversations({})).contenu
    const fil = (await s.messages(groupe.id)).contenu
    const reponse = fil.find((m) => m.reponseA)!
    expect(reponse.reponseA).toMatchObject({ auteur: { nom: 'Rachel Biyong Mengue' }, piecesJointes: 2, supprime: false })
    expect(reponse.mentions).toEqual([{ id: 'U-001', nom: 'Paul Mbarga Atangana' }])
    expect(reponse.mentionneMoi).toBe(true)
    const purge = (await s.messages(directe.id)).contenu.find((m) => m.reponseA)!
    expect(purge.reponseA).toEqual({ id: 'MSG-PURGE', auteur: null, extrait: '', piecesJointes: 0, envoyeLe: null, supprime: true })
    expect(purge.mentionneMoi).toBe(false)
  })

  it('envoi : citation de la conversation, mentions des membres ; refus 422 sinon', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi }).messagerie
    const groupe = (await s.conversations({})).contenu[0]
    const fil = (await s.messages(groupe.id)).contenu
    const evenement = fil.find((m) => m.evenement)!
    const sna = fil.find((m) => m.auteur.id === 'u-sna')!
    const e = await s.envoyer(groupe.id, { contenu: '@Rachel Biyong Mengue merci', etiquettes: [], fichiers: [], reponseA: sna.id, mentions: ['u-sna', 'u-sna'] })
    expect(e.reponseA).toMatchObject({ id: sna.id, supprime: false })
    expect(e.mentions).toEqual([{ id: 'u-sna', nom: 'Rachel Biyong Mengue' }])
    expect(e.mentionneMoi).toBe(false)
    const base = { contenu: 'x', etiquettes: [], fichiers: [] }
    expect(await code(s.envoyer(groupe.id, { ...base, reponseA: 'inconnu' }))).toBe('422 COL_REPONSE_INVALIDE')
    expect(await code(s.envoyer(groupe.id, { ...base, reponseA: evenement.id }))).toBe('422 COL_REPONSE_INVALIDE')
    expect(await code(s.envoyer(groupe.id, { ...base, mentions: ['u-dg'] }))).toBe('422 COL_MENTION_INVALIDE')
    expect(await code(s.envoyer(groupe.id, { ...base, mentions: ['U-001'] }))).toBe('422 COL_MENTION_INVALIDE')
    expect(await code(s.envoyer(groupe.id, { ...base, mentions: [''] }))).toBe('422 COL_MENTION_INVALIDE')
    // Création : citation seulement dans la conversation directe réutilisée.
    const directe = (await s.conversations({})).contenu.find((c) => c.type === 'DIRECTE' && c.membres.some((m) => m.id === 'U-201'))!
    const cite = (await s.messages(directe.id)).contenu[0]
    expect(await code(s.demarrer({ ...base, destinataires: ['U-201'], titre: null, liaisonId: null, reponseA: cite.id, mentions: ['U-201'] }))).toBe('ok')
    expect(await code(s.demarrer({ ...base, destinataires: ['u-dg'], titre: null, liaisonId: null, reponseA: cite.id }))).toBe('422 COL_REPONSE_INVALIDE')
    expect(await code(s.demarrer({ ...base, destinataires: ['u-dg'], titre: null, liaisonId: null, mentions: ['u-prod'] }))).toBe('422 COL_MENTION_INVALIDE')
  })
})

describe('notifications MENTION et REPONSE (D-82, notifications.md §7)', () => {
  it('réception simulée : mention, réponse, une seule notification si mentionné et cité ; aperçu du message', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi })
    const n = notificationsDemo(s, { moi: moi.id })
    await n.marquerLues()
    const demo = (window as unknown as { gmDemoNotifications: DemoNotifications }).gmDemoNotifications
    const cid = demo.recevoirMessage({ contenu: 'Pouvez-vous valider ?', mention: true })
    demo.recevoirMessage({ conversationId: cid, contenu: 'Je confirme.', reponse: true })
    demo.recevoirMessage({ conversationId: cid, contenu: 'Les deux à la fois.', reponse: true, mention: true })
    const liste = (await n.lister({ etat: 'non-lues' })).contenu
    const types = liste.map((x) => x.type)
    expect(types.filter((x) => x === 'MENTION')).toHaveLength(2)
    expect(types.filter((x) => x === 'REPONSE')).toHaveLength(1)
    const mention = liste.find((x) => x.type === 'MENTION')!
    expect(mention).toMatchObject({ titre: 'Aïcha Mahamat Nour vous a mentionné', conversationId: cid, reference: { service: 'collaboration', type: 'MESSAGE' }, nombre: 1 })
    expect(mention.lien).toMatch(new RegExp(`^/des/messagerie\\?conversation=${cid}&message=MSG-`))
    expect(await lecteurApercu(s)(mention)).toBe('@Paul Mbarga Atangana Les deux à la fois.')
    const reponse = liste.find((x) => x.type === 'REPONSE')!
    expect(reponse.titre).toBe('Aïcha Mahamat Nour a répondu à votre message')
    // Message introuvable dans la dernière page : pas d'aperçu.
    expect(await lecteurApercu(s)({ ...reponse, reference: { ...reponse.reference, id: 'absent' } })).toBeNull()
    // Ouvrir la conversation les lit toutes.
    await n.marquerLues({ reference: referenceConversation(cid) })
    expect((await n.compteur()).nonLues).toBe(0)
  })

  it('arriveeMention : rien pour son propre message ni sans mention ni citation ; sous-titre d’un groupe', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: moi }).messagerie
    const groupe = (await s.conversations({})).contenu[0]
    const r = s.recevoirDemo({ conversationId: groupe.id, mention: true })
    expect(arriveeMention(r, moi.id)).toMatchObject({ type: 'MENTION', sousTitre: 'Essais de LIA-2026-030' })
    expect(arriveeMention(r, 'autre')).toBeNull()
    expect(arriveeMention({ ...r, message: { ...r.message, moi: true } }, moi.id)).toBeNull()
    const n = { type: 'MENTION', reference: { service: 'collaboration', type: 'MESSAGE', id: 'x' }, conversationId: null } as unknown as NotificationPlateforme
    expect(await lecteurApercu({ messagerie: s })(n)).toBeNull()
  })
})

describe('aperçu bureautique (D-83)', () => {
  it('HTTP : POST /bff/{couche}/document/apercus, corps brut, X-Nom-Fichier encodé, PDF en retour', async () => {
    const f = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('%PDF', { status: 200, headers: { 'Content-Type': 'application/pdf' } }))
    const source = new Blob(['PK'], { type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation' })
    const pdf = await apercuHttp('des').convertir(source, 'Plan de bascule v2.pptx')
    expect(await pdf.text()).toBe('%PDF')
    await apercuHttp('console').convertir(new Blob(['x']), 'note.doc')
    const [u0, i0] = f.mock.calls[0] as [URL, RequestInit]
    expect(`${i0.method} ${new URL(String(u0)).pathname}`).toBe('POST /bff/des/document/apercus')
    const h = new Headers(i0.headers)
    expect(h.get('X-Nom-Fichier')).toBe('Plan%20de%20bascule%20v2.pptx')
    expect(h.get('Accept')).toBe('application/pdf, application/problem+json')
    expect(h.get('Content-Type')).toBe('application/vnd.openxmlformats-officedocument.presentationml.presentation')
    expect(i0.body).toBe(source)
    expect(new URL(String(f.mock.calls[1][0])).pathname).toBe('/bff/console/document/apercus')
    expect(new Headers((f.mock.calls[1][1] as RequestInit).headers).get('Content-Type')).toBe('application/octet-stream')
    expect(servicesHttp('portail').apercu).toBeUndefined()
    expect(servicesHttp('console').apercu).toBeDefined()
  })

  it('simulé : PDF de démonstration, refus du contrat', async () => {
    const a = servicesSimules({ latenceMs: 0 }).apercu!
    const pdf = await a.convertir(new Blob(['PK']), 'plan.pptx')
    expect(pdf.type).toBe('application/pdf')
    expect((await a.convertir(new Blob(['PK']), 'tableau.ods')).size).toBeGreaterThan(100)
    expect((await a.convertir(new Blob(['PK']), 'note.docx')).size).toBeGreaterThan(100)
    expect(await code(a.convertir({ size: 21 * 1024 * 1024 } as Blob, 'gros.docx'))).toBe('413 DOC_APERCU_TROP_VOLUMINEUX')
    expect(await code(a.convertir(new Blob(['x']), 'macro.docm'))).toBe('415 DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE')
    expect(await code(a.convertir(new Blob(['x']), 'contrat-protege.docx'))).toBe('422 DOC_APERCU_ILLISIBLE')
    expect(await code(a.convertir(new Blob(['x']), 'indisponible.xlsx'))).toBe('503 DOC_APERCU_INDISPONIBLE')
  })
})
