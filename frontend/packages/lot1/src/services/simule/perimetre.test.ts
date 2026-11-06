import { describe, expect, it } from 'vitest'
import { ApiError } from '@gimac/socle'
import type { Liaison } from '../../modele/homologation'
import { GESTIONNAIRE_DEMO, magasinPerimetres } from './perimetre'

// D-133 simulé : règles de PerimetreLiaison (gestionnaire seul avec HOM_PERIMETRE_EDIT, HOM_PERIMETRE_EDIT_TOUS sur tout
// projet), un test par refus ; chaque modification est tracée.
const liaison = (etat = 'OUVERTE'): Liaison => ({ id: 'LIA-T', typeProjet: 'PAIEMENT_MOBILE', etat, perimetre: ['TRANSVERSE', 'TRANSFERT', 'PAIEMENT'] }) as unknown as Liaison
const code = (f: () => unknown) => {
  try {
    f()
  } catch (e) {
    return (e as ApiError).code
  }
  return null
}

describe('périmètre simulé du projet de certification', () => {
  it('gestionnaire : modification tracée, version 2, service retiré hors périmètre, ajout d’un souscrit', () => {
    const m = magasinPerimetres({ ...GESTIONNAIRE_DEMO, profils: ['ROLE_CHEF_DDRM'] })
    const l = liaison()
    expect(m.lire(l)).toMatchObject({ version: 1, modifiable: true, gestionnaireNom: 'Chef de la DDRM' })
    const v = m.modifier(l, ['PAIEMENT', 'FACTURE'], ' Lot 1 ')
    expect(v.version).toBe(2)
    expect(v.services.find((s) => s.code === 'TRANSFERT')).toMatchObject({ inclus: false, etat: 'HORS_PERIMETRE' })
    expect(v.services.find((s) => s.code === 'FACTURE')?.inclus).toBe(true)
    expect(m.historique(l, 0, 10).contenu[0]).toMatchObject({ version: 2, permissionUtilisee: 'HOM_PERIMETRE_EDIT', avant: ['PAIEMENT', 'TRANSFERT', 'TRANSVERSE'], apres: ['FACTURE', 'PAIEMENT', 'TRANSVERSE'], motif: 'Lot 1' })
    expect(m.modifier(l, ['PAIEMENT', 'FACTURE'], 'Sans changement').version).toBe(2)
    expect(m.historique(l, 1, 10).contenu).toEqual([])
  })
  it('super administrateur ou chargé SNA : droit sur tout projet', () => {
    const m = magasinPerimetres({ id: 'U-SA', nom: 'Super administrateur', profils: ['ROLE_SUPER_ADMIN'] })
    const l = liaison()
    m.modifier(l, ['PAIEMENT'], 'Arbitrage')
    expect(m.historique(l, 0, 10).contenu[0].permissionUtilisee).toBe('HOM_PERIMETRE_EDIT_TOUS')
    expect(magasinPerimetres({ id: 'U-SNA', nom: 'Chargé SNA', profils: ['ROLE_CHARGE_IMPLEMENTATION_SNA'] }).lire(l).modifiable).toBe(true)
  })
  it('refus : autre chef de la DDRM (non gestionnaire), agent SNA, projet clos, motif absent, vide, non souscrit', () => {
    const l = liaison()
    expect(code(() => magasinPerimetres({ id: 'U-AUTRE', nom: 'Chef de la DDRM', profils: ['ROLE_CHEF_DDRM'] }).modifier(l, ['PAIEMENT'], 'x'))).toBe('HOM_PERIMETRE_GESTIONNAIRE_SEUL')
    const sna = magasinPerimetres({ id: 'U-SNA', nom: 'Agent SNA', profils: ['ROLE_SNA'] })
    expect(sna.lire(l)).toMatchObject({ modifiable: false, proposables: [] })
    expect(code(() => sna.modifier(l, ['PAIEMENT'], 'x'))).toBe('HOM_PERIMETRE_GESTIONNAIRE_SEUL')
    const libre = magasinPerimetres(undefined)
    expect(code(() => libre.modifier(l, ['PAIEMENT'], ' '))).toBe('HOM_PERIMETRE_MOTIF_REQUIS')
    expect(code(() => libre.modifier(l, ['TRANSVERSE'], 'x'))).toBe('HOM_PERIMETRE_VIDE')
    expect(code(() => libre.modifier(l, ['PAIEMENT', 'CREDIT'], 'x'))).toBe('HOM_PERIMETRE_SERVICE_NON_SOUSCRIT')
    expect(code(() => libre.modifier(liaison('CLOSE'), ['PAIEMENT'], 'x'))).toBe('HOM_PERIMETRE_LIAISON_CLOSE')
    expect(libre.lire(liaison('CERTIFIEE')).modifiable).toBe(false)
  })
})
