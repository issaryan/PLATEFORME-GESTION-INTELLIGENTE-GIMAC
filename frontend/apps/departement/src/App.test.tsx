import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import i18next from 'i18next'
import { MOT_DE_PASSE_DEMO, initI18n } from '@gimac/socle'
import { FONCTIONNALITES_DEMO, catalogues } from '@gimac/lot1'
import { catalogueCompte } from '@gimac/compte'
import { cataloguesAdministration } from '@gimac/administration'
import { cataloguesExploitation } from '@gimac/exploitation'
import App from './App'
import { CLE_PROFIL_DEMO, PROFILS_DEMO, roleDemo, sessionDemo } from './profils'

// Application du département : session de démonstration par profil (D-57, D-58), navigation élaguée
// selon les permissions, premier accès, session réelle par la passerelle, déconnexion.
beforeAll(() => {
  if (!i18next.isInitialized) initI18n([...catalogues, catalogueCompte, ...cataloguesAdministration, ...cataloguesExploitation], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
// Session de démonstration ouverte (connexion simulée, D-65) ; retirée par les scénarios de connexion.
beforeEach(() => sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: 'test' })))
afterEach(() => {
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})
/** Canal poussé (SSE) : absent de jsdom, simulé sans événement. */
class FluxMuet {
  onopen: (() => void) | null = null
  onerror: (() => void) | null = null
  onmessage: (() => void) | null = null
  addEventListener() {}
  removeEventListener() {}
  close() {}
}
const monter = (chemin = '/') => render(<MemoryRouter initialEntries={[chemin]}><App /></MemoryRouter>)
const navigation = () => screen.getByRole('navigation', { name: 'Navigation principale' })

describe('profils de démonstration', () => {
  it('chefs du DES et de la DDRM : ouverture et désignation ; ingénieur : ni l’une ni l’autre ; conducteur : lecture', () => {
    // CR v1.7 (D-105) : l'ouverture d'un projet revient à la DDRM ; le chef du DES désigne les ressources.
    expect(PROFILS_DEMO.ROLE_CHEF_DDRM.permissions).toEqual(expect.arrayContaining(['HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']))
    expect(PROFILS_DEMO.ROLE_CHEF_DES.permissions).toContain('HOM_PROJECT_DESIGNATE')
    expect(PROFILS_DEMO.ROLE_CHEF_DES.permissions).not.toContain('HOM_LIAISON_OPEN')
    expect(PROFILS_DEMO.ROLE_INGENIEUR_HOMOLOGATION.permissions).not.toContain('HOM_LIAISON_OPEN')
    expect(PROFILS_DEMO.ROLE_CONDUCTEUR_PROJET.permissions).not.toContain('HOM_PROJECT_DESIGNATE')
    expect(PROFILS_DEMO.ROLE_CONDUCTEUR_PROJET.permissions).toContain('HOM_LIAISON_READ')
    expect(PROFILS_DEMO.ROLE_CHARGE_DDRM.permissions).not.toContain('PAR_TRANSITION_APPROVE')
  })
  it('QUO-01 : comptes de démonstration du service RH, chacun avec son profil de l’organisation simulée', async () => {
    const { COMPTES_DEMO } = await import('./connexionDemo')
    const gestionnaire = COMPTES_DEMO.find((c) => c.identifiant === 't.bekolo')!
    const cheffe = COMPTES_DEMO.find((c) => c.identifiant === 's.ateba')!
    expect(gestionnaire.session).toMatchObject({ sujet: 'u-rh', nom: 'Thierry Bekolo Ngoa', roles: ['ROLE_COLLABORATEUR'] })
    expect(cheffe.session).toMatchObject({ sujet: 'u-rh-chef', nom: 'Solange Ateba Mvondo', roles: ['ROLE_RESPONSABLE_SERVICE'] })
    // La session ouverte par la connexion simulée garde le compte (et non le premier porteur du profil).
    sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: 't.bekolo', role: gestionnaire.role }))
    expect(sessionDemo()).toMatchObject({ sujet: 'u-rh', roles: ['ROLE_COLLABORATEUR'] })
  })
  it('profil choisi par la préférence locale, chef de la DDRM à défaut', () => {
    expect(roleDemo()).toBe('ROLE_CHEF_DDRM')
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_CONDUCTEUR_PROJET')
    expect(sessionDemo()).toMatchObject({ sujet: 'U-201', nom: 'Aïcha Mahamat Nour', roles: ['ROLE_CONDUCTEUR_PROJET'], doubleFacteur: true })
    expect(roleDemo('ROLE_INCONNU')).toBe('ROLE_CHEF_DDRM')
    // Super administrateur (D-69) : même identité que dans la console.
    expect(sessionDemo('ROLE_SUPER_ADMIN')).toMatchObject({ sujet: 'u-super-admin', nom: 'Serge Mouelle Kombi' })
  })
})

