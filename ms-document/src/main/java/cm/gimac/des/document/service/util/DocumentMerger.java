package cm.gimac.des.document.service.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@ConditionalOnProperty(name = "gimac.document.fusion-pdf.actif", havingValue = "true")
@Component
public class DocumentMerger {

    private final Logger log = LoggerFactory.getLogger(DocumentMerger.class);

    /**
     * Merges multiple multipart files into a single PDF byte array.
     * Supports merging both Images (JPEG/PNG) and PDFs.
     */
    public byte[] mergeToPdf(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files to merge");
        }

        // If only one file and it's already a PDF, return its bytes to avoid processing overhead
        if (files.size() == 1 && isPdf(files.get(0))) {
            return files.get(0).getBytes();
        }

        try (PDDocument mergedDoc = new PDDocument()) {
            for (MultipartFile file : files) {
                if (isPdf(file)) {
                    try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                        for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                            // Using a simple clone to append pages
                            mergedDoc.addPage(pdf.getPage(i));
                        }
                    }
                } else if (isImage(file)) {
                    PDImageXObject image = PDImageXObject.createFromByteArray(mergedDoc, file.getBytes(), file.getOriginalFilename());
                    // Create a page matching the image dimensions
                    PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
                    PDPage page = new PDPage(pageSize);
                    mergedDoc.addPage(page);

                    try (PDPageContentStream contentStream = new PDPageContentStream(mergedDoc, page)) {
                        contentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
                    }
                } else {
                    log.warn("Unsupported file type for merge: {}", file.getContentType());
                    // Just fallback to ignoring or we could throw an exception
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mergedDoc.save(baos);
            return baos.toByteArray();
        }
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.equalsIgnoreCase("application/pdf");
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.startsWith("image/") || contentType.equalsIgnoreCase("application/octet-stream"));
    }
}
