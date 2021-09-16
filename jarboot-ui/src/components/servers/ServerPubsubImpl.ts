/**
 * 服务订阅发布实现
 */
const TOPIC_SPLIT = '\r';

export class ServerPubsubImpl implements PublishSubmit {
    private handlers = new Map<string, Set<(data: any) => void>>();
    private genTopicKey(namespace: string, event: string) {
        return `${namespace}${TOPIC_SPLIT}${event}`;
    }
    public publish(namespace: string, event: string, data?: any): void {
        const key = this.genTopicKey(namespace, event);
        let sets = this.handlers.get(key);
        if (sets?.size) {
            sets.forEach(handler => handler && handler(data));
        }
    }

    public submit(namespace: string, event: string, handler: (data: any) => void): void {
        const key = this.genTopicKey(namespace, event);
        let sets = this.handlers.get(key);
        if (sets?.size) {
            sets.add(handler);
        } else {
            sets = new Set<(data: any) => void>();
            sets.add(handler);
            this.handlers.set(key, sets);
        }
    }

    public unSubmit(namespace: string, event: string, handler: (data: any) => void): void {
        const key = this.genTopicKey(namespace, event);
        const sets = this.handlers.get(key);
        if (sets?.size) {
            sets.delete(handler);
            if (sets.size === 0) {
                this.handlers.delete(key);
            }
        }
    }

}
