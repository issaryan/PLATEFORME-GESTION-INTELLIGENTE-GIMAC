import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button, CheckboxGroup, CrudDialog, DataTable, DateField, InlineAlert, KeyValueList, Link, ProgressRing, RadioGroup, Select, Skeleton, TextArea, TextField, Timeline, type Column,
} from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import { LIMITES_GROUPE, PRIORITES, TRANSITIONS_TACHE, motifExige, type EtatTache, type PersonneActivite, type Priorite, type Tache } from '../../../modele/pilotage'
import { couverts, couvreUnites, rangPortee, type MesPouvoirs } from '../../../modele/pouvoirs'
import { depuisSaisie, versSaisie } from '../../../modele/jours'
import {
  useAttribuer, useCreerGroupe, useCreerTache, useGroupe, useModifierTache, usePrendre, useRelancer, useTache, useTransition, useUnites,
} from '../../../hooks/pilotage'
import { BadgePriorite, BadgeTache, pourDialogue, useDates, useErreur, useLibelleUnite } from './communs'

// Tâches (D-76 §2.3, CP PIL-04 et PIL-08 ; D-81 §6) : création pour soi ou, avec PIL_TASK_ASSIGN et selon la
// matrice des pouvoirs (TACHE_CONFIER), pour une personne, plusieurs personnes ou une unité de sa portée ;
// modification (créateur ou responsable) et réaffectation (créateur) ; transitions motivées quand le cycle de vie
// l'exige ; tâche à prendre (prise, attribution) ; relance ; fiche avec l'origine et l'historique.

/** Destination d'une nouvelle tâche (D-81 §6) : une personne (soi compris), plusieurs personnes ou une unité. */
export type Destination = 'personne' | 'plusieurs' | 'unite'

