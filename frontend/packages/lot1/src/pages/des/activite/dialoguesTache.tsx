import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CrudDialog, DateField, InlineAlert, KeyValueList, Link, Select, Skeleton, TextArea, TextField, Timeline } from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import { PRIORITES, TRANSITIONS_TACHE, motifExige, type EtatTache, type PersonneActivite, type Priorite, type Tache } from '../../../modele/pilotage'
import { depuisSaisie, versSaisie } from '../../../modele/jours'
import { useCreerTache, useModifierTache, useTache, useTransition } from '../../../hooks/pilotage'
import { BadgePriorite, BadgeTache, pourDialogue, useDates, useErreur } from './communs'

// Tâches (D-76 §2.3, CP PIL-04 et PIL-08) : création pour soi ou, avec PIL_TASK_ASSIGN, pour un membre de son
// équipe ; modification (créateur ou responsable) et réaffectation (créateur) ; transitions motivées quand
// le cycle de vie l'exige ; fiche avec l'origine (lien vers l'objet qui l'a fait naître) et l'historique.

export function DialogueTache({ tache, membres, peutConfier, onFermer, onFait }: {
  tache?: Tache; membres: PersonneActivite[]; peutConfier: boolean; onFermer: () => void; onFait: (message: string) => void
}) {
  const { t } = useTranslation()
  const session = useSession()
  const erreur = useErreur()
  const creer = useCreerTache()
  const modifier = useModifierTache()
  const geste = tache ? modifier : creer
  const moi = session?.sujet ?? ''
  const reaffectable = peutConfier && (!tache || tache.createur.id === moi)
  const [initiale] = useState(() => ({
    titre: tache?.titre ?? '', description: tache?.description ?? '', echeance: tache?.echeance ? versSaisie(tache.echeance) : '', priorite: tache?.priorite ?? 'NORMALE' as Priorite,
    responsableId: tache?.responsable.id ?? moi,
  }))
  const [v, setV] = useState(initiale)
  const [tente, setTente] = useState(false)
  const champ = (k: keyof typeof v) => (x: string) => setV((o) => ({ ...o, [k]: x }))
  const echeance = v.echeance.trim() ? depuisSaisie(v.echeance) : null
  const erreurs = {
    titre: !v.titre.trim() ? t('act.tache.erreur.titre') : undefined,
    echeance: v.echeance.trim() && !echeance ? t('act.erreur.date') : undefined,
  }
  const soumettre = () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean)) return
    const entree = {
      titre: v.titre.trim(), description: v.description.trim() || null, echeance, priorite: v.priorite,
      ...(reaffectable && v.responsableId !== (tache?.responsable.id ?? moi) ? { responsableId: v.responsableId } : {}),
    }
    if (tache) modifier.mutate({ id: tache.id, t: entree }, { onSuccess: () => onFait(t('act.tache.modifiee')) })
    else creer.mutate(entree, { onSuccess: () => onFait(t(v.responsableId !== moi ? 'act.tache.confiee' : 'act.tache.creee')) })
  }
  const personnes = [{ id: moi, nomAffiche: t('act.tache.moiMeme') }, ...membres.filter((m) => m.id !== moi)]
  if (tache && !personnes.some((p) => p.id === tache.responsable.id)) personnes.push(tache.responsable)
  const e = (k: keyof typeof erreurs) => (tente ? erreurs[k] : undefined)
  return (
    <CrudDialog
      context={tache ? 'edit' : 'create'}
      id="gm-dlg-tache"
      title={t(tache ? 'act.tache.modifier' : 'act.tache.nouvelle')}
      subject={tache?.titre}
      onClose={onFermer}
      onSubmit={soumettre}
      submitLabel={t(tache ? 'act.tache.enregistrer' : 'act.tache.creer')}
      submitting={geste.isPending}
      dirty={JSON.stringify(v) !== JSON.stringify(initiale)}
      error={pourDialogue(erreur(geste.error))}
    >
      <TextField name="titre" label={t('act.tache.titre')} required maxLength={200} value={v.titre} onChange={champ('titre')} error={e('titre')} />
      <TextArea name="description" label={t('act.tache.description')} rows={3} maxLength={4000} value={v.description} onChange={champ('description')} />
      <div className="gm-form-grid">
        <div className="gm-form-field"><DateField name="echeance" label={t('act.tache.echeance')} help={t('act.tache.echeanceAide')} value={v.echeance} onChange={champ('echeance')} error={e('echeance')} /></div>
        <div className="gm-form-field">
          <Select name="priorite" label={t('act.tache.priorite')} value={v.priorite} onChange={champ('priorite')} options={PRIORITES.map((p) => ({ value: p, label: t(`act.priorite.${p}`) }))} />
        </div>
        {reaffectable ? (
          <div className="gm-form-field">
            <Select name="responsableId" label={t('act.tache.responsable')} help={t('act.tache.responsableAide')} value={v.responsableId} onChange={champ('responsableId')}
              options={personnes.map((p) => ({ value: p.id, label: p.nomAffiche }))} />
          </div>
        ) : null}
      </div>
    </CrudDialog>
  )
}

