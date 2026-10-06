import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import InfrastructurePage from './InfrastructurePage'
import SupervisionPage from './SupervisionPage'
import { WidgetChargesEnErreur, WidgetSupervision } from '../composants/widgetsPlateforme'
import { avec, rendrePage, simules } from '../test/rendu'
import { PROFILS_DEMO } from '../profilsDemo'

// D-78 : écran « Infrastructure » (charges, journaux, gestes signés, historique) ; D-79 : widgets de plateforme.
afterEach(cleanup)
const ADMIN = PROFILS_DEMO.ROLE_ADMIN_SYSTEME.permissions
const RSSI = PROFILS_DEMO.ROLE_RSSI.permissions
const page = (permissions: string[], o: Partial<Parameters<typeof rendrePage>[1]> = {}, chemin = '/console/plateforme/infrastructure') =>
  rendrePage(<InfrastructurePage />, { chemin, motif: '/console/plateforme/infrastructure', permissions, ...o })
const ligne = async (nom: string) => (await screen.findAllByText(nom, { selector: '.gm-mono' }))[0].closest('tr') as HTMLElement

describe('Infrastructure', () => {
  it('administrateur système : charges (état, santé, répliques, protection) et gestes permis seulement', async () => {
    page(ADMIN, { services: simules({ dureeGesteMs: 60_000, acteur: { id: 'u-admin-systeme', nom: 'Admin', permissions: ADMIN } }) })
    expect(await screen.findByText('Charges de travail (Kubernetes)')).toBeTruthy()
    const gateway = await ligne('gateway')
    expect(within(gateway).getByText('Protégée')).toBeTruthy()
    expect(within(gateway).queryByRole('button', { name: 'Arrêter gateway' })).toBeNull()
    expect(within(gateway).getByRole('button', { name: 'Redémarrer gateway' })).toBeTruthy()
    const pilotage = await ligne('pilotage')
    expect(within(pilotage).getByText('En erreur')).toBeTruthy()
    expect(within(pilotage).getByText('0 / 1')).toBeTruthy()
    expect(within(await ligne('collaboration')).getByRole('button', { name: 'Démarrer collaboration' })).toBeTruthy()
    expect(screen.queryByText(/Lecture seule/)).toBeNull()
  })

  it('geste signé : motif, clé de sécurité, opération acceptée puis inscrite dans l’historique', async () => {
    page(ADMIN, { services: simules({ dureeGesteMs: 60_000, acteur: { id: 'u-admin-systeme', nom: 'Admin', permissions: ADMIN } }) })
    fireEvent.click(within(await ligne('iam')).getByRole('button', { name: 'Redémarrer iam' }))
    const d = await screen.findByRole('dialog', { name: 'Redémarrer iam' })
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Prise en compte de la configuration' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    expect(await within(d).findByText('Signature confirmée')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText(/Opération acceptée sur iam/)).toBeTruthy()
    const historique = screen.getByText('Historique des opérations').closest('section') as HTMLElement
    expect(await within(historique).findByText('Prise en compte de la configuration')).toBeTruthy()
  })

  it('mise à l’échelle : répliques hors bornes refusées, puis acceptées ; refus du service lisible', async () => {
    page(ADMIN, { services: simules({ dureeGesteMs: 60_000, acteur: { id: 'u-admin-systeme', nom: 'Admin', permissions: ADMIN } }) })
    fireEvent.click(within(await ligne('certification')).getByRole('button', { name: 'Mettre certification à l’échelle' }))
    const d = await screen.findByRole('dialog', { name: 'Mettre certification à l’échelle' })
    fireEvent.change(within(d).getByLabelText(/Répliques voulues/), { target: { value: '9' } })
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Montée en charge de la campagne' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    expect(await within(d).findByText('Le nombre de répliques doit être compris entre 0 et 5.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Répliques voulues/), { target: { value: '3' } })
    fireEvent.click(within(d).getByRole('button', { name: /Réessayer|Utiliser ma clé de sécurité/ }))
    expect(await within(d).findByText('Signature confirmée')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    cleanup()
    page(ADMIN, { services: avec('infrastructure', { executer: async () => { throw new ApiError(409, 'OPS_CHARGE_PROTEGEE', 'x', 'cid-ops') } }) })
    fireEvent.click(within(await ligne('iam')).getByRole('button', { name: 'Arrêter iam' }))
    const d2 = await screen.findByRole('dialog', { name: 'Arrêter iam' })
    fireEvent.change(within(d2).getByLabelText(/Motif/), { target: { value: 'Maintenance programmée de nuit' } })
    fireEvent.click(within(d2).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    expect(await within(d2).findByText(/ne peut être arrêtée ni ramenée à zéro réplique/)).toBeTruthy()
  })

  it('journaux à la demande : lignes, durée, actualisation, fermeture ; lignes hors bornes signalées', async () => {
    page(ADMIN)
    fireEvent.click(within(await ligne('iam')).getByRole('button', { name: 'Journaux de iam' }))
    const liste = await screen.findByRole('list', { name: 'Journaux de iam' })
    expect(within(liste).getAllByRole('listitem').length).toBe(24)
    fireEvent.change(screen.getByLabelText(/^Lignes/), { target: { value: '0' } })
    expect(screen.getByText('Entre 1 et 500 lignes.')).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/^Lignes/), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Actualiser' }))
    await waitFor(() => expect(within(screen.getByRole('list', { name: 'Journaux de iam' })).getAllByRole('listitem').length).toBe(5))
    expect(screen.getByText(/Les 5 dernières lignes sont affichées/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Fermer les journaux' }))
    await waitFor(() => expect(screen.queryByRole('list', { name: 'Journaux de iam' })).toBeNull())
    cleanup()
    page(ADMIN, {}, '/console/plateforme/infrastructure?charge=collaboration')
    expect(await screen.findByText('Aucune ligne de journal sur cette période.')).toBeTruthy()
  })

  it('RSSI : lecture seule, aucun geste ; exploitation non configurée ; erreur de lecture lisible', async () => {
    page(RSSI)
    expect(await screen.findByText(/Lecture seule/)).toBeTruthy()
    expect(within(await ligne('iam')).queryByRole('button', { name: 'Redémarrer iam' })).toBeNull()
    expect(within(await ligne('iam')).getByRole('button', { name: 'Journaux de iam' })).toBeTruthy()
    cleanup()
    page(ADMIN, { services: simules({ orchestrateur: 'AUCUN' }) })
    expect(await screen.findByText('Exploitation non configurée')).toBeTruthy()
    cleanup()
    page(ADMIN, { services: avec('infrastructure', { charges: async () => { throw new ApiError(502, 'OPS_ORCHESTRATEUR_INDISPONIBLE', 'x', 'cid-502') } }) })
    expect(await screen.findByText(/Orchestrateur injoignable/)).toBeTruthy()
  })

  it('historique paginé : page suivante', async () => {
    page(ADMIN)
    const historique = (await screen.findByText('Historique des opérations')).closest('section') as HTMLElement
    expect(await within(historique).findByText('Maintenance programmée de la messagerie')).toBeTruthy()
    expect(within(historique).getByText('Échouée')).toBeTruthy()
  })
})

describe('widgets de plateforme et supervision', () => {
  it('résumé de la supervision et charges en erreur, liens vers les écrans de l’application', async () => {
    rendrePage(<><WidgetSupervision /><WidgetChargesEnErreur /></>, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', permissions: ADMIN })
    const sup = await screen.findByRole('region', { name: 'Supervision de la plateforme' })
    expect(await within(sup).findByText('7 / 8')).toBeTruthy()
    expect(within(sup).getByRole('link', { name: 'Ouvrir la supervision' }).getAttribute('href')).toBe('/console/plateforme/supervision')
    const charges = screen.getByRole('region', { name: 'Charges en erreur' })
    expect((await within(charges).findByRole('link', { name: /pilotage/ })).getAttribute('href')).toBe('/console/plateforme/infrastructure?charge=pilotage')
    cleanup()
    rendrePage(<><WidgetSupervision /><WidgetChargesEnErreur /></>, {
      chemin: '/x', motif: '/x', permissions: ADMIN,
      services: avec('plateforme', { services: async () => { throw new ApiError(503, 'X', 'x', 'c') } }, simules({ orchestrateur: 'AUCUN' })),
    })
    expect(await screen.findByText('Exploitation non configurée')).toBeTruthy()
    expect(await screen.findAllByText('Le chargement a échoué.')).toBeTruthy()
  })
  it('écran « Supervision » : en-tête et section de supervision', async () => {
    rendrePage(<SupervisionPage />, { chemin: '/des/plateforme/supervision', motif: '/des/plateforme/supervision', permissions: ADMIN })
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Centre de contrôle')
    expect(await screen.findByRole('region', { name: 'État en temps réel' })).toBeTruthy()
  })

  it('courbes d’une charge : tendance CPU dans la table, fiche processeur et mémoire avec sa propre période', async () => {
    page(ADMIN)
    expect(await screen.findByRole('columnheader', { name: 'Tendance CPU' })).toBeTruthy()
    expect((await screen.findAllByRole('img', { name: /^Processeur par charge · gateway : de .+ % à .+ %$/ })).length).toBe(1)
    fireEvent.click(await screen.findByRole('button', { name: 'Courbes de gateway' }))
    const fiche = await screen.findByRole('dialog')
    expect(within(fiche).getByText('Courbes de la charge gateway')).toBeTruthy()
    expect(await within(fiche).findByRole('region', { name: 'Processeur' })).toBeTruthy()
    expect(within(fiche).getByRole('region', { name: 'Mémoire' })).toBeTruthy()
    fireEvent.click(within(fiche).getByRole('radio', { name: '24 heures' }))
    await waitFor(() => expect(within(fiche).getAllByText(/· 24 heures$/).length).toBe(2))
    fireEvent.click(within(fiche).getByRole('button', { name: 'Fermer' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('Kubernetes : pods et événements de la charge, nœuds prêts en anneau, jobs et cronjobs', async () => {
    page(ADMIN)
    expect(await screen.findByRole('img', { name: /^Nœuds prêts : 67 %/ })).toBeTruthy()
    expect(screen.getByText('2 sur 3 prêts')).toBeTruthy()
    expect(screen.getByText('non planifiable')).toBeTruthy()
    expect(await screen.findByText('jhipster-grafana-dashboard')).toBeTruthy()
    expect(screen.getByText('réussie')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Courbes de gateway' }))
    const fiche = await screen.findByRole('dialog')
    fireEvent.click(within(fiche).getByRole('tab', { name: 'Pods' }))
    expect((await within(fiche).findAllByText(/^gateway-/, { selector: '.gm-mono' })).length).toBeGreaterThan(0)
    fireEvent.click(within(fiche).getByRole('tab', { name: 'Événements' }))
    expect(await within(fiche).findByText('ScalingReplicaSet')).toBeTruthy()
  })

  it('Docker : fiche réduite aux courbes, ni nœuds ni tâches', async () => {
    page(ADMIN, { services: simules({ orchestrateur: 'DOCKER' }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Courbes de gateway' }))
    const fiche = await screen.findByRole('dialog')
    expect(within(fiche).queryByRole('tab', { name: 'Pods' })).toBeNull()
    expect(screen.queryByText('Nœuds du cluster')).toBeNull()
    expect(screen.queryByText('Tâches (jobs et cronjobs)')).toBeNull()
  })
})
