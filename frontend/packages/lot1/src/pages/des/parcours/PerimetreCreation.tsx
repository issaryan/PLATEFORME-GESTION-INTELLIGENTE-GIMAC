import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CheckboxGroup, InlineAlert, Skeleton } from '@gimac/design-system'
import { usePerimetreProposable } from '../../../hooks/parcours'
import type { PerimetreProposable } from '../../../modele/parcours'

// D-145 : le gestionnaire du projet au GIMAC définit le périmètre à la création de la liaison. Les services proposables
// (souscrits par l'institution, sinon catalogue du type de projet) sont tous retenus au départ ; le gestionnaire retire
// ceux que le projet ne couvre pas. MS-HOMOLOGATION revérifie, journalise la version 1 et publie le périmètre à la
// sandbox, où l'institution le lit sans pouvoir le modifier.

export type ChoixPerimetre = {
  proposable: PerimetreProposable | undefined
  enCours: boolean
  indisponible: boolean
  choisis: string[]
  choisir: (codes: string[]) => void
  /** Aucun service retenu alors que des services sont proposables : la création est retenue. */
  vide: boolean
  /** Périmètre à envoyer : services retenus, ou rien quand aucun service n'est proposable (le service dérive). */
  aEnvoyer: string[] | undefined
}

/** Choix du périmètre pour une institution et un type de projet : tout est retenu à chaque nouvelle liste proposable. */
export function useChoixPerimetre(institutionId: string, typeProjet: string): ChoixPerimetre {
  const q = usePerimetreProposable(institutionId, typeProjet)
  const [choisis, choisir] = useState<string[]>([])
  const codes = (q.data?.services ?? []).map((s) => s.code)
  const cle = `${institutionId}|${typeProjet}|${codes.join(',')}`
  useEffect(() => {
    choisir(codes)
    // La liste proposable est résumée par sa clé : institution, type et codes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cle])
  const retenus = choisis.filter((c) => codes.includes(c))
  return {
    proposable: q.data,
    enCours: Boolean(institutionId && typeProjet) && q.isPending,
    indisponible: q.isError,
    choisis: retenus,
    choisir,
    vide: codes.length > 0 && retenus.length === 0,
    aEnvoyer: codes.length > 0 ? retenus : undefined,
  }
}

/** Section « Périmètre du projet » des écrans de création : cases des services proposables, avec leur interface. */
export function ChampPerimetre({ choix, pret, erreur }: { choix: ChoixPerimetre; pret: boolean; erreur?: string }) {
  const { t } = useTranslation()
  if (!pret) return <InlineAlert level="info" title={t('perimetreCreation.titre')}>{t('perimetreCreation.attente')}</InlineAlert>
  if (choix.enCours) return <Skeleton height={96} />
  if (choix.indisponible) return <InlineAlert level="warn" title={t('perimetreCreation.titre')}>{t('perimetreCreation.indisponible')}</InlineAlert>
  const services = choix.proposable?.services ?? []
  if (!services.length) return <InlineAlert level="info" title={t('perimetreCreation.titre')}>{t('perimetreCreation.aucun')}</InlineAlert>
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
      <CheckboxGroup
        id="gm-perimetre-creation"
        label={t('perimetreCreation.titre')}
        value={choix.choisis}
        onChange={choix.choisir}
        error={erreur}
        options={services.map((s) => ({
          value: s.code,
          label: `${s.libelle === s.code ? s.code : `${s.libelle} (${s.code})`}${s.interfaceSandbox ? ` · ${t(`perimetreCreation.interface.${s.interfaceSandbox}`)}` : ''}`,
        }))}
      />
      <span className="gm-field-help">{t(choix.proposable?.souscrits ? 'perimetreCreation.aide.souscrits' : 'perimetreCreation.aide.catalogue')}</span>
    </div>
  )
}
