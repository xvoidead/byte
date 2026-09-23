import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { HomePage } from './pages/HomePage';
import { LessonPage } from './pages/LessonPage';
import { PlaygroundPage } from './pages/PlaygroundPage';
import './styles.css';

function NotFound() {
  return (
    <div className="page-state">
      <h1>Страница не найдена</h1>
      <Link to="/">← На главную</Link>
    </div>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="lessons/:slug" element={<LessonPage />} />
          <Route path="playground" element={<PlaygroundPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
);