export function DialogueTache({ tache, pouvoirs, peutConfier, initiale: preremplie, onFermer, onFait }: {
  tache?: Tache; pouvoirs?: MesPouvoirs; peutConfier: boolean; initiale?: { destination?: Destination; responsableId?: string; uniteCible?: string }
  onFermer: () => void; onFait: (message: string) => void
}) {
  const { t } = useTranslation()
  const session = useSession()
  const erreur = useErreur()
  const libelleUnite = useLibelleUnite()
  const creer = useCreerTache()
  const creerGroupe = useCreerGroupe()
  const modifier = useModifierTache()
  const moi = session?.sujet ?? ''
  const membres = couverts(pouvoirs, 'TACHE_CONFIER').filter((m) => m.actif && m.id !== moi)
  const unites = peutConfier && couvreUnites(pouvoirs, 'TACHE_CONFIER') ? pouvoirs?.unitesDirigees ?? [] : []
  const reaffectable = peutConfier && (!tache || (tache.createur.id === moi && !!tache.responsable))
  const destinations: Destination[] = !tache && peutConfier ? ['personne', ...(membres.length >= 2 ? ['plusieurs' as const] : []), ...(unites.length ? ['unite' as const] : [])] : ['personne']
  const [initiale] = useState(() => ({
    titre: tache?.titre ?? '', description: tache?.description ?? '', echeance: tache?.echeance ? versSaisie(tache.echeance) : '', priorite: tache?.priorite ?? 'NORMALE' as Priorite,
    responsableId: tache?.responsable?.id ?? preremplie?.responsableId ?? moi, destination: preremplie?.destination ?? 'personne' as Destination,
    responsables: [] as string[], uniteCible: preremplie?.uniteCible ?? '',
  }))
  const [v, setV] = useState(initiale)
  const [tente, setTente] = useState(false)
  const champ = (k: keyof typeof v) => (x: string | string[]) => setV((o) => ({ ...o, [k]: x }))
  const echeance = v.echeance.trim() ? depuisSaisie(v.echeance) : null
  const destination = destinations.includes(v.destination) ? v.destination : 'personne'
  const erreurs = {
    titre: !v.titre.trim() ? t('act.tache.erreur.titre') : undefined,
    echeance: v.echeance.trim() && !echeance ? t('act.erreur.date') : undefined,
    responsables: destination === 'plusieurs' && (v.responsables.length < LIMITES_GROUPE.min || v.responsables.length > LIMITES_GROUPE.max)
      ? t('pvr.tache.erreur.plusieurs', { min: LIMITES_GROUPE.min, max: LIMITES_GROUPE.max }) : undefined,
    uniteCible: destination === 'unite' && !v.uniteCible ? t('pvr.tache.erreur.unite') : undefined,
  }
  const geste = tache ? modifier : destination === 'plusieurs' ? creerGroupe : creer
  const soumettre = () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean)) return
    const base = { titre: v.titre.trim(), description: v.description.trim() || null, echeance, priorite: v.priorite }
    if (tache) {
      const entree = { ...base, ...(reaffectable && v.responsableId !== tache.responsable?.id ? { responsableId: v.responsableId } : {}) }
      modifier.mutate({ id: tache.id, t: entree }, { onSuccess: () => onFait(t('act.tache.modifiee')) })
    } else if (destination === 'plusieurs') {
      creerGroupe.mutate({ ...base, responsables: v.responsables }, { onSuccess: (g) => onFait(t('pvr.tache.confieeGroupe', { count: g.taches.length })) })
    } else if (destination === 'unite') {
      creer.mutate({ ...base, uniteCible: v.uniteCible }, { onSuccess: () => onFait(t('pvr.tache.confieeUnite', { unite: libelleUnite(v.uniteCible) })) })
    } else {
      creer.mutate({ ...base, ...(reaffectable && v.responsableId !== moi ? { responsableId: v.responsableId } : {}) }, { onSuccess: () => onFait(t(v.responsableId !== moi ? 'act.tache.confiee' : 'act.tache.creee')) })
    }
  }
  const personnes: PersonneActivite[] = [{ id: moi, nomAffiche: t('act.tache.moiMeme') }, ...membres]
  if (tache?.responsable && !personnes.some((p) => p.id === tache.responsable!.id)) personnes.push(tache.responsable)
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
      </div>
      {destinations.length > 1 ? (
        <RadioGroup name="destination" label={t('pvr.tache.destination')} value={destination} onChange={champ('destination')}
          options={destinations.map((d) => ({ value: d, label: t(`pvr.tache.destination.${d}`) }))} />
      ) : null}
      {reaffectable && destination === 'personne' ? (
        <Select name="responsableId" label={t('act.tache.responsable')} help={t(tache ? 'act.tache.responsableAide' : 'pvr.tache.portee')} value={v.responsableId} onChange={champ('responsableId')}
          options={personnes.map((p) => ({ value: p.id, label: p.nomAffiche }))} />
      ) : null}
      {destination === 'plusieurs' ? (
        <CheckboxGroup id="gm-tache-responsables" label={t('pvr.tache.responsables')} value={v.responsables} onChange={champ('responsables')} error={e('responsables')}
          options={membres.map((m) => ({ value: m.id, label: `${m.nomAffiche}${m.unite ? ` · ${libelleUnite(m.unite)}` : ''}` }))} />
      ) : null}
      {destination === 'unite' ? (
        <Select name="uniteCible" label={t('pvr.tache.unite')} help={t('pvr.tache.uniteAide')} required value={v.uniteCible} onChange={champ('uniteCible')} error={e('uniteCible')}
          options={[{ value: '', label: t('pvr.tache.choisirUnite') }, ...unites.map((u) => ({ value: u, label: libelleUnite(u) }))]} />
      ) : null}
    </CrudDialog>
  )
}

/** Attribution d'une tâche à prendre à un membre de l'unité (créateur ou responsable de l'unité, D-81 §6). */
export function DialogueAttribution({ tache, pouvoirs, onFermer, onFait }: { tache: Tache; pouvoirs?: MesPouvoirs; onFermer: () => void; onFait: (message: string) => void }) {
  const { t } = useTranslation()
  const erreur = useErreur()
  const unites = useUnites()
  const attribuer = useAttribuer()
  const [choix, setChoix] = useState('')
  const [tente, setTente] = useState(false)
  const sous = (code: string): string[] => [code, ...(unites.data ?? []).filter((u) => u.parent === code).flatMap((u) => sous(u.code))]
  const perimetre = tache.uniteCible ? sous(tache.uniteCible) : []
  const membres = couverts(pouvoirs, 'TACHE_CONFIER').filter((m) => m.actif && m.unite && perimetre.includes(m.unite))
  return (
    <CrudDialog
      context="edit"
      id="gm-dlg-attribution"
      title={t('pvr.tache.attribuer')}
      subject={tache.titre}
      description={t('pvr.tache.attribuerTexte')}
      onClose={onFermer}
      onSubmit={() => {
        setTente(true)
        if (choix) attribuer.mutate({ id: tache.id, responsableId: choix }, { onSuccess: (x) => onFait(t('pvr.tache.attribuee', { nom: x.responsable?.nomAffiche ?? '' })) })
      }}
      submitLabel={t('pvr.tache.attribuer')}
      submitting={attribuer.isPending}
      dirty={!!choix}
      error={pourDialogue(erreur(attribuer.error))}
    >
      <Select name="responsableId" label={t('act.tache.responsable')} required value={choix} onChange={setChoix} error={tente && !choix ? t('pvr.tache.erreur.membre') : undefined}
        options={[{ value: '', label: t('pvr.tache.choisirMembre') }, ...membres.map((m) => ({ value: m.id, label: m.nomAffiche }))]} />
    </CrudDialog>
  )
}

