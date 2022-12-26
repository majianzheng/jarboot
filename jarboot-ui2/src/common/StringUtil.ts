/**
 * 字符串工具类
 * @author majianzheng
 */
class StringUtil {
    /**
     * 判断是否为字符串
     * @param {*} object
     * @return {Boolean} isString
     */
    public static isString(object: any) {
        return (object instanceof String || typeof object === 'string' || (this.isNotNull(object) && object.constructor === String));
    }

    /**
     * 判断字符串是否为空
     * @param {String} str
     *
     */
    public static isEmpty(str: string|undefined|null) {
        if (null == str) {
            return true;
        }
        return (this.isNull(str) || (this.isString(str) && str.trim() === ""));
    }

    /**
     * 判断字符串是否不为空
     * @param {String} str
     *
     */
    public static isNotEmpty(str: string|undefined|null) {
        return !this.isEmpty(str);
    }

    /**
     * 判断是否为整数,默认验证非负整数
     * @param {String} str
     * @param positive
     * @return {Boolean} isNumber
     */
    public static isInt(str: string, positive = true) {
        let exp = "^[0-9]*$";
        if (positive) {
            exp = "^\\d+$";
        } else if (!positive) {
            exp = "^((-\\d+)|(0+))$";
        }
        let reg = new RegExp(exp);
        return reg.test(str)
    }


    /**
     * 判断是否为浮点数,默认验证非负浮点数
     * @param {String} str
     * @param {Boolean} positive 是否为正数,true验证非负浮点,false验证非正浮点
     * @return {Boolean}
     */
    public static isFloat(str: string, positive = true) {
        let exp = "^(-?\\d+)(\\.\\d+)?$";
        if (positive) {
            exp = "^\\d+(\\.\\d+)?$";
        } else if (!positive) {
            exp = "^((-\\d+(\\.\\d+)?)|(0+(\\.0+)?))$";
        }
        const reg = new RegExp(exp);
        return reg.test(str)
    }

    /**
     * 判断是否为数字(整数或浮点数),默认验证非负数
     * @param {String} str
     * @param {Boolean} positive 是否为正数,true验证非负浮点,false验证非正浮点
     * @return {Boolean}
     */
    public static isNumber(str: string, positive = true) {
        //先验证是否为整数
        let valid = this.isInt(str, positive);
        //若不为整数,再验证是否为浮点数
        if (valid === false) {
            valid = this.isFloat(str, positive);
        }
        return valid;
    }

    /**
     * 判断是否为数字(整数或浮点数),不区分正负
     * @param {String} str
     * @return {Boolean}
     */
    public static isNumberUnsigned(str: string) {
        let valid = this.isNumber(str, true);
        if (valid === false) {
            valid = this.isNumber(str, false);
        }
        return valid;
    }

    /**
     * 判断是否为null或undefined
     */
    public static isNull(obj: any) {
        return obj === null || obj === undefined;
    }

    /**
     * 判断是否不为null和undefined
     */
    public static isNotNull(obj: any) {
        if (obj != null && obj != undefined) {
            return true;
        }
        return false;
    }

    /**
     * 将值转换为boolean
     * 仅当值为1或true或"true"或"TRUE"时,返回true
     */
    public static toBoolean(val: any) {
        return !!(this.isNotEmpty(val) && (val === 1 || val === "1" || val === true || val === "true" || val === "TRUE"));
    }

    /**
     * 数组转为字符串,形式如: "1","2","3"
     *
     */
    public static arrayToString(arr: any, spliter = ",") {
        let str = "";
        if (this.isNotNull(arr)) {
            for (let value of arr) {
                str += spliter + value;
            }
            if (str.length > 0) {
                str = str.substring(1, str.length);
            }
        }
        return str;
    }

    /**
     * 判断是否为JSON字符串
     * @param {String} str
     */
    public static isJsonStr(str: any) {
        try {
            if (typeof JSON.parse(str) == "object") {
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * 对过长的字符串进行截断处理
     * @param {String} str 原始字符串
     * @param {Number} targetLength 要显示的字符串长度
     */
    public static ellipsis(str, targetLength) {
        let result = str
        if (str.length > targetLength) {
            result = str.substring(0, targetLength - 2) + "...";
        }
        return result;
    }

}

export default StringUtil;
