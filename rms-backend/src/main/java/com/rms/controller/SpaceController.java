package com.rms.controller;

import com.rms.dto.*;
import com.rms.entity.SpaceStatus;
import com.rms.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
@CrossOrigin(origins = "http://localhost:5173")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SpaceResponseDTO> getAll(@RequestParam(required = false) SpaceStatus status) {
        return spaceService.getAll(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpaceResponseDTO getById(@PathVariable Long id) {
        return spaceService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponseDTO create(@Valid @RequestBody SpaceCreateRequest request) {
        return spaceService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpaceResponseDTO update(@PathVariable Long id, @Valid @RequestBody SpaceUpdateRequest request) {
        return spaceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        spaceService.delete(id);
    }
}
