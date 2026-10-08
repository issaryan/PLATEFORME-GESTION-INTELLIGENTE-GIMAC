import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import TableauDeBordPage from './pages/des/TableauDeBordPage'
import { rendrePage, sessionTest } from './test/rendu'
import { urlConsole, versConsole } from './navigation'
import { ECRANS_DES, navigationDepuis } from './fonctionnalites'
import { FONCTIONNALITES_INITIALES } from './services/simule/fonctionnalites'

const tr = { t: (k: string) => k, existe: () => true }
const NAVIGATION_DES = navigationDepuis(FONCTIONNALITES_INITIALES, 'des', ECRANS_DES, tr, 'nav.groupe.')
import { PROFILS } from './permissions'
import { ExtensionsTableauDeBordProvider } from './pages/des/tableau/extensions'

// Administration des comptes dans l'application du département (D-62, D-69) : le RSSI et le super
// administrateur (IAM_ACCOUNT_READ) y gèrent les comptes sous /des/administration, menés par le groupe
// « Administration » de la navigation et par la section de leur tableau de bord ; la console de la
// plateforme (supervision, configuration) reste un lien externe.
afterEach(cleanup)

const IAM_SANS_PRIVILEGE = ['IAM_ACCOUNT_READ', 'IAM_ACCOUNT_CREATE', 'IAM_ACCOUNT_EDIT', 'IAM_ACCOUNT_ROLES', 'IAM_ACCOUNT_STATUS', 'IAM_ACCOUNT_DELETE', 'IAM_ACCOUNT_SECURITY', 'IAM_JOURNAL_READ', 'IAM_PROFILE_EDIT']

describe('profil du RSSI (reflet de generer-realm-gimac.py)', () => {
  it('administration des comptes sans octroi privilégié, supervision et configuration en lecture, politique et messagerie', () => {
    const rssi = PROFILS.ROLE_RSSI
    for (const p of [...IAM_SANS_PRIVILEGE, 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_SECURITY_POLICY_EDIT', 'PLATFORM_MESSAGING_EDIT',
      'CER_QUITUS_SECURITE_SIGN', 'CER_SECURITY_VERIFY', 'HOM_EVIDENCE_SUBMIT']) {
      expect(rssi).toContain(p)
    }
    expect(rssi).not.toContain('IAM_ROLE_PRIVILEGED_GRANT')
    expect(rssi).not.toContain('PLATFORM_CONFIG_EDIT')
    // Seuls le RSSI et le super administrateur administrent les comptes ; seul le second octroie un profil privilégié.
    expect(Object.keys(PROFILS).filter((r) => PROFILS[r].includes('IAM_ACCOUNT_READ'))).toEqual(['ROLE_RSSI', 'ROLE_SUPER_ADMIN'])
    expect(PROFILS.ROLE_SUPER_ADMIN).toContain('IAM_ROLE_PRIVILEGED_GRANT')
    expect(PROFILS.ROLE_SUPER_ADMIN).toContain('PLATFORM_CONFIG_EDIT')
  })
})

describe('navigation : groupe « Mon activité » (D-76)', () => {
  it('juste après le tableau de bord : journée, agenda, tâches, rapports (PIL_ACTIVITY_OWN), équipe (PIL_TEAM_READ)', () => {
    expect(NAVIGATION_DES[1].libelleKey).toBe('nav.groupe.monActivite')
    expect(NAVIGATION_DES[1].entrees.map((e) => [e.chemin, e.predicat?.any?.[0]])).toEqual([
      ['/des/activite', 'PIL_ACTIVITY_OWN'],
      ['/des/agenda', 'PIL_ACTIVITY_OWN'],
      ['/des/taches', 'PIL_ACTIVITY_OWN'],
      ['/des/rapports', 'PIL_ACTIVITY_OWN'],
      ['/des/equipe', 'PIL_TEAM_READ'],
      // Processus de l'unité (D-87) : tout membre interne.
      ['/des/processus', 'PIL_ACTIVITY_OWN'],
      // Lancer une demande et Mes demandes (D-98) : tout compte interne.
      ['/des/demandes/lancer', 'PIL_ACTIVITY_OWN'],
      ['/des/demandes', 'PIL_ACTIVITY_OWN'],
    ])
  })
})

