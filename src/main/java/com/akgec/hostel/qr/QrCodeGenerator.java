package com.akgec.hostel.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class QrCodeGenerator {

    @Value("${app.file.pdf-dir}")
    private String pdfDir;

    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;

    /**
     * Generates a secure QR token string
     * Format: leaveId|studentId|uuid
     */
    public String generateQrToken(Long leaveId, Long studentId) {
        String rawToken = leaveId + "|" + studentId + "|" + UUID.randomUUID().toString().replace("-", "");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken.getBytes());
    }

    /**
     * Decodes QR token back to components
     */
    public String[] decodeQrToken(String token) {
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        return new String(decoded).split("\\|");
    }

    /**
     * Generates QR code image PNG and saves to disk
     * Returns the file path
     */
    public String generateQrCodeImage(String qrToken, String leavePassNumber) throws WriterException, IOException {
        Path dir = Paths.get(pdfDir, "qr");
        Files.createDirectories(dir);

        String fileName = "qr_" + leavePassNumber.replace("-", "_") + ".png";
        Path filePath = dir.resolve(fileName);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrToken, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);
        log.info("QR code generated at: {}", filePath);
        return filePath.toString();
    }

    /**
     * Returns QR code as byte array (for embedding in PDF)
     */
    public byte[] generateQrCodeBytes(String qrToken) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrToken, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}
