import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import LancerCertificationPage from './LancerCertificationPage'
import MesActionsCertificationPage from './MesActionsCertificationPage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import { PROFILS } from '../../../permissions'

// D-98 : « Lancer une certification » et « Mes actions de certification » sur les services simulés alignés sur
// MS-HOMOLOGATION (GET /parcours/mes-operations), puis le flux complet d'un type créé dans la plateforme jusqu'à la
// mise en production (statut de la liaison, jalons, institution ACTIF).

afterEach(cleanup)
const cleBrute = /\b(certif|nav|parcours)\.[a-zA-Z]/
const sous = (role: string) => servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-TEST', nom: 'Agent', profils: [role] } })
const fichier = (nom: string) => new File(['%PDF-1.7'], nom, { type: 'application/pdf' })

describe('D-98 : mes actions de certification', () => {
  it('opérations à faire de la personne, liens directs vers l’écran de l’opération, rôles et actions servis', async () => {
    rendrePage(<MesActionsCertificationPage />, { chemin: '/des/certification/mes-actions', motif: '/des/certification/mes-actions', session: sessionTest(PROFILS.ROLE_SNA, ['ROLE_SNA']) })
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Mes actions de certification')
    const tableau = await screen.findByRole('table')
    const liens = within(tableau).getAllByRole('link').map((a) => a.getAttribute('href') ?? '')
    expect(liens.some((h) => /^\/des\/parcours\/LIA-[^?]+\?onglet=(operations|decision|mise-en-service)&operation=/.test(h))).toBe(true)
    expect(within(tableau).getAllByText(/Responsable|Validateur/).length).toBeGreaterThan(0)
    expect(screen.getByRole('group', { name: 'Mes compteurs de certification' })).toBeTruthy()
    expect(document.body.textContent).not.toMatch(cleBrute)
    // Lancement réservé au catalogue des fonctionnalités : l'ingénieur Normes et Agréments ne lance pas.
    expect(screen.queryByRole('link', { name: 'Lancer une certification' })).toBeNull()
  })

  it('conducteur : liaisons suivies avec statut, avancement et jalons ; opérations à suivre', async () => {
    rendrePage(<MesActionsCertificationPage />, { chemin: '/des/certification/mes-actions?onglet=liaisons', motif: '/des/certification/mes-actions', session: sessionTest(PROFILS.ROLE_CONDUCTEUR_PROJET, ['ROLE_CONDUCTEUR_PROJET']) })
    expect((await screen.findAllByText('Vous conduisez cette liaison')).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('list', { name: 'Jalons' }).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/En production : (Attendu|Atteint le)/).length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('tab', { name: /À suivre/ }))
    expect(await screen.findByRole('columnheader', { name: 'Validateur' })).toBeTruthy()
  })
})