/** Transition d'une tâche ; motif obligatoire (dix caractères au moins) pour BLOQUEE, ANNULEE et la réouverture. */
export function DialogueTransition({ tache, vers, onFermer, onFait }: { tache: Tache; vers: EtatTache; onFermer: () => void; onFait: (message: string) => void }) {
  const { t } = useTranslation()
  const erreur = useErreur()
  const transition = useTransition()
  const motif = motifExige(tache.etat, vers)
  return (
    <CrudDialog
      context="confirm"
      id="gm-dlg-transition"
      title={t(`act.tache.vers.${vers}`)}
      subject={tache.titre}
      description={t('act.tache.transitionTexte', { de: t(`act.tache.etat.${tache.etat}`), vers: t(`act.tache.etat.${vers}`) })}
      reason={motif}
      danger={vers === 'ANNULEE'}
      onClose={onFermer}
      onSubmit={(m) => transition.mutate({ id: tache.id, etat: vers, ...(motif ? { motif: m } : {}) }, { onSuccess: () => onFait(t('act.tache.transitionFaite', { etat: t(`act.tache.etat.${vers}`) })) })}
      submitLabel={t(`act.tache.vers.${vers}`)}
      submitting={transition.isPending}
      error={pourDialogue(erreur(transition.error))}
    />
  )
}

/** Fiche d'une tâche : état, échéance, responsable, origine, historique ; transitions et modification permises. */
export function DetailTache({ id, onFermer, onModifier, onTransition }: { id: string; onFermer: () => void; onModifier: (t: Tache) => void; onTransition: (t: Tache, vers: EtatTache) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const session = useSession()
  const tache = useTache(id)
  const x = tache.data
  const moi = session?.sujet
  const responsable = x?.responsable.id === moi
  const createur = x?.createur.id === moi
  const transitions = x ? TRANSITIONS_TACHE[x.etat].filter((v) => responsable || (createur && v === 'ANNULEE')) : []
  return (
    <CrudDialog
      context="detail"
      id="gm-dlg-detail-tache"
      title={x?.titre ?? t('act.tache.fiche')}
      subject={x ? t('act.tache.creeePar', { nom: x.createur.nomAffiche, quand: dates.instant(x.creeeLe) }) : undefined}
      onClose={onFermer}
      extraActions={x ? (
        <>
          {transitions.map((v) => (
            <Button key={v} variant={v === 'ANNULEE' ? 'danger' : 'secondary'} onClick={() => onTransition(x, v)}>{t(`act.tache.vers.${v}`)}</Button>
          ))}
          {responsable || createur ? <Button variant="primary" icon="pen-line" onClick={() => onModifier(x)}>{t('act.tache.modifier')}</Button> : null}
        </>
      ) : undefined}
    >
      {tache.isError ? <InlineAlert level="crit" correlationId={tache.error.correlationId || undefined}>{erreur(tache.error)?.titre}</InlineAlert> : !x ? <Skeleton height={200} /> : (
        <>
          <KeyValueList
            items={[
              { key: t('act.tache.etat'), value: <BadgeTache etat={x.etat} enRetard={x.enRetard} /> },
              { key: t('act.tache.priorite'), value: <BadgePriorite priorite={x.priorite} /> },
              { key: t('act.tache.echeance'), value: x.echeance ? dates.jourLong(x.echeance) : t('act.tache.sansEcheance') },
              { key: t('act.tache.responsable'), value: x.responsable.nomAffiche },
              { key: t('act.tache.origine'), value: (
                <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                  <span>{t(`act.tache.origine.${x.origine.type}`)}</span>
                  {x.origine.reference ? <span className="gm-mono">{x.origine.reference}</span> : null}
                  {x.origine.lien ? <Link href={x.origine.lien}>{t('act.tache.ouvrirOrigine')}</Link> : null}
                </span>
              ) },
              ...(x.description ? [{ key: t('act.tache.description'), value: x.description }] : []),
              ...(x.termineeLe ? [{ key: t('act.tache.termineeLe'), value: dates.instant(x.termineeLe) }] : []),
            ]}
          />
          <section aria-labelledby="gm-tache-historique" className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <h3 id="gm-tache-historique" className="gm-type-h3" style={{ margin: 0 }}>{t('act.tache.historique')}</h3>
            <Timeline events={(x.historique ?? []).map((h) => ({
              event: `${t(`act.tache.action.${h.action}`)}${h.detail ? ` : ${h.detail}` : ''}`, at: dates.instant(h.quand), actor: h.acteur.nomAffiche,
            }))} />
          </section>
        </>
      )}
    </CrudDialog>
  )
}
