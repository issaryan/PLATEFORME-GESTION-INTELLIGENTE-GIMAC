import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import MaLiaisonPage from './MaLiaisonPage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './parcours/outilsTest'

afterEach(cleanup)

const monter = (chemin: string, o: Parameters<typeof ouvrir>[3] = {}) =>
  ouvrir(<MaLiaisonPage />, chemin, chemin.startsWith('/portail/liaisons/') ? '/portail/liaisons/:id' : '/portail/liaisons', o)

describe('POR-03 Mes liaisons et suivi de mon projet', () => {
  it('liste : les liaisons de l’institution du jeton, étape courante et deux couvertures', async () => {
    const { container } = monter('/portail/liaisons')
    expect(await screen.findByText('LIA-2025-088')).toBeTruthy()
    for (const id of ['LIA-2026-030', 'LIA-2026-027', 'LIA-2026-046']) expect(screen.getByText(id)).toBeTruthy()
    expect(screen.queryByText('LIA-2026-022')).toBeNull()
    expect(screen.getAllByRole('img', { name: /Couverture de certification : 100 %/ }).length).toBeGreaterThan(0)
    await auditer(container)
  })

  it('D-104 : plusieurs projets du même type, distingués par leur intitulé, chacun mène à son suivi', async () => {
    const s = services((base) => ({
      mesLiaisons: async () => (await base.portailParcours.mesLiaisons()).slice(0, 2).map((l, i) => ({ ...l, typeProjet: 'PAIEMENT_MOBILE' as const, intituleProjet: `Projet Mobile EP A Lot ${i + 1}` })),
    }))
    monter('/portail/liaisons', { services: s })
    expect(await screen.findByText('Projet Mobile EP A Lot 1')).toBeTruthy()
    expect(screen.getByText('Projet Mobile EP A Lot 2')).toBeTruthy()
    const liens = screen.getAllByRole('link').map((a) => a.getAttribute('href')).filter((h) => h?.startsWith('/portail/liaisons/'))
    expect(new Set(liens).size).toBe(2)
  })

  it('D-104 : le suivi rappelle l’intitulé du projet ; la référence d’origine n’est jamais montrée', async () => {
    const s = services((base) => ({
      dossier: async (id: string) => ({ ...(await base.portailParcours.dossier(id)), intituleProjet: 'Projet Mobile EP A Lot 2', referenceOrigine: null }),
    }))
    monter('/portail/liaisons/LIA-2026-030', { services: s })
    expect(await screen.findByText('Projet Mobile EP A Lot 2')).toBeTruthy()
  })

  it('liste vide : état explicite', async () => {
    monter('/portail/liaisons', { services: services(() => ({ mesLiaisons: () => Promise.resolve([]) })) })
    expect(await screen.findByText('Aucune liaison ouverte pour votre institution')).toBeTruthy()
  })

  it('suivi : blocage qui le concerne, dix étapes numérotées et verrou 2, interlocuteurs par fonction', async () => {
    const { container } = monter('/portail/liaisons/LIA-2026-030')
    expect(await screen.findByRole('heading', { level: 1, name: 'Suivi de mon projet LIA-2026-030' })).toBeTruthy()
    expect(await screen.findByText('Élément de sécurité du participant à renouveler avant la reprise des essais')).toBeTruthy()
    expect(screen.getByText('Obtenir le nouvel élément auprès du participant')).toBeTruthy()
    expect(screen.getAllByText('Pôle réseau').length).toBeGreaterThan(0)
    const barre = screen.getByRole('list', { name: 'Progression des dix étapes' })
    expect(within(barre).getAllByRole('listitem')).toHaveLength(10)
    expect(within(barre).getByText('6. Intégration').getAttribute('aria-current')).toBe('step')
    expect(screen.getByText('Préalable : Approbation de la sécurité')).toBeTruthy()
    expect(screen.getByText('Conducteur de projet')).toBeTruthy()
    expect(screen.getByText('participant-030.essai:8443')).toBeTruthy()
    expect(await screen.findByText('Rien ne vous est demandé pour le moment')).toBeTruthy()
    sansCleNiNomInterne()
    await auditer(container)
  })

  it('suivi : les tâches de la liaison mènent à leur geste ; pièces avec leur état', async () => {
    monter('/portail/liaisons/LIA-2026-027')
    expect(await screen.findByRole('button', { name: 'Signer' })).toBeTruthy()
    expect(await screen.findByText('Établi, à signer')).toBeTruthy()
    expect(screen.getByText('pas encore attendues')).toBeTruthy()
    expect(screen.getByText('Banque B')).toBeTruthy()
  })

  it('suivi : service supplémentaire rattaché à sa liaison d’origine, taux du simulateur repris', async () => {
    monter('/portail/liaisons/LIA-2026-046')
    expect(await screen.findByText('Service supplémentaire de LIA-2025-088')).toBeTruthy()
    expect(screen.getByText('Non communiquées')).toBeTruthy()
  })

  it('cloisonnement : une liaison d’une autre institution est introuvable', async () => {
    monter('/portail/liaisons/LIA-2026-022')
    const alerte = await screen.findByRole('alert')
    expect(alerte.textContent).toContain('Ce parcours est introuvable.')
  })

  it('erreur avec identifiant de corrélation', async () => {
    monter('/portail/liaisons/LIA-2026-099', { services: services(() => ({ dossier: () => Promise.reject(new ApiError(404, 'HOM_LIAISON_NOT_FOUND', 'Introuvable.', 'cid-404')) })) })
    expect((await screen.findByRole('alert')).textContent).toContain('cid-404')
  })

  it('sans CER_PORTAL_READ, les pièces de certification ne sont pas proposées', async () => {
    monter('/portail/liaisons/LIA-2026-027', { sans: ['CER_PORTAL_READ'] })
    await screen.findByText('Vos pièces')
    expect(screen.queryByText('Procès-verbal de certification')).toBeNull()
    expect(screen.getByText('Fiche de suivi des services')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('suivi en %s sans clé brute', async (langue) => {
    monter('/portail/liaisons/LIA-2025-088', { langue })
    await screen.findByText(langue === 'en' ? 'Tracking my project LIA-2025-088' : 'Seguimiento de mi proyecto LIA-2025-088')
    await screen.findByText(langue === 'en' ? 'Your documents' : 'Sus documentos')
    sansCleNiNomInterne()
  })
})

// Livrables du participant : la tâche du tableau de bord et du dossier mène à l'écran de dépôt
// (/portail/liaisons/:id/livrables/:op), quel que soit le lien servi.
describe('Portail : tâches de livrable vers l’écran de dépôt', () => {
  it('dossier : bouton « Déposer » de la tâche et pièce « à déposer » menant à l’écran de dépôt', async () => {
    const s = services((b) => ({
      mesTaches: async () => {
        const t = await b.portailParcours.mesTaches()
        const livrable = { ...t.taches[0], operationId: 'LIA-2026-046:15', liaisonId: 'LIA-2026-046', liaisonReference: 'LIA-2026-046', rang: '15', lien: '/portail/liaisons/LIA-2026-046', actionsPermises: ['DEPOSER_LIVRABLE'] }
        return { ...t, taches: [...t.taches, livrable] }
      },
    }))
    monter('/portail/liaisons/LIA-2026-046', { services: s, autres: [{ motif: '/portail/liaisons/:id/livrables/:op', element: <span>écran de dépôt</span> }] })
    const depot = await screen.findByRole('link', { name: 'Déposer le livrable' })
    expect(depot.getAttribute('href')).toBe('/portail/liaisons/LIA-2026-046/livrables/LIA-2026-046%3A15')
    expect(screen.getByText('à déposer')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Déposer' }))
    expect(await screen.findByText('écran de dépôt')).toBeTruthy()
  })
})