describe('navigation : groupe « Administration » dans le département', () => {
  it('écrans sous /des/administration ; console de la plateforme par VITE_URL_CONSOLE (http://localhost:4202 par défaut)', () => {
    expect(urlConsole()).toBe('http://localhost:4202')
    expect(versConsole('/console/comptes')).toBe('http://localhost:4202/console/comptes')
    const groupe = NAVIGATION_DES.find((g) => g.libelleKey === 'nav.groupe.administration')!
    expect(groupe.entrees.map((e) => [e.chemin, e.predicat?.any?.[0]])).toEqual([
      ['/des/administration/comptes', 'IAM_ACCOUNT_READ'],
      // Pouvoirs hiérarchiques (D-81) : demandes des chefs à traiter, matrice des pouvoirs.
      ['/des/administration/demandes', 'IAM_ACCOUNT_READ'],
      ['/des/administration/profils', 'IAM_ACCOUNT_READ'],
      // Matrice des pouvoirs (D-94 §6.2) : IAM_ACCOUNT_READ et IAM_PROFILE_EDIT exigées (tous).
      ['/des/administration/matrice-pouvoirs', undefined],
      // Catalogue des unités organisationnelles (D-76).
      ['/des/administration/organisation', 'IAM_ACCOUNT_READ'],
      // Organigramme (D-86 §3).
      ['/des/administration/organigramme', 'IAM_ACCOUNT_READ'],
      ['/des/administration/journal', 'IAM_JOURNAL_READ'],
      ['/des/administration/politique-securite', 'PLATFORM_SECURITY_POLICY_READ'],
    ])
    // Plateforme (D-79) : écrans de la console servis aussi dans l'application interne, et lien vers la console.
    const plateforme = NAVIGATION_DES.find((g) => g.libelleKey === 'nav.groupe.plateforme')!
    expect(plateforme.entrees.map((e) => [e.chemin, e.predicat?.any?.[0]])).toEqual([
      ['/des/plateforme/supervision', 'PLATFORM_SUPERVISION_READ'],
      ['/des/plateforme/infrastructure', 'PLATFORM_OPS_READ'],
      ['/des/plateforme/configuration', 'PLATFORM_CONFIG_READ'],
      ['/des/plateforme/journalisation', 'PLATFORM_CONFIG_READ'],
      ['/des/plateforme/courriels', 'PLATFORM_MESSAGING_EDIT'],
      ['http://localhost:4202/console/tableau-de-bord', 'PLATFORM_CONFIG_READ'],
    ])
  })
})

const page = (permissions: string[], roles: string[]) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(permissions, roles) })

describe('tableau de bord : « Administration et sécurité des comptes »', () => {
  it('RSSI, sans tableau d’administration fourni : barre d’actions vers les écrans du département, sans liste ni « Mes accès »', async () => {
    rendrePage(<TableauDeBordPage />, {
      chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(PROFILS.ROLE_RSSI, ['ROLE_RSSI']),
      autres: [{ motif: '/des/administration/comptes', element: <span>écran des comptes</span> }],
    })
    const section = await screen.findByRole('region', { name: 'Administration et sécurité des comptes' })
    expect(within(section).queryByRole('list')).toBeNull()
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
    const barre = within(section).getByRole('group', { name: 'Écrans d’administration' })
    expect(within(barre).getAllByRole('button').map((b) => b.textContent)).toEqual([
      'Créer un compte', 'Tous les comptes', 'Comptes sans second facteur', 'Premiers accès en attente', 'Comptes désactivés',
      'Profils et permissions', 'Journal d’administration', 'Politique de sécurité', 'Console de la plateforme',
    ])
    fireEvent.click(within(barre).getByRole('button', { name: 'Comptes sans second facteur' }))
    expect(await screen.findByText('écran des comptes')).toBeTruthy()
  })
  it('tableau d’administration fourni par l’application : il remplace la barre d’actions', async () => {
    rendrePage(
      <ExtensionsTableauDeBordProvider valeur={{ administration: () => <section aria-label="Tableau d’administration">indicateurs</section> }}>
        <TableauDeBordPage />
      </ExtensionsTableauDeBordProvider>,
      { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(PROFILS.ROLE_SUPER_ADMIN, ['ROLE_SUPER_ADMIN']) },
    )
    expect(await screen.findByRole('region', { name: 'Tableau d’administration' })).toBeTruthy()
    expect(screen.queryByRole('region', { name: 'Administration et sécurité des comptes' })).toBeNull()
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
  })
  it('lecteur des comptes sans création, journal ni politique : actions restreintes ; sans IAM_ACCOUNT_READ : pas de section', async () => {
    page(['IAM_ACCOUNT_READ'], ['ROLE_LECTEUR'])
    const section = await screen.findByRole('region', { name: 'Administration et sécurité des comptes' })
    const textes = within(section).getAllByRole('button').map((l) => l.textContent)
    expect(textes).not.toContain('Créer un compte')
    expect(textes).not.toContain('Journal d’administration')
    expect(textes).not.toContain('Politique de sécurité')
    expect(textes).not.toContain('Console de la plateforme')
    expect(screen.queryByText('Aucun tableau de bord pour votre profil')).toBeNull()
    cleanup()
    page(PROFILS.ROLE_SNA, ['ROLE_SNA'])
    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('region', { name: 'Administration et sécurité des comptes' })).toBeNull()
  })
})
