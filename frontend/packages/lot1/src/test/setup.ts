import { configure } from '@testing-library/react'

// Les pages attendent des services simulés et des chargements paresseux : sous la charge de la suite
// complète, le délai d'une seconde de Testing Library rend certains tests instables.
configure({ asyncUtilTimeout: 5000 })
