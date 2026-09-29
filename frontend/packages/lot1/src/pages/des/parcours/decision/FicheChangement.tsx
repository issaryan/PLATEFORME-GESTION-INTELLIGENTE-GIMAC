import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, FileDropzone, Hash, InlineAlert, KeyValueList, StatusBadge, TextField } from '@gimac/design-system'
import type { ApprobationSecurite, DossierLiaison } from '../../../../modele/parcours'
import { useConstituerDossierChangement, useReferencerFicheChangement, useRelireFicheChangement } from '../../../../hooks/parcours'
import { SousTitre, TON_FICHE_CHANGEMENT, useLancer, usePeut, Vide, type Action, PERMISSION_GESTE } from './commun'

// Verrou 2, rangs 20, 24 à 26 : dossier de changement constitué par la production monétique ; fiche de
// changement du dispositif existant référencée (CHG-AAAA-N), puis LUE, jamais refaite (D-56, D-16, RG-34) :
// son état d'approbation vient de ce dispositif et la plateforme en garde la date de lecture.

const taille = (octets: number) => (octets < 1024 * 1024 ? `${Math.max(1, Math.round(octets / 1024))} Ko` : `${(octets / 1024 / 1024).toFixed(1)} Mo`)

export function BlocDossierChangement({ dossier, approbation }: { dossier: DossierLiaison; approbation: ApprobationSecurite }) {
  const { t } = useTranslation()
  const peut = usePeut()
  const lancer = useLancer()
  const constituer = useConstituerDossierChangement()
  const [fichier, setFichier] = useState<File | null>(null)
  const d = approbation.dossierChangement
  const action: Action<{ fichier: File }> = {
    key: 'constituerDossier', permission: PERMISSION_GESTE.constituerDossier[0], label: 'decision.changement.action.constituer', icon: 'upload', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: (r) => <p style={{ margin: 0 }}>{t('decision.changement.constituer.consequence', { nom: r.fichier.name })}</p>,
    run: async ({ row }) => {
      await constituer.mutateAsync({ liaisonId: approbation.liaisonId, fichier: row.fichier })
      setFichier(null)
    },
  }
  return (
    <div className="gm-col">
      <SousTitre>{t('decision.changement.titre')}</SousTitre>
      {d ? (
        <KeyValueList
          items={[
            { key: t('decision.changement.piece'), value: <span className="gm-mono">{d.nom}</span> },
            { key: t('decision.changement.deposePar'), value: t('decision.changement.deposeLe', { nom: d.deposePar, date: d.deposeLe }) },
            { key: t('decision.pv.empreinte'), value: <Hash value={d.empreinte} /> },
          ]}
        />
      ) : peut(approbation.actionsPermises, 'CONSTITUER_DOSSIER', 'constituerDossier') ? (
        <div className="gm-col" style={{ maxWidth: 560 }}>
          <FileDropzone
            label={t('decision.changement.fichier')}
            required
            help={t('decision.changement.aide')}
            accept=".pdf"
            maxSize="20 Mo"
            file={fichier ? { name: fichier.name, size: taille(fichier.size) } : undefined}
            onFile={setFichier}
            onRemove={() => setFichier(null)}
          />
          <div className="gm-row">
            <Button
              variant="primary"
              icon="upload"
              disabled={!fichier}
              disabledReason={t('decision.changement.fichierRequis')}
              onClick={() => fichier && lancer(action, { fichier }, dossier.reference)}
            >
              {t('decision.changement.action.constituer')}
            </Button>
          </div>
        </div>
      ) : (
        <Vide texte={t(approbation.etat === 'EN_ATTENTE_DOSSIER' ? 'decision.changement.attendu' : 'commun.nonRenseigne')} />
      )}
    </div>
  )
}

