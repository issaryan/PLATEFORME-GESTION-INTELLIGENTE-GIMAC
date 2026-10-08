import type { CatalogueModelisateur, EntreeCatalogue, EtapeReferentiel, Livrable, NoeudGraphe, PositionNoeud, ReferenceOperation, SaisieOperation, SignatureReferentiel } from '../modele/homologation'
import type { SegmentParcours } from '../modele/parcours'

// Adaptateurs du modélisateur de processus (DES-12, D-77 §6) : géométrie du canevas, saisie de la modale et
// contrôles de forme avant envoi. Les règles du graphe (cycles, anomalies, chemin critique, échéances) restent
// celles du service : l'interface ne fait que les afficher.

type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string

/** Carte d'opération du canevas (px) et pas de la disposition automatique. */
export const CARTE = { largeur: 232, hauteur: 128 }
const PAS = { colonne: 264, ligne: 152, marge: 48, entete: 40 }
export const SEGMENTS: SegmentParcours[] = ['AMONT', 'BRANCHE', 'AVAL']

/**
 * Étapes des couloirs : celles de la version (D-89, `version`), dans leur ordre, avec leur libellé et la variable CSS
 * de leur couleur ; à défaut, la liste du catalogue (libellés i18n parcours.etape.<code>).
 */
export type SourceEtapes = { etapes: { code: string; libelle?: string; jeton?: string | null }[]; version?: boolean }

/** Source des couloirs : étapes du graphe triées par ordre (D-89), sinon le catalogue. */
export function sourceEtapes(etapes: EtapeReferentiel[] | undefined, catalogue: Pick<CatalogueModelisateur, 'etapes'>): SourceEtapes {
  return etapes?.length ? { etapes: [...etapes].sort((a, b) => a.ordre - b.ordre), version: true } : catalogue
}

/** Rang d'affichage d'une étape : ordre de la source (APPROBATION_SECURITE, sans rang, garde sa place). */
export function rangEtapes(source: SourceEtapes): Map<string, number> {
  return new Map(source.etapes.map((e, i) => [e.code, i]))
}

/**
 * Disposition automatique : colonnes par étape (rang du catalogue), sous-colonnes par couche de Kahn du graphe
 * (servie par le service), bandes par segment ; dans une case, ordre puis code.
 */
export function dispositionAuto(noeuds: NoeudGraphe[], source: SourceEtapes): Map<string, PositionNoeud> {
  const rang = rangEtapes(source)
  // Étapes de la version (D-89) : une colonne réservée à chaque étape vide, pour que son couloir ait sa place.
  const presentes = new Set(noeuds.map((n) => n.etape ?? ''))
  const etapes = [...new Set([...(source.version ? source.etapes.map((e) => e.code) : []), ...presentes])].sort((a, b) => (rang.get(a) ?? 99) - (rang.get(b) ?? 99))
  // Sous-colonnes : couches présentes dans chaque étape, rangées.
  const colonnes = new Map<string, number>()
  let x = 0
  for (const e of etapes) {
    const couches = [...new Set(noeuds.filter((n) => (n.etape ?? '') === e).map((n) => n.couche ?? 0))].sort((a, b) => a - b)
    couches.forEach((c, i) => colonnes.set(`${e}|${c}`, x + i))
    x += Math.max(couches.length, 1)
  }
  // Bandes : hauteur de la plus haute case du segment.
  const cases = new Map<string, NoeudGraphe[]>()
  for (const n of noeuds) {
    const k = `${n.segment ?? 'AMONT'}|${n.etape ?? ''}|${n.couche ?? 0}`
    cases.set(k, [...(cases.get(k) ?? []), n])
  }
  const positions = new Map<string, PositionNoeud>()
  let y = 0
  for (const s of SEGMENTS) {
    const duSegment = [...cases.entries()].filter(([k]) => k.startsWith(`${s}|`))
    if (!duSegment.length) continue
    const hauteur = Math.max(...duSegment.map(([, l]) => l.length))
    for (const [k, l] of duSegment) {
      const [, e, c] = k.split('|')
      ;[...l].sort((a, b) => (a.ordre ?? 0) - (b.ordre ?? 0) || a.code.localeCompare(b.code)).forEach((n, i) => {
        positions.set(n.code, { x: (colonnes.get(`${e}|${c}`) ?? 0) * PAS.colonne, y: PAS.entete + y + i * PAS.ligne })
      })
    }
    y += hauteur * PAS.ligne + PAS.marge
  }
  return positions
}

