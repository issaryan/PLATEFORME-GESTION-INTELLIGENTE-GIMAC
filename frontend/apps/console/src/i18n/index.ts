import type { Catalogue } from '@gimac/socle'
import { fr } from './fr'
import { en } from './en'
import { es } from './es'

// Catalogues de la console (FR, EN, ES), fusionnés à ceux du socle et de « Mon compte ».
export const catalogues: Catalogue[] = [{ fr, en, es }]
