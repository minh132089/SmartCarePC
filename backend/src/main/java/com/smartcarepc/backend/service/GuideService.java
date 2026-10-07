package com.smartcarepc.backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.dto.AssemblyStepResponse;
import com.smartcarepc.backend.dto.ModelCreditResponse;
import com.smartcarepc.backend.repository.AssemblyStepRepository;
import com.smartcarepc.backend.repository.ModelCreditRepository;

import lombok.RequiredArgsConstructor;

/** Nội dung hướng dẫn: các bước lắp máy và nguồn model 3D. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideService {

    private final AssemblyStepRepository assemblyStepRepository;
    private final ModelCreditRepository modelCreditRepository;

    public List<AssemblyStepResponse> findAssemblySteps() {
        return assemblyStepRepository.findAll(Sort.by("stepNo")).stream().map(AssemblyStepResponse::from).toList();
    }

    public List<ModelCreditResponse> findCredits() {
        return modelCreditRepository.findAll(Sort.by("id")).stream().map(ModelCreditResponse::from).toList();
    }
}
