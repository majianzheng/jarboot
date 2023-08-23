package io.github.majianzheng.jarboot.core.cmd.model;

import io.github.majianzheng.jarboot.core.utils.affect.EnhancerAffect;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class EnhancerModel extends ResultModel {

    private final EnhancerAffectVO effect;
    private boolean success;
    private String message;

    public EnhancerModel(EnhancerAffect effect, boolean success) {
        if (effect != null) {
            this.effect = new EnhancerAffectVO(effect);
            this.success = success;
        } else {
            this.effect = new EnhancerAffectVO(-1, 0, 0, -1);
            this.success = false;
        }
    }

    public EnhancerModel(EnhancerAffect effect, boolean success, String message) {
        this(effect, success);
        this.message = message;
    }

    public EnhancerAffectVO getEffect() {
        return effect;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getName() {
        return "enhancer";
    }
}
