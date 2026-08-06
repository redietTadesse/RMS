package com.rms.controller;

import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import com.rms.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    /** GET /api/spaces  or  GET /api/spaces?status=VACANT */
    @GetMapping
    public List<Space> getAll(@RequestParam(required = false) SpaceStatus status) {
        return spaceService.getAll(status);
    }

    @GetMapping("/{id}")
    public Space getById(@PathVariable Long id) {
        return spaceService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Space create(@Valid @RequestBody Space space) {
        return spaceService.create(space);
    }

    @PutMapping("/{id}")
    public Space update(@PathVariable Long id, @Valid @RequestBody Space space) {
        return spaceService.update(id, space);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        spaceService.delete(id);
    }
}
