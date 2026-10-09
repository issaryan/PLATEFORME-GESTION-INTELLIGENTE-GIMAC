import { afterEach, describe, expect, it } from 'vitest'
import { act, cleanup, fireEvent, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import TableauDeBordPage from './TableauDeBordPage'
import { RSSI, SUPER_ADMIN, avec, rendrePage, session, simules } from '../test/rendu'
import type { EcouteurFlux } from '../services/ports'
import { instantane } from '../services/simule/donnees'
import { PROFILS_DEMO } from '../profilsDemo'

afterEach(cleanup)
const page = (permissions: string[], o: Parameters<typeof rendrePage>[1] extends infer T ? Partial<T> : never = {}) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/console/tableau-de-bord', motif: '/console/tableau-de-bord', session: session(permissions, { roles: ['ROLE_SUPER_ADMIN'] }), ...o })

// D-61, D-62 : tableau de bord composé selon les permissions de la session.
describe('Tableau de bord de la console', () => {
  it('super administrateur : état de la plateforme en temps réel, services, serveurs, alertes, comptes', async () => {
    const { container } = page(SUPER_ADMIN)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Tableau de bord du super administrateur')
    expect(screen.getByText(/· Super administrateur$/)).toBeTruthy()
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
    expect(await screen.findByRole('group', { name: 'Comptes de la plateforme' })).toBeTruthy()
    expect(screen.queryByRole('group', { name: 'Posture de sécurité des comptes' })).toBeNull()
    // Gestion des utilisateurs en tête du tableau de bord, avant la supervision.
    const regions = screen.getAllByRole('region').map((r) => r.getAttribute('aria-labelledby') && document.getElementById(r.getAttribute('aria-labelledby')!)?.textContent)
    expect(regions.indexOf('Gestion des utilisateurs')).toBeLessThan(regions.indexOf('Supervision de la plateforme'))
  })

  it('RSSI : posture de sécurité, derniers actes, politique, et supervision ouverte par sa permission', async () => {
    page(RSSI)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Tableau de bord de la sécurité des SI')
    expect(await screen.findByRole('group', { name: 'Posture de sécurité des comptes' })).toBeTruthy()
    expect(await screen.findByText('Sans second facteur')).toBeTruthy()
    expect(screen.getByText('Créés depuis 30 jours')).toBeTruthy()
    expect(await screen.findByRole('listitem', { name: 'Super administrateur : 1' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Super administrateur' })).toBeTruthy()
    const activite = screen.getByRole('region', { name: 'Activité des 30 derniers jours' })
    expect(within(activite).getByText('Comptes créés')).toBeTruthy()
    expect(within(activite).getByText('Actes d’administration')).toBeTruthy()
    expect(await screen.findByText('Désactivation')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Consulter la politique de sécurité' }).getAttribute('href')).toBe('/console/politique-securite')
    expect(await screen.findByRole('region', { name: 'État en temps réel' })).toBeTruthy()
  })

  it('RSSI sans supervision ni journal : ni flux ni derniers actes', async () => {
    page(RSSI.filter((p) => p !== 'PLATFORM_SUPERVISION_READ' && p !== 'IAM_JOURNAL_READ'))
    expect(await screen.findByRole('group', { name: 'Posture de sécurité des comptes' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Journal d’administration' })).toBeNull()
    expect(screen.queryByText('Supervision de la plateforme')).toBeNull()
    expect(screen.queryByText('Derniers actes d’administration')).toBeNull()
  })

  it.each([['super administrateur', SUPER_ADMIN], ['RSSI', RSSI]] as const)('%s : gestion des utilisateurs, actions directes et indicateurs cliquables vers la liste filtrée', async (_n, permissions) => {
    // Motif large : le tableau de bord reste monté pendant que l'adresse change.
    page([...permissions], { motif: '/console/*' })
    const gestion = screen.getByRole('region', { name: 'Gestion des utilisateurs' })
    const actions = within(gestion).getByRole('group', { name: 'Actions de gestion des utilisateurs' })
    const allerPar = async (nom: string, cible: string) => {
      fireEvent.click(within(actions).getByRole('button', { name: nom }))
      expect(screen.getByTestId('emplacement').textContent).toBe(cible)
    }
    await allerPar('Tous les comptes', '/console/comptes')
    await allerPar('Comptes sans second facteur', '/console/comptes?etat=SANS_2FA')
    await allerPar('Premiers accès en attente', '/console/comptes?etat=PREMIER_ACCES')
    await allerPar('Profils et permissions', '/console/profils')
    await allerPar('Journal d’administration', '/console/journal')
    await allerPar('Politique de sécurité', '/console/politique-securite')
    const cartes = await within(gestion).findByRole('group', { name: /Comptes de la plateforme|Posture de sécurité des comptes/ })
    const carte = (libelle: string) => within(cartes).getByText(libelle).closest('[role="button"]') as HTMLElement
    fireEvent.click(carte('Sans second facteur'))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes?etat=SANS_2FA')
    fireEvent.keyDown(carte('Premier accès en attente'), { key: 'Enter' })
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes?etat=PREMIER_ACCES')
    fireEvent.click(carte('Actifs'))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes?etat=ACTIF')
    fireEvent.click(carte('Désactivés'))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/comptes?etat=INACTIF')
    fireEvent.click(carte('Comptes'))
    fireEvent.click(carte('Comptes privilégiés'))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/profils')
    fireEvent.click(carte('Actes d’administration (24 h)'))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/journal')
    // Création d'un compte dans une fenêtre modale, sans quitter le tableau de bord.
    fireEvent.click(within(actions).getByRole('button', { name: 'Créer un compte' }))
    const d = screen.getByRole('dialog', { name: 'Créer un compte' })
    expect(d.getAttribute('aria-modal')).toBe('true')
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('sans droit de création : pas de bouton « Créer un compte »', async () => {
    page(RSSI.filter((p) => p !== 'IAM_ACCOUNT_CREATE'))
    await screen.findByRole('group', { name: 'Posture de sécurité des comptes' })
    expect(screen.queryByRole('button', { name: 'Créer un compte' })).toBeNull()
  })

  it('profil sans tableau de bord : état vide', () => {
    page([], { session: session(['CER_DOSSIER_READ']) })
    expect(screen.getByText('Aucun tableau de bord pour votre profil')).toBeTruthy()
    cleanup()
    page(['IAM_ROLE_PRIVILEGED_GRANT'])
    expect(screen.getByText('Aucun tableau de bord pour votre profil')).toBeTruthy()
  })

  // D-78, D-79 : composé par permission ; l'administrateur système n'a ni comptes ni politique.
  it('administrateur système : exploitation (charges en erreur) puis supervision, sans gestion des comptes', async () => {
    page(PROFILS_DEMO.ROLE_ADMIN_SYSTEME.permissions)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Tableau de bord de l’exploitation')
    const charges = await screen.findByRole('region', { name: 'Charges en erreur' })
    expect(await within(charges).findByText('pilotage')).toBeTruthy()
    expect(within(charges).getByText('certification')).toBeTruthy()
    expect(within(charges).getByRole('link', { name: 'Ouvrir l’infrastructure' }).getAttribute('href')).toBe('/console/plateforme/infrastructure')
    expect(await screen.findByRole('region', { name: 'État en temps réel' })).toBeTruthy()
    expect(screen.queryByRole('group', { name: 'Comptes de la plateforme' })).toBeNull()
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
    // Valeur en mots : taille réduite et césure pour rester dans la carte (signalement du 09/10/2026).
    expect(screen.getByText('Opérationnelle', { selector: '.gm-stat-value' }).classList.contains('gm-stat-value-texte')).toBe(true)
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
    await screen.findByRole('group', { name: langue === 'en' ? 'Account security posture' : 'Postura de seguridad de las cuentas' })
    await screen.findByRole('region', { name: langue === 'en' ? 'User management' : 'Gestión de usuarios' })
    await screen.findByText(langue === 'en' ? 'Real time' : 'Tiempo real')
    expect(container.textContent).not.toMatch(/\b(console|compte|commun)\.[a-zA-Z]/)
  })
})
