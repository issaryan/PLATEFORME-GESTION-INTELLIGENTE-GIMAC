import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { FileDropzone, CrudDialog, ResourceForm, type Option, type ResourceDescriptor } from '@gimac/design-system'
import { champsRefuses } from '@gimac/socle'
import type { ConditionsAdhesion, Institution, ModeAdhesion, PieceAdhesion } from '../../../modele/participant'
import { useDefinirConditions, useDeposerPiece, useEnregistrerInstitution, useModifierInstitution } from '../../../hooks/participant'
import { useErreurService } from '../liaisons/communs'
import { candidature, erreursDuService, verifierCandidature } from './saisie'

// Formulaires de la DDRM composés du catalogue (DES-06, DES-07) : prise de contact, données
// descriptives, conditions d'adhésion, dépôt d'une pièce, chacun dans une fenêtre modale ouverte sur la
// liste ou la fiche. Le service décide ; le formulaire affiche son refus.

const texte = (v: unknown) => (typeof v === 'string' && v.trim() !== '' ? v.trim() : null)

/**
 * DES-07 : prise de contact (création) ou modification des données descriptives. `onCree` (D-98, « Lancer une
 * certification ») : l'institution créée est rendue à l'appelant au lieu d'ouvrir sa fiche.
 */
export function FormulaireInstitution({ descripteur, institution, onFermer, onCree }: {
  descripteur: ResourceDescriptor<Institution>
  institution?: Institution
  onFermer: () => void
  onCree?: (i: Institution) => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const enregistrer = useEnregistrerInstitution()
  const modifier = useModifierInstitution()
  const erreur = useErreurService()
  // Contrôles avant envoi, puis refus du service par champ (fieldErrors), affichés sous le champ concerné.
  const [erreursSaisie, setErreursSaisie] = useState<Record<string, string>>({})
  const mutation = institution ? modifier : enregistrer
  const erreurs = { ...erreursDuService(champsRefuses(mutation.error), t), ...erreursSaisie }
  const valider = (v: Record<string, unknown>) => {
    const e = Object.fromEntries(Object.entries(verifierCandidature(v)).map(([champ, x]) => [champ, t(x.cle, x.valeurs)]))
    setErreursSaisie(e)
    return Object.keys(e).length === 0
  }
  if (institution) {
    return (
      <ResourceForm
        descriptor={descripteur}
        mode="edit"
        title={t('institution.form.modifier', { nom: institution.nom })}
        submitLabel={t('institution.action.modifier')}
        initial={{ ...institution }}
        presentation="dialog"
        submitting={modifier.isPending}
        errors={erreurs}
        serverError={erreur(modifier.error)}
        onCancel={onFermer}
        onSubmit={(v) => valider(v) && modifier.mutate({ id: institution.id, c: candidature(v) }, { onSuccess: onFermer })}
      />
    )
  }
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="create"
      title={t('institution.form.priseDeContact')}
      submitLabel={t('institution.action.enregistrer')}
      presentation="dialog"
      submitting={enregistrer.isPending}
      errors={erreurs}
      serverError={erreur(enregistrer.error)}
      onCancel={onFermer}
      onSubmit={(v) => valider(v) && enregistrer.mutate(candidature(v), { onSuccess: (i) => (onCree ? onCree(i as Institution) : void navigate(`/des/institutions/${(i as Institution).id}`)) })}
    />
  )
}

type ValeursConditions = Record<keyof ConditionsAdhesion, string>

