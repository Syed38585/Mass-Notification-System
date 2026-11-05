package com.project.mailSender.Controller;

import com.project.mailSender.Model.UpdateDto;
import com.project.mailSender.Model.UserDto;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Service.JwtService;
import com.project.mailSender.Service.UnsubscribeTokenService;
import com.project.mailSender.Service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.hibernate.annotations.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    UserService service;

    @Autowired
    JwtService jwtService;

    @Cacheable(key = "#id", value = "users")
    @GetMapping("/get/all")
    public ResponseEntity<List<Users>> getAllUsers() {
        return new ResponseEntity<>(service.getAll(), HttpStatus.OK);
    }

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
}
