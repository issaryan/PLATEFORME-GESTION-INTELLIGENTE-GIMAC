import { describe, expect, it } from 'vitest'
import type { ApiError } from '@gimac/socle'
import { servicesSimules } from './index'
import { fonctionsDesProfils, visibles } from './referentiels'

// Référentiel simulé, D-89 : étapes de la version (ajout, modification, ordre, retrait et refus HOM_ETAPE_*),
// catalogue de la version, lecture filtrée (fantômes, X-Gimac-Filtre), export CSV filtré.
const s = (profils?: string[]) => servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-101', nom: 'Ingénieur', profils } }).homologation
const code = (e: unknown) => (e as ApiError).code
const texte = (b: Blob) => b.text()

describe('référentiel simulé : étapes configurables (D-89 §2)', () => {
  it('onze étapes canoniques, ajout, modification, ordre, retrait ; refus du service', async () => {
    const h = s()
    const g = await h.grapheReferentiel('REF-CI-4')
    expect(g.etapes!.map((e) => e.code)).toEqual(['PRISE_CONTACT', 'ADHESION', 'CADRAGE', 'LANCEMENT', 'ATELIERS', 'INTEGRATION', 'TESTS', 'CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION', 'SUIVI'])
    expect(g.etapes!.find((e) => e.code === 'CERTIFICATION')).toMatchObject({ protegee: true, supprimable: false, jeton: '--gm-color-status-ok-fg' })
    expect(g.etapes!.find((e) => e.code === 'PRISE_CONTACT')).toMatchObject({ nombreOperations: 0, supprimable: true })
    expect(g.filtre).toBe('COMPLET')
    const ajoutee = await h.ajouterEtape('REF-CI-4', { code: 'RECETTE', libelle: ' Recette interne ', couleur: 'ACCENT', canonique: null, ordre: 8 })
    expect(ajoutee).toMatchObject({ code: 'RECETTE', libelle: 'Recette interne', ordre: 8, jeton: '--gm-color-brand-accent', canonique: null, canoniqueEffective: 'TESTS', supprimable: true })
    expect(await h.ajouterEtape('REF-CI-4', { code: 'FIN', libelle: 'Fin', couleur: null, canonique: null })).toMatchObject({ ordre: 13, couleur: 'NEUTRE' })
    expect(await h.modifierEtape('REF-CI-4', 'RECETTE', { libelle: 'Recette', couleur: 'INFO', canonique: 'TESTS' })).toMatchObject({ libelle: 'Recette', couleur: 'INFO', canonique: 'TESTS' })
    const catalogue = await h.catalogueReferentiels('REF-CI-4')
    expect(catalogue.etapesVersion!.map((e) => e.code)).toContain('RECETTE')
    expect(catalogue.couleurs!.map((c) => c.code)).toEqual(['NEUTRE', 'INFO', 'SUCCES', 'ALERTE', 'CRITIQUE', 'ACTION', 'ACCENT'])
    expect(catalogue.etapesCanoniques!.filter((e) => e.protegee).map((e) => e.code)).toEqual(['CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION'])
    expect((await h.catalogueReferentiels()).etapesVersion).toBeUndefined()
    // Une opération passe dans l'étape ajoutée ; une étape inconnue est refusée.
    const op = (await h.operationsReferentiel('REF-CI-4')).operations.find((o) => o.code === 'C11')!
    const { code: _c, successeurs: _s, ...saisie } = op
    expect((await h.enregistrerOperation('REF-CI-4', 'C11', { ...saisie, etape: 'RECETTE' } as never)).etape).toBe('RECETTE')
    await expect(h.enregistrerOperation('REF-CI-4', 'C11', { ...saisie, etape: 'INCONNUE' } as never)).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_INCONNUE')
    await expect(h.creerOperation('REF-CI-4', { ...saisie, code: 'NV-9', etape: 'INCONNUE' } as never)).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_INCONNUE')
    await expect(h.supprimerEtape('REF-CI-4', 'RECETTE')).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_NON_VIDE')
    await expect(h.supprimerEtape('REF-CI-4', 'CERTIFICATION')).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_CANONIQUE')
    await expect(h.modifierEtape('REF-CI-4', 'CERTIFICATION', { libelle: 'Certif', couleur: null, canonique: 'TESTS' })).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_CANONIQUE')
    expect(await h.modifierEtape('REF-CI-4', 'CERTIFICATION', { libelle: 'Certification', couleur: null, canonique: 'CERTIFICATION' })).toMatchObject({ couleur: 'SUCCES' })
    await expect(h.modifierEtape('REF-CI-4', 'XX', { libelle: 'x', couleur: null, canonique: null })).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_NOT_FOUND')
    await expect(h.modifierEtape('REF-CI-4', 'RECETTE', { libelle: ' ', couleur: null, canonique: null })).rejects.toSatisfy((e) => code(e) === 'VALIDATION_ERROR')
    await expect(h.ajouterEtape('REF-CI-4', { code: 'recette', libelle: 'x', couleur: null, canonique: null })).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_CODE_INVALIDE')
    await expect(h.ajouterEtape('REF-CI-4', { code: 'RECETTE', libelle: 'x', couleur: null, canonique: null })).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_CODE_DUPLIQUE')
    await expect(h.ajouterEtape('REF-CI-3', { code: 'NOUVELLE', libelle: 'x', couleur: null, canonique: null })).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_FROZEN')
    expect((await h.grapheReferentiel('REF-CI-3')).etapes!.every((e) => !e.supprimable)).toBe(true)
    // Ordre : une permutation exacte.
    const codes = (await h.grapheReferentiel('REF-CI-4')).etapes!.map((e) => e.code)
    await expect(h.ordonnerEtapes('REF-CI-4', codes.slice(1))).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_ORDRE_INVALIDE')
    await expect(h.ordonnerEtapes('REF-CI-4', [...codes.slice(1), codes[1]])).rejects.toSatisfy((e) => code(e) === 'HOM_ETAPE_ORDRE_INVALIDE')
    const ordonnees = await h.ordonnerEtapes('REF-CI-4', [...codes].reverse())
    expect(ordonnees[0]).toMatchObject({ code: 'FIN', ordre: 1, canoniqueEffective: 'CADRAGE' })
    // Retrait d'une étape vide ; la dérivation et un nouveau brouillon portent leurs étapes.
    await h.supprimerEtape('REF-CI-4', 'FIN')
    expect((await h.grapheReferentiel('REF-CI-4')).etapes!.some((e) => e.code === 'FIN')).toBe(false)
    const d = await h.deriverBrouillon('REF-CI-4')
    expect((await h.grapheReferentiel(d.id)).etapes!.map((e) => e.code)).toContain('RECETTE')
    const b = await h.creerBrouillon({ typeProjet: 'DELEGATAIRE', libelle: 'Nouveau' })
    expect((await h.grapheReferentiel(b.id)).etapes).toHaveLength(11)
  })
})

