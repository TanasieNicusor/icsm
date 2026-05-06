package com.example.icsm.service;

import com.example.icsm.model.Claim;
import com.example.icsm.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;

    public List<Claim> getClaimsByCustomer(Long customerId) {
        return claimRepository.findByCustomerId(customerId);
    }

    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    public Optional<Claim> getClaimById(Long id) {
        return claimRepository.findById(id);
    }

    public Claim saveClaim(Claim claim) {
        return claimRepository.save(claim);
    }

    public void deleteClaim(Long id) {
        claimRepository.deleteById(id);
    }
}
