package com.rms.service;

import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;

    public SpaceService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public List<Space> getAll(SpaceStatus status) {
        return status == null ? spaceRepository.findAll() : spaceRepository.findByStatus(status);
    }

    public Space getById(Long id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id " + id));
    }

    public Space create(Space space) {
        if (spaceRepository.existsByUnitNumber(space.getUnitNumber())) {
            throw new IllegalArgumentException("Unit number already exists: " + space.getUnitNumber());
        }
        space.setId(null);
        return spaceRepository.save(space);
    }

    public Space update(Long id, Space updated) {
        Space space = getById(id);
        space.setUnitNumber(updated.getUnitNumber());
        space.setFloor(updated.getFloor());
        space.setCategory(updated.getCategory());
        space.setSize(updated.getSize());
        space.setStatus(updated.getStatus());
        return spaceRepository.save(space);
    }

    public void delete(Long id) {
        Space space = getById(id);
        spaceRepository.delete(space);
    }
}
