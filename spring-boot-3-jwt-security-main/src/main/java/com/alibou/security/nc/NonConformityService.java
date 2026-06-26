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
               "<html><head><style>" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 0; }" +
               ".container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }" +
               ".header { background-color: #00A3E0; color: #ffffff; padding: 25px; text-align: center; }" +
               ".header h2 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 1px; }" +
               ".content { padding: 30px; color: #333333; line-height: 1.6; }" +
               ".content h3 { color: #005A9C; margin-top: 0; font-size: 20px; }" +
               ".highlight { display: inline-block; background-color: #e6f2ff; color: #005A9C; padding: 5px 12px; border-radius: 15px; font-weight: 600; font-size: 14px; margin-bottom: 20px; border: 1px solid #b3d9ff; }" +
               ".btn { display: inline-block; background-color: #00A3E0; color: #ffffff !important; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 25px; transition: background-color 0.3s; }" +
               ".btn:hover { background-color: #0081b3; }" +
               ".footer { background-color: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #888888; border-top: 1px solid #eeeeee; }" +
               "</style></head><body>" +
               "<div class='container'>" +
               "<div class='header'><h2>Notification Sagemcom</h2></div>" +
               "<div class='content'>" +
               "<h3>" + title + "</h3>" +
               (ncReference != null ? "<div class='highlight'>Réf: " + ncReference + "</div>" : "") +
               "<p>" + message.replace("\n", "<br>") + "</p>" +
               (actionUrl != null ? "<div style='text-align: center;'><a href='" + actionUrl + "' class='btn'>" + ctaText + "</a></div>" : "") +
               "</div>" +
               "<div class='footer'>Cet email est généré automatiquement par le système de gestion de la qualité Sagemcom. Merci de ne pas y répondre.</div>" +
               "</div></body></html>";
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
