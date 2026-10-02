// Adresse d'un fichier servi tel quel par Vite (suffixe ?url) : ici le travailleur de pdf.js (D-73).
declare module '*?url' {
  const adresse: string
  export default adresse
}
