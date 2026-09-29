import { afterEach, describe, expect, it } from 'vitest'
import { act, cleanup, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import TableauDeBordPage from './TableauDeBordPage'
import { LECTEUR, RSSI, SUPER_ADMIN, avec, rendrePage, session, simules } from '../test/rendu'
import type { EcouteurFlux } from '../services/ports'
import { instantane } from '../services/simule/donnees'

afterEach(cleanup)
const page = (permissions: string[], o: Parameters<typeof rendrePage>[1] extends infer T ? Partial<T> : never = {}) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/console/tableau-de-bord', motif: '/console/tableau-de-bord', session: session(permissions, { roles: ['ROLE_SUPER_ADMIN'] }), ...o })

// D-61, D-62 : tableau de bord composé selon les permissions de la session.
describe('Tableau de bord de la console', () => {
  it('super administrateur : état de la plateforme en temps réel, services, serveurs, alertes, comptes', async () => {
    const { container } = page(SUPER_ADMIN)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Tableau de bord du super administrateur')
    expect(screen.getByText('Super administrateur')).toBeTruthy()
    const direct = await screen.findByRole('region', { name: 'État en temps réel' })
    expect(within(direct).getByText('Dégradée', { selector: '.gm-stat-value' })).toBeTruthy()
    expect(within(direct).getByText('7 / 8')).toBeTruthy()
    expect(await within(direct).findByText('Temps réel')).toBeTruthy()
    expect(container.querySelector('[aria-live="polite"]')).toBeTruthy()
    expect(await screen.findByText('gimac-donnees-01')).toBeTruthy()
    expect(screen.getByText('keycloak')).toBeTruthy()
    expect(screen.getAllByText('injoignable').length).toBeGreaterThan(0)
    expect(await screen.findByText('ServiceInjoignable')).toBeTruthy()
    expect(screen.getAllByText('Critiques').length).toBe(2)
    expect(await screen.findByText('Comptes de la plateforme')).toBeTruthy()
    expect(screen.queryByText('Posture de sécurité des comptes')).toBeNull()
  })

  it('RSSI : posture de sécurité, derniers actes, politique, et supervision ouverte par sa permission', async () => {
    page(RSSI)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Tableau de bord de la sécurité des SI')
    expect(await screen.findByText('Posture de sécurité des comptes')).toBeTruthy()
    expect(await screen.findByText('Sans second facteur')).toBeTruthy()
    expect(screen.getByText('Créés depuis 30 jours')).toBeTruthy()
    expect(await screen.findByText(/Super administrateur : 1/)).toBeTruthy()
    expect(await screen.findByText('Désactivation')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Consulter la politique de sécurité' }).getAttribute('href')).toBe('/console/politique-securite')
    expect(await screen.findByRole('region', { name: 'État en temps réel' })).toBeTruthy()
  })

  it('RSSI sans supervision ni journal : ni flux ni derniers actes', async () => {
    page(RSSI.filter((p) => p !== 'PLATFORM_SUPERVISION_READ' && p !== 'IAM_JOURNAL_READ'))
    expect(await screen.findByText('Posture de sécurité des comptes')).toBeTruthy()
    expect(screen.queryByText('Supervision de la plateforme')).toBeNull()
    expect(screen.queryByText('Derniers actes d’administration')).toBeNull()
  })

  it('profil sans tableau de bord : état vide', () => {
    page([], { session: session(['CER_DOSSIER_READ']) })
    expect(screen.getByText('Aucun tableau de bord pour votre profil')).toBeTruthy()
    cleanup()
    page(LECTEUR.concat('IAM_ROLE_PRIVILEGED_GRANT'))
    expect(screen.getByText('Aucun tableau de bord pour votre profil')).toBeTruthy()
  })

  it('flux interrompu : bandeau et état annoncé, impulsion reçue ensuite ; Alertmanager injoignable', async () => {
    let ecouteur: EcouteurFlux | null = null
    const services = avec('plateforme', {
      flux: (e) => {
        ecouteur = e
        e.onEtat('perdu')
        return () => undefined
      },
    })
    page(SUPER_ADMIN, { services })
    expect(await screen.findByText(/Le flux temps réel est interrompu/)).toBeTruthy()
    expect(await screen.findByText('Flux interrompu, reconnexion en cours')).toBeTruthy()
    act(() => {
      ecouteur!.onEtat('connecte')
      ecouteur!.onImpulsion({ sequence: 9, instantane: instantane(0, false), nombreAlertes: -1 })
    })
    expect(await screen.findByText('Opérationnelle', { selector: '.gm-stat-value' })).toBeTruthy()
    expect(screen.getAllByText('Alertmanager injoignable').length).toBeGreaterThan(0)
    expect(screen.queryByText(/Le flux temps réel est interrompu/)).toBeNull()
  })

  it('supervision indisponible : erreur avec corrélation ; Prometheus et Alertmanager injoignables', async () => {
    const base = simules()
    const services = avec('plateforme', {
      flux: () => () => undefined,
      services: async () => { throw new ApiError(502, 'HTTP_502', 'Service injoignable ou en erreur', 'RQ-GW-502') },
      serveurs: async () => ({ ...(await base.plateforme.serveurs()), prometheusJoignable: false, erreur: 'connexion refusée', hotes: [], conteneurs: [] }),
      alertes: async () => ({ horodatage: '', joignable: false, erreur: 'délai dépassé', nombre: 0, alertes: [] }),
    }, base)
    page(SUPER_ADMIN, { services })
    expect(await screen.findByText('RQ-GW-502')).toBeTruthy()
    expect(await screen.findByText(/Prometheus est injoignable/)).toBeTruthy()
    expect(screen.getByText('Aucun serveur remonté (node-exporter absent)')).toBeTruthy()
    expect(await screen.findByText('délai dépassé')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page(RSSI, { langue })
    await screen.findByText(langue === 'en' ? 'Account security posture' : 'Postura de seguridad de las cuentas')
    await screen.findByText(langue === 'en' ? 'Real time' : 'Tiempo real')
    expect(container.textContent).not.toMatch(/\b(console|compte|commun)\.[a-zA-Z]/)
  })
})
