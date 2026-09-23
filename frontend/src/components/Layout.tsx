import { Link, NavLink, Outlet } from 'react-router-dom';

export function Layout() {
  return (
    <div className="app">
      <header className="header">
        <Link to="/" className="logo" aria-label="Byte — на главную">
          <span className="logo-mark">{'{b}'}</span>
          <span className="logo-text">byte</span>
        </Link>
        <nav className="nav">
          <NavLink to="/" end>
            Курс
          </NavLink>
          <NavLink to="/playground">Песочница</NavLink>
        </nav>
      </header>
      <Outlet />
    </div>
  );
}
