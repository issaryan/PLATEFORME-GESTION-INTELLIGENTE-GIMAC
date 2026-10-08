import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { EmptyState, Skeleton } from '@gimac/design-system'
import { satisfait, usePermissions, type GroupeNavigation } from '@gimac/socle'
import {
  accesFonctionnalite, fonctionnalitesOuvertes, navigationDepuis, navigationRepli, widgetsOuverts, type Application, type CleWidget, type DefinitionFonctionnalite, type Ecran,
  type EtatCatalogue, type Traduire,
} from './fonctionnalites'

// Catalogue des fonctionnalités servi par MS-IAM (D-94), mis à disposition des écrans de l'application : l'application
// le lit (GET …/iam/configuration/fonctionnalites) et le fournit ici ; barre latérale, gardes et tableau de bord en
// dérivent. Sans fournisseur, ou tant que le catalogue n'est pas servi, rien n'est ouvert au-delà de l'accueil.

const INDISPONIBLE: EtatCatalogue = { etat: 'indisponible', fonctionnalites: [] }
const Contexte = createContext<EtatCatalogue>(INDISPONIBLE)

export function CatalogueFonctionnalitesProvider({ valeur, children }: { valeur: EtatCatalogue; children: ReactNode }) {
  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

export const useCatalogueFonctionnalites = () => useContext(Contexte)

/** Traduction d'un libellé servi : clé connue des catalogues de traduction, sinon texte tel quel. */
export function useTraduire(): Traduire {
  const { t, i18n } = useTranslation()
  return useMemo(() => ({ t: (k: string) => t(k), existe: (k: string) => i18n.exists(k) }), [t, i18n])
}

/** Barre latérale d'une application, dérivée du catalogue servi ; l'accueil seul en repli. */
export function useNavigationCatalogue(application: 'des' | 'console', ecrans: Record<string, Ecran>, prefixeGroupe: string): GroupeNavigation[] {
  const c = useCatalogueFonctionnalites()
  const tr = useTraduire()
  return useMemo(
    () => (c.etat === 'pret' ? navigationDepuis(c.fonctionnalites, application, ecrans, tr, prefixeGroupe) : navigationRepli(application)),
    [c, application, ecrans, tr, prefixeGroupe],
  )
}

/** Fonctionnalités ouvertes et widgets du tableau de bord pour la session. */
export function useFonctionnalitesOuvertes(application: Application = 'des'): { etat: EtatCatalogue['etat']; ouvertes: DefinitionFonctionnalite[]; widgets: CleWidget[] } {
  const c = useCatalogueFonctionnalites()
  const permissions = usePermissions()
  return useMemo(() => ({
    etat: c.etat,
    ouvertes: c.etat === 'pret' ? fonctionnalitesOuvertes(c.fonctionnalites, application, permissions) : [],
    widgets: c.etat === 'pret' ? widgetsOuverts(c.fonctionnalites, permissions) : [],
  }), [c, application, permissions])
}

/** Message du repli prudent : le catalogue n'est pas servi, seul l'accueil est ouvert. */
export function CatalogueIndisponible() {
  const { t } = useTranslation()
  return <EmptyState icon="lock" title={t('catalogue.indisponible.titre')} text={t('catalogue.indisponible.texte')} />
}

/**
 * Garde de route : l'écran d'une fonctionnalité ne s'ouvre que si le catalogue la rend visible et que les permissions
 * satisfont son prédicat (et détiennent `droit` s'il est donné). Refus explicite ; la passerelle refait le contrôle.
 */
export function GardeFonctionnalite({ cle, droit, children }: { cle: string; droit?: string; children: ReactNode }) {
  const { t } = useTranslation()
  const c = useCatalogueFonctionnalites()
  const acces = accesFonctionnalite(c, cle, usePermissions(), droit)
  if (acces === 'ouvert') return <>{children}</>
  if (acces === 'chargement') return <Skeleton height={32} />
  if (acces === 'indisponible') return <CatalogueIndisponible />
  return <EmptyState icon="lock" title={t('catalogue.refuse.titre')} text={t('catalogue.refuse.texte')} />
}

/**
 * Garde d'un lien profond vers un objet qui concerne la personne (D-98 : instance ou étape d'un processus ouverte depuis une
 * notification, une tâche ou « Mes demandes ») : ouverte à tout porteur de la permission d'entrée, indépendamment de la
 * visibilité d'une fonctionnalité de la barre latérale (« Processus de l'unité » peut être masquée à l'initiateur ou à
 * l'intervenant d'un autre service). Le service refait le contrôle de lecture (intervenant, initiateur, unité).
 */
export function GardePermission({ permission, children }: { permission: string; children: ReactNode }) {
  const { t } = useTranslation()
  if (satisfait(usePermissions(), { all: [], any: [permission] })) return <>{children}</>
  return <EmptyState icon="lock" title={t('catalogue.refuse.titre')} text={t('catalogue.refuse.texte')} />
}
