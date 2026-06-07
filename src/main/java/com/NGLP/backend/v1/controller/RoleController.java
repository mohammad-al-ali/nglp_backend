package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.Role;
import com.NGLP.backend.v1.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) { this.roleService = roleService; }

    @GetMapping
    public List<Role> getAll() { return roleService.findAll(); }

    @GetMapping("/{id}")
    public Role getById(@PathVariable Long id) { return roleService.findById(id); }
}
