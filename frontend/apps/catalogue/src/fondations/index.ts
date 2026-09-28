import type { ComponentType } from 'react'
import Couverture from './Couverture'
import Couleurs from './Couleurs'
import Typographie from './Typographie'
import Espacements from './Espacements'
import Logo from './Logo'
import Motif from './Motif'
import Pictogrammes from './Pictogrammes'

export interface Fondation {
  id: string
  nom: string
  description: string
  largeur: number | null
  hauteur: number
  Apercu: ComponentType
}

export const FONDATIONS: Fondation[] = [
  { id: 'couverture', nom: 'Couverture', largeur: 1000, hauteur: 320, Apercu: Couverture,
    description: "Système de conception de la plateforme intégrée du Département Exploitation et Supervision (DES) du GIMAC : application du département, portail des participants et console d'administration et d'audit. Il est la référence unique du frontend : toute maquette, tout composant, tout écran s'y conforme ; un écart est une demande de changement, pas une initiative." },
  { id: 'couleurs', nom: 'Couleurs', largeur: 1100, hauteur: 600, Apercu: Couleurs,
    description: "Les composants ne consomment que la couche sémantique `gm-color-*`, seule redéfinie par le mode sombre. Une seule couleur d'action, le bleu ; le vert, l'ocre et le rouge sont des couleurs de statut, jamais d'action ni de décoration. Les graphiques emploient la palette `gm-data-1` à `gm-data-8` dans l'ordre fixe." },
  { id: 'typographie', nom: 'Typographie', largeur: 1000, hauteur: 600, Apercu: Typographie,
    description: "Titres en Ubuntu (`gm-type-display` à `gm-type-h3`), interface en Inter (`gm-type-body` à `gm-type-button`), identifiants en JetBrains Mono (`gm-type-mono`). Un seul `gm-type-display` par écran : c'est le h1. Polices auto-hébergées." },
  { id: 'espacements', nom: 'Espacements et formes', largeur: 1000, hauteur: 600, Apercu: Espacements,
    description: "Grille de 8 px, pas de 4 px. `gm-radius` = 0 partout, seule exception l'avatar. Aucune ombre : la hiérarchie passe par les fonds et des bordures de 1 px. Mouvement : 120, 200 et 320 ms, courbe `gm-motion-ease`." },
  { id: 'logo', nom: 'Logo', largeur: 1000, hauteur: 240, Apercu: Logo,
    description: "Le logo original (1024 × 341) est la référence colorimétrique : bleu `#006EB7`, vert `#0B6938`. Taille minimale 120 px ; barre supérieure à 28 px de haut. Ne jamais déformer, recolorer, ombrer ni animer le logo." },
  { id: 'motif', nom: 'Motif de trame', largeur: 760, hauteur: 520, Apercu: Motif,
    description: "Pavage de losanges et de chevrons sur la grille de 8 px (période 32 px), trait de 1 px `gm-color-brand-accent` à 8 % sur `gm-color-brand-pattern`. Réservé à la connexion, aux états vides, à l'en-tête des rapports et à l'accueil du portail ; jamais derrière du texte courant, dans un tableau, dans un tiroir ni sur le mur d'écrans." },
  { id: 'pictogrammes', nom: 'Pictogrammes', largeur: 1000, hauteur: 600, Apercu: Pictogrammes,
    description: "Un seul jeu : pictogrammes linéaires Lucide à trait de 1,5 px, 16, 20 ou 24 px, couleur du texte. Toujours accompagnés d'un libellé visible ou d'une infobulle et d'un nom accessible. Les pictogrammes de statut sont réservés." },
]
