import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Card, FileDropzone, InlineAlert, ResourceForm, type Option, type ResourceDescriptor } from '@gimac/design-system'
import type { CategorieInstitution, Candidature, ConditionsAdhesion, Institution, ModeAdhesion, PieceAdhesion } from '../../../modele/participant'
import { useDefinirConditions, useDeposerPiece, useEnregistrerInstitution, useModifierInstitution } from '../../../hooks/participant'
import { useErreurService } from '../liaisons/communs'

// Formulaires de la DDRM composés du catalogue (DES-06, DES-07) : prise de contact, données
// descriptives, conditions d'adhésion, dépôt d'une pièce. Le service décide ; le formulaire affiche son refus.

const texte = (v: unknown) => (typeof v === 'string' && v.trim() !== '' ? v.trim() : null)

const candidature = (v: Record<string, unknown>): Candidature => ({
  nom: texte(v.nom) ?? '',
  sigle: texte(v.sigle),
  pays: texte(v.pays) ?? '',
  categorie: (texte(v.categorie) ?? 'BANQUE') as CategorieInstitution,
  codeIdentification: texte(v.codeIdentification)?.toUpperCase() ?? null,
  codeBanqueBeac: texte(v.codeBanqueBeac)?.toUpperCase() ?? null,
  datePriseDeContact: texte(v.datePriseDeContact),
  originePriseDeContact: texte(v.originePriseDeContact),
  projetEnvisage: texte(v.projetEnvisage),
})

/** DES-07 : prise de contact (création) ou modification des données descriptives. */
export function FormulaireInstitution({ descripteur, institution, onFermer }: {
  descripteur: ResourceDescriptor<Institution>
  institution?: Institution
  onFermer: () => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const enregistrer = useEnregistrerInstitution()
  const modifier = useModifierInstitution()
  const erreur = useErreurService()
  if (institution) {
    return (
      <ResourceForm
        descriptor={descripteur}
        mode="edit"
        title={t('institution.form.modifier', { nom: institution.nom })}
        submitLabel={t('institution.action.modifier')}
        initial={{ ...institution }}
        serverError={erreur(modifier.error)}
        onCancel={onFermer}
        onSubmit={(v) => modifier.mutate({ id: institution.id, c: candidature(v) }, { onSuccess: onFermer })}
      />
    )
  }
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="create"
      title={t('institution.form.priseDeContact')}
      submitLabel={t('institution.action.enregistrer')}
      serverError={erreur(enregistrer.error)}
      onCancel={onFermer}
      onSubmit={(v) => enregistrer.mutate(candidature(v), { onSuccess: (i) => void navigate(`/des/institutions/${(i as Institution).id}`) })}
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
      { name: 'banqueMarraineId', label: t('adhesion.banqueMarraine'), kind: 'searchSelect', inForm: true, options: marraines, help: t('adhesion.banqueMarraine.aide') },
    ],
  }
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="edit"
      title={t('adhesion.form.conditions', { nom: institution.nom })}
      submitLabel={t('adhesion.action.enregistrerConditions')}
      initial={{ codeParticipant: institution.codeParticipant ?? '', mode: institution.modeAdhesion ?? '' }}
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
    <Card
      title={t('piece.form.depot', { piece: piece.libelle })}
      footer={
        <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
          <Button variant="secondary" onClick={onFermer}>{t('commun.annuler')}</Button>
          <Button
            variant="primary"
            icon="upload"
            loading={deposer.isPending}
            disabled={!fichier}
            disabledReason={t('piece.form.fichierRequis')}
            onClick={() => fichier && deposer.mutate({ id: institutionId, code: piece.code, fichier }, { onSuccess: onFermer })}
          >
            {t('piece.action.deposer')}
          </Button>
        </div>
      }
    >
      <div className="gm-col">
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
        {refus ? <InlineAlert level="crit" correlationId={refus.correlationId || undefined}>{refus.message}</InlineAlert> : null}
      </div>
    </Card>
  )
}