/** Position affichée : celle enregistrée, sinon celle de la disposition automatique. */
export function positionsEffectives(noeuds: NoeudGraphe[], auto: Map<string, PositionNoeud>): Map<string, PositionNoeud> {
  return new Map(noeuds.map((n) => [n.code, n.position ?? auto.get(n.code) ?? { x: 0, y: 0 }]))
}

export type Couloir = { cle: string; libelle: string; x: number; y: number; largeur: number; hauteur: number; jeton?: string | null }

/** Couloirs du canevas : une colonne par étape et une bande par segment, englobant les cartes qui s'y trouvent. */
export function couloirs(noeuds: NoeudGraphe[], positions: Map<string, PositionNoeud>, source: SourceEtapes, t: Traduire): { etapes: Couloir[]; segments: Couloir[] } {
  if (!noeuds.length) return { etapes: [], segments: [] }
  const pos = (n: NoeudGraphe) => positions.get(n.code) ?? { x: 0, y: 0 }
  const xs = noeuds.map((n) => pos(n).x)
  const ys = noeuds.map((n) => pos(n).y)
  const haut = Math.min(...ys) - PAS.entete - 8
  const bas = Math.max(...ys) + CARTE.hauteur + 16
  const gauche = Math.min(...xs) - 24
  const droite = Math.max(...xs) + CARTE.largeur + 24
  const rang = rangEtapes(source)
  const parCode = new Map(source.etapes.map((e) => [e.code, e]))
  const libelle = (e: string) => {
    if (!e) return noeuds.every((n) => n.etape || n.fantome) && noeuds.some((n) => !n.etape && n.fantome) ? t('modelisateur.couloir.fantomes') : t('modelisateur.sansEtape')
    return source.version ? parCode.get(e)?.libelle ?? e : t(`parcours.etape.${e}`, { defaultValue: parCode.get(e)?.libelle ?? e })
  }
  const codes = [...new Set([...(source.version ? source.etapes.map((e) => e.code) : []), ...noeuds.map((n) => n.etape ?? '')])].sort((a, b) => (rang.get(a) ?? 99) - (rang.get(b) ?? 99))
  // Une étape vide de la version (D-89) prend place juste après le couloir précédent ; celles qui précèdent la
  // première étape occupée se rangent à sa gauche (colonnes réservées par la disposition automatique).
  const VIDE = CARTE.largeur + 24
  const xs0 = codes.map((e) => noeuds.filter((n) => (n.etape ?? '') === e).map((n) => pos(n).x))
  const premiere = xs0.findIndex((l) => l.length > 0)
  let droitePrecedente = premiere > 0 ? Math.min(...xs0[premiere]) - 12 - premiere * (VIDE + 8) - 8 : gauche
  const etapes = codes.map((e, i) => {
    const l = xs0[i]
    const x = l.length ? Math.min(...l) - 12 : droitePrecedente + 8
    const largeur = l.length ? Math.max(...l) + CARTE.largeur + 12 - x : VIDE
    droitePrecedente = x + largeur
    return { cle: `etape-${e || 'aucune'}`, libelle: libelle(e), x, y: haut, largeur, hauteur: bas - haut, jeton: parCode.get(e)?.jeton ?? null }
  })
  // Bandes de segment sur toute la largeur des couloirs (étapes vides comprises).
  const g = Math.min(gauche, ...etapes.map((e) => e.x))
  const d = Math.max(droite, ...etapes.map((e) => e.x + e.largeur))
  const segments = SEGMENTS.filter((s) => noeuds.some((n) => n.segment === s)).map((s) => {
    const l = noeuds.filter((n) => n.segment === s).map((n) => pos(n).y)
    const y = Math.min(...l) - 12
    return { cle: `segment-${s}`, libelle: t(`parcours.segment.${s}`), x: g, y, largeur: d - g, hauteur: Math.max(...l) + CARTE.hauteur + 12 - y }
  })
  return { etapes, segments }
}

