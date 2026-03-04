package com.litegateway.admin.constants;

/**
 * 字符串常量
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
public class StringConstants {
    public static final String UTF8 = "utf-8";
    public static final String VERTICAL_BAR = "|";
    public static final String DOT = ".";
    public static final String PERIOD = "。";
    public static final String ASTERISK = "*";
    public static final String DOLLAR = "$";
    public static final String UPER = "^";
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String QUESTION_MARK = "?";
    public static final String SEMICOLON = ";";
    public static final String SPRIT = "/";
    public static final String HIPHEN = "-";
    public static final String UNDERLINE = "_";
    public static final String BLANK = "";
    public static final String AND = "&";

    public static final String X_AUTH_TOKEN = "X-Auth-Token";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    public static final String GATEWAY_ROUTES = "gateway_routes";
    public static final String PATH_VARIANLE = "{id}";

    /**
     * 应用数据库字段：0正常，1无效
     */
    public static final String YES = "0";
    public static final String NO = "1";

    /**
     * 应用前后端字段：0表示未获取响应结果（异常），1表示有响应结果（正常）
     */
    public static final String SUCCESS = "1";
    public static final String FAILED = "0";

    public static final String ENCODING = "UTF-8";
    public static final String HTTP_REQUEST = "request";
    public static final String HTTP_RESPONSE = "response";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json;charset=UTF-8";

    public static final String SEPARATOR_SIGN = ",";
    public static final String NULL = "null";
    public static final String HTTP = "http";

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * 下一步,默认从1开始
     */
    public static final int CURRENT_PAGE = 1;
    /**
     * 分页记录行大小
     */
    public static final int PAGE_SIZE = 10;

    /**
     * 时间常量
     */
    public static final String DAY = "day";
    public static final String HOUR = "hour";
    public static final String MIN = "min";

    /**
     * 日期格式化常量
     */
    public static final String DATE_FORMAT_DAY = "yyyyMMdd";
    public static final String DATE_FORMAT_HOUR = "HH";
    public static final String DATE_FORMAT_MIN = "mm";
    public static final String YYYYMMDD = DATE_FORMAT_DAY;
    public static final String YYYYMMDDHH = "yyyyMMddHH";
    public static final String YYYYMMDDHHMM = "yyyyMMddHHmm";

    /**
     * 动作标识：update更新，remove删除
     */
    public static final String UPDATE = "update";
    public static final String REMOVE = "remove";

    public static final String INSTANCE_CACHE = "INSTANCE_CACHE";

    public static final Integer CAFFEINE_MAX_SIZE = 10000;
    public static final Integer CAFFEINE_EXPIRE_TIME = 3;

    private StringConstants() {
    }
}
