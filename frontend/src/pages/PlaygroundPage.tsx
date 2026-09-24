import { lazy, Suspense } from 'react';
import { useTitle } from '../components/useTitle';

const Ide = lazy(() => import('../ide/Ide'));

const SAMPLE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Как тебя зовут?");
        String name = in.nextLine();
        System.out.println("Привет, " + name + "!");

        for (int i = 1; i <= 5; i++) {
            System.out.println(i + " в квадрате = " + i * i);
        }
    }
}
`;

export function PlaygroundPage() {
  useTitle('Песочница — byte');
  return (
    <main className="playground">
      <div className="playground-head">
        <h1>Песочница</h1>
        <p className="muted">
          Пишите любой код на Java и запускайте его. Код сохраняется в этом браузере. Попробуйте набрать{' '}
          <code>sout</code> или <code>fori</code> и нажать Tab.
        </p>
      </div>
      <div className="playground-ide">
        <Suspense fallback={<div className="ide-loading">Загружаем редактор…</div>}>
          <Ide storageKey="playground" initialCode={SAMPLE} />
        </Suspense>
      </div>
    </main>
  );
}
