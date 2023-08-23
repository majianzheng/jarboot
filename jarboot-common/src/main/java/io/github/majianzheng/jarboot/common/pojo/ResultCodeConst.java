package io.github.majianzheng.jarboot.common.pojo;

/**
 * 执行结果错误码定义
 * @author majianzheng
 */
public final class ResultCodeConst {
	/** 执行成功 */
	public static final int SUCCESS = 0;
	/** 验证失败 */
	public static final int VALIDATE_FAILED = -9000;
	/** 非法操作 */
	public static final int INVALID_OPTION = -9001;
	/** 非法参数 */
	public static final int INVALID_PARAM = -9002;
	/** 已存在 */
	public static final int ALREADY_EXIST = -9004;
	/** 不存在 */
	public static final int NOT_EXIST = -9005;
	/** 参数为空 */
	public static final int EMPTY_PARAM = -9006;
	/** 超时 */
	public static final int TIME_OUT = -9998;
	/** 内部错误 **/
	public static final int INTERNAL_ERROR = -9999;

	public static final int NOT_LOGIN_ERROR = 401;
	private ResultCodeConst() {

	}
}
