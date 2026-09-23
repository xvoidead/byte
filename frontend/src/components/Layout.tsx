import { Link, NavLink, Outlet } from 'react-router-dom';
import { Mascot } from './Mascot';

export function Logo() {
  return (
    <Link to="/" className="logo" aria-label="byte — на главную">
      <Mascot className="logo-img" />
      <span className="wordmark">
        byte<span className="wordmark-cursor" aria-hidden="true" />
      </span>
    </Link>
  );
}

export function Layout() {
  return (
    <div className="app">
      <header className="header">
        <div className="header-inner">
          <Logo />
          <nav className="nav">
            <NavLink to="/" end>
              Курс
            </NavLink>
            <NavLink to="/playground">Песочница</NavLink>
          </nav>
          <Link to="/lessons/hello-world" className="btn btn-primary btn-sm header-cta">
            Начать
          </Link>
        </div>
      </header>
      <Outlet />
    </div>
  );
}
