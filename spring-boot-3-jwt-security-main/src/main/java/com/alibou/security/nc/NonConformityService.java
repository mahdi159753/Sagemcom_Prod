package com.alibou.security.nc;

import com.alibou.security.produit.FileStorageService;
import com.alibou.security.notification.EmailService;
import com.alibou.security.notification.NotificationService;
import com.alibou.security.notification.NotificationType;
import com.alibou.security.user.UserRepository;
import com.alibou.security.user.User;
import com.alibou.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NonConformityService {

    private final NonConformityRepository repository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NCAgentClient ncAgentClient;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private String buildHtmlTemplate(String title, String message, String ncReference, String actionUrl, String ctaText) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<style>" +
               "body { font-family: 'Inter', 'Segoe UI', sans-serif; background-color: #f0f2f5; margin: 0; padding: 40px 0; color: #1a1a1a; }" +
               ".wrapper { width: 100%; max-width: 650px; margin: 0 auto; }" +
               ".container { background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08); border: 1px solid #e1e4e8; }" +
               ".header { background: linear-gradient(135deg, #0056b3 0%, #007bff 100%); padding: 35px 40px; text-align: left; }" +
               ".header h2 { margin: 0; font-size: 26px; font-weight: 700; color: #ffffff; letter-spacing: 0.5px; }" +
               ".header p { margin: 8px 0 0 0; color: rgba(255,255,255,0.85); font-size: 15px; }" +
               ".content { padding: 40px; line-height: 1.7; }" +
               ".content h3 { color: #111827; margin-top: 0; font-size: 22px; font-weight: 600; }" +
               ".badge { display: inline-block; background-color: #eff6ff; color: #1d4ed8; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 15px; margin-bottom: 25px; border: 1px solid #bfdbfe; box-shadow: 0 2px 5px rgba(29, 78, 216, 0.05); }" +
               ".message-box { background-color: #f8fafc; border-left: 4px solid #007bff; padding: 20px; border-radius: 0 8px 8px 0; margin: 25px 0; color: #334155; }" +
               ".btn-container { text-align: center; margin-top: 35px; margin-bottom: 15px; }" +
               ".btn { display: inline-block; background: linear-gradient(to right, #0056b3, #007bff); color: #ffffff !important; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; transition: transform 0.2s, box-shadow 0.2s; box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3); }" +
               ".btn:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(0, 123, 255, 0.4); }" +
               ".footer { background-color: #f8fafc; padding: 25px 40px; text-align: center; font-size: 13px; color: #64748b; border-top: 1px solid #e2e8f0; }" +
               ".footer b { color: #475569; }" +
               "</style></head><body>" +
               "<div class='wrapper'><div class='container'>" +
               "<div class='header'>" +
               "<h2>SAGEMCOM</h2>" +
               "<p>Système de Gestion de Qualité</p>" +
               "</div>" +
               "<div class='content'>" +
               "<h3>" + title + "</h3>" +
               (ncReference != null ? "<div class='badge'>Réf: " + ncReference + "</div>" : "") +
               "<div class='message-box'>" + message.replace("\n", "<br>") + "</div>" +
               (actionUrl != null ? "<div class='btn-container'><a href='" + actionUrl + "' class='btn'>" + ctaText + "</a></div>" : "") +
               "</div>" +
               "<div class='footer'>Cet email est généré automatiquement par le portail <b>Sagemcom Quality Control</b>.<br>Veuillez ne pas répondre à ce message.</div>" +
               "</div></div></body></html>";
    }

    public List<NonConformity> getAll() {
        return repository.findAll();
    }

    public NonConformity getById(Integer id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Non-Conformité non trouvée"));
    }

    public NonConformity create(NonConformity nc, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            nc.setReportPdfPath(fileName);
        }
        NonConformity saved = repository.save(nc);
        if ("CRITIQUE".equalsIgnoreCase(saved.getGravite())) {
            notificationService.sendAlert("Non-Conformité CRITIQUE signalée sur: " + saved.getReference(), NotificationType.NC);
        }
        return saved;
    }

    public NonConformity update(Integer id, NonConformity ncDetails, MultipartFile file) {
        NonConformity existing = getById(id);

        existing.setReference(ncDetails.getReference());
        existing.setOrigine(ncDetails.getOrigine());
        existing.setLocalisation(ncDetails.getLocalisation());
        existing.setGravite(ncDetails.getGravite());
        existing.setStatut(ncDetails.getStatut());
        existing.setDescription(ncDetails.getDescription());
        existing.setActionCorrective(ncDetails.getActionCorrective());
        existing.setProcedureType(ncDetails.getProcedureType());
        existing.setLotConcerne(ncDetails.getLotConcerne());
        existing.setValideeParClient(ncDetails.getValideeParClient());

        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            existing.setReportPdfPath(fileName);
        }

        return repository.save(existing);
    }

    public void delete(Integer id) {
        NonConformity nc = getById(id);
        repository.delete(nc);
    }

    public NonConformity assign(Integer id, String assigneeEmail) {
        NonConformity existing = getById(id);
        existing.setAssigneeEmail(assigneeEmail);
        existing.setStatut("EN_TRAITEMENT");
        NonConformity saved = repository.save(existing);
        
        if (assigneeEmail != null && !assigneeEmail.isBlank()) {
            String title = "Nouvelle assignation de Non-Conformité";
            String message = "Bonjour,\nVous êtes désormais responsable du traitement de la NC: " + existing.getReference() + 
                             "\n\n<b>Gravité:</b> " + existing.getGravite() + 
                             "\n<b>Description:</b> " + existing.getDescription();
            String html = buildHtmlTemplate(title, message, existing.getReference(), frontendUrl + "/non-conformites", "Traiter la Non-Conformité");
            
            emailService.sendHtmlEmail(assigneeEmail, 
                "Action Requise : Vous avez été assigné à la Non-Conformité " + existing.getReference(), 
                html);
        }
        return saved;
    }

    public NonConformity treat(Integer id, String actionCorrective) {
        NonConformity existing = getById(id);
        existing.setActionCorrective(actionCorrective);
        existing.setStatut("A_VALIDER");
        NonConformity saved = repository.save(existing);
        
        List<User> ingenieurs = userRepository.findByRole(Role.INGENIEUR_QUALITE);
        for (User u : ingenieurs) {
            String title = "Validation requise : Action corrective soumise";
            String message = "L'utilisateur assigné (" + (existing.getAssigneeEmail() != null ? existing.getAssigneeEmail() : "Inconnu") + 
                             ") a soumis l'action suivante pour validation :\n\n<i>" + actionCorrective + "</i>";
            String html = buildHtmlTemplate(title, message, existing.getReference(), frontendUrl + "/non-conformites", "Valider l'action");
            
            emailService.sendHtmlEmail(u.getEmail(), 
                "Validation Requise : Action corrective pour la NC " + existing.getReference(), 
                html);
        }
            
        return saved;
    }

    public NonConformity validateAndClose(Integer id) {
        NonConformity existing = getById(id);
        existing.setStatut("CLOTUREE");
        // dateCloture is updated via @PreUpdate
        NonConformity saved = repository.save(existing);
        
        if (existing.getAssigneeEmail() != null && !existing.getAssigneeEmail().isBlank()) {
            String title = "Non-Conformité Clôturée";
            String message = "Bonjour,\nL'action corrective pour la NC " + existing.getReference() + 
                             " a été validée avec succès.\nLa Non-Conformité est désormais clôturée.";
            String html = buildHtmlTemplate(title, message, existing.getReference(), frontendUrl + "/non-conformites", "Consulter l'historique");
            
             emailService.sendHtmlEmail(existing.getAssigneeEmail(), 
                "Succès : Non-Conformité Clôturée (" + existing.getReference() + ")", 
                html);
        }
        
        return saved;
    }

    public NCAnalyzeResponse analyzeNC(String description, String localisation) {
        // Fetch historical CLOTUREE (closed) NCs to learn from successful resolutions
        List<NonConformity> closedNCs = repository.findAll().stream()
                .filter(nc -> "CLOTUREE".equalsIgnoreCase(nc.getStatut()))
                .toList();

        return ncAgentClient.analyze(description, localisation, closedNCs);
    }
}