describe('Mon activité (D-76)', () => {
  it('profils de démonstration : collaborateur et responsable de service, PIL_* selon le contrat', () => {
    expect(PROFILS_DEMO.ROLE_COLLABORATEUR).toMatchObject({ sujet: 'u-collab', nom: 'Nadège Mvondo Assiga' })
    expect([...PROFILS_DEMO.ROLE_COLLABORATEUR.permissions].sort()).toEqual(['COL_MESSAGE_READ', 'COL_MESSAGE_SEND', 'NOT_INBOX_READ', 'PIL_ACTIVITY_OWN'])
    expect(PROFILS_DEMO.ROLE_RESPONSABLE_SERVICE.permissions).toEqual(expect.arrayContaining(['PIL_ACTIVITY_OWN', 'PIL_TEAM_READ', 'PIL_TASK_ASSIGN']))
    for (const r of ['ROLE_CHEF_DES', 'ROLE_CHEF_DDRM', 'ROLE_DG', 'ROLE_RSSI', 'ROLE_CONFORMITE']) expect(PROFILS_DEMO[r].permissions).toContain('PIL_TEAM_READ')
    for (const p of Object.values(PROFILS_DEMO)) expect(p.permissions).toEqual(expect.arrayContaining(['PIL_ACTIVITY_OWN', 'NOT_INBOX_READ']))
    expect(PROFILS_DEMO.ROLE_SNA.permissions).not.toContain('PIL_TEAM_READ')
  })
  it('collaborateur : groupe « Mon activité » sans « Mon équipe » ni registre ; « Ma journée » en tête du tableau de bord', async () => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_COLLABORATEUR')
    monter()
    expect(await screen.findByRole('region', { name: 'Ma journée' }, { timeout: 8000 })).toBeTruthy()
    for (const n of ['Ma journée', 'Agenda', 'Tâches', 'Rapports']) expect(within(navigation()).getByRole('link', { name: n })).toBeTruthy()
    expect(within(navigation()).queryByRole('link', { name: 'Mon équipe' })).toBeNull()
    expect(within(navigation()).queryByRole('link', { name: 'Institutions membres' })).toBeNull()
  })
  it('écran réservé : « Mon équipe » refusé au collaborateur', async () => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_COLLABORATEUR')
    monter('/des/equipe')
    expect(await screen.findByText('Accès refusé', {}, { timeout: 8000 })).toBeTruthy()
  })
})

describe('Astreintes et monitoring (D-114, D-116)', () => {
  it('production monétique : « Aujourd’hui », programme, rapports et activité ; supervision et relève hors navigation ; chef de la DDRM : refusé', async () => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_PRODUCTION_MONETIQUE')
    monter('/des/exploitation/aujourdhui')
    expect(await screen.findByRole('heading', { level: 1, name: 'Aujourd’hui' }, { timeout: 8000 })).toBeTruthy()
    for (const n of ['Aujourd’hui', 'Programme d’astreinte', 'Rapports de monitoring', 'Activité de l’équipe']) expect(within(navigation()).getByRole('link', { name: n })).toBeTruthy()
    for (const n of ['Liaisons et alertes', 'Vacation et relève']) expect(within(navigation()).queryByRole('link', { name: n })).toBeNull()
    cleanup()
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_PRODUCTION_MONETIQUE')
    monter('/des/exploitation/supervision')
    expect(await screen.findByText('Accès refusé', {}, { timeout: 8000 })).toBeTruthy()
    cleanup()
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_CHEF_DDRM')
    monter('/des/exploitation/astreintes')
    expect(await screen.findByText('Accès refusé', {}, { timeout: 8000 })).toBeTruthy()
  })
})

