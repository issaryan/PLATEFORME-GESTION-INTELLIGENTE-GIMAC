import { describe, expect, it } from 'vitest'
import { executionsSandboxDemo, resultatsSandboxDemo } from './sandbox'

// Démonstration des résultats de la sandbox (US-EXI-17) : interface de test selon le type de projet, campagne inconnue.
describe('résultats de la sandbox de démonstration', () => {
  const mobile = { id: 'LIA-2026-099', typeProjet: 'PAIEMENT_MOBILE' as const, statut: 'EN_TEST', services: [{ code: 'TRANSFERT', inclus: true }] }
  it('paiement mobile : interface MOB et scénarios MOB ; autre campagne : aucune exécution', () => {
    const r = resultatsSandboxDemo(mobile)
    expect(r.campagnes[0].interfaceTest).toBe('MOB')
    const page = executionsSandboxDemo(mobile, r.campagnes[0].campagneId, 0, 25)
    expect(page.contenu.some((e) => e.identifiantScenario.startsWith('SCN-MOB-'))).toBe(true)
    expect(executionsSandboxDemo(mobile, 'autre-campagne', 0, 25)).toMatchObject({ contenu: [], total: 0 })
  })
})
