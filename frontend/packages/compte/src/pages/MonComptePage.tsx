import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, ConfirmDialog, DataTable, InlineAlert, KeyValueList, Select, StatusBadge, Tag, TextField, type Column } from '@gimac/design-system'
import { EtatsRequete, type CleSecurite } from '@gimac/socle'
import { LANGUES_COMPTE, type ActionLibreService, type LangueCompte, type Moi, type SessionCompte } from '../modele'
import {
  useActionLibreService, useEnrolerCle, useFermerSession, useFermerSessions, useMesCles, useMesSessions, useModifierFiche, useMoi, useSupprimerCle,
} from '../hooks'
import { dateHeure, useLibelleProfil, useMessageErreur, type MessageErreur } from '../outils'

// Mon compte (D-62), commun aux trois couches : fiche personnelle, mot de passe et second facteur
// (actions exécutées par le fournisseur d'identité après reconnexion), clés de sécurité FIDO2,
// sessions ouvertes. Aucune page du fournisseur d'identité n'est ouverte en dehors de l'authentification.

type Retour = { niveau: 'info' | 'crit' | 'warn'; message: MessageErreur } | null

/** Résultat d'une action : InlineAlert du catalogue, annoncé (role status ou alert). */
function Annonce({ retour }: { retour: Retour }) {
  return retour ? <InlineAlert level={retour.niveau} title={retour.message.texte} correlationId={retour.message.correlationId} /> : null
}

function Fiche({ moi }: { moi: Moi }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const libelleProfil = useLibelleProfil()
  const modifier = useModifierFiche()
  const c = moi.compte
  const [v, setV] = useState({ prenom: c.prenom ?? '', nom: c.nom ?? '', telephone: c.telephone ?? '', fonction: c.fonction ?? '', langue: (c.langue ?? 'fr') as LangueCompte })
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [retour, setRetour] = useState<Retour>(null)
  const champ = (k: keyof typeof v) => (x: string) => setV({ ...v, [k]: x })
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreurs({})
    setRetour(null)
    try {
      await modifier.mutateAsync({ prenom: v.prenom.trim(), nom: v.nom.trim(), telephone: v.telephone.trim() || null, fonction: v.fonction.trim() || null, langue: v.langue })
      setRetour({ niveau: 'info', message: { texte: t('compte.fiche.enregistree') } })
    } catch (x) {
      const m = message(x)
      if (m.code === 'IAM_PHONE_INVALID') setErreurs({ telephone: m.texte })
      else if (m.code === 'IAM_LANGUAGE_INVALID') setErreurs({ langue: m.texte })
      else setRetour({ niveau: 'crit', message: m })
    }
  }
  return (
    <Card title={t('compte.fiche.titre')}>
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('compte.identifiant'), value: c.identifiant, mono: true },
            { key: t('compte.email'), value: c.email ?? '—' },
            { key: t('compte.profils'), value: <span className="gm-row">{c.profils.map((p) => <Tag key={p}>{libelleProfil(p)}</Tag>)}</span> },
          ]}
        />
        <p className="gm-field-help" style={{ margin: 0 }}>{t('compte.fiche.administration')}</p>
        <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-label={t('compte.fiche.titre')}>
          <div className="gm-form-grid">
            <div className="gm-form-field"><TextField name="prenom" label={t('compte.prenom')} value={v.prenom} onChange={champ('prenom')} autoComplete="given-name" /></div>
            <div className="gm-form-field"><TextField name="nom" label={t('compte.nom')} value={v.nom} onChange={champ('nom')} autoComplete="family-name" /></div>
            <div className="gm-form-field"><TextField name="telephone" type="tel" label={t('compte.telephone')} value={v.telephone} onChange={champ('telephone')} error={erreurs.telephone} help={t('compte.telephone.aide')} autoComplete="tel" /></div>
            <div className="gm-form-field"><TextField name="fonction" label={t('compte.fonction')} value={v.fonction} onChange={champ('fonction')} /></div>
            <div className="gm-form-field">
              <Select name="langue" label={t('compte.langue')} value={v.langue} onChange={champ('langue')} error={erreurs.langue} options={LANGUES_COMPTE.map((l) => ({ value: l, label: t(`compte.langue.${l}`) }))} />
            </div>
          </div>
          <Annonce retour={retour} />
          <div><Button variant="primary" type="submit" loading={modifier.isPending}>{t('compte.fiche.enregistrer')}</Button></div>
        </form>
      </div>
    </Card>
  )
}

