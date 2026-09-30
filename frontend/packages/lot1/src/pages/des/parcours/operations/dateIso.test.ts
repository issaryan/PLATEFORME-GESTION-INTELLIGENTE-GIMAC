import { describe, expect, it } from 'vitest'
import { dateIso } from './Execution'

// Date de tenue de la cérémonie C1 : saisie au format du champ (JJ/MM/AAAA), envoyée en ISO.
describe('dateIso', () => {
  it('convertit JJ/MM/AAAA, laisse passer l’ISO et les espaces de bord', () => {
    expect(dateIso('28/09/2026')).toBe('2026-09-28')
    expect(dateIso(' 01/10/2026 ')).toBe('2026-10-01')
    expect(dateIso('2026-09-28')).toBe('2026-09-28')
  })
})