export function BlocFicheChangement({ dossier, approbation }: { dossier: DossierLiaison; approbation: ApprobationSecurite }) {
  const { t } = useTranslation()
  const peut = usePeut()
  const lancer = useLancer()
  const referencer = useReferencerFicheChangement()
  const relire = useRelireFicheChangement()
  const [reference, setReference] = useState('')
  const f = approbation.ficheChangement
  const actionReferencer: Action<{ reference: string }> = {
    key: 'referencerFiche', permission: PERMISSION_GESTE.referencerFiche[0], label: 'decision.fiche.action.referencer', icon: 'paperclip', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: (r) => <p style={{ margin: 0 }}>{t('decision.fiche.referencer.consequence', { reference: r.reference })}</p>,
    run: async ({ row }) => {
      await referencer.mutateAsync({ liaisonId: approbation.liaisonId, reference: row.reference })
      setReference('')
    },
  }
  const actionRelire: Action<ApprobationSecurite> = {
    key: 'relireFiche', permission: PERMISSION_GESTE.relireFiche[0], label: 'decision.fiche.action.relire', icon: 'refresh', scope: 'row', confirm: 'none',
    run: () => relire.mutateAsync(approbation.liaisonId),
  }
  const visasEnAttente = approbation.visas.some((v) => v.actif && v.etat !== 'SIGNE_FAVORABLE')
  return (
    <div className="gm-col">
      <SousTitre>{t('decision.fiche.titre')}</SousTitre>
      <p className="gm-field-help" style={{ margin: 0 }}>{t('decision.fiche.explication')}</p>
      {f ? (
        <KeyValueList
          items={[
            { key: t('decision.fiche.reference'), value: <span className="gm-mono">{f.reference}</span> },
            { key: t('decision.fiche.etatApprobation'), value: <StatusBadge status={TON_FICHE_CHANGEMENT[f.etatApprobation] ?? 'neutral'} label={t(`parcours.ficheChangement.${f.etatApprobation}`)} /> },
            {
              key: t('decision.fiche.lueLe'),
              value: (
                <span className="gm-row">
                  <span>{f.lueLe}</span>
                  {f.aRelire ? <StatusBadge status="warn" label={t('decision.fiche.aRelire')} /> : null}
                </span>
              ),
            },
            { key: t('decision.fiche.documentParametrage'), value: approbation.documentParametrageRecu ?? <Vide /> },
          ]}
        />
      ) : peut(approbation.actionsPermises, 'REFERENCER_FICHE', 'referencerFiche') ? (
        <div className="gm-col" style={{ maxWidth: 560 }}>
          <TextField name="reference" label={t('decision.fiche.reference')} required mono value={reference} onChange={(v) => setReference(v.toUpperCase())} help={t('decision.fiche.format')} maxLength={20} />
          <div className="gm-row">
            <Button
              variant="primary"
              icon="paperclip"
              disabled={!reference.trim()}
              disabledReason={t('decision.fiche.referenceRequise')}
              onClick={() => lancer(actionReferencer, { reference: reference.trim() }, dossier.reference)}
            >
              {t('decision.fiche.action.referencer')}
            </Button>
          </div>
        </div>
      ) : (
        <Vide texte={t('decision.fiche.attendue')} />
      )}
      {f && approbation.etat === 'VERIFIE_ARCHIVE' && visasEnAttente ? <InlineAlert level="warn">{t('decision.fiche.visasAvant')}</InlineAlert> : null}
      {f && approbation.etat !== 'VERIFIE_ARCHIVE' && approbation.etat !== 'APPROUVEE' ? <InlineAlert level="info">{t('decision.fiche.archivageAvant')}</InlineAlert> : null}
      {f && peut(approbation.actionsPermises, 'RELIRE_FICHE', 'relireFiche') ? (
        <div className="gm-row">
          <Button variant="secondary" icon="refresh" onClick={() => lancer(actionRelire, approbation, f.reference)}>{t('decision.fiche.action.relire')}</Button>
        </div>
      ) : null}
    </div>
  )
}
