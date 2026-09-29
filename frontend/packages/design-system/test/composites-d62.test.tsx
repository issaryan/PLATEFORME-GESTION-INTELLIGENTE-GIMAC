// Composites et extensions de la console et de « Mon compte » (D-62) : assemblages de composants
// existants, rétrocompatibles, sans nouvelle feuille de style.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Checkbox, CheckboxGroup, LangueProvider, LiveStatGroup, OneTimeSecret, PermissionPicker, TextField } from '../src'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('TextField : saisie masquée', () => {
  it('type password : saisie masquée, jamais préremplie par le navigateur', () => {
    render(<TextField name="motDePasse" label="Mot de passe" type="password" value="" />)
    const champ = screen.getByLabelText('Mot de passe')
    expect(champ.getAttribute('type')).toBe('password')
    expect(champ.getAttribute('autocomplete')).toBe('new-password')
  })
  it('sans type : texte, autocomplétion transmise', () => {
    render(<TextField name="tel" label="Téléphone" autoComplete="tel" />)
    expect(screen.getByLabelText('Téléphone').getAttribute('autocomplete')).toBe('tel')
  })
})

describe('Checkbox et CheckboxGroup : erreur et lecture seule', () => {
  it('Checkbox : erreur annoncée et reliée à la case', () => {
    render(<Checkbox name="charte" label="J’accepte la charte" error="La charte doit être acceptée." />)
    const c = screen.getByLabelText('J’accepte la charte')
    expect(c.getAttribute('aria-invalid')).toBe('true')
    expect(document.getElementById(c.getAttribute('aria-describedby')!)?.textContent).toContain('La charte doit être acceptée.')
    expect(screen.getByRole('alert')).toBeTruthy()
  })
  it('CheckboxGroup : erreur, cases désactivées, « tout » calculé sur les seules options du groupe', () => {
    const change = vi.fn()
    const opts = [{ value: 'A', label: 'Alpha' }, { value: 'B', label: 'Bêta' }]
    const { rerender } = render(<CheckboxGroup id="g" label="Profils" options={opts} value={['A', 'B', 'Z']} onChange={change} error="Au moins un profil." />)
    expect((screen.getByLabelText('Tout sélectionner') as HTMLInputElement).checked).toBe(true)
    expect(document.getElementById('g-error')?.textContent).toContain('Au moins un profil.')
    rerender(<CheckboxGroup label="Profils" options={opts} value={['A']} onChange={change} disabled />)
    expect((screen.getByLabelText('Alpha') as HTMLInputElement).disabled).toBe(true)
    expect((screen.getByLabelText('Tout sélectionner') as HTMLInputElement).indeterminate).toBe(true)
    expect(screen.queryByRole('alert')).toBeNull()
  })
})

describe('OneTimeSecret', () => {
  it('avertissement, valeur et copie confirmée', async () => {
    const onCopy = vi.fn(async () => undefined)
    render(<OneTimeSecret label="Mot de passe provisoire" value="Kp7#vQ2m" onCopy={onCopy} />)
    expect(screen.getByRole('group', { name: 'Mot de passe provisoire' })).toBeTruthy()
    expect(screen.getByTestId('secret-valeur').textContent).toBe('Kp7#vQ2m')
    expect(document.body.textContent).toContain("n'est affichée qu'une fois")
    fireEvent.click(screen.getByRole('button', { name: 'Copier' }))
    await waitFor(() => expect(screen.getByTestId('secret-copie').textContent).toBe('Copié dans le presse-papiers.'))
    expect(onCopy).toHaveBeenCalledWith('Kp7#vQ2m')
  })
  it('presse-papiers par défaut, échec signalé, avertissement propre', async () => {
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: vi.fn(async () => { throw new Error('refus') }) }, configurable: true })
    render(<OneTimeSecret label="Secret" value="x" warning="Transmettre au titulaire en main propre." />)
    expect(screen.getByText('Transmettre au titulaire en main propre.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Copier' }))
    await waitFor(() => expect(screen.getByTestId('secret-copie').textContent).toContain('Copie impossible'))
  })
})

describe('PermissionPicker', () => {
  const groupes = [
    { key: 'IAM', label: 'Comptes (IAM)', options: [{ value: 'IAM_ACCOUNT_READ', label: 'IAM_ACCOUNT_READ' }, { value: 'IAM_JOURNAL_READ', label: 'IAM_JOURNAL_READ' }] },
    { key: 'PLATFORM', label: 'Plateforme', options: [{ value: 'PLATFORM_CONFIG_READ', label: 'PLATFORM_CONFIG_READ' }] },
  ]
  it('un groupe par domaine ; un changement conserve les autres domaines', () => {
    const change = vi.fn()
    render(<PermissionPicker label="Permissions" groups={groupes} value={['PLATFORM_CONFIG_READ']} onChange={change} />)
    expect(screen.getByText('1 permission(s) sur 3')).toBeTruthy()
    fireEvent.click(screen.getByLabelText('IAM_JOURNAL_READ'))
    expect(change).toHaveBeenLastCalledWith(['PLATFORM_CONFIG_READ', 'IAM_JOURNAL_READ'])
    fireEvent.click(screen.getAllByLabelText('Tout sélectionner')[0])
    expect(change).toHaveBeenLastCalledWith(['PLATFORM_CONFIG_READ', 'IAM_ACCOUNT_READ', 'IAM_JOURNAL_READ'])
  })
  it('lecture seule et erreur', () => {
    render(<PermissionPicker label="Permissions" groups={groupes} readOnly error="Permission inconnue" />)
    expect((screen.getByLabelText('IAM_ACCOUNT_READ') as HTMLInputElement).disabled).toBe(true)
    expect(screen.getByRole('alert').textContent).toContain('Permission inconnue')
    fireEvent.click(screen.getByLabelText('IAM_ACCOUNT_READ'))
  })
})

describe('LiveStatGroup', () => {
  const items = [{ key: 'etat', label: 'État global', value: 'Opérationnelle', status: 'ok' as const, statusLabel: 'nominal' }, { key: 'alertes', label: 'Alertes', value: 3 }]
  it('état du flux annoncé, valeurs en zone aria-live, heure de mise à jour', () => {
    const { container, rerender } = render(<LiveStatGroup title="Plateforme" state="connecting" items={items} />)
    expect(screen.getByRole('heading', { level: 2, name: 'Plateforme' })).toBeTruthy()
    expect(screen.getByRole('status').textContent).toContain('Connexion au flux')
    expect(container.querySelector('[aria-live="polite"]')?.querySelectorAll('.gm-stat')).toHaveLength(2)
    rerender(<LiveStatGroup title="Plateforme" state="live" items={items} updatedAt="14:05:32" level={3} />)
    expect(screen.getByRole('heading', { level: 3 })).toBeTruthy()
    expect(screen.getByRole('status').textContent).toContain('Temps réel')
    expect(screen.getByText('Mis à jour à 14:05:32')).toBeTruthy()
    rerender(<LangueProvider langue="en"><LiveStatGroup title="Platform" state="lost" items={items} /></LangueProvider>)
    expect(screen.getByRole('status').textContent).toContain('Stream interrupted')
  })
})
