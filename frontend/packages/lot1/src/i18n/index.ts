import type { Catalogue } from '@gimac/socle'
import { commun } from './commun'

// Catalogues du lot 1 : un fichier par écran (FR, EN, ES), fusionnés ici. Chaque écran ajoute le sien.
const ecrans = import.meta.glob<{ catalogue: Catalogue }>('./ecrans/*.ts', { eager: true })
export const catalogues: Catalogue[] = [commun, ...Object.values(ecrans).map((m) => m.catalogue)]
