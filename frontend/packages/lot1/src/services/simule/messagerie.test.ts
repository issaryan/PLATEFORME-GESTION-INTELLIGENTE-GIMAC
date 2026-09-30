import { describe, expect, it } from 'vitest'
import { servicesSimules } from './index'
import { ANNUAIRE, masquerNumerosCarte } from './messagerie'

// Messagerie simulée (D-64) : mêmes formes et mêmes refus que le contrat de MS-COLLABORATION.
const svc = (id = 'U-001') => servicesSimules({ latenceMs: 0, utilisateur: { id, nom: ANNUAIRE.find((p) => p.id === id)?.nom ?? 'Moi' } }).messagerie
const refus = (code: string) => expect.objectContaining({ code })
const fichier = (nom: string, taille = 10) => new File(['x'.repeat(taille)], nom, { type: 'application/pdf' })

describe('RG-52 : masquage des numéros de carte', () => {
  it('numéro valide (Luhn) masqué 6 + 4, avec espaces ou tirets ; suite non valide intacte', () => {
    expect(masquerNumerosCarte('Carte 4111 1111 1111 1111 à bloquer')).toBe('Carte 411111******1111 à bloquer')
    expect(masquerNumerosCarte('5555-5555-5555-4444')).toBe('555555******4444')
    expect(masquerNumerosCarte('Ticket 1234567890123')).toBe('Ticket 1234567890123')
    expect(masquerNumerosCarte('LIA-2026-027')).toBe('LIA-2026-027')
  })
})

