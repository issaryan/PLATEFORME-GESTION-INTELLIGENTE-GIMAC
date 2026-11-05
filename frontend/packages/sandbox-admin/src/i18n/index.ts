import type { Catalogue } from '@gimac/socle'
import { fr } from './fr'
import { en } from './en'
import { es } from './es'
import * as activite from '../activite/i18n'

// Catalogues de l'espace sandbox du DES (FR, EN, ES, D-122), fusionnés à ceux du socle et du lot 1.
export const catalogues: Catalogue[] = [{ fr, en, es }, { fr: activite.fr, en: activite.en, es: activite.es }]
