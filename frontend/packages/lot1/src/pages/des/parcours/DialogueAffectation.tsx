import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CrudDialog, InlineAlert, KeyValueList, Select } from '@gimac/design-system'
import type { DossierLiaison, Fonction } from '../../../modele/parcours'
import { useAffecterRessources, usePersonnes } from '../../../hooks/parcours'
import { useErreurService } from '../liaisons/communs'

// Étape 4, rang 6 : affectation des ressources du projet par les chefs des deux départements, une personne
// par fonction (CR §7.2.2 : responsable fonctionnel, responsable technique, et les intervenants des pôles).
// Le conducteur est désigné à part (D-58). Le service vérifie l'ouverture de l'étape et les habilitations.

/** Fonctions affectées sur la liaison ; les deux premières sont les responsables exigés du dossier numérique. */
const FONCTIONS: { fonction: Fonction; requise: boolean }[] = [
  { fonction: 'DDRM', requise: true },
  { fonction: 'SNA', requise: true },
  { fonction: 'POLE_RESEAU', requise: false },
  { fonction: 'POLE_SYSTEME', requise: false },
  { fonction: 'PRODUCTION_MONETIQUE', requise: false },
  { fonction: 'RSSI', requise: false },
  { fonction: 'EXPLOITATION', requise: false },
]

function ChoixPersonne({ fonction, requise, valeur, erreur, onChange }: { fonction: Fonction; requise: boolean; valeur: string; erreur?: string; onChange: (v: string) => void }) {
  const { t } = useTranslation()
  const personnes = usePersonnes(fonction)
  const role = fonction === 'DDRM' ? t('parcoursAffectation.responsableFonctionnel') : fonction === 'SNA' ? t('parcoursAffectation.responsableTechnique') : t(`parcours.fonction.${fonction}`)
  return (
    <Select
      name={`affectation-${fonction}`}
      label={role}
      required={requise}
      help={fonction === 'DDRM' || fonction === 'SNA' ? t(`parcours.fonction.${fonction}`) : undefined}
      value={valeur}
      options={[{ value: '', label: t(requise ? 'parcoursOuverture.choisir' : 'parcoursAffectation.nonAffecte') }, ...(personnes.data ?? []).map((p) => ({ value: p.id, label: p.nom }))]}
      disabled={personnes.isPending}
      error={erreur}
      onChange={onChange}
    />
  )
}

export function DialogueAffectation({ dossier, onFermer, onAffecte }: { dossier: DossierLiaison; onFermer: () => void; onAffecte: () => void }) {
  const { t } = useTranslation()
  const affecter = useAffecterRessources()
  const erreurService = useErreurService()
  const [choix, setChoix] = useState<Partial<Record<Fonction, string>>>({
    ...(dossier.responsableFonctionnel ? { DDRM: dossier.responsableFonctionnel.id } : {}),
    ...(dossier.responsableTechnique ? { SNA: dossier.responsableTechnique.id } : {}),
  })
  const [tente, setTente] = useState(false)
  const manquantes = FONCTIONS.filter((f) => f.requise && !choix[f.fonction]).map((f) => f.fonction)
  const refus = erreurService(affecter.error)
  const soumettre = () => {
    setTente(true)
    if (manquantes.length) return
    const affectations = FONCTIONS.flatMap(({ fonction }) => (choix[fonction] ? [{ fonction, personneId: choix[fonction] as string }] : []))
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
        <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-4)' }}>
          {FONCTIONS.map(({ fonction, requise }) => (
            <div key={fonction} style={{ flex: '1 1 240px', minWidth: 0 }}>
              <ChoixPersonne
                fonction={fonction}
                requise={requise}
                valeur={choix[fonction] ?? ''}
                erreur={tente && manquantes.includes(fonction) ? t('parcoursAffectation.erreur.requise') : undefined}
                onChange={(v) => setChoix((c) => ({ ...c, [fonction]: v }))}
              />
            </div>
          ))}
        </div>
      </div>
    </CrudDialog>
  )
}
