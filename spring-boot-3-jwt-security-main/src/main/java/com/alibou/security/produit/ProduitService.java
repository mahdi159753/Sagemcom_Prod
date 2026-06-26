package com.alibou.security.produit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final FileStorageService fileStorageService;
    private final com.alibou.security.ligneproduction.LigneProductionRepository ligneProductionRepository;
    private final com.alibou.security.figuide.FiGuideService fiGuideService;

    public List<Produit> getAll() {
        return produitRepository.findAll();
    }

    public Produit getById(Integer id) {
        return produitRepository.findById(id).orElseThrow(() -> new RuntimeException("Produit non trouvé"));
    }

    public Produit create(Produit produit, MultipartFile file) {
        return produitRepository.findByNameAndReference(produit.getName(), produit.getReference())
            .map(existingProduit -> {
                existingProduit.setTotalProduced((existingProduit.getTotalProduced() == null ? 0 : existingProduit.getTotalProduced()) + 1);
                existingProduit.setCategory(produit.getCategory());
                existingProduit.setStatus(produit.getStatus());
                existingProduit.setCycleTime(produit.getCycleTime());
                existingProduit.setTargetRate(produit.getTargetRate());
                
                if (file != null && !file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file);
                    existingProduit.setInstructionPdfPath(fileName);
                }
                return produitRepository.save(existingProduit);
            }).orElseGet(() -> {
                produit.setTotalProduced(1);
                if (file != null && !file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file);
                    produit.setInstructionPdfPath(fileName);
                }
                return produitRepository.save(produit);
            });
    }

    public Produit update(Integer id, Produit produitDetails, MultipartFile file) {
        Produit produit = getById(id);
        
        produit.setName(produitDetails.getName());
        produit.setReference(produitDetails.getReference());
        produit.setCategory(produitDetails.getCategory());
        produit.setStatus(produitDetails.getStatus());
        produit.setTargetRate(produitDetails.getTargetRate());
        produit.setCycleTime(produitDetails.getCycleTime());
        
        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            produit.setInstructionPdfPath(fileName);
        }
        
        return produitRepository.save(produit);
    }

    public void delete(Integer id) {
        Produit produit = getById(id);
        
        // Clear the foreign key in LigneProduction before deleting the product
        List<com.alibou.security.ligneproduction.LigneProduction> lignes = ligneProductionRepository.findByProduitId(id);
        for (com.alibou.security.ligneproduction.LigneProduction ligne : lignes) {
            ligne.setProduit(null);
            ligneProductionRepository.save(ligne);
        }

        // Cascade delete FI documents, instructions, sessions and validations
        fiGuideService.deleteAllForProduit(id);
        
        // We could also delete the file from the disk, but keeping it is fine for now
        produitRepository.delete(produit);
    }
}
