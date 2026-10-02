import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, waitFor } from '@testing-library/react'
import type { SourceRecherche } from '@gimac/socle'
import { useSourceComptes } from './recherche'
import { rendrePage, RSSI } from './test/rendu'

// Recherche globale (Ctrl+K) : comptes pour les porteurs de IAM_ACCOUNT_READ, fiche ouverte dans l'application courante.
afterEach(cleanup)

let sources: SourceRecherche[] = []
function Sonde({ permissions }: { permissions: string[] }) {
  sources = useSourceComptes(permissions)
  return <span>{sources.length ? 'comptes' : 'aucune'}</span>
}

describe('useSourceComptes', () => {
  it('RSSI : comptes trouvés par nom, lien vers la fiche ; sans permission : aucune source', async () => {
    rendrePage(<Sonde permissions={RSSI} />, { chemin: '/console/x', motif: '/console/x' })
    expect(await screen.findByText('comptes')).toBeTruthy()
    const r = await sources[0].chercher('biyong')
    expect(r[0]).toMatchObject({ type: 'compte', libelle: expect.stringContaining('Biyong'), href: expect.stringMatching(/^\/console\/comptes\//) })
    cleanup()
    rendrePage(<Sonde permissions={[]} />, { chemin: '/console/x', motif: '/console/x' })
    await waitFor(() => expect(screen.getByText('aucune')).toBeTruthy())
  })
})
