package io.github.majianzheng.jarboot.text.lang;

import io.github.majianzheng.jarboot.text.Color;
import io.github.majianzheng.jarboot.text.Style;
import io.github.majianzheng.jarboot.text.Style.Composite;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 语法高亮的配置，即每一种syntax对应的颜色配置
 * 
 * @author duanling 2015年12月2日 下午3:17:14
 *
 */
public class HighLightTheme {

    Map<Integer, Style> styleMap = new HashMap<Integer, Style>();

    static Map<String, Integer> tokenTypeMap;

    static final String defaulConfigPath = "com/taobao/text/ui/themes/default.xml";

    private static volatile HighLightTheme defaultTheme = null;

    static {
        tokenTypeMap = new HashMap<String, Integer>();
        // 获取当前支持的所有的token type
        Field[] declaredFields = TokenTypes.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                if (field.getType().equals(int.class) && Modifier.isStatic(field.getModifiers())) {
                    try {
                        tokenTypeMap.put(field.getName(), field.getInt(null));
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
    }

    public static HighLightTheme load(URL url) throws Exception {
        InputStream openStream = null;
        try {
            openStream = url.openStream();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document document = db.parse(openStream);
            NodeList elements = document.getElementsByTagName("style");

            HighLightTheme theme = new HighLightTheme();

            for (int i = 0; i < elements.getLength(); ++i) {
                Node item = elements.item(i);
                NamedNodeMap attributes = item.getAttributes();
                // token="RESERVED_WORD" fg="blue" bold="true"

                Node token = attributes.getNamedItem("token");
                String tokenValue = token.getNodeValue();
                Integer tokenType = tokenTypeMap.get(tokenValue);
                if (tokenType == null) {
                    // skip unknown token type
                    continue;
                }

                Color fgColor = null;
                Color bgColor = null;
                Boolean boldValue = null;

                Node fg = attributes.getNamedItem("fg");
                if (fg != null) {
                    fgColor = Color.valueOf(fg.getNodeValue());
                }

                Node bg = attributes.getNamedItem("bg");
                if (bg != null) {
                    bgColor = Color.valueOf(bg.getNodeValue());
                }

                Node bold = attributes.getNamedItem("bold");
                if (bold != null) {
                    boldValue = Boolean.parseBoolean(bold.getNodeValue());
                }

                theme.setStyle(tokenType, Composite.style(boldValue, null, null, fgColor, bgColor));
            }

            return theme;

        } finally {
            if (openStream != null) {
                openStream.close();
            }
        }
    }

    public static HighLightTheme defaultTheme() {
        if (defaultTheme == null) {
            URL resource = HighLightTheme.class.getClassLoader().getResource(defaulConfigPath);
            try {
                defaultTheme = load(resource);
            } catch (Exception e) {
                throw new RuntimeException("load text.ui theme error!", e);
            }
        }

        return defaultTheme;
    }

    public Style getStyle(int tokenType) {
        Style style = styleMap.get(tokenType);
        if (style != null) {
            return style;
        }

        return Style.style();
    }

    public void setStyle(int tokenType, Style style) {
        styleMap.put(tokenType, style);
    }

}
