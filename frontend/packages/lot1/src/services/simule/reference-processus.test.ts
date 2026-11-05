import { describe, expect, it } from 'vitest'
import { referenceSimulee } from './reference-processus'

describe('referenceSimulee', () => {
  it('remplace le code, la date et la séquence, ou retourne null sans gabarit', () => {
    const date = new Date('2026-10-28T10:00:00Z')
    expect(referenceSimulee('CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}', 'DEMANDE_CHANGEMENT', date, 42)).toBe('CHG-2026-10-28-000042')
    expect(referenceSimulee('{CODE}-{AAAA}-{MM}-{JJ}-{NNNNNN}', 'CONGE', date, 1234567)).toBe('CONGE-2026-10-28-1234567')
    expect(referenceSimulee('CHG-{AAAA}-{N}', 'DEMANDE_CHANGEMENT', date, 412)).toBe('CHG-2026-412')
    expect(referenceSimulee(undefined, 'CONGE', date, 1)).toBeNull()
    expect(referenceSimulee(null, 'CONGE', date, 1)).toBeNull()
    expect(referenceSimulee('', 'CONGE', date, 1)).toBeNull()
  })
})
