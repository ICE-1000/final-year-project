package com.inventory.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BarcodeService {
    public byte[] generateBarcode(String text) throws WriterException, IOException {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, 300, 100);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }

    public String storeBarcodeImage(String text) {
        return "/api/barcode/image/" + text;
    }
}
