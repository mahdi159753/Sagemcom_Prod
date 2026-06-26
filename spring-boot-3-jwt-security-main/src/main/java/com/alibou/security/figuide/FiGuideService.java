package com.alibou.security.figuide;

import com.alibou.security.produit.Produit;
import com.alibou.security.produit.ProduitRepository;
import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiGuideService {

    private final FiDocumentRepository fiDocumentRepository;
    private final InstructionStepRepository instructionStepRepository;
    private final ControlSessionRepository controlSessionRepository;
    private final ControlStepValidationRepository controlStepValidationRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;

    private final Path fileStorageLocation = Paths.get("uploads/fi_files").toAbsolutePath().normalize();
    private final Path imageStorageLocation = Paths.get("uploads/fi_images").toAbsolutePath().normalize();

    private static class SheetImage {
        int row;
        int col;
        int widthCells;
        int heightCells;
        byte[] data;
        String extension;
        public SheetImage(int row, int col, int widthCells, int heightCells, byte[] data, String extension) {
            this.row = row; this.col = col; 
            this.widthCells = widthCells; this.heightCells = heightCells;
            this.data = data; this.extension = extension;
        }
    }

    private static class TempStep {
        InstructionStep step;
        int row;
        int col;
        public TempStep(InstructionStep step, int row, int col) {
            this.step = step;
            this.row = row;
            this.col = col;
        }
    }

    // ── UPLOAD & PARSE ────────────────────────────────────────────────────────

    @Transactional
    public FiDocument uploadFiche(Integer produitId, MultipartFile file, String version, User u) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + produitId));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty or not provided.");
        }

        if (version == null || version.trim().isEmpty()) {
            throw new RuntimeException("Version is mandatory.");
        }

        // 1. Save Excel file
        String filePath = saveExcelFile(file, produit.getReference(), version);

        // 2. Deactivate previous active FIs for this product
        Optional<FiDocument> activeFi = fiDocumentRepository.findByProduitIdAndActiveTrue(produitId);
        if (activeFi.isPresent()) {
            FiDocument oldFi = activeFi.get();
            oldFi.setActive(false);
            fiDocumentRepository.save(oldFi);
        }

        // 3. Save new FiDocument (without extracted images first to generate ID)
        FiDocument fiDocument = FiDocument.builder()
                .produit(produit)
                .version(version.trim())
                .fileName(StringUtils.cleanPath(file.getOriginalFilename()))
                .filePath(filePath)
                .uploadedBy(u)
                .active(true)
                .build();
        fiDocument = fiDocumentRepository.save(fiDocument);

        // 4. Render Excel sheets to PNG files using Python OLE automation
        renderSheetsToImages(filePath, fiDocument.getId());

        // 5. Parse Excel file and automatically map sheet images
        ParseResult parseResult;
        try {
            parseResult = parseExcel(filePath, fiDocument.getId());
        } catch (Exception e) {
            log.error("Error parsing Excel file", e);
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        List<InstructionStep> steps = parseResult.steps;
        List<String> extractedImageUrls = parseResult.extractedImageUrls;

        if (steps.isEmpty()) {
            throw new RuntimeException("No quality control operations found in the Excel file. Verify columns: Step, Operation, Criteria, Warning.");
        }

        // 6. Link steps and save
        for (InstructionStep step : steps) {
            step.setFiDocument(fiDocument);
            instructionStepRepository.save(step);
        }

        // 7. Update FiDocument with the extracted page URLs
        fiDocument.setExtractedImageUrls(String.join(",", extractedImageUrls));
        fiDocument = fiDocumentRepository.save(fiDocument);

        return fiDocument;
    }

    private void renderSheetsToImages(String filePath, Integer docId) {
        try {
            String prefix = "doc_" + docId;
            String outputDir = this.imageStorageLocation.toString();
            
            log.info("Starting Python Excel to PNG rendering for file: {}, prefix: {}", filePath, prefix);
            
            // Execute: python excel_to_png.py <excel_path> <output_dir> <prefix>
            ProcessBuilder pb = new ProcessBuilder("python", "excel_to_png.py", filePath, outputDir, prefix);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output to logs
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python Render] " + line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python Excel to PNG process failed with exit code: " + exitCode);
                throw new RuntimeException("Python rendering process failed with exit code " + exitCode);
            }
            log.info("Python Excel to PNG rendering completed successfully");
        } catch (Exception e) {
            log.error("Failed to render Excel sheets to PNG", e);
            throw new RuntimeException("Excel sheet rendering failed: " + e.getMessage(), e);
        }
    }

    private String saveExcelFile(MultipartFile file, String reference, String version) {
        try {
            Files.createDirectories(this.fileStorageLocation);
            
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String cleanVersion = version.replaceAll("[^a-zA-Z0-9]", "");
            String cleanRef = reference.replaceAll("[^a-zA-Z0-9]", "");
            String fileName = String.format("fi_%s_%s_%s.xlsx", cleanRef, cleanVersion, dateStr);
            
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    // Helper class to return both steps and extracted images from parsing
    private static class ParseResult {
        List<InstructionStep> steps;
        List<String> extractedImageUrls;
        public ParseResult(List<InstructionStep> steps, List<String> extractedImageUrls) {
            this.steps = steps;
            this.extractedImageUrls = extractedImageUrls;
        }
    }

    private ParseResult parseExcel(String filePath, Integer docId) throws Exception {
        List<InstructionStep> steps = new ArrayList<>();
        List<String> allExtractedImageUrls = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(new File(filePath));

        java.util.regex.Pattern opPattern = java.util.regex.Pattern.compile("(?i)^Op[^\\d]*(\\d+)\\s*[:]*\\s*([^\\n]+)");
        Set<String> ignoreWords = Set.of("emetteur", "vérificateur", "approbateur", "signataires", "mode opératoire", "folio", "ref. produit", "24 033 884", "contrôles et paramètres");
        Set<String> skipSheets = Set.of("Table 1", "Annexe", "Feuil1");
        
        Files.createDirectories(this.imageStorageLocation);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName().trim();
            if (skipSheets.contains(sheetName)) {
                continue;
            }
            
            // Extract sheet number to map PNG
            java.util.regex.Matcher numMatcher = java.util.regex.Pattern.compile("\\d+").matcher(sheetName);
            Integer sheetNum = null;
            if (numMatcher.find()) {
                sheetNum = Integer.parseInt(numMatcher.group(0));
            }
            
            if (sheetNum == null) {
                continue;
            }
            
            String pageImageName = String.format("doc_%d_page_%02d.png", docId, sheetNum);
            String imageUrl = "/api/v1/fi-guide/images/" + pageImageName;
            
            List<InstructionStep> sheetSteps = new ArrayList<>();

            // Parse operations
            for (Row row : sheet) {
                String rowWarning = null;
                for (Cell cell : row) {
                    String val = getCellValueAsString(cell).trim();
                    if (val.toLowerCase().contains("attention")) {
                        rowWarning = (rowWarning == null ? "" : rowWarning + "\n") + val;
                    }
                }

                for (Cell cell : row) {
                    String text = getCellValueAsString(cell).trim();
                    if (text.isEmpty() || ignoreWords.contains(text.toLowerCase())) {
                        continue;
                    }

                    java.util.regex.Matcher m = opPattern.matcher(text);
                    if (m.find()) {
                        int stepNum = Integer.parseInt(m.group(1));
                        String opTitle = m.group(2).trim();
                        opTitle = opTitle.replaceAll("[:\\s]+$", "");
                        opTitle = opTitle.replaceAll("\\d{6,}$", "").trim();

                        String[] lines = text.split("\\r?\\n");
                        List<String> criteriaList = new ArrayList<>();
                        
                        for (int j = 1; j < lines.length; j++) {
                            String line = lines[j].trim();
                            if (line.isEmpty() || line.toLowerCase().startsWith("op")) {
                                continue;
                            }
                            String criterion = line.replaceAll("^[*•\\-\\.]+\\s*", "").trim();
                            if (!criterion.isEmpty()) {
                                criteriaList.add(criterion);
                            }
                        }

                        String description = String.join("\n", criteriaList);
                        
                        InstructionStep step = InstructionStep.builder()
                                .stepNumber(String.valueOf(stepNum))
                                .operation(opTitle)
                                .description(description)
                                .warningText(rowWarning)
                                .imageUrls(imageUrl) // Auto-assign the rendered sheet PNG URL
                                .build();
                        sheetSteps.add(step);
                    }
                }
            }
            
            if (!sheetSteps.isEmpty()) {
                steps.addAll(sheetSteps);
                if (!allExtractedImageUrls.contains(imageUrl)) {
                    allExtractedImageUrls.add(imageUrl);
                }
            }
        }

        // Sort by step number
        steps.sort(Comparator.comparingInt(s -> Integer.parseInt(s.getStepNumber())));

        // Reassign sequenceOrder
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setSequenceOrder(i + 1);
        }

        workbook.close();
        return new ParseResult(steps, allExtractedImageUrls);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double doubleVal = cell.getNumericCellValue();
                if (doubleVal == (long) doubleVal) {
                    return String.valueOf((long) doubleVal);
                }
                return String.valueOf(doubleVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ── ACTIVE INSTRUCTIONS ───────────────────────────────────────────────────

    public FiDocument getActiveDocument(Integer produitId) {
        return fiDocumentRepository.findByProduitIdAndActiveTrue(produitId)
                .orElseThrow(() -> new RuntimeException("No active guided instruction found for product " + produitId));
    }

    public List<InstructionStep> getStepsForDocument(Integer fiDocId) {
        return instructionStepRepository.findByFiDocumentIdOrderBySequenceOrderAsc(fiDocId);
    }

    // ── QUALITY CONTROL SESSIONS ──────────────────────────────────────────────

    @Transactional
    public ControlSession startSession(Integer produitId, User operator) {
        FiDocument activeFi = fiDocumentRepository.findByProduitIdAndActiveTrue(produitId)
                .orElseThrow(() -> new RuntimeException("Cannot start control session: no active instruction sheet uploaded for this product."));

        ControlSession session = ControlSession.builder()
                .fiDocument(activeFi)
                .produit(activeFi.getProduit())
                .operator(operator)
                .build(); // PrePersist initializes status to IN_PROGRESS, conforme = true

        return controlSessionRepository.save(session);
    }

    @Transactional
    public ControlStepValidation validateStep(Integer sessionId, Integer stepId, String status, String comment, User operator) {
        ControlSession session = controlSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("Cannot validate step. This session is already " + session.getStatus());
        }

        InstructionStep step = instructionStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Instruction step not found: " + stepId));

        if (!step.getFiDocument().getId().equals(session.getFiDocument().getId())) {
            throw new RuntimeException("This step does not belong to the session's active document.");
        }

        if ("NON_CONFORME".equalsIgnoreCase(status) && (comment == null || comment.trim().isEmpty())) {
            throw new RuntimeException("A comment is mandatory if the status is NON_CONFORME.");
        }

        // If step is Non-Conforme, overall session conformance is false
        if ("NON_CONFORME".equalsIgnoreCase(status)) {
            session.setConforme(false);
            controlSessionRepository.save(session);
        }

        // Save validation (overwrite if exists)
        Optional<ControlStepValidation> existingVal = controlStepValidationRepository
                .findByControlSessionIdAndInstructionStepId(sessionId, stepId);

        ControlStepValidation validation;
        if (existingVal.isPresent()) {
            validation = existingVal.get();
            validation.setStatus(status.toUpperCase());
            validation.setComment(comment != null ? comment.trim() : null);
            validation.setValidatedAt(LocalDateTime.now());
        } else {
            validation = ControlStepValidation.builder()
                    .controlSession(session)
                    .instructionStep(step)
                    .status(status.toUpperCase())
                    .comment(comment != null ? comment.trim() : null)
                    .build(); // PrePersist adds validatedAt
        }

        return controlStepValidationRepository.save(validation);
    }

    @Transactional
    public ControlSession completeSession(Integer sessionId) {
        ControlSession session = controlSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("Session is already completed or closed.");
        }

        // Check if all steps have been validated
        List<InstructionStep> steps = instructionStepRepository.findByFiDocumentIdOrderBySequenceOrderAsc(session.getFiDocument().getId());
        List<ControlStepValidation> validations = controlStepValidationRepository.findByControlSessionIdOrderByValidatedAtAsc(sessionId);

        if (validations.size() < steps.size()) {
            throw new RuntimeException("Cannot complete session. Only " + validations.size() + " out of " + steps.size() + " steps have been validated.");
        }

        // Update overall status
        boolean allConforme = validations.stream().allMatch(val -> "CONFORME".equalsIgnoreCase(val.getStatus()));
        session.setConforme(allConforme);
        session.setStatus(allConforme ? "COMPLETED" : "FAILED");
        session.setCompletedAt(LocalDateTime.now());

        return controlSessionRepository.save(session);
    }

    // ── REPORT GENERATION ─────────────────────────────────────────────────────

    public Map<String, Object> getSessionReport(Integer sessionId) {
        ControlSession session = controlSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        List<ControlStepValidation> validations = controlStepValidationRepository.findByControlSessionIdOrderByValidatedAtAsc(sessionId);
        List<InstructionStep> steps = instructionStepRepository.findByFiDocumentIdOrderBySequenceOrderAsc(session.getFiDocument().getId());

        Map<String, Object> report = new HashMap<>();
        
        // Metadata
        report.put("sessionId", session.getId());
        report.put("produitName", session.getProduit().getName());
        report.put("produitRef", session.getProduit().getReference());
        report.put("fiVersion", session.getFiDocument().getVersion());
        report.put("fiFileName", session.getFiDocument().getFileName());
        report.put("operatorName", session.getOperator().getFirstname() + " " + session.getOperator().getLastname());
        report.put("operatorEmail", session.getOperator().getEmail());
        report.put("operatorMatricule", session.getOperator().getMatricule());
        report.put("status", session.getStatus());
        report.put("conforme", session.getConforme());
        report.put("startedAt", session.getStartedAt());
        report.put("completedAt", session.getCompletedAt());

        // Step validation details
        List<Map<String, Object>> stepDetails = new ArrayList<>();
        for (InstructionStep step : steps) {
            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("stepId", step.getId());
            stepMap.put("stepNumber", step.getStepNumber());
            stepMap.put("operation", step.getOperation());
            stepMap.put("description", step.getDescription());
            stepMap.put("warningText", step.getWarningText());
            stepMap.put("imageUrls", step.getImageUrls());

            // Check if validated
            Optional<ControlStepValidation> valOpt = validations.stream()
                    .filter(v -> v.getInstructionStep().getId().equals(step.getId()))
                    .findFirst();

            if (valOpt.isPresent()) {
                ControlStepValidation val = valOpt.get();
                stepMap.put("validated", true);
                stepMap.put("status", val.getStatus());
                stepMap.put("comment", val.getComment());
                stepMap.put("validatedAt", val.getValidatedAt());
            } else {
                stepMap.put("validated", false);
                stepMap.put("status", "PENDING");
                stepMap.put("comment", "");
                stepMap.put("validatedAt", null);
            }
            stepDetails.add(stepMap);
        }
        report.put("steps", stepDetails);

        return report;
    }

    public List<ControlSession> getAllSessions() {
        return controlSessionRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional
    public void deleteAllForProduit(Integer produitId) {
        // 1. Delete all sessions and their validations
        List<ControlSession> sessions = controlSessionRepository.findByProduitId(produitId);
        for (ControlSession session : sessions) {
            List<ControlStepValidation> validations = controlStepValidationRepository.findByControlSessionIdOrderByValidatedAtAsc(session.getId());
            controlStepValidationRepository.deleteAll(validations);
        }
        controlSessionRepository.deleteAll(sessions);

        // 2. Delete all FiDocuments and their steps
        List<FiDocument> documents = fiDocumentRepository.findByProduitIdOrderByUploadedAtDesc(produitId);
        for (FiDocument doc : documents) {
            List<InstructionStep> steps = instructionStepRepository.findByFiDocumentIdOrderBySequenceOrderAsc(doc.getId());
            instructionStepRepository.deleteAll(steps);
        }
        fiDocumentRepository.deleteAll(documents);
    }

    // ── IMAGE ASSIGNMENT (Manual by Manager) ─────────────────────────────────

    public List<String> getExtractedImages(Integer fiDocumentId) {
        FiDocument doc = fiDocumentRepository.findById(fiDocumentId)
                .orElseThrow(() -> new RuntimeException("FiDocument not found: " + fiDocumentId));
        String urls = doc.getExtractedImageUrls();
        if (urls == null || urls.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(urls.split(",")));
    }

    @Transactional
    public InstructionStep assignImageToStep(Integer stepId, String imageUrl) {
        InstructionStep step = instructionStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));
        
        String current = step.getImageUrls();
        if (current == null || current.isBlank()) {
            step.setImageUrls(imageUrl);
        } else {
            step.setImageUrls(current + "," + imageUrl);
        }
        return instructionStepRepository.save(step);
    }

    @Transactional
    public InstructionStep removeImageFromStep(Integer stepId, String imageUrl) {
        InstructionStep step = instructionStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));
        
        String current = step.getImageUrls();
        if (current != null) {
            List<String> urls = new ArrayList<>(Arrays.asList(current.split(",")));
            urls.remove(imageUrl);
            step.setImageUrls(urls.isEmpty() ? null : String.join(",", urls));
        }
        return instructionStepRepository.save(step);
    }

    @Transactional
    public void deleteSession(Integer sessionId) {
        ControlSession session = controlSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // 1. Delete all step validations for this session
        List<ControlStepValidation> validations = controlStepValidationRepository.findByControlSessionIdOrderByValidatedAtAsc(sessionId);
        controlStepValidationRepository.deleteAll(validations);
        
        // 2. Delete the session itself
        controlSessionRepository.delete(session);
    }
}