describe('référentiel simulé : lecture filtrée (D-89 §1)', () => {
  it('fonctions des profils ; éditeurs et READ_ALL complets ; un pôle ne voit que les siennes et leurs voisines en fantômes', async () => {
    expect(fonctionsDesProfils(['ROLE_CHEF_DES'])).toEqual(expect.arrayContaining(['CHEF_DES', 'CHEFS_DEPARTEMENT']))
    expect(visibles([], ['ROLE_DG']).filtre).toBe('COMPLET')
    expect((await s(['ROLE_INGENIEUR_HOMOLOGATION']).grapheReferentiel('REF-CI-4')).filtre).toBe('COMPLET')
    const h = s(['ROLE_POLE_RESEAU'])
    const g = await h.grapheReferentiel('REF-CI-4')
    expect(g.filtre).toBe('CONCERNE')
    const fantomes = g.noeuds.filter((n) => n.fantome)
    const miennes = g.noeuds.filter((n) => !n.fantome)
    expect(miennes.length).toBeGreaterThan(0)
    expect(fantomes.length).toBeGreaterThan(0)
    expect(fantomes.every((n) => n.etape === null && n.responsable === null && !!n.libelle)).toBe(true)
    expect(miennes.every((n) => [n.responsable, n.validateur, ...n.consultes, ...n.informes, ...n.externes, ...n.livrables.map((l) => l.deposant)].includes('POLE_RESEAU'))).toBe(true)
    const codes = new Set(g.noeuds.map((n) => n.code))
    expect(g.arcs.every((a) => codes.has(a.de) && codes.has(a.vers))).toBe(true)
    const liste = await h.operationsReferentiel('REF-CI-4')
    expect(liste.filtre).toBe('CONCERNE')
    expect(liste.operations.filter((o) => o.fantome)).toHaveLength(fantomes.length)
    const versions = await h.listerReferentiels()
    expect(versions.find((v) => v.id === 'REF-CI-4')).toMatchObject({ filtre: 'CONCERNE', nombreOperations: miennes.length })
    expect((await h.lireReferentiel('REF-CI-4')).filtre).toBe('CONCERNE')
    const csv = await texte((await h.exporterReferentiel('REF-CI-4')).blob)
    const lignes = csv.trim().split('\n')
    expect(lignes[0]).toBe('code;libelle;etape;segment;responsable;validateur;consultes;informes;externes;predecesseurs;delaiJours;fantome')
    expect(lignes).toHaveLength(g.noeuds.length + 1)
    expect(lignes.filter((l) => l.endsWith(';true'))).toHaveLength(fantomes.length)
    expect((await s().exporterReferentiel('REF-CI-3')).nom).toBe('referentiel-carte_interbancaire-v3.csv')
    expect((await s().exporterReferentiel('REF-CI-4')).nom).toBe('referentiel-carte_interbancaire-brouillon.csv')
  })
})
