package com.sachin.aiassistant.service;

import com.sachin.aiassistant.dto.DocumentResponse;
import com.sachin.aiassistant.entity.*;
import com.sachin.aiassistant.repository.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DocumentService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PAGES = 20;
    private static final int MAX_DOCUMENTS = 10;
    private static final int CHUNK_SIZE = 1100;
    private static final Pattern SAFE_NAME = Pattern.compile("[^a-zA-Z0-9._-]");

    private final KnowledgeDocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final OrganizationService organizations;
    private final Path storageRoot;

    public DocumentService(KnowledgeDocumentRepository documents, DocumentChunkRepository chunks,
                           OrganizationService organizations, @Value("${app.storage.location}") String storageLocation) {
        this.documents = documents; this.chunks = chunks; this.organizations = organizations;
        this.storageRoot = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID organizationId) {
        return documents.findAllByOrganization_IdOrderByUploadedAtDesc(organizationId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse upload(UUID organizationId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a PDF file to upload.");
        if (documents.countByOrganization_Id(organizationId) >= MAX_DOCUMENTS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Each organization can upload a maximum of 10 PDF documents.");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF must be 10 MB or smaller.");
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("document.pdf");
        if (!original.toLowerCase(Locale.ROOT).endsWith(".pdf") || (file.getContentType() != null && !file.getContentType().equalsIgnoreCase("application/pdf") && !file.getContentType().equalsIgnoreCase("application/octet-stream")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted.");

        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file."); }
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-')
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid PDF.");

        List<PageText> pages = extractPages(bytes);
        if (pages.size() == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The PDF contains no pages.");
        if (pages.size() > MAX_PAGES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each PDF can contain a maximum of 20 pages.");

        Organization organization = organizations.find(organizationId);
        String safeOriginal = SAFE_NAME.matcher(Paths.get(original).getFileName().toString()).replaceAll("_");
        String storedName = UUID.randomUUID() + ".pdf";
        Path organizationFolder = storageRoot.resolve(organizationId.toString()).normalize();
        Path target = organizationFolder.resolve(storedName).normalize();
        if (!target.startsWith(organizationFolder)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename.");

        try {
            Files.createDirectories(organizationFolder);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the PDF.");
        }

        List<String> pageChunks = new ArrayList<>();
        for (PageText page : pages) {
            String normalized = page.text().replaceAll("\\s+", " ").trim();
            if (normalized.isEmpty()) continue;
            for (int start = 0; start < normalized.length(); start += CHUNK_SIZE) {
                pageChunks.add(page.page() + "\u001f" + normalized.substring(start, Math.min(start + CHUNK_SIZE, normalized.length())));
            }
        }
        if (pageChunks.isEmpty()) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No selectable text was found in this PDF. Upload a text-based PDF.");
        }
        KnowledgeDocument doc = documents.save(new KnowledgeDocument(organization, safeOriginal, storedName,
                target.toString(), bytes.length, pages.size(), pageChunks.size()));
        for (String chunk : pageChunks) {
            int separator = chunk.indexOf('\u001f');
            chunks.save(new DocumentChunk(organization, doc, Integer.parseInt(chunk.substring(0, separator)), chunk.substring(separator + 1)));
        }
        return toResponse(doc);
    }

    @Transactional
    public void delete(UUID organizationId, UUID documentId) {
        KnowledgeDocument document = documents.findByIdAndOrganization_Id(documentId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found in this organization."));
        chunks.deleteForDocument(documentId, organizationId);
        documents.delete(document);
        try { Files.deleteIfExists(Paths.get(document.getStoragePath()).normalize()); }
        catch (IOException ignored) { /* metadata and chunks are removed; orphaned file can be cleaned by maintenance */ }
    }

    private List<PageText> extractPages(byte[] bytes) {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> result = new ArrayList<>();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page);
                result.add(new PageText(page, stripper.getText(pdf)));
            }
            return result;
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded PDF could not be processed.");
        }
    }

    private DocumentResponse toResponse(KnowledgeDocument d) {
        return new DocumentResponse(d.getId(), d.getOriginalFilename(), d.getFileSize(), d.getPageCount(), d.getChunkCount(), d.getUploadedAt());
    }
    private record PageText(int page, String text) {}
}
