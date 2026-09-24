import type { Diagnostic, OutputFile, ProjectFile, RunStatus } from '../types';

export type ConsoleEvent =
  | { type: 'started'; diagnostics: Diagnostic[]; compileTimeMs: number }
  | { type: 'compile_error'; diagnostics: Diagnostic[]; compileTimeMs: number }
  | { type: 'out'; stream: 'stdout' | 'stderr'; data: string }
  | { type: 'exit'; status: RunStatus; exitCode: number | null; timeMs: number; files?: OutputFile[] }
  | { type: 'error'; message: string };

export type ConsoleCommand =
  | { type: 'run'; files: ProjectFile[] }
  | { type: 'input'; data: string }
  | { type: 'eof' }
  | { type: 'stop' };

const CONNECT_TIMEOUT_MS = 5000;

/**
 * Соединение с интерактивной консолью. Подключается при первом запуске
 * и переподключается, если соединение было закрыто.
 */
export class ConsoleSocket {
  private socket: WebSocket | null = null;
  private opening: Promise<WebSocket> | null = null;

  constructor(
    private readonly onEvent: (event: ConsoleEvent) => void,
    private readonly onDisconnect: () => void,
  ) {}

  async send(command: ConsoleCommand): Promise<void> {
    const socket = await this.connect();
    socket.send(JSON.stringify(command));
  }

  close() {
    this.socket?.close();
    this.socket = null;
    this.opening = null;
  }

  private connect(): Promise<WebSocket> {
    if (this.socket?.readyState === WebSocket.OPEN) return Promise.resolve(this.socket);
    if (this.opening) return this.opening;
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const socket = new WebSocket(`${protocol}://${window.location.host}/api/console`);
    this.opening = new Promise<WebSocket>((resolve, reject) => {
      const timer = window.setTimeout(() => {
        socket.close();
        reject(new Error('timeout'));
      }, CONNECT_TIMEOUT_MS);
      socket.onopen = () => {
        window.clearTimeout(timer);
        this.socket = socket;
        resolve(socket);
      };
      socket.onerror = () => {
        window.clearTimeout(timer);
        reject(new Error('connection failed'));
      };
    }).finally(() => {
      this.opening = null;
    });
    socket.onmessage = (message) => {
      try {
        this.onEvent(JSON.parse(message.data) as ConsoleEvent);
      } catch {
        // повреждённое сообщение игнорируем
      }
    };
    socket.onclose = () => {
      if (this.socket === socket) {
        this.socket = null;
        this.onDisconnect();
      }
    };
    return this.opening;
  }
}
