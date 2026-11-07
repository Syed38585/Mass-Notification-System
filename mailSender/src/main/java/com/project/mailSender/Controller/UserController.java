package com.project.mailSender.Controller;

import com.project.mailSender.Model.UpdateDto;
import com.project.mailSender.Model.UserDto;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Service.JwtService;
import com.project.mailSender.Service.UnsubscribeTokenService;
import com.project.mailSender.Service.UserImportPreviewService;
import com.project.mailSender.Service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.hibernate.annotations.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    UserService service;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserImportPreviewService importPreviewService;

    @Cacheable(key = "#id", value = "users")
    @GetMapping("/get/all")
    public ResponseEntity<List<Users>> getAllUsers() {
        return new ResponseEntity<>(service.getAll(), HttpStatus.OK);
    }
//
//    @GetMapping("/trash")
//    public ResponseEntity<List<Users>> getTrash(){
//        return new ResponseEntity<>(service.getTrash(),HttpStatus.OK);
//    }

    @PostMapping("/create/user")
    public ResponseEntity<String> createUser(@RequestBody UserDto user) {
        return new ResponseEntity<>(service.create(user), HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    public void updateUser(@PathVariable Long id,
                           @RequestBody UpdateDto updateDto) {
        service.update(id, updateDto);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(
            @RequestParam String token) {

        System.out.println("Unsubscribe endpoint called");


        try {

            Long userId = jwtService.extractUnsubscribeUserId(token);

            String result =
                    service.unsubscribe(userId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid or expired unsubscribe link."
                    );
        }
    }

    @PatchMapping("/update/subs")
    public String updateSubStatus(@RequestParam Long id){
        System.out.println("Update API called");
        return service.updateStatus(id);
    }

    @PatchMapping("/update/subs/disable")
    public String disableSubStatus(@RequestParam Long id){
        System.out.println("Update API called");
        return service.updateStatusDisable(id);
    }

    @PostMapping(
            value = "/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> previewImport(
            @RequestParam("file") MultipartFile file) {

        try {
            return ResponseEntity.ok(importPreviewService.preview(file));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of("message",
                            "Failed to preview Excel file."));
        }
    }

    @PostMapping(
            value = "/import/commit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> commitImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("consent") boolean consent) {

        try {
            return ResponseEntity.ok(
                    importPreviewService.commit(file, consent)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Import failed."));
        }
    }
}
