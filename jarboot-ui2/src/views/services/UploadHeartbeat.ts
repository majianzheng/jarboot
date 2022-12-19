import StringUtil from "@/common/StringUtil";
import UploadFileService from "@/services/UploadFileService";
import CommonNotice from "@/common/CommonNotice";

export default class UploadHeartbeat {
    private static inst = new UploadHeartbeat();
    private handler: any = -1;
    private server: string = "";
    public static getInstance() {
        return UploadHeartbeat.inst;
    }
    private heartbeat = () => {
        if (StringUtil.isEmpty(this.server)) {
            this.stop();
            return;
        }
        UploadFileService.uploadHeartbeat(this.server)
            .then((resp: any) => {
                if (resp.resultCode !== 0) {
                    this.stop();
                    CommonNotice.errorFormatted(resp);
                }
            }).catch((error) => {
                this.stop();
                CommonNotice.errorFormatted(error);
            });
    };
    public start(server: string) {
        if (-1 !== this.handler) {
            return;
        }
        if (StringUtil.isEmpty(server)) {
            return;
        }
        this.server = server;
        this.handler = setInterval(this.heartbeat, 5000);
    }
    public stop() {
        if (-1 === this.handler) {
            return;
        }
        clearInterval(this.handler);
        this.handler = -1;
    }
}