describe('messagerieSimulee', () => {
  it('annuaire sans l’utilisateur courant, filtré et paginé', async () => {
    const m = svc()
    const tout = await m.annuaire('')
    expect(tout.contenu.some((p) => p.id === 'U-001')).toBe(false)
    expect(tout.total).toBe(ANNUAIRE.length - 1)
    expect((await m.annuaire('sécurité')).contenu.map((p) => p.id)).toEqual(['u-rssi'])
    expect((await m.annuaire('', 1, 5)).contenu).toHaveLength(5)
  })
  it('conversations de l’utilisateur, la plus récente en tête ; non-lus ; recherche et liaison', async () => {
    const m = svc()
    const l = await m.conversations({})
    expect(l.total).toBe(3)
    expect(l.contenu[0]).toMatchObject({ titre: 'Essais de LIA-2026-030', type: 'GROUPE', nonLus: 2, liaison: { reference: 'LIA-2026-030' } })
    expect(l.contenu[0].dernierMessage).toMatchObject({ auteurNom: 'Rachel Biyong Mengue' })
    expect((await m.nonLus()).total).toBe(3)
    expect((await m.conversations({ recherche: 'pci' })).contenu).toHaveLength(1)
    expect((await m.conversations({ liaisonId: 'LIA-2026-027' })).contenu).toHaveLength(1)
    expect((await m.conversations({ page: 1, taille: 2 })).contenu).toHaveLength(1)
    await m.marquerLu(l.contenu[0].id)
    expect((await m.nonLus()).total).toBe(1)
  })
  it('fil : messages du plus ancien au plus récent, pièce téléchargeable par un membre', async () => {
    const m = svc()
    const [c] = (await m.conversations({})).contenu
    const fil = await m.messages(c.id)
    expect(fil.plusAnciens).toBe(false)
    expect(fil.contenu.map((x) => x.auteur.nom)).toEqual(['Rachel Biyong Mengue', 'Hervé Kamga Tchoupo', 'Rachel Biyong Mengue'])
    const piece = fil.contenu[0].piecesJointes[0]
    expect(piece).toMatchObject({ nom: 'rapport-essais-LIA-2026-030.pdf', typeMime: 'application/pdf' })
    expect((await m.telechargerPiece(piece.id)).blob.size).toBeGreaterThan(0)
    await expect(m.telechargerPiece('inconnue')).rejects.toEqual(refus('COL_ATTACHMENT_NOT_FOUND'))
    // Conversation inexistante ou fermée : même refus, sans en révéler l'existence.
    await expect(m.conversation('CONV-9999')).rejects.toEqual(refus('COL_CONVERSATION_NOT_FOUND'))
    await expect(m.messages('CONV-9999')).rejects.toEqual(refus('COL_CONVERSATION_NOT_FOUND'))
    await expect(m.marquerLu('CONV-9999')).rejects.toEqual(refus('COL_CONVERSATION_NOT_FOUND'))
  })
  it('démarrer : refus du contrat, conversation directe réutilisée, groupe créé ; envoi masqué et étiqueté', async () => {
    const m = svc()
    await expect(m.demarrer({ destinataires: [], titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_RECIPIENT_REQUIRED'))
    await expect(m.demarrer({ destinataires: ['U-001'], titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_RECIPIENT_SELF'))
    await expect(m.demarrer({ destinataires: ['inconnu'], titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_RECIPIENT_UNKNOWN'))
    await expect(m.demarrer({ destinataires: Array.from({ length: 51 }, (_, i) => `u${i}`), titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_TOO_MANY_RECIPIENTS'))
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: ' ', etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_MESSAGE_EMPTY'))
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: 'x'.repeat(4001), etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_MESSAGE_TOO_LONG'))
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: 'x', etiquettes: [{ liaisonId: 'L', reference: 'LIA-26' }], fichiers: [] })).rejects.toEqual(refus('COL_TAG_INVALID'))
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: 'x', etiquettes: Array.from({ length: 11 }, (_, i) => ({ liaisonId: `L${i}`, reference: `LIA-2026-${String(i).padStart(3, '0')}` })), fichiers: [] })).rejects.toEqual(refus('COL_TOO_MANY_TAGS'))
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: Array.from({ length: 6 }, (_, i) => fichier(`f${i}.pdf`)) })).rejects.toEqual(refus('COL_TOO_MANY_ATTACHMENTS'))
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: [fichier('virus.exe')] })).rejects.toEqual(refus('COL_ATTACHMENT_TYPE'))
    const gros = fichier('gros.pdf')
    Object.defineProperty(gros, 'size', { value: 21 * 1024 * 1024 })
    await expect(m.demarrer({ destinataires: ['u-sna'], titre: null, liaisonId: null, contenu: 'x', etiquettes: [], fichiers: [gros] })).rejects.toEqual(refus('COL_ATTACHMENT_TOO_LARGE'))
    // Échange direct avec la conductrice : la conversation existante est réutilisée.
    const directe = (await m.conversations({ recherche: 'signer' })).contenu[0]
    const reprise = await m.demarrer({ destinataires: ['U-201'], titre: null, liaisonId: null, contenu: 'Signé.', etiquettes: [], fichiers: [] })
    expect(reprise.id).toBe(directe.id)
    // Groupe rattaché à une liaison, avec pièce et numéro de carte masqué.
    const g = await m.demarrer({ destinataires: ['u-sna', 'u-prod'], titre: 'Bascule LIA-2026-012', liaisonId: 'LIA-2026-012', contenu: 'Carte de test 4111111111111111', etiquettes: [{ liaisonId: 'LIA-2026-012', reference: 'LIA-2026-012' }], fichiers: [fichier('plan.pdf')] })
    expect(g).toMatchObject({ type: 'GROUPE', titre: 'Bascule LIA-2026-012', liaison: { reference: 'LIA-2026-012' }, nonLus: 0 })
    expect(g.membres.map((x) => x.id)).toEqual(['U-001', 'u-sna', 'u-prod'])
    const [msg] = (await m.messages(g.id)).contenu
    expect(msg).toMatchObject({ moi: true, contenu: 'Carte de test 411111******1111', etiquettes: [{ reference: 'LIA-2026-012' }], piecesJointes: [{ nom: 'plan.pdf' }] })
    const suite = await m.envoyer(g.id, { contenu: 'Suite', etiquettes: [], fichiers: [] })
    expect(suite.conversationId).toBe(g.id)
    await expect(m.envoyer('inconnue', { contenu: 'x', etiquettes: [], fichiers: [] })).rejects.toEqual(refus('COL_CONVERSATION_NOT_FOUND'))
  })
  it('pagination du fil vers le passé', async () => {
    const m = svc()
    const g = await m.demarrer({ destinataires: ['u-sna'], titre: 'Long', liaisonId: null, contenu: 'm0', etiquettes: [], fichiers: [] })
    for (let i = 1; i < 60; i++) await m.envoyer(g.id, { contenu: `m${i}`, etiquettes: [], fichiers: [] })
    const recent = await m.messages(g.id)
    expect(recent.contenu).toHaveLength(50)
    expect(recent.plusAnciens).toBe(true)
    const ancien = await m.messages(g.id, recent.contenu[0].id)
    expect(ancien.contenu.map((x) => x.contenu)).toEqual(Array.from({ length: 10 }, (_, i) => `m${i}`))
    expect(ancien.plusAnciens).toBe(false)
  })
})
