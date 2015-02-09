// string
interface String {
  startsWith(str: string): boolean;
}

if (!String.prototype.startsWith) {
  String.prototype.startsWith = function (str: string) : boolean {
    return this.indexOf(str) == 0;
  };
}

// jquery perfect scrollbar
interface JQuery {
  perfectScrollbar: any;
}

// nvd3
declare var nv: NVD3

interface NVD3 {
  models: any;
  utils: any;
}

// angular-websocket

interface WebSocketEvent {
  data: any;
}

interface AngularWebSocket {
  send: (object: any) => void;
  onopen: (func: any) => void;
  onmessage: (func: (event: WebSocketEvent) => void) => void;
  onerror: (func: (event: WebSocketEvent) => void) => void;
  onclose: (func: (event: WebSocketEvent) => void) => void;
  currentState: () => string;
}
