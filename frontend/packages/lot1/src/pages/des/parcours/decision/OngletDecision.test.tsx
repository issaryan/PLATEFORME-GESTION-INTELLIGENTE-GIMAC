import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useParams } from 'react-router'
import axe from 'axe-core'
import { ApiError, type Langue } from '@gimac/socle'
import { rendrePage, sessionTest } from '../../../../test/rendu'
import { servicesSimules } from '../../../../services/simule'
import type { Services } from '../../../../services/ports'
import { useDossierLiaison } from '../../../../hooks/parcours'
import { PROFILS } from '../../../../permissions'
import { OngletDecision } from '../onglets/OngletDecision'

// Onglet « Décision » (D-56) monté sur un banc d'essai : dossier chargé par son hook, profils du jeton,
// gestes permis par le service ET par la matrice des rôles, dialogues d'ExecuteurActions.

afterEach(cleanup)

function Banc() {
  const { id = '' } = useParams()
  const q = useDossierLiaison(id)
  return q.data ? <OngletDecision dossier={q.data} /> : null
}

const simules = (): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, reauth: { assertion: () => Promise.resolve('YXNzZXJ0aW9u') } }
}
const ouvrir = (liaison: string, role?: string, o: { services?: Services; langue?: Langue; theme?: 'light' | 'dark' } = {}) =>
  rendrePage(<Banc />, {
    chemin: `/des/parcours/${liaison}?onglet=decision`, motif: '/des/parcours/:id', services: o.services ?? simules(), langue: o.langue, theme: o.theme,
    session: role ? sessionTest(PROFILS[role], [role]) : undefined,
  })
const carte = (titre: RegExp) => screen.getByRole('heading', { name: titre }).closest('.gm-card') as HTMLElement

describe('Verrou 1 : procès-verbal de certification', () => {
  it('chef du DES : PV établi à signer, signature GIMAC par clé de sécurité, puis attente du participant', async () => {
    ouvrir('LIA-2026-027', 'ROLE_CHEF_DES')
    expect(await screen.findByText('Établi, à signer')).toBeTruthy()
    expect(screen.getByText('Attendue du chef du DES')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Signer le procès-verbal pour le GIMAC' }))
    const d = await screen.findByRole('dialog')
    expect(d.textContent).toContain('LIA-2026-027')
    expect(d.textContent).toContain('La liaison sera certifiée dès la signature du participant')
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await within(d).findByRole('button', { name: 'Continuer' })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Signé par le GIMAC')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Signer le procès-verbal pour le GIMAC' })).toBeNull()
    expect(screen.getByText('Attendue sur le portail du participant')).toBeTruthy()
  })

  it('conducteur : ni signature GIMAC sur un PV établi, ni verrou 2 ouvert', async () => {
    ouvrir('LIA-2026-027', 'ROLE_CONDUCTEUR_PROJET')
    await screen.findByText('Établi, à signer')
    expect(screen.queryByRole('button', { name: 'Signer le procès-verbal pour le GIMAC' })).toBeNull()
    expect(screen.getByText('Verrou 2 en attente du procès-verbal')).toBeTruthy()
  })

  it('conducteur : couverture complète constatée, établissement du PV avec son empreinte', async () => {
    ouvrir('LIA-2026-025', 'ROLE_CONDUCTEUR_PROJET')
    expect(await screen.findByText(/Complète : 3 services sur 3/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Établir le procès-verbal' }))
    const d = await screen.findByRole('alertdialog')
    expect(d.textContent).toContain('figé avec son empreinte')
    fireEvent.click(within(d).getByRole('button', { name: 'Établir le procès-verbal' }))
    expect(await screen.findByText('Établi, à signer')).toBeTruthy()
    expect(carte(/Verrou 1/).querySelector('.gm-chip[title]')).not.toBeNull()
    expect(await screen.findByText('Établir le procès-verbal : effectué.')).toBeTruthy()
  })

  it('chef du DES : ne peut pas établir le PV, même quand le service le permet', async () => {
    ouvrir('LIA-2026-025', 'ROLE_CHEF_DES')
    await screen.findByText(/Complète : 3 services sur 3/)
    expect(screen.queryByRole('button', { name: 'Établir le procès-verbal' })).toBeNull()
  })

  it('couverture incomplète : établissement bloqué et expliqué (75 %, le PV exige 100 %)', async () => {
    const services = simules()
    await services.parcours.constaterCouverture('LIA-2026-022')
    ouvrir('LIA-2026-022', 'ROLE_CONDUCTEUR_PROJET', { services })
    expect(await screen.findByText('Procès-verbal bloqué par la couverture')).toBeTruthy()
    expect(screen.getByText(/La couverture de certification est de 75 % \(3 services certifiés sur 4\)/)).toBeTruthy()
    const bouton = screen.getByRole('button', { name: /Établir le procès-verbal/ }) as HTMLButtonElement
    expect(bouton.disabled || bouton.getAttribute('aria-disabled') === 'true').toBe(true)
  })

  it('avant le constat de la couverture : attente expliquée, aucune action', async () => {
    ouvrir('LIA-2026-022', 'ROLE_CONDUCTEUR_PROJET')
    expect(await screen.findByText(/après le constat de la couverture du périmètre/)).toBeTruthy()
    expect(screen.queryByTestId('actions-pv')).toBeNull()
  })

  it('refus du service lors de la signature : message traduit et identifiant de corrélation dans le dialogue', async () => {
    const s = simules()
    const services: Services = { ...s, decision: { ...s.decision, signerProcesVerbalGimac: () => Promise.reject(new ApiError(409, 'CER_PV_NOT_SIGNABLE', 'x', 'cid-409')) } }
    ouvrir('LIA-2026-027', 'ROLE_CHEF_DES', { services })
    fireEvent.click(await screen.findByRole('button', { name: 'Signer le procès-verbal pour le GIMAC' }))
    const d = await screen.findByRole('dialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await waitFor(() => expect(d.textContent).toContain('Le procès-verbal n\'attend pas votre signature. (cid-409)'))
    expect(d.textContent).toContain('cid-409')
  })
})

