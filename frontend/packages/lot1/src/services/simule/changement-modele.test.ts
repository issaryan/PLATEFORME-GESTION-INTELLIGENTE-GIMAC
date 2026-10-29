import { describe, expect, it } from 'vitest'
import { definitionChangementDemo } from './changement-modele'

describe('definitionChangementDemo', () => {
  it('décrit les quatre sections du demandeur puis la décision SSI signée', () => {
    const p = definitionChangementDemo()
    expect(p.code).toBe('DEMANDE_CHANGEMENT')
    expect(p.referenceModele).toBe('CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}')
    expect(p.initiateurs?.regles).toEqual(['TOUT_LE_GIMAC'])
    expect(p.initiateurs?.initiateurIntervenant).toBe(true)
    expect(p.formulaire?.champs).toHaveLength(12)
    expect(p.formulaire?.champs.every((c) => c.obligatoire)).toBe(true)
    expect(p.etapes.map((e) => e.code)).toEqual(['IDENTIFICATION', 'RAISON', 'IMPACT', 'IMPLEMENTATION', 'SSI'])
    expect(p.etapes.slice(0, 3).every((e) => e.intervenant.mode === 'LANCEUR')).toBe(true)
    expect(p.etapes[3].intervenant.mode).toBe('AU_LANCEMENT')
    expect(p.etapes[3].livrables[0]).toMatchObject({ code: 'DOSSIER_CHANGEMENT', obligatoire: true })
    expect(p.etapes[4].intervenant).toMatchObject({ mode: 'PROFIL', profil: 'ROLE_AGENT_SSI' })
    expect(p.etapes[4].signatures[0].porte).toEqual(['DOSSIER_CHANGEMENT'])
    expect(p.etapes[4].champs.map((c) => c.code)).toEqual(['DATE_DEPLOIEMENT', 'DATE_REVUE', 'EVALUATION', 'DECISION', 'DESCRIPTION'])
  })
})
