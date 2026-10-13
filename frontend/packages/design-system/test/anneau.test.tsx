import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ProgressRing } from '../src'

describe('ProgressRing', () => {
  it('au moins aussi large que son cercle, sans le borner : libellé et badge ne débordent plus', () => {
    const { container } = render(<ProgressRing label="Disponibilité des services" value={100} size={104} status="ok" statusLabel="14 sur 14 démarrés" />)
    const anneau = container.querySelector('.gm-ring') as HTMLElement
    expect(anneau.style.minWidth).toBe('104px')
    expect(anneau.style.width).toBe('')
    expect(screen.getByText('Disponibilité des services')).toBeTruthy()
    expect(screen.getByText('14 sur 14 démarrés')).toBeTruthy()
    expect(screen.getByRole('img').getAttribute('aria-label')).toBe('Disponibilité des services : 100 %')
  })
})
