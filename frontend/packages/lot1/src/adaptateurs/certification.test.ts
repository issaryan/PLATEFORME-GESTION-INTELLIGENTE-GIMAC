import { describe, expect, it } from 'vitest'
import type { Circuit } from '../modele/certification'
import { designationsPossibles, etatQuitusVersCircuit, quitusSignables, separerDesignations, verifierEmpreinte, versQuitusCircuit } from './certification'

const circuit: Circuit = {
  id: 'C1',
  socle: { empreinte: 'ab', operations: [{ code: 'OP-E01', libelle: 'Clés' }], services: ['RETRAIT'] },
  quitus: [
    { nature: 'TECHNIQUE', etat: 'SIGNE_FAVORABLE', signataireRole: 'CHEF_DES', dateSignature: '25/09/2026 16:10', actionsPermises: [] },
    { nature: 'SECURITE', etat: 'ATTENDU', signataireRole: 'RSSI', actionsPermises: ['SIGNER'] },
    { nature: 'CONFORMITE', etat: 'SIGNE_DEFAVORABLE', signataireRole: 'RESPONSABLE_CONFORMITE', motif: 'pièce manquante', actionsPermises: [] },
    { nature: 'DIRECTION_GENERALE', etat: 'ATTENDU', signataireRole: 'DG', actionsPermises: [] },
  ],
}

describe('adaptateurs de certification', () => {
  it('traduit les quatre natures vers QuitusCircuit en mode « quatre »', () => {
    expect(versQuitusCircuit(circuit, (r) => `r:${r}`)).toEqual({
      tech: { state: 'signed', signer: 'r:CHEF_DES', at: '25/09/2026 16:10', reason: undefined },
      sec: { state: 'pending', signer: 'r:RSSI', at: undefined, reason: undefined },
      conf: { state: 'refused', signer: 'r:RESPONSABLE_CONFORMITE', at: undefined, reason: 'pièce manquante' },
      dg: { state: 'todo', signer: 'r:DG', at: undefined, reason: undefined },
    })
  })
  it('un quitus attendu sans action permise reste « à venir »', () => {
    expect(etatQuitusVersCircuit({ ...circuit.quitus[3] })).toBe('todo')
  })
  it('seuls les quitus annoncés signables par le service sont proposés', () => {
    expect(quitusSignables(circuit).map((q) => q.nature)).toEqual(['SECURITE'])
  })
  it('désignations : opérations et services du socle, puis séparation', () => {
    expect(designationsPossibles(circuit).map((d) => d.value)).toEqual(['OP:OP-E01', 'SV:RETRAIT'])
    expect(separerDesignations(['OP:OP-E01', 'SV:RETRAIT'])).toEqual({ operationsDesignees: ['OP-E01'], servicesDesignes: ['RETRAIT'] })
  })
  it('vérification d\'empreinte : identique, divergente, indisponible', () => {
    expect(verifierEmpreinte('AB12', 'ab12')).toBe(true)
    expect(verifierEmpreinte('ab13', 'ab12')).toBe(false)
    expect(verifierEmpreinte(null, 'ab12')).toBeNull()
  })
})
