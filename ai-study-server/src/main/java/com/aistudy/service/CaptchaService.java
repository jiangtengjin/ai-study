package com.aistudy.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成验证码，返回 [base64图片, 验证码文本]
     */
    public String[] generate() {
        String code = generateCode();
        String base64 = generateImage(code);
        return new String[]{base64, code};
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(randomColor(160, 200));
            int x1 = random.nextInt(WIDTH), y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH), y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 文字
        Font font = new Font("Arial", Font.BOLD, 24);
        g.setFont(font);
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(20, 130));
            int x = i * 28 + 6;
            int y = 28 + random.nextInt(6);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    private Color randomColor(int min, int max) {
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return new Color(r, g, b);
    }
}
