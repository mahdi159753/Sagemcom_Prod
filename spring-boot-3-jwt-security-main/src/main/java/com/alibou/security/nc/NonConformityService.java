package com.alibou.security.nc;

import com.alibou.security.produit.FileStorageService;
import com.alibou.security.notification.EmailService;
import com.alibou.security.notification.NotificationService;
import com.alibou.security.notification.NotificationType;
import com.alibou.security.user.UserRepository;
import com.alibou.security.user.User;
import com.alibou.security.user.Role;
import lombok.RequiredArgsConstructor;
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
            emailService.sendEmail(assigneeEmail, 
                "Vous avez été assigné à la Non-Conformité " + existing.getReference(), 
                "Bonjour,\nVous êtes désormais responsable du traitement de la NC: " + existing.getReference() + 
                "\nGravité: " + existing.getGravite() + "\nDescription: " + existing.getDescription());
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
            emailService.sendEmail(u.getEmail(), 
                "Action corrective soumise pour la NC " + existing.getReference(), 
                "L'utilisateur assigné (" + (existing.getAssigneeEmail() != null ? existing.getAssigneeEmail() : "Inconnu") + ") a soumis l'action suivante pour validation :\n\n" + actionCorrective);
        }
            
        return saved;
    }

    public NonConformity validateAndClose(Integer id) {
        NonConformity existing = getById(id);
        existing.setStatut("CLOTUREE");
        // dateCloture is updated via @PreUpdate
        NonConformity saved = repository.save(existing);
        
        if (existing.getAssigneeEmail() != null && !existing.getAssigneeEmail().isBlank()) {
             emailService.sendEmail(existing.getAssigneeEmail(), 
                "Non-Conformité Clôturée: " + existing.getReference(), 
                "Bonjour,\nL'action corrective pour la NC " + existing.getReference() + 
                " a été validée avec succès. La NC est désormais clôturée.");
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