function Securite({ moi }: { moi: Moi }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const agir = useActionLibreService()
  const [demande, setDemande] = useState<ActionLibreService | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const confirmer = async () => {
    const a = demande!
    setDemande(null)
    setRetour(null)
    try {
      await agir.mutateAsync(a)
      setRetour({ niveau: 'info', message: { texte: t('compte.securite.reconnexion') } })
    } catch (x) {
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  return (
    <Card title={t('compte.securite.titre')}>
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('compte.secondFacteur'), value: <StatusBadge status={moi.compte.secondFacteur ? 'ok' : 'warn'} label={t(moi.compte.secondFacteur ? 'compte.secondFacteur.actif' : 'compte.secondFacteur.absent')} /> },
            { key: t('compte.email'), value: <StatusBadge status={moi.compte.emailVerifie ? 'ok' : 'warn'} label={t(moi.compte.emailVerifie ? 'compte.email.verifie' : 'compte.email.nonVerifie')} /> },
          ]}
        />
        <p className="gm-field-help" style={{ margin: 0 }}>{t('compte.securite.texte')}</p>
        <div className="gm-row">
          <Button icon="key-round" onClick={() => setDemande('UPDATE_PASSWORD')}>{t('compte.securite.motDePasse')}</Button>
          <Button icon="smartphone" onClick={() => setDemande('CONFIGURE_TOTP')}>{t('compte.securite.secondFacteur')}</Button>
        </div>
        <Annonce retour={retour} />
      </div>
      {demande ? (
        <ConfirmDialog
          open
          title={t(demande === 'UPDATE_PASSWORD' ? 'compte.securite.motDePasse' : 'compte.securite.secondFacteur')}
          description={t(demande === 'UPDATE_PASSWORD' ? 'compte.securite.motDePasse.texte' : 'compte.securite.secondFacteur.texte')}
          confirmLabel={t('compte.securite.continuer')}
          onConfirm={() => void confirmer()}
          onClose={() => setDemande(null)}
        />
      ) : null}
    </Card>
  )
}

