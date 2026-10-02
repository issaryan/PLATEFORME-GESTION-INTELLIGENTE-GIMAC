import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, waitFor } from '@testing-library/react'
import type { SourceRecherche } from '@gimac/socle'
import { useSourcesRecherche } from './recherche'
import { rendrePage, sessionTest } from './test/rendu'
import { servicesSimules } from './services/simule'
import { PROFILS } from './permissions'

// Recherche globale (Ctrl+K) : liaisons, institutions et conversations selon les permissions, liens vers les écrans.
afterEach(cleanup)

let sources: SourceRecherche[] = []
function Sonde({ permissions, espace }: { permissions: string[]; espace?: string }) {
  sources = useSourcesRecherche(permissions, espace)
  return <span>{sources.map((s) => s.cle).join(',')}</span>
}
const monter = (permissions: string[], espace?: string) =>
  rendrePage(<Sonde permissions={permissions} espace={espace} />, {
    chemin: '/des/x', motif: '/des/x', session: { ...sessionTest(permissions), sujet: 'U-001' }, services: servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-001', nom: 'Paul Mbarga Atangana' } }),
  })

describe('useSourcesRecherche', () => {
  it('chef du DES : liaisons, institutions et conversations, avec leurs liens', async () => {
    monter(PROFILS.ROLE_CHEF_DES)
    expect(await screen.findByText('liaisons,institutions,conversations')).toBeTruthy()
    const [liaisons, institutions, conversations] = sources
    const l = await liaisons.chercher('LIA-2026-030')
    expect(l[0]).toMatchObject({ type: 'liaison', code: 'LIA-2026-030', href: expect.stringMatching(/^\/des\/parcours\//) })
    const i = await institutions.chercher('Banque')
    expect(i.length).toBeGreaterThan(0)
    expect(i[0].href).toMatch(/^\/des\/institutions\//)
    const c = await conversations.chercher('Essais')
    expect(c[0]).toMatchObject({ type: 'conversation', libelle: 'Essais de LIA-2026-030', detail: 'LIA-2026-030', href: expect.stringMatching(/^\/des\/messagerie\?conversation=/) })
    // Conversation directe : le nom de l'interlocuteur, sans le sien.
    const d = await conversations.chercher('')
    expect(d.map((x) => x.libelle)).toContain('Aïcha Mahamat Nour')
    expect(d.map((x) => x.libelle).join()).not.toContain('Paul Mbarga Atangana')
  })
  it('console : conversations seulement ; sans permission : aucune source', async () => {
    monter(PROFILS.ROLE_RSSI, '/console')
    expect(await screen.findByText('conversations')).toBeTruthy()
    expect((await sources[0].chercher('Essais'))[0].href).toMatch(/^\/console\/messagerie\?conversation=/)
    cleanup()
    monter([])
    await waitFor(() => expect(sources).toEqual([]))
  })
})
