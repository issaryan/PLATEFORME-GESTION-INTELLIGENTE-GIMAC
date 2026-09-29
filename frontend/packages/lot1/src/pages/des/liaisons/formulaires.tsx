import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ResourceForm, type ResourceDescriptor } from '@gimac/design-system'
import type { TypeProjet } from '../../../modele/commun'
import type { Liaison, OperationInstance } from '../../../modele/homologation'
import { useAttribuerOperation, useDesignerChefDeProjet, useOuvrirLiaison } from '../../../hooks/liaisons'
import { useErreurService } from './communs'

// Formulaires du lot composés de ResourceForm : ouverture d'une liaison (HOM-04), désignation du chef de
// projet, attribution d'une opération (HOM-08). Le service décide ; le formulaire affiche son refus.

const texte = (v: unknown) => (typeof v === 'string' ? v : '')

export function FormulaireOuverture({ descripteur, onFermer }: { descripteur: ResourceDescriptor<Liaison>; onFermer: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const ouvrir = useOuvrirLiaison()
  const erreur = useErreurService()
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="create"
      title={t('liaison.form.ouvrir')}
      submitLabel={t('liaison.action.ouvrir')}
      serverError={erreur(ouvrir.error)}
      onCancel={onFermer}
      onSubmit={(v) =>
        ouvrir.mutate(
          {
            institutionId: texte(v.institutionId),
            typeProjet: texte(v.typeProjet) as TypeProjet,
            chefDeProjetId: texte(v.chefDeProjetId),
            banquePartenaireId: texte(v.banquePartenaireId) || null,
          },
          { onSuccess: (l) => void navigate(`/des/liaisons/${(l as Liaison).id}`) },
        )
      }
    />
  )
}

export function FormulaireChefDeProjet({ descripteur, liaison, onFermer }: { descripteur: ResourceDescriptor<Liaison>; liaison: Liaison; onFermer: () => void }) {
  const { t } = useTranslation()
  const designer = useDesignerChefDeProjet()
  const erreur = useErreurService()
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="edit"
      title={t('liaison.form.chef', { institution: liaison.institutionNom })}
      submitLabel={t('liaison.action.designerChef')}
      initial={{ chefDeProjetId: liaison.chefDeProjetId }}
      serverError={erreur(designer.error)}
      onCancel={onFermer}
      onSubmit={(v) => designer.mutate({ id: liaison.id, chefDeProjetId: texte(v.chefDeProjetId) }, { onSuccess: onFermer })}
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
