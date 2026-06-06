package com.example.merchant_service.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates QR code images using the ZXing (Zebra Crossing) library.
 *
 * The QR code payload is the merchant code (e.g. "MCH-20240101-ABC123").
 * The customer's app decodes this and calls:
 *   POST /api/v1/merchants/{merchantCode}/pay
 *
 * Output: Base64-encoded PNG string, suitable for embedding in
 * HTML as <img src="data:image/png;base64,{qrCodeBase64}">
 * or sending in the API response body.
 *
 * Error correction level: Q (25% data restoration capability).
 * This is a good balance — the QR code can be scanned even if up
 * to 25% of the image is obscured (e.g. a logo overlay).
 */
@Slf4j
@Service
public class QrCodeService {

    @Value("${app.qr.width:300}")
    private int qrWidth;

    @Value("${app.qr.height:300}")
    private int qrHeight;

    /**
     * Generates a QR code for the given content and returns it as
     * a Base64-encoded PNG string.
     *
     * @param content  the data to encode (e.g. a merchant code)
     * @return         Base64-encoded PNG bytes
     * @throws IllegalStateException if ZXing encoding fails
     */
    public String generateQrCodeBase64(String content) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2); // quiet zone in modules

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE,
                    qrWidth, qrHeight, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] pngBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(pngBytes);

            log.debug("QR code generated for content length={}", content.length());
            return base64;

        } catch (WriterException e) {
            log.error("Failed to encode QR code for content: {}", content, e);
            throw new IllegalStateException("Failed to generate QR code: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to write QR code PNG for content: {}", content, e);
            throw new IllegalStateException("Failed to write QR code image: " + e.getMessage(), e);
        }
    }
}