/** Suivi d'un groupe de tâches confiées à plusieurs personnes : avancement et relance de chacune. */
export function DialogueGroupe({ groupeId, onFermer }: { groupeId: string; onFermer: () => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const groupe = useGroupe(groupeId)
  const relancer = useRelancer()
  const [relancee, setRelancee] = useState<string | null>(null)
  const g = groupe.data
  const pourcent = g && g.avancement.total ? Math.round((g.avancement.terminees / g.avancement.total) * 100) : 0
  const colonnes: Column<Tache>[] = [
    { key: 'responsable', header: t('act.tache.responsable'), render: (x) => x.responsable?.nomAffiche ?? '' },
    { key: 'etat', header: t('act.tache.etat'), render: (x) => <BadgeTache etat={x.etat} enRetard={x.enRetard} /> },
    { key: 'echeance', header: t('act.tache.echeance'), render: (x) => (x.echeance ? dates.jour(x.echeance) : t('act.tache.sansEcheance')) },
    {
      key: 'actions', header: '', type: 'actions', render: (x) => (x.etat === 'TERMINEE' || x.etat === 'ANNULEE' ? null : (
        <Button size="sm" variant="secondary" icon="bell-ring" aria-label={t('pvr.tache.relancerQui', { nom: x.responsable?.nomAffiche ?? '' })}
          // QUO-15 : un nouveau geste efface le retour du précédent (jamais un succès et un refus affichés ensemble).
          onClick={() => { setRelancee(null); relancer.mutate(x.id, { onSuccess: () => setRelancee(x.responsable?.nomAffiche ?? '') }) }}>{t('pvr.tache.relancer')}</Button>
      )),
    },
  ]
  return (
    <CrudDialog
      context="detail"
      size="lg"
      id="gm-dlg-groupe"
      title={g ? t('pvr.groupe.titre', { titre: g.titre }) : t('pvr.groupe.suivi')}
      subject={g ? t('pvr.groupe.creePar', { nom: g.createur.nomAffiche }) : undefined}
      onClose={onFermer}
      error={pourDialogue(erreur(relancer.error))}
    >
      {groupe.isError ? <InlineAlert level="crit" correlationId={groupe.error.correlationId || undefined}>{erreur(groupe.error)?.titre}</InlineAlert> : !g ? <Skeleton height={160} /> : (
        <>
          {relancee !== null ? <InlineAlert level="info">{t('pvr.tache.relanceeA', { nom: relancee })}</InlineAlert> : null}
          <div className="gm-row" style={{ gap: 'var(--gm-space-4)', alignItems: 'center', flexWrap: 'wrap' }}>
            <ProgressRing label={t('pvr.groupe.avancement')} value={pourcent} size={72} status={g.avancement.enRetard ? 'warn' : 'ok'} />
            <KeyValueList items={[
              { key: t('pvr.groupe.terminees'), value: t('pvr.groupe.sur', { faites: g.avancement.terminees, total: g.avancement.total }) },
              { key: t('pvr.groupe.enRetard'), value: String(g.avancement.enRetard) },
            ]} />
          </div>
          <DataTable columns={colonnes} rows={g.taches} rowKey={(x) => x.id} density="compact" />
        </>
      )}
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
export function DetailTache({ id, pouvoirs, onFermer, onModifier, onTransition, onAttribuer, onGroupe, onFait }: {
  id: string; pouvoirs?: MesPouvoirs; onFermer: () => void; onModifier: (t: Tache) => void; onTransition: (t: Tache, vers: EtatTache) => void
  onAttribuer?: (t: Tache) => void; onGroupe?: (groupeId: string) => void; onFait?: (message: string) => void
}) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const libelleUnite = useLibelleUnite()
  const session = useSession()
  const tache = useTache(id)
  const prendre = usePrendre()
  const relancer = useRelancer()
  const x = tache.data
  const moi = session?.sujet
  const responsable = !!x?.responsable && x.responsable.id === moi
  const createur = x?.createur.id === moi
  // Tâche d'une étape de processus (D-87 §5.3) : elle suit l'étape, ni terminée ni annulée à la main.
  // DDR-28 : la tâche d'une décision (transition de statut) se clôt quand le service prononce ou refuse.
  const deProcessus = x?.origine.type === 'PROCESSUS' || x?.origine.type === 'DECISION'
  const transitions = x ? TRANSITIONS_TACHE[x.etat].filter((v) => (responsable || (createur && v === 'ANNULEE')) && !(deProcessus && (v === 'TERMINEE' || v === 'ANNULEE'))) : []
  const aPrendre = x?.etat === 'A_PRENDRE'
  // Attribution : le créateur ou le responsable de l'unité visée ; relance : le créateur ou TACHE_SUIVRE (le service décide).
  const attribuable = aPrendre && (createur || (!!x?.uniteCible && !!pouvoirs?.unitesDirigees.includes(x.uniteCible)))
  const relancable = !!x?.responsable && !responsable && x.etat !== 'TERMINEE' && x.etat !== 'ANNULEE' && (createur || rangPortee(pouvoirs?.actions.TACHE_SUIVRE) > 0)
  const geste = prendre.error ? prendre : relancer
  return (
    <CrudDialog
      context="detail"
      id="gm-dlg-detail-tache"
      title={x?.titre ?? t('act.tache.fiche')}
      subject={x ? t('act.tache.creeePar', { nom: x.createur.nomAffiche, quand: dates.instant(x.creeeLe) }) : undefined}
      onClose={onFermer}
      error={pourDialogue(erreur(geste.error))}
      extraActions={x ? (
        <>
          {x.groupeId && onGroupe ? <Button variant="ghost" icon="users" onClick={() => onGroupe(x.groupeId!)}>{t('pvr.groupe.suivi')}</Button> : null}
          {relancable ? <Button variant="secondary" icon="bell-ring" disabled={relancer.isPending} onClick={() => relancer.mutate(x.id, { onSuccess: () => onFait?.(t('pvr.tache.relanceeA', { nom: x.responsable!.nomAffiche })) })}>{t('pvr.tache.relancer')}</Button> : null}
          {aPrendre && !createur ? <Button variant="primary" icon="check" disabled={prendre.isPending} onClick={() => prendre.mutate(x.id, { onSuccess: () => onFait?.(t('pvr.tache.prise')) })}>{t('pvr.tache.prendre')}</Button> : null}
          {attribuable && onAttribuer ? <Button variant="secondary" icon="user" onClick={() => onAttribuer(x)}>{t('pvr.tache.attribuer')}</Button> : null}
          {transitions.map((v) => (
            <Button key={v} variant={v === 'ANNULEE' ? 'danger' : 'secondary'} onClick={() => onTransition(x, v)}>{t(`act.tache.vers.${v}`)}</Button>
          ))}
          {(responsable || createur) && !aPrendre ? <Button variant="primary" icon="pen-line" onClick={() => onModifier(x)}>{t('act.tache.modifier')}</Button> : null}
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
              // DDR-16 : tâche offerte sans unité (décision, fonction) : « À prendre », sans deux-points orphelins.
              { key: t('act.tache.responsable'), value: x.responsable?.nomAffiche ?? (x.uniteCible ? t('pvr.tache.aPrendreUnite', { unite: libelleUnite(x.uniteCible) }) : t('pvr.tache.aPrendreSansUnite')) },
              ...(x.uniteCible ? [{ key: t('pvr.tache.unite'), value: libelleUnite(x.uniteCible) }] : []),
              ...(x.profilRequis ? [{ key: t('proc.tache.profilRequis'), value: t(`compte.profil.${x.profilRequis}`, { defaultValue: x.profilRequis }) }] : []),
              ...(x.permissionsRequises?.length ? [{ key: t('pvr.tache.permissionsRequisesCle'), value: x.permissionsRequises.map((p) => t(`droits.permission.${p}`, { defaultValue: p })).join(', ') }] : []),
              ...(x.profilsRequis?.length ? [{ key: t('pvr.tache.profilsRequisCle'), value: x.profilsRequis.map((p) => t(`compte.profil.${p}`, { defaultValue: p })).join(', ') }] : []),
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
