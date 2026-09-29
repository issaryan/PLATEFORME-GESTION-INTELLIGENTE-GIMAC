import { useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Breadcrumb, Button, Card, FourElementsChecklist, Hash, InlineAlert, KeyValueList, StatusBadge, Stepper } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import { useElementsDeCloture } from '../../hooks/cloture'
import { useExecuteurActions } from '../../ressources/actions'
import { clotureOperationDescriptor, type ClotureAvecFichier } from '../../ressources/cloture'
import { ETAPES_CLOTURE, etapeDepuis, versQuatreElements, type EtapeCloture } from '../../adaptateurs/cloture'
import { etatOperation } from '../../dictionnaires'
import type { ClotureOperation } from '../../modele/homologation'
import { DernierDepot, SelecteurPreuve } from './compositions/cloture'

// DES-10 Clore une opération (gabarit G4, HOM-09 et HOM-10) : preuve, quatre éléments, validation signée.
// L'étape est dans l'adresse (?etape=) ; l'état et les actions permises viennent du service.
const action = (cle: string) => clotureOperationDescriptor.actions.find((a) => a.key === cle)!

export default function ClotureOperationPage() {
  const { id = '', op = '' } = useParams()
  const { t } = useTranslation()
  const q = useElementsDeCloture(op)
  return (
    <EtatsRequete etat={q} emptyTitle={t('erreur.introuvable')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(c) => <Assistant cloture={c} liaisonId={id} />}
    </EtatsRequete>
  )
}

function Assistant({ cloture: c, liaisonId }: { cloture: ClotureOperation; liaisonId: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const executeur = useExecuteurActions()
  const [params, setParams] = useSearchParams()
  const etape = etapeDepuis(params.get('etape'))
  const [fichier, setFichier] = useState<File | null>(null)
  const index = ETAPES_CLOTURE.indexOf(etape)
  const aller = (e: EtapeCloture) => setParams((p) => ({ ...Object.fromEntries(p), etape: e }), { replace: true })
  const ligne: ClotureAvecFichier = { ...c, preuve: fichier }
  const deposer = action('deposer')
  const valider = action('valider')
  const statut = etatOperation[c.etat]
  const retour = `/des/liaisons/${liaisonId}?onglet=operations`

  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)', maxWidth: 960 }}>
      <Breadcrumb
        items={[
          { label: t('nav.liaisons'), href: '/des/liaisons' },
          { label: liaisonId, href: `/des/liaisons/${liaisonId}` },
          { label: c.code },
        ]}
      />
      <header className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        <h1 className="gm-heading-display" style={{ margin: 0 }}>{t('cloture.titre', { code: c.code })}</h1>
        <div className="gm-row">
          <span>{c.libelle}</span>
          {statut ? <StatusBadge status={statut.status} label={t(statut.label)} /> : null}
        </div>
      </header>

      <Stepper steps={ETAPES_CLOTURE.map((e, i) => ({ label: t(`cloture.etape.${e}`), state: i < index ? 'done' : i === index ? 'current' : 'todo' }))} />

      {etape === 'preuve' ? (
        <Card
          title={t('cloture.etape.preuve')}
          footer={
            deposer.isAllowed?.(ligne) ? (
              <Button
                variant="primary"
                icon="upload"
                disabled={!fichier}
                disabledReason={t('cloture.fichier.requis')}
                onClick={() => executeur.lancer(deposer, ligne, `${c.code} · ${fichier?.name ?? ''}`, 'homologation')}
              >
                {t(deposer.label)}
              </Button>
            ) : undefined
          }
        >
          <div className="gm-col">
            <KeyValueList items={[{ key: t('cloture.preuveAttendue'), value: c.preuveAttendue }]} />
            {deposer.isAllowed?.(ligne) ? <SelecteurPreuve fichier={fichier} onFichier={setFichier} dernierDepot={c.dernierDepot} /> : null}
            {c.dernierDepot ? (
              <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
                <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('cloture.dernierDepot')}</h2>
                <DernierDepot depot={c.dernierDepot} />
              </div>
            ) : (
              <InlineAlert level="info">{t('cloture.aucunDepot')}</InlineAlert>
            )}
          </div>
        </Card>
      ) : null}

      {etape === 'elements' ? (
        <Card title={t('cloture.quatreElements')}>
          <div className="gm-col">
            <p className="gm-field-help" style={{ margin: 0 }}>{t('cloture.quatreElements.regle')}</p>
            <FourElementsChecklist {...versQuatreElements(c.elements, t)} />
          </div>
        </Card>
      ) : null}

      {etape === 'signature' ? (
        <Card
          title={t('cloture.etape.signature')}
          footer={
            valider.isAllowed?.(ligne) ? (
              <Button variant="primary" icon="pen-line" onClick={() => executeur.lancer(valider, ligne, `${c.code} · ${c.libelle}`, 'homologation')}>
                {t(valider.label)}
              </Button>
            ) : undefined
          }
        >
          <div className="gm-col">
            <KeyValueList
              items={[
                { key: t('operation.code'), value: c.code, mono: true },
                { key: t('operation.libelle'), value: c.libelle },
                { key: t('cloture.empreinteAValider'), value: c.empreinteAValider ? <Hash value={c.empreinteAValider} /> : <span className="gm-field-help">{t('cloture.empreinteIndisponible')}</span> },
              ]}
            />
            {c.restrictionValidation ? <InlineAlert level="info" title={t('cloture.regle')}>{t(`cloture.restriction.${c.restrictionValidation}`)}</InlineAlert> : null}
            {c.etat === 'VALIDEE_HORODATAGE_EN_ATTENTE' ? <InlineAlert level="info">{t('cloture.horodatageEnAttente')}</InlineAlert> : null}
            {c.etat === 'CLOSE' ? <InlineAlert level="info" title={t('cloture.close.titre')}>{t('cloture.close.texte')}</InlineAlert> : null}
            {!valider.isAllowed?.(ligne) && !c.restrictionValidation && c.etat !== 'CLOSE' && c.etat !== 'VALIDEE_HORODATAGE_EN_ATTENTE' ? (
              <InlineAlert level="warn">{t('cloture.validationIndisponible')}</InlineAlert>
            ) : null}
          </div>
        </Card>
      ) : null}

      <div className="gm-row">
        <Button variant="ghost" onClick={() => void navigate(retour)}>{t('cloture.retourLiaison')}</Button>
        <span className="gm-grow" style={{ flex: 1 }} />
        {index > 0 ? <Button variant="secondary" onClick={() => aller(ETAPES_CLOTURE[index - 1])}>{t('cloture.etapePrecedente')}</Button> : null}
        {index < ETAPES_CLOTURE.length - 1 ? (
          <Button variant="primary" icon="arrow-right" onClick={() => aller(ETAPES_CLOTURE[index + 1])}>{t('cloture.etapeSuivante')}</Button>
        ) : null}
      </div>
    </div>
  )
}