describe('D-98 : lancer une certification', () => {
  it('chef du DES : institution en homologation, type créé dans la plateforme, conducteur, puis dossier de la liaison', async () => {
    rendrePage(<LancerCertificationPage />, {
      chemin: '/des/certification/lancer', motif: '/des/certification/lancer', session: sessionTest(PROFILS.ROLE_CHEF_DES, ['ROLE_CHEF_DES']),
      autres: [{ motif: '/des/parcours/:id', element: <p>Dossier ouvert</p> }],
    })
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Lancer une certification')
    const select = await screen.findByLabelText('Institution')
    await waitFor(() => expect(within(select).getAllByRole('option').length).toBeGreaterThan(2))
    const option = within(select).getAllByRole('option').find((o) => o.textContent?.startsWith('Établissement de paiement A')) as HTMLOptionElement
    fireEvent.change(select, { target: { value: option.value } })
    expect(await screen.findByText(/Adhésion prononcée/)).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir la liaison' }))
    expect(await screen.findByText('Choisissez le type de projet.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('radio', { name: 'Agrégateur de paiement' }))
    const conducteur = screen.getByLabelText(/Conducteur de projet/)
    await waitFor(() => expect(within(conducteur).getAllByRole('option').length).toBeGreaterThan(1))
    fireEvent.change(conducteur, { target: { value: within(conducteur).getAllByRole('option')[1].getAttribute('value') } })
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la liaison' }))
    expect(await screen.findByText('Dossier ouvert')).toBeTruthy()
    expect(document.body.textContent).not.toMatch(cleBrute)
  })

  it('chargé de la DDRM : candidat à instruire, sans ouverture permise', async () => {
    rendrePage(<LancerCertificationPage />, { chemin: '/des/certification/lancer', motif: '/des/certification/lancer', session: sessionTest(PROFILS.ROLE_CHARGE_DDRM, ['ROLE_CHARGE_DDRM']) })
    const select = await screen.findByLabelText('Institution')
    await waitFor(() => expect(within(select).getAllByRole('option').length).toBeGreaterThan(2))
    const option = within(select).getAllByRole('option').find((o) => o.textContent?.startsWith('Banque de Libreville')) as HTMLOptionElement
    fireEvent.change(select, { target: { value: option.value } })
    expect(await screen.findByText(/L'adhésion n'est pas encore prononcée/)).toBeTruthy()
    expect(screen.getByRole('link', { name: "Instruire le dossier d'adhésion" }).getAttribute('href')).toMatch(/\/des\/institutions\/.+\?onglet=pieces$/)
    expect(screen.getByText("L'ouverture suit le prononcé de l'adhésion.")).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Enregistrer une nouvelle institution' })).toBeTruthy()
  })

  it('D-106 : ouvrir une liaison exige aussi la désignation du conducteur (bouton masqué, service simulé 403 comme MS-HOMOLOGATION)', async () => {
    await expect(sous('ROLE_CHARGE_DDRM').parcours.ouvrirLiaison({ institutionId: 'INST-0042', typeProjet: 'AGREGATEUR', chefDeProjetId: 'U-COND-1', banquePartenaireId: null }))
      .rejects.toMatchObject({ status: 403, code: 'HOM_PROJECT_DESIGNATION_FORBIDDEN' })
    // Profil composé avec HOM_LIAISON_OPEN sans HOM_PROJECT_DESIGNATE : l'écran explique au lieu d'offrir un geste refusé.
    const sansDesignation = PROFILS.ROLE_CHEF_DES.filter((p) => p !== 'HOM_PROJECT_DESIGNATE')
    rendrePage(<LancerCertificationPage />, { chemin: '/des/certification/lancer', motif: '/des/certification/lancer', session: sessionTest(sansDesignation, ['ROLE_CHEF_DES']) })
    const select = await screen.findByLabelText('Institution')
    await waitFor(() => expect(within(select).getAllByRole('option').length).toBeGreaterThan(2))
    const option = within(select).getAllByRole('option').find((o) => o.textContent?.startsWith('Établissement de paiement A')) as HTMLOptionElement
    fireEvent.change(select, { target: { value: option.value } })
    expect(await screen.findByText(/Adhésion prononcée/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ouvrir la liaison' })).toBeNull()
  })
})

describe('D-98 : démonstration simulée alignée, du lancement à la mise en production', () => {
  afterEach(() => ['gm.demoContinue', 'gm.demo.parcours', 'gm.demo.participant'].forEach((k) => localStorage.removeItem(k)))

  it('démonstration continue : chaque acteur trouve son opération dans « Mes actions », agit, et la liaison atteint EN_PRODUCTION', async () => {
    // Démonstration continue : chaque acteur ouvre sa session (nouvelle instance) sur l'état laissé par le précédent.
    localStorage.setItem('gm.demoContinue', '1')
    const ouverte = await sous('ROLE_CHEF_DES').parcours.ouvrirLiaison({ institutionId: 'INST-0042', typeProjet: 'AGREGATEUR', chefDeProjetId: 'U-COND-1', banquePartenaireId: null })
    const action = async (role: string, code: string) => {
      const a = (await sous(role).parcours.mesOperations()).aAgir.find((o) => o.liaisonId === ouverte.id && o.code === code)
      expect(a, `${role} ${code}`).toBeTruthy()
      expect(a!.lien).toContain(`operation=${encodeURIComponent(a!.operationId)}`)
      return { a: a!, s: sous(role) }
    }
    expect((await sous('ROLE_CHEF_DES').parcours.operations(ouverte.id)).map((o) => o.rang)).toEqual(['A1', 'A2', 'A3', 'A4', 'A5', 'A6', 'A7', 'A8'])
    expect((await sous('ROLE_CHEF_DES').participant.dossier('INST-0042')).institution.statut).toBe('EN_HOMOLOGATION')
    let x = await action('ROLE_CHARGE_DDRM', 'A1')
    expect(x.a).toMatchObject({ qualite: 'R', actionsPermises: expect.arrayContaining(['DEPOSER_PREUVE']) })
    await x.s.parcours.deposerPreuve(x.a.operationId, fichier('perimetre.pdf'), {})
    x = await action('ROLE_CONDUCTEUR_PROJET', 'A1')
    expect(x.a).toMatchObject({ qualite: 'V', etat: 'EN_VALIDATION', actionsPermises: expect.arrayContaining(['VALIDER', 'RENVOYER']) })
    await x.s.parcours.renvoyer(x.a.operationId, 'Calendrier à préciser')
    x = await action('ROLE_CHARGE_DDRM', 'A1')
    await x.s.parcours.deposerPreuve(x.a.operationId, fichier('perimetre-v2.pdf'), {})
    x = await action('ROLE_CONDUCTEUR_PROJET', 'A1')
    await x.s.parcours.validerOperation(x.a.operationId, 'assertion')
    x = await action('ROLE_SNA', 'A2')
    await x.s.parcours.deposerPreuve(x.a.operationId, fichier('essais.pdf'), {})
    x = await action('ROLE_CONDUCTEUR_PROJET', 'A2')
    await x.s.parcours.validerOperation(x.a.operationId, 'assertion')
    x = await action('ROLE_SNA', 'A3')
    expect(x.a.actionsPermises).toContain('CONSTATER_COUVERTURE')
    await x.s.parcours.constaterCouverture(ouverte.id)
    x = await action('ROLE_CONDUCTEUR_PROJET', 'A4')
    expect(x.a.lien).toContain('onglet=decision')
    await x.s.decision.etablirProcesVerbal(ouverte.id)
    x = await action('ROLE_CHEF_DES', 'A5')
    await x.s.decision.signerProcesVerbalGimac(ouverte.id, 'assertion')
    // Participant au portail : sa tâche mène à l'écran de signature du procès-verbal.
    const portail = servicesSimules({ latenceMs: 0 })
    const tache = (await portail.portailParcours.mesTaches()).taches.find((t) => t.liaisonId === ouverte.id)
    expect(tache).toMatchObject({ lien: `/portail/liaisons/${ouverte.id}/proces-verbal`, actionsPermises: ['SIGNER_PROCES_VERBAL'] })
    await portail.portailParcours.signerProcesVerbal(ouverte.id, 'assertion')
    expect((await sous('ROLE_CHEF_DES').parcours.dossier(ouverte.id)).statut).toBe('CERTIFIEE')
    x = await action('ROLE_PRODUCTION_MONETIQUE', 'A6')
    await x.s.decision.constituerDossierChangement(ouverte.id, fichier('dossier-changement.pdf'))
    x = await action('ROLE_PRODUCTION_MONETIQUE', 'A7')
    await x.s.decision.referencerFicheChangement(ouverte.id, 'CHG-2026-0420')
    x = await action('ROLE_RSSI', 'A7')
    expect(x.a).toMatchObject({ qualite: 'V', etat: 'EN_VALIDATION' })
    await x.s.decision.relireFicheChangement(ouverte.id)
    expect((await sous('ROLE_CHEF_DES').parcours.dossier(ouverte.id)).statut).toBe('APPROUVEE_SECURITE')
    x = await action('ROLE_PRODUCTION_MONETIQUE', 'A8')
    await x.s.parcours.deposerPreuve(x.a.operationId, fichier('bascule.pdf'), {})
    x = await action('ROLE_CONDUCTEUR_PROJET', 'A8')
    await x.s.parcours.validerOperation(x.a.operationId, 'assertion')
    const fin = sous('ROLE_CONDUCTEUR_PROJET')
    expect((await fin.parcours.dossier(ouverte.id)).statut).toBe('EN_PRODUCTION')
    expect((await fin.participant.dossier('INST-0042')).institution.statut).toBe('ACTIF')
    const suivie = (await fin.parcours.mesOperations()).liaisons.find((l) => l.liaisonId === ouverte.id)
    expect(suivie).toMatchObject({ conducteur: true, statut: 'EN_PRODUCTION', operationsFaites: 8, operationsTotal: 8 })
    expect(suivie!.jalons.map((j) => [j.jalon, j.atteint])).toEqual([['APTE_CERTIFICATION', true], ['CERTIFIEE', true], ['APPROUVEE_SECURITE', true], ['EN_PRODUCTION', true]])
  })
})
