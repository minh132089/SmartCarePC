import { IconContext } from '@phosphor-icons/react'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import Root from './Root'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* Một bộ icon (Phosphor) và một độ đậm cho cả app */}
    <IconContext.Provider value={{ weight: 'bold' }}>
      {/* /feedback (hoặc #feedback): trang xem dữ liệu phản hồi cho nhóm nghiên cứu, cần khoá */}
      <Root />
    </IconContext.Provider>
  </StrictMode>,
)
