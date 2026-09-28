// Page d'accueil provisoire de la couche. L'enveloppe commune (IHM-01), la
// navigation élaguée (IHM-03) et la bibliothèque de composants (IHM-02) la remplaceront.
const COUCHE = 'Portail des participants'

export default function App() {
  return (
    <>
      <header className="barre-superieure">
        <span className="marque">GIMAC</span>
        <span className="nom-couche">{COUCHE}</span>
      </header>
      <main className="contenu">
        <h1>{COUCHE}</h1>
        <p>Plateforme de suivi des activités du Département Exploitation et Supervision.</p>
        <p className="public">Public : Correspondants des institutions membres.</p>
      </main>
      <footer className="pied">Plateforme DES · GIMAC · Informations à usage interne</footer>
    </>
  )
}
