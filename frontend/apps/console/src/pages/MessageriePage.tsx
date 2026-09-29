import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, Checkbox, NumberField, StatusBadge, TextArea, TextField } from '@gimac/design-system'
import { useMessageErreur } from '@gimac/compte'
import { useEffet, useMessagerie } from '../hooks'
import type { ConfigurationMessagerie } from '../modele/configuration'
import { Annonce } from '../composants/Annonce'
import { type Retour } from '../composants/actes'
import { Donnees } from '../composants/communs'

// Messagerie de la plateforme (D-62, PLATFORM_MESSAGING_EDIT) : serveur d'envoi des invitations et
// des réinitialisations, sans passer par la console du fournisseur d'identité. Le mot de passe est en
// écriture seule : MS-IAM ne le restitue jamais (« défini ») ; un champ vide conserve l'existant.

type Saisie = Omit<ConfigurationMessagerie, 'port' | 'utilisateur' | 'motDePasse'> & { port: string; utilisateur: string; motDePasse: string }
const versSaisie = (m: ConfigurationMessagerie): Saisie => ({ ...m, port: String(m.port), utilisateur: m.utilisateur ?? '', motDePasse: '' })

function Formulaire({ messagerie, retour, setRetour }: { messagerie: ConfigurationMessagerie; retour: Retour; setRetour: (r: Retour) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const appliquer = useEffet((s, v: { m: ConfigurationMessagerie; motif: string }) => s.configuration.appliquerMessagerie(v.m, v.motif))
  const tester = useEffet((s, d: string) => s.configuration.testerMessagerie(d), [])
  const [v, setV] = useState<Saisie>(() => versSaisie(messagerie))
  const [motif, setMotif] = useState('')
  const [destinataire, setDestinataire] = useState('')
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [retourTest, setRetourTest] = useState<Retour>(null)
  const champ = (k: keyof Saisie) => (x: string | boolean) => setV((o) => ({ ...o, [k]: x }))

  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    const port = Number(v.port)
    const locales: Record<string, string> = {}
    if (!v.hote.trim()) locales.hote = t('console.creation.requis')
    if (!Number.isInteger(port) || port < 1 || port > 65535) locales.port = t('console.messagerie.port.invalide')
    if (!v.expediteur.trim()) locales.expediteur = t('console.creation.requis')
    if (v.authentification && !v.utilisateur.trim()) locales.utilisateur = t('console.creation.requis')
    if (motif.trim().length < 10) locales.motif = t('console.motif.aide')
    setErreurs(locales)
    setRetour(null)
    if (Object.keys(locales).length) return
    try {
      await appliquer.mutateAsync({
        m: { ...v, port, hote: v.hote.trim(), expediteur: v.expediteur.trim(), utilisateur: v.authentification ? v.utilisateur.trim() : null, motDePasse: v.motDePasse || null },
        motif: motif.trim(),
      })
      setMotif('')
      setRetour({ niveau: 'info', message: { texte: t('console.messagerie.enregistree') } })
    } catch (x) {
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  const envoyerTest = async () => {
    setRetourTest(null)
    try {
      await tester.mutateAsync(destinataire.trim())
      setRetourTest({ niveau: 'info', message: { texte: t('console.messagerie.test.envoye', { destinataire: destinataire.trim() }) } })
    } catch (x) {
      setRetourTest({ niveau: 'crit', message: message(x) })
    }
  }

  return (
    <div className="gm-col">
      <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-labelledby="gm-messagerie-titre" noValidate>
        <Card title={t('console.messagerie.serveur')}>
          <div className="gm-form-grid">
            <div className="gm-form-field"><TextField name="hote" label={t('console.messagerie.hote')} required mono value={v.hote} onChange={champ('hote')} error={erreurs.hote} /></div>
            <div className="gm-form-field"><NumberField name="port" label={t('console.messagerie.port')} required value={v.port} onChange={champ('port')} error={erreurs.port} help={t('console.messagerie.port.aide')} /></div>
            <div className="gm-form-field"><TextField name="expediteur" type="email" label={t('console.messagerie.expediteur')} required value={v.expediteur} onChange={champ('expediteur')} error={erreurs.expediteur} /></div>
            <div className="gm-form-field"><TextField name="nomExpediteur" label={t('console.messagerie.nomExpediteur')} value={v.nomExpediteur} onChange={champ('nomExpediteur')} /></div>
            <div className="gm-form-field"><Checkbox name="starttls" label={t('console.messagerie.starttls')} checked={v.starttls} onChange={champ('starttls')} /></div>
            <div className="gm-form-field"><Checkbox name="ssl" label={t('console.messagerie.ssl')} checked={v.ssl} onChange={champ('ssl')} /></div>
          </div>
        </Card>
        <Card title={t('console.messagerie.authentification')}>
          <div className="gm-form-grid">
            <div className="gm-form-field gm-form-wide"><Checkbox name="authentification" label={t('console.messagerie.authentification.active')} checked={v.authentification} onChange={champ('authentification')} /></div>
            <div className="gm-form-field">
              <TextField name="utilisateur" label={t('console.messagerie.utilisateur')} value={v.utilisateur} onChange={champ('utilisateur')} error={erreurs.utilisateur} disabled={!v.authentification} />
            </div>
            <div className="gm-form-field">
              <TextField name="motDePasse" type="password" label={t('console.messagerie.motDePasse')} value={v.motDePasse} onChange={champ('motDePasse')} disabled={!v.authentification}
                help={t(messagerie.motDePasseDefini ? 'console.messagerie.motDePasse.defini' : 'console.messagerie.motDePasse.nonDefini')} />
            </div>
            <div className="gm-form-field">
              <StatusBadge status={messagerie.motDePasseDefini ? 'ok' : 'warn'} label={t(messagerie.motDePasseDefini ? 'console.messagerie.etat.defini' : 'console.messagerie.etat.nonDefini')} />
            </div>
          </div>
        </Card>
        <Card>
          <TextArea name="motifMessagerie" label={t('console.motif')} required value={motif} onChange={setMotif} error={erreurs.motif} help={t('console.motif.aide')} />
        </Card>
        <Annonce retour={retour} />
        <div><Button variant="primary" type="submit" loading={appliquer.isPending}>{t('console.messagerie.enregistrer')}</Button></div>
      </form>
      <Card title={t('console.messagerie.test')}>
        <div className="gm-col">
          <p className="gm-field-help" style={{ margin: 0 }}>{t('console.messagerie.test.texte')}</p>
          <div className="gm-row" style={{ alignItems: 'flex-end' }}>
            <div style={{ width: 360 }}><TextField name="destinataire" type="email" label={t('console.messagerie.destinataire')} value={destinataire} onChange={setDestinataire} /></div>
            <Button icon="send" loading={tester.isPending} onClick={() => void envoyerTest()}>{t('console.messagerie.test.envoyer')}</Button>
          </div>
          <Annonce retour={retourTest} />
        </div>
      </Card>
    </div>
  )
}

export default function MessageriePage() {
  const { t } = useTranslation()
  const messagerie = useMessagerie()
  // Annonce au niveau de la page : le formulaire est remonté après chaque relecture.
  const [retour, setRetour] = useState<Retour>(null)
  return (
    <div className="gm-col">
      <h1 id="gm-messagerie-titre" className="gm-heading-display">{t('console.messagerie.titre')}</h1>
      <p style={{ margin: 0 }}>{t('console.messagerie.texte')}</p>
      <Donnees requete={messagerie}>{(m) => <Formulaire key={JSON.stringify(m)} messagerie={m} retour={retour} setRetour={setRetour} />}</Donnees>
    </div>
  )
}