/** Corps d'enregistrement (PUT) : tous les champs de l'opération, sans son code ni ce que le service calcule. */
export function saisieDe(o: ReferenceOperation): SaisieOperation {
  return {
    libelle: o.libelle ?? '', description: o.description ?? null, etape: o.etape ?? null, segment: o.segment ?? null, ordre: o.ordre ?? null,
    responsable: o.responsable ?? null, validateur: o.validateur ?? null, consultes: o.consultes ?? [], informes: o.informes ?? [], externes: o.externes ?? [],
    predecesseurs: o.predecesseurs ?? [], delaiJours: o.delaiJours ?? null, grain: o.grain ?? null, plateforme: o.plateforme ?? null,
    cle: !!o.cle, secret: !!o.secret, oubliSansSymptome: !!o.oubliSansSymptome, pivot: !!o.pivot, valeurReference: o.secret ? null : o.valeurReference ?? null,
    livrables: (o.livrables ?? []).map((l) => ({ ...l, typesPieces: [...(l.typesPieces ?? [])] })), geste: o.geste ?? null, jalon: o.jalon ?? null,
    declencheur: o.geste === 'EVENEMENT_EXTERNE' ? o.declencheur ?? null : null, particularite: o.particularite ?? null, position: o.position ?? null,
    phase: o.phase ?? null, partie: o.partie ?? null, serviceCode: o.serviceCode ?? null, natureCouverture: o.natureCouverture ?? null,
    preuveAttendue: o.preuveAttendue ?? null, idScenario: o.idScenario ?? null, fonctionResponsable: o.fonctionResponsable ?? null, grille: o.grille ?? null,
    // D-91 : signatures (toujours envoyées : [] les retire) ; D-93 : vue et tâche aux consultés (absentes : conservées).
    signatures: (o.signatures ?? []).map((x) => ({ ...x, porte: [...(x.porte ?? [])] })),
    ...(o.vue !== undefined ? { vue: o.vue } : {}),
    ...(o.consultesRecoiventTache !== undefined ? { consultesRecoiventTache: !!o.consultesRecoiventTache } : {}),
  }
}

/** Opération vierge proposée à la création (étape et segment de départ facultatifs). */
export function operationVierge(etape: ReferenceOperation['etape'] = null, segment: SegmentParcours = 'BRANCHE'): ReferenceOperation {
  return {
    code: '', libelle: '', description: null, etape, segment, ordre: null, responsable: null, validateur: null, consultes: [], informes: [], externes: [],
    predecesseurs: [], delaiJours: 3, grain: 'OPERATION', plateforme: null, cle: false, secret: false, oubliSansSymptome: false, pivot: false, valeurReference: null,
    livrables: [], geste: 'DEPOSER_PREUVE', jalon: null, declencheur: null, particularite: null, position: null,
    phase: null, partie: null, serviceCode: null, natureCouverture: null, idScenario: null, fonctionResponsable: null, preuveAttendue: null,
    signatures: [], vue: null, consultesRecoiventTache: false,
  }
}

export const livrableVierge = (n: number, deposant: Livrable['deposant']): Livrable => ({ code: `L${n}`, libelle: '', nature: 'LIVRABLE', deposant, typesPieces: ['application/pdf'], obligatoire: true })
/** Signature vierge (D-91) : code S<n>, ordre à la suite, livrables portés à choisir. */
export const signatureVierge = (n: number): SignatureReferentiel => ({ code: `S${n}`, libelle: '', signataire: null, ordre: n, porte: [] })
/** Code d'une signature (D-91 §5.4). */
export const CODE_SIGNATURE = /^[A-Z][A-Z0-9_]{1,39}$/

export const CODE_OPERATION = /^[A-Z0-9][A-Z0-9_-]*$/

/**
 * Contrôles de forme de la modale, avant envoi (le service refait tout) : clés d'erreur i18n par champ, ou par
 * `livrables.<i>.<champ>`. V ≠ R, déclencheur exigé par un événement externe, secret sans valeur de référence.
 */
