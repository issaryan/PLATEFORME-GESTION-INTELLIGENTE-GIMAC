// Pied de page GIMAC (§6.3) : identité du groupement, plateforme, classification, documentation,
// identifiant de corrélation, dans les trois langues.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { Footer, LangueProvider } from '../src'

afterEach(cleanup)

describe('Footer', () => {
  it('porte le groupement, la version, la classification et le droit d’auteur', () => {
    const { container } = render(<Footer version="1.0.0" annee={2026} />)
    const pied = container.querySelector('footer')
    expect(pied?.className).toBe('gm-footer')
    expect(screen.getByText('GIMAC').className).toBe('gm-footer-marque')
    expect(screen.getByText("Groupement Interbancaire Monétique de l'Afrique Centrale")).toBeTruthy()
    expect(screen.getByText('Plateforme de suivi des activités du GIMAC, version 1.0.0')).toBeTruthy()
    expect(screen.getByText('Usage interne, confidentiel')).toBeTruthy()
    expect(screen.getByText('© 2026 GIMAC')).toBeTruthy()
    expect(screen.queryByText('Documentation')).toBeNull()
  })

  it('affiche la documentation et l’identifiant de corrélation de la dernière requête', () => {
    render(<Footer documentation="/docs" correlationId="c0ffee00-1234" />)
    expect(screen.getByText('Plateforme de suivi des activités du GIMAC')).toBeTruthy()
    expect(screen.getByText('Documentation').closest('a')?.getAttribute('href')).toBe('/docs')
    expect(screen.getByText(/Dernière requête/)).toBeTruthy()
    expect(document.body.textContent).toContain('c0ffee00-1234')
  })

  it('produit hébergé (D-118) : désignation et classification propres, ou aucune classification', () => {
    render(<Footer version="1.0.0" plateforme="Sandbox GIMAC, version 0.1.0" classification="Environnement de test" />)
    expect(screen.getByText('Sandbox GIMAC, version 0.1.0')).toBeTruthy()
    expect(screen.getByText('Environnement de test')).toBeTruthy()
    expect(screen.queryByText(/Plateforme de suivi/)).toBeNull()
    cleanup()
    render(<Footer couche="console" classification="" />)
    expect(screen.queryByText('Usage interne, confidentiel')).toBeNull()
    expect(screen.getByText('Plateforme de suivi des activités du GIMAC')).toBeTruthy()
  })

  it('se traduit en anglais et en espagnol', () => {
    const { rerender } = render(<LangueProvider langue="en"><Footer /></LangueProvider>)
    expect(screen.getByText('Central African Interbank Electronic Payment Group')).toBeTruthy()
    rerender(<LangueProvider langue="es"><Footer /></LangueProvider>)
    expect(screen.getByText('Uso interno, confidencial')).toBeTruthy()
  })
})
