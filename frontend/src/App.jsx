import { BrowserRouter, Routes, Route } from 'react-router-dom'
import TicketList from './pages/TicketList'
import TicketForm from './pages/TicketForm'

export default function App() {
  return (
    <BrowserRouter>
      <div className="container">
        <Routes>
          <Route path="/" element={<TicketList />} />
          <Route path="/tickets/new" element={<TicketForm />} />
          <Route path="/tickets/:id" element={<TicketForm />} />
        </Routes>
      </div>
    </BrowserRouter>
  )
}
