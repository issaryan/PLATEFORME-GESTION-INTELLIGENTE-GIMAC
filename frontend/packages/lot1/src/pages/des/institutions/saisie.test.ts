import { describe, expect, it } from 'vitest'
import { candidature, dateContact, erreursDuService, normaliserCode, verifierCandidature } from './saisie'

// Saisie de la prise de contact (DES-07) : corps envoyé à MS-PARTICIPANT et contrôles alignés sur
// InstitutionRequests.CandidatureRequest (formats BIC et BEAC, longueurs, date non future à Douala).

const base = { nom: 'Banque des Lagunes', pays: 'GA', categorie: 'BANQUE' }
const cles = (v: Record<string, unknown>, jour = '2026-10-07') => Object.fromEntries(Object.entries(verifierCandidature(v, jour)).map(([k, e]) => [k, e?.cle]))

describe('Prise de contact : normalisation', () => {
  it('BIC et code BEAC : majuscules, espaces retirés, vide ramené à null', () => {
    expect(normaliserCode(' bdlg ga li ')).toBe('BDLGGALI')
    expect(normaliserCode('10005 001')).toBe('10005001')
    expect(normaliserCode('   ')).toBeNull()
    expect(normaliserCode(undefined)).toBeNull()
  })

  it('date du champ (JJ/MM/AAAA) envoyée en ISO, date ISO conservée, date inexistante signalée', () => {
    expect(dateContact('06/10/2026')).toBe('2026-10-06')
    expect(dateContact(' 07/10/2026 ')).toBe('2026-10-07')
    expect(dateContact('2026-01-12')).toBe('2026-01-12')
    expect(dateContact('')).toBeNull()
    expect(dateContact(null)).toBeNull()
    expect(dateContact('31/02/2026')).toBeUndefined()
    expect(dateContact('6/10/26')).toBeUndefined()
    expect(dateContact('2026-02-31')).toBeUndefined()
  })

  it('corps de la requête : celui qu’attend le service, sans la saisie brute de la date', () => {
    expect(candidature({ ...base, nom: '  Banque des Lagunes ', sigle: ' ', codeIdentification: 'bdlg ga li xxx', codeBanqueBeac: 'cm 001', datePriseDeContact: '06/10/2026', originePriseDeContact: 'Salon', projetEnvisage: '' }))
      .toEqual({
        nom: 'Banque des Lagunes', sigle: null, pays: 'GA', categorie: 'BANQUE', codeIdentification: 'BDLGGALIXXX', codeBanqueBeac: 'CM001',
        datePriseDeContact: '2026-10-06', originePriseDeContact: 'Salon', projetEnvisage: null, groupe: null,
      })
  })

  it('D-104 : groupe ou maison mère, espaces réduits comme le service ; plus de 100 caractères refusé', () => {
    expect(candidature({ ...base, groupe: '  Groupe \t Afriland  ' }).groupe).toBe('Groupe Afriland')
    expect(candidature({ ...base, groupe: '   ' }).groupe).toBeNull()
    expect(cles({ ...base, groupe: 'g'.repeat(100) })).toEqual({})
    expect(cles({ ...base, groupe: 'g'.repeat(101) })).toEqual({ groupe: 'institution.erreur.longueur' })
  })
})

describe('Prise de contact : contrôles avant envoi', () => {
  it('saisie réaliste acceptée : BIC à 8 ou 11 caractères, en minuscules ou avec espaces, date du jour à Douala', () => {
    expect(cles({ ...base, codeIdentification: 'BDLGGALI' })).toEqual({})
    expect(cles({ ...base, codeIdentification: 'bdlg ga li xxx', codeBanqueBeac: '10005 001', datePriseDeContact: '07/10/2026' })).toEqual({})
    expect(cles({ ...base, sigle: 'S'.repeat(50), projetEnvisage: 'p'.repeat(1000), originePriseDeContact: 'o'.repeat(255) })).toEqual({})
  })

  it('formats BIC et BEAC refusés : longueur, chiffre dans le code banque, tiret', () => {
    expect(cles({ ...base, codeIdentification: 'BDLGGALIX' })).toEqual({ codeIdentification: 'institution.erreur.codeIdentification' })
    expect(cles({ ...base, codeIdentification: 'BD1GGALI' })).toEqual({ codeIdentification: 'institution.erreur.codeIdentification' })
    expect(cles({ ...base, codeBanqueBeac: '10005-001' })).toEqual({ codeBanqueBeac: 'institution.erreur.codeBanqueBeac' })
    expect(cles({ ...base, codeBanqueBeac: '12345678901' })).toEqual({ codeBanqueBeac: 'institution.erreur.codeBanqueBeac' })
  })

  it('date : format JJ/MM/AAAA exigé, lendemain du jour de Douala refusé', () => {
    expect(cles({ ...base, datePriseDeContact: '2026/10/06' })).toEqual({ datePriseDeContact: 'institution.erreur.dateFormat' })
    expect(cles({ ...base, datePriseDeContact: '08/10/2026' })).toEqual({ datePriseDeContact: 'institution.erreur.dateFuture' })
    expect(cles({ ...base, datePriseDeContact: '07/10/2026' })).toEqual({})
  })

  it('obligatoires, listes et longueurs du service', () => {
    expect(cles({ nom: ' ', pays: '', categorie: '' })).toEqual({ nom: 'institution.erreur.requis', pays: 'institution.erreur.requis', categorie: 'institution.erreur.requis' })
    expect(cles({ nom: 'B', pays: 'FR', categorie: 'AUTRE' })).toEqual({ pays: 'institution.erreur.choix', categorie: 'institution.erreur.choix' })
    const e = verifierCandidature({ ...base, nom: 'n'.repeat(256), sigle: 'S'.repeat(51), originePriseDeContact: 'o'.repeat(256), projetEnvisage: 'p'.repeat(1001) }, '2026-10-07')
    expect(e).toEqual({
      nom: { cle: 'institution.erreur.longueur', valeurs: { max: 255 } },
      sigle: { cle: 'institution.erreur.longueur', valeurs: { max: 50 } },
      originePriseDeContact: { cle: 'institution.erreur.longueur', valeurs: { max: 255 } },
      projetEnvisage: { cle: 'institution.erreur.longueur', valeurs: { max: 1000 } },
    })
  })

  it('refus du service par champ : libellé du champ, générique pour un champ inconnu', () => {
    const t = (cle: string, o?: Record<string, unknown>) => (cle === 'institution.erreur.service.inconnu' ? String(o?.defaultValue) : cle)
    expect(erreursDuService({ codeIdentification: 'must match', inconnu: 'x' }, t)).toEqual({
      codeIdentification: 'institution.erreur.service.codeIdentification',
      inconnu: 'institution.erreur.service',
    })
  })
})
