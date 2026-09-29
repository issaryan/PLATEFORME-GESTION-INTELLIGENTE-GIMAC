import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ResourceForm, type ResourceDescriptor } from '@gimac/design-system'
import type { Liaison, OperationInstance } from '../../../modele/homologation'
import { useAttribuerOperation, useDesignerChefDeProjet } from '../../../hooks/liaisons'
import { useErreurService } from './communs'

// Formulaires du lot composés de ResourceForm : désignation du conducteur de projet (D-58), attribution
// d'une opération (HOM-08). L'ouverture d'une liaison se fait depuis la fiche de l'institution
// (institutions/cadrage.tsx). Le service décide ; le formulaire affiche son refus.

const texte = (v: unknown) => (typeof v === 'string' ? v : '')

/** Désignation (ou remplacement motivé) du conducteur de projet d'une liaison (D-58). */
export function FormulaireChefDeProjet({ descripteur, liaison, onFermer }: { descripteur: ResourceDescriptor<Liaison>; liaison: Liaison; onFermer: () => void }) {
  const { t } = useTranslation()
  const designer = useDesignerChefDeProjet()
  const erreur = useErreurService()
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const avecMotif: ResourceDescriptor<Liaison> = {
    ...descripteur,
    fields: [
      ...descripteur.fields.filter((f) => f.name === 'chefDeProjetId'),
      // Le motif accompagne la désignation ; il n'est pas un attribut de la liaison.
      { name: 'motif' as keyof Liaison & string, label: t('cadrage.motif'), kind: 'textarea', inForm: 'edit', help: t('cadrage.motif.aide') },
    ],
  }
  return (
    <ResourceForm
      descriptor={avecMotif}
      mode="edit"
      title={t('liaison.form.chef', { institution: liaison.institutionNom })}
      submitLabel={t('liaison.action.designerChef')}
      initial={{ chefDeProjetId: liaison.chefDeProjetId, motif: '' }}
      errors={erreurs}
      serverError={erreur(designer.error)}
      onCancel={onFermer}
      onSubmit={(v) => {
        if (!texte(v.chefDeProjetId)) return setErreurs({ chefDeProjetId: t('cadrage.requis.conducteur') })
        setErreurs({})
        designer.mutate({ id: liaison.id, chefDeProjetId: texte(v.chefDeProjetId), motif: texte(v.motif).trim() || undefined }, { onSuccess: onFermer })
      }}
    />
  )
}

export function FormulaireAttribution({ descripteur, operation, onFermer }: { descripteur: ResourceDescriptor<OperationInstance>; operation: OperationInstance; onFermer: () => void }) {
  const { t } = useTranslation()
  const attribuer = useAttribuerOperation()
  const erreur = useErreurService()
  const avecMotif: ResourceDescriptor<OperationInstance> = {
    ...descripteur,
    fields: [
      ...descripteur.fields.filter((f) => f.inForm === 'edit' || f.inForm === true),
      // Champ de saisie propre au formulaire : le motif accompagne la désignation, il n'est pas un attribut de l'opération.
      { name: 'motif' as keyof OperationInstance & string, label: t('operation.attribution.motif'), kind: 'textarea', inForm: 'edit', required: true, help: t('operation.attribution.motif.aide') },
    ],
  }
  return (
    <ResourceForm
      descriptor={avecMotif}
      mode="edit"
      embedded
      submitLabel={t('operation.action.attribuer')}
      initial={{ responsableId: operation.responsableId ?? '', motif: '' }}
      serverError={erreur(attribuer.error)}
      onCancel={onFermer}
      onSubmit={(v) => attribuer.mutate({ id: operation.id, responsableId: texte(v.responsableId), motif: texte(v.motif) }, { onSuccess: onFermer })}
    />
  )
}
