package com.yep.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.yep.config.YepProperties;
import com.yep.domain.learning.Certificate;

@Service
public class CertificatePdfService {

    private final YepProperties properties;

    public CertificatePdfService(YepProperties properties) {
        this.properties = properties;
    }

    public byte[] render(Certificate certificate) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 56, 56, 48, 48);
            PdfWriter.getInstance(document, output);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 30);
            Font subtitle = FontFactory.getFont(FontFactory.HELVETICA, 16);
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 14);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph heading = new Paragraph("YEP", title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);

            Paragraph certificateTitle = new Paragraph("Certificate of Achievement", subtitle);
            certificateTitle.setAlignment(Element.ALIGN_CENTER);
            certificateTitle.setSpacingAfter(24);
            document.add(certificateTitle);

            Paragraph name = new Paragraph(certificate.getYouth().getName(), nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingAfter(18);
            document.add(name);

            Paragraph achievement = new Paragraph(certificate.getAchievement(), body);
            achievement.setAlignment(Element.ALIGN_CENTER);
            achievement.setSpacingAfter(28);
            document.add(achievement);

            String verifyUrl = properties.getFrontendUrl() + "/verify/" + certificate.getCertificateNumber();
            Paragraph details = new Paragraph("Issued on "
                    + certificate.getIssueDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    + "\nCertificate number: " + certificate.getCertificateNumber(), body);
            details.setAlignment(Element.ALIGN_CENTER);
            document.add(details);

            BitMatrix matrix = new QRCodeWriter().encode(verifyUrl, BarcodeFormat.QR_CODE, 220, 220);
            BufferedImage buffered = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", png);
            Image qr = Image.getInstance(png.toByteArray());
            qr.setAlignment(Element.ALIGN_CENTER);
            qr.scaleToFit(110, 110);
            qr.setSpacingBefore(18);
            document.add(qr);

            Paragraph verify = new Paragraph("Verify at " + verifyUrl, small);
            verify.setAlignment(Element.ALIGN_CENTER);
            verify.setSpacingBefore(8);
            document.add(verify);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate certificate PDF", exception);
        }
    }
}