function Cles() {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const cles = useMesCles()
  const enroler = useEnrolerCle()
  const supprimer = useSupprimerCle()
  const [nom, setNom] = useState('')
  const [aSupprimer, setASupprimer] = useState<CleSecurite | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const ajouter = async () => {
    setRetour(null)
    if (!nom.trim()) {
      setRetour({ niveau: 'warn', message: { texte: t('compte.cles.nom.requis') } })
      return
    }
    try {
      const c = await enroler.mutateAsync(nom)
      setNom('')
      setRetour({ niveau: 'info', message: { texte: t('compte.cles.enrolee', { nom: c.nom }) } })
    } catch (x) {
      setRetour({ niveau: 'warn', message: message(x) })
    }
  }
  const retirer = async () => {
    const c = aSupprimer!
    setASupprimer(null)
    try {
      await supprimer.mutateAsync(c.id)
      setRetour({ niveau: 'info', message: { texte: t('compte.cles.supprimee', { nom: c.nom }) } })
    } catch (x) {
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  const colonnes: Column<CleSecurite>[] = [
    { key: 'nom', header: t('compte.cles.nom') },
    { key: 'creeeLe', header: t('compte.cles.creeeLe'), render: (c) => dateHeure(c.creeeLe, i18n.language) },
    { key: 'derniereUtilisation', header: t('compte.cles.derniereUtilisation'), render: (c) => (c.derniereUtilisation ? dateHeure(c.derniereUtilisation, i18n.language) : t('compte.cles.jamais')) },
    { key: 'actions', header: '', type: 'actions', render: (c) => <Button size="sm" variant="danger" onClick={() => setASupprimer(c)}>{t('compte.cles.supprimer')}</Button> },
  ]
  return (
    <Card title={t('compte.cles.titre')}>
      <div className="gm-col">
        <p className="gm-field-help" style={{ margin: 0 }}>{t('compte.cles.texte')}</p>
        <EtatsRequete etat={cles} isEmpty={(l) => l.length === 0} emptyTitle={t('compte.cles.aucune')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
          {(l) => <DataTable columns={colonnes} rows={l} rowKey={(c) => c.id} density="compact" />}
        </EtatsRequete>
        <div className="gm-row" style={{ alignItems: 'flex-end' }}>
          <div style={{ width: 320 }}><TextField name="nomNouvelleCle" label={t('compte.cles.nom')} value={nom} onChange={setNom} maxLength={100} /></div>
          <Button variant="primary" icon="key-round" loading={enroler.isPending} onClick={() => void ajouter()}>{t('compte.cles.enroler')}</Button>
        </div>
        <Annonce retour={retour} />
      </div>
      {aSupprimer ? (
        <ConfirmDialog open danger title={t('compte.cles.supprimer')} description={t('compte.cles.supprimer.texte')} confirmLabel={t('compte.cles.supprimer')}
          object={[{ key: t('compte.cles.nom'), value: aSupprimer.nom }]} onConfirm={() => void retirer()} onClose={() => setASupprimer(null)} />
      ) : null}
    </Card>
  )
}

function Sessions() {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const sessions = useMesSessions()
  const fermer = useFermerSession()
  const fermerTout = useFermerSessions()
  const [toutes, setToutes] = useState(false)
  const [retour, setRetour] = useState<Retour>(null)
  const agir = async (f: () => Promise<unknown>, succes: string) => {
    setRetour(null)
    try {
      await f()
      setRetour({ niveau: 'info', message: { texte: succes } })
    } catch (x) {
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  const colonnes: Column<SessionCompte>[] = [
    { key: 'adresseIp', header: t('compte.sessions.adresse'), render: (s) => <span className="gm-mono">{s.adresseIp ?? '—'}</span> },
    { key: 'debut', header: t('compte.sessions.debut'), render: (s) => dateHeure(s.debut, i18n.language) },
    { key: 'dernierAcces', header: t('compte.sessions.dernierAcces'), render: (s) => dateHeure(s.dernierAcces, i18n.language) },
    { key: 'applications', header: t('compte.sessions.applications'), render: (s) => s.applications.join(', ') },
    {
      key: 'actions', header: '', type: 'actions',
      render: (s) => <Button size="sm" onClick={() => void agir(() => fermer.mutateAsync(s.id), t('compte.sessions.fermee'))}>{t('compte.sessions.fermer')}</Button>,
    },
  ]
  return (
    <Card title={t('compte.sessions.titre')} actions={<Button size="sm" variant="danger" onClick={() => setToutes(true)}>{t('compte.sessions.fermerToutes')}</Button>}>
      <div className="gm-col">
        <EtatsRequete etat={sessions} isEmpty={(l) => l.length === 0} emptyTitle={t('compte.sessions.aucune')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
          {(l) => <DataTable columns={colonnes} rows={l} rowKey={(s) => s.id} density="compact" />}
        </EtatsRequete>
        <Annonce retour={retour} />
      </div>
      {toutes ? (
        <ConfirmDialog open danger title={t('compte.sessions.fermerToutes')} description={t('compte.sessions.fermerToutes.texte')} confirmLabel={t('compte.sessions.fermerToutes')}
          onConfirm={() => { setToutes(false); void agir(() => fermerTout.mutateAsync(undefined), t('compte.sessions.fermees')) }} onClose={() => setToutes(false)} />
      ) : null}
    </Card>
  )
}

export default function MonComptePage() {
  const { t } = useTranslation()
  const moi = useMoi()
  useEffect(() => {
    document.title = `GIMAC · ${t('compte.titre')}`
  }, [t])
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <h1 className="gm-heading-display">{t('compte.titre')}</h1>
      <EtatsRequete etat={moi} emptyTitle={t('commun.vide')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
        {(m) => (
          <>
            <Fiche moi={m} />
            <Securite moi={m} />
            <Cles />
            <Sessions />
          </>
        )}
      </EtatsRequete>
    </div>
  )
}