export function erreursSaisie(o: ReferenceOperation, creation: boolean, codesExistants: string[]): Record<string, string> {
  const e: Record<string, string> = {}
  if (creation) {
    if (!o.code) e.code = 'commun.champObligatoire'
    else if (o.code.length > 40 || !CODE_OPERATION.test(o.code)) e.code = 'modelisateur.erreur.code'
    else if (codesExistants.includes(o.code)) e.code = 'erreur.HOM_OPERATION_CODE_DUPLICATE'
  }
  if (!o.libelle.trim()) e.libelle = 'commun.champObligatoire'
  else if (o.libelle.length > 300) e.libelle = 'modelisateur.erreur.long'
  if ((o.description ?? '').length > 4000) e.description = 'modelisateur.erreur.long'
  if (!o.etape) e.etape = 'commun.champObligatoire'
  if (!o.segment) e.segment = 'commun.champObligatoire'
  if (!o.responsable) e.responsable = 'commun.champObligatoire'
  if (!o.validateur) e.validateur = 'commun.champObligatoire'
  else if (o.validateur === o.responsable) e.validateur = 'modelisateur.erreur.validateurResponsable'
  if (o.delaiJours === null || o.delaiJours < 0 || o.delaiJours > 365 || !Number.isInteger(o.delaiJours)) e.delaiJours = 'modelisateur.erreur.delai'
  if (o.geste === 'EVENEMENT_EXTERNE' && !o.declencheur) e.declencheur = 'modelisateur.erreur.declencheur'
  if (o.secret && o.valeurReference) e.valeurReference = 'erreur.HOM_OPERATION_SECRET_VALUE'
  if ((o.valeurReference ?? '').length > 120) e.valeurReference = 'modelisateur.erreur.long'
  if (o.code && o.predecesseurs.includes(o.code)) e.predecesseurs = 'erreur.HOM_OPERATION_SELF_PREDECESSOR'
  const codes = o.livrables.map((l) => l.code.trim())
  const signatures = o.signatures ?? []
  const codesSig = signatures.map((x) => x.code.trim())
  signatures.forEach((x, i) => {
    if (!x.code.trim()) e[`signatures.${i}.code`] = 'commun.champObligatoire'
    else if (!CODE_SIGNATURE.test(x.code.trim())) e[`signatures.${i}.code`] = 'modelisateur.erreur.codeSignature'
    else if (codesSig.indexOf(x.code.trim()) !== i) e[`signatures.${i}.code`] = 'modelisateur.erreur.signatureDupliquee'
    if (!x.libelle.trim()) e[`signatures.${i}.libelle`] = 'commun.champObligatoire'
    else if (x.libelle.length > 200) e[`signatures.${i}.libelle`] = 'modelisateur.erreur.long'
    if (!x.signataire) e[`signatures.${i}.signataire`] = 'commun.champObligatoire'
    if (!Number.isInteger(x.ordre) || x.ordre < 1 || x.ordre > 99) e[`signatures.${i}.ordre`] = 'modelisateur.erreur.ordreSignature'
    if (!x.porte.length) e[`signatures.${i}.porte`] = 'modelisateur.erreur.signatureSansLivrable'
  })
  o.livrables.forEach((l, i) => {
    if (!l.code.trim()) e[`livrables.${i}.code`] = 'commun.champObligatoire'
    else if (codes.indexOf(l.code.trim()) !== i) e[`livrables.${i}.code`] = 'modelisateur.erreur.livrableDuplique'
    if (!l.libelle.trim()) e[`livrables.${i}.libelle`] = 'commun.champObligatoire'
    if (!l.deposant) e[`livrables.${i}.deposant`] = 'commun.champObligatoire'
  })
  return e
}

/** Onglet de la modale qui porte un champ en erreur (pour y conduire l'utilisateur). */
export function ongletDuChamp(champ: string): 'general' | 'roles' | 'livrables' | 'signatures' | 'execution' | 'chronologie' {
  if (champ.startsWith('livrables')) return 'livrables'
  if (champ.startsWith('signatures')) return 'signatures'
  if (['responsable', 'validateur', 'consultes', 'informes', 'externes'].includes(champ)) return 'roles'
  if (['geste', 'jalon', 'declencheur', 'particularite', 'plateforme', 'grain', 'valeurReference', 'vue'].includes(champ)) return 'execution'
  if (['predecesseurs', 'delaiJours'].includes(champ)) return 'chronologie'
  return 'general'
}

/** Libellé d'une entrée du catalogue : clé i18n de l'interface si elle existe, sinon libellé du service. */
export function libelleEntree(t: (cle: string, o?: Record<string, unknown>) => string, prefixe: string | null, e: EntreeCatalogue): string {
  return prefixe ? t(`${prefixe}.${e.code}`, { defaultValue: e.libelle }) : e.libelle
}

/** Libellé d'une opération du parcours : celui que sert le référentiel publié (D-93), son code à défaut. */
export function libelleOperation(_t: unknown, o: { rang: string; libelle?: string | null }): string {
  return o.libelle || o.rang
}
