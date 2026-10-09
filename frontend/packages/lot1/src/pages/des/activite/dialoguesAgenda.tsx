import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, ConfirmDialog, CrudDialog, DateField, KeyValueList, Note, Select, StatusBadge, TextArea, TextField } from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import type { Correspondant } from '../../../modele/messagerie'
import { CATEGORIES_EVENEMENT, type CategorieEvenement, type Evenement } from '../../../modele/pilotage'
import { ajouterJours, depuisSaisie, heureDe, heureValide, instant, jourDe, versSaisie } from '../../../modele/jours'
import { useCreerEvenement, useModifierEvenement, useRepondre, useSupprimerEvenement } from '../../../hooks/pilotage'
import { Annuaire } from '../messagerie/MessageriePage'
import { TagCategorie, pourDialogue, useDates, useErreur } from './communs'

// Agenda (D-76 §2.1) : création et modification d'un événement (catégorie, horaires ou journée entière,
// lieu, participants choisis dans l'annuaire de la messagerie), fiche d'un événement, réponse à une
// invitation, suppression par le propriétaire. Les horaires sont ceux de Douala.

const PARTICIPANTS_MAX = 50

export function DialogueEvenement({ jour, evenement, onFermer, onFait }: { jour: string; evenement?: Evenement; onFermer: () => void; onFait: (message: string) => void }) {
  const { t } = useTranslation()
  const session = useSession()
  const erreur = useErreur()
  const creer = useCreerEvenement()
  const modifier = useModifierEvenement()
  const geste = evenement ? modifier : creer
  const [initiale] = useState(() => ({
    titre: evenement?.titre ?? '', categorie: evenement?.categorie ?? 'REUNION' as CategorieEvenement, journeeEntiere: evenement?.journeeEntiere ?? false,
    debut: versSaisie(evenement ? jourDe(evenement.debut) : jour),
    fin: versSaisie(evenement ? (evenement.journeeEntiere ? ajouterJours(jourDe(evenement.fin), -1) : jourDe(evenement.fin)) : jour),
    heureDebut: evenement && !evenement.journeeEntiere ? heureDe(evenement.debut) : '09:00',
    heureFin: evenement && !evenement.journeeEntiere ? heureDe(evenement.fin) : '10:00',
    lieu: evenement?.lieu ?? '', description: evenement?.description ?? '',
  }))
  const [v, setV] = useState(initiale)
  const [participants, setParticipants] = useState<Correspondant[]>(() => (evenement?.participants ?? []).map((p) => ({ id: p.id, nom: p.nomAffiche, fonction: null })))
  const [tente, setTente] = useState(false)
  const champ = <K extends keyof typeof v>(k: K) => (x: (typeof v)[K]) => setV((o) => ({ ...o, [k]: x }))
  const debut = depuisSaisie(v.debut)
  const finJour = v.journeeEntiere ? depuisSaisie(v.fin) : debut
  const bornes = debut && finJour && (v.journeeEntiere || (heureValide(v.heureDebut) && heureValide(v.heureFin)))
    ? v.journeeEntiere ? [instant(debut, '00:00'), instant(ajouterJours(finJour, 1), '00:00')] : [instant(debut, v.heureDebut.trim()), instant(debut, v.heureFin.trim())]
    : null
  const erreurs = {
    titre: !v.titre.trim() ? t('act.evenement.erreur.titre') : undefined,
    debut: !debut ? t('act.erreur.date') : undefined,
    fin: v.journeeEntiere && !finJour ? t('act.erreur.date') : undefined,
    heures: !v.journeeEntiere && !(heureValide(v.heureDebut) && heureValide(v.heureFin)) ? t('act.evenement.erreur.heure') : undefined,
    ordre: bornes && new Date(bornes[1]).getTime() <= new Date(bornes[0]).getTime() ? t('act.evenement.erreur.ordre') : undefined,
    participants: participants.length > PARTICIPANTS_MAX ? t('act.evenement.erreur.participants', { max: PARTICIPANTS_MAX }) : undefined,
  }
  const soumettre = () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean) || !bornes) return
    const entree = {
      titre: v.titre.trim(), categorie: v.categorie, journeeEntiere: v.journeeEntiere, debut: bornes[0], fin: bornes[1], lieu: v.lieu.trim() || null,
      description: v.description.trim() || null, participants: participants.map((p) => p.id),
    }
    if (evenement) modifier.mutate({ id: evenement.id, e: entree }, { onSuccess: () => onFait(t('act.evenement.modifie')) })
    else creer.mutate(entree, { onSuccess: () => onFait(t('act.evenement.cree')) })
  }
  const e = (k: keyof typeof erreurs) => (tente ? erreurs[k] : undefined)
  return (
    <CrudDialog
      context={evenement ? 'edit' : 'create'}
      id="gm-dlg-evenement"
      title={t(evenement ? 'act.evenement.modifier' : 'act.evenement.nouveau')}
      subject={evenement?.titre}
      onClose={onFermer}
      onSubmit={soumettre}
      submitLabel={t(evenement ? 'act.evenement.enregistrer' : 'act.evenement.creer')}
      submitting={geste.isPending}
      dirty={JSON.stringify(v) !== JSON.stringify(initiale)}
      error={pourDialogue(erreur(geste.error))}
    >
      <TextField name="titre" label={t('act.evenement.titre')} required maxLength={200} value={v.titre} onChange={champ('titre')} error={e('titre')} />
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <Select name="categorie" label={t('act.evenement.categorie')} value={v.categorie} onChange={(x) => champ('categorie')(x as CategorieEvenement)}
            options={CATEGORIES_EVENEMENT.map((c) => ({ value: c, label: t(`act.categorie.${c}`) }))} />
        </div>
        <div className="gm-form-field" style={{ alignSelf: 'end' }}>
          <Checkbox name="journeeEntiere" label={t('act.evenement.journeeEntiere')} checked={v.journeeEntiere} onChange={champ('journeeEntiere')} />
        </div>
        <div className="gm-form-field"><DateField name="debut" label={t(v.journeeEntiere ? 'act.evenement.du' : 'act.evenement.date')} required value={v.debut} onChange={champ('debut')} error={e('debut')} /></div>
        {v.journeeEntiere ? (
          <div className="gm-form-field"><DateField name="fin" label={t('act.evenement.au')} required value={v.fin} onChange={champ('fin')} error={e('fin') ?? e('ordre')} /></div>
        ) : (
          <div className="gm-form-field">
            <div className="gm-row" style={{ gap: 'var(--gm-space-2)', alignItems: 'flex-start', flexWrap: 'nowrap' }}>
              <TextField name="heureDebut" label={t('act.evenement.heureDebut')} inputMode="numeric" maxLength={5} value={v.heureDebut} onChange={champ('heureDebut')} error={e('heures')} />
              <TextField name="heureFin" label={t('act.evenement.heureFin')} inputMode="numeric" maxLength={5} value={v.heureFin} onChange={champ('heureFin')} error={e('ordre')} />
            </div>
          </div>
        )}
      </div>
      <TextField name="lieu" label={t('act.evenement.lieu')} maxLength={200} value={v.lieu} onChange={champ('lieu')} />
      <TextArea name="description" label={t('act.evenement.description')} rows={2} maxLength={2000} value={v.description} onChange={champ('description')} />
      <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
        <span className="gm-field-label">{t('act.evenement.participants')}</span>
        <span className="gm-field-help">{t('act.evenement.participantsAide', { max: PARTICIPANTS_MAX })}</span>
        {/* QUO-17 : les personnes choisies ne s'affichent qu'une fois, dans l'annuaire (« N personnes choisies »). */}
        {e('participants') ? <span className="gm-field-error" role="alert">{e('participants')}</span> : null}
        <Annuaire choisis={participants} onChoisis={setParticipants} multiple exclus={session?.sujet ? [session.sujet] : []} />
      </div>
    </CrudDialog>
  )
}

