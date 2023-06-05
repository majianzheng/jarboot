export class FileUpload {
  private file: File;
  private socket: WebSocket;
  public constructor(file: File) {
    this.file = file;
    this.socket = new WebSocket(``);
    this.socket.onopen = () => {
      //todo
    };
    this.socket.onmessage = event => {
      //todo
    };
  }
}
