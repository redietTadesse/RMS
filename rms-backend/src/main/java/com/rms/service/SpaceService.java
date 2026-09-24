package com.rms.service;

import com.rms.dto.*;
import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;

    public SpaceService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public List<SpaceResponseDTO> getAll(SpaceStatus status) {
        List<Space> spaces = status == null ? spaceRepository.findAll() : spaceRepository.findByStatus(status);
        return spaces.stream()
                .map(SpaceResponseDTO::from)
                .collect(Collectors.toList());
    }

    public SpaceResponseDTO getById(Long id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id " + id));
        return SpaceResponseDTO.from(space);
    }

    public SpaceResponseDTO create(SpaceCreateRequest request) {
        if (spaceRepository.existsByUnitNumber(request.getUnitNumber())) {
            throw new IllegalArgumentException("Unit number already exists: " + request.getUnitNumber());
        }
        Space space = new Space();
        space.setUnitNumber(request.getUnitNumber());
        space.setFloor(request.getFloor());
        space.setCategory(request.getCategory());
        space.setSize(request.getSize());
        space.setStatus(request.getStatus() != null ? request.getStatus() : SpaceStatus.VACANT);

        return SpaceResponseDTO.from(spaceRepository.save(space));
    }

    @Transactional
    public SpaceResponseDTO update(Long id, SpaceUpdateRequest request) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id " + id));

        space.setUnitNumber(request.getUnitNumber());
        space.setFloor(request.getFloor());
        space.setCategory(request.getCategory());
        space.setSize(request.getSize());
        if (request.getStatus() != null) {
            space.setStatus(request.getStatus());
        }

        return SpaceResponseDTO.from(spaceRepository.save(space));
    }

    public void delete(Long id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id " + id));
        spaceRepository.delete(space);
    }
}