describe('Visas optionnels', () => {
  it('absents quand ils ne sont pas activés', async () => {
    ouvrir('LIA-2026-012', 'ROLE_DG')
    await screen.findByText('Changement approuvé par la sécurité')
    expect(screen.queryByText('Visas de la conformité et de la direction générale')).toBeNull()
  })

  it('conformité : visa favorable motivé et signé ; le DG ne voit que le sien', async () => {
    ouvrir('LIA-2026-019', 'ROLE_CONFORMITE')
    const zone = await screen.findByRole('region', { name: 'Visa de la conformité' })
    const dg = screen.getByRole('region', { name: 'Visa de la direction générale' })
    expect(within(dg).queryByRole('button')).toBeNull()
    fireEvent.click(within(zone).getByRole('button', { name: 'Viser favorablement' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByRole('textbox'), { target: { value: 'Dossier conforme aux exigences réglementaires.' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    fireEvent.click(await within(d).findByRole('button', { name: 'Continuer' }))
    const apres = await screen.findByRole('region', { name: 'Visa de la conformité' })
    await waitFor(() => expect(apres.textContent).toContain('Favorable'))
    expect(apres.textContent).toContain('Dossier conforme aux exigences réglementaires.')
  })

  it('direction générale : visa défavorable, motif obligatoire', async () => {
    ouvrir('LIA-2026-019', 'ROLE_DG')
    const zone = await screen.findByRole('region', { name: 'Visa de la direction générale' })
    fireEvent.click(within(zone).getByRole('button', { name: 'Viser défavorablement' }))
    const d = await screen.findByRole('dialog')
    const cle = within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }) as HTMLButtonElement
    expect(cle.disabled || cle.getAttribute('aria-disabled') === 'true').toBe(true)
    fireEvent.change(within(d).getByRole('textbox'), { target: { value: 'Risque opérationnel non couvert à ce jour.' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    fireEvent.click(await within(d).findByRole('button', { name: 'Continuer' }))
    await waitFor(() => expect(screen.getByRole('region', { name: 'Visa de la direction générale' }).textContent).toContain('Défavorable'))
  })
})

describe('Verrou 2 : approbation de la sécurité', () => {
  it('RSSI : attestation déclarée conforme puis archivage ; l\'approbation attend les visas', async () => {
    ouvrir('LIA-2026-019', 'ROLE_RSSI')
    expect(await screen.findByText('1 attestation fournie attend votre vérification.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Déclarer conforme' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Déclarer conforme' }))
    expect(await screen.findByText('Conforme')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Archiver les attestations' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Archiver les attestations' }))
    expect(await screen.findByText('Vérifiée et archivée')).toBeTruthy()
  })

  it('RSSI : rejet motivé qui redemande l\'attestation au participant', async () => {
    ouvrir('LIA-2026-019', 'ROLE_RSSI')
    fireEvent.click(await screen.findByRole('button', { name: 'Rejeter' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.change(within(d).getByRole('textbox'), { target: { value: 'Attestation expirée depuis le trimestre dernier.' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Rejeter' }))
    expect(await screen.findByText('Rejetée, à fournir de nouveau')).toBeTruthy()
    expect(screen.getByText('Motif : Attestation expirée depuis le trimestre dernier.')).toBeTruthy()
  })

  it('production monétique : pas de vérification d\'attestation ; référence de fiche refusée puis acceptée', async () => {
    ouvrir('LIA-2026-019', 'ROLE_PRODUCTION_MONETIQUE')
    await screen.findByText('Dossier de changement (rang 20)')
    expect(screen.queryByRole('button', { name: 'Déclarer conforme' })).toBeNull()
    fireEvent.change(screen.getByRole('textbox', { name: /Référence de la fiche/ }), { target: { value: 'chg412' } })
    fireEvent.click(screen.getByRole('button', { name: 'Référencer la fiche de changement' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Référencer la fiche de changement' }))
    await waitFor(() => expect(d.textContent).toContain('Référence de fiche de changement invalide'))
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    fireEvent.change(screen.getByRole('textbox', { name: /Référence de la fiche/ }), { target: { value: 'CHG-2026-412' } })
    fireEvent.click(screen.getByRole('button', { name: 'Référencer la fiche de changement' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Référencer la fiche de changement' }))
    expect(await screen.findByText('CHG-2026-412')).toBeTruthy()
    expect(screen.getByText('En examen')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Relire la fiche de changement' }))
    expect(await screen.findByText(/une fois les attestations vérifiées et archivées/)).toBeTruthy()
  })

  it('production monétique : dossier de changement constitué après le PV signé des deux parties', async () => {
    const services = simules()
    await services.decision.signerProcesVerbalGimac('LIA-2026-027', 'YQ==')
    await services.portailParcours.signerProcesVerbal('LIA-2026-027', 'YQ==')
    ouvrir('LIA-2026-027', 'ROLE_PRODUCTION_MONETIQUE', { services })
    expect(await screen.findByText('Liaison certifiée')).toBeTruthy()
    const depot = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(depot, { target: { files: [new File(['%PDF'], 'dossier-changement.pdf', { type: 'application/pdf' })] } })
    fireEvent.click(await screen.findByRole('button', { name: 'Constituer le dossier de changement' }))
    const d = await screen.findByRole('alertdialog')
    expect(d.textContent).toContain('dossier-changement.pdf')
    fireEvent.click(within(d).getByRole('button', { name: 'Constituer le dossier de changement' }))
    expect(await screen.findByText('Dossier constitué')).toBeTruthy()
  })

  it('liaison approuvée : toutes les étapes franchies, approbation datée', async () => {
    const { container } = ouvrir('LIA-2026-012', 'ROLE_CHEF_DES')
    expect(await screen.findByText('Changement approuvé par la sécurité')).toBeTruthy()
    expect(container.querySelectorAll('.gm-states .gm-state-done').length).toBeGreaterThanOrEqual(5)
    expect(carte(/Verrou 2/).textContent).toContain('Approuvée')
  })

  it('erreur de lecture : identifiant de corrélation et réessai', async () => {
    const s = simules()
    const services: Services = { ...s, decision: { ...s.decision, approbation: () => Promise.reject(new ApiError(503, 'CER_INDISPONIBLE', 'Indisponible', 'cid-503')) } }
    ouvrir('LIA-2026-019', 'ROLE_RSSI', { services })
    const alerte = await screen.findByRole('alert')
    expect(alerte.textContent).toContain('cid-503')
    expect(within(alerte).getByRole('button', { name: 'Réessayer' })).toBeTruthy()
  })
})

describe('Langues, thème et accessibilité', () => {
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    ouvrir('LIA-2026-019', 'ROLE_RSSI', { langue })
    await screen.findByText(langue === 'en' ? 'Lock 1: certification report' : 'Cerrojo 1: acta de certificación')
    expect(document.body.textContent).not.toMatch(/\b(decision|parcours|miseEnService)\.[a-zA-Z]/)
  })

  it.each(['light', 'dark'] as const)('axe sans violation (thème %s)', async (theme) => {
    const { container } = ouvrir('LIA-2026-019', 'ROLE_RSSI', { theme })
    await screen.findByText('Verrou 1 : procès-verbal de certification')
    const res = await axe.run(container, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] }, rules: { 'color-contrast': { enabled: false }, region: { enabled: false } } })
    expect(res.violations.map((v) => `${v.id}: ${v.nodes[0]?.html.slice(0, 120)}`)).toEqual([])
  })
})
