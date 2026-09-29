import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ICONS, type IconName, type Option, type ResourceDescriptor } from '@gimac/design-system'
import type { ResourceDescriptorLot1 } from '@gimac/socle'
import type { Services } from '../services/ports'
import { useExecuteurActions } from './actions'

// Conversion d'un descripteur du lot (libellés en clés i18n, actions branchées sur les services) en
// descripteur du design system (libellés traduits, actions exécutées par ExecuteurActions). Les
// composants universels du catalogue restent ainsi purement visuels et ignorent les services.

export type Pilotes<T> = { openForm?: (mode: 'create' | 'edit', row: T) => void; openTab?: (key: string) => void }

const icone = (n?: string): IconName | undefined => (n && n in ICONS ? (n as IconName) : undefined)

export function useDescripteurDS<T extends object>(d: ResourceDescriptorLot1<T, Services>, pilotes: Pilotes<T> = {}): ResourceDescriptor<T> {
  const { t, i18n } = useTranslation()
  const executeur = useExecuteurActions()
  const sujet = d.liveTopic
  return useMemo<ResourceDescriptor<T>>(() => {
    const traduireOptions = (o: Option[] | (() => Promise<Option[]>) | undefined) =>
      Array.isArray(o) ? o.map((x) => ({ ...x, label: t(x.label) })) : o
    return {
      ...d,
      label: { ...d.label, singular: t(d.label.singular), plural: t(d.label.plural) },
      fields: d.fields.map((f) => ({ ...f, label: t(f.label), help: f.help ? t(f.help) : undefined, options: traduireOptions(f.options) })),
      tabs: d.tabs?.map((o) => ({ ...o, label: t(o.label) })),
      statusDictionary: d.statusDictionary && Object.fromEntries(Object.entries(d.statusDictionary).map(([k, v]) => [k, { ...v, label: t(v.label) }])),
      actions: d.actions.map((a) => ({
        key: a.key,
        label: t(a.label),
        icon: icone(a.icon),
        variant: a.variant,
        scope: a.scope,
        permission: a.permission,
        isAllowed: a.isAllowed,
        // Les dialogues sont ceux d'ExecuteurActions : motif, signature et erreurs y sont gérés.
        confirm: 'none' as const,
        run: ({ row }: { row?: T | null }) =>
          executeur.lancer(a, (row ?? null) as T, row ? String((row as Record<string, unknown>)[d.titleField] ?? '') : t(d.label.plural), sujet, {
            openForm: pilotes.openForm as ((mode: 'create' | 'edit', row: unknown) => void) | undefined,
            openTab: pilotes.openTab,
          }),
      })),
    }
    // La langue fait partie des dépendances : bascule sans rechargement.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [d, t, i18n.language, executeur, pilotes.openForm, pilotes.openTab])
}