describe('Application du département', () => {
  it('chef de la DDRM (défaut) : tableau de bord, « Mon travail », registre, messagerie et Mon compte à son nom', async () => {
    monter()
    expect(await screen.findByRole('heading', { level: 1, name: 'Tableau de bord' }, { timeout: 8000 })).toBeTruthy()
    expect(await screen.findByRole('region', { name: 'Mon travail' })).toBeTruthy()
    // Messages non lus en pastille sur l'entrée « Messagerie » (D-64).
    expect(await screen.findByLabelText(/non lus/)).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toBeTruthy()
    for (const n of ['Institutions membres', 'Liaisons et parcours', 'Mon compte', 'Se déconnecter']) expect(screen.getAllByRole('link', { name: n }).length).toBeGreaterThan(0)
    expect(document.querySelector('.gm-footer')).toBeTruthy()
    fireEvent.click(screen.getByRole('link', { name: 'Mon compte' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon compte' })).toBeTruthy()
    expect(await screen.findByText('Bertrand')).toBeTruthy()
  })
  it('conducteur de projet : « Mes projets », sans registre des institutions en écriture', async () => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_CONDUCTEUR_PROJET')
    monter('/des/liaisons')
    expect(await screen.findByRole('switch', { name: 'Mes projets' }, { timeout: 8000 })).toBeTruthy()
    fireEvent.click(screen.getAllByRole('link', { name: 'Tableau de bord' })[0])
    expect(await screen.findByRole('heading', { name: /^Mes projets \(/ })).toBeTruthy()
  })
  it.each([
    ['/des/institutions', 'Institutions membres'],
    ['/des/institutions/INST-0105', 'Mobile Money Sanaga'],
    // Ancienne adresse d'une liaison : renvoyée vers son dossier de parcours.
    ['/des/liaisons/LIA-2026-030', 'Établissement de paiement A'],
    ['/des/liaisons/LIA-2025-102/mise-en-service', 'Banque B'],
    ['/des/liaisons/LIA-2026-017/operations/OPI-017-3/cloture', /OP-E04/],
    ['/des/referentiels/operations', 'Référentiels d\'opérations'],
    ['/des/messagerie?conversation=CONV-0001', 'Messagerie'],
    // Processus d'unité (D-87) : catalogue et modélisateur d'une version publiée.
    ['/des/processus', 'Processus de l’unité'],
  ])('route %s : écran chargé à la demande', async (chemin, titre) => {
    monter(chemin)
    expect(await screen.findByRole('heading', { level: 1, name: titre }, { timeout: 8000 })).toBeTruthy()
  })
  it.each([
    ['/des/processus/INCONNU', 'Le processus n’a pas pu être lu.'],
    ['/des/processus/instances/INCONNUE', 'Demande introuvable'],
  ])('processus d’unité (D-87) : %s chargé à la demande', async (chemin, message) => {
    monter(chemin)
    expect(await screen.findByText(message, {}, { timeout: 8000 })).toBeTruthy()
  })
  it('administration (D-69) : le RSSI gère les comptes dans le département, sans quitter son application', async () => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_RSSI')
    monter('/des/tableau-de-bord')
    // Tableau de bord : indicateurs des comptes et derniers actes, plus de listes de liens (D-69).
    expect(await screen.findByRole('region', { name: 'Gestion des utilisateurs' }, { timeout: 8000 })).toBeTruthy()
    expect(await screen.findByText('Sans second facteur')).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Derniers actes d’administration' })).toBeTruthy()
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
    const lien = (await screen.findAllByRole('link', { name: 'Comptes utilisateurs' }, { timeout: 8000 }))[0]
    expect(lien.getAttribute('href')).toBe('/des/administration/comptes')
    fireEvent.click(lien)
    expect(await screen.findByRole('heading', { level: 1, name: 'Comptes' }, { timeout: 8000 })).toBeTruthy()
    // Toujours l'application du département : sa navigation reste en place.
    expect(screen.getAllByRole('link', { name: 'Liaisons et parcours' }).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: /Console de la plateforme/ }).getAttribute('href')).toBe('http://localhost:4202/console/tableau-de-bord')
  })
  it.each([
    ['/des/administration/comptes/nouveau', 'Comptes'],
    ['/des/administration/comptes/u-sna', /./],
    ['/des/administration/journal', 'Journal d’administration'],
    ['/des/administration/profils', 'Profils et permissions'],
    ['/des/administration/politique-securite', 'Politique de sécurité'],
    ['/des/administration/organigramme', 'Organigramme'],
    ['/des/administration/demandes', /./],
    ['/des/administration/matrice-pouvoirs', /./],
    ['/des/administration/organisation', /./],
    ['/des/plateforme/configuration', /./],
    ['/des/plateforme/journalisation', /./],
    ['/des/plateforme/courriels', /./],
  ])('administration, super administrateur : %s', async (chemin, titre) => {
    localStorage.setItem(CLE_PROFIL_DEMO, 'ROLE_SUPER_ADMIN')
    monter(chemin)
    expect(await screen.findByRole('heading', { level: 1, name: titre }, { timeout: 8000 })).toBeTruthy()
  })
  it('administration : refusée sans la permission (chef de la DDRM)', async () => {
    monter('/des/administration/comptes')
    expect(await screen.findByText('Accès refusé', { exact: false }, { timeout: 8000 }).catch(() => screen.findByRole('heading', { name: /refus|autoris/i }))).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Comptes utilisateurs' })).toBeNull()
  })
  it('adresse inconnue : retour au tableau de bord', async () => {
    monter('/des/inconnue')
    expect(await screen.findByRole('heading', { level: 1, name: 'Tableau de bord' }, { timeout: 8000 })).toBeTruthy()
    cleanup()
    monter('/ailleurs')
    expect(await screen.findByRole('heading', { level: 1, name: 'Tableau de bord' }, { timeout: 8000 })).toBeTruthy()
  })
  it('premier accès : l’assistant remplace tous les écrans, sans navigation', async () => {
    localStorage.setItem('gm.premierAccesDemo', '1')
    monter('/des/institutions')
    expect(await screen.findByRole('heading', { name: 'Premier accès à la plateforme' }, { timeout: 8000 })).toBeTruthy()
    expect(navigation().querySelectorAll('a[href="/des/institutions"]')).toHaveLength(0)
  })
  it('services réels : session de la passerelle, Mon compte de la couche des', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    vi.stubGlobal('EventSource', FluxMuet)
    const reponse = (corps: unknown, status = 200) => new Response(JSON.stringify(corps), { status, headers: { 'Content-Type': 'application/json' } })
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (u) => {
      const chemin = new URL(String(u)).pathname
      if (chemin === '/api/session') return reponse({ sujet: 'u-7', nom: 'Agent SNA', identifiant: 'sna', roles: ['ROLE_SNA'], permissions: ['HOM_LIAISON_LIST'], institutionId: '', acr: '2', doubleFacteur: true })
      if (chemin === '/bff/des/iam/moi') return reponse({ compte: { id: 'u-7', nomAffiche: 'Agent SNA', profils: ['ROLE_SNA'], actionsRequises: [], profilComplet: true }, clesSecurite: 1, sessions: 1, etapesRestantes: [] })
      if (chemin.endsWith('/liaisons')) return reponse({ contenu: [], total: 0 })
      // Catalogue des fonctionnalités servi par MS-IAM (D-94) : barre latérale et gardes en dérivent.
      if (chemin === '/bff/des/iam/configuration/fonctionnalites') return servi ? reponse(FONCTIONNALITES_DEMO) : reponse({ code: 'IAM_INDISPONIBLE' }, 503)
      return reponse({}, 404)
    })
    let servi = true
    monter('/des/liaisons')
    expect(await screen.findByRole('heading', { level: 1, name: 'Liaisons et parcours' }, { timeout: 8000 })).toBeTruthy()
    await waitFor(() => expect(spy.mock.calls.map(([u]) => new URL(String(u)).pathname)).toContain('/bff/des/homologation/liaisons'))
    expect(spy.mock.calls.map(([u]) => new URL(String(u)).pathname)).toContain('/bff/des/iam/moi')
    expect(screen.getByRole('link', { name: 'Liaisons et parcours' })).toBeTruthy()
    // Catalogue non servi : repli prudent, l'accueil seul et un message clair.
    cleanup()
    servi = false
    monter('/des/liaisons')
    expect(await screen.findByText('Catalogue des fonctionnalités indisponible', {}, { timeout: 8000 })).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Liaisons et parcours' })).toBeNull()
    expect(screen.getByRole('link', { name: 'Tableau de bord' })).toBeTruthy()
  })
  it('services réels par défaut : sans VITE_SERVICES, la session vient de la passerelle et aucun profil de démonstration ne sert', async () => {
    vi.stubEnv('VITE_SERVICES', '')
    vi.stubGlobal('EventSource', FluxMuet)
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ code: 'CNX_SESSION_EXPIREE' }), { status: 401, headers: { 'Content-Type': 'application/json' } }))
    Object.defineProperty(window, 'location', { value: { ...window.location, assign: vi.fn() }, configurable: true })
    monter('/des/tableau-de-bord')
    await waitFor(() => expect(spy.mock.calls.map(([u]) => new URL(String(u)).pathname)).toContain('/api/session'))
    // La session de démonstration de l'onglet (gm.sessionDemo) est ignorée : aucun écran n'est servi sans la passerelle.
    expect(screen.queryByRole('heading', { level: 1, name: 'Tableau de bord' })).toBeNull()
  })
  it('services réels : sans session, retour à l’écran de connexion de la plateforme (D-65)', async () => {
    vi.stubEnv('VITE_SERVICES', 'http')
    vi.stubGlobal('EventSource', FluxMuet)
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ code: 'CNX_SESSION_EXPIREE' }), { status: 401, headers: { 'Content-Type': 'application/json' } }))
    monter('/des/tableau-de-bord')
    expect(await screen.findByText(/session a expiré/, {}, { timeout: 8000 })).toBeTruthy()
    await waitFor(() => expect(assign).toHaveBeenCalledWith(expect.stringMatching(/^\/connexion(\?|$)/)))
    expect(assign.mock.calls.flat().join(' ')).not.toContain('oauth2')
    cleanup()
    monter('/connexion')
    expect(await screen.findByRole('button', { name: 'Se connecter' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Mot de passe oublié ?' })).toBeTruthy()
  })
  it('services simulés sans session : écran de connexion ; connexion d’un profil de démonstration', async () => {
    sessionStorage.clear()
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    monter('/des/liaisons')
    expect(await screen.findByRole('button', { name: 'Se connecter' }, { timeout: 8000 })).toBeTruthy()
    fireEvent.change(document.querySelector('input[name="identifiant"]')!, { target: { value: PROFILS_DEMO.ROLE_CHEF_DES.identifiant } })
    fireEvent.change(document.querySelector('input[name="motDePasse"]')!, { target: { value: MOT_DE_PASSE_DEMO } })
    fireEvent.click(screen.getByRole('button', { name: 'Se connecter' }))
    fireEvent.change(await screen.findByLabelText(/^Code à six chiffres/, {}, { timeout: 8000 }), { target: { value: '000000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Vérifier le code' }))
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/des/liaisons'), { timeout: 8000 })
    expect(roleDemo()).toBe('ROLE_CHEF_DES')
  })
  it('déconnexion : la passerelle clôt la session, retour à la connexion', async () => {
    const assign = vi.fn()
    Object.defineProperty(window, 'location', { value: { ...window.location, assign }, configurable: true })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response(JSON.stringify({ logoutUrl: '/fin' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    monter('/des/deconnexion')
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/connexion?motif=deconnexion'), { timeout: 8000 })
  })
})
