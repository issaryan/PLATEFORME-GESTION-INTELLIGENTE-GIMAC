import { configure } from '@testing-library/react'

// Services simulés, chargements paresseux et dialogues : délai de Testing Library allongé sous la charge.
configure({ asyncUtilTimeout: 5000 })
