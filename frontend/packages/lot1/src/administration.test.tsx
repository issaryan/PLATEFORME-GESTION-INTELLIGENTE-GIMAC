import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, within } from '@testing-library/react'
import TableauDeBordPage from './pages/des/TableauDeBordPage'
import { rendrePage, sessionTest } from './test/rendu'
import { NAVIGATION_DES, URL_CONSOLE, versConsole } from './navigation'
import { PROFILS } from './permissions'

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

describe('navigation : groupe « Administration » dans le département', () => {
  it('écrans sous /des/administration ; console de la plateforme par VITE_URL_CONSOLE (http://localhost:4202 par défaut)', () => {
    expect(URL_CONSOLE).toBe('http://localhost:4202')
    expect(versConsole('/console/comptes')).toBe('http://localhost:4202/console/comptes')
    const groupe = NAVIGATION_DES.find((g) => g.libelleKey === 'nav.groupe.administration')!
    expect(groupe.entrees.map((e) => [e.chemin, e.permission])).toEqual([
      ['/des/administration/comptes', 'IAM_ACCOUNT_READ'],
      ['/des/administration/profils', 'IAM_ACCOUNT_READ'],
      ['/des/administration/journal', 'IAM_JOURNAL_READ'],
      ['/des/administration/politique-securite', 'PLATFORM_SECURITY_POLICY_READ'],
      ['http://localhost:4202/console/tableau-de-bord', 'PLATFORM_CONFIG_READ'],
    ])
  })
})

const page = (permissions: string[], roles: string[]) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(permissions, roles) })

describe('tableau de bord : « Administration et sécurité des comptes »', () => {
  it('RSSI : liens vers les écrans du département (création, listes filtrées, profils, journal, politique) et la console', async () => {
    page(PROFILS.ROLE_RSSI, ['ROLE_RSSI'])
    const section = await screen.findByRole('region', { name: 'Administration et sécurité des comptes' })
    const liens = within(within(section).getByRole('list', { name: 'Écrans d’administration' })).getAllByRole('link')
    expect(liens.map((l) => [l.textContent, l.getAttribute('href')])).toEqual([
      ['Créer un compte', '/des/administration/comptes/nouveau'],
      ['Tous les comptes', '/des/administration/comptes'],
      ['Comptes sans second facteur', '/des/administration/comptes?etat=SANS_2FA'],
      ['Premiers accès en attente', '/des/administration/comptes?etat=PREMIER_ACCES'],
      ['Comptes désactivés', '/des/administration/comptes?etat=INACTIF'],
      ['Profils et permissions', '/des/administration/profils'],
      ['Journal d’administration', '/des/administration/journal'],
      ['Politique de sécurité', '/des/administration/politique-securite'],
      [expect.stringContaining('Console de la plateforme'), 'http://localhost:4202/console/tableau-de-bord'],
    ])
  })
  it('lecteur des comptes sans création, journal ni politique : liens restreints ; sans IAM_ACCOUNT_READ : pas de section', async () => {
    page(['IAM_ACCOUNT_READ'], ['ROLE_LECTEUR'])
    const section = await screen.findByRole('region', { name: 'Administration et sécurité des comptes' })
    const textes = within(section).getAllByRole('link').map((l) => l.textContent)
    expect(textes).not.toContain('Créer un compte')
    expect(textes).not.toContain('Journal d’administration')
    expect(textes).not.toContain('Politique de sécurité')
    expect(textes.some((x) => x?.includes('Console de la plateforme'))).toBe(false)
    expect(screen.queryByText('Aucun tableau de bord pour votre profil')).toBeNull()
    cleanup()
    page(PROFILS.ROLE_SNA, ['ROLE_SNA'])
    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('region', { name: 'Administration et sécurité des comptes' })).toBeNull()
  })
})
