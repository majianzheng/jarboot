import Request from "@/common/Request";
import Logger from "@/common/Logger";

const urlBase = "/api/jarboot/cloud";

/**
 * Cloud service
 */
export default class CloudService {
    public static pushServerDirectory(file: File, force?: boolean) {
        let form :FormData = new FormData();
        if (file) {
            form.append("file", file);
        } else {
            Logger.error("file is null.", file)
        }
        form.append("force", `${(force ?? false)}`);
        return Request.post(`${urlBase}/push/server`, form);
    }
}
