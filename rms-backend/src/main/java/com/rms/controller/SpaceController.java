package com.rms.controller;

import com.rms.entity.Space;
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
    public List<Space> getAll(@RequestParam(required = false) SpaceStatus status) {
        return spaceService.getAll(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Space getById(@PathVariable Long id) {
        return spaceService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Space create(@Valid @RequestBody Space space) {
        return spaceService.create(space);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Space update(@PathVariable Long id, @Valid @RequestBody Space space) {
        return spaceService.update(id, space);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        spaceService.delete(id);
    }
}
