import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { CrudDialog, Icon, InlineAlert, RadioGroup, Select } from '@gimac/design-system'
import type { TypeProjet } from '../../../modele/commun'
import { useInstitutions } from '../../../hooks/participant'
import { useConducteurs, useOuvrirLiaison } from '../../../hooks/parcours'
import { useErreurService } from '../liaisons/communs'

// Étape 3, cadrage (D-57, D-58) : un chef de département ouvre la liaison sur une institution adhérente et
// désigne le conducteur de projet dans le même acte. La banque partenaire (rang 4) est déclarée, ou son
// absence expressément mentionnée. Le service décide (doublon, éligibilité) ; le dialogue affiche son refus.

const TYPES: TypeProjet[] = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE']

/** Erreur d'un groupe de boutons radio : même anatomie que celle des champs du catalogue. */
function ErreurGroupe({ texte }: { texte?: string }) {
  if (!texte) return null
  return (
    <div className="gm-field-error" role="alert">
      <Icon name="octagon-alert" />
      {texte}
    </div>
  )
}

export function DialogueOuverture({ onFermer }: { onFermer: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const adherents = useInstitutions({ page: 1, taille: 200, statut: ['ADHERENT'] })
  const banques = useInstitutions({ page: 1, taille: 200, statut: ['ACTIF'] })
  const conducteurs = useConducteurs()
  const ouvrir = useOuvrirLiaison()
  const erreurService = useErreurService()
  const [institutionId, setInstitution] = useState('')
  const [typeProjet, setType] = useState('')
  const [portage, setPortage] = useState('')
  const [banqueId, setBanque] = useState('')
  const [conducteurId, setConducteur] = useState('')
  const [tente, setTente] = useState(false)

  const choisir = { value: '', label: t('parcoursOuverture.choisir') }
  const optionsAdherents = (adherents.data?.contenu ?? []).map((i) => ({ value: i.id, label: i.sigle ? `${i.nom} (${i.sigle})` : i.nom }))
  const optionsBanques = (banques.data?.contenu ?? []).filter((i) => i.id !== institutionId).map((i) => ({ value: i.id, label: i.nom }))
  const optionsConducteurs = (conducteurs.data ?? []).map((p) => ({ value: p.id, label: p.nom }))
  const erreurs = {
    institutionId: !institutionId ? t('parcoursOuverture.erreur.institution') : undefined,
    typeProjet: !typeProjet ? t('parcoursOuverture.erreur.type') : undefined,
    portage: !portage ? t('parcoursOuverture.erreur.portage') : undefined,
    banqueId: portage === 'AVEC' && !banqueId ? t('parcoursOuverture.erreur.banque') : undefined,
    conducteurId: !conducteurId ? t('parcoursOuverture.erreur.conducteur') : undefined,
  }
  const valide = Object.values(erreurs).every((e) => !e)
  const refus = erreurService(ouvrir.error)
  const aucunAdherent = adherents.isSuccess && optionsAdherents.length === 0

  const soumettre = () => {
    setTente(true)
    if (!valide) return
    ouvrir.mutate(
      { institutionId, typeProjet: typeProjet as TypeProjet, chefDeProjetId: conducteurId, banquePartenaireId: portage === 'AVEC' ? banqueId : null },
      { onSuccess: (l) => void navigate(`/des/parcours/${l.id}?nouvelle=1`) },
    )
  }
  const e = (cle: keyof typeof erreurs) => (tente ? erreurs[cle] : undefined)

  return (
    <CrudDialog
      context="create"
      size="lg"
      id="gm-ouverture"
      title={t('parcoursOuverture.titre')}
      description={t('parcoursOuverture.description')}
      onClose={onFermer}
      onSubmit={soumettre}
      submitLabel={t('parcoursOuverture.valider')}
      submitIcon="plus"
      submitting={ouvrir.isPending}
      submitDisabled={aucunAdherent} submitDisabledReason={t('parcoursOuverture.aucunAdherent')}
    >
      <div className="gm-col">
        {refus ? (
          <InlineAlert level="crit" title={t('parcoursOuverture.refus')} correlationId={refus.correlationId || undefined}>
            {refus.message}
          </InlineAlert>
        ) : null}
        {adherents.isError ? <InlineAlert level="crit" correlationId={adherents.error?.correlationId || undefined}>{t('parcoursOuverture.registreIndisponible')}</InlineAlert> : null}
        {aucunAdherent ? <InlineAlert level="info">{t('parcoursOuverture.aucunAdherent')}</InlineAlert> : null}
        <Select
          name="institutionId"
          label={t('parcoursOuverture.institution')}
          required
          help={t('parcoursOuverture.institution.aide')}
          value={institutionId}
          options={[choisir, ...optionsAdherents]}
          disabled={adherents.isPending || aucunAdherent}
          error={e('institutionId')}
          onChange={setInstitution}
        />
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <RadioGroup name="typeProjet" label={t('parcoursOuverture.type')} value={typeProjet} options={TYPES.map((v) => ({ value: v, label: t(`parcours.typeProjet.${v}`) }))} onChange={setType} />
          <ErreurGroupe texte={e('typeProjet')} />
        </div>
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <RadioGroup
            name="portage"
            label={t('parcoursOuverture.portage')}
            value={portage}
            options={[
              { value: 'AVEC', label: t('parcoursOuverture.portage.avec') },
              { value: 'SANS', label: t('parcoursOuverture.portage.sans') },
            ]}
            onChange={setPortage}
          />
          <span className="gm-field-help">{t('parcoursOuverture.portage.aide')}</span>
          <ErreurGroupe texte={e('portage')} />
        </div>
        {portage === 'AVEC' ? (
          banques.isSuccess && optionsBanques.length === 0 ? (
            <InlineAlert level="warn">{t('parcoursOuverture.aucuneBanque')}</InlineAlert>
          ) : (
            <Select
              name="banquePartenaireId"
              label={t('parcoursOuverture.banque')}
              required
              value={banqueId}
              options={[choisir, ...optionsBanques]}
              disabled={banques.isPending}
              error={e('banqueId')}
              onChange={setBanque}
            />
          )
        ) : null}
        <Select
          name="conducteurId"
          label={t('parcoursOuverture.conducteur')}
          required
          help={t('parcoursOuverture.conducteur.aide')}
          value={conducteurId}
          options={[choisir, ...optionsConducteurs]}
          disabled={conducteurs.isPending}
          error={e('conducteurId')}
          onChange={setConducteur}
        />
      </div>
    </CrudDialog>
  )
}