const STATUT_REPONSE = { EN_ATTENTE: 'neutral', ACCEPTE: 'ok', DECLINE: 'warn' } as const

/** Fiche d'un événement : horaires, lieu, participants et leur réponse ; gestes du propriétaire ou de l'invité. */
export function DetailEvenement({ evenement: x, onFermer, onModifier, onFait }: { evenement: Evenement; onFermer: () => void; onModifier: () => void; onFait: (message: string) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const session = useSession()
  const repondre = useRepondre()
  const supprimer = useSupprimerEvenement()
  const [confirmer, setConfirmer] = useState(false)
  const maReponse = x.participants.find((p) => p.id === session?.sujet)?.reponse
  const horaires = x.journeeEntiere
    ? ajouterJours(jourDe(x.fin), -1) === jourDe(x.debut) ? t('act.evenement.journee', { jour: dates.jourLong(jourDe(x.debut)) })
      : t('act.evenement.periode', { du: dates.jourLong(jourDe(x.debut)), au: dates.jourLong(ajouterJours(jourDe(x.fin), -1)) })
    : t('act.evenement.horaires', { jour: dates.jourLong(jourDe(x.debut)), debut: dates.heure(x.debut), fin: dates.heure(x.fin) })
  const refus = erreur(repondre.error ?? supprimer.error)
  if (confirmer) {
    return (
      <ConfirmDialog danger irreversible title={t('act.evenement.supprimer')} description={t('act.evenement.supprimerTexte')} confirmLabel={t('act.evenement.supprimer')}
        object={[{ key: t('act.evenement.titre'), value: x.titre }, { key: t('act.evenement.quand'), value: horaires }]}
        onClose={() => setConfirmer(false)} onConfirm={() => supprimer.mutate(x.id, { onSuccess: () => onFait(t('act.evenement.supprime')), onError: () => setConfirmer(false) })} />
    )
  }
  return (
    <CrudDialog
      context="detail"
      id="gm-dlg-detail-evenement"
      title={x.titre}
      subject={horaires}
      onClose={onFermer}
      error={refus ? { title: refus.titre, correlationId: refus.correlationId } : undefined}
      extraActions={x.moi === 'PROPRIETAIRE' ? (
        <>
          <Button variant="danger" icon="trash" onClick={() => setConfirmer(true)}>{t('act.evenement.supprimer')}</Button>
          <Button variant="primary" icon="pen-line" onClick={onModifier}>{t('act.evenement.modifier')}</Button>
        </>
      ) : x.moi === 'PARTICIPANT' && maReponse ? (
        <>
          <Button variant="secondary" icon="x" disabled={maReponse === 'DECLINE'} disabledReason={t('act.evenement.dejaDecline')} loading={repondre.isPending && repondre.variables?.reponse === 'DECLINE'}
            onClick={() => repondre.mutate({ id: x.id, reponse: 'DECLINE' }, { onSuccess: () => onFait(t('act.evenement.decline')) })}>{t('act.evenement.decliner')}</Button>
          <Button variant="primary" icon="check" disabled={maReponse === 'ACCEPTE'} disabledReason={t('act.evenement.dejaAccepte')} loading={repondre.isPending && repondre.variables?.reponse === 'ACCEPTE'}
            onClick={() => repondre.mutate({ id: x.id, reponse: 'ACCEPTE' }, { onSuccess: () => onFait(t('act.evenement.accepte')) })}>{t('act.evenement.accepter')}</Button>
        </>
      ) : undefined}
    >
      <KeyValueList
        items={[
          { key: t('act.evenement.categorie'), value: <TagCategorie categorie={x.categorie} /> },
          { key: t('act.evenement.proprietaire'), value: x.proprietaire.nomAffiche },
          ...(x.lieu ? [{ key: t('act.evenement.lieu'), value: x.lieu }] : []),
          ...(x.description ? [{ key: t('act.evenement.description'), value: x.description }] : []),
        ]}
      />
      {x.moi === 'EQUIPE' ? <Note>{t('act.evenement.lectureEquipe')}</Note> : null}
      {x.participants.length ? (
        <ul className="gm-col" aria-label={t('act.evenement.participants')} style={{ listStyle: 'none', margin: 0, padding: 0, gap: 'var(--gm-space-1)' }}>
          {x.participants.map((p) => (
            <li key={p.id} className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
              <span>{p.nomAffiche}</span>
              <StatusBadge status={STATUT_REPONSE[p.reponse]} icon={null} label={t(`act.evenement.reponse.${p.reponse}`)} />
            </li>
          ))}
        </ul>
      ) : null}
    </CrudDialog>
  )
}
