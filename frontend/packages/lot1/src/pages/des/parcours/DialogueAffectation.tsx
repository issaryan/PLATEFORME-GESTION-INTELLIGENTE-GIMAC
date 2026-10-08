import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CrudDialog, InlineAlert, KeyValueList, Select } from '@gimac/design-system'
import type { DossierLiaison, Fonction } from '../../../modele/parcours'
import { useAffecterRessources, usePersonnes } from '../../../hooks/parcours'
import { useErreurService } from '../liaisons/communs'

// Affectation des ressources du projet (vue AFFECTATION, D-93) par les chefs des deux départements : une personne par
// fonction interne que tient le RACI de la liaison (intervenants servis), sans liste figée de fonctions. Le conducteur
// est désigné à part (D-58). Le service vérifie l'ouverture de l'étape et les habilitations.

/** Fonctions qui ne s'affectent pas ici (D-93 §8.1 : 422 HOM_FONCTION_NON_AFFECTABLE). */
export const NON_AFFECTABLES = ['PARTICIPANT', 'CONDUCTEUR']

function ChoixPersonne({ fonction, libelle, valeur, onChange }: { fonction: Fonction; libelle: string; valeur: string; onChange: (v: string) => void }) {
  const { t } = useTranslation()
  const personnes = usePersonnes(fonction)
  return (
    <Select
      name={`affectation-${fonction}`}
      label={libelle}
      value={valeur}
      options={[{ value: '', label: t('parcoursAffectation.nonAffecte') }, ...(personnes.data ?? []).map((p) => ({ value: p.id, label: p.nom }))]}
      disabled={personnes.isPending}
      onChange={onChange}
    />
  )
}

export function DialogueAffectation({ dossier, onFermer, onAffecte }: { dossier: DossierLiaison; onFermer: () => void; onAffecte: () => void }) {
  const { t } = useTranslation()
  const affecter = useAffecterRessources()
  const erreurService = useErreurService()
  // Fonctions internes du RACI de la liaison (D-93 §8.5), personne déjà désignée proposée d'emblée.
  const fonctions = (dossier.intervenants ?? []).filter((i) => !i.externe && !NON_AFFECTABLES.includes(i.fonction))
  const [choix, setChoix] = useState<Partial<Record<Fonction, string>>>(() => Object.fromEntries(fonctions.filter((i) => i.personne && !i.aAffecter).map((i) => [i.fonction, i.personne!.id])))
  const [tente, setTente] = useState(false)
  const affectations = fonctions.flatMap(({ fonction }) => (choix[fonction] ? [{ fonction, personneId: choix[fonction] as string }] : []))
  const refus = erreurService(affecter.error)
  const soumettre = () => {
    setTente(true)
    if (!affectations.length) return
    affecter.mutate({ liaisonId: dossier.id, affectations }, { onSuccess: onAffecte })
  }
  return (
    <CrudDialog
      context="edit"
      size="lg"
      id="gm-affectation"
      title={t('parcoursAffectation.titre')}
      description={t('parcoursAffectation.description')}
      onClose={onFermer}
      onSubmit={soumettre}
      submitLabel={t('parcoursAffectation.valider')}
      submitIcon="users"
      submitting={affecter.isPending}
      subject={`${dossier.reference} · ${dossier.institutionNom}`}
    >
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('parcoursDossier.reference'), value: dossier.reference, mono: true },
            { key: t('parcoursDossier.conducteur'), value: dossier.conducteur?.nom ?? t('parcoursListe.aDesigner') },
          ]}
        />
        {!dossier.conducteur ? <InlineAlert level="warn">{t('parcoursAffectation.sansConducteur')}</InlineAlert> : null}
        {refus ? (
          <InlineAlert level="crit" title={t('parcoursAffectation.refus')} correlationId={refus.correlationId || undefined}>
            {refus.message}
          </InlineAlert>
        ) : null}
        {tente && !affectations.length ? <InlineAlert level="warn">{t('parcoursAffectation.erreur.aucune')}</InlineAlert> : null}
        {fonctions.length ? null : <InlineAlert level="info">{t('parcoursAffectation.aucuneFonction')}</InlineAlert>}
        <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-4)' }}>
          {fonctions.map(({ fonction, libelle }) => (
            <div key={fonction} style={{ flex: '1 1 240px', minWidth: 0 }}>
              <ChoixPersonne fonction={fonction} libelle={libelle} valeur={choix[fonction] ?? ''} onChange={(v) => setChoix((c) => ({ ...c, [fonction]: v }))} />
            </div>
          ))}
        </div>
      </div>
    </CrudDialog>
  )
}
