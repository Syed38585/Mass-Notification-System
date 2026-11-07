package com.project.mailSender.Controller;

import com.project.mailSender.Model.Users;
import com.project.mailSender.Service.UserTrashService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserTrashController {

    private final UserTrashService service;

    public UserTrashController(UserTrashService service) {
        this.service = service;
    }

    // GET TRASH
    @GetMapping("/trash")
    public ResponseEntity<List<Users>> getTrash() {

        return ResponseEntity.ok(service.getTrash());
    }

    // RESTORE USER
    @PutMapping("/restore/{id}")
    public ResponseEntity<?> restore(
            @PathVariable Long id) {

        Users user = service.restore(id);

        return ResponseEntity.ok(Map.of(
                "message", "User restored successfully",
                "id", user.getId()
        ));
    }

    // PERMANENT DELETE
    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<?> permanentlyDelete(
            @PathVariable Long id) {

        service.permanentlyDelete(id);

        return ResponseEntity.ok(Map.of(
                "message", "User permanently deleted"
        ));
    }
}