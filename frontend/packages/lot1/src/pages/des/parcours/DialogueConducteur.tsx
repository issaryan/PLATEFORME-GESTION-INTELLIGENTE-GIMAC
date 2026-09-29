import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dialog, InlineAlert, KeyValueList, Select, TextArea } from '@gimac/design-system'
import type { Personne } from '../../../modele/parcours'
import { useConducteurs, useDesignerConducteur } from '../../../hooks/parcours'
import { useErreurService } from '../liaisons/communs'

// Désignation du conducteur de projet sur une liaison (D-58, rang 6) par un chef de département, parmi les
// détenteurs du rôle CONDUCTEUR_PROJET. Une redésignation est motivée et tracée ; le service décide.

const MOTIF_MIN = 10

export function DialogueConducteur({ liaison, onFermer, onDesigne }: {
  liaison: { id: string; reference: string; institutionNom: string; conducteur: Personne | null }
  onFermer: () => void
  onDesigne: (nom: string) => void
}) {
  const { t } = useTranslation()
  const conducteurs = useConducteurs()
  const designer = useDesignerConducteur()
  const erreurService = useErreurService()
  const redesignation = liaison.conducteur !== null
  const [conducteurId, setConducteur] = useState('')
  const [motif, setMotif] = useState('')
  const [tente, setTente] = useState(false)
  const options = (conducteurs.data ?? []).filter((p) => p.id !== liaison.conducteur?.id).map((p) => ({ value: p.id, label: p.nom }))
  const erreurs = {
    conducteur: !conducteurId ? t('parcoursOuverture.erreur.conducteur') : undefined,
    motif: redesignation && motif.trim().length < MOTIF_MIN ? t('parcoursConducteur.erreur.motif', { n: MOTIF_MIN }) : undefined,
  }
  const refus = erreurService(designer.error)
  const soumettre = () => {
    setTente(true)
    if (erreurs.conducteur || erreurs.motif) return
    const nom = options.find((o) => o.value === conducteurId)?.label ?? ''
    designer.mutate({ liaisonId: liaison.id, conducteurId, motif: motif.trim() }, { onSuccess: () => onDesigne(nom) })
  }
  const titre = t(redesignation ? 'parcoursConducteur.redesigner' : 'parcoursConducteur.designer')
  return (
    <Dialog
      open
      id="gm-conducteur"
      title={titre}
      description={t(redesignation ? 'parcoursConducteur.description.redesignation' : 'parcoursConducteur.description.designation')}
      onClose={onFermer}
      footer={
        <>
          <Button variant="secondary" onClick={onFermer}>{t('commun.annuler')}</Button>
          <Button variant="primary" icon="user" loading={designer.isPending} onClick={soumettre}>{titre}</Button>
        </>
      }
    >
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('parcoursDossier.reference'), value: liaison.reference, mono: true },
            { key: t('parcoursDossier.institution'), value: liaison.institutionNom },
            { key: t('parcoursConducteur.actuel'), value: liaison.conducteur?.nom ?? t('parcoursConducteur.aucun') },
          ]}
        />
        {refus ? (
          <InlineAlert level="crit" title={t('parcoursConducteur.refus')} correlationId={refus.correlationId || undefined}>
            {refus.message}
          </InlineAlert>
        ) : null}
        <Select
          name="conducteurId"
          label={t('parcoursConducteur.nouveau')}
          required
          help={t('parcoursOuverture.conducteur.aide')}
          value={conducteurId}
          options={[{ value: '', label: t('parcoursOuverture.choisir') }, ...options]}
          disabled={conducteurs.isPending}
          error={tente ? erreurs.conducteur : undefined}
          onChange={setConducteur}
        />
        {redesignation ? (
          <TextArea
            name="motif"
            label={t('parcoursConducteur.motif')}
            required
            help={t('parcoursConducteur.motif.aide', { n: MOTIF_MIN })}
            value={motif}
            error={tente ? erreurs.motif : undefined}
            onChange={setMotif}
          />
        ) : null}
      </div>
    </Dialog>
  )
}
