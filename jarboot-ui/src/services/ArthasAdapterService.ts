import Request from '../common/Request';

const urlBase = "/jarboot-arthas";

export default class ArthasAdapterService {
    /**
     * 检查是否安装了Arthas
     */
    public static checkArthasInstalled() {
        return Request.get(`${urlBase}/checkArthasInstalled`, {});
    }

    public static attachToServer(server: string) {
        return Request.get(`${urlBase}/attachToServer`, {server});
    }

    public static getCurrentRunning() {
        return Request.get(`${urlBase}/getCurrentRunning`, {});
    }
}
