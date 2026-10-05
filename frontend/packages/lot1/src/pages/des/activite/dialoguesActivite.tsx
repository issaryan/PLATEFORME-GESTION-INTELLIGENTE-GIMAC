import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CrudDialog, DateField, NumberField, TextArea, TextField } from '@gimac/design-system'
import type { Activite } from '../../../modele/pilotage'
import { depuisSaisie, jourOuvreSuivant, versSaisie } from '../../../modele/jours'
import { useChangerStatut, useCreerActivite, useModifierActivite } from '../../../hooks/pilotage'
import { pourDialogue, useDates, useErreur } from './communs'

// Fenêtres des activités du jour (D-76 §2.2) : ajout et modification (intitulé, description, temps passé,
// jour), report à un jour ultérieur (le service crée la copie « À faire » de ce jour).

const DUREE_MAX = 1440

export function DialogueActivite({ jour, activite, onFermer, onFait }: { jour: string; activite?: Activite; onFermer: () => void; onFait: (message: string) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const creer = useCreerActivite()
  const modifier = useModifierActivite()
  const [initiale] = useState(() => ({ titre: activite?.titre ?? '', description: activite?.description ?? '', duree: activite?.dureeMinutes?.toString() ?? '', jour: versSaisie(activite?.jour ?? jour) }))
  const [v, setV] = useState(initiale)
  const [tente, setTente] = useState(false)
  const champ = (k: keyof typeof v) => (x: string) => setV((o) => ({ ...o, [k]: x }))
  const jourIso = depuisSaisie(v.jour)
  const duree = v.duree.trim() === '' ? null : Number(v.duree.replace(',', '.'))
  const erreurs = {
    titre: !v.titre.trim() ? t('act.activite.erreur.titre') : undefined,
    duree: duree !== null && (!Number.isInteger(duree) || duree < 0 || duree > DUREE_MAX) ? t('act.activite.erreur.duree', { max: DUREE_MAX }) : undefined,
    jour: !jourIso ? t('act.erreur.date') : undefined,
  }
  const geste = activite ? modifier : creer
  const soumettre = () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean) || !jourIso) return
    const entree = { jour: jourIso, titre: v.titre.trim(), description: v.description.trim() || null, dureeMinutes: duree }
    if (activite) modifier.mutate({ id: activite.id, a: { ...entree, ordre: activite.ordre } }, { onSuccess: () => onFait(t('act.activite.modifiee')) })
    else creer.mutate(entree, { onSuccess: () => onFait(t('act.activite.ajoutee')) })
  }
  const e = (k: keyof typeof erreurs) => (tente ? erreurs[k] : undefined)
  return (
    <CrudDialog
      context={activite ? 'edit' : 'create'}
      size="md"
      id="gm-dlg-activite"
      title={t(activite ? 'act.activite.modifier' : 'act.activite.ajouter')}
      subject={dates.jourLong(jourIso ?? jour)}
      onClose={onFermer}
      onSubmit={soumettre}
      submitLabel={t(activite ? 'act.activite.enregistrer' : 'act.activite.ajouter')}
      submitting={geste.isPending}
      dirty={JSON.stringify(v) !== JSON.stringify(initiale)}
      error={pourDialogue(erreur(geste.error))}
    >
      <TextField name="titre" label={t('act.activite.titre')} required maxLength={200} value={v.titre} onChange={champ('titre')} error={e('titre')} />
      <TextArea name="description" label={t('act.activite.description')} rows={2} maxLength={2000} value={v.description} onChange={champ('description')} />
      <div className="gm-form-grid">
        <div className="gm-form-field"><DateField name="jour" label={t('act.activite.jour')} required value={v.jour} onChange={champ('jour')} error={e('jour')} /></div>
        <div className="gm-form-field">
          <NumberField name="duree" label={t('act.activite.duree')} unit="min" help={t('act.activite.dureeAide')} value={v.duree} onChange={champ('duree')} error={e('duree')} />
        </div>
      </div>
    </CrudDialog>
  )
}

/** Report d'une activité : jour ouvré suivant proposé ; la copie « À faire » est créée par le service. */
export function DialogueReport({ activite, onFermer, onFait }: { activite: Activite; onFermer: () => void; onFait: (message: string) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const changer = useChangerStatut()
  const [saisie, setSaisie] = useState(versSaisie(jourOuvreSuivant(activite.jour)))
  const [tente, setTente] = useState(false)
  const iso = depuisSaisie(saisie)
  const invalide = !iso ? t('act.erreur.date') : iso <= activite.jour ? t('act.activite.erreur.report') : undefined
  return (
    <CrudDialog
      context="confirm"
      id="gm-dlg-report"
      title={t('act.activite.reporter')}
      subject={activite.titre}
      description={t('act.activite.reporterTexte')}
      onClose={onFermer}
      onSubmit={() => {
        setTente(true)
        if (invalide || !iso) return
        changer.mutate({ id: activite.id, statut: 'REPORTEE', reporteeAu: iso }, { onSuccess: () => onFait(t('act.activite.reportee', { jour: dates.jourLong(iso) })) })
      }}
      submitLabel={t('act.activite.reporter')}
      submitIcon="calendar-clock"
      submitting={changer.isPending}
      error={pourDialogue(erreur(changer.error))}
    >
      <DateField name="reporteeAu" label={t('act.activite.reporteeAu')} required value={saisie} onChange={setSaisie} error={tente ? invalide : undefined} />
    </CrudDialog>
  )
}
