import { describe, expect, it } from 'vitest'
import { ajouterJours, depuisSaisie, heureDe, heureValide, instant, jourDe, jourOuvreSuivant, jourSemaine, lundiDe, lundiDeSemaine, minutesDe, semaineDe, semaineIso, versSaisie } from './jours'

// Jours de « Mon activité » (D-76) : fuseau de Douala (UTC+1), semaines ISO, saisie JJ/MM/AAAA.
describe('jours et semaines', () => {
  it('jour à Douala d’un instant : minuit UTC+1', () => {
    expect(jourDe('2026-10-05T23:30:00Z')).toBe('2026-10-06')
    expect(jourDe('2026-10-05T22:59:00Z')).toBe('2026-10-05')
    expect(heureDe('2026-10-05T08:15:00Z')).toBe('09:15')
    expect(minutesDe('2026-10-05T08:15:00Z')).toBe(9 * 60 + 15)
    expect(instant('2026-10-06', '09:00')).toBe('2026-10-06T09:00:00+01:00')
  })
  it('semaines ISO, lundi, jours ouvrés', () => {
    expect(jourSemaine('2026-10-05')).toBe(1)
    expect(jourSemaine('2026-10-11')).toBe(7)
    expect(lundiDe('2026-10-08')).toBe('2026-10-05')
    expect(semaineDe('2026-10-08')).toEqual(['2026-10-05', '2026-10-06', '2026-10-07', '2026-10-08', '2026-10-09', '2026-10-10', '2026-10-11'])
    expect(semaineIso('2026-10-06')).toBe('2026-W41')
    expect(semaineIso('2027-01-01')).toBe('2026-W53')
    expect(semaineIso('2024-12-30')).toBe('2025-W01')
    expect(lundiDeSemaine('2026-W41')).toBe('2026-10-05')
    expect(lundiDeSemaine('2025-W01')).toBe('2024-12-30')
    expect(jourOuvreSuivant('2026-10-09')).toBe('2026-10-12')
    expect(jourOuvreSuivant('2026-10-06')).toBe('2026-10-07')
    expect(ajouterJours('2026-12-31', 1)).toBe('2027-01-01')
    expect(() => lundiDeSemaine('2026-41')).toThrow()
    expect(() => ajouterJours('06/10/2026', 1)).toThrow()
  })
  it('saisie JJ/MM/AAAA et heures HH:MM', () => {
    expect(depuisSaisie('06/10/2026')).toBe('2026-10-06')
    expect(depuisSaisie(' 29/02/2028 ')).toBe('2028-02-29')
    expect(depuisSaisie('31/02/2026')).toBeNull()
    expect(depuisSaisie('2026-10-06')).toBeNull()
    expect(versSaisie('2026-10-06')).toBe('06/10/2026')
    expect(heureValide('09:30')).toBe(true)
    expect(heureValide('24:00')).toBe(false)
    expect(heureValide('9h30')).toBe(false)
  })
})
