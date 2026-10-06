import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'

/**
 * Libellés de profils servis par le serveur : catalogue des profils (GET /configuration/profils, console) ou table
 * code vers libellé (libellesProfils de GET /pouvoirs/moi, application interne).
 */
export type LibellesProfils = Readonly<Record<string, string | null | undefined>> | ReadonlyArray<{ code: string; libelle?: string | null }>

const duServeur = (libelles: LibellesProfils | null | undefined, code: string): string | null | undefined =>
  !libelles ? undefined : Array.isArray(libelles) ? (libelles as ReadonlyArray<{ code: string; libelle?: string | null }>).find((p) => p.code === code)?.libelle : (libelles as Record<string, string | null | undefined>)[code]

/**
 * Libellé d'un profil (D-86 §2, §5), unique pour toutes les applications : celui du serveur quand il est connu
 * (argument `serveur` de l'appel, sinon la source passée au hook), sinon le catalogue i18n (compte.profil.<code>),
 * le code à défaut. Un libellé égal au code (attribut gimac_libelle absent) cède aussi la place au catalogue.
 */
export function useLibelleProfil(libelles?: LibellesProfils | null) {
  const { t } = useTranslation()
  return useCallback((code: string, serveur?: string | null) => {
    const l = serveur || duServeur(libelles, code)
    return l && l !== code ? l : t(`compte.profil.${code}`, { defaultValue: code })
  }, [libelles, t])
}
