import { afterEach, describe, expect, it } from 'vitest'
import { avatarsSimules, photoDemo, urlAvatar } from './avatar'

// Photos de profil (docs/lot1/mon-compte.md §1) : adresse de la passerelle versionnée, initiales sans version.
afterEach(() => avatarsSimules.reinitialiser())

describe('urlAvatar', () => {
  it('adresse /bff/<couche>/iam/avatars/{id}?v=<version> ; rien sans compte ni version', () => {
    expect(urlAvatar('des', 'U-001', 'a1b2c3')).toBe('/bff/des/iam/avatars/U-001?v=a1b2c3')
    expect(urlAvatar('console', 'u/2', 'v 1')).toBe('/bff/console/iam/avatars/u%2F2?v=v%201')
    expect(urlAvatar('portail', 'U-001', null)).toBeUndefined()
    expect(urlAvatar('portail', 'U-001', 'a1')).toBe('/bff/portail/iam/moi/avatar?v=a1')
    expect(urlAvatar('portail', null, 'x')).toBeUndefined()
  })

  it('services simulés : photo rangée en mémoire, rendue en priorité, retirée ensuite', () => {
    const v = avatarsSimules.definir('U-9', 'data:image/png;base64,AAAA')
    expect(v).toMatch(/^sim/)
    expect(avatarsSimules.version('U-9')).toBe(v)
    expect(urlAvatar('des', 'U-9', v)).toBe('data:image/png;base64,AAAA')
    const v2 = avatarsSimules.definir('U-9', 'data:image/png;base64,BBBBBB')
    expect(v2).not.toBe(v)
    avatarsSimules.retirer('U-9')
    expect(avatarsSimules.version('U-9')).toBeNull()
    expect(urlAvatar('des', 'U-9', v2)).toBe(`/bff/des/iam/avatars/U-9?v=${v2}`)
  })

  it('photo de démonstration déterministe, rangée une seule fois', () => {
    expect(photoDemo('U-1')).toBe(photoDemo('U-1'))
    expect(photoDemo('U-1')).not.toBe(photoDemo('U-2'))
    expect(photoDemo('U-1')).toMatch(/^data:image\/svg\+xml;charset=utf-8,/)
    const v = avatarsSimules.demo('U-3')
    expect(avatarsSimules.demo('U-3')).toBe(v)
    expect(urlAvatar('des', 'U-3', v)).toBe(photoDemo('U-3'))
  })
})
