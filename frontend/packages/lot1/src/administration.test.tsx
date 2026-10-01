import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, within } from '@testing-library/react'
import TableauDeBordPage from './pages/des/TableauDeBordPage'
import { rendrePage, sessionTest } from './test/rendu'
import { NAVIGATION_DES, URL_CONSOLE, versConsole } from './navigation'
import { PROFILS } from './permissions'

// Administration des comptes depuis l'application du département (D-62) : la gestion des comptes est
// dans la console ; le RSSI (IAM_ACCOUNT_READ) y est mené par le groupe « Administration » de la
// navigation et par la section de son tableau de bord.
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
    // Aucun autre profil de l'application du département n'administre les comptes.
    expect(Object.keys(PROFILS).filter((r) => PROFILS[r].includes('IAM_ACCOUNT_READ'))).toEqual(['ROLE_RSSI'])
  })
})

describe('navigation : groupe « Administration » vers la console', () => {
  it('adresse de la console par VITE_URL_CONSOLE (http://localhost:4202 par défaut), entrées gardées par permission', () => {
    expect(URL_CONSOLE).toBe('http://localhost:4202')
    expect(versConsole('/console/comptes')).toBe('http://localhost:4202/console/comptes')
    const groupe = NAVIGATION_DES.find((g) => g.libelleKey === 'nav.groupe.administration')!
    expect(groupe.entrees.map((e) => [e.chemin, e.permission])).toEqual([
      ['http://localhost:4202/console/comptes', 'IAM_ACCOUNT_READ'],
      ['http://localhost:4202/console/profils', 'IAM_ACCOUNT_READ'],
      ['http://localhost:4202/console/journal', 'IAM_JOURNAL_READ'],
      ['http://localhost:4202/console/politique-securite', 'PLATFORM_SECURITY_POLICY_READ'],
      ['http://localhost:4202/console/tableau-de-bord', 'IAM_ACCOUNT_READ'],
    ])
  })
})

const page = (permissions: string[], roles: string[]) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(permissions, roles) })

describe('tableau de bord : « Administration et sécurité des comptes »', () => {
  it('RSSI : liens vers les écrans de la console (création, listes filtrées, profils, journal, politique)', async () => {
    page(PROFILS.ROLE_RSSI, ['ROLE_RSSI'])
    const section = await screen.findByRole('region', { name: 'Administration et sécurité des comptes' })
    const liens = within(within(section).getByRole('list', { name: 'Écrans d’administration de la console' })).getAllByRole('link')
    expect(liens.map((l) => [l.textContent, l.getAttribute('href')])).toEqual([
      ['Créer un compte', 'http://localhost:4202/console/comptes/nouveau'],
      ['Tous les comptes', 'http://localhost:4202/console/comptes'],
      ['Comptes sans second facteur', 'http://localhost:4202/console/comptes?etat=SANS_2FA'],
      ['Premiers accès en attente', 'http://localhost:4202/console/comptes?etat=PREMIER_ACCES'],
      ['Comptes désactivés', 'http://localhost:4202/console/comptes?etat=INACTIF'],
      ['Profils et permissions', 'http://localhost:4202/console/profils'],
      ['Journal d’administration', 'http://localhost:4202/console/journal'],
      ['Politique de sécurité', 'http://localhost:4202/console/politique-securite'],
      ['Tableau de bord de la console', 'http://localhost:4202/console/tableau-de-bord'],
    ])
  })
  it('lecteur des comptes sans création, journal ni politique : liens restreints ; sans IAM_ACCOUNT_READ : pas de section', async () => {
    page(['IAM_ACCOUNT_READ'], ['ROLE_LECTEUR'])
    const section = await screen.findByRole('region', { name: 'Administration et sécurité des comptes' })
    const textes = within(section).getAllByRole('link').map((l) => l.textContent)
    expect(textes).not.toContain('Créer un compte')
    expect(textes).not.toContain('Journal d’administration')
    expect(textes).not.toContain('Politique de sécurité')
    expect(screen.queryByText('Aucun tableau de bord pour votre profil')).toBeNull()
    cleanup()
    page(PROFILS.ROLE_SNA, ['ROLE_SNA'])
    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('region', { name: 'Administration et sécurité des comptes' })).toBeNull()
  })
})
