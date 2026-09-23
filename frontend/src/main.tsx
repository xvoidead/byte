import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import '@fontsource-variable/inter';
import '@fontsource/jetbrains-mono/400.css';
import '@fontsource/jetbrains-mono/500.css';
import { Layout } from './components/Layout';
import { useTitle } from './components/useTitle';
import { HomePage } from './pages/HomePage';
import { LessonPage } from './pages/LessonPage';
import { PlaygroundPage } from './pages/PlaygroundPage';
import { PrivacyPage, TermsPage } from './pages/LegalPages';
import './styles.css';

function NotFound() {
  useTitle('Страница не найдена — byte');
  return (
    <div className="page-state">
      <h1>Страница не найдена</h1>
      <p className="muted">Возможно, ссылка устарела или в адресе опечатка.</p>
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
          <Route path="privacy" element={<PrivacyPage />} />
          <Route path="terms" element={<TermsPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
);
