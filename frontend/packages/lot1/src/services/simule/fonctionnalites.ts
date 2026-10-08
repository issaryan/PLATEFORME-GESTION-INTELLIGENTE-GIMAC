import type { DefinitionFonctionnalite } from '../../fonctionnalites'
import donnees from './fonctionnalites-initiales.json'

// Catalogue des fonctionnalités de démonstration (D-94) : copie des valeurs de départ de MS-IAM
// (ms-iam/src/main/resources/config/fonctionnalites-initiales.json), servie par MS-IAM simulé ; jamais par une
// construction de production (services réels : GET /bff/{des,console}/iam/configuration/fonctionnalites).
export const FONCTIONNALITES_INITIALES: DefinitionFonctionnalite[] = (donnees.fonctionnalites as unknown as DefinitionFonctionnalite[]).map((f) => ({ ...f, modifieLe: null, modifiePar: null }))