/** Conditions de l'adhésion (CR-CERT rang 2) : code participant, mode, comptes et banque marraine. */
export function FormulaireConditions({ institution, marraines, onFermer }: { institution: Institution; marraines: Option[]; onFermer: () => void }) {
  const { t } = useTranslation()
  const definir = useDefinirConditions()
  const erreur = useErreurService()
  const descripteur: ResourceDescriptor<ValeursConditions> = {
    key: 'conditionsAdhesion',
    label: { singular: t('adhesion.conditions'), plural: t('adhesion.conditions'), gender: 'f' },
    idField: 'codeParticipant',
    titleField: 'codeParticipant',
    endpoints: { list: '', get: '' },
    permissions: { list: 'PAR_INSTITUTION_READ', read: 'PAR_INSTITUTION_READ', update: 'PAR_INSTITUTION_EDIT' },
    actions: [],
    fields: [
      { name: 'codeParticipant', label: t('institution.codeParticipant'), kind: 'mono', required: true, inForm: true, help: t('adhesion.codeParticipant.aide') },
      { name: 'mode', label: t('adhesion.mode'), kind: 'radio', required: true, inForm: true, options: ['DIRECT', 'INDIRECT'].map((v) => ({ value: v, label: t(`institution.mode.${v}`) })) },
      { name: 'compteReglement', label: t('adhesion.compteReglement'), kind: 'mono', inForm: true, help: t('adhesion.compteReglement.aide') },
      { name: 'compteCantonnement', label: t('adhesion.compteCantonnement'), kind: 'mono', inForm: true, help: t('adhesion.compteCantonnement.aide') },
      { name: 'banqueMarraineId', label: t('adhesion.banqueMarraine'), kind: 'select', inForm: true, options: marraines, help: t('adhesion.banqueMarraine.aide') },
    ],
  }
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="edit"
      title={t('adhesion.form.conditions', { nom: institution.nom })}
      submitLabel={t('adhesion.action.enregistrerConditions')}
      initial={{ codeParticipant: institution.codeParticipant ?? '', mode: institution.modeAdhesion ?? '' }}
      presentation="dialog"
      submitting={definir.isPending}
      serverError={erreur(definir.error)}
      onCancel={onFermer}
      onSubmit={(v) =>
        definir.mutate(
          {
            id: institution.id,
            c: {
              codeParticipant: (texte(v.codeParticipant) ?? '').toUpperCase(),
              mode: (texte(v.mode) ?? 'DIRECT') as ModeAdhesion,
              compteReglement: texte(v.compteReglement),
              compteCantonnement: texte(v.compteCantonnement),
              banqueMarraineId: texte(v.banqueMarraineId),
            },
          },
          { onSuccess: onFermer },
        )
      }
    />
  )
}

/** Dépôt d'une pièce du dossier d'adhésion : MS-DOCUMENT la conserve, MS-PARTICIPANT rattache sa référence. */
export function FormulaireDepot({ institutionId, piece, onFermer }: { institutionId: string; piece: PieceAdhesion; onFermer: () => void }) {
  const { t } = useTranslation()
  const deposer = useDeposerPiece()
  const erreur = useErreurService()
  const [fichier, setFichier] = useState<File | null>(null)
  const refus = erreur(deposer.error)
  return (
    <CrudDialog
      context="create"
      id="gm-dlg-piece"
      size="md"
      title={t('piece.form.depot', { piece: piece.libelle })}
      onClose={onFermer}
      onSubmit={() => fichier && deposer.mutate({ id: institutionId, code: piece.code, fichier }, { onSuccess: onFermer })}
      submitLabel={t('piece.action.deposer')}
      submitIcon="upload"
      submitting={deposer.isPending}
      submitDisabled={!fichier}
      submitDisabledReason={t('piece.form.fichierRequis')}
      error={refus ? { title: refus.message, correlationId: refus.correlationId || undefined } : undefined}
    >
      <FileDropzone
        label={t('piece.fichier')}
        required
        accept="application/pdf,image/png,image/jpeg"
        maxSize="20 Mo"
        help={t('piece.form.aide')}
        file={fichier ? { name: fichier.name, size: `${Math.max(1, Math.round(fichier.size / 1024))} Ko` } : undefined}
        state={deposer.isPending ? 'uploading' : fichier ? 'done' : 'idle'}
        onFile={setFichier}
        onRemove={() => setFichier(null)}
      />
    </CrudDialog>
  )
}
