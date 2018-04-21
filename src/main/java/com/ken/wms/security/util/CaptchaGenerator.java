package com.ken.wms.security.util;


import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 图形验证码生成器
 *
 * @author Ken
 */
public class CaptchaGenerator {

    /**
     * 图形验证码中包含的字符
     */
//    private static char[] codeSequence = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
//            'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static char[] codeSequence = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    /**
     * 图形验证码参数
     */
    private static final int width = 80;// 验证码图片的宽度
    private static final int height = 35;// 验证码图片的高度
    private static final int characterCount = 4;// 验证码图片字符的个数
    private static final int lineCount = 10;// 验证码干扰线数目
    private static Font font;// 验证码字体样式
    private static Color captchaBgColor = Color.white;// 验证码图片背景颜色
    private static Color lineColor = Color.gray;// 验证码干扰线颜色

    static {
        font = new Font("Arial", Font.BOLD | Font.ITALIC, 25);
    }

    /**
     * 随机生成图形验证码
     *
     * @return 返回一个Map，其中包含图形验证码图片以及其对应的文本
     */
    public static Map<String, Object> generateCaptcha() {

        // 存储验证码
        char[] code = new char[characterCount];

        Random random = new Random();

        // 创建 BufferedImage 对象
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        // 创建Graphics2D对象
        Graphics graphics = image.getGraphics();
        Graphics2D graphics2d = (Graphics2D) graphics;

        // 设置图片
        graphics.setColor(captchaBgColor);
        graphics.fillRect(1, 1, width - 2, height - 2);
        graphics.setFont(font);
        graphics.setColor(lineColor);

        // 绘制颜色和位置全部为随机产生的线条
        for (int i = 1; i <= lineCount; i++) {
            int x = random.nextInt(width - 1);
            int y = random.nextInt(height - 1);
            int x1 = random.nextInt(width - 1);
            int y1 = random.nextInt(height - 1);

            Line2D line2d = new Line2D.Double(x, y, x1, y1);
            graphics2d.draw(line2d);
        }

        // 设置验证码中的字符
        for (int i = 0; i < characterCount; i++) {
            graphics.setColor(getRandColor());
            code[i] = codeSequence[random.nextInt(codeSequence.length - 1)];
            graphics2d.drawString(String.valueOf(code[i]), random.nextInt(10) + 15 * i, 20 + random.nextInt(10));
        }

        Map<String, Object> captcha = new HashMap<>();
        captcha.put("captchaString", String.valueOf(code));
        captcha.put("captchaImage", image);
        return captcha;
    }

    /**
     * 随机生成颜色
     *
     * @return 返回随机生成的颜色
     */
    private static Color getRandColor() {
        Random random = new Random();

        int r, g, b;
        r = random.nextInt(255);
        g = random.nextInt(255);
        b = random.nextInt(255);

        return new Color(r, g, b);
    }
}
