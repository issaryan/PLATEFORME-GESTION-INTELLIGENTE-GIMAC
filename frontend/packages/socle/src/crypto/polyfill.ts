// Premier import de chaque application (console, département, portail) : servi par l'application elle-même, il
// remplace le script en ligne que la politique de sécurité du contenu (script-src 'self') bloque.
import { installerRandomUUID } from './uuid'

installerRandomUUID()
