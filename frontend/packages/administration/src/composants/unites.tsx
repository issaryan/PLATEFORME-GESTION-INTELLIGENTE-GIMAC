import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CrudDialog, Select, TextField } from '@gimac/design-system'
import { useMessageErreur, type MessageErreur } from '@gimac/compte'
import { TYPES_UNITE, type TypeUnite, type Unite } from '@gimac/lot1'
import { useComptes, useEffet } from '../hooks'
import { enArbre } from './rattachement'

// Fenêtre d'une unité organisationnelle (D-76 §1.1, D-81 §2), commune à « Organisation » et à l'organigramme (D-86 §3) :
// code (fixé à la création), libellé, type, unité parente et responsable ; refus ORG_* restitués sur leur champ.

const CHAMPS_UNITE: Record<string, keyof Unite> = { code: 'code', libelle: 'libelle', parent: 'parent', type: 'type', responsableId: 'responsableId' }

/** Comptes internes actifs, candidats à la responsabilité d'une unité. */
export function useResponsables(actif = true) {
  const comptes = useComptes({ page: 1, taille: 100, etat: 'ACTIF' }, actif)
  return (comptes.data?.contenu ?? []).filter((c) => !c.institutionId)
}

export function DialogueUnite({ unite, unites, parentInitial, onFermer, onFait }: { unite?: Unite; unites: Unite[]; parentInitial?: string; onFermer: () => void; onFait: (u: Unite) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const enregistrer = useEffet((s, u: Unite) => (unite ? s.organisation.modifierUnite(unite.code, u) : s.organisation.creerUnite(u)))
  const responsables = useResponsables()
  const [initiale] = useState(() => ({ code: unite?.code ?? '', libelle: unite?.libelle ?? '', type: unite?.type ?? 'SERVICE' as TypeUnite, parent: unite?.parent ?? parentInitial ?? '', responsableId: unite?.responsableId ?? '' }))
  const [v, setV] = useState(initiale)
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const champ = (k: keyof typeof v) => (x: string) => setV((o) => ({ ...o, [k]: x }))
  // Parent : une unité active, ni l'unité elle-même ni l'une de ses sous-unités ; aucun pour une direction.
  const descendants = (code: string): string[] => unites.filter((u) => u.parent === code).flatMap((u) => [u.code, ...descendants(u.code)])
  const exclus = unite ? [unite.code, ...descendants(unite.code)] : []
  const parents = enArbre(unites.filter((u) => u.active && !exclus.includes(u.code)))
  const soumettre = async () => {
    setErreurs({})
    setErreur(null)
    const code = v.code.trim().toUpperCase()
    const local: Record<string, string> = {}
    if (!/^[A-Z][A-Z0-9_]{1,31}$/.test(code)) local.code = t('console.org.unite.erreur.code')
    if (!v.libelle.trim()) local.libelle = t('console.org.unite.erreur.libelle')
    if (v.type !== 'DIRECTION' && !v.parent) local.parent = t('console.org.unite.erreur.parent')
    if (Object.keys(local).length) return setErreurs(local)
    try {
      onFait(await enregistrer.mutateAsync({ code, libelle: v.libelle.trim(), type: v.type, parent: v.type === 'DIRECTION' ? null : v.parent || null, active: unite?.active ?? true, responsableId: v.responsableId || null }))
    } catch (x) {
      const m = message(x)
      const details = (x as { details?: { champ?: string } }).details
      const c = m.code === 'ORG_UNITE_INVALIDE' ? CHAMPS_UNITE[details?.champ ?? ''] : m.code === 'ORG_UNITE_EXISTANTE' ? 'code' : m.code === 'ORG_UNITE_INCONNUE' || m.code === 'ORG_CYCLE' ? 'parent'
        : m.code === 'ORG_RESPONSABLE_INVALIDE' ? 'responsableId' : undefined
      if (c) setErreurs({ [c]: m.texte })
      else setErreur(m)
    }
  }
  return (
    <CrudDialog
      context={unite ? 'edit' : 'create'}
      size="md"
      id="gm-dlg-unite"
      title={t(unite ? 'console.org.unite.modifier' : 'console.org.unite.nouvelle')}
      subject={unite ? <span className="gm-mono">{unite.code}</span> : undefined}
      description={t('console.org.unite.texte')}
      onClose={onFermer}
      onSubmit={() => void soumettre()}
      submitLabel={t(unite ? 'console.org.unite.enregistrer' : 'console.org.unite.creer')}
      submitting={enregistrer.isPending}
      dirty={JSON.stringify(v) !== JSON.stringify(initiale)}
      error={erreur ? { title: erreur.texte, correlationId: erreur.correlationId } : undefined}
    >
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <TextField name="code" label={t('console.org.unite.code')} help={t('console.org.unite.codeAide')} mono required maxLength={32} readOnly={!!unite}
            value={v.code} onChange={(x) => champ('code')(x.toUpperCase())} error={erreurs.code} />
        </div>
        <div className="gm-form-field">
          <Select name="type" label={t('console.org.unite.type')} value={v.type} onChange={champ('type')} options={TYPES_UNITE.map((x) => ({ value: x, label: t(`console.org.type.${x}`) }))} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <TextField name="libelle" label={t('console.org.unite.libelle')} required maxLength={120} value={v.libelle} onChange={champ('libelle')} error={erreurs.libelle} />
        </div>
        {v.type !== 'DIRECTION' ? (
          <div className="gm-form-field gm-form-wide">
            <Select name="parent" label={t('console.org.unite.parent')} required value={v.parent} onChange={champ('parent')} error={erreurs.parent}
              options={[{ value: '', label: t('console.org.unite.choisirParent') }, ...parents.map(({ unite: u, profondeur }) => ({ value: u.code, label: `${'  '.repeat(profondeur)}${u.libelle}` }))]} />
          </div>
        ) : null}
        <div className="gm-form-field gm-form-wide">
          <Select name="responsableId" label={t('console.org.unite.responsable')} help={t('console.org.unite.responsableAide')} value={v.responsableId} onChange={champ('responsableId')} error={erreurs.responsableId}
            options={[{ value: '', label: t('console.org.unite.sansResponsable') }, ...responsables.map((c) => ({ value: c.id, label: `${c.nomAffiche}${c.fonction ? ` · ${c.fonction}` : ''}` })),
              ...(v.responsableId && !responsables.some((c) => c.id === v.responsableId) ? [{ value: v.responsableId, label: v.responsableId }] : [])]} />
        </div>
      </div>
    </CrudDialog>
  )
}
